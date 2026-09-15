package com.match.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class MemberPrivacyRelation(
    val memberUid: String,
    val profileHidden: Boolean,
    val contactHidden: Boolean
)

enum class ContactVisibility(val wireValue: String) {
    MUTUAL_MATCHES("mutual_matches"),
    NOBODY("nobody");

    companion object {
        fun fromWire(value: String?): ContactVisibility =
            entries.firstOrNull { it.wireValue == value } ?: MUTUAL_MATCHES
    }
}

/**
 * Owner-controlled privacy settings.
 *
 * `privacyRelations/{ownerUid}/members/{viewerUid}` records one-way visibility exceptions.
 * The target member cannot list or read the owner's exception list.
 *
 * `privacySettings/{ownerUid}` stores global privacy choices that trusted backend functions
 * consult before releasing private data such as a phone number.
 */
@Singleton
class FirestorePrivacyService @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()

    private fun relation(ownerUid: String, memberUid: String) =
        db.collection("privacyRelations").document(ownerUid).collection("members").document(memberUid)

    private fun relations(ownerUid: String) =
        db.collection("privacyRelations").document(ownerUid).collection("members")

    private fun settings(ownerUid: String) = db.collection("privacySettings").document(ownerUid)

    suspend fun setProfileHidden(ownerUid: String, memberUid: String, hidden: Boolean) {
        validate(ownerUid, memberUid)
        relation(ownerUid, memberUid).set(
            mapOf(
                "memberUid" to memberUid,
                "profileHidden" to hidden,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun setContactHidden(ownerUid: String, memberUid: String, hidden: Boolean) {
        validate(ownerUid, memberUid)
        relation(ownerUid, memberUid).set(
            mapOf(
                "memberUid" to memberUid,
                "contactHidden" to hidden,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun getRelation(ownerUid: String, memberUid: String): MemberPrivacyRelation {
        validate(ownerUid, memberUid)
        val snap = relation(ownerUid, memberUid).get().await()
        return MemberPrivacyRelation(
            memberUid = memberUid,
            profileHidden = snap.getBoolean("profileHidden") == true,
            contactHidden = snap.getBoolean("contactHidden") == true
        )
    }

    fun observeRelations(ownerUid: String): Flow<List<MemberPrivacyRelation>> = callbackFlow {
        require(ownerUid.isNotBlank()) { "Missing account identity" }
        val registration = relations(ownerUid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val values = snapshot?.documents?.mapNotNull { doc ->
                val uid = doc.getString("memberUid")?.takeIf { it.isNotBlank() } ?: doc.id
                val profileHidden = doc.getBoolean("profileHidden") == true
                val contactHidden = doc.getBoolean("contactHidden") == true
                if (!profileHidden && !contactHidden) null
                else MemberPrivacyRelation(uid, profileHidden, contactHidden)
            }.orEmpty()
            trySend(values)
        }
        awaitClose { registration.remove() }
    }

    suspend fun setContactVisibility(ownerUid: String, visibility: ContactVisibility) {
        require(ownerUid.isNotBlank()) { "Missing account identity" }
        settings(ownerUid).set(
            mapOf(
                "contactVisibility" to visibility.wireValue,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    fun observeContactVisibility(ownerUid: String): Flow<ContactVisibility> = callbackFlow {
        require(ownerUid.isNotBlank()) { "Missing account identity" }
        val registration = settings(ownerUid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(ContactVisibility.fromWire(snapshot?.getString("contactVisibility")))
        }
        awaitClose { registration.remove() }
    }

    private fun validate(ownerUid: String, memberUid: String) {
        require(ownerUid.isNotBlank() && memberUid.isNotBlank()) { "Missing account identity" }
        require(ownerUid != memberUid) { "You cannot create a privacy exception for yourself" }
    }
}
