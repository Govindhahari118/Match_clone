package com.match.app.data.local.dao

import androidx.room.*
import com.match.app.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(n: NotificationEntity): Long

    @Query("SELECT * FROM notifications WHERE userId = :uid ORDER BY createdAt DESC")
    fun observe(uid: Long): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :uid AND isRead = 0")
    fun observeUnreadCount(uid: Long): Flow<Int>

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :uid")
    suspend fun markAllRead(uid: Long)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)
}
