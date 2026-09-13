package com.match.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromUserId: Long,
    val toUserId: Long,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val localMessageId: Long = 0,
    val clientMessageId: String = "",
    val type: String = "TEXT",
    val mediaUri: String = "",
    val durationMs: Long = 0
)
