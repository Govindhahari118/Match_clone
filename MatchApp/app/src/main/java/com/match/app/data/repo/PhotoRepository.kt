package com.match.app.data.repo

import android.content.Context
import android.net.Uri
import com.match.app.data.local.dao.PhotoDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.PhotoEntity
import com.match.app.data.remote.FirebaseStorageService
import com.match.app.data.remote.FirestoreProfileService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PhotoDao,
    private val userDao: UserDao,
    private val storageService: FirebaseStorageService,
    private val profileService: FirestoreProfileService
) {
    fun observe(userId: Long): Flow<List<PhotoEntity>> = dao.observeForUser(userId)
    suspend fun primaryPath(userId: Long): String? = withContext(Dispatchers.IO) { dao.primaryFor(userId)?.path }

    suspend fun import(userId: Long, src: Uri): Result<PhotoEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val user = userDao.findById(userId) ?: error("User not found")
            val firebaseUid = user.firebaseUid.takeIf { it.isNotBlank() } ?: error("Profile is not linked to Firebase")
            val remoteUrl = storageService.uploadPhoto(firebaseUid, src).getOrThrow()
            val makePrimary = dao.primaryFor(userId) == null
            val id = dao.insert(PhotoEntity(userId = userId, path = remoteUrl, isPrimary = makePrimary))
            val saved = dao.byId(id) ?: PhotoEntity(id = id, userId = userId, path = remoteUrl, isPrimary = makePrimary)
            if (makePrimary) profileService.updateFields(firebaseUid, mapOf("photoUrl" to remoteUrl))
            saved
        }
    }

    suspend fun setPrimary(userId: Long, photoId: Long) = withContext(Dispatchers.IO) {
        val user = userDao.findById(userId) ?: return@withContext
        val photo = dao.byId(photoId) ?: return@withContext
        dao.setPrimary(userId, photoId)
        if (user.firebaseUid.isNotBlank()) profileService.updateFields(user.firebaseUid, mapOf("photoUrl" to photo.path))
    }

    suspend fun setPrivacy(photoId: Long, privacy: String) = withContext(Dispatchers.IO) {
        require(privacy in setOf("PUBLIC", "ACCEPTED_ONLY", "HIDDEN"))
        dao.setPrivacy(photoId, privacy)
    }

    suspend fun delete(photo: PhotoEntity) = withContext(Dispatchers.IO) {
        if (photo.path.startsWith("https://") || photo.path.startsWith("gs://") || photo.path.startsWith("photos/")) {
            storageService.deletePhoto(photo.path).getOrThrow()
        } else File(photo.path).delete()
        dao.delete(photo.id)
        if (photo.isPrimary) {
            val user = userDao.findById(photo.userId)
            val next = dao.primaryFor(photo.userId)
            if (user != null && user.firebaseUid.isNotBlank()) {
                profileService.updateFields(user.firebaseUid, mapOf("photoUrl" to (next?.path ?: "")))
            }
        }
    }
}
