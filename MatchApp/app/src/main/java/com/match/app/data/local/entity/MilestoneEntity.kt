package com.match.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timeline_milestones",
    indices = [Index("userId")]
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val type: String,      // PROFILE_CREATED, VERIFIED, PHOTO_UPLOAD, MATCH, CHAT, CALL, MEET, ENGAGED, WEDDING
    val title: String,
    val description: String,
    val date: String,      // Display date string
    val isDone: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
