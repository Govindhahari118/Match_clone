package com.match.app.data.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
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
            // Firebase Auth is the sole password authority. Never persist password verifiers locally.
            val authResult = firebaseAuth.createUserWithEmailAndPassword(e, password).await()
            val firebaseUid = authResult.user?.uid
                ?: return AuthResult.Error("Firebase account creation failed")

            // Room is an offline profile cache only.
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

        return try {
            // Explicit sign-in always requires Firebase authentication. We do not fall back to a
            // locally cached password because a disabled/deleted remote account must not regain access.
            val authResult = firebaseAuth.signInWithEmailAndPassword(e, password).await()
            val firebaseUid = authResult.user?.uid
                ?: return AuthResult.Error("Firebase sign-in failed")

            val firestoreEntity = firestoreProfile.fetchProfile(firebaseUid)
            val localUser = userDao.findByFirebaseUid(firebaseUid)
            val localId: Long

            if (localUser != null) {
                if (firestoreEntity != null) {
                    val updated = localUser.copy(
                        passwordHash = "",
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
                localId = userDao.insert(firestoreEntity.copy(passwordHash = ""))
            } else {
                // A legacy local profile may be attached only after Firebase has already authenticated
                // this email/password pair. Its old local password hash is discarded during migration.
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
        } catch (ex: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            AuthResult.Error("No account for that email")
        } catch (ex: Exception) {
            Log.e("AuthRepository", "signIn failed", ex)
            AuthResult.Error("Unable to sign in securely. Check your connection and try again.")
        }
    }

    suspend fun signOut() {
        // Revoke this device's notification destination while the user is still authenticated.
        val uid = firebaseAuth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            try {
                firestoreProfile.updateFields(uid, mapOf("fcmToken" to FieldValue.delete()))
            } catch (ex: Exception) {
                Log.w("AuthRepository", "Unable to clear FCM token during sign-out", ex)
            }
        }
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
                dateOfBirth = u.dateOfBirth,
                weight = u.weight,
                complexion = u.complexion,
                physicalStatus = u.physicalStatus,
                birthTime = u.birthTime,
                birthPlace = u.birthPlace,
                familyStatus = u.familyStatus,
                educationField = u.educationField,
                institution = u.institution,
                graduationYear = u.graduationYear,
                occupationCategory = u.occupationCategory,
                employer = u.employer,
                employerType = u.employerType,
                citizenship = u.citizenship,
                isNRI = u.isNRI,
                fitnessActivities = u.fitnessActivities,
                matrimonyId = u.matrimonyId,
                photoUrl = u.photoUrl,
                voiceBioUrl = u.voiceBioUrl,
                profileCompleteness = u.profileCompleteness,
                verificationLevel = u.verificationLevel,
                stealthMode = u.stealthMode,
                showLastActive = u.showLastActive,
                showHoroscope = u.showHoroscope,
                incomeDisclosure = u.incomeDisclosure,
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

    suspend fun getFirebaseUid(userId: Long): String =
        userDao.findById(userId)?.firebaseUid ?: ""

    /**
     * Permanently erases the account through the restartable trusted backend cleanup.
     * Local state is removed only after the server confirms completion; a network/backend failure
     * is surfaced so the user can retry instead of being shown a false deletion success.
     */
    suspend fun deleteAccount(userId: Long): AuthResult {
        return try {
            com.google.firebase.functions.FirebaseFunctions.getInstance()
                .getHttpsCallable("deleteUserAccount")
                .call()
                .await()

            userDao.deleteById(userId)
            firebaseAuth.signOut()
            session.clear()
            AuthResult.Success(userId)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Account deletion was not confirmed by server", e)
            AuthResult.Error("Account deletion could not be completed. Check your connection and retry.")
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

            val firestoreEntity = firestoreProfile.fetchProfile(firebaseUid)
            val existingLocal = userDao.findByFirebaseUid(firebaseUid)
                ?: userDao.findByEmail(email.trim().lowercase())

            val localId: Long = when {
                existingLocal != null -> {
                    if (firestoreEntity != null) {
                        userDao.update(existingLocal.copy(
                            firebaseUid = firebaseUid,
                            passwordHash = "",
                            displayName = firestoreEntity.displayName.ifBlank { displayName },
                            isPremium = firestoreEntity.isPremium,
                            isVerified = firestoreEntity.isVerified
                        ))
                    }
                    existingLocal.id
                }
                firestoreEntity != null -> {
                    userDao.insert(firestoreEntity.copy(passwordHash = ""))
                }
                else -> {
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
