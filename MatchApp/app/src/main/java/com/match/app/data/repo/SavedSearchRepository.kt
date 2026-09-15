package com.match.app.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.match.app.data.local.dao.SavedSearchDao
import com.match.app.data.local.entity.SavedSearchEntity
import com.match.app.domain.model.MatchFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Account-level saved discovery preset. Firestore is authoritative across devices. */
data class SavedSearchPreset(
    val id: String,
    val name: String,
    val createdAt: Long,
    val filter: MatchFilter
)

@Singleton
class SavedSearchRepository @Inject constructor(
    private val dao: SavedSearchDao
) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    /** Legacy local cache flow retained only for existing installs and old callers. */
    fun observeForUser(userId: Long): Flow<List<SavedSearchEntity>> = dao.observeForUser(userId)

    /** Cross-device source of truth; Firestore persistence also provides an offline read cache. */
    fun observeRemote(): Flow<List<SavedSearchPreset>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = db.collection("savedSearches").document(uid).collection("items")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    SavedSearchPreset(
                        id = doc.id,
                        name = (data["name"] as? String).orEmpty().ifBlank { "Saved search" },
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                        filter = mapToFilter(data)
                    )
                })
            }
        awaitClose { registration.remove() }
    }

    /**
     * Creation is server-authoritative so a modified client cannot bypass the per-account quota or
     * store an unexpected filter schema. The callable sanitizes all values and atomically updates
     * the saved-search counter.
     */
    suspend fun saveRemote(name: String, filter: MatchFilter): String = withContext(Dispatchers.IO) {
        check(auth.currentUser != null) { "Sign in required" }
        val cleanName = name.trim().replace(Regex("\\s+"), " ").take(60).ifBlank { "Saved search" }
        val response = functions.getHttpsCallable("saveSavedSearch")
            .call(mapOf("name" to cleanName, "filter" to filterToMap(filter)))
            .await()
        @Suppress("UNCHECKED_CAST")
        val result = response.data as? Map<String, Any?> ?: error("Invalid saved-search response")
        (result["id"] as? String)?.takeIf { it.isNotBlank() } ?: error("Saved-search id missing")
    }

    /** Deletion also goes through the trusted backend so quota metadata stays transactionally correct. */
    suspend fun deleteRemote(id: String) = withContext(Dispatchers.IO) {
        check(auth.currentUser != null) { "Sign in required" }
        require(id.isNotBlank()) { "Invalid saved search" }
        functions.getHttpsCallable("deleteSavedSearch")
            .call(mapOf("id" to id))
            .await()
        Unit
    }

    /**
     * Legacy Room compatibility intentionally stores only the v18 columns. New production UI uses
     * Firestore saved searches, which preserve the complete MatchFilter without a Room migration.
     */
    suspend fun save(userId: Long, name: String, filter: MatchFilter) = withContext(Dispatchers.IO) {
        dao.save(
            SavedSearchEntity(
                userId = userId,
                name = name.trim().take(60).ifBlank { "Saved search" },
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
                maritalStatus = filter.maritalStatus
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
        religion = entity.religions,
        motherTongue = entity.motherTongues,
        maritalStatus = entity.maritalStatus,
        incomeMin = entity.incomeBand,
        educationLevel = entity.education,
        diet = entity.diet,
        smoking = entity.smoking,
        drinking = entity.drinking
    )

    private fun filterToMap(f: MatchFilter): Map<String, Any> = mapOf(
        "ageMin" to f.ageMin, "ageMax" to f.ageMax, "city" to f.city, "state" to f.state,
        "caste" to f.caste, "subCaste" to f.subCaste, "minScore" to f.minScore,
        "religion" to f.religion, "motherTongue" to f.motherTongue,
        "maritalStatus" to f.maritalStatus, "verifiedOnly" to f.verifiedOnly,
        "incomeMin" to f.incomeMin, "incomeMax" to f.incomeMax,
        "educationLevel" to f.educationLevel, "diet" to f.diet,
        "residentialStatus" to f.residentialStatus, "hasChildren" to f.hasChildren,
        "keyword" to f.keyword, "gothra" to f.gothra, "nativeState" to f.nativeState,
        "countryOfResidence" to f.countryOfResidence, "nriOnly" to f.nriOnly,
        "willingToRelocate" to f.willingToRelocate,
        "recentlyJoinedDays" to f.recentlyJoinedDays, "smoking" to f.smoking,
        "drinking" to f.drinking, "familyType" to f.familyType,
        "familyStatus" to f.familyStatus, "physicalStatus" to f.physicalStatus,
        "hasChildrenFilter" to f.hasChildrenFilter, "citizenship" to f.citizenship,
        "nriStatus" to f.nriStatus, "educationField" to f.educationField,
        "occupationCategory" to f.occupationCategory, "employerType" to f.employerType,
        "nakshatra" to f.nakshatra, "rasi" to f.rasi, "manglik" to f.manglik,
        "hobbies" to f.hobbies, "withPhotoOnly" to f.withPhotoOnly,
        "verifiedLevel" to f.verifiedLevel, "premiumOnly" to f.premiumOnly,
        "lastActiveWithinDays" to f.lastActiveWithinDays,
        "minPoruthamScore" to f.minPoruthamScore, "hasHoroscope" to f.hasHoroscope
    )

    private fun mapToFilter(data: Map<String, Any?>) = MatchFilter(
        ageMin = (data["ageMin"] as? Number)?.toInt() ?: 18,
        ageMax = (data["ageMax"] as? Number)?.toInt() ?: 70,
        city = string(data, "city"), state = string(data, "state"), caste = string(data, "caste"),
        subCaste = string(data, "subCaste"), minScore = (data["minScore"] as? Number)?.toFloat() ?: 0f,
        religion = string(data, "religion"), motherTongue = string(data, "motherTongue"),
        maritalStatus = string(data, "maritalStatus"), verifiedOnly = bool(data, "verifiedOnly"),
        incomeMin = string(data, "incomeMin"), incomeMax = string(data, "incomeMax"),
        educationLevel = string(data, "educationLevel"), diet = string(data, "diet"),
        residentialStatus = string(data, "residentialStatus"), hasChildren = string(data, "hasChildren"),
        keyword = string(data, "keyword"), gothra = string(data, "gothra"), nativeState = string(data, "nativeState"),
        countryOfResidence = string(data, "countryOfResidence"), nriOnly = bool(data, "nriOnly"),
        willingToRelocate = bool(data, "willingToRelocate"),
        recentlyJoinedDays = int(data, "recentlyJoinedDays"), smoking = string(data, "smoking"),
        drinking = string(data, "drinking"), familyType = string(data, "familyType"),
        familyStatus = string(data, "familyStatus"), physicalStatus = string(data, "physicalStatus"),
        hasChildrenFilter = string(data, "hasChildrenFilter"), citizenship = string(data, "citizenship"),
        nriStatus = string(data, "nriStatus"), educationField = string(data, "educationField"),
        occupationCategory = string(data, "occupationCategory"), employerType = string(data, "employerType"),
        nakshatra = string(data, "nakshatra"), rasi = string(data, "rasi"), manglik = string(data, "manglik"),
        hobbies = string(data, "hobbies"), withPhotoOnly = data["withPhotoOnly"] as? Boolean ?: true,
        verifiedLevel = int(data, "verifiedLevel"), premiumOnly = bool(data, "premiumOnly"),
        lastActiveWithinDays = int(data, "lastActiveWithinDays"), minPoruthamScore = int(data, "minPoruthamScore"),
        hasHoroscope = string(data, "hasHoroscope")
    )

    private fun string(data: Map<String, Any?>, key: String) = data[key] as? String ?: ""
    private fun bool(data: Map<String, Any?>, key: String) = data[key] as? Boolean ?: false
    private fun int(data: Map<String, Any?>, key: String) = (data[key] as? Number)?.toInt() ?: 0
}
