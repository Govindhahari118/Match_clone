package com.match.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
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
 * Public/discoverable fields live in `users/{firebaseUid}`. Sensitive account/contact
 * fields live in `userPrivate/{firebaseUid}` and are readable only by the owner or
 * trusted server code. Firebase Auth UID is the only remote security identity; Room IDs
 * are local cache identifiers only.
 */
@Singleton
class FirestoreProfileService @Inject constructor(
    private val userDao: UserDao
) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCol = db.collection("users")
    private val privateCol = db.collection("userPrivate")

    companion object {
        private val SERVER_OWNED_FIELDS = setOf(
            "isPremium", "isVerified", "matrimonyId", "verificationLevel",
            "subscriptionPlan", "subscriptionExpiry", "premiumPlan", "premiumUntil",
            "paymentId", "contactsRevealedThisMonth", "contactsResetAt"
        )
        private val PRIVATE_FIELDS = setOf(
            "email", "phoneNumber", "fcmToken", "dateOfBirth", "rasi", "nakshatra",
            "manglik", "birthTime", "birthPlace", "incomeBand", "lastActiveAt"
        )
    }

    /** Push the profile while keeping contact/private data out of discoverable documents. */
    suspend fun pushProfile(entity: UserEntity) {
        if (entity.firebaseUid.isBlank()) return
        val uid = entity.firebaseUid
        val publicData = entityToPublicMap(entity).toMutableMap().apply {
            put("email", FieldValue.delete())
            put("phoneNumber", FieldValue.delete())
            put("fcmToken", FieldValue.delete())
            put("dateOfBirth", FieldValue.delete())
        }
        val privateData = entityToPrivateMap(entity)

        val batch = db.batch()
        batch.set(usersCol.document(uid), publicData, SetOptions.merge())
        batch.set(privateCol.document(uid), privateData, SetOptions.merge())
        batch.commit().await()
    }

    fun ageBucketFor(age: Int): String = when {
        age < 18 -> "<18"
        age >= 63 -> "63+"
        else -> {
            val lo = 18 + ((age - 18) / 5) * 5
            "$lo-${lo + 4}"
        }
    }

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

    /**
     * Update profile fields without ever permitting the Android client to mutate
     * billing/verification authority. Sensitive owner fields are routed to userPrivate.
     */
    suspend fun updateFields(firebaseUid: String, fields: Map<String, Any?>) {
        if (firebaseUid.isBlank() || fields.isEmpty()) return
        require(auth.currentUser?.uid == firebaseUid) { "Cannot update another user's profile" }
        require(fields.keys.none { it in SERVER_OWNED_FIELDS }) { "Server-owned field update rejected" }

        val privateUpdates = fields.filterKeys { it in PRIVATE_FIELDS }
        val publicUpdates = fields.filterKeys { it !in PRIVATE_FIELDS }
        val batch = db.batch()
        if (publicUpdates.isNotEmpty()) batch.set(usersCol.document(firebaseUid), publicUpdates, SetOptions.merge())
        if (privateUpdates.isNotEmpty()) batch.set(privateCol.document(firebaseUid), privateUpdates, SetOptions.merge())
        batch.commit().await()
    }

    /** Fetch one profile. Private data is merged only when the signed-in owner is reading it. */
    suspend fun fetchProfile(firebaseUid: String): UserEntity? = fetchProfile(firebaseUid, Source.DEFAULT)

    /**
     * Force a server authorization round-trip before using an already-cached member profile.
     * This is used by privacy-sensitive surfaces such as request/inbox hydration where a stale
     * local or Firestore cache must not keep showing someone after a new hide/block/stealth rule.
     */
    suspend fun fetchProfileFromServer(firebaseUid: String): UserEntity? = fetchProfile(firebaseUid, Source.SERVER)

    private suspend fun fetchProfile(firebaseUid: String, source: Source): UserEntity? {
        if (firebaseUid.isBlank()) return null
        val doc = usersCol.document(firebaseUid).get(source).await()
        if (!doc.exists()) return null
        var data: Map<String, Any?> = doc.data ?: return null
        if (auth.currentUser?.uid == firebaseUid) {
            val privateDoc = privateCol.document(firebaseUid).get(source).await()
            if (privateDoc.exists()) data = data + (privateDoc.data ?: emptyMap())
        }
        return mapToEntity(firebaseUid, data)
    }

    suspend fun fetchProfiles(uids: List<String>): List<UserEntity> {
        if (uids.isEmpty()) return emptyList()
        return uids.chunked(30).flatMap { chunk ->
            usersCol.whereIn("firebaseUid", chunk).get().await().documents.mapNotNull { doc ->
                mapToEntity(doc.id, doc.data ?: return@mapNotNull null)
            }
        }
    }

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
        query = query.whereGreaterThanOrEqualTo("age", ageMin)
            .whereLessThanOrEqualTo("age", ageMax)

        val snap = query.get().await()
        return snap.documents.mapNotNull { doc ->
            if (doc.id == excludeUid) return@mapNotNull null
            val entity = mapToEntity(doc.id, doc.data ?: return@mapNotNull null)
            if (gender != null && !entity.gender.equals(gender, ignoreCase = true)) return@mapNotNull null
            if (!religion.isNullOrBlank() && !entity.religion.equals(religion, ignoreCase = true)) return@mapNotNull null
            if (!city.isNullOrBlank() && !entity.city.equals(city, ignoreCase = true)) return@mapNotNull null
            entity
        }
    }

    fun observeProfile(firebaseUid: String): Flow<UserEntity?> = callbackFlow {
        val reg = usersCol.document(firebaseUid).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            if (snap == null || !snap.exists()) { trySend(null); return@addSnapshotListener }
            trySend(mapToEntity(firebaseUid, snap.data ?: return@addSnapshotListener))
        }
        awaitClose { reg.remove() }
    }

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
        val query = usersCol
            .whereEqualTo("gender", gender)
            .whereIn("ageBucket", buckets)
            .orderBy("lastActiveAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
        val snap = query.get().await()
        return snap.documents.mapNotNull { doc ->
            if (doc.id == excludeUid) return@mapNotNull null
            val entity = mapToEntity(doc.id, doc.data ?: return@mapNotNull null)
            if (!religion.isNullOrBlank() && !entity.religion.equals(religion, ignoreCase = true)) return@mapNotNull null
            if (!city.isNullOrBlank() && !entity.city.equals(city, ignoreCase = true)) return@mapNotNull null
            entity
        }
    }

    suspend fun saveFcmToken(firebaseUid: String, token: String) {
        if (firebaseUid.isBlank() || token.isBlank()) return
        require(auth.currentUser?.uid == firebaseUid) { "Cannot update another user's token" }
        privateCol.document(firebaseUid).set(
            mapOf("fcmToken" to token, "updatedAt" to System.currentTimeMillis()),
            SetOptions.merge()
        ).await()
    }

    suspend fun deleteProfile(firebaseUid: String) {
        if (firebaseUid.isBlank()) return
        require(auth.currentUser?.uid == firebaseUid) { "Cannot delete another user's profile" }
        val batch = db.batch()
        batch.delete(usersCol.document(firebaseUid))
        batch.delete(privateCol.document(firebaseUid))
        batch.commit().await()
    }

    private fun entityToPrivateMap(e: UserEntity): Map<String, Any?> = mapOf(
        "email" to e.email,
        "phoneNumber" to e.phoneNumber,
        "dateOfBirth" to e.dateOfBirth,
        "rasi" to e.rasi,
        "nakshatra" to e.nakshatra,
        "manglik" to e.manglik,
        "birthTime" to e.birthTime,
        "birthPlace" to e.birthPlace,
        "incomeBand" to e.incomeBand,
        "lastActiveAt" to e.lastActiveAt,
        "updatedAt" to System.currentTimeMillis()
    )

    private fun entityToPublicMap(e: UserEntity): Map<String, Any?> = mapOf(
        "firebaseUid" to e.firebaseUid,
        "displayName" to e.displayName,
        "age" to e.age,
        "gender" to e.gender,
        "lookingFor" to e.lookingFor,
        "city" to e.city,
        "bio" to e.bio,
        "religion" to e.religion,
        "caste" to e.caste,
        "motherTongue" to e.motherTongue,
        "education" to e.education,
        "profession" to e.profession,
        "maritalStatus" to e.maritalStatus,
        "heightCm" to e.heightCm,
        "state" to e.state,
        "subCaste" to e.subCaste,
        "gothra" to e.gothra,
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
        "boostActiveUntil" to e.boostActiveUntil,
        "nativeState" to e.nativeState,
        "countryOfResidence" to e.countryOfResidence,
        "visaStatus" to e.visaStatus,
        "willingToRelocate" to e.willingToRelocate,
        "createdAt" to e.createdAt,
        "ageBucket" to ageBucketFor(e.age),
        "isIncognito" to e.isIncognito,
        "familyValues" to e.familyValues,
        "aboutFamily" to e.aboutFamily,
        "weight" to e.weight,
        "complexion" to e.complexion,
        "physicalStatus" to e.physicalStatus,
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
        "photoUrl" to e.photoUrl,
        "voiceBioUrl" to e.voiceBioUrl,
        "profileCompleteness" to e.profileCompleteness,
        "stealthMode" to e.stealthMode,
        "showLastActive" to e.showLastActive,
        "showHoroscope" to e.showHoroscope,
        "incomeDisclosure" to e.incomeDisclosure,
        "matchScore" to e.matchScore,
        "updatedAt" to System.currentTimeMillis()
    )

    private fun mapToEntity(firebaseUid: String, data: Map<String, Any?>): UserEntity = UserEntity(
        firebaseUid = firebaseUid,
        email = data["email"] as? String ?: "",
        passwordHash = "",
        displayName = data["displayName"] as? String ?: "",
        age = (data["age"] as? Number)?.toInt() ?: 0,
        gender = data["gender"] as? String ?: "OTHER",
        lookingFor = data["lookingFor"] as? String ?: "ANY",
        city = data["city"] as? String ?: "",
        bio = data["bio"] as? String ?: "",
        rasi = data["rasi"] as? String ?: "",
        nakshatra = data["nakshatra"] as? String ?: "",
        religion = data["religion"] as? String ?: "",
        caste = data["caste"] as? String ?: "",
        motherTongue = data["motherTongue"] as? String ?: "",
        education = data["education"] as? String ?: "",
        profession = data["profession"] as? String ?: "",
        maritalStatus = data["maritalStatus"] as? String ?: "",
        heightCm = (data["heightCm"] as? Number)?.toInt() ?: 0,
        isVerified = data["isVerified"] as? Boolean ?: false,
        isPremium = data["isPremium"] as? Boolean ?: false,
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
        profileViewCount = (data["profileViewCount"] as? Number)?.toInt() ?: 0,
        boostActiveUntil = (data["boostActiveUntil"] as? Number)?.toLong() ?: 0L,
        nativeState = data["nativeState"] as? String ?: "",
        countryOfResidence = data["countryOfResidence"] as? String ?: "",
        visaStatus = data["visaStatus"] as? String ?: "",
        willingToRelocate = data["willingToRelocate"] as? Boolean ?: false,
        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        lastActiveAt = (data["lastActiveAt"] as? Number)?.toLong() ?: 0L,
        phoneNumber = data["phoneNumber"] as? String ?: "",
        isIncognito = data["isIncognito"] as? Boolean ?: false,
        ageBucket = data["ageBucket"] as? String ?: "",
        familyValues = data["familyValues"] as? String ?: "",
        aboutFamily = data["aboutFamily"] as? String ?: "",
        manglik = data["manglik"] as? String ?: "",
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
