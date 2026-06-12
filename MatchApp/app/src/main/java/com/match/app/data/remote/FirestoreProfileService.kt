package com.match.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.UserEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Syncs user profiles between local Room DB and Firestore.
 *
 * Firestore collection: `users/{firebaseUid}`
 * This is the source of truth for profile data in production.
 * Room serves as an offline cache.
 */
@Singleton
class FirestoreProfileService @Inject constructor(
    private val userDao: UserDao
) {
    private val db = FirebaseFirestore.getInstance()
    private val usersCol = db.collection("users")

    // ── Write profile to Firestore ────────────────────────────────────────

    /** Push the full profile to Firestore. Call on signup and every profile edit. */
    suspend fun pushProfile(entity: UserEntity) {
        if (entity.firebaseUid.isBlank()) return
        val data = entityToMap(entity)
        usersCol.document(entity.firebaseUid).set(data, SetOptions.merge()).await()
    }

    /**
     * Discrete age bucket used by [discoverProfilesIndexed] for index-friendly
     * equality queries instead of expensive Firestore range scans.
     * Mirrors the SQL backfill in [com.match.app.data.local.Migrations.MIGRATION_14_15].
     */
    fun ageBucketFor(age: Int): String = when {
        age < 18 -> "<18"
        age >= 63 -> "63+"
        else -> {
            val lo = 18 + ((age - 18) / 5) * 5
            "$lo-${lo + 4}"
        }
    }

    /** All age buckets that overlap [ageMin]..[ageMax]. Caller passes to whereIn. */
    fun ageBucketsFor(ageMin: Int, ageMax: Int): List<String> {
        val lo = maxOf(ageMin, 18)
        val hi = minOf(ageMax, 70)
        if (hi < lo) return emptyList()
        val buckets = linkedSetOf<String>()
        var a = lo
        while (a <= hi) {
            buckets.add(ageBucketFor(a))
            a += 1
        }
        return buckets.toList()
    }

    /** Update specific fields in Firestore (partial update). */
    suspend fun updateFields(firebaseUid: String, fields: Map<String, Any?>) {
        if (firebaseUid.isBlank()) return
        usersCol.document(firebaseUid).update(fields).await()
    }

    // ── Read profile from Firestore ───────────────────────────────────────

    /** Fetch a single profile by Firebase UID. Returns null if not found. */
    suspend fun fetchProfile(firebaseUid: String): UserEntity? {
        val doc = usersCol.document(firebaseUid).get().await()
        if (!doc.exists()) return null
        return mapToEntity(firebaseUid, doc.data ?: return null)
    }

    /** Fetch a batch of profiles by Firebase UIDs. */
    suspend fun fetchProfiles(uids: List<String>): List<UserEntity> {
        if (uids.isEmpty()) return emptyList()
        // Firestore `in` query supports max 30 items per batch
        return uids.chunked(30).flatMap { chunk ->
            usersCol.whereIn("firebaseUid", chunk).get().await().documents.mapNotNull { doc ->
                val uid = doc.id
                mapToEntity(uid, doc.data ?: return@mapNotNull null)
            }
        }
    }

    /**
     * Discover profiles from Firestore with basic filters.
     * Returns raw maps — caller converts to UserEntity/UserProfile.
     */
    suspend fun discoverProfiles(
        excludeUid: String,
        gender: String? = null,
        ageMin: Int = 18,
        ageMax: Int = 70,
        religion: String? = null,
        city: String? = null,
        limit: Int = 40
    ): List<UserEntity> {
        var query = usersCol.limit(limit.toLong())

        // Firestore can only do inequality on ONE field, so we filter age
        // and do remaining filters client-side
        query = query.whereGreaterThanOrEqualTo("age", ageMin)
                     .whereLessThanOrEqualTo("age", ageMax)

        val snap = query.get().await()
        return snap.documents.mapNotNull { doc ->
            if (doc.id == excludeUid) return@mapNotNull null
            val data = doc.data ?: return@mapNotNull null
            val entity = mapToEntity(doc.id, data)
            // Client-side filters
            if (gender != null && !entity.gender.equals(gender, ignoreCase = true)) return@mapNotNull null
            if (!religion.isNullOrBlank() && !entity.religion.equals(religion, ignoreCase = true)) return@mapNotNull null
            if (!city.isNullOrBlank() && !entity.city.equals(city, ignoreCase = true)) return@mapNotNull null
            entity
        }
    }

    /** Observe a profile in real-time. */
    fun observeProfile(firebaseUid: String): Flow<UserEntity?> = callbackFlow {
        val reg = usersCol.document(firebaseUid).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            if (snap == null || !snap.exists()) { trySend(null); return@addSnapshotListener }
            trySend(mapToEntity(firebaseUid, snap.data ?: return@addSnapshotListener))
        }
        awaitClose { reg.remove() }
    }

    /**
     * Sprint 9 Discovery 2.0: index-friendly profile discovery.
     * Uses equality on `ageBucket` (whereIn) plus `gender` so the query rides on the
     * composite index `(gender, ageBucket, lastActiveAt desc)`. Active profiles bubble
     * to the top — dormant profiles never compete for the feed head.
     *
     * `religion` and `city` are still applied client-side because adding them to the
     * index multiplies index storage cost; for v1 the bucket+gender narrowing is
     * sufficient (~10x read reduction vs. range query).
     */
    suspend fun discoverProfilesIndexed(
        excludeUid: String,
        gender: String,
        ageMin: Int = 18,
        ageMax: Int = 70,
        religion: String? = null,
        city: String? = null,
        limit: Int = 40
    ): List<UserEntity> {
        val buckets = ageBucketsFor(ageMin, ageMax)
        if (buckets.isEmpty()) return emptyList()
        // Firestore whereIn supports up to 30 values — we have at most ~10 buckets
        val query = usersCol
            .whereEqualTo("gender", gender)
            .whereIn("ageBucket", buckets)
            .orderBy("lastActiveAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
        val snap = query.get().await()
        return snap.documents.mapNotNull { doc ->
            if (doc.id == excludeUid) return@mapNotNull null
            val data = doc.data ?: return@mapNotNull null
            val entity = mapToEntity(doc.id, data)
            if (!religion.isNullOrBlank() && !entity.religion.equals(religion, ignoreCase = true)) return@mapNotNull null
            if (!city.isNullOrBlank() && !entity.city.equals(city, ignoreCase = true)) return@mapNotNull null
            entity
        }
    }

    /** Save FCM token to Firestore for push notifications. */
    suspend fun saveFcmToken(firebaseUid: String, token: String) {
        if (firebaseUid.isBlank()) return
        usersCol.document(firebaseUid).update("fcmToken", token).await()
    }

    /** Delete profile from Firestore (account deletion). */
    suspend fun deleteProfile(firebaseUid: String) {
        if (firebaseUid.isBlank()) return
        usersCol.document(firebaseUid).delete().await()
    }

    // ── Mapping helpers ───────────────────────────────────────────────────

    private fun entityToMap(e: UserEntity): Map<String, Any?> = mapOf(
        "firebaseUid" to e.firebaseUid,
        "email" to e.email,
        "displayName" to e.displayName,
        "age" to e.age,
        "gender" to e.gender,
        "lookingFor" to e.lookingFor,
        "city" to e.city,
        "bio" to e.bio,
        "rasi" to e.rasi,
        "nakshatra" to e.nakshatra,
        "religion" to e.religion,
        "motherTongue" to e.motherTongue,
        "education" to e.education,
        "profession" to e.profession,
        "maritalStatus" to e.maritalStatus,
        "heightCm" to e.heightCm,
        "isVerified" to e.isVerified,
        "isPremium" to e.isPremium,
        "caste" to e.caste,
        "state" to e.state,
        "subCaste" to e.subCaste,
        "gothra" to e.gothra,
        "incomeBand" to e.incomeBand,
        "diet" to e.diet,
        "familyType" to e.familyType,
        "fatherOccupation" to e.fatherOccupation,
        "motherOccupation" to e.motherOccupation,
        "siblings" to e.siblings,
        "smoking" to e.smoking,
        "drinking" to e.drinking,
        "personalityType" to e.personalityType,
        "hobbies" to e.hobbies,
        "spokenLanguages" to e.spokenLanguages,
        "videoUrl" to e.videoUrl,
        "residentialStatus" to e.residentialStatus,
        "hasChildren" to e.hasChildren,
        "nativeState" to e.nativeState,
        "countryOfResidence" to e.countryOfResidence,
        "visaStatus" to e.visaStatus,
        "willingToRelocate" to e.willingToRelocate,
        "createdAt" to e.createdAt,
        "lastActiveAt" to e.lastActiveAt,
        "isIncognito" to e.isIncognito,
        "phoneNumber" to e.phoneNumber,
        "ageBucket" to (e.ageBucket.ifBlank { ageBucketFor(e.age) }),
        "familyValues" to e.familyValues,
        "aboutFamily" to e.aboutFamily,
        "manglik" to e.manglik,
        // Sprint 10: New fields
        "dateOfBirth" to e.dateOfBirth,
        "weight" to e.weight,
        "complexion" to e.complexion,
        "physicalStatus" to e.physicalStatus,
        "birthTime" to e.birthTime,
        "birthPlace" to e.birthPlace,
        "familyStatus" to e.familyStatus,
        "educationField" to e.educationField,
        "institution" to e.institution,
        "graduationYear" to e.graduationYear,
        "occupationCategory" to e.occupationCategory,
        "employer" to e.employer,
        "employerType" to e.employerType,
        "citizenship" to e.citizenship,
        "isNRI" to e.isNRI,
        "fitnessActivities" to e.fitnessActivities,
        "matrimonyId" to e.matrimonyId,
        "photoUrl" to e.photoUrl,
        "voiceBioUrl" to e.voiceBioUrl,
        "profileCompleteness" to e.profileCompleteness,
        "verificationLevel" to e.verificationLevel,
        "stealthMode" to e.stealthMode,
        "showLastActive" to e.showLastActive,
        "showHoroscope" to e.showHoroscope,
        "incomeDisclosure" to e.incomeDisclosure,
        "subscriptionPlan" to e.subscriptionPlan,
        "subscriptionExpiry" to e.subscriptionExpiry,
        "matchScore" to e.matchScore,
        "updatedAt" to System.currentTimeMillis()
    )

    private fun mapToEntity(uid: String, data: Map<String, Any?>): UserEntity = UserEntity(
        firebaseUid = uid,
        email = data["email"] as? String ?: "",
        passwordHash = "", // Never stored in Firestore
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
        lastActiveAt = (data["lastActiveAt"] as? Number)?.toLong() ?: 0L,
        isIncognito = data["isIncognito"] as? Boolean ?: false,
        phoneNumber = data["phoneNumber"] as? String ?: "",
        ageBucket = data["ageBucket"] as? String ?: "",
        familyValues = data["familyValues"] as? String ?: "",
        aboutFamily = data["aboutFamily"] as? String ?: "",
        manglik = data["manglik"] as? String ?: "",
        // Sprint 10: New fields
        dateOfBirth = data["dateOfBirth"] as? String ?: "",
        weight = (data["weight"] as? Number)?.toFloat() ?: 0f,
        complexion = data["complexion"] as? String ?: "",
        physicalStatus = data["physicalStatus"] as? String ?: "",
        birthTime = data["birthTime"] as? String ?: "",
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
        matchScore = (data["matchScore"] as? Number)?.toFloat() ?: 0f
    )
}
