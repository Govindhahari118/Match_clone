package com.match.app.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.google.firebase.auth.FirebaseAuth
import com.match.app.core.matching.Astrology
import com.match.app.core.matching.MatchScorer
import com.match.app.core.matching.Vectors
import com.match.app.data.local.Vec
import com.match.app.data.local.dao.QuestionnaireDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.QuestionnaireEntity
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

    suspend fun discoverPaged(filter: MatchFilter = MatchFilter()): Flow<PagingData<UserProfile>> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val me = uid.let { userDao.findByFirebaseUid(it) }
        val myGender = me?.gender ?: "MALE"
        val myLookingFor = me?.lookingFor ?: "FEMALE"
        val blockedUids = blockService.getBlockedUids(uid)
        val likedUids = interestService.getSentInterestUids(uid)

        return Pager(
            config = PagingConfig(pageSize = FirestorePagingSource.PAGE_SIZE, enablePlaceholders = false),
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
        ).flow.map { data -> data.map { it.toProfile() } }
    }

    suspend fun myCity(userId: Long): String = userDao.findById(userId)?.city ?: ""
    suspend fun myState(userId: Long): String = userDao.findById(userId)?.state ?: ""

    suspend fun recommendations(
        seekerId: Long,
        mode: MatchMode,
        filter: MatchFilter = MatchFilter()
    ): List<MatchResult> = withContext(Dispatchers.Default) {
        val seekerEntity = userDao.findById(seekerId) ?: return@withContext emptyList()
        val blockedIds = social.blockedIds(seekerId).toSet()
        val qMap = qDao.all().associateBy { it.userId }
        val seekerProfile = seekerEntity.toProfile(qMap)
        val now = System.currentTimeMillis()

        userDao.allExcluding(seekerId)
            .asSequence()
            .filter { it.id !in blockedIds }
            .filter { genderFilter(seekerEntity, it) }
            .filter { filter.city.isBlank() || it.city.equals(filter.city, true) }
            .filter { filter.state.isBlank() || it.state.equals(filter.state, true) }
            .filter { filter.caste.isBlank() || it.caste.equals(filter.caste, true) }
            .filter { filter.subCaste.isBlank() || it.subCaste.equals(filter.subCaste, true) }
            .filter { it.age in filter.ageMin..filter.ageMax }
            .filter { filter.religion.isBlank() || it.religion.equals(filter.religion, true) }
            .filter { filter.motherTongue.isBlank() || it.motherTongue.equals(filter.motherTongue, true) }
            .filter { filter.maritalStatus.isBlank() || it.maritalStatus.equals(filter.maritalStatus, true) }
            .filter { !filter.verifiedOnly || it.isVerified }
            .filter { filter.verifiedLevel <= 0 || it.verificationLevel >= filter.verifiedLevel }
            .filter { !filter.premiumOnly || it.isPremium }
            .filter { !filter.withPhotoOnly || it.photoUrl.isNotBlank() }
            .filter { filter.diet.isBlank() || it.diet.equals(filter.diet, true) }
            .filter { filter.educationLevel.isBlank() || it.education.equals(filter.educationLevel, true) }
            .filter { filter.educationField.isBlank() || it.educationField.equals(filter.educationField, true) }
            .filter { filter.occupationCategory.isBlank() || it.occupationCategory.equals(filter.occupationCategory, true) }
            .filter { filter.employerType.isBlank() || it.employerType.equals(filter.employerType, true) }
            .filter { filter.residentialStatus.isBlank() || it.residentialStatus.equals(filter.residentialStatus, true) }
            .filter { filter.gothra.isBlank() || it.gothra.equals(filter.gothra, true) }
            .filter { filter.smoking.isBlank() || it.smoking.equals(filter.smoking, true) }
            .filter { filter.drinking.isBlank() || it.drinking.equals(filter.drinking, true) }
            .filter { filter.familyType.isBlank() || it.familyType.equals(filter.familyType, true) }
            .filter { filter.familyStatus.isBlank() || it.familyStatus.equals(filter.familyStatus, true) }
            .filter { filter.physicalStatus.isBlank() || it.physicalStatus.equals(filter.physicalStatus, true) }
            .filter { filter.citizenship.isBlank() || it.citizenship.equals(filter.citizenship, true) }
            .filter { filter.nakshatra.isBlank() || it.nakshatra.equals(filter.nakshatra, true) }
            .filter { filter.rasi.isBlank() || it.rasi.equals(filter.rasi, true) }
            .filter { filter.manglik.isBlank() || it.manglik.equals(filter.manglik, true) }
            .filter { filter.hobbies.isBlank() || it.hobbies.contains(filter.hobbies, true) }
            .filter { filter.keyword.isBlank() || it.displayName.contains(filter.keyword, true) || it.bio.contains(filter.keyword, true) || it.profession.contains(filter.keyword, true) }
            .filter { filter.hasChildren.isBlank() || filter.hasChildren.equals("Any", true) || (filter.hasChildren.equals("Yes", true) == it.hasChildren) }
            .filter {
                filter.hasChildrenFilter.isBlank() || filter.hasChildrenFilter.equals("Don't mind", true) ||
                    when {
                        filter.hasChildrenFilter.startsWith("No", true) -> !it.hasChildren
                        filter.hasChildrenFilter.startsWith("Yes", true) -> it.hasChildren
                        else -> true
                    }
            }
            .filter { filter.nativeState.isBlank() || it.nativeState.equals(filter.nativeState, true) }
            .filter { filter.countryOfResidence.isBlank() || it.countryOfResidence.equals(filter.countryOfResidence, true) }
            .filter { !filter.nriOnly || it.isNRI || (it.countryOfResidence.isNotBlank() && !it.countryOfResidence.equals("India", true)) }
            .filter {
                when (filter.nriStatus.lowercase()) {
                    "only" -> it.isNRI
                    "exclude" -> !it.isNRI
                    else -> true
                }
            }
            .filter { !filter.willingToRelocate || it.willingToRelocate }
            .filter { filter.incomeMin.isBlank() || it.incomeBand.contains(filter.incomeMin, true) }
            .filter { filter.recentlyJoinedDays <= 0 || (now - it.createdAt) <= filter.recentlyJoinedDays * DAY_MS }
            .filter { filter.lastActiveWithinDays <= 0 || (now - it.lastActiveAt) <= filter.lastActiveWithinDays * DAY_MS }
            .filter {
                when (filter.hasHoroscope.lowercase()) {
                    "yes" -> it.rasi.isNotBlank() && it.nakshatra.isNotBlank()
                    "no" -> it.rasi.isBlank() && it.nakshatra.isBlank()
                    else -> true
                }
            }
            .map { candidate ->
                val candidateProfile = candidate.toProfile(qMap)
                val qScore = if (seekerProfile.selfVector != null && seekerProfile.partnerVector != null) {
                    qMap[candidate.id]?.let { q ->
                        Vectors.questionnaireScore(
                            seekerProfile.selfVector,
                            seekerProfile.partnerVector,
                            Vec.decode(q.selfVector),
                            Vec.decode(q.partnerVector)
                        )
                    } ?: 0f
                } else 0f
                val astro = Astrology.score(seekerEntity.rasi, seekerEntity.nakshatra, candidate.rasi, candidate.nakshatra)
                MatchResult(
                    user = candidateProfile,
                    questionnaireScore = qScore,
                    astrologyScore = astro,
                    combinedScore = MatchScorer.calculate(seekerProfile, candidateProfile).toFloat() / 100f,
                    mode = mode
                )
            }
            .filter { filter.minScore <= 0f || it.combinedScore >= filter.minScore }
            .sortedByDescending { it.combinedScore }
            .take(200)
            .toList()
    }

    private fun safeGender(raw: String): Gender = runCatching { Gender.valueOf(raw) }.getOrDefault(Gender.OTHER)
    private fun safeLookingFor(raw: String): LookingFor = runCatching { LookingFor.valueOf(raw) }.getOrDefault(LookingFor.ANY)

    private fun genderFilter(seeker: UserEntity, candidate: UserEntity): Boolean {
        val wants = safeLookingFor(seeker.lookingFor)
        val candidateGender = safeGender(candidate.gender)
        val seekerGender = safeGender(seeker.gender)
        val candidateWants = safeLookingFor(candidate.lookingFor)
        val seekerAccepts = when (wants) {
            LookingFor.ANY -> true
            LookingFor.MALE -> candidateGender == Gender.MALE
            LookingFor.FEMALE -> candidateGender == Gender.FEMALE
        }
        val candidateAccepts = when (candidateWants) {
            LookingFor.ANY -> true
            LookingFor.MALE -> seekerGender == Gender.MALE
            LookingFor.FEMALE -> seekerGender == Gender.FEMALE
        }
        return seekerAccepts && candidateAccepts
    }

    private fun UserEntity.toProfile(qMap: Map<Long, QuestionnaireEntity> = emptyMap()) = UserProfile(
        id = id,
        firebaseUid = firebaseUid,
        email = email,
        displayName = displayName,
        age = age,
        gender = safeGender(gender),
        lookingFor = safeLookingFor(lookingFor),
        city = city,
        bio = bio,
        rasi = rasi,
        nakshatra = nakshatra,
        hasQuestionnaire = id in qMap,
        primaryPhotoPath = photoUrl.ifBlank { null },
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
        selfVector = qMap[id]?.let { Vec.decode(it.selfVector) },
        partnerVector = qMap[id]?.let { Vec.decode(it.partnerVector) },
        profileCompleteness = profileCompleteness,
        verificationLevel = verificationLevel,
        stealthMode = stealthMode,
        showLastActive = showLastActive,
        showHoroscope = showHoroscope,
        incomeDisclosure = incomeDisclosure,
        subscriptionPlan = subscriptionPlan,
        subscriptionExpiry = subscriptionExpiry,
        matchScore = matchScore
    )

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
