package com.match.app.data.local.dao

import androidx.room.*
import com.match.app.data.local.entity.QuestionnaireEntity

@Dao
interface QuestionnaireDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(q: QuestionnaireEntity): Long

    @Query("SELECT * FROM questionnaire WHERE userId = :userId LIMIT 1")
    suspend fun forUser(userId: Long): QuestionnaireEntity?

    @Query("SELECT * FROM questionnaire")
    suspend fun all(): List<QuestionnaireEntity>
}
