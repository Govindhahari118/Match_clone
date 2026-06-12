package com.match.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "profile_views",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["viewerId"],  onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["viewerId", "profileId"]), Index("profileId")]
)
data class ProfileViewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val viewerId: Long,
    val profileId: Long,
    val viewedAt: Long = System.currentTimeMillis()
)
