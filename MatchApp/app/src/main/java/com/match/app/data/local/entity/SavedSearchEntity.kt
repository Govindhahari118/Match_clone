package com.match.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Complete named MatchFilter snapshot owned by one local signed-in account. */
@Entity(tableName = "saved_searches")
data class SavedSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),

    val minAge: Int = 18,
    val maxAge: Int = 70,
    // Retained for schema compatibility and future height filtering.
    val minHeightCm: Int = 140,
    val maxHeightCm: Int = 200,
    val city: String = "",
    val state: String = "",
    val religions: String = "",
    val castes: String = "",
    val motherTongues: String = "",
    val diet: String = "",
    val smoking: String = "",
    val drinking: String = "",
    val incomeBand: String = "",
    val education: String = "",
    val maritalStatus: String = "",

    val subCaste: String = "",
    val minScore: Float = 0f,
    val verifiedOnly: Boolean = false,
    val incomeMax: String = "",
    val residentialStatus: String = "",
    val hasChildren: String = "",
    val keyword: String = "",
    val gothra: String = "",
    val nativeState: String = "",
    val countryOfResidence: String = "",
    val nriOnly: Boolean = false,
    val willingToRelocate: Boolean = false,
    val recentlyJoinedDays: Int = 0,
    val familyType: String = "",
    val familyStatus: String = "",
    val physicalStatus: String = "",
    val hasChildrenFilter: String = "",
    val citizenship: String = "",
    val nriStatus: String = "",
    val educationField: String = "",
    val occupationCategory: String = "",
    val employerType: String = "",
    val nakshatra: String = "",
    val rasi: String = "",
    val manglik: String = "",
    val hobbies: String = "",
    val withPhotoOnly: Boolean = true,
    val verifiedLevel: Int = 0,
    val premiumOnly: Boolean = false,
    val lastActiveWithinDays: Int = 0,
    val minPoruthamScore: Int = 0,
    val hasHoroscope: String = ""
)
