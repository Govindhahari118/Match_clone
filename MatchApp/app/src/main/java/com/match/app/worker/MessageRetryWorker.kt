package com.match.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.match.app.data.local.dao.MessageDao
import com.match.app.data.local.dao.PendingMessageDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.PendingMessageEntity
import com.match.app.data.remote.FirebaseStorageService
import com.match.app.data.repo.ChatOutboxMediaPolicy
import com.match.app.data.remote.FirestoreChatService
import com.match.app.security.ChatCrypto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class MessageRetryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingDao: PendingMessageDao,
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val firestoreChat: FirestoreChatService,
    private val storage: FirebaseStorageService
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val pending = pendingDao.oldestAutomatic(MAX_AUTO_RETRIES)
        // Rows that exhaust automatic retries remain persisted so the user can explicitly retry
        // the same stable clientMessageId later instead of silently losing the operation.
        if (pending.isEmpty()) return Result.success()
        for (msg in pending) {
            try {
                val senderUid = userDao.findById(msg.fromUserId)?.firebaseUid?.takeIf { it.isNotBlank() } ?: error("Sender UID missing")
                val recipientUid = userDao.findById(msg.toUserId)?.firebaseUid?.takeIf { it.isNotBlank() } ?: error("Recipient UID missing")
                val threadId = if (msg.type == "IMAGE" || msg.type == "VOICE") {
                    firestoreChat.prepareThread(senderUid, recipientUid)
                } else {
                    FirestoreChatService.threadId(senderUid, recipientUid)
                }
                val plaintextBody = plaintextForRetry(msg)
                var voicePath: String? = null
                var imagePath: String? = null
                when (msg.type) {
                    "IMAGE" -> imagePath = storage.uploadChatImage(threadId, senderUid, recipientUid, msg.clientMessageId, msg.mediaUri).getOrThrow()
                    "VOICE" -> voicePath = storage.uploadChatVoice(threadId, senderUid, recipientUid, msg.clientMessageId, msg.mediaUri).getOrThrow()
                }
                firestoreChat.sendMessage(msg.clientMessageId, plaintextBody, senderUid, recipientUid, voicePath, imagePath, msg.durationMs.takeIf { it > 0 })
                if (msg.localMessageId > 0) messageDao.updateStatus(msg.localMessageId, "sent")
                pendingDao.delete(msg.id)
                if (msg.type == "IMAGE" || msg.type == "VOICE") {
                    ChatOutboxMediaPolicy.deleteIfManaged(applicationContext.filesDir, msg.mediaUri)
                }
            } catch (_: Exception) {
                pendingDao.incrementRetry(msg.id)
                if (msg.localMessageId > 0) messageDao.updateStatus(msg.localMessageId, "failed")
            }
        }
        return if (pendingDao.countAutomatic(MAX_AUTO_RETRIES) > 0) Result.retry() else Result.success()
    }

    /**
     * New pending rows are versioned ciphertext. Rows created by older releases stored plaintext;
     * migrate those in-place before retrying so durable retries no longer retain message text in
     * clear. Historical unprefixed ciphertext is also normalized to the versioned format.
     */
    private suspend fun plaintextForRetry(msg: PendingMessageEntity): String {
        ChatCrypto.decryptFromStorage(msg.body)?.let { decrypted ->
            if (!ChatCrypto.isVersionedStorage(msg.body)) {
                val normalized = ChatCrypto.encryptForStorage(decrypted)
                    ?: error("Secure local message storage is unavailable")
                pendingDao.updateBody(msg.id, normalized)
            }
            return decrypted
        }

        if (!ChatCrypto.isVersionedStorage(msg.body)) {
            val legacyPlaintext = msg.body
            val migrated = ChatCrypto.encryptForStorage(legacyPlaintext)
                ?: error("Secure local message storage is unavailable")
            pendingDao.updateBody(msg.id, migrated)
            return legacyPlaintext
        }

        error("Pending message body cannot be decrypted")
    }

    companion object {
        private const val UNIQUE_WORK = "chat_outbox_retry"
        private const val MAX_AUTO_RETRIES = 10
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<MessageRetryWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.KEEP, request)
        }
    }
}
