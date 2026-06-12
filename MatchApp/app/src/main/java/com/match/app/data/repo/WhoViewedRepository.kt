package com.match.app.data.repo

import com.match.app.data.local.dao.ProfileViewDao
import com.match.app.data.local.entity.ProfileViewEntity
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
    private val session: SessionStore
) {
    /** Record a profile view — skipped silently when the viewer has incognito mode on. */
    suspend fun record(viewerId: Long, profileId: Long) = withContext(Dispatchers.IO) {
        val incognito = session.incognitoMode.first()
        if (!incognito) {
            dao.record(ProfileViewEntity(viewerId = viewerId, profileId = profileId))
        }
    }
    fun observeViewerIds(me: Long): Flow<List<Long>> = dao.observeViewerIds(me)
    fun observeViewCount(me: Long): Flow<Int> = dao.observeViewCount(me)
}
