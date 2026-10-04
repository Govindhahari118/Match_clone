package com.match.app.data.repo

import android.content.Context
import android.net.Uri
import com.match.app.data.local.dao.PhotoDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.PhotoEntity
import com.match.app.data.remote.FirebaseStorageService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ProfilePhotoAccess(
    val visibility: String = "PUBLIC",
    val canView: Boolean = true,
    val canRequest: Boolean = false,
    val requestStatus: String = ""
)

data class ProfilePhotoAccessRequest(
    val requesterUid: String,
    val targetUid: String,
    val status: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Singleton
class PhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PhotoDao,
    private val userDao: UserDao,
    private val storageService: FirebaseStorageService
) {
    private val functions = FirebaseFunctions.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    fun observe(userId: Long): Flow<List<PhotoEntity>> = dao.observeForUser(userId)
    suspend fun primaryPath(userId: Long): String? = withContext(Dispatchers.IO) { dao.primaryFor(userId)?.path }
    fun observeProfilePhotoVisibility(ownerUid: String): Flow<String> = callbackFlow {
        require(ownerUid.isNotBlank()) { "Missing account identity" }
        val reg = firestore.collection("privacySettings").document(ownerUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val value = snapshot?.getString("photoVisibility")?.uppercase()
                trySend(value?.takeIf { it in setOf("PUBLIC", "ACCEPTED_ONLY", "HIDDEN") } ?: "PUBLIC")
            }
        awaitClose { reg.remove() }
    }

    fun observeIncomingPhotoRequests(targetUid: String): Flow<List<ProfilePhotoAccessRequest>> = callbackFlow {
        require(targetUid.isNotBlank()) { "Missing account identity" }
        val reg = firestore.collection("photoRequests")
            .whereEqualTo("targetUid", targetUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val values = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val requesterUid = doc.getString("requesterUid")?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    val target = doc.getString("targetUid")?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    val status = doc.getString("status").orEmpty()
                    if (status != "PENDING") return@mapNotNull null
                    ProfilePhotoAccessRequest(
                        requesterUid = requesterUid,
                        targetUid = target,
                        status = status,
                        createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        updatedAtMillis = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    )
                }.sortedByDescending { it.updatedAtMillis }
                trySend(values)
            }
        awaitClose { reg.remove() }
    }

    suspend fun profilePhotoAccess(targetUid: String): Result<ProfilePhotoAccess> = runCatching {
        require(targetUid.isNotBlank()) { "Missing target profile" }
        val result = functions.getHttpsCallable("getProfilePhotoAccess")
            .call(mapOf("targetUid" to targetUid)).await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid photo access response")
        ProfilePhotoAccess(
            visibility = data["visibility"] as? String ?: "PUBLIC",
            canView = data["canView"] as? Boolean ?: false,
            canRequest = data["canRequest"] as? Boolean ?: false,
            requestStatus = data["requestStatus"] as? String ?: ""
        )
    }

    suspend fun setProfilePhotoVisibility(visibility: String): Result<String> = runCatching {
        val normalized = visibility.trim().uppercase()
        require(normalized in setOf("PUBLIC", "ACCEPTED_ONLY", "HIDDEN"))
        val result = functions.getHttpsCallable("setProfilePhotoVisibility")
            .call(mapOf("visibility" to normalized)).await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid photo visibility response")
        data["visibility"] as? String ?: error("Missing photo visibility")
    }

    suspend fun requestProfilePhotoAccess(targetUid: String): Result<ProfilePhotoAccess> = runCatching {
        require(targetUid.isNotBlank()) { "Missing target profile" }
        val result = functions.getHttpsCallable("requestProfilePhotoAccess")
            .call(mapOf("targetUid" to targetUid)).await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid photo request response")
        ProfilePhotoAccess(
            visibility = "ACCEPTED_ONLY",
            canView = data["canView"] as? Boolean ?: false,
            canRequest = false,
            requestStatus = data["status"] as? String ?: "UNKNOWN"
        )
    }

    suspend fun respondProfilePhotoAccess(requesterUid: String, approve: Boolean): Result<String> = runCatching {
        require(requesterUid.isNotBlank()) { "Missing requester profile" }
        val result = functions.getHttpsCallable("respondProfilePhotoAccess")
            .call(mapOf("requesterUid" to requesterUid, "approve" to approve)).await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid photo request response")
        data["status"] as? String ?: error("Missing photo request status")
    }


    suspend fun import(userId: Long, src: Uri): Result<PhotoEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val user = userDao.findById(userId) ?: error("User not found")
            val firebaseUid = user.firebaseUid.takeIf { it.isNotBlank() } ?: error("Profile is not linked to Firebase")
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
        val photo = dao.byId(photoId) ?: return@withContext
        if (photo.isPrimary) {
            setProfilePhotoVisibility(privacy).getOrThrow()
        }
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
