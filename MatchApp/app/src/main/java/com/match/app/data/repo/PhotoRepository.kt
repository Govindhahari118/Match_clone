package com.match.app.data.repo

import android.content.Context
import android.net.Uri
import com.match.app.data.local.dao.PhotoDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.PhotoEntity
import com.match.app.data.remote.FirebaseStorageService
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PhotoDao,
    private val userDao: UserDao,
    private val storageService: FirebaseStorageService,
    private val consentRepository: ConsentRepository
) {
    private val functions = FirebaseFunctions.getInstance()
    fun observe(userId: Long): Flow<List<PhotoEntity>> = dao.observeForUser(userId)
    suspend fun primaryPath(userId: Long): String? = withContext(Dispatchers.IO) { dao.primaryFor(userId)?.path }

    suspend fun import(userId: Long, src: Uri): Result<PhotoEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val user = userDao.findById(userId) ?: error("User not found")
            val firebaseUid = user.firebaseUid.takeIf { it.isNotBlank() } ?: error("Profile is not linked to Firebase")
            // Choosing Upload to Profile is the explicit action that records the current media
            // processing notice before the protected object is submitted for moderation.
            consentRepository.set("media_processing", true)
            val remoteUrl = storageService.uploadPhoto(firebaseUid, src).getOrThrow()
            functions.getHttpsCallable("submitProfilePhoto")
                .call(mapOf("storagePath" to remoteUrl))
                .await()
            val makePrimaryLocally = dao.primaryFor(userId) == null
            val id = dao.insert(
                PhotoEntity(
                    userId = userId,
                    path = remoteUrl,
                    isPrimary = makePrimaryLocally
                )
            )
            dao.byId(id) ?: PhotoEntity(
                id = id,
                userId = userId,
                path = remoteUrl,
                isPrimary = makePrimaryLocally
            )
        }
    }

    suspend fun setPrimary(userId: Long, photoId: Long) = withContext(Dispatchers.IO) {
        val user = userDao.findById(userId) ?: return@withContext
        val photo = dao.byId(photoId) ?: return@withContext
        if (user.firebaseUid.isBlank()) return@withContext
        functions.getHttpsCallable("setPrimaryApprovedPhoto")
            .call(mapOf("storagePath" to photo.path))
            .await()
        dao.setPrimary(userId, photoId)
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
            val next = dao.primaryFor(photo.userId)
            if (next != null) {
                runCatching {
                    functions.getHttpsCallable("setPrimaryApprovedPhoto")
                        .call(mapOf("storagePath" to next.path))
                        .await()
                    dao.setPrimary(photo.userId, next.id)
                }
            }
        }
    }
}
