package com.match.app.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Storage service for uploading profile photos.
 * Photos are stored at: photos/{userId}/{timestamp}.jpg
 * Max size: 500KB (JPEG, quality reduced in loop to meet target).
 */
@Singleton
class FirebaseStorageService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val storage = FirebaseStorage.getInstance()

    companion object {
        private const val MAX_BYTES = 500 * 1024L  // 500 KB
        private const val MAX_DIMENSION = 1024       // px
    }

    /**
     * Compress a photo from [uri] to ≤500KB JPEG and upload to Firebase Storage.
     * Returns the download URL on success.
     */
    suspend fun uploadPhoto(userId: Long, uri: Uri): Result<String> = runCatching {
        val bytes = compressToTarget(uri)
        val ref = storage.reference.child("photos/$userId/${System.currentTimeMillis()}.jpg")
        ref.putBytes(bytes).await()
        ref.downloadUrl.await().toString()
    }

    /**
     * Delete a photo by its Firebase Storage URL.
     */
    suspend fun deletePhoto(url: String): Result<Unit> = runCatching {
        storage.getReferenceFromUrl(url).delete().await()
    }

    /**
     * Compress bitmap from URI to ≤MAX_BYTES JPEG.
     * Downscales first if dimensions > MAX_DIMENSION, then reduces quality.
     */
    private fun compressToTarget(uri: Uri): ByteArray {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = false }
        val bmp = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, opts)
        } ?: throw IllegalArgumentException("Cannot decode bitmap from URI: $uri")

        // Scale down if too large
        val scaled = if (bmp.width > MAX_DIMENSION || bmp.height > MAX_DIMENSION) {
            val ratio = MAX_DIMENSION.toFloat() / maxOf(bmp.width, bmp.height)
            val w = (bmp.width * ratio).toInt()
            val h = (bmp.height * ratio).toInt()
            Bitmap.createScaledBitmap(bmp, w, h, true).also {
                if (it !== bmp) bmp.recycle()
            }
        } else bmp

        // Reduce quality loop to hit ≤500KB
        var quality = 90
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            quality -= 10
        } while (bytes.size > MAX_BYTES && quality > 10)

        scaled.recycle()
        return bytes
    }
}
