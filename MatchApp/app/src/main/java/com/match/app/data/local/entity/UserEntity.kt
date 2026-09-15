package com.match.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true), Index(value = ["firebaseUid"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Firebase Auth UID — the canonical user identity used in Firestore. */
    val firebaseUid: String = "",
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val age: Int,
    val gender: String,          // MALE / FEMALE / OTHER
    val lookingFor: String,      // MALE / FEMALE / ANY
    val city: String,
    val bio: String,
    val rasi: String,            // e.g. "Cancer"
    val nakshatra: String,       // e.g. "Pushya"
    val isSeed: Boolean = false,
    // ── New enrichment fields ──────────────────────────────
    val religion: String = "Hindu",
    val motherTongue: String = "Telugu",
    val education: String = "Graduate",
    val profession: String = "Software Engineer",
    val maritalStatus: String = "Never Married",
    val heightCm: Int = 165,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false,
    val isShortlisted: Boolean = false,
    val profileViewCount: Int = 0,
    val caste: String = "",
    val state: String = "",
    // Phase-1 additions
    val subCaste: String = "",
    val gothra: String = "",
    val incomeBand: String = "",
    val diet: String = "",
    val familyType: String = "",
    val fatherOccupation: String = "",
    val motherOccupation: String = "",
    val siblings: Int = 0,
    val smoking: String = "",
    val drinking: String = "",
    val personalityType: String = "",
    val hobbies: String = "",
    val spokenLanguages: String = "",
    val videoUrl: String = "",
    val residentialStatus: String = "",
    val hasChildren: Boolean = false,
    // Boost feature
    val boostActiveUntil: Long = 0L,
    // Sprint 5: Family origin filter
    val nativeState: String = "",
    // Sprint 6: NRI filters
    val countryOfResidence: String = "",
    val visaStatus: String = "",
    val willingToRelocate: Boolean = false,
    // Sprint 6: Recently joined badge
    val createdAt: Long = System.currentTimeMillis(),
    // Sprint 7: Activity status
    val lastActiveAt: Long = 0L,
    // Sprint 7: Incognito browse mode
    val isIncognito: Boolean = false,
    // Sprint 7: Contact reveal
    val phoneNumber: String = "",
    // Sprint 9: Discovery 2.0
    val ageBucket: String = "",
    // Sprint 10: Family & astrology completeness
    val familyValues: String = "",
    val aboutFamily: String = "",
    val manglik: String = "",
    // Physical details
    val dateOfBirth: String = "",
    val weight: Float = 0f,
    val complexion: String = "",
    val physicalStatus: String = "",
    // Kundali essentials
    val birthTime: String = "",
    val birthPlace: String = "",
    // Family economic status
    val familyStatus: String = "",
    // Education detail
    val educationField: String = "",
    val institution: String = "",
    val graduationYear: Int = 0,
    // Occupation detail
    val occupationCategory: String = "",
    val employer: String = "",
    val employerType: String = "",
    // Citizenship & NRI
    val citizenship: String = "",
    val isNRI: Boolean = false,
    // Lifestyle detail
    val fitnessActivities: String = "",
    // Platform identity
    val matrimonyId: String = "",
    val photoUrl: String = "",
    val voiceBioUrl: String = "",
    // Profile quality
    val profileCompleteness: Float = 0f,
    val verificationLevel: Int = 0,
    // Privacy settings
    val stealthMode: Boolean = false,
    val showLastActive: Boolean = true,
    val showHoroscope: Boolean = true,
    val incomeDisclosure: String = "range",
    // Subscription
    val subscriptionPlan: String = "FREE",
    val subscriptionExpiry: Long = 0L,
    // Match scoring
    val matchScore: Float = 0f,
    /** Unique public handle reserved by the backend, e.g. avinash_21. */
    val username: String = ""
)
