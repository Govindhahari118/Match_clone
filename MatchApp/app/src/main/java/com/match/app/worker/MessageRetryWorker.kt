package com.match.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.match.app.data.local.dao.MessageDao
import com.match.app.data.local.dao.PendingMessageDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.remote.FirebaseStorageService
import com.match.app.data.remote.FirestoreChatService
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
        pendingDao.pruneStale()
        val pending = pendingDao.oldest()
        if (pending.isEmpty()) return Result.success()
        for (msg in pending) {
            try {
                val senderUid = userDao.findById(msg.fromUserId)?.firebaseUid?.takeIf { it.isNotBlank() } ?: error("Sender UID missing")
                val recipientUid = userDao.findById(msg.toUserId)?.firebaseUid?.takeIf { it.isNotBlank() } ?: error("Recipient UID missing")
                val threadId = FirestoreChatService.threadId(senderUid, recipientUid)
                var voicePath: String? = null
                var imagePath: String? = null
                when (msg.type) {
                    "IMAGE" -> imagePath = storage.uploadChatImage(threadId, senderUid, recipientUid, msg.clientMessageId, msg.mediaUri).getOrThrow()
                    "VOICE" -> voicePath = storage.uploadChatVoice(threadId, senderUid, recipientUid, msg.clientMessageId, msg.mediaUri).getOrThrow()
                }
                firestoreChat.sendMessage(msg.clientMessageId, msg.body, senderUid, recipientUid, voicePath, imagePath, msg.durationMs.takeIf { it > 0 })
                if (msg.localMessageId > 0) messageDao.updateStatus(msg.localMessageId, "sent")
                pendingDao.delete(msg.id)
            } catch (_: Exception) {
                pendingDao.incrementRetry(msg.id)
                if (msg.localMessageId > 0) messageDao.updateStatus(msg.localMessageId, "failed")
            }
        }
        return if (pendingDao.count() > 0) Result.retry() else Result.success()
    }

    companion object {
        private const val UNIQUE_WORK = "chat_outbox_retry"
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<MessageRetryWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.KEEP, request)
        }
    }
}
