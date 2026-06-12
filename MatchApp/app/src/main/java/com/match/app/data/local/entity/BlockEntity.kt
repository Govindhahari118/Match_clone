package com.match.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocks",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["blockerId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["blockedId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["blockerId", "blockedId"], unique = true), Index("blockedId")]
)
data class BlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blockerId: Long,
    val blockedId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
