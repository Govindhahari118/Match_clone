package com.match.app.data.repo

import com.match.app.data.local.dao.SavedSearchDao
import com.match.app.data.local.entity.SavedSearchEntity
import com.match.app.domain.model.MatchFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedSearchRepository @Inject constructor(
    private val dao: SavedSearchDao
) {
    fun observeForUser(userId: Long): Flow<List<SavedSearchEntity>> = dao.observeForUser(userId)

    suspend fun save(userId: Long, name: String, filter: MatchFilter) = withContext(Dispatchers.IO) {
        val cleanName = name.trim().take(60).ifBlank { "Saved search" }
        dao.save(
            SavedSearchEntity(
                userId = userId,
                name = cleanName,
                minAge = filter.ageMin,
                maxAge = filter.ageMax,
                city = filter.city,
                state = filter.state,
                religions = filter.religion,
                castes = filter.caste,
                motherTongues = filter.motherTongue,
                diet = filter.diet,
                smoking = filter.smoking,
                drinking = filter.drinking,
                incomeBand = filter.incomeMin,
                education = filter.educationLevel,
                maritalStatus = filter.maritalStatus,
                subCaste = filter.subCaste,
                minScore = filter.minScore,
                verifiedOnly = filter.verifiedOnly,
                incomeMax = filter.incomeMax,
                residentialStatus = filter.residentialStatus,
                hasChildren = filter.hasChildren,
                keyword = filter.keyword,
                gothra = filter.gothra,
                nativeState = filter.nativeState,
                countryOfResidence = filter.countryOfResidence,
                nriOnly = filter.nriOnly,
                willingToRelocate = filter.willingToRelocate,
                recentlyJoinedDays = filter.recentlyJoinedDays,
                familyType = filter.familyType,
                familyStatus = filter.familyStatus,
                physicalStatus = filter.physicalStatus,
                hasChildrenFilter = filter.hasChildrenFilter,
                citizenship = filter.citizenship,
                nriStatus = filter.nriStatus,
                educationField = filter.educationField,
                occupationCategory = filter.occupationCategory,
                employerType = filter.employerType,
                nakshatra = filter.nakshatra,
                rasi = filter.rasi,
                manglik = filter.manglik,
                hobbies = filter.hobbies,
                withPhotoOnly = filter.withPhotoOnly,
                verifiedLevel = filter.verifiedLevel,
                premiumOnly = filter.premiumOnly,
                lastActiveWithinDays = filter.lastActiveWithinDays,
                minPoruthamScore = filter.minPoruthamScore,
                hasHoroscope = filter.hasHoroscope
            )
        )
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) { dao.deleteById(id) }

    fun toFilter(entity: SavedSearchEntity): MatchFilter = MatchFilter(
        ageMin = entity.minAge,
        ageMax = entity.maxAge,
        city = entity.city,
        state = entity.state,
        caste = entity.castes,
        minScore = entity.minScore,
        religion = entity.religions,
        motherTongue = entity.motherTongues,
        maritalStatus = entity.maritalStatus,
        verifiedOnly = entity.verifiedOnly,
        incomeMin = entity.incomeBand,
        incomeMax = entity.incomeMax,
        educationLevel = entity.education,
        diet = entity.diet,
        residentialStatus = entity.residentialStatus,
        hasChildren = entity.hasChildren,
        keyword = entity.keyword,
        gothra = entity.gothra,
        nativeState = entity.nativeState,
        countryOfResidence = entity.countryOfResidence,
        nriOnly = entity.nriOnly,
        willingToRelocate = entity.willingToRelocate,
        recentlyJoinedDays = entity.recentlyJoinedDays,
        smoking = entity.smoking,
        drinking = entity.drinking,
        familyType = entity.familyType,
        familyStatus = entity.familyStatus,
        physicalStatus = entity.physicalStatus,
        hasChildrenFilter = entity.hasChildrenFilter,
        citizenship = entity.citizenship,
        nriStatus = entity.nriStatus,
        educationField = entity.educationField,
        occupationCategory = entity.occupationCategory,
        employerType = entity.employerType,
        subCaste = entity.subCaste,
        nakshatra = entity.nakshatra,
        rasi = entity.rasi,
        manglik = entity.manglik,
        hobbies = entity.hobbies,
        withPhotoOnly = entity.withPhotoOnly,
        verifiedLevel = entity.verifiedLevel,
        premiumOnly = entity.premiumOnly,
        lastActiveWithinDays = entity.lastActiveWithinDays,
        minPoruthamScore = entity.minPoruthamScore,
        hasHoroscope = entity.hasHoroscope
    )
}
