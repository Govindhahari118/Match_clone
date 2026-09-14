package com.match.app.data.repo

import android.util.Log
import com.match.app.data.local.dao.ShortlistDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.ShortlistEntity
import com.match.app.data.local.entity.UserEntity
import com.match.app.data.remote.FirestoreProfileService
import com.match.app.data.remote.FirestoreShortlistService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortlistRepository @Inject constructor(
    private val dao: ShortlistDao,
    private val userDao: UserDao,
    private val firestoreShortlist: FirestoreShortlistService,
    private val firestoreProfile: FirestoreProfileService
) {
    suspend fun toggle(owner: Long, target: Long): Boolean = withContext(Dispatchers.IO) {
        val ownerUid = userDao.findById(owner)?.firebaseUid.orEmpty()
        val targetUid = userDao.findById(target)?.firebaseUid.orEmpty()
        if (ownerUid.isBlank() || targetUid.isBlank()) error("Profile is not linked to Firebase")

        val nowSaved = firestoreShortlist.toggle(ownerUid, targetUid)
        if (nowSaved) {
            if (!dao.isSaved(owner, target)) dao.save(ShortlistEntity(ownerId = owner, targetId = target))
        } else {
            dao.remove(owner, target)
        }
        nowSaved
    }

    /** Firestore is authoritative whenever both profiles have remote identities. */
    suspend fun isSaved(owner: Long, target: Long): Boolean = withContext(Dispatchers.IO) {
        val ownerUid = userDao.findById(owner)?.firebaseUid.orEmpty()
        val targetUid = userDao.findById(target)?.firebaseUid.orEmpty()
        if (ownerUid.isNotBlank() && targetUid.isNotBlank()) {
            return@withContext runCatching { firestoreShortlist.isSaved(ownerUid, targetUid) }
                .getOrElse { dao.isSaved(owner, target) }
        }
        dao.isSaved(owner, target)
    }

    fun observeIsSaved(owner: Long, target: Long): Flow<Boolean> = dao.observeIsSaved(owner, target)
    fun observeSavedIds(owner: Long): Flow<List<Long>> = dao.observeSavedIds(owner)
    fun observeCount(owner: Long): Flow<Int> = dao.observeCount(owner)

    /** Cross-device authoritative shortlist stream with remote profiles hydrated into Room. */
    fun observeSavedIdsRemote(ownerUid: String): Flow<List<Long>> =
        firestoreShortlist.observeSavedUids(ownerUid).map { uids ->
            uids.distinct().mapNotNull { uid -> hydrate(uid)?.id }
        }

    suspend fun getSavedIdsRemote(ownerUid: String): List<Long> = withContext(Dispatchers.IO) {
        firestoreShortlist.getSavedUids(ownerUid).distinct().mapNotNull { uid -> hydrate(uid)?.id }
    }

    private suspend fun hydrate(uid: String): UserEntity? {
        if (uid.isBlank()) return null
        userDao.findByFirebaseUid(uid)?.let { return it }
        val remote = runCatching { firestoreProfile.fetchProfile(uid) }
            .onFailure { Log.w("ShortlistRepository", "Unable to hydrate shortlisted profile $uid", it) }
            .getOrNull() ?: return null
        val cached = remote.copy(
            email = "${remote.firebaseUid}@cache.invalid",
            passwordHash = "",
            isSeed = false
        )
        val id = userDao.insert(cached)
        return cached.copy(id = id)
    }
}
