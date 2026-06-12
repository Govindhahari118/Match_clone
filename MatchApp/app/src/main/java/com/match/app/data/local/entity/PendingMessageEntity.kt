package com.match.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Queue for messages that failed to send (e.g. no network).
 * [MessageRetryWorker] picks these up and retries.
 */
@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromUserId: Long,
    val toUserId: Long,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
