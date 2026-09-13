package com.match.app.data.local.dao

import androidx.room.*
import com.match.app.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert suspend fun insert(m: MessageEntity): Long
    @Query("SELECT * FROM messages WHERE (fromUserId = :a AND toUserId = :b) OR (fromUserId = :b AND toUserId = :a) ORDER BY sentAt ASC")
    fun observeThread(a: Long, b: Long): Flow<List<MessageEntity>>
    @Query("UPDATE messages SET readAt = :now WHERE toUserId = :me AND fromUserId = :peer AND readAt IS NULL")
    suspend fun markRead(me: Long, peer: Long, now: Long = System.currentTimeMillis())
    @Query("UPDATE messages SET status = :status WHERE id = :id") suspend fun updateStatus(id: Long, status: String)
    @Query("SELECT COUNT(*) FROM messages WHERE toUserId = :me AND readAt IS NULL") fun observeUnread(me: Long): Flow<Int>
    @Query("SELECT DISTINCT CASE WHEN fromUserId = :me THEN toUserId ELSE fromUserId END AS peerId FROM messages WHERE fromUserId = :me OR toUserId = :me")
    fun observeConversationPeerIds(me: Long): Flow<List<Long>>
    @Query("SELECT * FROM messages WHERE (fromUserId = :a AND toUserId = :b) OR (fromUserId = :b AND toUserId = :a) ORDER BY sentAt DESC LIMIT 1")
    suspend fun lastMessage(a: Long, b: Long): MessageEntity?
    @Query("SELECT COUNT(*) FROM messages WHERE fromUserId = :me AND toUserId = :peer") suspend fun countSentMessages(me: Long, peer: Long): Int
    @Query("SELECT COUNT(*) FROM messages WHERE fromUserId = :from AND toUserId = :to AND sentAt = :sentAt") suspend fun countBySentAt(from: Long, to: Long, sentAt: Long): Int
}
