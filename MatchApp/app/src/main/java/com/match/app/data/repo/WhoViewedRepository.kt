package com.match.app.data.repo

import android.util.Log
import com.match.app.data.local.dao.ProfileViewDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.ProfileViewEntity
import com.match.app.data.remote.FirestoreProfileService
import com.match.app.data.remote.FirestoreProfileViewService
import com.match.app.data.session.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhoViewedRepository @Inject constructor(
    private val dao: ProfileViewDao,
    private val userDao: UserDao,
    private val profileService: FirestoreProfileService,
    private val profileViewService: FirestoreProfileViewService,
    private val session: SessionStore
) {
    /**
     * Record a profile view locally and in Firestore. Incognito/private browsing suppresses both
     * so the privacy control has the same behaviour across devices and notification triggers.
     */
    suspend fun record(viewerId: Long, profileId: Long) = withContext(Dispatchers.IO) {
        if (session.incognitoMode.first()) return@withContext
        if (viewerId == profileId) return@withContext

        dao.record(ProfileViewEntity(viewerId = viewerId, profileId = profileId))

        val viewerUid = userDao.findById(viewerId)?.firebaseUid.orEmpty()
        val viewedUid = userDao.findById(profileId)?.firebaseUid.orEmpty()
        if (viewerUid.isBlank() || viewedUid.isBlank()) return@withContext
        runCatching { profileViewService.record(viewerUid, viewedUid) }
            .onFailure { Log.w("WhoViewedRepository", "Cloud profile-view sync failed", it) }
    }

    /**
     * Hydrate cloud view history into the Room cache so Who Viewed works on a second device.
     * Missing viewer profiles are fetched from the public profile collection before the local
     * ProfileViewEntity is inserted, preserving Room foreign-key integrity.
     */
    suspend fun refreshFromCloud(me: Long) = withContext(Dispatchers.IO) {
        val meEntity = userDao.findById(me) ?: return@withContext
        val myUid = meEntity.firebaseUid
        if (myUid.isBlank()) return@withContext

        runCatching { profileViewService.fetchViewerUids(myUid) }
            .onSuccess { viewerUids ->
                // Cloud history is authoritative. Rebuild only from viewer profiles that still pass
                // a forced server read so block/privacy/account suppression cannot survive in Room.
                dao.deleteForProfile(me)
                viewerUids.distinct().forEach { viewerUid ->
                    if (viewerUid == myUid) return@forEach
                    val remote = runCatching { profileService.fetchProfileFromServer(viewerUid) }
                        .getOrNull() ?: return@forEach
                    val existing = userDao.findByFirebaseUid(viewerUid)
                    val viewer = if (existing != null) {
                        val merged = remote.copy(
                            id = existing.id,
                            email = existing.email,
                        )
                        userDao.update(merged)
                        merged
                    } else {
                        val cached = remote.copy(
                            email = "${remote.firebaseUid}@cache.invalid",
                        )
                        val localId = runCatching { userDao.insert(cached) }.getOrNull()
                            ?: return@forEach
                        cached.copy(id = localId)
                    }
                    dao.record(
                        ProfileViewEntity(
                            viewerId = viewer.id,
                            profileId = me,
                            viewedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            .onFailure { Log.w("WhoViewedRepository", "Failed to refresh cloud profile views", it) }
    }

    fun observeViewerIds(me: Long): Flow<List<Long>> = dao.observeViewerIds(me)
    fun observeViewCount(me: Long): Flow<Int> = dao.observeViewCount(me)
}
