package com.match.app.data.local.dao

import androidx.room.*
import com.match.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Query("SELECT * FROM profile_notes WHERE ownerId = :me AND targetId = :target LIMIT 1")
    fun observe(me: Long, target: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM profile_notes WHERE ownerId = :me AND targetId = :target LIMIT 1")
    suspend fun get(me: Long, target: Long): NoteEntity?

    @Query("DELETE FROM profile_notes WHERE ownerId = :me AND targetId = :target")
    suspend fun delete(me: Long, target: Long)
}
