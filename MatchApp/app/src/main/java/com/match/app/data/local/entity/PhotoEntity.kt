package com.match.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    /** Absolute file path in app-internal storage. */
    val path: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isPrimary: Boolean = false,
    /** Privacy: PUBLIC | ACCEPTED_ONLY | HIDDEN */
    val privacy: String = "PUBLIC"
)
