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
                    msg.body.length > 24 && msg.body.all { it.isLetterOrDigit() || it in "+/=" } ->
                        msg.copy(body = "⚠ Unable to decrypt message")
                    else -> msg
                }
            }
        }

    fun unread(me: Long): Flow<Int> = dao.observeUnread(me)

    suspend fun isFreeLimitReached(me: Long, peer: Long): Boolean = withContext(Dispatchers.IO) {
        val user = userDao.findById(me) ?: return@withContext false
        if (user.isPremium) return@withContext false
        val sentCount = dao.countSentMessages(me, peer)
        sentCount >= FREE_MSG_LIMIT
    }

    suspend fun send(me: Long, peer: Long, body: String, replyToId: Long? = null) = withContext(Dispatchers.IO) {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return@withContext
        if (trimmed.length > MAX_PLAINTEXT_LENGTH) return@withContext

        val user = userDao.findById(me)
        if (user != null && !user.isPremium) {
            val sentCount = dao.countSentMessages(me, peer)
            if (sentCount >= FREE_MSG_LIMIT) return@withContext
        }

        val encrypted = ChatCrypto.encrypt(trimmed) ?: trimmed
        dao.insert(
            MessageEntity(
                fromUserId = me,
                toUserId = peer,
                body = encrypted,
                replyToId = replyToId
            )
        )

        val myUid = user?.firebaseUid.orEmpty()
        val peerUid = userDao.findById(peer)?.firebaseUid.orEmpty()
        if (myUid.isBlank() || peerUid.isBlank()) {
            pendingDao.insert(PendingMessageEntity(fromUserId = me, toUserId = peer, body = trimmed))
            return@withContext
        }

        try {
            firestoreChat.sendMessage(
                me,
                peer,
                encrypted,
                myFirebaseUid = myUid,
                peerFirebaseUid = peerUid
            )
        } catch (_: Exception) {
            pendingDao.insert(PendingMessageEntity(fromUserId = me, toUserId = peer, body = trimmed))
        }
    }

    suspend fun sendVoice(me: Long, peer: Long, voiceUri: String, durationMs: Long) =
        withContext(Dispatchers.IO) {
            val entity = MessageEntity(
                fromUserId = me,
                toUserId = peer,
                body = "🎤 Voice message",
                voiceUri = voiceUri,
                voiceDurationMs = durationMs
            )
            dao.insert(entity)

            val myUid = userDao.findById(me)?.firebaseUid.orEmpty()
            val peerUid = userDao.findById(peer)?.firebaseUid.orEmpty()
            if (myUid.isBlank() || peerUid.isBlank()) return@withContext

            try {
                firestoreChat.sendMessage(
                    me,
                    peer,
                    entity.body,
                    voiceUri = voiceUri,
                    myFirebaseUid = myUid,
                    peerFirebaseUid = peerUid
                )
            } catch (_: Exception) {
                // Keep the local copy and fail gracefully. Media upload/retry is handled
                // separately from plain-text pending messages because local URIs are not
                // safe to replay as cross-device URLs.
            }
        }

    suspend fun sendImage(me: Long, peer: Long, imageUri: String) =
        withContext(Dispatchers.IO) {
            val entity = MessageEntity(
                fromUserId = me,
                toUserId = peer,
                body = "📷 Image",
                imageUri = imageUri
            )
            dao.insert(entity)

            val myUid = userDao.findById(me)?.firebaseUid.orEmpty()
            val peerUid = userDao.findById(peer)?.firebaseUid.orEmpty()
            if (myUid.isBlank() || peerUid.isBlank()) return@withContext

            try {
                firestoreChat.sendMessage(
                    me,
                    peer,
                    entity.body,
                    imageUri = imageUri,
                    myFirebaseUid = myUid,
                    peerFirebaseUid = peerUid
                )
            } catch (_: Exception) {
                // Local message remains available without crashing the UI coroutine.
            }
        }

    suspend fun markRead(me: Long, peer: Long) = withContext(Dispatchers.IO) {
        dao.markRead(me, peer)
        try {
            firestoreChat.markRead(me, peer)
        } catch (_: Exception) {
            // Local read state is still valid while offline; remote state can catch up later.
        }
    }

    suspend fun startFirestoreSync(me: Long, peer: Long) = withContext(Dispatchers.IO) {
        try {
            firestoreChat.observeThread(me, peer).collect { firestoreMsgs ->
                for (fm in firestoreMsgs) {
                    if (fm.fromUserId == peer && fm.toUserId == me) {
                        val alreadyExists = dao.countBySentAt(fm.fromUserId, fm.toUserId, fm.sentAt) > 0
                        if (!alreadyExists) {
                            dao.insert(
                                MessageEntity(
                                    fromUserId = fm.fromUserId,
                                    toUserId = fm.toUserId,
                                    body = fm.body,
                                    voiceUri = fm.voiceUri,
                                    imageUri = fm.imageUri,
                                    sentAt = fm.sentAt
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Offline or listener error — Room cache remains valid.
        }
    }
}
