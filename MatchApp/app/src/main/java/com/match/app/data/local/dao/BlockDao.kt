package com.match.app.data.local.dao

import androidx.room.*
import com.match.app.data.local.entity.BlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun block(e: BlockEntity): Long

    @Query("DELETE FROM blocks WHERE blockerId = :me AND blockedId = :them")
    suspend fun unblock(me: Long, them: Long)

    @Query("SELECT blockedId FROM blocks WHERE blockerId = :me")
    suspend fun blockedIds(me: Long): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM blocks WHERE blockerId = :me AND blockedId = :them)")
    suspend fun isBlocked(me: Long, them: Long): Boolean

    @Query("SELECT blockedId FROM blocks WHERE blockerId = :me")
    fun observeBlockedIds(me: Long): Flow<List<Long>>
}
