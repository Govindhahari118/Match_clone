package com.match.app.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.match.app.data.local.entity.UserEntity
import com.match.app.domain.model.MatchFilter
import kotlinx.coroutines.tasks.await

/**
 * Paging 3 source that loads user profiles from Firestore in pages.
 *
 * Uses cursor-based pagination with DocumentSnapshot as the key.
 * Applies server-side filters where possible; remaining filters applied client-side.
 */
class FirestorePagingSource(
    private val myUid: String,
    private val myGender: String,
    private val myLookingFor: String,
    private val filter: MatchFilter,
    private val blockedUids: Set<String>,
    private val likedUids: Set<String>
) : PagingSource<DocumentSnapshot, UserEntity>() {

    private val db = FirebaseFirestore.getInstance()
    private val usersCol = db.collection("users")

    companion object {
        const val PAGE_SIZE = 20

        /** Same logic as [FirestoreProfileService.ageBucketFor] — kept static to avoid DI. */
        private fun ageBucketFor(age: Int): String = when {
            age < 18 -> "<18"
            age >= 63 -> "63+"
            else -> {
                val lo = 18 + ((age - 18) / 5) * 5
                "$lo-${lo + 4}"
            }
        }

        /** All age buckets overlapping [ageMin]..[ageMax]. */
        private fun ageBucketsFor(ageMin: Int, ageMax: Int): List<String> {
            val lo = maxOf(ageMin, 18)
            val hi = minOf(ageMax, 70)
            if (hi < lo) return emptyList()
            val buckets = linkedSetOf<String>()
            var a = lo
            while (a <= hi) { buckets.add(ageBucketFor(a)); a++ }
            return buckets.toList()
        }
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, UserEntity>): DocumentSnapshot? = null

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, UserEntity> {
        return try {
            // Compute ageBuckets using the same logic as FirestoreProfileService.ageBucketFor
            val buckets = ageBucketsFor(filter.ageMin, filter.ageMax)

            var query: Query = if (buckets.isNotEmpty() && myLookingFor != "ANY") {
                // Preferred path: composite index on (gender, ageBucket, lastActiveAt)
                usersCol
                    .whereEqualTo("gender", myLookingFor)
                    .whereIn("ageBucket", buckets.take(30))
                    .orderBy("lastActiveAt", Query.Direction.DESCENDING)
                    .limit((PAGE_SIZE * 2).toLong())
            } else {
                // Fallback for ANY preference or wide age range
                usersCol
                    .orderBy("lastActiveAt", Query.Direction.DESCENDING)
                    .limit((PAGE_SIZE * 2).toLong())
            }

            // Cursor pagination
            val startAfter = params.key
            if (startAfter != null) {
                query = query.startAfter(startAfter)
            }

            val snapshot = query.get().await()
            val documents = snapshot.documents

            val profiles = documents.mapNotNull { doc ->
                if (doc.id == myUid) return@mapNotNull null
                if (doc.id in blockedUids) return@mapNotNull null

                val data = doc.data ?: return@mapNotNull null
                val entity = mapToEntity(doc.id, data)

                // ── Privacy enforcement: hide stealth/incognito users ──
                if (entity.stealthMode || entity.isIncognito) return@mapNotNull null

                // Client-side filters
                if (!matchesGenderPreference(entity)) return@mapNotNull null
                if (filter.city.isNotBlank() && !entity.city.equals(filter.city, ignoreCase = true)) return@mapNotNull null
                if (filter.state.isNotBlank() && !entity.state.equals(filter.state, ignoreCase = true)) return@mapNotNull null
                if (filter.religion.isNotBlank() && !entity.religion.equals(filter.religion, ignoreCase = true)) return@mapNotNull null
                if (filter.caste.isNotBlank() && !entity.caste.equals(filter.caste, ignoreCase = true)) return@mapNotNull null
                if (filter.motherTongue.isNotBlank() && !entity.motherTongue.equals(filter.motherTongue, ignoreCase = true)) return@mapNotNull null
                if (filter.maritalStatus.isNotBlank() && !entity.maritalStatus.equals(filter.maritalStatus, ignoreCase = true)) return@mapNotNull null
                if (filter.verifiedOnly && !entity.isVerified) return@mapNotNull null
                if (filter.diet.isNotBlank() && !entity.diet.equals(filter.diet, ignoreCase = true)) return@mapNotNull null
                if (filter.educationLevel.isNotBlank() && !entity.education.equals(filter.educationLevel, ignoreCase = true)) return@mapNotNull null
                if (filter.nativeState.isNotBlank() && !entity.nativeState.equals(filter.nativeState, ignoreCase = true)) return@mapNotNull null
                if (filter.countryOfResidence.isNotBlank() && !entity.countryOfResidence.equals(filter.countryOfResidence, ignoreCase = true)) return@mapNotNull null
                if (filter.nriOnly && (entity.countryOfResidence.isBlank() || entity.countryOfResidence.equals("India", ignoreCase = true))) return@mapNotNull null
                if (filter.willingToRelocate && !entity.willingToRelocate) return@mapNotNull null
                if (filter.gothra.isNotBlank() && !entity.gothra.equals(filter.gothra, ignoreCase = true)) return@mapNotNull null
                if (filter.keyword.isNotBlank()) {
                    val kw = filter.keyword
                    val matches = entity.displayName.contains(kw, ignoreCase = true) ||
                            entity.bio.contains(kw, ignoreCase = true) ||
                            entity.profession.contains(kw, ignoreCase = true)
                    if (!matches) return@mapNotNull null
                }
                if (filter.recentlyJoinedDays > 0) {
                    val cutoff = System.currentTimeMillis() - filter.recentlyJoinedDays * 24 * 60 * 60 * 1000L
                    if (entity.createdAt < cutoff) return@mapNotNull null
                }
                // ── Sprint-10 extended filters ──────────────────────────
                if (filter.smoking.isNotBlank() && !entity.smoking.equals(filter.smoking, ignoreCase = true)) return@mapNotNull null
                if (filter.drinking.isNotBlank() && !entity.drinking.equals(filter.drinking, ignoreCase = true)) return@mapNotNull null
                if (filter.familyType.isNotBlank() && !entity.familyType.equals(filter.familyType, ignoreCase = true)) return@mapNotNull null
                if (filter.familyStatus.isNotBlank() && !entity.familyStatus.equals(filter.familyStatus, ignoreCase = true)) return@mapNotNull null
                if (filter.physicalStatus.isNotBlank() && !entity.physicalStatus.equals(filter.physicalStatus, ignoreCase = true)) return@mapNotNull null
                if (filter.educationField.isNotBlank() && !entity.educationField.equals(filter.educationField, ignoreCase = true)) return@mapNotNull null
                if (filter.occupationCategory.isNotBlank() && !entity.occupationCategory.equals(filter.occupationCategory, ignoreCase = true)) return@mapNotNull null
                if (filter.manglik.isNotBlank() && !entity.manglik.equals(filter.manglik, ignoreCase = true)) return@mapNotNull null
                if (filter.rasi.isNotBlank() && !entity.rasi.equals(filter.rasi, ignoreCase = true)) return@mapNotNull null
                if (filter.nakshatra.isNotBlank() && !entity.nakshatra.equals(filter.nakshatra, ignoreCase = true)) return@mapNotNull null
                if (filter.withPhotoOnly && entity.photoUrl.isBlank()) return@mapNotNull null
                if (filter.premiumOnly && !entity.isPremium) return@mapNotNull null
                if (filter.lastActiveWithinDays > 0) {
                    val cutoff = System.currentTimeMillis() - filter.lastActiveWithinDays * 24 * 60 * 60 * 1000L
                    if (entity.lastActiveAt < cutoff) return@mapNotNull null
                }
                if (filter.verifiedLevel > 0 && entity.verificationLevel < filter.verifiedLevel) return@mapNotNull null

                entity
            }.take(PAGE_SIZE)

            val lastDoc = if (documents.isNotEmpty()) documents.last() else null

            LoadResult.Page(
                data = profiles,
                prevKey = null, // Only forward paging
                nextKey = if (documents.size < PAGE_SIZE) null else lastDoc
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private fun matchesGenderPreference(candidate: UserEntity): Boolean {
        // Check if my preference matches candidate's gender
        val iWant = myLookingFor
        val theyAre = candidate.gender
        val theyWant = candidate.lookingFor
        val iAm = myGender

        val iAccept = iWant == "ANY" || iWant == theyAre
        val theyAccept = theyWant == "ANY" || theyWant == iAm
        return iAccept && theyAccept
    }

    private fun mapToEntity(uid: String, data: Map<String, Any?>): UserEntity = UserEntity(
        firebaseUid = uid,
        email = data["email"] as? String ?: "",
        passwordHash = "",
        displayName = data["displayName"] as? String ?: "",
        age = (data["age"] as? Number)?.toInt() ?: 25,
        gender = data["gender"] as? String ?: "MALE",
        lookingFor = data["lookingFor"] as? String ?: "FEMALE",
        city = data["city"] as? String ?: "",
        bio = data["bio"] as? String ?: "",
        rasi = data["rasi"] as? String ?: "",
        nakshatra = data["nakshatra"] as? String ?: "",
        religion = data["religion"] as? String ?: "Hindu",
        motherTongue = data["motherTongue"] as? String ?: "",
        education = data["education"] as? String ?: "",
        profession = data["profession"] as? String ?: "",
        maritalStatus = data["maritalStatus"] as? String ?: "Never Married",
        heightCm = (data["heightCm"] as? Number)?.toInt() ?: 165,
        isVerified = data["isVerified"] as? Boolean ?: false,
        isPremium = data["isPremium"] as? Boolean ?: false,
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
        hobbies = data["hobbies"] as? String ?: "",
        spokenLanguages = data["spokenLanguages"] as? String ?: "",
        videoUrl = data["videoUrl"] as? String ?: "",
        residentialStatus = data["residentialStatus"] as? String ?: "",
        hasChildren = data["hasChildren"] as? Boolean ?: false,
        nativeState = data["nativeState"] as? String ?: "",
        countryOfResidence = data["countryOfResidence"] as? String ?: "",
        visaStatus = data["visaStatus"] as? String ?: "",
        willingToRelocate = data["willingToRelocate"] as? Boolean ?: false,
        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        boostActiveUntil = (data["boostActiveUntil"] as? Number)?.toLong() ?: 0L,
        lastActiveAt = (data["lastActiveAt"] as? Number)?.toLong() ?: 0L,
        isIncognito = data["isIncognito"] as? Boolean ?: false,
        phoneNumber = data["phoneNumber"] as? String ?: "",
        ageBucket = data["ageBucket"] as? String ?: "",
        familyValues = data["familyValues"] as? String ?: "",
        aboutFamily = data["aboutFamily"] as? String ?: "",
        manglik = data["manglik"] as? String ?: "",
        stealthMode = data["stealthMode"] as? Boolean ?: false,
        verificationLevel = (data["verificationLevel"] as? Number)?.toInt() ?: 0,
        subscriptionPlan = data["subscriptionPlan"] as? String ?: "FREE",
        photoUrl = data["photoUrl"] as? String ?: "",
        profileCompleteness = (data["profileCompleteness"] as? Number)?.toFloat() ?: 0f,
        physicalStatus = data["physicalStatus"] as? String ?: "",
        educationField = data["educationField"] as? String ?: "",
        occupationCategory = data["occupationCategory"] as? String ?: "",
        familyStatus = data["familyStatus"] as? String ?: "",
        employer = data["employer"] as? String ?: "",
        institution = data["institution"] as? String ?: "",
        weight = (data["weight"] as? Number)?.toFloat() ?: 0f,
        complexion = data["complexion"] as? String ?: "",
        dateOfBirth = data["dateOfBirth"] as? String ?: "",
        birthTime = data["birthTime"] as? String ?: "",
        birthPlace = data["birthPlace"] as? String ?: "",
        fitnessActivities = data["fitnessActivities"] as? String ?: ""
    )
}
