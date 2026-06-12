package com.match.app.data.local.dao

import androidx.room.*
import com.match.app.data.local.entity.ProfileViewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileViewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun record(e: ProfileViewEntity): Long

    @Query("SELECT viewerId FROM profile_views WHERE profileId = :me ORDER BY viewedAt DESC LIMIT 50")
    fun observeViewerIds(me: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM profile_views WHERE profileId = :me")
    fun observeViewCount(me: Long): Flow<Int>
}
