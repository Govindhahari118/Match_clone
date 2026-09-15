package com.match.app.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    /** Legacy local cache flow retained for offline/backward compatibility. */
    fun observeForUser(userId: Long): Flow<List<SavedSearchEntity>> = dao.observeForUser(userId)

    /**
     * Cross-device source of truth. Saved searches are private to the authenticated owner and
     * Firestore's local persistence supplies an offline cache automatically.
     */
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
                val presets = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    SavedSearchPreset(
                        id = doc.id,
                        name = (data["name"] as? String).orEmpty().ifBlank { "Saved search" },
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                        filter = mapToFilter(data)
                    )
                }
                trySend(presets)
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveRemote(name: String, filter: MatchFilter): String = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: error("Sign in required")
        val cleanName = name.trim().replace(Regex("\\s+"), " ").take(60).ifBlank { "Saved search" }
        val items = db.collection("savedSearches").document(uid).collection("items")
        // Keep the feature intentionally bounded so an account cannot create an unmanageable list.
        val existing = items.limit(MAX_SAVED_SEARCHES.toLong()).get().await()
        if (existing.size() >= MAX_SAVED_SEARCHES) error("You can keep up to $MAX_SAVED_SEARCHES saved searches.")
        val ref = items.document()
        ref.set(filterToMap(filter) + mapOf("name" to cleanName, "createdAt" to System.currentTimeMillis())).await()
        ref.id
    }

    suspend fun deleteRemote(id: String) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: error("Sign in required")
        require(id.isNotBlank()) { "Invalid saved search" }
        db.collection("savedSearches").document(uid).collection("items").document(id).delete().await()
    }

    // Local methods are kept for older installations while the account-level Firestore model is
    // rolled out. New production UI uses saveRemote/observeRemote/deleteRemote.
    suspend fun save(userId: Long, name: String, filter: MatchFilter) = withContext(Dispatchers.IO) {
        val cleanName = name.trim().take(60).ifBlank { "Saved search" }
        dao.save(entityFrom(userId, cleanName, filter))
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

    private fun entityFrom(userId: Long, name: String, filter: MatchFilter) = SavedSearchEntity(
        userId = userId, name = name, minAge = filter.ageMin, maxAge = filter.ageMax,
        city = filter.city, state = filter.state, religions = filter.religion, castes = filter.caste,
        motherTongues = filter.motherTongue, diet = filter.diet, smoking = filter.smoking,
        drinking = filter.drinking, incomeBand = filter.incomeMin, education = filter.educationLevel,
        maritalStatus = filter.maritalStatus, subCaste = filter.subCaste, minScore = filter.minScore,
        verifiedOnly = filter.verifiedOnly, incomeMax = filter.incomeMax,
        residentialStatus = filter.residentialStatus, hasChildren = filter.hasChildren,
        keyword = filter.keyword, gothra = filter.gothra, nativeState = filter.nativeState,
        countryOfResidence = filter.countryOfResidence, nriOnly = filter.nriOnly,
        willingToRelocate = filter.willingToRelocate, recentlyJoinedDays = filter.recentlyJoinedDays,
        familyType = filter.familyType, familyStatus = filter.familyStatus,
        physicalStatus = filter.physicalStatus, hasChildrenFilter = filter.hasChildrenFilter,
        citizenship = filter.citizenship, nriStatus = filter.nriStatus,
        educationField = filter.educationField, occupationCategory = filter.occupationCategory,
        employerType = filter.employerType, nakshatra = filter.nakshatra, rasi = filter.rasi,
        manglik = filter.manglik, hobbies = filter.hobbies, withPhotoOnly = filter.withPhotoOnly,
        verifiedLevel = filter.verifiedLevel, premiumOnly = filter.premiumOnly,
        lastActiveWithinDays = filter.lastActiveWithinDays, minPoruthamScore = filter.minPoruthamScore,
        hasHoroscope = filter.hasHoroscope
    )

    private fun string(data: Map<String, Any?>, key: String) = data[key] as? String ?: ""
    private fun bool(data: Map<String, Any?>, key: String) = data[key] as? Boolean ?: false
    private fun int(data: Map<String, Any?>, key: String) = (data[key] as? Number)?.toInt() ?: 0

    private companion object {
        const val MAX_SAVED_SEARCHES = 20
    }
}
