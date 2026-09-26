package com.match.app.data.repo

import android.content.Context
import android.net.Uri
import com.match.app.data.local.dao.MessageDao
import com.match.app.data.local.dao.PendingMessageDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.MessageEntity
import com.match.app.data.local.entity.PendingMessageEntity
import com.match.app.data.remote.FirebaseStorageService
import com.match.app.data.remote.FirestoreChatService
import com.match.app.security.ChatCrypto
import com.match.app.worker.MessageRetryWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: MessageDao,
    private val pendingDao: PendingMessageDao,
    private val userDao: UserDao,
    private val firestoreChat: FirestoreChatService,
    private val storage: FirebaseStorageService
) {
    companion object { private const val MAX_PLAINTEXT_LENGTH = 3000 }

    /** Local Room copies are encrypted at rest with a device-local Android Keystore key. */
    fun thread(me: Long, peer: Long): Flow<List<MessageEntity>> = dao.observeThread(me, peer).map { list ->
        list.map { msg -> ChatCrypto.decrypt(msg.body)?.let { msg.copy(body = it) } ?: msg }
    }
    fun unread(me: Long): Flow<Int> = dao.observeUnread(me)

    /** Kept for older UI callers. Messaging is not monetized client-side. */
    suspend fun isFreeLimitReached(me: Long, peer: Long): Boolean = false

    suspend fun send(me: Long, peer: Long, body: String, replyToId: Long? = null) = withContext(Dispatchers.IO) {
        val trimmed = body.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_PLAINTEXT_LENGTH) return@withContext
        val clientId = UUID.randomUUID().toString().replace("-", "_")
        val encrypted = ChatCrypto.encrypt(trimmed) ?: trimmed
        val localId = dao.insert(MessageEntity(fromUserId = me, toUserId = peer, body = encrypted, replyToId = replyToId, clientMessageId = clientId, status = "sending"))
        sendOrQueue(me, peer, localId, clientId, "TEXT", trimmed, "", 0)
    }

    suspend fun sendVoice(me: Long, peer: Long, voiceUri: String, durationMs: Long) = withContext(Dispatchers.IO) {
        require(durationMs in 500..5 * 60 * 1000L) { "Invalid voice duration" }
        val clientId = UUID.randomUUID().toString().replace("-", "_")
        val persisted = persistOutboxMedia(voiceUri, clientId, "m4a")
        val localId = dao.insert(MessageEntity(fromUserId = me, toUserId = peer, body = "Voice message", voiceUri = persisted, voiceDurationMs = durationMs, clientMessageId = clientId, status = "sending"))
        sendOrQueue(me, peer, localId, clientId, "VOICE", "Voice message", persisted, durationMs)
    }

    suspend fun sendImage(me: Long, peer: Long, imageUri: String) = withContext(Dispatchers.IO) {
        val clientId = UUID.randomUUID().toString().replace("-", "_")
        val persisted = persistOutboxMedia(imageUri, clientId, "jpg")
        val localId = dao.insert(MessageEntity(fromUserId = me, toUserId = peer, body = "Image", imageUri = persisted, clientMessageId = clientId, status = "sending"))
        sendOrQueue(me, peer, localId, clientId, "IMAGE", "Image", persisted, 0)
    }

    private suspend fun sendOrQueue(
        me: Long,
        peer: Long,
        localId: Long,
        clientId: String,
        type: String,
        body: String,
        mediaUri: String,
        durationMs: Long
    ) {
        val myUid = userDao.findById(me)?.firebaseUid.orEmpty()
        val peerUid = userDao.findById(peer)?.firebaseUid.orEmpty()
        if (myUid.isBlank() || peerUid.isBlank()) {
            queue(me, peer, localId, clientId, type, body, mediaUri, durationMs)
            return
        }
        try {
            val threadId = FirestoreChatService.threadId(myUid, peerUid)
            var voicePath: String? = null
            var imagePath: String? = null
            when (type) {
                "IMAGE" -> imagePath = storage.uploadChatImage(threadId, myUid, peerUid, clientId, mediaUri).getOrThrow()
                "VOICE" -> voicePath = storage.uploadChatVoice(threadId, myUid, peerUid, clientId, mediaUri).getOrThrow()
            }
            firestoreChat.sendMessage(clientId, body, myUid, peerUid, voicePath, imagePath, durationMs.takeIf { it > 0 })
            dao.updateStatus(localId, "sent")
        } catch (_: Exception) {
            queue(me, peer, localId, clientId, type, body, mediaUri, durationMs)
        }
    }

    private suspend fun queue(me: Long, peer: Long, localId: Long, clientId: String, type: String, body: String, mediaUri: String, durationMs: Long) {
        dao.updateStatus(localId, "failed")
        pendingDao.insert(PendingMessageEntity(fromUserId = me, toUserId = peer, body = body, localMessageId = localId, clientMessageId = clientId, type = type, mediaUri = mediaUri, durationMs = durationMs))
        MessageRetryWorker.enqueue(context)
    }

    private fun persistOutboxMedia(source: String, clientId: String, ext: String): String {
        val dir = File(context.filesDir, "chat_outbox").apply { mkdirs() }
        val target = File(dir, "$clientId.$ext")
        val parsed = Uri.parse(source)
        if (parsed.scheme.isNullOrBlank()) {
            val src = File(source)
            require(src.exists() && src.length() > 0) { "Media file unavailable" }
            src.copyTo(target, overwrite = true)
        } else {
            context.contentResolver.openInputStream(parsed)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Media unavailable")
        }
        require(target.length() > 0) { "Empty media" }
        return target.absolutePath
    }

    suspend fun retryFailed(message: MessageEntity) = withContext(Dispatchers.IO) {
        require(message.id > 0 && message.clientMessageId.isNotBlank()) {
            "Message is not retryable"
        }
        val reset = pendingDao.resetRetry(message.id)
        require(reset > 0) {
            "This failed message is no longer in the durable outbox"
        }
        dao.updateStatus(message.id, "sending")
        MessageRetryWorker.enqueue(context)
    }

    suspend fun markRead(me: Long, peer: Long) = withContext(Dispatchers.IO) {
        dao.markRead(me, peer)
        val myUid = userDao.findById(me)?.firebaseUid.orEmpty()
        val peerUid = userDao.findById(peer)?.firebaseUid.orEmpty()
        if (myUid.isNotBlank() && peerUid.isNotBlank()) runCatching { firestoreChat.markRead(myUid, peerUid) }
    }

    suspend fun startFirestoreSync(me: Long, peer: Long) = withContext(Dispatchers.IO) {
        val myUid = userDao.findById(me)?.firebaseUid.orEmpty()
        val peerUid = userDao.findById(peer)?.firebaseUid.orEmpty()
        if (myUid.isBlank() || peerUid.isBlank()) return@withContext
        runCatching {
            firestoreChat.observeThread(myUid, peerUid).collect { remoteMessages ->
                val deliveryAcks = mutableListOf<String>()
                remoteMessages.forEach { remote ->
                    if (remote.fromFirebaseUid == myUid && remote.toFirebaseUid == peerUid) {
                        val status = when {
                            remote.readAt != null || remote.isRead -> "read"
                            remote.deliveredAt != null -> "delivered"
                            else -> "sent"
                        }
                        dao.updateRemoteStatus(remote.id, status, remote.readAt ?: 0L)
                        return@forEach
                    }

                    if (remote.fromFirebaseUid != peerUid || remote.toFirebaseUid != myUid) return@forEach
                    if (remote.deliveredAt == null) deliveryAcks += remote.id

                    val incomingStatus = if (remote.readAt != null || remote.isRead) "read" else "delivered"
                    if (dao.countByClientMessageId(remote.id) > 0) {
                        dao.updateRemoteStatus(remote.id, incomingStatus, remote.readAt ?: 0L)
                        return@forEach
                    }

                    val voiceLocal = remote.voiceUri?.let { storage.downloadChatMedia(it).getOrNull() }
                    val imageLocal = remote.imageUri?.let { storage.downloadChatMedia(it).getOrNull() }
                    if (remote.voiceUri != null && voiceLocal == null) return@forEach
                    if (remote.imageUri != null && imageLocal == null) return@forEach
                    val localBody = if (remote.voiceUri == null && remote.imageUri == null) {
                        ChatCrypto.encrypt(remote.body) ?: remote.body
                    } else {
                        remote.body
                    }
                    dao.insert(
                        MessageEntity(
                            fromUserId = peer,
                            toUserId = me,
                            body = localBody,
                            voiceUri = voiceLocal,
                            imageUri = imageLocal,
                            voiceDurationMs = remote.voiceDurationMs,
                            sentAt = remote.sentAt,
                            readAt = remote.readAt,
                            clientMessageId = remote.id,
                            status = incomingStatus
                        )
                    )
                }
                if (deliveryAcks.isNotEmpty()) {
                    runCatching { firestoreChat.acknowledgeDelivered(myUid, peerUid, deliveryAcks) }
                }
            }
        }
    }
}
