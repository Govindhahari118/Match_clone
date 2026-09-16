package com.match.app.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trusted boundary for confirming the member's canonical matrimonial-profile religion.
 * The Android client never writes religion lock metadata directly.
 */
@Singleton
class ReligionProfileRepository @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    fun observeConfirmed(firebaseUid: String): Flow<Boolean> = callbackFlow {
        if (firebaseUid.isBlank()) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val registration = db.collection("users").document(firebaseUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val data = snapshot?.data
                trySend(
                    snapshot?.exists() == true &&
                        data?.get("religionLocked") == true &&
                        data["religionConfirmedAt"] != null &&
                        !((data["religionId"] as? String).isNullOrBlank())
                )
            }
        awaitClose { registration.remove() }
    }

    suspend fun confirm(religionLabel: String) {
        val result = functions.getHttpsCallable("confirmReligion")
            .call(mapOf("religion" to religionLabel.trim()))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid religion confirmation response")
        check(data["locked"] == true) { "Religion confirmation was not completed" }
    }
}
