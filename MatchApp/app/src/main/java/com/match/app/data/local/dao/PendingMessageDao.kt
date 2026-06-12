package com.match.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.match.app.data.local.entity.PendingMessageEntity

@Dao
interface PendingMessageDao {
    @Insert
    suspend fun insert(msg: PendingMessageEntity)

    @Query("SELECT * FROM pending_messages ORDER BY createdAt ASC LIMIT 50")
    suspend fun oldest(): List<PendingMessageEntity>

    @Query("DELETE FROM pending_messages WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE pending_messages SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)

    @Query("DELETE FROM pending_messages WHERE retryCount >= 10")
    suspend fun pruneStale()

    @Query("SELECT COUNT(*) FROM pending_messages")
    suspend fun count(): Int
}
