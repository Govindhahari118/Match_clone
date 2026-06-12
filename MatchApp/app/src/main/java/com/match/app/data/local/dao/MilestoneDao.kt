package com.match.app.data.local.dao

import androidx.room.*
import com.match.app.data.local.entity.MilestoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(milestone: MilestoneEntity): Long

    @Query("SELECT * FROM timeline_milestones WHERE userId = :uid ORDER BY createdAt ASC")
    fun observeForUser(uid: Long): Flow<List<MilestoneEntity>>

    @Query("SELECT COUNT(*) FROM timeline_milestones WHERE userId = :uid AND type = :type")
    suspend fun countByType(uid: Long, type: String): Int

    @Query("DELETE FROM timeline_milestones WHERE userId = :uid")
    suspend fun clearForUser(uid: Long)
}
