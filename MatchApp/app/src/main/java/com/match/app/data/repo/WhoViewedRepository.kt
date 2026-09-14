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
                viewerUids.forEach { viewerUid ->
                    if (viewerUid == myUid) return@forEach
                    var viewer = userDao.findByFirebaseUid(viewerUid)
                    if (viewer == null) {
                        val remote = runCatching { profileService.fetchProfile(viewerUid) }.getOrNull()
                        if (remote != null) {
                            val localId = runCatching { userDao.insert(remote) }.getOrNull()
                            viewer = if (localId != null) userDao.findById(localId) else userDao.findByFirebaseUid(viewerUid)
                        }
                    }
                    viewer?.let {
                        dao.record(ProfileViewEntity(viewerId = it.id, profileId = me, viewedAt = System.currentTimeMillis()))
                    }
                }
            }
            .onFailure { Log.w("WhoViewedRepository", "Failed to refresh cloud profile views", it) }
    }

    fun observeViewerIds(me: Long): Flow<List<Long>> = dao.observeViewerIds(me)
    fun observeViewCount(me: Long): Flow<Int> = dao.observeViewCount(me)
}
