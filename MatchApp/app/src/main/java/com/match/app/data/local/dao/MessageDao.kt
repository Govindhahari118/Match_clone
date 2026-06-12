package com.match.app.data.local.dao

import androidx.room.*
import com.match.app.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(m: MessageEntity): Long

    /** Full thread between two users, chronological. */
    @Query("""
        SELECT * FROM messages
        WHERE (fromUserId = :a AND toUserId = :b) OR (fromUserId = :b AND toUserId = :a)
        ORDER BY sentAt ASC
    """)
    fun observeThread(a: Long, b: Long): Flow<List<MessageEntity>>

    @Query("UPDATE messages SET readAt = :now WHERE toUserId = :me AND fromUserId = :peer AND readAt IS NULL")
    suspend fun markRead(me: Long, peer: Long, now: Long = System.currentTimeMillis())

    @Query("""
        SELECT COUNT(*) FROM messages WHERE toUserId = :me AND readAt IS NULL
    """)
    fun observeUnread(me: Long): Flow<Int>

    /** All user IDs I have had a conversation with (deduplicated). */
    @Query("""
        SELECT DISTINCT CASE WHEN fromUserId = :me THEN toUserId ELSE fromUserId END AS peerId
        FROM messages
        WHERE fromUserId = :me OR toUserId = :me
    """)
    fun observeConversationPeerIds(me: Long): Flow<List<Long>>

    /** Most recent message in a thread between two users. */
    @Query("""
        SELECT * FROM messages
        WHERE (fromUserId = :a AND toUserId = :b) OR (fromUserId = :b AND toUserId = :a)
        ORDER BY sentAt DESC LIMIT 1
    """)
    suspend fun lastMessage(a: Long, b: Long): MessageEntity?

    /** Count of messages sent by [me] to [peer] — used to enforce free message limits. */
    @Query("SELECT COUNT(*) FROM messages WHERE fromUserId = :me AND toUserId = :peer")
    suspend fun countSentMessages(me: Long, peer: Long): Int

    /** Check if a message from Firestore (identified by sender + timestamp) already exists. */
    @Query("SELECT COUNT(*) FROM messages WHERE fromUserId = :from AND toUserId = :to AND sentAt = :sentAt")
    suspend fun countBySentAt(from: Long, to: Long, sentAt: Long): Int
}
