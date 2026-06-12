package com.match.app.data.repo

import android.util.Log
import com.match.app.core.notification.NotificationHelper
import com.match.app.data.local.dao.BlockDao
import com.match.app.data.local.dao.LikeDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.BlockEntity
import com.match.app.data.local.entity.LikeEntity
import com.match.app.data.remote.FirestoreBlockService
import com.match.app.data.remote.FirestoreInterestService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val likeDao: LikeDao,
    private val blockDao: BlockDao,
    private val userDao: UserDao,
    private val notificationHelper: NotificationHelper,
    private val firestoreInterest: FirestoreInterestService,
    private val firestoreBlock: FirestoreBlockService
) {
    // ── Likes ──────────────────────────────────────────────────────────────
    suspend fun like(from: Long, to: Long)   = withContext(Dispatchers.IO) {
        likeDao.like(LikeEntity(fromUserId = from, toUserId = to))
        syncInterestToFirestore(from, to, isLike = true)
    }
    suspend fun unlike(from: Long, to: Long) = withContext(Dispatchers.IO) {
        likeDao.unlike(from, to)
        syncInterestToFirestore(from, to, isLike = false)
    }
    suspend fun toggleLike(from: Long, to: Long): Boolean = withContext(Dispatchers.IO) {
        val already = likeDao.isLiked(from, to)
        if (already) {
            likeDao.unlike(from, to)
            syncInterestToFirestore(from, to, isLike = false)
        } else {
            likeDao.like(LikeEntity(fromUserId = from, toUserId = to))
            val theirProfile = userDao.findById(to)
            val myProfile    = userDao.findById(from)
            val theyAlreadyLikedMe = likeDao.isLiked(to, from)

            // Sync to Firestore and check for mutual match
            val isMutual = syncInterestToFirestore(from, to, isLike = true)

            if (theyAlreadyLikedMe || isMutual) {
                val name = theirProfile?.displayName ?: "Someone"
                notificationHelper.notifyMutualMatch(name)
            } else {
                val myName = myProfile?.displayName ?: "Someone"
                notificationHelper.notifyInterestReceived(myName)
            }
        }
        !already
    }
    suspend fun isLiked(from: Long, to: Long): Boolean = withContext(Dispatchers.IO) { likeDao.isLiked(from, to) }
    fun observeLiked(from: Long, to: Long): Flow<Boolean> = likeDao.observeIsLiked(from, to)
    fun observeMutual(me: Long): Flow<List<LikeEntity>> = likeDao.observeMutualLikes(me)
    fun observeIncoming(me: Long): Flow<Int> = likeDao.observeIncomingCount(me)

    // ── Blocks ─────────────────────────────────────────────────────────────
    suspend fun block(me: Long, them: Long)   = withContext(Dispatchers.IO) {
        blockDao.block(BlockEntity(blockerId = me, blockedId = them))
        syncBlockToFirestore(me, them, isBlock = true)
    }
    suspend fun unblock(me: Long, them: Long) = withContext(Dispatchers.IO) {
        blockDao.unblock(me, them)
        syncBlockToFirestore(me, them, isBlock = false)
    }
    suspend fun isBlocked(me: Long, them: Long): Boolean = withContext(Dispatchers.IO) { blockDao.isBlocked(me, them) }
    suspend fun blockedIds(me: Long): List<Long> = withContext(Dispatchers.IO) { blockDao.blockedIds(me) }
    fun observeBlockedIds(me: Long): Flow<List<Long>> = blockDao.observeBlockedIds(me)

    // ── Interests (alias for likes used semantically as interests) ─────────
    fun observeReceivedInterests(me: Long): Flow<List<Long>> = likeDao.observeReceivedInterests(me)
    fun observeSentInterests(me: Long): Flow<List<Long>> = likeDao.observeSentInterests(me)

    /** Send a Super Interest (premium feature — stands out in recipient's inbox). */
    suspend fun superLike(from: Long, to: Long): Boolean = withContext(Dispatchers.IO) {
        val already = likeDao.isLiked(from, to)
        if (!already) {
            likeDao.like(LikeEntity(fromUserId = from, toUserId = to, isSuperLike = true))
        }
        // Always mark as super even if already liked (upgrade plain → super)
        val myProfile    = userDao.findById(from)
        val isMutual = syncInterestToFirestore(from, to, isLike = true, isSuperLike = true)
        val name = myProfile?.displayName ?: "Someone"
        notificationHelper.notifyInterestReceived("⭐ $name sent a Super Interest!")
        isMutual
    }

    /** Whether the given interest is a Super Interest. */
    suspend fun isSuperLike(from: Long, to: Long): Boolean =
        withContext(Dispatchers.IO) { likeDao.isSuperLike(from, to) }

    // ── Firestore sync helpers ─────────────────────────────────────────────

    private suspend fun syncInterestToFirestore(fromLocalId: Long, toLocalId: Long, isLike: Boolean, isSuperLike: Boolean = false): Boolean {
        return try {
            val fromUid = userDao.findById(fromLocalId)?.firebaseUid ?: return false
            val toUid = userDao.findById(toLocalId)?.firebaseUid ?: return false
            if (fromUid.isBlank() || toUid.isBlank()) return false
            if (isLike) firestoreInterest.sendInterest(fromUid, toUid, isSuperLike)
            else { firestoreInterest.removeInterest(fromUid, toUid); false }
        } catch (e: Exception) {
            Log.w("SocialRepository", "Firestore interest sync failed", e)
            false
        }
    }

    private suspend fun syncBlockToFirestore(meLocalId: Long, themLocalId: Long, isBlock: Boolean) {
        try {
            val meUid = userDao.findById(meLocalId)?.firebaseUid ?: return
            val themUid = userDao.findById(themLocalId)?.firebaseUid ?: return
            if (meUid.isBlank() || themUid.isBlank()) return
            if (isBlock) firestoreBlock.block(meUid, themUid)
            else firestoreBlock.unblock(meUid, themUid)
        } catch (e: Exception) {
            Log.w("SocialRepository", "Firestore block sync failed", e)
        }
    }
}
