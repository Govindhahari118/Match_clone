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
        private const val MAX_PROFILE_VIDEO_BYTES = 50 * 1024 * 1024L
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
        ref.downloadUrl.await().toString()
    }

    suspend fun deletePhoto(urlOrPath: String): Result<Unit> = runCatching {
        referenceFor(urlOrPath).delete().await()
    }

    /**
     * Uploads the signed-in member's single profile video and returns a private Storage path.
     * The app stores this path rather than a tokenized public download URL.
     */
    suspend fun uploadProfileVideo(firebaseUid: String, uri: Uri): Result<String> = runCatching {
        require(firebaseUid.isNotBlank())
        val contentType = context.contentResolver.getType(uri)
            ?.takeIf { it.startsWith("video/") }
            ?: "video/mp4"
        require(contentType.startsWith("video/")) { "Selected file is not a video" }

        val declaredLength = runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        }.getOrNull()
        if (declaredLength != null && declaredLength >= 0) {
            require(declaredLength in 1..MAX_PROFILE_VIDEO_BYTES) { "Video must be 50 MB or smaller" }
        }

        val path = "videos/$firebaseUid/profile_video.mp4"
        val metadata = StorageMetadata.Builder()
            .setContentType(contentType)
            .setCustomMetadata("ownerUid", firebaseUid)
            .build()
        storage.reference.child(path).putFile(uri, metadata).await()
        path
    }

    /** Downloads an authorized private profile-video path into app-private cache for playback. */
    suspend fun downloadProfileVideo(storagePath: String): Result<String> = runCatching {
        require(storagePath.startsWith("videos/")) { "Unexpected video path" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(storagePath.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val dir = File(context.filesDir, "profile_videos").apply { mkdirs() }
        val file = File(dir, "$digest.mp4")
        if (!file.exists() || file.length() == 0L) {
            storage.reference.child(storagePath).getFile(file).await()
        }
        file.absolutePath
    }

    suspend fun deleteProfileVideo(storagePath: String): Result<Unit> = runCatching {
        require(storagePath.startsWith("videos/")) { "Unexpected video path" }
        storage.reference.child(storagePath).delete().await()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(storagePath.toByteArray())
            .joinToString("") { "%02x".format(it) }
        File(File(context.filesDir, "profile_videos"), "$digest.mp4").delete()
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
