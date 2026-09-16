package com.match.app.ui.common

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Resolves protected Firebase Storage object references without minting long-lived download URLs.
 *
 * New protected media is stored as a `gs://` reference. Before a cached copy is reused we perform
 * an authenticated metadata read so current Firebase Storage Rules (block/hide/privacy state) are
 * evaluated again. If access has been revoked the cached copy is removed and no model is exposed.
 * Legacy HTTPS URLs and local/content URIs remain supported during migration.
 */
@Composable
fun rememberSecureMediaModel(source: String?): Any? {
    val context = LocalContext.current.applicationContext
    val normalized = source?.trim().orEmpty()
    val model by produceState<Any?>(initialValue = immediateLegacyModel(normalized), key1 = normalized) {
        value = resolveSecureMediaModel(context, normalized)
    }
    return model
}

/** Media3 needs a Uri rather than Coil's polymorphic model. */
@Composable
fun rememberSecureMediaUri(source: String?): Uri? {
    return when (val model = rememberSecureMediaModel(source)) {
        is Uri -> model
        is File -> Uri.fromFile(model)
        is String -> Uri.parse(model)
        else -> null
    }
}

private fun immediateLegacyModel(source: String): Any? = when {
    source.isBlank() -> null
    source.startsWith("gs://", ignoreCase = true) -> null
    source.startsWith("http://", ignoreCase = true) || source.startsWith("https://", ignoreCase = true) -> source
    source.startsWith("content://", ignoreCase = true) || source.startsWith("file://", ignoreCase = true) -> Uri.parse(source)
    else -> File(source)
}

suspend fun resolveSecureMediaModel(context: Context, source: String?): Any? {
    val normalized = source?.trim().orEmpty()
    if (normalized.isBlank()) return null
    if (!normalized.startsWith("gs://", ignoreCase = true)) return immediateLegacyModel(normalized)
    return resolveProtectedMediaFile(context, normalized)
}

/**
 * Resolve one protected `gs://` object to a private app-cache file through the authenticated
 * Firebase Storage SDK. A metadata request is deliberately made on every resolve so a newly
 * applied block/hide/privacy rule invalidates an older cached copy instead of leaking it.
 */
suspend fun resolveProtectedMediaFile(context: Context, gsUrl: String): File? {
    require(gsUrl.startsWith("gs://", ignoreCase = true)) { "Expected Firebase Storage gs:// reference" }
    return withContext(Dispatchers.IO) {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(gsUrl)
        val cacheDir = File(context.applicationContext.cacheDir, "protected_media").apply { mkdirs() }
        val extension = storageRef.name.substringAfterLast('.', "bin").take(8)
        val cacheFile = File(cacheDir, "${sha256(gsUrl)}.$extension")

        try {
            storageRef.metadata.await()
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                val temporary = File(cacheDir, "${cacheFile.name}.part")
                runCatching { temporary.delete() }
                storageRef.getFile(temporary).await()
                if (cacheFile.exists()) cacheFile.delete()
                if (!temporary.renameTo(cacheFile)) {
                    temporary.copyTo(cacheFile, overwrite = true)
                    temporary.delete()
                }
            }
            cacheFile
        } catch (_: Exception) {
            runCatching { cacheFile.delete() }
            null
        }
    }
}

private fun sha256(value: String): String = MessageDigest
    .getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
