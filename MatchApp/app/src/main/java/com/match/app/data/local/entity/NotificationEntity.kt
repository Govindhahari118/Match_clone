package com.match.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [Index("userId")]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,             // recipient
    val type: String,             // LIKE | INTEREST | MATCH | VIEW | SYSTEM
    val fromUserId: Long? = null,
    val title: String,
    val body: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
