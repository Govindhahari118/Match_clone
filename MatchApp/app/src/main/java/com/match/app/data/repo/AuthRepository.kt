package com.match.app.data.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.messaging.FirebaseMessaging
import com.match.app.data.local.MatchDatabase
import com.match.app.data.local.dao.LikeDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.UserEntity
import com.match.app.data.remote.FirestoreProfileService
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.Gender
import com.match.app.domain.model.LookingFor
import com.match.app.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val userId: Long) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val likeDao: LikeDao,
    private val questionnaireRepo: QuestionnaireRepository,
    private val photoRepo: PhotoRepository,
    private val session: SessionStore,
    private val firestoreProfile: FirestoreProfileService,
    private val localDb: MatchDatabase
) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun signUp(
        email: String, password: String, displayName: String,
        age: Int, gender: Gender, lookingFor: LookingFor,
        city: String, bio: String, rasi: String, nakshatra: String,
        phone: String = ""
    ): AuthResult {
        val e = email.trim().lowercase()
        if (!e.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) return AuthResult.Error("Enter a valid email")
        if (password.length < 8) return AuthResult.Error("Password must be at least 8 characters")
        if (displayName.isBlank()) return AuthResult.Error("Name required")
        if (age !in 18..99) return AuthResult.Error("Age must be between 18 and 99")

        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(e, password).await()
            val firebaseUid = authResult.user?.uid
                ?: return AuthResult.Error("Firebase account creation failed")

            val entity = UserEntity(
                firebaseUid = firebaseUid,
                email = e,
                passwordHash = "",
                displayName = displayName.trim(),
                age = age,
                gender = gender.name,
                lookingFor = lookingFor.name,
                city = city.trim(),
                bio = bio.trim(),
                rasi = rasi,
                nakshatra = nakshatra,
                phoneNumber = phone.trim(),
                lastActiveAt = System.currentTimeMillis()
            )
            val localId = userDao.insert(entity)

            firestoreProfile.pushProfile(entity.copy(id = localId))
            registerFcmToken(firebaseUid)

            session.setUser(localId)
            session.setFirebaseUid(firebaseUid)
            AuthResult.Success(localId)
        } catch (ex: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            AuthResult.Error("Email is already registered")
        } catch (ex: Exception) {
            Log.e("AuthRepository", "signUp failed", ex)
            AuthResult.Error(ex.message ?: "Sign up failed")
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        val e = email.trim().lowercase()
        val previousUid = firebaseAuth.currentUser?.uid

        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(e, password).await()
            val firebaseUid = authResult.user?.uid
                ?: return AuthResult.Error("Firebase sign-in failed")

            if (!previousUid.isNullOrBlank() && previousUid != firebaseUid) {
                clearLocalAccountState()
            }

            val firestoreEntity = firestoreProfile.fetchProfile(firebaseUid)
            val localUser = userDao.findByFirebaseUid(firebaseUid)
            val localId: Long

            if (localUser != null) {
                if (firestoreEntity != null) {
                    userDao.update(
                        firestoreEntity.copy(
                            id = localUser.id,
                            email = e,
                            passwordHash = "",
                            isSeed = false
                        )
                    )
                }
                localId = localUser.id
            } else if (firestoreEntity != null) {
                localId = userDao.insert(firestoreEntity.copy(email = e, passwordHash = "", isSeed = false))
            } else {
                val legacyUser = userDao.findByEmail(e)
                    ?: return AuthResult.Error("Profile data is unavailable. Please contact support.")
                val migratedUser = legacyUser.copy(firebaseUid = firebaseUid, passwordHash = "")
                userDao.update(migratedUser)
                firestoreProfile.pushProfile(migratedUser)
                localId = legacyUser.id
            }

            registerFcmToken(firebaseUid)
            userDao.updateLastActiveAt(firebaseUid, System.currentTimeMillis())

            session.setUser(localId)
            session.setFirebaseUid(firebaseUid)
            AuthResult.Success(localId)
        } catch (ex: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("Incorrect email or password")
        } catch (ex: Exception) {
            Log.e("AuthRepository", "signIn failed", ex)
            AuthResult.Error("Unable to sign in securely. Check your connection and try again.")
        }
    }

    suspend fun signOut() {
        val uid = firebaseAuth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            try {
                firestoreProfile.updateFields(uid, mapOf("fcmToken" to FieldValue.delete()))
            } catch (ex: Exception) {
                Log.w("AuthRepository", "Unable to clear FCM token during sign-out", ex)
            }
        }
        firebaseAuth.signOut()
        clearLocalAccountState()
    }

    private suspend fun clearLocalAccountState() {
        try {
            withContext(Dispatchers.IO) { localDb.clearAllTables() }
        } catch (ex: Exception) {
            Log.e("AuthRepository", "Unable to clear account-scoped Room cache", ex)
        }
        session.clear()
    }

    suspend fun sendPasswordReset(email: String): String? {
        return try {
            firebaseAuth.sendPasswordResetEmail(email.trim().lowercase()).await()
            null
        } catch (ex: Exception) {
            Log.w("AuthRepository", "Password reset request failed", ex)
            "Unable to send the reset email right now. Please retry."
        }
    }

    /**
     * Returns an authorized profile view.
     *
     * Other members are fail-closed: before any Room-cached data is exposed we force a server
     * Firestore read. This re-evaluates block, stealth and per-member privacy rules and prevents a
     * profile hidden after an earlier view from remaining readable through a stale local cache.
     * The owner's own profile remains available from Room for normal offline account editing.
     */
    suspend fun currentProfile(userId: Long): UserProfile? {
        var local = userDao.findById(userId) ?: return null
        val signedInUid = firebaseAuth.currentUser?.uid.orEmpty()

        if (local.firebaseUid.isNotBlank() && local.firebaseUid != signedInUid) {
            try {
                val authorization = firestore.collection("users")
                    .document(local.firebaseUid)
                    .get(Source.SERVER)
                    .await()
                if (!authorization.exists()) return null

                // The successful server read also refreshes Firestore's cache. Hydrate the Room row
                // while preserving its stable local navigation id and private local account metadata.
                firestoreProfile.fetchProfile(local.firebaseUid)?.let { remote ->
                    val refreshed = remote.copy(
                        id = local.id,
                        email = local.email,
                        passwordHash = local.passwordHash,
                        isSeed = false
                    )
                    userDao.update(refreshed)
                    local = refreshed
                }
            } catch (ex: Exception) {
                Log.w("AuthRepository", "Profile access denied or unavailable for ${local.firebaseUid}", ex)
                return null
            }
        }

        return local.toUserProfile()
    }

    suspend fun updateFamilyDetails(
        userId: Long,
        fatherOccupation: String,
        motherOccupation: String,
        siblings: Int,
        familyType: String,
        familyValues: String,
        nativePlace: String,
        gotra: String,
        aboutFamily: String
    ) {
        val u = userDao.findById(userId) ?: return
        val updated = u.copy(
            fatherOccupation = fatherOccupation,
            motherOccupation = motherOccupation,
            siblings = siblings,
            familyType = familyType,
            familyValues = familyValues,
            gothra = gotra,
            city = nativePlace.ifBlank { u.city },
            aboutFamily = aboutFamily
        )
        userDao.update(updated)
        if (u.firebaseUid.isNotBlank()) {
            try { firestoreProfile.pushProfile(updated) } catch (ex: Exception) {
                Log.w("AuthRepository", "Family details cloud sync failed", ex)
            }
        }
    }

    suspend fun updateBio(userId: Long, bio: String) {
        val u = userDao.findById(userId) ?: return
        userDao.updateBio(userId, bio)
        if (u.firebaseUid.isNotBlank()) {
            try { firestoreProfile.updateFields(u.firebaseUid, mapOf("bio" to bio)) } catch (ex: Exception) {
                Log.w("AuthRepository", "Bio cloud sync failed", ex)
            }
        }
    }

    suspend fun updatePersonalityType(userId: Long, personalityType: String) {
        val u = userDao.findById(userId) ?: return
        userDao.update(u.copy(personalityType = personalityType))
        if (u.firebaseUid.isNotBlank()) {
            try { firestoreProfile.updateFields(u.firebaseUid, mapOf("personalityType" to personalityType)) } catch (ex: Exception) {
                Log.w("AuthRepository", "Personality cloud sync failed", ex)
            }
        }
    }

    suspend fun updateAccountDetails(
        userId: Long,
        displayName: String,
        city: String,
        profession: String,
        education: String
    ) {
        val u = userDao.findById(userId) ?: return
        val updated = u.copy(
            displayName = displayName.ifBlank { u.displayName },
            city = city.ifBlank { u.city },
            profession = profession.ifBlank { u.profession },
            education = education.ifBlank { u.education }
        )
        userDao.update(updated)
        if (u.firebaseUid.isNotBlank()) {
            try { firestoreProfile.pushProfile(updated) } catch (ex: Exception) {
                Log.w("AuthRepository", "Account details cloud sync failed", ex)
            }
        }
    }

    suspend fun getLikeCount(userId: Long): Int = likeDao.observeIncomingCountOnce(userId)

    suspend fun getViewCount(userId: Long): Int = userDao.findById(userId)?.profileViewCount ?: 0

    /**
     * Legacy API retained only for source compatibility.
     * Boost activation must come from PlayBillingManager -> verifyGooglePlayPurchase -> server
     * entitlement. Calling this path must never create a local-only or client-authored boost.
     */
    @Deprecated("Boost activation is server-authoritative through Google Play verification")
    suspend fun activateBoost(userId: Long, durationMs: Long = 24 * 60 * 60 * 1000L) {
        throw IllegalStateException(
            "Boost cannot be activated locally. Use the verified Google Play purchase flow."
        )
    }

    suspend fun isBoostActive(userId: Long): Boolean {
        val u = userDao.findById(userId) ?: return false
        return u.boostActiveUntil > System.currentTimeMillis()
    }

    suspend fun getBoostExpiryMs(userId: Long): Long = userDao.findById(userId)?.boostActiveUntil ?: 0L

    suspend fun getFirebaseUid(userId: Long): String = userDao.findById(userId)?.firebaseUid ?: ""

    suspend fun getMatrimonyPaused(): Boolean {
        return try {
            val result = com.google.firebase.functions.FirebaseFunctions.getInstance()
                .getHttpsCallable("getMyAccountLifecycle")
                .call()
                .await()
            @Suppress("UNCHECKED_CAST")
            val data = result.data as? Map<String, Any?>
            data?.get("paused") as? Boolean ?: false
        } catch (e: Exception) {
            Log.w("AuthRepository", "Unable to load matrimony lifecycle", e)
            false
        }
    }

    suspend fun setMatrimonyPaused(paused: Boolean): Result<Boolean> = runCatching {
        val result = com.google.firebase.functions.FirebaseFunctions.getInstance()
            .getHttpsCallable("setMatrimonyPaused")
            .call(mapOf("paused" to paused))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid lifecycle response")
        data["paused"] as? Boolean ?: error("Missing lifecycle state")
    }

    /**
     * Permanently erases the account through the restartable trusted backend cleanup.
     * Local Room state is wiped only after the server confirms remote erasure, so messages,
     * notes, searches, notifications and other cached rows cannot survive account deletion.
     */
    suspend fun deleteAccount(userId: Long): AuthResult {
        return try {
            com.google.firebase.functions.FirebaseFunctions.getInstance()
                .getHttpsCallable("deleteUserAccount")
                .call()
                .await()

            firebaseAuth.signOut()
            clearLocalAccountState()
            AuthResult.Success(userId)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Account deletion was not confirmed by server", e)
            AuthResult.Error("Account deletion could not be completed. Check your connection and retry.")
        }
    }

    suspend fun signInWithGoogle(idToken: String, displayName: String, email: String): AuthResult {
        val previousUid = firebaseAuth.currentUser?.uid
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUid = authResult.user?.uid
                ?: return AuthResult.Error("Google sign-in failed")

            if (!previousUid.isNullOrBlank() && previousUid != firebaseUid) {
                clearLocalAccountState()
            }

            val firestoreEntity = firestoreProfile.fetchProfile(firebaseUid)
            val normalizedEmail = email.trim().lowercase()
            val existingLocal = userDao.findByFirebaseUid(firebaseUid)
                ?: userDao.findByEmail(normalizedEmail)

            val localId: Long = when {
                existingLocal != null -> {
                    if (firestoreEntity != null) {
                        userDao.update(
                            firestoreEntity.copy(
                                id = existingLocal.id,
                                email = normalizedEmail.ifBlank { existingLocal.email },
                                passwordHash = "",
                                isSeed = false
                            )
                        )
                    } else if (existingLocal.firebaseUid != firebaseUid) {
                        userDao.update(existingLocal.copy(firebaseUid = firebaseUid, passwordHash = ""))
                    }
                    existingLocal.id
                }
                firestoreEntity != null -> userDao.insert(
                    firestoreEntity.copy(email = normalizedEmail, passwordHash = "", isSeed = false)
                )
                else -> {
                    val entity = UserEntity(
                        firebaseUid = firebaseUid,
                        email = normalizedEmail,
                        passwordHash = "",
                        displayName = displayName.ifBlank { email.substringBefore('@') },
                        age = 0,
                        gender = "OTHER",
                        lookingFor = "ANY",
                        city = "",
                        bio = "",
                        rasi = "",
                        nakshatra = "",
                        lastActiveAt = System.currentTimeMillis()
                    )
                    val id = userDao.insert(entity)
                    firestoreProfile.pushProfile(entity.copy(id = id))
                    id
                }
            }

            registerFcmToken(firebaseUid)
            session.setUser(localId)
            session.setFirebaseUid(firebaseUid)
            AuthResult.Success(localId)
        } catch (ex: Exception) {
            Log.e("AuthRepository", "signInWithGoogle failed", ex)
            AuthResult.Error(ex.message ?: "Google sign-in failed")
        }
    }

    private suspend fun UserEntity.toUserProfile() = UserProfile(
        id = id,
        firebaseUid = firebaseUid,
        email = if (email.endsWith("@cache.invalid")) "" else email,
        displayName = displayName,
        age = age,
        gender = runCatching { Gender.valueOf(gender) }.getOrDefault(Gender.OTHER),
        lookingFor = runCatching { LookingFor.valueOf(lookingFor) }.getOrDefault(LookingFor.ANY),
        city = city,
        bio = bio,
        rasi = rasi,
        nakshatra = nakshatra,
        hasQuestionnaire = questionnaireRepo.hasQuestionnaire(id),
        primaryPhotoPath = photoRepo.primaryPath(id),
        religion = religion,
        caste = caste,
        motherTongue = motherTongue,
        education = education,
        profession = profession,
        maritalStatus = maritalStatus,
        heightCm = heightCm,
        isVerified = isVerified,
        isPremium = isPremium,
        state = state,
        subCaste = subCaste,
        gothra = gothra,
        faithTradition = faithTradition,
        faithSubTradition = faithSubTradition,
        faithInstitution = faithInstitution,
        incomeBand = incomeBand,
        diet = diet,
        familyType = familyType,
        fatherOccupation = fatherOccupation,
        motherOccupation = motherOccupation,
        siblings = siblings,
        smoking = smoking,
        drinking = drinking,
        personalityType = personalityType,
        hobbies = hobbies.split(',').map { it.trim() }.filter { it.isNotBlank() },
        spokenLanguages = spokenLanguages.split(',').map { it.trim() }.filter { it.isNotBlank() },
        videoUrl = videoUrl,
        residentialStatus = residentialStatus,
        hasChildren = hasChildren,
        profileViewCount = profileViewCount,
        nativeState = nativeState,
        countryOfResidence = countryOfResidence,
        visaStatus = visaStatus,
        willingToRelocate = willingToRelocate,
        createdAt = createdAt,
        lastActiveAt = lastActiveAt,
        phoneNumber = phoneNumber,
        isIncognito = isIncognito,
        familyValues = familyValues,
        aboutFamily = aboutFamily,
        manglik = manglik,
        dateOfBirth = dateOfBirth,
        weight = weight,
        complexion = complexion,
        physicalStatus = physicalStatus,
        birthTime = birthTime,
        birthPlace = birthPlace,
        familyStatus = familyStatus,
        educationField = educationField,
        institution = institution,
        graduationYear = graduationYear,
        occupationCategory = occupationCategory,
        employer = employer,
        employerType = employerType,
        citizenship = citizenship,
        isNRI = isNRI,
        fitnessActivities = fitnessActivities,
        matrimonyId = matrimonyId,
        photoUrl = photoUrl,
        voiceBioUrl = voiceBioUrl,
        profileCompleteness = profileCompleteness,
        verificationLevel = verificationLevel,
        stealthMode = stealthMode,
        showLastActive = showLastActive,
        showHoroscope = showHoroscope,
        incomeDisclosure = incomeDisclosure,
        subscriptionPlan = subscriptionPlan,
        subscriptionExpiry = subscriptionExpiry,
        matchScore = matchScore,
        username = username
    )

    private suspend fun registerFcmToken(firebaseUid: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            firestoreProfile.saveFcmToken(firebaseUid, token)
        } catch (ex: Exception) {
            Log.w("AuthRepository", "FCM token registration failed", ex)
        }
    }
}
