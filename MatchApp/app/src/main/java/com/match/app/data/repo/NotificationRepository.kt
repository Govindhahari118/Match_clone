package com.match.app.data.repo

import com.match.app.data.local.dao.NotificationDao
import com.match.app.data.local.entity.NotificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(private val dao: NotificationDao) {
    fun observe(uid: Long): Flow<List<NotificationEntity>> = dao.observe(uid)
    fun observeUnreadCount(uid: Long): Flow<Int> = dao.observeUnreadCount(uid)
    suspend fun markAllRead(uid: Long) = withContext(Dispatchers.IO) { dao.markAllRead(uid) }
    suspend fun markRead(id: Long) = withContext(Dispatchers.IO) { dao.markRead(id) }
    suspend fun push(n: NotificationEntity) = withContext(Dispatchers.IO) { dao.insert(n) }
}
