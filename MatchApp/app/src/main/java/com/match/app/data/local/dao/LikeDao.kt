package com.match.app.data.local.dao

import androidx.room.*
import com.match.app.data.local.entity.LikeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LikeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun like(e: LikeEntity): Long

    @Query("DELETE FROM likes WHERE fromUserId = :from AND toUserId = :to")
    suspend fun unlike(from: Long, to: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM likes WHERE fromUserId = :from AND toUserId = :to)")
    suspend fun isLiked(from: Long, to: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM likes WHERE fromUserId = :from AND toUserId = :to)")
    fun observeIsLiked(from: Long, to: Long): Flow<Boolean>

    /** Mutual likes = a match. */
    @Query("""
        SELECT * FROM likes WHERE fromUserId = :me
        AND toUserId IN (SELECT fromUserId FROM likes WHERE toUserId = :me)
    """)
    fun observeMutualLikes(me: Long): Flow<List<LikeEntity>>

    /** IDs that liked me but I haven't liked back. */
    @Query("""
        SELECT fromUserId FROM likes WHERE toUserId = :me
        AND fromUserId NOT IN (SELECT toUserId FROM likes WHERE fromUserId = :me)
    """)
    fun observeLikedByMe_notYet(me: Long): Flow<List<Long>>

    /** Count of people who liked me (my "requests"). */
    @Query("SELECT COUNT(*) FROM likes WHERE toUserId = :me")
    fun observeIncomingCount(me: Long): Flow<Int>

    /** One-shot count of people who liked me. */
    @Query("SELECT COUNT(*) FROM likes WHERE toUserId = :me")
    suspend fun observeIncomingCountOnce(me: Long): Int

    /** People who liked me (received interests). */
    @Query("SELECT fromUserId FROM likes WHERE toUserId = :me")
    fun observeReceivedInterests(me: Long): Flow<List<Long>>

    /** People I liked (sent interests). */
    @Query("SELECT toUserId FROM likes WHERE fromUserId = :me")
    fun observeSentInterests(me: Long): Flow<List<Long>>

    /** Check if this interest was sent as a Super Interest. */
    @Query("SELECT isSuperLike FROM likes WHERE fromUserId = :from AND toUserId = :to LIMIT 1")
    suspend fun isSuperLike(from: Long, to: Long): Boolean

    /** All super interests received by [me]. */
    @Query("SELECT fromUserId FROM likes WHERE toUserId = :me AND isSuperLike = 1")
    fun observeSuperInterests(me: Long): Flow<List<Long>>
}
