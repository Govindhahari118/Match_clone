package com.match.app.data.repo

import com.match.app.core.matching.Astrology
import com.match.app.core.matching.MatchScorer
import com.match.app.core.matching.Vectors
import com.match.app.data.local.Vec
import com.match.app.data.local.dao.QuestionnaireDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.UserEntity
import com.match.app.data.remote.FirestoreBlockService
import com.match.app.data.remote.FirestoreInterestService
import com.match.app.data.remote.FirestorePagingSource
import com.match.app.domain.model.Gender
import com.match.app.domain.model.LookingFor
import com.match.app.domain.model.MatchFilter
import com.match.app.domain.model.MatchMode
import com.match.app.domain.model.MatchResult
import com.match.app.domain.model.UserProfile
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchingRepository @Inject constructor(
    private val userDao: UserDao,
    private val qDao: QuestionnaireDao,
    private val social: SocialRepository,
    private val blockService: FirestoreBlockService,
    private val interestService: FirestoreInterestService
) {

    /**
     * Firestore-backed paginated discovery feed.
     * Returns a Flow of PagingData<UserProfile> for Compose LazyColumn.
     */
    suspend fun discoverPaged(filter: MatchFilter = MatchFilter()): Flow<PagingData<UserProfile>> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val me = uid.let { u -> userDao.findByFirebaseUid(u) }
        val myGender = me?.gender ?: "MALE"
        val myLookingFor = me?.lookingFor ?: "FEMALE"
        val blockedUids = blockService.getBlockedUids(uid)
        val likedUids = interestService.getSentInterestUids(uid)

        return Pager(
            config = PagingConfig(
                pageSize = FirestorePagingSource.PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                FirestorePagingSource(
                    myUid = uid,
                    myGender = myGender,
                    myLookingFor = myLookingFor,
                    filter = filter,
                    blockedUids = blockedUids,
                    likedUids = likedUids
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { entity ->
                entity.toProfile()
            }
        }
    }

    /** Quick lookup for the current user's city (used by NearbyMatchesScreen). */
    suspend fun myCity(userId: Long): String = userDao.findById(userId)?.city ?: ""
    /** Quick lookup for the current user's state. */
    suspend fun myState(userId: Long): String = userDao.findById(userId)?.state ?: ""

    suspend fun recommendations(
        seekerId: Long,
        mode: MatchMode,
        filter: MatchFilter = MatchFilter()
    ): List<MatchResult> = withContext(Dispatchers.Default) {
        val seekerEntity = userDao.findById(seekerId) ?: return@withContext emptyList()
        val blockedIds   = social.blockedIds(seekerId).toSet()
        
        // Fetch all questionnaire data for scoring
        val allQuestionnaires = qDao.all()
        val qMap = allQuestionnaires.associateBy { it.userId }
        val seekerProfile = seekerEntity.toProfile(qMap)

        val all = userDao.allExcluding(seekerId)
            .filter { it.id !in blockedIds }
            .filter { genderFilter(seekerEntity, it) }
            .filter { filter.city.isBlank() || it.city.equals(filter.city, ignoreCase = true) }
            .filter { filter.state.isBlank() || it.state.equals(filter.state, ignoreCase = true) }
            .filter { filter.caste.isBlank() || it.caste.equals(filter.caste, ignoreCase = true) }
            .filter { it.age in filter.ageMin..filter.ageMax }
            .filter { filter.religion.isBlank() || it.religion.equals(filter.religion, ignoreCase = true) }
            .filter { filter.motherTongue.isBlank() || it.motherTongue.equals(filter.motherTongue, ignoreCase = true) }
            .filter { filter.maritalStatus.isBlank() || it.maritalStatus.equals(filter.maritalStatus, ignoreCase = true) }
            .filter { !filter.verifiedOnly || it.isVerified }
            .filter { filter.diet.isBlank() || it.diet.equals(filter.diet, ignoreCase = true) }
            .filter { filter.educationLevel.isBlank() || it.education.equals(filter.educationLevel, ignoreCase = true) }
            .filter { filter.residentialStatus.isBlank() || it.residentialStatus.equals(filter.residentialStatus, ignoreCase = true) }
            .filter { filter.gothra.isBlank() || it.gothra.equals(filter.gothra, ignoreCase = true) }
            .filter { filter.keyword.isBlank() || it.displayName.contains(filter.keyword, ignoreCase = true) || it.bio.contains(filter.keyword, ignoreCase = true) || it.profession.contains(filter.keyword, ignoreCase = true) }
            .filter { filter.hasChildren.isBlank() || (filter.hasChildren == "Yes") == it.hasChildren || filter.hasChildren == "Any" }
            // Sprint 5: Family origin / native state filter
            .filter { filter.nativeState.isBlank() || it.nativeState.equals(filter.nativeState, ignoreCase = true) }
            // Sprint 6: NRI filters
            .filter { filter.countryOfResidence.isBlank() || it.countryOfResidence.equals(filter.countryOfResidence, ignoreCase = true) }
            .filter { !filter.nriOnly || it.countryOfResidence.isNotBlank() && !it.countryOfResidence.equals("India", ignoreCase = true) }
            .filter { !filter.willingToRelocate || it.willingToRelocate }
            // Sprint 10+: Extended filters
            .filter { filter.occupationCategory.isBlank() || it.occupationCategory.equals(filter.occupationCategory, ignoreCase = true) }
            .filter { filter.smoking.isBlank() || it.smoking.equals(filter.smoking, ignoreCase = true) }
            .filter { filter.manglik.isBlank() || it.manglik.equals(filter.manglik, ignoreCase = true) }
            .filter { filter.rasi.isBlank() || it.rasi.equals(filter.rasi, ignoreCase = true) }
            .filter { filter.incomeMin.isBlank() || it.incomeBand.contains(filter.incomeMin, ignoreCase = true) }
            // Sprint 6: Recently joined filter
            .filter { filter.recentlyJoinedDays <= 0 || (System.currentTimeMillis() - it.createdAt) <= filter.recentlyJoinedDays * 24 * 60 * 60 * 1000L }

        all.map { u ->
            val uProfile = u.toProfile(qMap)
            val finalScore = MatchScorer.calculate(seekerProfile, uProfile)
            
            val astro = Astrology.score(seekerEntity.rasi, seekerEntity.nakshatra, u.rasi, u.nakshatra)
            val qScore: Float = if (seekerProfile.selfVector != null && seekerProfile.partnerVector != null) {
                qMap[u.id]?.let { cq ->
                    Vectors.questionnaireScore(
                        seekerProfile.selfVector, 
                        seekerProfile.partnerVector, 
                        Vec.decode(cq.selfVector), 
                        Vec.decode(cq.partnerVector)
                    )
                } ?: 0f
            } else 0f

            MatchResult(
                user = uProfile,
                questionnaireScore = qScore,
                astrologyScore = astro,
                combinedScore = finalScore.toFloat() / 100f,
                mode = mode
            )
        }.sortedByDescending { it.combinedScore }.take(200)
    }

    /** Safe enum lookup that falls back instead of crashing on malformed DB values. */
    private fun safeGender(raw: String): Gender =
        runCatching { Gender.valueOf(raw) }.getOrDefault(Gender.OTHER)

    private fun safeLookingFor(raw: String): LookingFor =
        runCatching { LookingFor.valueOf(raw) }.getOrDefault(LookingFor.ANY)

    private fun genderFilter(seeker: UserEntity, c: UserEntity): Boolean {
        val wants = safeLookingFor(seeker.lookingFor)
        val candG = safeGender(c.gender)
        val seekG = safeGender(seeker.gender)
        val candW = safeLookingFor(c.lookingFor)
        val seekerAccepts = when (wants) { LookingFor.ANY -> true; LookingFor.MALE -> candG == Gender.MALE; LookingFor.FEMALE -> candG == Gender.FEMALE }
        val candAccepts   = when (candW)  { LookingFor.ANY -> true; LookingFor.MALE -> seekG == Gender.MALE;  LookingFor.FEMALE -> seekG == Gender.FEMALE }
        return seekerAccepts && candAccepts
    }

    private fun UserEntity.toProfile(qMap: Map<Long, com.match.app.data.local.entity.QuestionnaireEntity> = emptyMap()) = UserProfile(
        id = id, firebaseUid = firebaseUid, email = email, displayName = displayName, age = age,
        gender = safeGender(gender),
        lookingFor = safeLookingFor(lookingFor),
        city = city, bio = bio, rasi = rasi, nakshatra = nakshatra,
        hasQuestionnaire = id in qMap,
        selfVector = qMap[id]?.let { Vec.decode(it.selfVector) },
        partnerVector = qMap[id]?.let { Vec.decode(it.partnerVector) },
        religion = religion, caste = caste, motherTongue = motherTongue,
        education = education, profession = profession,
        maritalStatus = maritalStatus, heightCm = heightCm,
        isVerified = isVerified, isPremium = isPremium, state = state,
        subCaste = subCaste, gothra = gothra, incomeBand = incomeBand,
        diet = diet, familyType = familyType,
        fatherOccupation = fatherOccupation, motherOccupation = motherOccupation,
        siblings = siblings, smoking = smoking, drinking = drinking,
        personalityType = personalityType,
        hobbies = if (hobbies.isBlank()) emptyList() else hobbies.split(",").filter { it.isNotBlank() },
        spokenLanguages = if (spokenLanguages.isBlank()) emptyList() else spokenLanguages.split(",").filter { it.isNotBlank() },
        videoUrl = videoUrl, residentialStatus = residentialStatus,
        hasChildren = hasChildren,
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
        manglik = manglik
    )
}
