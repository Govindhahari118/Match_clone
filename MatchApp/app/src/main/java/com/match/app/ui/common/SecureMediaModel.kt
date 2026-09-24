package com.match.app.ui.common

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder
import java.security.MessageDigest

/**
 * Resolves protected Firebase Storage media through the authenticated SDK instead of exposing
 * long-lived download-token URLs to image/video consumers.
 *
 * Every protected resolve performs a metadata read first. That forces current Storage Rules to be
 * evaluated again, so a newly applied block/privacy restriction invalidates an older private cache.
 */
@Composable
fun rememberSecureMediaModel(source: String?): Any? {
    val context = LocalContext.current.applicationContext
    val normalized = source?.trim().orEmpty()
    val model by produceState<Any?>(initialValue = immediateUnprotectedModel(normalized), key1 = normalized) {
        value = resolveSecureMediaModel(context, normalized)
    }
    return model
}

/** Media3 consumers need a Uri rather than Coil's polymorphic model. */
@Composable
fun rememberSecureMediaUri(source: String?): Uri? {
    return when (val model = rememberSecureMediaModel(source)) {
        is Uri -> model
        is File -> Uri.fromFile(model)
        is String -> Uri.parse(model)
        else -> null
    }
}

fun isProtectedFirebaseStorageSource(source: String?): Boolean {
    val value = source?.trim().orEmpty()
    if (value.startsWith("gs://", ignoreCase = true)) return true
    val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return false
    val host = uri.host?.lowercase().orEmpty()
    return host == "firebasestorage.googleapis.com" ||
        host == "storage.googleapis.com" ||
        host.endsWith(".firebasestorage.app")
}

private fun immediateUnprotectedModel(source: String): Any? = when {
    source.isBlank() -> null
    isProtectedFirebaseStorageSource(source) -> null
    source.startsWith("http://", ignoreCase = true) ||
        source.startsWith("https://", ignoreCase = true) -> source
    source.startsWith("content://", ignoreCase = true) ||
        source.startsWith("file://", ignoreCase = true) -> Uri.parse(source)
    else -> File(source)
}

suspend fun resolveSecureMediaModel(context: Context, source: String?): Any? {
    val normalized = source?.trim().orEmpty()
    if (normalized.isBlank()) return null
    if (!isProtectedFirebaseStorageSource(normalized)) return immediateUnprotectedModel(normalized)
    return resolveProtectedMediaFile(context, normalized)
}

/**
 * Resolve a protected Firebase object into app-private cache bytes.
 *
 * Legacy tokenized Firebase HTTPS URLs are converted back to StorageReference identity before read,
 * so the token itself is never used as authorization. New writes should store gs:// references.
 */
suspend fun resolveProtectedMediaFile(context: Context, source: String): File? {
    require(isProtectedFirebaseStorageSource(source)) { "Expected Firebase Storage reference" }
    return withContext(Dispatchers.IO) {
        val storageRef = storageReferenceForProtectedSource(source)
        val cacheDir = File(context.applicationContext.cacheDir, "protected_media").apply { mkdirs() }
        val extension = storageRef.name.substringAfterLast('.', "bin").take(8).ifBlank { "bin" }
        val cacheFile = File(cacheDir, sha256(source) + "." + extension)

        try {
            // Re-authorize every resolve before any cached bytes can be returned.
            storageRef.metadata.await()
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                val temporary = File(cacheDir, cacheFile.name + ".part")
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

private fun storageReferenceForProtectedSource(source: String): StorageReference {
    val storage = FirebaseStorage.getInstance()
    if (source.startsWith("gs://", ignoreCase = true)) {
        return storage.getReferenceFromUrl(source)
    }

    val uri = Uri.parse(source)
    val host = uri.host?.lowercase().orEmpty()
    if (host == "firebasestorage.googleapis.com") {
        // Download-token form: /v0/b/<bucket>/o/<url-encoded-object-path>
        val segments = uri.pathSegments
        val bucketIndex = segments.indexOf("b")
        val objectIndex = segments.indexOf("o")
        if (bucketIndex >= 0 && bucketIndex + 1 < segments.size &&
            objectIndex >= 0 && objectIndex + 1 < segments.size
        ) {
            val bucket = segments[bucketIndex + 1]
            val encodedObject = segments.subList(objectIndex + 1, segments.size).joinToString("/")
            val objectPath = URLDecoder.decode(encodedObject, Charsets.UTF_8.name())
            return FirebaseStorage.getInstance("gs://" + bucket).reference.child(objectPath)
        }
    }
    if (host == "storage.googleapis.com") {
        val segments = uri.pathSegments
        require(segments.size >= 2) { "Malformed Firebase Storage URL" }
        val bucket = segments.first()
        val objectPath = segments.drop(1).joinToString("/")
        return FirebaseStorage.getInstance("gs://" + bucket).reference.child(objectPath)
    }

    // Keep SDK parsing as a final compatibility path for supported Firebase URL formats.
    return storage.getReferenceFromUrl(source)
}

private fun sha256(value: String): String = MessageDigest
    .getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
