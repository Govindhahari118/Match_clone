package com.match.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Firestore real-time chat keyed exclusively by canonical Firebase Auth UIDs. */
@Singleton
class FirestoreChatService @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    companion object {
        fun threadId(uid1: String, uid2: String): String {
            require(uid1.isNotBlank() && uid2.isNotBlank() && uid1 != uid2)
            val canonical = listOf(uid1, uid2).sorted().joinToString("\n")
            return MessageDigest.getInstance("SHA-256")
                .digest(canonical.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }
    }

    fun observePeerTyping(myUid: String, peerUid: String): Flow<Boolean> = callbackFlow {
        val tid = threadId(myUid, peerUid)
        var expiryJob: Job? = null
        val registration = db.collection("chats").document(tid)
            .collection("typing").document(peerUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                expiryJob?.cancel()
                val expiresAt = snapshot?.getLong("expiresAtMillis") ?: 0L
                val active = snapshot?.getBoolean("typing") == true &&
                    expiresAt > System.currentTimeMillis()
                trySend(active)
                if (active) {
                    expiryJob = launch {
                        delay((expiresAt - System.currentTimeMillis()).coerceAtLeast(1L))
                        trySend(false)
                    }
                }
            }
        awaitClose {
            expiryJob?.cancel()
            registration.remove()
        }
    }

    suspend fun setTyping(recipientUid: String, typing: Boolean) {
        require(recipientUid.isNotBlank())
        val result = functions.getHttpsCallable("setChatTyping")
            .call(mapOf("recipientUid" to recipientUid, "typing" to typing))
            .await()
        @Suppress("UNCHECKED_CAST")
        val payload = result.data as? Map<String, Any?> ?: error("Invalid typing response")
        check(payload["success"] == true) { "Typing state was not accepted by the server" }
    }

    fun observeThreads(myUid: String): Flow<List<FirestoreChatThread>> = callbackFlow {
        require(myUid.isNotBlank())
        val registration = db.collection("chats")
            .whereArrayContains("participantUids", myUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val threads = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val participants = (doc.get("participantUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
                    if (participants.size != 2 || myUid !in participants) return@mapNotNull null
                    val peerUid = participants.firstOrNull { it != myUid } ?: return@mapNotNull null
                    val lastSentAt = doc.getLong("lastSentAt") ?: 0L
                    if (lastSentAt <= 0L) return@mapNotNull null
                    FirestoreChatThread(
                        peerFirebaseUid = peerUid,
                        lastMessage = doc.getString("lastMessage").orEmpty(),
                        lastSentAt = lastSentAt
                    )
                }.sortedByDescending { it.lastSentAt }
                trySend(threads)
            }
        awaitClose { registration.remove() }
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
                        deliveredAt = doc.getTimestamp("deliveredAt")?.toDate()?.time,
                        readAt = doc.getTimestamp("readAt")?.toDate()?.time,
                        isRead = doc.getBoolean("isRead") ?: false
                    )
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { reg.remove() }
    }

    /**
     * Establish the canonical server-authorized thread before uploading IMAGE/VOICE media.
     * Prepared threads are not user-visible until sendChatMessage accepts a real message.
     */
    suspend fun prepareThread(myFirebaseUid: String, peerFirebaseUid: String): String {
        require(myFirebaseUid.isNotBlank() && peerFirebaseUid.isNotBlank() && myFirebaseUid != peerFirebaseUid)
        val result = functions.getHttpsCallable("prepareChatThread")
            .call(mapOf("targetUid" to peerFirebaseUid))
            .await()
        @Suppress("UNCHECKED_CAST")
        val payload = result.data as? Map<String, Any?>
            ?: error("Invalid chat-prepare response")
        check(payload["success"] == true) { "Chat thread was not authorized by the server" }
        val returned = payload["threadId"] as? String ?: error("Missing chat thread identity")
        val expected = threadId(myFirebaseUid, peerFirebaseUid)
        check(returned == expected) { "Server returned an unexpected chat thread identity" }
        return returned
    }

    /**
     * Idempotent server-authoritative send. The stable client-generated message id is preserved
     * across retries by the durable outbox. Trusted Functions owns thread/message creation and
     * re-checks account, match, block/privacy, media identity and safety ceilings on every attempt.
     */
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
        require(voiceUri.isNullOrBlank() || imageUri.isNullOrBlank()) {
            "A message may contain only one media attachment"
        }

        val result = functions.getHttpsCallable("sendChatMessage")
            .call(
                buildMap<String, Any> {
                    put("clientMessageId", clientMessageId)
                    put("targetUid", peerFirebaseUid)
                    put("body", body)
                    if (!voiceUri.isNullOrBlank()) put("voicePath", voiceUri)
                    if (!imageUri.isNullOrBlank()) put("imagePath", imageUri)
                    if (voiceDurationMs != null && voiceDurationMs > 0) {
                        put("voiceDurationMs", voiceDurationMs)
                    }
                }
            )
            .await()

        @Suppress("UNCHECKED_CAST")
        val payload = result.data as? Map<String, Any?>
            ?: error("Invalid chat-send response")
        check(payload["success"] == true) { "Message was not accepted by the server" }
    }

    /** Recipient-side receipt acknowledgement. Server persistence alone is only SENT. */
    suspend fun acknowledgeDelivered(myUid: String, peerUid: String, messageIds: Collection<String>) {
        if (messageIds.isEmpty()) return
        val tid = threadId(myUid, peerUid)
        val batch = db.batch()
        messageIds.distinct().take(200).forEach { messageId ->
            require(messageId.matches(Regex("[A-Za-z0-9_-]{16,128}"))) { "Invalid message id" }
            batch.update(
                db.collection("chats").document(tid).collection("messages").document(messageId),
                "deliveredAt",
                FieldValue.serverTimestamp()
            )
        }
        batch.commit().await()
    }

    suspend fun markRead(myUid: String, peerUid: String) {
        val tid = threadId(myUid, peerUid)
        val unread = db.collection("chats").document(tid).collection("messages")
            .whereEqualTo("toFirebaseUid", myUid)
            .whereEqualTo("isRead", false)
            .get().await()
        if (unread.isEmpty) return
        val batch = db.batch()
        unread.documents.forEach { doc ->
            val updates = mutableMapOf<String, Any>(
                "isRead" to true,
                "readAt" to FieldValue.serverTimestamp()
            )
            if (doc.getTimestamp("deliveredAt") == null) {
                updates["deliveredAt"] = FieldValue.serverTimestamp()
            }
            batch.update(doc.reference, updates)
        }
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

data class FirestoreChatThread(
    val peerFirebaseUid: String,
    val lastMessage: String,
    val lastSentAt: Long
)

data class FirestoreMessage(
    val id: String = "",
    val fromFirebaseUid: String = "",
    val toFirebaseUid: String = "",
    val body: String = "",
    val voiceUri: String? = null,
    val imageUri: String? = null,
    val voiceDurationMs: Long? = null,
    val sentAt: Long = 0L,
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
    val isRead: Boolean = false
)
