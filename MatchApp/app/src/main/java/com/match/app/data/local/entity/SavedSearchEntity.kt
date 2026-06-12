package com.match.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists a named MatchFilter preset so users can quickly reapply
 * commonly used search criteria without re-entering every field.
 *
 * Filter fields are stored as individual columns for queryability.
 * Lists (castes, religions) are serialized as comma-separated strings.
 */
@Entity(tableName = "saved_searches")
data class SavedSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),

    // ── Basic filters ──────────────────────────────────────────────────────
    val minAge: Int = 18,
    val maxAge: Int = 50,
    val minHeightCm: Int = 140,
    val maxHeightCm: Int = 200,

    // ── Location ──────────────────────────────────────────────────────────
    val city: String = "",
    val state: String = "",

    // ── Community (comma-separated lists) ─────────────────────────────────
    val religions: String = "",    // e.g. "Hindu,Jain"
    val castes: String = "",
    val motherTongues: String = "",

    // ── Lifestyle ─────────────────────────────────────────────────────────
    val diet: String = "",
    val smoking: String = "",
    val drinking: String = "",
    val incomeBand: String = "",
    val education: String = "",
    val maritalStatus: String = ""
)
