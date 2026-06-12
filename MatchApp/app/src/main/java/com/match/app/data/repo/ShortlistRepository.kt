package com.match.app.data.repo

import android.util.Log
import com.match.app.data.local.dao.ShortlistDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.ShortlistEntity
import com.match.app.data.remote.FirestoreShortlistService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortlistRepository @Inject constructor(
    private val dao: ShortlistDao,
    private val userDao: UserDao,
    private val firestoreShortlist: FirestoreShortlistService
) {
    suspend fun toggle(owner: Long, target: Long): Boolean = withContext(Dispatchers.IO) {
        val saved = dao.isSaved(owner, target)
        if (saved) dao.remove(owner, target) else dao.save(ShortlistEntity(ownerId = owner, targetId = target))
        // Sync to Firestore
        syncToFirestore(owner, target, !saved)
        !saved
    }
    suspend fun isSaved(owner: Long, target: Long): Boolean = withContext(Dispatchers.IO) { dao.isSaved(owner, target) }
    fun observeIsSaved(owner: Long, target: Long): Flow<Boolean> = dao.observeIsSaved(owner, target)
    fun observeSavedIds(owner: Long): Flow<List<Long>> = dao.observeSavedIds(owner)
    fun observeCount(owner: Long): Flow<Int> = dao.observeCount(owner)

    private suspend fun syncToFirestore(ownerLocalId: Long, targetLocalId: Long, isSaved: Boolean) {
        try {
            val ownerUid = userDao.findById(ownerLocalId)?.firebaseUid ?: return
            val targetUid = userDao.findById(targetLocalId)?.firebaseUid ?: return
            if (ownerUid.isBlank() || targetUid.isBlank()) return
            if (isSaved) firestoreShortlist.save(ownerUid, targetUid)
            else firestoreShortlist.remove(ownerUid, targetUid)
        } catch (e: Exception) {
            Log.w("ShortlistRepository", "Firestore shortlist sync failed", e)
        }
    }
}
