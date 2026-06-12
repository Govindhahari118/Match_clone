package com.match.app.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.match.app.data.local.dao.PhotoDao
import com.match.app.data.local.entity.PhotoEntity
import com.match.app.data.remote.FirebaseStorageService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PhotoDao,
    private val storageService: FirebaseStorageService
) {
    companion object {
        private const val MAX_BYTES = 500 * 1024L   // 500 KB target
        private const val MAX_DIM   = 1024           // max dimension in px
    }

    fun observe(userId: Long): Flow<List<PhotoEntity>> = dao.observeForUser(userId)

    suspend fun primaryPath(userId: Long): String? =
        withContext(Dispatchers.IO) { dao.primaryFor(userId)?.path }

    /**
     * Import a photo: compress to ≤500 KB locally, attempt Firebase Storage upload,
     * persist the remote URL (or local path as fallback) in Room.
     */
    suspend fun import(userId: Long, src: Uri): Result<PhotoEntity> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Compress locally first
            val dir  = File(context.filesDir, "photos/$userId").apply { mkdirs() }
            val file = File(dir, "p_${System.currentTimeMillis()}.jpg")
            val compressedBytes = compressToTarget(src)
            file.writeBytes(compressedBytes)

            // 2. Attempt Firebase Storage upload (best-effort)
            val remoteUrl = storageService.uploadPhoto(userId, src).getOrNull()

            val makePrimary = dao.primaryFor(userId) == null
            val id = dao.insert(
                PhotoEntity(
                    userId    = userId,
                    path      = remoteUrl ?: file.absolutePath,
                    isPrimary = makePrimary
                )
            )
            dao.byId(id) ?: PhotoEntity(
                id = id, userId = userId,
                path = remoteUrl ?: file.absolutePath,
                isPrimary = makePrimary
            )
        }
    }

    suspend fun setPrimary(userId: Long, photoId: Long) =
        withContext(Dispatchers.IO) { dao.setPrimary(userId, photoId) }

    suspend fun setPrivacy(photoId: Long, privacy: String) =
        withContext(Dispatchers.IO) { dao.setPrivacy(photoId, privacy) }

    suspend fun delete(photo: PhotoEntity) = withContext(Dispatchers.IO) {
        dao.delete(photo.id)
        // Delete from Firebase Storage if it's a remote URL
        if (photo.path.startsWith("https://")) {
            storageService.deletePhoto(photo.path)
        } else {
            runCatching { File(photo.path).delete() }
        }
    }

    // ── Compression helper ────────────────────────────────────────────────────

    private fun compressToTarget(uri: Uri): ByteArray {
        val bmp = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: throw IllegalArgumentException("Cannot decode bitmap from URI: $uri")

        // Scale down if too large
        val scaled = if (bmp.width > MAX_DIM || bmp.height > MAX_DIM) {
            val ratio = MAX_DIM.toFloat() / maxOf(bmp.width, bmp.height)
            val w = (bmp.width * ratio).toInt()
            val h = (bmp.height * ratio).toInt()
            Bitmap.createScaledBitmap(bmp, w, h, true).also { if (it !== bmp) bmp.recycle() }
        } else bmp

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

