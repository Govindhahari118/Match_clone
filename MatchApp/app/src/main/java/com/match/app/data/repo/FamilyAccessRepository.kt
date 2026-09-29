package com.match.app.data.repo

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class FamilyDelegateAccess(
    val delegateUid: String,
    val role: String,
    val permissions: List<String>,
    val active: Boolean
)

data class ManagedFamilyProfile(
    val ownerUid: String,
    val role: String,
    val permissions: List<String>
)

data class FamilyAccessSnapshot(
    val delegates: List<FamilyDelegateAccess>,
    val managedProfiles: List<ManagedFamilyProfile>
)

data class FamilyInvite(
    val inviteToken: String,
    val expiresAtMillis: Long,
    val role: String,
    val permissions: List<String>
)

@Singleton
class FamilyAccessRepository @Inject constructor() {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun listAccess(): FamilyAccessSnapshot {
        val result = functions.getHttpsCallable("listMyFamilyAccess").call().await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid family-access response")
        val delegates = (data["delegates"] as? List<*>).orEmpty().mapNotNull { raw ->
            val row = raw as? Map<*, *> ?: return@mapNotNull null
            val uid = row["delegateUid"] as? String ?: return@mapNotNull null
            FamilyDelegateAccess(
                delegateUid = uid,
                role = row["role"] as? String ?: "",
                permissions = (row["permissions"] as? List<*>).orEmpty().filterIsInstance<String>(),
                active = row["active"] == true
            )
        }
        val managed = (data["managedProfiles"] as? List<*>).orEmpty().mapNotNull { raw ->
            val row = raw as? Map<*, *> ?: return@mapNotNull null
            val uid = row["ownerUid"] as? String ?: return@mapNotNull null
            ManagedFamilyProfile(
                ownerUid = uid,
                role = row["role"] as? String ?: "",
                permissions = (row["permissions"] as? List<*>).orEmpty().filterIsInstance<String>()
            )
        }
        return FamilyAccessSnapshot(delegates, managed)
    }

    suspend fun createInvite(role: String, canEdit: Boolean): FamilyInvite {
        val permissions = buildList {
            add("VIEW_PROFILE")
            if (canEdit) add("EDIT_PROFILE")
        }
        val result = functions.getHttpsCallable("createFamilyDelegateInvite")
            .call(mapOf("role" to role, "permissions" to permissions))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid family invite response")
        return FamilyInvite(
            inviteToken = data["inviteToken"] as? String ?: error("Missing family invite token"),
            expiresAtMillis = (data["expiresAtMillis"] as? Number)?.toLong() ?: 0L,
            role = data["role"] as? String ?: role,
            permissions = (data["permissions"] as? List<*>).orEmpty().filterIsInstance<String>()
        )
    }

    suspend fun acceptInvite(token: String) {
        require(token.isNotBlank()) { "Enter the family invite token" }
        functions.getHttpsCallable("acceptFamilyDelegateInvite")
            .call(mapOf("inviteToken" to token.trim()))
            .await()
    }

    suspend fun revoke(delegateUid: String) {
        require(delegateUid.isNotBlank())
        functions.getHttpsCallable("revokeFamilyDelegate")
            .call(mapOf("delegateUid" to delegateUid))
            .await()
    }
}
