package com.match.app.domain.model

/** What the user is. */
enum class Gender { MALE, FEMALE, OTHER }

/** What the user wants (can be ANY). */
enum class LookingFor { MALE, FEMALE, ANY }

enum class ProfileCreatedFor(val displayLabel: String) {
    SELF("Self-managed"),
    SON("Managed by family for son"),
    DAUGHTER("Managed by family for daughter"),
    SIBLING("Managed by sibling"),
    PARENT("Managed by family for parent"),
    RELATIVE("Managed by another family member");

    companion object {
        fun fromWire(value: String): ProfileCreatedFor =
            entries.firstOrNull { it.name == value } ?: SELF
    }
}

data class UserProfile(
    val id: Long,
    val firebaseUid: String = "",
    val email: String,
    val displayName: String,
    val age: Int,
    val gender: Gender,
    val lookingFor: LookingFor,
    val profileCreatedFor: ProfileCreatedFor = ProfileCreatedFor.SELF,
    val city: String,
    val bio: String,
    val rasi: String,
    val nakshatra: String,
    val hasQuestionnaire: Boolean,
    val primaryPhotoPath: String? = null,
    val religion: String = "",
    val caste: String = "",
    val motherTongue: String = "",
    val education: String = "",
    val profession: String = "",
    val maritalStatus: String = "",
    val heightCm: Int = 0,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false,
    val state: String = "",
    val subCaste: String = "",
    val gothra: String = "",
    val faithTradition: String = "",
    val faithSubTradition: String = "",
    val faithInstitution: String = "",
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
    val nativeState: String = "",
    val countryOfResidence: String = "",
    val visaStatus: String = "",
    val willingToRelocate: Boolean = false,
    val createdAt: Long = 0L,
    val lastActiveAt: Long = 0L,
    val phoneNumber: String = "",
    val isIncognito: Boolean = false,
    val familyValues: String = "",
    val aboutFamily: String = "",
    val manglik: String = "",
    val dateOfBirth: String = "",
    val weight: Float = 0f,
    val complexion: String = "",
    val physicalStatus: String = "",
    val birthTime: String = "",
    val birthPlace: String = "",
    val familyStatus: String = "",
    val educationField: String = "",
    val institution: String = "",
    val graduationYear: Int = 0,
    val occupationCategory: String = "",
    val employer: String = "",
    val employerType: String = "",
    val citizenship: String = "",
    val isNRI: Boolean = false,
    val fitnessActivities: String = "",
    val matrimonyId: String = "",
    val photoUrl: String = "",
    val voiceBioUrl: String = "",
    val selfVector: FloatArray? = null,
    val partnerVector: FloatArray? = null,
    val profileCompleteness: Float = 0f,
    val verificationLevel: Int = 0,
    val stealthMode: Boolean = false,
    val showLastActive: Boolean = true,
    val showHoroscope: Boolean = true,
    val incomeDisclosure: String = "range",
    val subscriptionPlan: String = "FREE",
    val subscriptionExpiry: Long = 0L,
    val matchScore: Float = 0f,
    val username: String = ""
)

data class CompatibilityFactor(
    val key: String,
    val score: Float,
    val configuredWeight: Float
)

data class MatchResult(
    val user: UserProfile,
    val questionnaireScore: Float,
    val astrologyScore: Float,
    val combinedScore: Float,
    val mode: MatchMode,
    val formulaVersion: String = "",
    val factors: List<CompatibilityFactor> = emptyList(),
    val forwardPreferenceScore: Float? = null,
    val reversePreferenceScore: Float? = null,
    val mutualPreferenceScore: Float? = null,
    val forwardPreferenceCriteria: Int = 0,
    val reversePreferenceCriteria: Int = 0,
    val mutualPreferenceCriteria: Int = 0
) {
    val displayScore: Int get() = (primary() * 100f).toInt()
    fun primary(): Float = when (mode) {
        MatchMode.QUESTIONNAIRE -> questionnaireScore
        MatchMode.ASTROLOGY -> astrologyScore
        MatchMode.ADVANCED -> combinedScore
    }
}

enum class MatchMode { QUESTIONNAIRE, ASTROLOGY, ADVANCED }

data class MatchFilter(
    val ageMin: Int = 18,
    val ageMax: Int = 70,
    val heightMinCm: Int = 90,
    val heightMaxCm: Int = 250,
    val city: String = "",
    val state: String = "",
    val caste: String = "",
    val minScore: Float = 0f,
    val minMutualMatchPercent: Int = 0,
    val religion: String = "",
    val motherTongue: String = "",
    val maritalStatus: String = "",
    val verifiedOnly: Boolean = false,
    val incomeMin: String = "",
    val incomeMax: String = "",
    val educationLevel: String = "",
    val diet: String = "",
    val residentialStatus: String = "",
    val hasChildren: String = "",
    val keyword: String = "",
    val gothra: String = "",
    val nativeState: String = "",
    val countryOfResidence: String = "",
    val nriOnly: Boolean = false,
    val willingToRelocate: Boolean = false,
    val recentlyJoinedDays: Int = 0,
    val smoking: String = "",
    val drinking: String = "",
    val familyType: String = "",
    val familyStatus: String = "",
    val physicalStatus: String = "",
    val hasChildrenFilter: String = "",
    val citizenship: String = "",
    val nriStatus: String = "",
    val educationField: String = "",
    val occupationCategory: String = "",
    val employerType: String = "",
    val subCaste: String = "",
    val faithTradition: String = "",
    val faithSubTradition: String = "",
    val faithInstitution: String = "",
    val visaStatus: String = "",
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
