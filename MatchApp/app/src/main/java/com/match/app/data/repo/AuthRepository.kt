package com.match.app.data.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.match.app.core.security.Passwords
import com.match.app.data.local.dao.LikeDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.UserEntity
import com.match.app.data.remote.FirestoreProfileService
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.Gender
import com.match.app.domain.model.LookingFor
import com.match.app.domain.model.UserProfile
import kotlinx.coroutines.tasks.await
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
    private val firestoreProfile: FirestoreProfileService
) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

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
            // 1. Create Firebase Auth account
            val authResult = firebaseAuth.createUserWithEmailAndPassword(e, password).await()
            val firebaseUid = authResult.user?.uid
                ?: return AuthResult.Error("Firebase account creation failed")

            // 2. Insert into Room (local cache)
            val entity = UserEntity(
                firebaseUid = firebaseUid,
                email = e,
                passwordHash = Passwords.hash(password),
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

            // 3. Push profile to Firestore
            firestoreProfile.pushProfile(entity.copy(id = localId))

            // 4. Register FCM token
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

        return try {
            // 1. Authenticate with Firebase Auth
            val authResult = firebaseAuth.signInWithEmailAndPassword(e, password).await()
            val firebaseUid = authResult.user?.uid
                ?: return AuthResult.Error("Firebase sign-in failed")

            // 2. Fetch profile from Firestore → cache in Room
            val firestoreEntity = firestoreProfile.fetchProfile(firebaseUid)

            val localUser = userDao.findByFirebaseUid(firebaseUid)
            val localId: Long

            if (localUser != null) {
                // Update local cache with Firestore data
                if (firestoreEntity != null) {
                    val updated = localUser.copy(
                        displayName = firestoreEntity.displayName,
                        age = firestoreEntity.age,
                        city = firestoreEntity.city,
                        bio = firestoreEntity.bio,
                        isPremium = firestoreEntity.isPremium,
                        isVerified = firestoreEntity.isVerified
                    )
                    userDao.update(updated)
                }
                localId = localUser.id
            } else if (firestoreEntity != null) {
                // First login on this device — insert from Firestore
                localId = userDao.insert(firestoreEntity.copy(
                    passwordHash = Passwords.hash(password)
                ))
            } else {
                // Fallback: check local Room by email (legacy accounts)
                val legacyUser = userDao.findByEmail(e)
                    ?: return AuthResult.Error("No account found. Please sign up first.")
                if (!Passwords.verify(password, legacyUser.passwordHash))
                    return AuthResult.Error("Incorrect password")
                // Migrate legacy user to Firebase
                val migratedUser = legacyUser.copy(firebaseUid = firebaseUid)
                userDao.update(migratedUser)
                firestoreProfile.pushProfile(migratedUser)
                localId = legacyUser.id
            }

            // 3. Register FCM token
            registerFcmToken(firebaseUid)

            // 4. Update last active timestamp
            userDao.updateLastActiveAt(firebaseUid, System.currentTimeMillis())

            session.setUser(localId)
            session.setFirebaseUid(firebaseUid)
            AuthResult.Success(localId)
        } catch (ex: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("Incorrect email or password")
        } catch (ex: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            AuthResult.Error("No account for that email")
        } catch (ex: Exception) {
            Log.e("AuthRepository", "signIn failed", ex)
            // Offline fallback: try local Room auth
            val u = userDao.findByEmail(e) ?: return AuthResult.Error(ex.message ?: "Sign-in failed")
            if (!Passwords.verify(password, u.passwordHash)) return AuthResult.Error("Incorrect password")
            session.setUser(u.id)
            if (u.firebaseUid.isNotBlank()) session.setFirebaseUid(u.firebaseUid)
            AuthResult.Success(u.id)
        }
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        session.clear()
    }

    /** Sends a password-reset email via Firebase Auth. Returns null on success or error message. */
    suspend fun sendPasswordReset(email: String): String? {
        return try {
            firebaseAuth.sendPasswordResetEmail(email.trim().lowercase()).await()
            null
        } catch (ex: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            "No account found for that email address."
        } catch (ex: Exception) {
            ex.message ?: "Failed to send reset email."
        }
    }

    suspend fun currentProfile(userId: Long): UserProfile? =
        userDao.findById(userId)?.let { u ->
            UserProfile(
                id = u.id, firebaseUid = u.firebaseUid, email = u.email, displayName = u.displayName,
                age = u.age,
                gender = runCatching { Gender.valueOf(u.gender) }.getOrDefault(Gender.OTHER),
                lookingFor = runCatching { LookingFor.valueOf(u.lookingFor) }.getOrDefault(LookingFor.ANY),
                city = u.city, bio = u.bio,
                rasi = u.rasi, nakshatra = u.nakshatra,
                hasQuestionnaire = questionnaireRepo.hasQuestionnaire(u.id),
                primaryPhotoPath = photoRepo.primaryPath(u.id),
                religion = u.religion, motherTongue = u.motherTongue,
                education = u.education, profession = u.profession,
                maritalStatus = u.maritalStatus, heightCm = u.heightCm,
                isVerified = u.isVerified, isPremium = u.isPremium,
                fatherOccupation = u.fatherOccupation,
                motherOccupation = u.motherOccupation,
                siblings = u.siblings,
                familyType = u.familyType,
                gothra = u.gothra,
                profileViewCount = u.profileViewCount,
                nativeState = u.nativeState,
                countryOfResidence = u.countryOfResidence,
                visaStatus = u.visaStatus,
                willingToRelocate = u.willingToRelocate,
                createdAt = u.createdAt,
                lastActiveAt = u.lastActiveAt,
                phoneNumber = u.phoneNumber,
                isIncognito = u.isIncognito,
                familyValues = u.familyValues,
                aboutFamily = u.aboutFamily,
                manglik = u.manglik,
                // Sprint 10 — physical & kundali
                dateOfBirth = u.dateOfBirth,
                weight = u.weight,
                complexion = u.complexion,
                physicalStatus = u.physicalStatus,
                birthTime = u.birthTime,
                birthPlace = u.birthPlace,
                // Sprint 10 — family
                familyStatus = u.familyStatus,
                // Sprint 10 — education & career
                educationField = u.educationField,
                institution = u.institution,
                graduationYear = u.graduationYear,
                occupationCategory = u.occupationCategory,
                employer = u.employer,
                employerType = u.employerType,
                // Sprint 10 — NRI & citizenship
                citizenship = u.citizenship,
                isNRI = u.isNRI,
                // Sprint 10 — lifestyle
                fitnessActivities = u.fitnessActivities,
                // Sprint 10 — platform
                matrimonyId = u.matrimonyId,
                photoUrl = u.photoUrl,
                voiceBioUrl = u.voiceBioUrl,
                // Sprint 10 — quality & privacy
                profileCompleteness = u.profileCompleteness,
                verificationLevel = u.verificationLevel,
                stealthMode = u.stealthMode,
                showLastActive = u.showLastActive,
                showHoroscope = u.showHoroscope,
                incomeDisclosure = u.incomeDisclosure,
                // Sprint 10 — subscription & matching
                subscriptionPlan = u.subscriptionPlan,
                subscriptionExpiry = u.subscriptionExpiry,
                matchScore = u.matchScore
            )
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
        // Sync to Firestore
        if (u.firebaseUid.isNotBlank()) {
            try { firestoreProfile.pushProfile(updated) } catch (_: Exception) {}
        }
    }

    suspend fun updateBio(userId: Long, bio: String) {
        val u = userDao.findById(userId) ?: return
        userDao.updateBio(userId, bio)
        if (u.firebaseUid.isNotBlank()) {
            try { firestoreProfile.updateFields(u.firebaseUid, mapOf("bio" to bio)) } catch (_: Exception) {}
        }
    }

    suspend fun updatePersonalityType(userId: Long, personalityType: String) {
        val u = userDao.findById(userId) ?: return
        userDao.update(u.copy(personalityType = personalityType))
        if (u.firebaseUid.isNotBlank()) {
            try { firestoreProfile.updateFields(u.firebaseUid, mapOf("personalityType" to personalityType)) } catch (_: Exception) {}
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
            try { firestoreProfile.pushProfile(updated) } catch (_: Exception) {}
        }
    }

    suspend fun getLikeCount(userId: Long): Int =
        likeDao.observeIncomingCountOnce(userId)

    suspend fun getViewCount(userId: Long): Int =
        userDao.findById(userId)?.profileViewCount ?: 0

    suspend fun activateBoost(userId: Long, durationMs: Long = 24 * 60 * 60 * 1000L) {
        val u = userDao.findById(userId) ?: return
        val boostUntil = System.currentTimeMillis() + durationMs
        userDao.updateBoostExpiry(userId, boostUntil)
        if (u.firebaseUid.isNotBlank()) {
            try { firestoreProfile.updateFields(u.firebaseUid, mapOf("boostActiveUntil" to boostUntil)) } catch (_: Exception) {}
        }
    }

    suspend fun isBoostActive(userId: Long): Boolean {
        val u = userDao.findById(userId) ?: return false
        return u.boostActiveUntil > System.currentTimeMillis()
    }

    suspend fun getBoostExpiryMs(userId: Long): Long =
        userDao.findById(userId)?.boostActiveUntil ?: 0L

    /** Get the Firebase UID for a local user ID. */
    suspend fun getFirebaseUid(userId: Long): String =
        userDao.findById(userId)?.firebaseUid ?: ""

    /**
     * Permanently deletes the user's account from local Room DB, Firestore, and Firebase Auth.
     *
     * Server-side cascading delete (interests, matches, shortlists, blocks, subscriptions,
     * FCM tokens, auth) is performed atomically by the `deleteUserAccount` Cloud Function
     * to satisfy DPDP Act 2023 right-to-erasure within 30 days.
     */
    suspend fun deleteAccount(userId: Long): AuthResult {
        return try {
            val u = userDao.findById(userId)
            // 1. Server-side cascade (auth + all collections).
            // The callable function uses request.auth.uid so the user can only delete themselves.
            try {
                com.google.firebase.functions.FirebaseFunctions.getInstance()
                    .getHttpsCallable("deleteUserAccount")
                    .call()
                    .await()
            } catch (ex: Exception) {
                Log.w("AuthRepository", "Server-side delete failed; falling back to client-only", ex)
                // Best-effort client fallback so the user is at least signed out & local-wiped.
                if (u != null && u.firebaseUid.isNotBlank()) {
                    try { firestoreProfile.deleteProfile(u.firebaseUid) } catch (_: Exception) {}
                }
                try { firebaseAuth.currentUser?.delete()?.await() } catch (_: Exception) {}
            }
            // 2. Local Room wipe + session clear (always run, regardless of network).
            userDao.deleteById(userId)
            session.clear()
            AuthResult.Success(userId)
        } catch (e: Exception) {
            AuthResult.Error("Failed to delete account: ${e.message}")
        }
    }

    /**
     * Authenticate using a Google ID token obtained from Credential Manager.
     * Creates or links the Firebase Auth account, then ensures a local Room user exists.
     */
    suspend fun signInWithGoogle(idToken: String, displayName: String, email: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUid = authResult.user?.uid
                ?: return AuthResult.Error("Google sign-in failed")

            // Fetch or create local user
            val firestoreEntity = firestoreProfile.fetchProfile(firebaseUid)
            val existingLocal = userDao.findByFirebaseUid(firebaseUid)
                ?: userDao.findByEmail(email.trim().lowercase())

            val localId: Long = when {
                existingLocal != null -> {
                    // Update with latest Firestore data
                    if (firestoreEntity != null) {
                        userDao.update(existingLocal.copy(
                            firebaseUid = firebaseUid,
                            displayName = firestoreEntity.displayName.ifBlank { displayName },
                            isPremium = firestoreEntity.isPremium,
                            isVerified = firestoreEntity.isVerified
                        ))
                    }
                    existingLocal.id
                }
                firestoreEntity != null -> {
                    // First login on this device — insert from Firestore
                    userDao.insert(firestoreEntity)
                }
                else -> {
                    // Brand-new user via Google — create profile
                    val entity = UserEntity(
                        firebaseUid = firebaseUid,
                        email = email.trim().lowercase(),
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

    private suspend fun registerFcmToken(firebaseUid: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            firestoreProfile.saveFcmToken(firebaseUid, token)
        } catch (ex: Exception) {
            Log.w("AuthRepository", "FCM token registration failed", ex)
        }
    }
}
