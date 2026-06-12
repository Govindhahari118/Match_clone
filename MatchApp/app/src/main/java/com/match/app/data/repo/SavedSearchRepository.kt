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
    fun observeForUser(userId: Long): Flow<List<SavedSearchEntity>> =
        dao.observeForUser(userId)

    suspend fun save(userId: Long, name: String, filter: MatchFilter) =
        withContext(Dispatchers.IO) {
            val entity = SavedSearchEntity(
                userId        = userId,
                name          = name.trim().ifBlank { "Search ${System.currentTimeMillis()}" },
                minAge        = filter.ageMin,
                maxAge        = filter.ageMax,
                city          = filter.city,
                state         = filter.state,
                religions     = filter.religion,
                castes        = filter.caste,
                motherTongues = filter.motherTongue,
                diet          = filter.diet,
                maritalStatus = filter.maritalStatus,
                education     = filter.educationLevel,
                incomeBand    = filter.incomeMin
            )
            dao.save(entity)
        }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) { dao.deleteById(id) }

    /** Convert a saved entity back to a MatchFilter for applying. */
    fun toFilter(entity: SavedSearchEntity): MatchFilter = MatchFilter(
        ageMin        = entity.minAge,
        ageMax        = entity.maxAge,
        city          = entity.city,
        state         = entity.state,
        religion      = entity.religions,
        caste         = entity.castes,
        motherTongue  = entity.motherTongues,
        diet          = entity.diet,
        maritalStatus = entity.maritalStatus,
        educationLevel = entity.education,
        incomeMin     = entity.incomeBand
    )
}
