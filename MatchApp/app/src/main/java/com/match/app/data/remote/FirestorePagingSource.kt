package com.match.app.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.functions.FirebaseFunctions
import com.match.app.data.local.entity.UserEntity
import com.match.app.domain.model.MatchFilter
import kotlinx.coroutines.tasks.await

/**
 * Cursor-paged production discovery source backed by a trusted callable.
 *
 * Per-document block and stealth visibility cannot safely be implemented by querying the
 * Firestore `users` collection directly because security rules are not query filters. The
 * backend derives viewer identity from Firebase Auth, excludes either-direction blocks,
 * lifecycle/privacy-ineligible profiles and applies the complete filter contract before returning
 * only public profile fields. Client checks are defensive UI consistency checks, never authority.
 */
class FirestorePagingSource(
    private val myUid: String,
    private val myGender: String,
    private val myLookingFor: String,
    private val filter: MatchFilter,
    private val blockedUids: Set<String> = emptySet(),
    @Suppress("unused") private val likedUids: Set<String> = emptySet()
) : PagingSource<String, UserEntity>() {

    private val functions = FirebaseFunctions.getInstance()

    companion object {
        const val PAGE_SIZE = 20
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }

    override fun getRefreshKey(state: PagingState<String, UserEntity>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, UserEntity> = try {
        val payload = mutableMapOf<String, Any>(
            "ageMin" to filter.ageMin,
            "ageMax" to filter.ageMax,
            "withPhotoOnly" to filter.withPhotoOnly,
            "verifiedOnly" to filter.verifiedOnly,
            "premiumOnly" to filter.premiumOnly,
            "nriOnly" to filter.nriOnly,
            "willingToRelocate" to filter.willingToRelocate,
            "verifiedLevel" to filter.verifiedLevel,
            "recentlyJoinedDays" to filter.recentlyJoinedDays,
            "lastActiveWithinDays" to filter.lastActiveWithinDays
        )
        fun putText(key: String, value: String) {
            value.trim().takeIf { it.isNotBlank() }?.let { payload[key] = it.take(100) }
        }
        params.key?.takeIf { it.isNotBlank() }?.let { payload["cursor"] = it }
        putText("keyword", filter.keyword.take(64))
        putText("city", filter.city)
        putText("state", filter.state)
        putText("religion", filter.religion)
        putText("caste", filter.caste)
        putText("subCaste", filter.subCaste)
        putText("motherTongue", filter.motherTongue)
        putText("maritalStatus", filter.maritalStatus)
        putText("incomeMin", filter.incomeMin)
        putText("incomeMax", filter.incomeMax)
        putText("educationLevel", filter.educationLevel)
        putText("educationField", filter.educationField)
        putText("occupationCategory", filter.occupationCategory)
        putText("employerType", filter.employerType)
        putText("diet", filter.diet)
        putText("residentialStatus", filter.residentialStatus)
        putText("hasChildren", filter.hasChildren)
        putText("hasChildrenFilter", filter.hasChildrenFilter)
        putText("gothra", filter.gothra)
        putText("nativeState", filter.nativeState)
        putText("countryOfResidence", filter.countryOfResidence)
        putText("nriStatus", filter.nriStatus)
        putText("smoking", filter.smoking)
        putText("drinking", filter.drinking)
        putText("familyType", filter.familyType)
        putText("familyStatus", filter.familyStatus)
        putText("physicalStatus", filter.physicalStatus)
        putText("citizenship", filter.citizenship)
        putText("rasi", filter.rasi)
        putText("nakshatra", filter.nakshatra)
        putText("manglik", filter.manglik)
        putText("hobbies", filter.hobbies)
        putText("hasHoroscope", filter.hasHoroscope)

        val response = functions.getHttpsCallable("discoverProfiles")
            .call(payload)
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = response.data as? Map<String, Any?>
            ?: return LoadResult.Error(IllegalStateException("Invalid discovery response"))
        @Suppress("UNCHECKED_CAST")
        val rawProfiles = data["profiles"] as? List<Map<String, Any?>> ?: emptyList()
        val nextCursor = data["nextCursor"] as? String
        val now = System.currentTimeMillis()

        val profiles = rawProfiles.mapNotNull { raw ->
            val uid = raw["firebaseUid"] as? String ?: return@mapNotNull null
            if (uid.isBlank() || uid == myUid || uid in blockedUids) return@mapNotNull null
            val entity = mapToEntity(uid, raw)

            if (!matchesGenderPreference(entity)) return@mapNotNull null
            if (entity.age !in filter.ageMin..filter.ageMax) return@mapNotNull null
            if (!matchesText(filter.city, entity.city)) return@mapNotNull null
            if (!matchesText(filter.state, entity.state)) return@mapNotNull null
            if (!matchesText(filter.religion, entity.religion)) return@mapNotNull null
            if (!matchesText(filter.caste, entity.caste)) return@mapNotNull null
            if (!matchesText(filter.subCaste, entity.subCaste)) return@mapNotNull null
            if (!matchesText(filter.motherTongue, entity.motherTongue)) return@mapNotNull null
            if (!matchesText(filter.maritalStatus, entity.maritalStatus)) return@mapNotNull null
            if (filter.verifiedOnly && !entity.isVerified) return@mapNotNull null
            if (filter.verifiedLevel > 0 && entity.verificationLevel < filter.verifiedLevel) return@mapNotNull null
            if (filter.premiumOnly && !entity.isPremium) return@mapNotNull null
            if (filter.withPhotoOnly && entity.photoUrl.isBlank()) return@mapNotNull null
            if (!matchesText(filter.diet, entity.diet)) return@mapNotNull null
            if (!matchesText(filter.educationLevel, entity.education)) return@mapNotNull null
            if (!matchesText(filter.educationField, entity.educationField)) return@mapNotNull null
            if (!matchesText(filter.occupationCategory, entity.occupationCategory)) return@mapNotNull null
            if (!matchesText(filter.employerType, entity.employerType)) return@mapNotNull null
            if (!matchesText(filter.residentialStatus, entity.residentialStatus)) return@mapNotNull null
            if (!matchesText(filter.nativeState, entity.nativeState)) return@mapNotNull null
            if (!matchesText(filter.countryOfResidence, entity.countryOfResidence)) return@mapNotNull null
            if (!matchesText(filter.citizenship, entity.citizenship)) return@mapNotNull null
            if (!matchesText(filter.smoking, entity.smoking)) return@mapNotNull null
            if (!matchesText(filter.drinking, entity.drinking)) return@mapNotNull null
            if (!matchesText(filter.familyType, entity.familyType)) return@mapNotNull null
            if (!matchesText(filter.familyStatus, entity.familyStatus)) return@mapNotNull null
            if (!matchesText(filter.physicalStatus, entity.physicalStatus)) return@mapNotNull null
            if (filter.hobbies.isNotBlank() && !entity.hobbies.contains(filter.hobbies, true)) return@mapNotNull null
            if (filter.keyword.isNotBlank()) {
                val keyword = filter.keyword.trim().removePrefix("@").lowercase()
                val searchable = listOf(
                    entity.displayName,
                    entity.username,
                    entity.matrimonyId,
                    entity.bio,
                    entity.profession,
                    entity.city,
                    entity.state
                )
                if (searchable.none { it.lowercase().contains(keyword) }) return@mapNotNull null
            }
            if (filter.hasChildren.isNotBlank() && !filter.hasChildren.equals("Any", true) &&
                (filter.hasChildren.equals("Yes", true) != entity.hasChildren)) return@mapNotNull null
            if (filter.hasChildrenFilter.isNotBlank() && !filter.hasChildrenFilter.equals("Don't mind", true)) {
                val wanted = when {
                    filter.hasChildrenFilter.startsWith("No", true) -> false
                    filter.hasChildrenFilter.startsWith("Yes", true) -> true
                    else -> entity.hasChildren
                }
                if (entity.hasChildren != wanted) return@mapNotNull null
            }
            if (filter.nriOnly && !entity.isNRI &&
                (entity.countryOfResidence.isBlank() || entity.countryOfResidence.equals("India", true))) return@mapNotNull null
            when (filter.nriStatus.lowercase()) {
                "only" -> if (!entity.isNRI) return@mapNotNull null
                "exclude" -> if (entity.isNRI) return@mapNotNull null
            }
            if (filter.willingToRelocate && !entity.willingToRelocate) return@mapNotNull null
            if (filter.recentlyJoinedDays > 0 && entity.createdAt < now - filter.recentlyJoinedDays * DAY_MS) return@mapNotNull null
            entity
        }

        LoadResult.Page(
            data = profiles,
            prevKey = null,
            nextKey = nextCursor?.takeIf { it.isNotBlank() && it != params.key }
        )
    } catch (e: Exception) {
        LoadResult.Error(e)
    }

    private fun matchesText(expected: String, actual: String): Boolean =
        expected.isBlank() || actual.equals(expected, true)

    private fun matchesGenderPreference(candidate: UserEntity): Boolean {
        val iAccept = myLookingFor == "ANY" || myLookingFor == candidate.gender
        val theyAccept = candidate.lookingFor == "ANY" || candidate.lookingFor == myGender
        return iAccept && theyAccept
    }

    private fun mapToEntity(uid: String, data: Map<String, Any?>): UserEntity = UserEntity(
        firebaseUid = uid,
        email = "",
        passwordHash = "",
        displayName = data["displayName"] as? String ?: "",
        age = (data["age"] as? Number)?.toInt() ?: 25,
        gender = data["gender"] as? String ?: "MALE",
        lookingFor = data["lookingFor"] as? String ?: "FEMALE",
        city = data["city"] as? String ?: "",
        bio = data["bio"] as? String ?: "",
        rasi = data["rasi"] as? String ?: "",
        nakshatra = data["nakshatra"] as? String ?: "",
        religion = data["religion"] as? String ?: "",
        motherTongue = data["motherTongue"] as? String ?: "",
        education = data["education"] as? String ?: "",
        profession = data["profession"] as? String ?: "",
        maritalStatus = data["maritalStatus"] as? String ?: "",
        heightCm = (data["heightCm"] as? Number)?.toInt() ?: 0,
        isVerified = data["isVerified"] as? Boolean ?: false,
        isPremium = data["isPremium"] as? Boolean ?: false,
        profileViewCount = (data["profileViewCount"] as? Number)?.toInt() ?: 0,
        caste = data["caste"] as? String ?: "",
        state = data["state"] as? String ?: "",
        subCaste = data["subCaste"] as? String ?: "",
        gothra = data["gothra"] as? String ?: "",
        incomeBand = data["incomeBand"] as? String ?: "",
        diet = data["diet"] as? String ?: "",
        familyType = data["familyType"] as? String ?: "",
        fatherOccupation = data["fatherOccupation"] as? String ?: "",
        motherOccupation = data["motherOccupation"] as? String ?: "",
        siblings = (data["siblings"] as? Number)?.toInt() ?: 0,
        smoking = data["smoking"] as? String ?: "",
        drinking = data["drinking"] as? String ?: "",
        personalityType = data["personalityType"] as? String ?: "",
        hobbies = when (val raw = data["hobbies"]) {
            is List<*> -> raw.filterIsInstance<String>().joinToString(",")
            is String -> raw
            else -> ""
        },
        spokenLanguages = when (val raw = data["spokenLanguages"]) {
            is List<*> -> raw.filterIsInstance<String>().joinToString(",")
            is String -> raw
            else -> ""
        },
        videoUrl = data["videoUrl"] as? String ?: "",
        residentialStatus = data["residentialStatus"] as? String ?: "",
        hasChildren = data["hasChildren"] as? Boolean ?: false,
        boostActiveUntil = (data["boostActiveUntil"] as? Number)?.toLong() ?: 0L,
        nativeState = data["nativeState"] as? String ?: "",
        countryOfResidence = data["countryOfResidence"] as? String ?: "",
        visaStatus = data["visaStatus"] as? String ?: "",
        willingToRelocate = data["willingToRelocate"] as? Boolean ?: false,
        createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
        lastActiveAt = (data["lastActiveAt"] as? Number)?.toLong() ?: 0L,
        isIncognito = data["isIncognito"] as? Boolean ?: false,
        phoneNumber = "",
        ageBucket = data["ageBucket"] as? String ?: "",
        familyValues = data["familyValues"] as? String ?: "",
        aboutFamily = data["aboutFamily"] as? String ?: "",
        manglik = data["manglik"] as? String ?: "",
        dateOfBirth = "",
        weight = (data["weight"] as? Number)?.toFloat() ?: 0f,
        complexion = data["complexion"] as? String ?: "",
        physicalStatus = data["physicalStatus"] as? String ?: "",
        birthTime = "",
        birthPlace = data["birthPlace"] as? String ?: "",
        familyStatus = data["familyStatus"] as? String ?: "",
        educationField = data["educationField"] as? String ?: "",
        institution = data["institution"] as? String ?: "",
        graduationYear = (data["graduationYear"] as? Number)?.toInt() ?: 0,
        occupationCategory = data["occupationCategory"] as? String ?: "",
        employer = data["employer"] as? String ?: "",
        employerType = data["employerType"] as? String ?: "",
        citizenship = data["citizenship"] as? String ?: "",
        isNRI = data["isNRI"] as? Boolean ?: false,
        fitnessActivities = data["fitnessActivities"] as? String ?: "",
        matrimonyId = data["matrimonyId"] as? String ?: "",
        photoUrl = data["photoUrl"] as? String ?: "",
        voiceBioUrl = data["voiceBioUrl"] as? String ?: "",
        profileCompleteness = (data["profileCompleteness"] as? Number)?.toFloat() ?: 0f,
        verificationLevel = (data["verificationLevel"] as? Number)?.toInt() ?: 0,
        stealthMode = data["stealthMode"] as? Boolean ?: false,
        showLastActive = data["showLastActive"] as? Boolean ?: true,
        showHoroscope = data["showHoroscope"] as? Boolean ?: true,
        incomeDisclosure = data["incomeDisclosure"] as? String ?: "range",
        subscriptionPlan = data["subscriptionPlan"] as? String ?: "FREE",
        subscriptionExpiry = (data["subscriptionExpiry"] as? Number)?.toLong() ?: 0L,
        matchScore = (data["matchScore"] as? Number)?.toFloat() ?: 0f,
        username = data["username"] as? String ?: ""
    )
}
