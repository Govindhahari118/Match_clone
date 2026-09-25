package com.match.app.data.repo

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.match.app.data.local.dao.NotificationDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.NotificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notifications are server-authoritative. Room is an offline cache only.
 *
 * Backend domain triggers persist the notification document before FCM delivery. This repository
 * observes those persisted events so a missed push, reinstall or second device still sees the same
 * notification history and read state.
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val dao: NotificationDao,
    private val userDao: UserDao
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val remoteIds = ConcurrentHashMap<Long, String>()

    fun observe(localUid: Long): Flow<List<NotificationEntity>> {
        val firebaseUid = auth.currentUser?.uid ?: return dao.observe(localUid)
        return callbackFlow {
            var registration: ListenerRegistration? = null
            registration = firestore.collection("notifications")
                .whereEqualTo("userId", firebaseUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    launch(Dispatchers.IO) {
                        val mapped = snapshot.documents.mapNotNull { doc ->
                            val type = doc.getString("type")?.uppercase()?.takeIf { it.isNotBlank() }
                                ?: return@mapNotNull null
                            val title = doc.getString("title")?.takeIf { it.isNotBlank() }
                                ?: return@mapNotNull null
                            val body = doc.getString("body").orEmpty()
                            val localId = stableLocalId(doc.id)
                            remoteIds[localId] = doc.id
                            val actorFirebaseUid = doc.getString("fromFirebaseUid").orEmpty()
                            val fromUserId = actorFirebaseUid.takeIf { it.isNotBlank() }
                                ?.let { userDao.findByFirebaseUid(it)?.id }
                            NotificationEntity(
                                id = localId,
                                userId = localUid,
                                type = type,
                                fromUserId = fromUserId,
                                title = title,
                                body = body,
                                isRead = doc.get("readAt") != null,
                                createdAt = createdAtMillis(doc.get("createdAt"))
                            )
                        }.sortedByDescending { it.createdAt }

                        mapped.forEach { dao.insert(it) }
                        trySend(mapped)
                    }
                }
            awaitClose { registration?.remove() }
        }
    }

    fun observeUnreadCount(localUid: Long): Flow<Int> {
        val firebaseUid = auth.currentUser?.uid ?: return dao.observeUnreadCount(localUid)
        return callbackFlow {
            val registration = firestore.collection("notifications")
                .whereEqualTo("userId", firebaseUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    trySend(snapshot.documents.count { it.get("readAt") == null })
                }
            awaitClose { registration.remove() }
        }
    }

    suspend fun markAllRead(localUid: Long) = withContext(Dispatchers.IO) {
        val firebaseUid = auth.currentUser?.uid
        if (firebaseUid != null) {
            val snapshot = firestore.collection("notifications")
                .whereEqualTo("userId", firebaseUid)
                .get()
                .await()
            snapshot.documents.filter { it.get("readAt") == null }.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.update(it.reference, "readAt", com.google.firebase.firestore.FieldValue.serverTimestamp()) }
                batch.commit().await()
            }
        }
        dao.markAllRead(localUid)
    }

    suspend fun markRead(id: Long) = withContext(Dispatchers.IO) {
        remoteIds[id]?.let { remoteId ->
            firestore.collection("notifications").document(remoteId)
                .update("readAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
                .await()
        }
        dao.markRead(id)
    }

    /** Debug/test-only local injection path. Production notification truth comes from Firestore. */
    suspend fun push(n: NotificationEntity) = withContext(Dispatchers.IO) { dao.insert(n) }

    private fun stableLocalId(remoteId: String): Long {
        val digest = MessageDigest.getInstance("SHA-256").digest(remoteId.toByteArray(Charsets.UTF_8))
        return ByteBuffer.wrap(digest.copyOfRange(0, 8)).long and Long.MAX_VALUE
    }

    private fun createdAtMillis(value: Any?): Long = when (value) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> System.currentTimeMillis()
    }
}
