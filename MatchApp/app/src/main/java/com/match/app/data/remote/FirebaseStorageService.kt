package com.match.app.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val storage = FirebaseStorage.getInstance()

    companion object {
        private const val MAX_PHOTO_BYTES = 500 * 1024L
        private const val MAX_DIMENSION = 1024
        private const val MAX_CHAT_IMAGE_BYTES = 8 * 1024 * 1024L
        private const val MAX_CHAT_VOICE_BYTES = 12 * 1024 * 1024L
    }

    suspend fun uploadPhoto(firebaseUid: String, uri: Uri): Result<String> = runCatching {
        require(firebaseUid.isNotBlank())
        val bytes = compressToTarget(uri)
        val ref = storage.reference.child("photos/$firebaseUid/${UUID.randomUUID()}.jpg")
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .setCustomMetadata("ownerUid", firebaseUid)
            .build()
        ref.putBytes(bytes, metadata).await()
        // Persist the authenticated object reference rather than minting a long-lived download
        // token. Rendering resolves gs:// through Firebase Storage so current privacy rules apply.
        ref.toString()
    }

    suspend fun deletePhoto(urlOrPath: String): Result<Unit> = runCatching {
        referenceFor(urlOrPath).delete().await()
    }

    suspend fun uploadChatImage(
        threadId: String,
        senderUid: String,
        recipientUid: String,
        clientMessageId: String,
        source: String
    ): Result<String> = uploadChatMedia(
        threadId, senderUid, recipientUid, clientMessageId, source,
        kind = "image", maxBytes = MAX_CHAT_IMAGE_BYTES, fallbackContentType = "image/jpeg"
    )

    suspend fun uploadChatVoice(
        threadId: String,
        senderUid: String,
        recipientUid: String,
        clientMessageId: String,
        source: String
    ): Result<String> = uploadChatMedia(
        threadId, senderUid, recipientUid, clientMessageId, source,
        kind = "voice", maxBytes = MAX_CHAT_VOICE_BYTES, fallbackContentType = "audio/mp4"
    )

    suspend fun downloadChatMedia(storagePath: String): Result<String> = runCatching {
        require(storagePath.startsWith("chat-media/")) { "Unexpected media path" }
        val ext = storagePath.substringAfterLast('.', "bin").take(8)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(storagePath.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val dir = File(context.filesDir, "chat_media").apply { mkdirs() }
        val file = File(dir, "$digest.$ext")
        if (!file.exists() || file.length() == 0L) storage.reference.child(storagePath).getFile(file).await()
        file.absolutePath
    }

    private suspend fun uploadChatMedia(
        threadId: String,
        senderUid: String,
        recipientUid: String,
        clientMessageId: String,
        source: String,
        kind: String,
        maxBytes: Long,
        fallbackContentType: String
    ): Result<String> = runCatching {
        require(threadId.matches(Regex("[a-f0-9]{64}")))
        require(senderUid.isNotBlank() && recipientUid.isNotBlank() && senderUid != recipientUid)
        require(clientMessageId.matches(Regex("[A-Za-z0-9_-]{16,128}")))
        val uri = sourceUri(source)
        val localFile = source.takeIf { !it.contains("://") }?.let(::File)
        if (localFile != null && localFile.exists()) require(localFile.length() in 1..maxBytes) { "Media too large" }
        val resolverType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        val contentType = resolverType?.takeIf {
            (kind == "image" && it.startsWith("image/")) || (kind == "voice" && it.startsWith("audio/"))
        } ?: fallbackContentType
        val ext = if (kind == "image") "jpg" else "m4a"
        val path = "chat-media/$threadId/$clientMessageId.$ext"
        val ref = storage.reference.child(path)
        val metadata = StorageMetadata.Builder()
            .setContentType(contentType)
            .setCustomMetadata("senderUid", senderUid)
            .setCustomMetadata("recipientUid", recipientUid)
            .setCustomMetadata("threadId", threadId)
            .setCustomMetadata("kind", kind)
            .build()
        ref.putFile(uri, metadata).await()
        path
    }

    private fun referenceFor(urlOrPath: String) = if (urlOrPath.startsWith("https://") || urlOrPath.startsWith("gs://")) {
        storage.getReferenceFromUrl(urlOrPath)
    } else storage.reference.child(urlOrPath.trimStart('/'))

    private fun sourceUri(value: String): Uri {
        val parsed = Uri.parse(value)
        return if (parsed.scheme.isNullOrBlank()) Uri.fromFile(File(value)) else parsed
    }

    private fun compressToTarget(uri: Uri): ByteArray {
        val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalArgumentException("Cannot decode image")
        val scaled = if (bmp.width > MAX_DIMENSION || bmp.height > MAX_DIMENSION) {
            val ratio = MAX_DIMENSION.toFloat() / maxOf(bmp.width, bmp.height)
            Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
                .also { if (it !== bmp) bmp.recycle() }
        } else bmp
        var quality = 90
        var bytes: ByteArray
        do {
            bytes = ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray()
            }
            quality -= 10
        } while (bytes.size > MAX_PHOTO_BYTES && quality > 10)
        scaled.recycle()
        require(bytes.size <= MAX_PHOTO_BYTES) { "Image could not be compressed to safe upload size" }
        return bytes
    }
}
