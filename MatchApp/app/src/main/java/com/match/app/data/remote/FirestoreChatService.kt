package com.match.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Firestore-backed real-time chat service.
 * Messages are stored under: chats/{threadId}/messages/{messageId}
 * threadId is deterministic: "uid_smaller-uid_larger"
 *
 * Firestore authorization is based on Firebase Auth UIDs (`participantUids`),
 * while the numeric user IDs are retained for the app's existing local/domain model.
 */
@Singleton
class FirestoreChatService @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    companion object {
        fun threadId(uid1: Long, uid2: Long): String {
            val a = minOf(uid1, uid2)
            val b = maxOf(uid1, uid2)
            return "${a}_${b}"
        }
    }

    /** Observe real-time messages for a thread. Returns a cold Flow. */
    fun observeThread(me: Long, peer: Long): Flow<List<FirestoreMessage>> = callbackFlow {
        val tid = threadId(me, peer)
        val reg = db.collection("chats")
            .document(tid)
            .collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val msgs = snap?.documents?.mapNotNull { doc ->
                    runCatching {
                        FirestoreMessage(
                            id = doc.id,
                            fromUserId = doc.getLong("fromUserId") ?: 0L,
                            toUserId = doc.getLong("toUserId") ?: 0L,
                            body = doc.getString("body") ?: "",
                            voiceUri = doc.getString("voiceUri"),
                            imageUri = doc.getString("imageUri"),
                            sentAt = doc.getLong("sentAt") ?: 0L,
                            isRead = doc.getBoolean("isRead") ?: false
                        )
                    }.getOrNull()
                } ?: emptyList()
                trySend(msgs)
            }
        awaitClose { reg.remove() }
    }

    /**
     * Send a message to Firestore.
     *
     * The parent thread is created/updated before the message so Firestore rules can
     * authorize the message against `participantUids`, including for the first message
     * in a brand-new conversation.
     */
    suspend fun sendMessage(
        me: Long,
        peer: Long,
        body: String,
        voiceUri: String? = null,
        imageUri: String? = null,
        myFirebaseUid: String = "",
        peerFirebaseUid: String = ""
    ) {
        require(me > 0L && peer > 0L && me != peer) { "Invalid chat participants" }
        require(myFirebaseUid.isNotBlank() && peerFirebaseUid.isNotBlank()) {
            "Firebase UIDs are required for secure chat authorization"
        }

        val tid = threadId(me, peer)
        val now = System.currentTimeMillis()
        val threadRef = db.collection("chats").document(tid)

        // Establish the authorization boundary first. Existing participantUids are
        // protected by Firestore rules and therefore cannot be changed by a client.
        threadRef.set(
            mapOf(
                "lastMessage" to body,
                "lastSentAt" to now,
                "participants" to listOf(me, peer),
                "participantUids" to listOf(myFirebaseUid, peerFirebaseUid).distinct()
            ),
            SetOptions.merge()
        ).await()

        val data = hashMapOf(
            "fromUserId" to me,
            "toUserId" to peer,
            "body" to body,
            "voiceUri" to voiceUri,
            "imageUri" to imageUri,
            "sentAt" to now,
            "isRead" to false,
            "fromFirebaseUid" to myFirebaseUid,
            "toFirebaseUid" to peerFirebaseUid
        )
        threadRef.collection("messages").add(data).await()
    }

    /** Mark all messages in a thread addressed to this local user as read. */
    suspend fun markRead(me: Long, peer: Long) {
        val tid = threadId(me, peer)
        val unread = db.collection("chats")
            .document(tid)
            .collection("messages")
            .whereEqualTo("toUserId", me)
            .whereEqualTo("isRead", false)
            .get()
            .await()
        val batch = db.batch()
        unread.documents.forEach { doc ->
            batch.update(doc.reference, "isRead", true)
        }
        if (unread.documents.isNotEmpty()) batch.commit().await()
    }

    /** Observe unread message count for a user (across all threads). */
    fun observeUnreadCount(userId: Long): Flow<Int> = callbackFlow {
        val reg = db.collectionGroup("messages")
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                trySend(snap?.size() ?: 0)
            }
        awaitClose { reg.remove() }
    }
}

data class FirestoreMessage(
    val id: String = "",
    val fromUserId: Long = 0L,
    val toUserId: Long = 0L,
    val body: String = "",
    val voiceUri: String? = null,
    val imageUri: String? = null,
    val sentAt: Long = 0L,
    val isRead: Boolean = false
)
