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

data class ContactGrant(
    val viewerUid: String,
    val phoneAllowed: Boolean,
    val whatsappAllowed: Boolean
)

enum class ContactVisibility(val wireValue: String) {
    MUTUAL_MATCHES("mutual_matches"),
    SELECTED_PEOPLE("selected_people"),
    NOBODY("nobody");

    companion object {
        fun fromWire(value: String?): ContactVisibility =
            entries.firstOrNull { it.wireValue == value } ?: MUTUAL_MATCHES
    }
}

enum class ActivityVisibility(val wireValue: String, val label: String) {
    EVERYONE("everyone", "Everyone"),
    INTERESTS("interests", "People with an interest"),
    MUTUAL("mutual", "Mutual interests"),
    NOBODY("nobody", "Nobody");

    companion object {
        fun fromWire(value: String?): ActivityVisibility =
            entries.firstOrNull { it.wireValue == value } ?: MUTUAL
    }
}

data class ActivityPrivacy(
    val onlineVisibility: ActivityVisibility = ActivityVisibility.MUTUAL,
    val lastActiveVisibility: ActivityVisibility = ActivityVisibility.MUTUAL
)

/**
 * Owner-controlled privacy settings. Global choices and per-member exceptions/grants are separate:
 * a hidden member never regains access merely because they have a contact grant.
 */
@Singleton
class FirestorePrivacyService @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()

    private fun relation(ownerUid: String, memberUid: String) =
        db.collection("privacyRelations").document(ownerUid).collection("members").document(memberUid)

    private fun relations(ownerUid: String) =
        db.collection("privacyRelations").document(ownerUid).collection("members")

    private fun settings(ownerUid: String) = db.collection("privacySettings").document(ownerUid)

    private fun contactGrant(ownerUid: String, viewerUid: String) =
        db.collection("contactGrants").document(ownerUid).collection("viewers").document(viewerUid)

    private fun contactGrants(ownerUid: String) =
        db.collection("contactGrants").document(ownerUid).collection("viewers")

    suspend fun setProfileHidden(ownerUid: String, memberUid: String, hidden: Boolean) {
        validate(ownerUid, memberUid)
        relation(ownerUid, memberUid).set(
            mapOf("memberUid" to memberUid, "profileHidden" to hidden, "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        ).await()
    }

    suspend fun setContactHidden(ownerUid: String, memberUid: String, hidden: Boolean) {
        validate(ownerUid, memberUid)
        relation(ownerUid, memberUid).set(
            mapOf("memberUid" to memberUid, "contactHidden" to hidden, "updatedAt" to FieldValue.serverTimestamp()),
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
            if (error != null) { close(error); return@addSnapshotListener }
            val values = snapshot?.documents?.mapNotNull { doc ->
                val uid = doc.getString("memberUid")?.takeIf { it.isNotBlank() } ?: doc.id
                val profileHidden = doc.getBoolean("profileHidden") == true
                val contactHidden = doc.getBoolean("contactHidden") == true
                if (!profileHidden && !contactHidden) null else MemberPrivacyRelation(uid, profileHidden, contactHidden)
            }.orEmpty()
            trySend(values)
        }
        awaitClose { registration.remove() }
    }

    suspend fun setContactVisibility(ownerUid: String, visibility: ContactVisibility) {
        require(ownerUid.isNotBlank()) { "Missing account identity" }
        settings(ownerUid).set(
            mapOf("contactVisibility" to visibility.wireValue, "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        ).await()
    }

    fun observeContactVisibility(ownerUid: String): Flow<ContactVisibility> = callbackFlow {
        require(ownerUid.isNotBlank()) { "Missing account identity" }
        val registration = settings(ownerUid).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(ContactVisibility.fromWire(snapshot?.getString("contactVisibility")))
        }
        awaitClose { registration.remove() }
    }

    suspend fun setContactGrant(ownerUid: String, viewerUid: String, phoneAllowed: Boolean, whatsappAllowed: Boolean) {
        validate(ownerUid, viewerUid)
        if (!phoneAllowed && !whatsappAllowed) {
            contactGrant(ownerUid, viewerUid).delete().await()
            return
        }
        contactGrant(ownerUid, viewerUid).set(
            mapOf(
                "viewerUid" to viewerUid,
                "phoneAllowed" to phoneAllowed,
                "whatsappAllowed" to whatsappAllowed,
                "grantedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    fun observeContactGrants(ownerUid: String): Flow<List<ContactGrant>> = callbackFlow {
        require(ownerUid.isNotBlank()) { "Missing account identity" }
        val registration = contactGrants(ownerUid).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(snapshot?.documents?.mapNotNull { doc ->
                val viewerUid = doc.getString("viewerUid")?.takeIf { it.isNotBlank() } ?: doc.id
                val phone = doc.getBoolean("phoneAllowed") == true
                val whatsapp = doc.getBoolean("whatsappAllowed") == true
                if (!phone && !whatsapp) null else ContactGrant(viewerUid, phone, whatsapp)
            }.orEmpty())
        }
        awaitClose { registration.remove() }
    }

    suspend fun setOnlineVisibility(ownerUid: String, visibility: ActivityVisibility) {
        setActivityField(ownerUid, "onlineVisibility", visibility)
    }

    suspend fun setLastActiveVisibility(ownerUid: String, visibility: ActivityVisibility) {
        setActivityField(ownerUid, "lastActiveVisibility", visibility)
    }

    fun observeActivityPrivacy(ownerUid: String): Flow<ActivityPrivacy> = callbackFlow {
        require(ownerUid.isNotBlank()) { "Missing account identity" }
        val registration = settings(ownerUid).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(
                ActivityPrivacy(
                    onlineVisibility = ActivityVisibility.fromWire(snapshot?.getString("onlineVisibility")),
                    lastActiveVisibility = ActivityVisibility.fromWire(snapshot?.getString("lastActiveVisibility"))
                )
            )
        }
        awaitClose { registration.remove() }
    }

    private suspend fun setActivityField(ownerUid: String, field: String, visibility: ActivityVisibility) {
        require(ownerUid.isNotBlank()) { "Missing account identity" }
        settings(ownerUid).set(
            mapOf(field to visibility.wireValue, "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        ).await()
    }

    private fun validate(ownerUid: String, memberUid: String) {
        require(ownerUid.isNotBlank() && memberUid.isNotBlank()) { "Missing account identity" }
        require(ownerUid != memberUid) { "You cannot create a privacy exception for yourself" }
    }
}
