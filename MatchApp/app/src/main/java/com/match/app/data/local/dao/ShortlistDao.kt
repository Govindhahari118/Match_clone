package com.match.app.data.local.dao

import androidx.room.*
import com.match.app.data.local.entity.ShortlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortlistDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun save(e: ShortlistEntity): Long

    @Query("DELETE FROM shortlists WHERE ownerId = :owner AND targetId = :target")
    suspend fun remove(owner: Long, target: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM shortlists WHERE ownerId = :owner AND targetId = :target)")
    suspend fun isSaved(owner: Long, target: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM shortlists WHERE ownerId = :owner AND targetId = :target)")
    fun observeIsSaved(owner: Long, target: Long): Flow<Boolean>

    @Query("SELECT targetId FROM shortlists WHERE ownerId = :owner ORDER BY savedAt DESC")
    fun observeSavedIds(owner: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM shortlists WHERE ownerId = :owner")
    fun observeCount(owner: Long): Flow<Int>
}
