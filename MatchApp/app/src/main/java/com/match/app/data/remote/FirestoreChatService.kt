package com.match.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Firestore real-time chat keyed exclusively by canonical Firebase Auth UIDs. */
@Singleton
class FirestoreChatService @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()

    companion object {
        /** Collision-resistant deterministic thread id that does not depend on local Room ids. */
        fun threadId(uid1: String, uid2: String): String {
            require(uid1.isNotBlank() && uid2.isNotBlank() && uid1 != uid2)
            val canonical = listOf(uid1, uid2).sorted().joinToString("\n")
            return MessageDigest.getInstance("SHA-256")
                .digest(canonical.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }
    }

    fun observeThread(myUid: String, peerUid: String): Flow<List<FirestoreMessage>> = callbackFlow {
        val tid = threadId(myUid, peerUid)
        val reg = db.collection("chats").document(tid).collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val messages = snap?.documents?.mapNotNull { doc ->
                    val fromUid = doc.getString("fromFirebaseUid") ?: return@mapNotNull null
                    val toUid = doc.getString("toFirebaseUid") ?: return@mapNotNull null
                    FirestoreMessage(
                        id = doc.id,
                        fromFirebaseUid = fromUid,
                        toFirebaseUid = toUid,
                        body = doc.getString("body") ?: "",
                        voiceUri = doc.getString("voiceUri"),
                        imageUri = doc.getString("imageUri"),
                        voiceDurationMs = doc.getLong("voiceDurationMs"),
                        sentAt = doc.getLong("sentAt") ?: 0L,
                        isRead = doc.getBoolean("isRead") ?: false
                    )
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { reg.remove() }
    }

    suspend fun sendMessage(
        clientMessageId: String,
        body: String,
        myFirebaseUid: String,
        peerFirebaseUid: String,
        voiceUri: String? = null,
        imageUri: String? = null,
        voiceDurationMs: Long? = null
    ) {
        require(clientMessageId.matches(Regex("[A-Za-z0-9_-]{16,128}"))) { "Invalid message id" }
        require(myFirebaseUid.isNotBlank() && peerFirebaseUid.isNotBlank() && myFirebaseUid != peerFirebaseUid)
        require(body.length <= 3000) { "Message too long" }

        val tid = threadId(myFirebaseUid, peerFirebaseUid)
        val now = System.currentTimeMillis()
        val threadRef = db.collection("chats").document(tid)
        val participantUids = listOf(myFirebaseUid, peerFirebaseUid).sorted()
        val preview = when {
            imageUri != null -> "📷 Image"
            voiceUri != null -> "🎤 Voice message"
            else -> body.take(120)
        }

        threadRef.set(
            mapOf("participantUids" to participantUids, "lastMessage" to preview, "lastSentAt" to now),
            SetOptions.merge()
        ).await()

        val data = mutableMapOf<String, Any>(
            "body" to body,
            "sentAt" to now,
            "isRead" to false,
            "fromFirebaseUid" to myFirebaseUid,
            "toFirebaseUid" to peerFirebaseUid
        )
        if (!voiceUri.isNullOrBlank()) data["voiceUri"] = voiceUri
        if (!imageUri.isNullOrBlank()) data["imageUri"] = imageUri
        if (voiceDurationMs != null && voiceDurationMs > 0) data["voiceDurationMs"] = voiceDurationMs
        threadRef.collection("messages").document(clientMessageId).set(data).await()
    }

    suspend fun markRead(myUid: String, peerUid: String) {
        val tid = threadId(myUid, peerUid)
        val unread = db.collection("chats").document(tid).collection("messages")
            .whereEqualTo("toFirebaseUid", myUid)
            .whereEqualTo("isRead", false)
            .get().await()
        if (unread.isEmpty) return
        val batch = db.batch()
        unread.documents.forEach { batch.update(it.reference, "isRead", true) }
        batch.commit().await()
    }

    fun observeUnreadCount(firebaseUid: String): Flow<Int> = callbackFlow {
        val reg = db.collectionGroup("messages")
            .whereEqualTo("toFirebaseUid", firebaseUid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.size() ?: 0)
            }
        awaitClose { reg.remove() }
    }
}

data class FirestoreMessage(
    val id: String = "",
    val fromFirebaseUid: String = "",
    val toFirebaseUid: String = "",
    val body: String = "",
    val voiceUri: String? = null,
    val imageUri: String? = null,
    val voiceDurationMs: Long? = null,
    val sentAt: Long = 0L,
    val isRead: Boolean = false
)
