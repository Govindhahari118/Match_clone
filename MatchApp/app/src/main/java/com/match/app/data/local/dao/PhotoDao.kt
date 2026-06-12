package com.match.app.data.local.dao

import androidx.room.*
import com.match.app.data.local.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(p: PhotoEntity): Long

    @Query("SELECT * FROM photos WHERE userId = :userId ORDER BY isPrimary DESC, createdAt DESC")
    fun observeForUser(userId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE userId = :userId ORDER BY isPrimary DESC, createdAt DESC LIMIT 1")
    suspend fun primaryFor(userId: Long): PhotoEntity?

    @Query("UPDATE photos SET isPrimary = CASE WHEN id = :photoId THEN 1 ELSE 0 END WHERE userId = :userId")
    suspend fun setPrimary(userId: Long, photoId: Long)

    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun delete(photoId: Long)

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun byId(id: Long): PhotoEntity?

    @Query("UPDATE photos SET privacy = :privacy WHERE id = :photoId")
    suspend fun setPrivacy(photoId: Long, privacy: String)
}
