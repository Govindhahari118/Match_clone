package com.match.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.match.app.data.local.dao.MessageDao
import com.match.app.data.local.dao.PendingMessageDao
import com.match.app.data.local.entity.MessageEntity
import com.match.app.security.ChatCrypto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Retries sending queued messages from [pending_messages] table.
 * Enqueued by [ChatRepository] when a send fails.
 */
@HiltWorker
class MessageRetryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingDao: PendingMessageDao,
    private val messageDao: MessageDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        pendingDao.pruneStale()
        val pending = pendingDao.oldest()
        if (pending.isEmpty()) return Result.success()

        for (msg in pending) {
            try {
                val encrypted = ChatCrypto.encrypt(msg.body) ?: msg.body
                messageDao.insert(
                    MessageEntity(
                        fromUserId = msg.fromUserId,
                        toUserId = msg.toUserId,
                        body = encrypted.take(4096)
                    )
                )
                pendingDao.delete(msg.id)
            } catch (_: Exception) {
                pendingDao.incrementRetry(msg.id)
            }
        }

        // If there are still pending messages, retry later
        return if (pendingDao.count() > 0) Result.retry() else Result.success()
    }
}
