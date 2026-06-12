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
    val boostActiveUntil: Long = 0L,   // epoch millis; 0 = not boosted
    // Sprint 5: Family origin filter (8,405 upvotes on Shaadi.com)
    val nativeState: String = "",      // e.g. "Andhra Pradesh", "Tamil Nadu"
    // Sprint 6: NRI filters
    val countryOfResidence: String = "",  // e.g. "USA", "UK", "UAE"
    val visaStatus: String = "",          // e.g. "Citizen", "PR", "Work Permit"
    val willingToRelocate: Boolean = false,
    // Sprint 6: Recently joined badge
    val createdAt: Long = System.currentTimeMillis(),  // epoch millis
    // Sprint 7: Activity status ("Active today" etc.)
    val lastActiveAt: Long = 0L,
    // Sprint 7: Incognito browse mode
    val isIncognito: Boolean = false,
    // Sprint 7: Contact reveal (phone visible to premium matches)
    val phoneNumber: String = "",
    // Sprint 9: Discovery 2.0 — discrete age bucket for index-friendly Firestore queries.
    // Format: "18-22", "23-27", "28-32", ... See Migrations.MIGRATION_14_15.
    val ageBucket: String = "",
    // Sprint 10: Family & astrology completeness fields previously silently discarded.
    val familyValues: String = "",     // e.g. "Traditional", "Moderate", "Liberal"
    val aboutFamily: String = "",      // free-text describing family background (500 chars max)
    val manglik: String = "",          // "Yes", "No", "Partial (Anshik)", ""

    // ── Sprint 10: Complete profile schema (Migration 16→17) ──────────────
    // Physical details
    val dateOfBirth: String = "",      // ISO format "1995-03-15"
    val weight: Float = 0f,            // kg
    val complexion: String = "",       // "Very Fair", "Fair", "Wheatish", "Dark"
    val physicalStatus: String = "",   // "Normal", "Physically Challenged"
    // Kundali essentials
    val birthTime: String = "",        // "14:30" (24h format for natal chart)
    val birthPlace: String = "",       // City of birth for Kundali
    // Family economic status
    val familyStatus: String = "",     // "Middle Class", "Upper Middle Class", "Affluent"
    // Education detail
    val educationField: String = "",   // "Computer Science", "Medicine", "Law"
    val institution: String = "",      // college/university name
    val graduationYear: Int = 0,       // e.g. 2018
    // Occupation detail
    val occupationCategory: String = "", // "Private Sector", "Government", "Business", "Defence"
    val employer: String = "",           // company name
    val employerType: String = "",       // "MNC", "Startup", "Government", "PSU"
    // Citizenship & NRI
    val citizenship: String = "",      // "Indian", "American", "British"
    val isNRI: Boolean = false,        // derived flag for NRI filtering
    // Lifestyle detail
    val fitnessActivities: String = "", // comma-separated: "Gym,Yoga,Running"
    // Platform identity
    val matrimonyId: String = "",      // auto-generated "TLG-XXXXX"
    val photoUrl: String = "",         // primary profile photo URL
    val voiceBioUrl: String = "",      // voice introduction recording URL
    // Profile quality
    val profileCompleteness: Float = 0f, // 0.0 to 1.0 calculated
    val verificationLevel: Int = 0,      // 0=none, 1=phone, 2=ID, 3=photo, 4=employment, 5=full
    // Privacy settings
    val stealthMode: Boolean = false,    // shadow profile — views leave no trace
    val showLastActive: Boolean = true,  // show "Active 2h ago" to others
    val showHoroscope: Boolean = true,   // show rasi/nakshatra publicly
    val incomeDisclosure: String = "range", // "exact", "range", "hidden"
    // Subscription
    val subscriptionPlan: String = "FREE", // "FREE", "STANDARD", "PREMIUM", "PLATINUM"
    val subscriptionExpiry: Long = 0L,     // epoch millis; 0 = no active subscription
    // Match scoring
    val matchScore: Float = 0f           // computed compatibility score (0.0–1.0)
)
