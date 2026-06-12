package com.match.app.data.local.dao

import androidx.room.*
import com.match.app.data.local.entity.SavedSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSearchDao {
    @Query("SELECT * FROM saved_searches WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeForUser(userId: Long): Flow<List<SavedSearchEntity>>

    @Query("SELECT * FROM saved_searches WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAllForUser(userId: Long): List<SavedSearchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: SavedSearchEntity): Long

    @Delete
    suspend fun delete(entity: SavedSearchEntity)

    @Query("DELETE FROM saved_searches WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM saved_searches WHERE userId = :userId")
    suspend fun countForUser(userId: Long): Int
}
