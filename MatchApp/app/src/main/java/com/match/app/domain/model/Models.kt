package com.match.app.domain.model

/** What the user is. */
enum class Gender { MALE, FEMALE, OTHER }

/** What the user wants (can be ANY). */
enum class LookingFor { MALE, FEMALE, ANY }

data class UserProfile(
    val id: Long,
    val firebaseUid: String = "",
    val email: String,
    val displayName: String,
    val age: Int,
    val gender: Gender,
    val lookingFor: LookingFor,
    val city: String,
    val bio: String,
    val rasi: String,
    val nakshatra: String,
    val hasQuestionnaire: Boolean,
    val primaryPhotoPath: String? = null,
    // enrichment
    val religion: String = "Hindu",
    val caste: String = "",
    val motherTongue: String = "Telugu",
    val education: String = "Graduate",
    val profession: String = "Software Engineer",
    val maritalStatus: String = "Never Married",
    val heightCm: Int = 165,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false,
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
    val hobbies: List<String> = emptyList(),
    val spokenLanguages: List<String> = emptyList(),
    val videoUrl: String = "",
    val residentialStatus: String = "",
    val hasChildren: Boolean = false,
    val profileViewCount: Int = 0,
    // Sprint 5-6 additions
    val nativeState: String = "",
    val countryOfResidence: String = "",
    val visaStatus: String = "",
    val willingToRelocate: Boolean = false,
    val createdAt: Long = 0L,
    // Sprint 7 additions
    val lastActiveAt: Long = 0L,
    val phoneNumber: String = "",
    val isIncognito: Boolean = false,
    // Sprint 10 additions — previously discarded family & astrology fields
    val familyValues: String = "",
    val aboutFamily: String = "",
    val manglik: String = "",
    // Sprint 10 — physical & kundali
    val dateOfBirth: String = "",
    val weight: Float = 0f,
    val complexion: String = "",
    val physicalStatus: String = "",
    val birthTime: String = "",
    val birthPlace: String = "",
    // Sprint 10 — family
    val familyStatus: String = "",
    // Sprint 10 — education & career
    val educationField: String = "",
    val institution: String = "",
    val graduationYear: Int = 0,
    val occupationCategory: String = "",
    val employer: String = "",
    val employerType: String = "",
    // Sprint 10 — NRI & citizenship
    val citizenship: String = "",
    val isNRI: Boolean = false,
    // Sprint 10 — lifestyle
    val fitnessActivities: String = "",
    // Sprint 10 — platform
    val matrimonyId: String = "",
    val photoUrl: String = "",
    val voiceBioUrl: String = "",
    // AI Vectors
    val selfVector: FloatArray? = null,
    val partnerVector: FloatArray? = null,
    // Sprint 10 — quality & privacy
    val profileCompleteness: Float = 0f,
    val verificationLevel: Int = 0,
    val stealthMode: Boolean = false,
    val showLastActive: Boolean = true,
    val showHoroscope: Boolean = true,
    val incomeDisclosure: String = "range",
    // Sprint 10 — subscription & matching
    val subscriptionPlan: String = "FREE",
    val subscriptionExpiry: Long = 0L,
    val matchScore: Float = 0f
)

data class MatchResult(
    val user: UserProfile,
    val questionnaireScore: Float,   // 0..1
    val astrologyScore: Float,       // 0..1
    val combinedScore: Float,        // 0..1
    val mode: MatchMode
) {
    val displayScore: Int get() = (primary() * 100f).toInt()
    fun primary(): Float = when (mode) {
        MatchMode.QUESTIONNAIRE -> questionnaireScore
        MatchMode.ASTROLOGY     -> astrologyScore
        MatchMode.ADVANCED      -> combinedScore
    }
}

enum class MatchMode { QUESTIONNAIRE, ASTROLOGY, ADVANCED }

data class MatchFilter(
    val ageMin: Int = 18,
    val ageMax: Int = 70,
    val city: String = "",
    val state: String = "",
    val caste: String = "",
    val minScore: Float = 0f,
    val religion: String = "",
    val motherTongue: String = "",
    val maritalStatus: String = "",
    val verifiedOnly: Boolean = false,
    // Phase-1 additions
    val incomeMin: String = "",
    val incomeMax: String = "",
    val educationLevel: String = "",
    val diet: String = "",
    val residentialStatus: String = "",
    val hasChildren: String = "",
    val keyword: String = "",
    val gothra: String = "",
    // Sprint 5: Family origin
    val nativeState: String = "",
    // Sprint 6: NRI filters
    val countryOfResidence: String = "",
    val nriOnly: Boolean = false,
    val willingToRelocate: Boolean = false,
    // Sprint 6: Recently joined
    val recentlyJoinedDays: Int = 0,  // 0 = all, 7 = last 7 days, 30 = last 30 days
    // Sprint 10+: Extended 35-filter system
    val smoking: String = "",          // Never / Occasionally / Regularly / Don't mind
    val drinking: String = "",
    val familyType: String = "",       // Joint / Nuclear / Either
    val familyStatus: String = "",     // Middle / Upper-middle / Affluent / Rich
    val physicalStatus: String = "",   // Normal / Differently Abled
    val hasChildrenFilter: String = "", // No / Yes-together / Yes-apart / Don't mind
    val citizenship: String = "",
    val nriStatus: String = "",        // include / only / exclude
    val educationField: String = "",
    val occupationCategory: String = "",
    val employerType: String = "",
    val subCaste: String = "",
    val nakshatra: String = "",
    val rasi: String = "",
    val manglik: String = "",          // Any / Manglik / Non-manglik
    val hobbies: String = "",
    val withPhotoOnly: Boolean = true,
    val verifiedLevel: Int = 0,        // min verificationLevel
    val premiumOnly: Boolean = false,
    val lastActiveWithinDays: Int = 0, // 0 = any, 1 = today, 7 = week, 30 = month
    val minPoruthamScore: Int = 0,     // 0–10
    val hasHoroscope: String = ""      // Any / Yes / No
)
