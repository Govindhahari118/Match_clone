package com.match.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.match.app.data.local.entity.PendingMessageEntity

@Dao
interface PendingMessageDao {
    @Insert
    suspend fun insert(msg: PendingMessageEntity)

    @Query("SELECT * FROM pending_messages WHERE retryCount < :maxRetries ORDER BY createdAt ASC LIMIT 50")
    suspend fun oldestAutomatic(maxRetries: Int): List<PendingMessageEntity>

    @Query("DELETE FROM pending_messages WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE pending_messages SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)

    @Query("UPDATE pending_messages SET retryCount = 0 WHERE localMessageId = :localMessageId")
    suspend fun resetRetry(localMessageId: Long): Int

    @Query("SELECT COUNT(*) FROM pending_messages WHERE retryCount < :maxRetries")
    suspend fun countAutomatic(maxRetries: Int): Int

    @Query("SELECT COUNT(*) FROM pending_messages")
    suspend fun count(): Int
}
