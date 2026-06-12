package com.match.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per user holding both their self-vector and the partner-vector,
 * encoded from the canonical 15-question template.
 */
@Entity(
    tableName = "questionnaire",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"], unique = true)]
)
data class QuestionnaireEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    /** Comma-separated floats (length = Questionnaire.VECTOR_LENGTH). */
    val selfVector: String,
    val partnerVector: String
)
