package com.match.app.data.repo

import com.match.app.data.local.dao.MessageDao
import com.match.app.data.local.dao.PendingMessageDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.MessageEntity
import com.match.app.data.local.entity.PendingMessageEntity
import com.match.app.data.remote.FirestoreChatService
import com.match.app.security.ChatCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val dao: MessageDao,
    private val pendingDao: PendingMessageDao,
    private val userDao: UserDao,
    private val firestoreChat: FirestoreChatService
) {

    companion object {
        /** Max plaintext chars allowed before encryption (encrypted output fits in Room TEXT). */
        private const val MAX_PLAINTEXT_LENGTH = 3000
        /**
         * Free users can send this many messages to a new match before being prompted to upgrade.
         * Premium users have no limit.
         */
        const val FREE_MSG_LIMIT = 5
    }

    /** Messages are decrypted transparently before being emitted to the UI. */
    fun thread(me: Long, peer: Long): Flow<List<MessageEntity>> = dao.observeThread(me, peer)
        .map { list ->
            list.map { msg ->
                val decrypted = ChatCrypto.decrypt(msg.body)
                when {
                    decrypted != null -> msg.copy(body = decrypted)
                    // If it looks like Base64 ciphertext but failed to decrypt, show error
                    msg.body.length > 24 && msg.body.all { it.isLetterOrDigit() || it in "+/=" } ->
                        msg.copy(body = "\u26A0 Unable to decrypt message")
                    // Legacy unencrypted message — show as-is
                    else -> msg
                }
            }
        }

    fun unread(me: Long): Flow<Int> = dao.observeUnread(me)

    /**
     * Returns `true` if this user is blocked by the free message limit.
     * Free users may send [FREE_MSG_LIMIT] messages per thread; premium users have no limit.
     */
    suspend fun isFreeLimitReached(me: Long, peer: Long): Boolean = withContext(Dispatchers.IO) {
        val user = userDao.findById(me) ?: return@withContext false
        if (user.isPremium) return@withContext false
        val sentCount = dao.countSentMessages(me, peer)
        sentCount >= FREE_MSG_LIMIT
    }

    suspend fun send(me: Long, peer: Long, body: String, replyToId: Long? = null) = withContext(Dispatchers.IO) {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return@withContext
        if (trimmed.length > MAX_PLAINTEXT_LENGTH) return@withContext // reject oversized messages
        // Enforce freemium limit
        val user = userDao.findById(me)
        if (user != null && !user.isPremium) {
            val sentCount = dao.countSentMessages(me, peer)
            if (sentCount >= FREE_MSG_LIMIT) return@withContext // blocked — UI should gate this
        }
        try {
            val encrypted = ChatCrypto.encrypt(trimmed) ?: trimmed
            // Write to local Room (optimistic, instant)
            dao.insert(MessageEntity(
                fromUserId = me, toUserId = peer,
                body = encrypted,
                replyToId = replyToId
            ))
            // Sync to Firestore (cross-device delivery)
            val myUid = userDao.findById(me)?.firebaseUid ?: ""
            val peerUid = userDao.findById(peer)?.firebaseUid ?: ""
            firestoreChat.sendMessage(me, peer, encrypted, myFirebaseUid = myUid, peerFirebaseUid = peerUid)
        } catch (_: Exception) {
            // Queue for retry if insertion/network fails
            pendingDao.insert(PendingMessageEntity(fromUserId = me, toUserId = peer, body = trimmed))
        }
    }

    suspend fun sendVoice(me: Long, peer: Long, voiceUri: String, durationMs: Long) =
        withContext(Dispatchers.IO) {
            val entity = MessageEntity(
                fromUserId      = me,
                toUserId        = peer,
                body            = "\uD83C\uDFA4 Voice message",
                voiceUri        = voiceUri,
                voiceDurationMs = durationMs
            )
            dao.insert(entity)
            val myUid = userDao.findById(me)?.firebaseUid ?: ""
            val peerUid = userDao.findById(peer)?.firebaseUid ?: ""
            firestoreChat.sendMessage(me, peer, entity.body, voiceUri = voiceUri, myFirebaseUid = myUid, peerFirebaseUid = peerUid)
        }

    suspend fun sendImage(me: Long, peer: Long, imageUri: String) =
        withContext(Dispatchers.IO) {
            val entity = MessageEntity(
                fromUserId = me,
                toUserId   = peer,
                body       = "\uD83D\uDCF7 Image",
                imageUri   = imageUri
            )
            dao.insert(entity)
            val myUid = userDao.findById(me)?.firebaseUid ?: ""
            val peerUid = userDao.findById(peer)?.firebaseUid ?: ""
            firestoreChat.sendMessage(me, peer, entity.body, imageUri = imageUri, myFirebaseUid = myUid, peerFirebaseUid = peerUid)
        }

    suspend fun markRead(me: Long, peer: Long) = withContext(Dispatchers.IO) {
        dao.markRead(me, peer)
        firestoreChat.markRead(me, peer)
    }

    /**
     * Opens a real-time Firestore listener for this thread and writes incoming
     * messages into Room so that [thread] emits them to the UI automatically.
     * Cancels when the calling coroutine scope is cancelled (e.g. ViewModel cleared).
     */
    suspend fun startFirestoreSync(me: Long, peer: Long) = withContext(Dispatchers.IO) {
        try {
            firestoreChat.observeThread(me, peer).collect { firestoreMsgs ->
                for (fm in firestoreMsgs) {
                    // Only write messages from the peer (our own are already in Room)
                    if (fm.fromUserId == peer && fm.toUserId == me) {
                        val alreadyExists = dao.countBySentAt(fm.fromUserId, fm.toUserId, fm.sentAt) > 0
                        if (!alreadyExists) {
                            dao.insert(MessageEntity(
                                fromUserId = fm.fromUserId,
                                toUserId   = fm.toUserId,
                                body       = fm.body,
                                voiceUri   = fm.voiceUri,
                                imageUri   = fm.imageUri,
                                sentAt     = fm.sentAt
                            ))
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Offline or listener error — Room cache remains valid
        }
    }
}
