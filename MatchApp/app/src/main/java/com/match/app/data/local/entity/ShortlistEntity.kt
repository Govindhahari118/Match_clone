package com.match.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shortlists",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["ownerId"],  onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["targetId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["ownerId", "targetId"], unique = true), Index("targetId")]
)
data class ShortlistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: Long,
    val targetId: Long,
    val savedAt: Long = System.currentTimeMillis()
)
