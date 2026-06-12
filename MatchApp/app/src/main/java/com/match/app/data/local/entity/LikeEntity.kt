package com.match.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A "like" / connection-request from [fromUserId] → [toUserId]. */
@Entity(
    tableName = "likes",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["fromUserId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["toUserId"],   onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["fromUserId", "toUserId"], unique = true), Index("toUserId")]
)
data class LikeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromUserId: Long,
    val toUserId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    /** true = "Super Interest" — premium feature that stands out in the recipient's inbox */
    val isSuperLike: Boolean = false
)
