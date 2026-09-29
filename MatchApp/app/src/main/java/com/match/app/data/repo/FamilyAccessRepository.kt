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

data class ManagedFamilyProfileDetail(
    val ownerUid: String,
    val displayName: String,
    val city: String,
    val bio: String,
    val education: String,
    val profession: String,
    val maritalStatus: String,
    val heightCm: Int,
    val countryOfResidence: String,
    val willingToRelocate: Boolean,
    val profileRevision: Long
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

    suspend fun getManagedProfile(ownerUid: String): ManagedFamilyProfileDetail {
        require(ownerUid.isNotBlank())
        val result = functions.getHttpsCallable("getFamilyManagedProfile")
            .call(mapOf("ownerUid" to ownerUid.trim()))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid managed-profile response")
        @Suppress("UNCHECKED_CAST")
        val profile = data["profile"] as? Map<String, Any?> ?: error("Managed profile missing")
        return ManagedFamilyProfileDetail(
            ownerUid = profile["uid"] as? String ?: ownerUid,
            displayName = profile["displayName"] as? String ?: "",
            city = profile["city"] as? String ?: "",
            bio = profile["bio"] as? String ?: "",
            education = profile["education"] as? String ?: "",
            profession = profile["profession"] as? String ?: "",
            maritalStatus = profile["maritalStatus"] as? String ?: "",
            heightCm = (profile["heightCm"] as? Number)?.toInt() ?: 0,
            countryOfResidence = profile["countryOfResidence"] as? String ?: "",
            willingToRelocate = profile["willingToRelocate"] == true,
            profileRevision = (profile["profileRevision"] as? Number)?.toLong() ?: 0L
        )
    }

    suspend fun updateManagedProfile(
        ownerUid: String,
        expectedRevision: Long,
        city: String,
        bio: String,
        education: String,
        profession: String
    ): Long {
        require(ownerUid.isNotBlank())
        val patch = mapOf(
            "city" to city.trim().take(160),
            "bio" to bio.trim().take(1000),
            "education" to education.trim().take(160),
            "profession" to profession.trim().take(160)
        )
        val result = functions.getHttpsCallable("updateFamilyManagedProfile")
            .call(
                mapOf(
                    "ownerUid" to ownerUid.trim(),
                    "expectedRevision" to expectedRevision,
                    "patch" to patch
                )
            )
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid managed-profile update")
        return (data["profileRevision"] as? Number)?.toLong()
            ?: error("Missing profile revision")
    }

    suspend fun revoke(delegateUid: String) {
        require(delegateUid.isNotBlank())
        functions.getHttpsCallable("revokeFamilyDelegate")
            .call(mapOf("delegateUid" to delegateUid))
            .await()
    }
}
