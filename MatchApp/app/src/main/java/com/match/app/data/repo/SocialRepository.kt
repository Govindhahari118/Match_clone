package com.match.app.data.repo

import android.util.Log
import com.match.app.core.notification.NotificationHelper
import com.match.app.data.local.dao.BlockDao
import com.match.app.data.local.dao.LikeDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.BlockEntity
import com.match.app.data.local.entity.LikeEntity
import com.match.app.data.local.entity.UserEntity
import com.match.app.data.remote.FirestoreBlockService
import com.match.app.data.remote.FirestoreInterestService
import com.match.app.data.remote.FirestoreProfileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val firestoreBlock: FirestoreBlockService,
    private val firestoreProfile: FirestoreProfileService
) {
    suspend fun like(from: Long, to: Long) = withContext(Dispatchers.IO) {
        val synced = syncInterestToFirestore(from, to, isLike = true)
        if (!synced && !remoteInterestExists(from, to)) throw IllegalStateException("Interest could not be sent")
        likeDao.like(LikeEntity(fromUserId = from, toUserId = to))
    }

    suspend fun unlike(from: Long, to: Long) = withContext(Dispatchers.IO) {
        syncInterestToFirestore(from, to, isLike = false)
        likeDao.unlike(from, to)
    }

    /** Recipient declines a pending request. This does not block the sender. */
    suspend fun declineIncoming(me: Long, sender: Long) = withContext(Dispatchers.IO) {
        val myUid = userDao.findById(me)?.firebaseUid.orEmpty()
        val senderUid = userDao.findById(sender)?.firebaseUid.orEmpty()
        if (myUid.isBlank() || senderUid.isBlank()) throw IllegalStateException("Profile is not linked to Firebase")
        firestoreInterest.declineIncomingInterest(senderUid)
        runCatching { likeDao.unlike(sender, me) }
    }

    suspend fun toggleLike(from: Long, to: Long): Boolean = withContext(Dispatchers.IO) {
        val already = isLiked(from, to)
        if (already) {
            syncInterestToFirestore(from, to, isLike = false)
            likeDao.unlike(from, to)
            false
        } else {
            val theirProfile = userDao.findById(to)
            val myProfile = userDao.findById(from)
            val isMutual = syncInterestToFirestore(from, to, isLike = true)
            if (!isMutual && !remoteInterestExists(from, to)) throw IllegalStateException("Interest could not be sent")
            likeDao.like(LikeEntity(fromUserId = from, toUserId = to))
            if (isMutual || isLiked(to, from)) notificationHelper.notifyMutualMatch(theirProfile?.displayName ?: "Someone")
            else notificationHelper.notifyInterestReceived(myProfile?.displayName ?: "Someone")
            true
        }
    }

    suspend fun isLiked(from: Long, to: Long): Boolean = withContext(Dispatchers.IO) {
        val fromUid = userDao.findById(from)?.firebaseUid.orEmpty()
        val toUid = userDao.findById(to)?.firebaseUid.orEmpty()
        if (fromUid.isNotBlank() && toUid.isNotBlank()) {
            return@withContext runCatching { firestoreInterest.isInterested(fromUid, toUid) }
                .getOrElse { likeDao.isLiked(from, to) }
        }
        likeDao.isLiked(from, to)
    }

    fun observeLiked(from: Long, to: Long): Flow<Boolean> = likeDao.observeIsLiked(from, to)
    fun observeMutual(me: Long): Flow<List<LikeEntity>> = likeDao.observeMutualLikes(me)
    fun observeIncoming(me: Long): Flow<Int> = likeDao.observeIncomingCount(me)

    /**
     * Interest documents are visible to their participants even after a profile is later hidden.
     * Therefore every list hydration performs a fresh server-authorized profile read instead of
     * trusting an old Room row. A hide/block immediately removes that member card on the next
     * snapshot, while a stealth sender remains visible only through the explicit-request exception.
     */
    fun observeReceivedInterestsRemote(myUid: String): Flow<List<Long>> =
        firestoreInterest.observeIncomingInterests(myUid).map { docs -> cacheAuthorizedRemoteUids(docs.map { it.fromUid }) }

    fun observeSentInterestsRemote(myUid: String): Flow<List<Long>> =
        firestoreInterest.observeOutgoingInterests(myUid).map { docs -> cacheAuthorizedRemoteUids(docs.map { it.toUid }) }

    fun observeMutualIdsRemote(myUid: String): Flow<List<Long>> =
        firestoreInterest.observeMatches(myUid).map { matches -> cacheAuthorizedRemoteUids(matches.map { it.otherUid }) }

    suspend fun isMutualMatch(me: Long, them: Long): Boolean = withContext(Dispatchers.IO) {
        val meUid = userDao.findById(me)?.firebaseUid.orEmpty()
        val themUid = userDao.findById(them)?.firebaseUid.orEmpty()
        if (meUid.isBlank() || themUid.isBlank()) return@withContext likeDao.isLiked(me, them) && likeDao.isLiked(them, me)
        runCatching {
            firestoreInterest.isInterested(meUid, themUid) && firestoreInterest.isInterested(themUid, meUid)
        }.getOrElse { likeDao.isLiked(me, them) && likeDao.isLiked(them, me) }
    }

    suspend fun block(me: Long, them: Long) = withContext(Dispatchers.IO) {
        syncBlockToFirestore(me, them, isBlock = true)
        blockDao.block(BlockEntity(blockerId = me, blockedId = them))
    }

    suspend fun unblock(me: Long, them: Long) = withContext(Dispatchers.IO) {
        syncBlockToFirestore(me, them, isBlock = false)
        blockDao.unblock(me, them)
    }

    suspend fun isBlocked(me: Long, them: Long): Boolean = withContext(Dispatchers.IO) { blockDao.isBlocked(me, them) }
    suspend fun blockedIds(me: Long): List<Long> = withContext(Dispatchers.IO) { blockDao.blockedIds(me) }
    fun observeBlockedIds(me: Long): Flow<List<Long>> = blockDao.observeBlockedIds(me)
    fun observeReceivedInterests(me: Long): Flow<List<Long>> = likeDao.observeReceivedInterests(me)
    fun observeSentInterests(me: Long): Flow<List<Long>> = likeDao.observeSentInterests(me)

    suspend fun superLike(from: Long, to: Long): Boolean = withContext(Dispatchers.IO) {
        val myProfile = userDao.findById(from)
        val isMutual = syncInterestToFirestore(from, to, isLike = true, isSuperLike = true)
        if (!isMutual && !remoteInterestExists(from, to)) throw IllegalStateException("Super Interest could not be sent")
        if (!likeDao.isLiked(from, to)) likeDao.like(LikeEntity(fromUserId = from, toUserId = to, isSuperLike = true))
        notificationHelper.notifyInterestReceived("⭐ ${myProfile?.displayName ?: "Someone"} sent a Super Interest!")
        isMutual
    }

    suspend fun isSuperLike(from: Long, to: Long): Boolean = withContext(Dispatchers.IO) { likeDao.isSuperLike(from, to) }

    private suspend fun syncInterestToFirestore(
        fromLocalId: Long,
        toLocalId: Long,
        isLike: Boolean,
        isSuperLike: Boolean = false
    ): Boolean {
        val fromUid = userDao.findById(fromLocalId)?.firebaseUid.orEmpty()
        val toUid = userDao.findById(toLocalId)?.firebaseUid.orEmpty()
        if (fromUid.isBlank() || toUid.isBlank()) throw IllegalStateException("Profile is not linked to Firebase")
        return if (isLike) firestoreInterest.sendInterest(fromUid, toUid, isSuperLike)
        else {
            firestoreInterest.removeInterest(fromUid, toUid)
            false
        }
    }

    private suspend fun remoteInterestExists(fromLocalId: Long, toLocalId: Long): Boolean {
        val fromUid = userDao.findById(fromLocalId)?.firebaseUid.orEmpty()
        val toUid = userDao.findById(toLocalId)?.firebaseUid.orEmpty()
        if (fromUid.isBlank() || toUid.isBlank()) return false
        return runCatching { firestoreInterest.isInterested(fromUid, toUid) }.getOrDefault(false)
    }

    private suspend fun syncBlockToFirestore(meLocalId: Long, themLocalId: Long, isBlock: Boolean) {
        val meUid = userDao.findById(meLocalId)?.firebaseUid.orEmpty()
        val themUid = userDao.findById(themLocalId)?.firebaseUid.orEmpty()
        if (meUid.isBlank() || themUid.isBlank()) throw IllegalStateException("Profile is not linked to Firebase")
        if (isBlock) firestoreBlock.block(meUid, themUid) else firestoreBlock.unblock(meUid, themUid)
    }

    private suspend fun cacheAuthorizedRemoteUids(uids: List<String>): List<Long> = withContext(Dispatchers.IO) {
        uids.distinct().mapNotNull { uid ->
            if (uid.isBlank()) return@mapNotNull null
            val remote = runCatching { firestoreProfile.fetchProfileFromServer(uid) }
                .onFailure { Log.i("SocialRepository", "Profile is no longer visible in social list: $uid") }
                .getOrNull() ?: return@mapNotNull null
            cacheRemoteProfile(remote).id
        }
    }

    private suspend fun cacheRemoteProfile(remote: UserEntity): UserEntity {
        val existing = userDao.findByFirebaseUid(remote.firebaseUid)
        if (existing != null) {
            val merged = remote.copy(
                id = existing.id,
                email = existing.email,
                passwordHash = "",
                isSeed = false
            )
            userDao.update(merged)
            return merged
        }
        val cached = remote.copy(email = "${remote.firebaseUid}@cache.invalid", passwordHash = "", isSeed = false)
        val id = userDao.insert(cached)
        return cached.copy(id = id)
    }
}
