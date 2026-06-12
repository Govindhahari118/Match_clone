package com.match.app.data.remote

import kotlinx.serialization.Serializable

/**
 * DTOs that mirror the WEB_MATCH backend (`/api/meta`, `/api/community`, `/api/matches`).
 * Fields are intentionally permissive (nullable / default) so a partial backend
 * response â€” or a different version of the API â€” does not crash deserialization.
 *
 * Actual `/api/meta/regions` response shape (port 4000):
 * { "success": true, "data": { "updatedAt": "...", "presets": [ { "slug": "...", "title": "...",
 *   "description": "...", "filters": { "city": ..., "state": ..., "motherTongue": ..., "caste": ... },
 *   "tags": [...] } ] } }
 */

// â”€â”€ Regions â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Serializable
data class RegionPresetFiltersDto(
    val city: String? = null,
    val state: String? = null,
    val motherTongue: String? = null,
    val caste: String? = null,
    val religion: String? = null,
    val country: String? = null,
    val occupationCategory: String? = null,
    val nriStatus: String? = null,
    val maritalStatus: String? = null
)

@Serializable
data class RegionPresetDto(
    val slug: String? = null,
    val title: String = "",
    val description: String? = null,
    val filters: RegionPresetFiltersDto = RegionPresetFiltersDto(),
    val tags: List<String> = emptyList()
)

@Serializable
data class RegionDataDto(
    val presets: List<RegionPresetDto> = emptyList(),
    val updatedAt: String? = null
)

/** Top-level wrapper returned by GET /api/meta/regions */
@Serializable
data class RegionsResponse(
    val success: Boolean? = null,
    val data: RegionDataDto? = null
)

// â”€â”€ Community groups â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Serializable
data class CommunityGroupDto(
    val id: String? = null,
    val name: String = "",
    val description: String? = null,
    val category: String? = null,
    val memberCount: Int? = null,
    val religion: String? = null,
    val caste: String? = null,
    val city: String? = null,
    val motherTongue: String? = null
)

@Serializable
data class CommunityGroupsResponse(
    val groups: List<CommunityGroupDto> = emptyList()
)

// â”€â”€ Health â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Serializable
data class HealthResponse(
    val status: String? = null,
    val service: String? = null,
    val version: String? = null
)

// ── Auth ─────────────────────────────────────────────────────────────────────

@Serializable
data class GoogleAuthRequest(
    val idToken: String,
    val displayName: String? = null,
    val email: String? = null
)

@Serializable
data class AuthTokenResponse(
    val success: Boolean = false,
    val token: String? = null,
    val userId: Long? = null,
    val message: String? = null
)

// ── FCM ──────────────────────────────────────────────────────────────────────

@Serializable
data class FcmTokenRequest(
    val fcmToken: String,
    val platform: String = "android"
)

@Serializable
data class SimpleResponse(
    val success: Boolean = false,
    val message: String? = null
)

// ── Report ───────────────────────────────────────────────────────────────────

@Serializable
data class ReportProfileRequest(
    val reporterId: Long,
    val targetId: Long,
    val reason: String
)

// ── Phone OTP ─────────────────────────────────────────────────────────────────

@Serializable
data class PhoneOtpRequest(
    val phone: String,
    val countryCode: String = "+91"
)

@Serializable
data class PhoneOtpVerifyRequest(
    val phone: String,
    val otp: String,
    val countryCode: String = "+91"
)

// ── Account Export ────────────────────────────────────────────────────────────

@Serializable
data class AccountExportResponse(
    val success: Boolean = false,
    val dataUrl: String? = null,
    val message: String? = null
)

// ── Profiles ──────────────────────────────────────────────────────────────────

@Serializable
data class ProfileDto(
    val id: Long = 0,
    val displayName: String = "",
    val age: Int = 0,
    val gender: String = "",
    val city: String = "",
    val state: String? = null,
    val religion: String? = null,
    val caste: String? = null,
    val motherTongue: String? = null,
    val education: String? = null,
    val profession: String? = null,
    val maritalStatus: String? = null,
    val heightCm: Int? = null,
    val bio: String? = null,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false,
    val photoUrl: String? = null,
    val createdAt: String? = null,
    val nativeState: String? = null,
    val residentialStatus: String? = null,
    val countryOfResidence: String? = null
)

@Serializable
data class ProfilesResponse(
    val success: Boolean = false,
    val profiles: List<ProfileDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val hasMore: Boolean = false
)

@Serializable
data class ProfileResponse(
    val success: Boolean = false,
    val profile: ProfileDto? = null
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val city: String? = null,
    val state: String? = null,
    val profession: String? = null,
    val education: String? = null,
    val nativeState: String? = null,
    val residentialStatus: String? = null,
    val countryOfResidence: String? = null
)

@Serializable
data class ProfileViewsResponse(
    val success: Boolean = false,
    val views: List<ProfileViewDto> = emptyList()
)

@Serializable
data class ProfileViewDto(
    val viewerId: Long = 0,
    val viewerName: String = "",
    val viewerPhotoUrl: String? = null,
    val viewedAt: String = ""
)

// ── Likes ─────────────────────────────────────────────────────────────────────

@Serializable
data class LikesResponse(
    val success: Boolean = false,
    val likes: List<LikeDto> = emptyList()
)

@Serializable
data class LikeDto(
    val userId: Long = 0,
    val displayName: String = "",
    val photoUrl: String? = null,
    val sentAt: String = ""
)

// ── Photos ────────────────────────────────────────────────────────────────────

@Serializable
data class PhotoUploadResponse(
    val success: Boolean = false,
    val photoId: Long? = null,
    val url: String? = null,
    val message: String? = null
)

// ── Payments ──────────────────────────────────────────────────────────────────

@Serializable
data class PaymentVerifyRequest(
    val razorpayOrderId: String,
    val razorpayPaymentId: String,
    val razorpaySignature: String,
    val planName: String,
    val amountPaise: Int
)

@Serializable
data class PaymentVerifyResponse(
    val success: Boolean = false,
    val isPremium: Boolean = false,
    val premiumUntil: String? = null,
    val message: String? = null
)

@Serializable
data class PaymentHistoryResponse(
    val success: Boolean = false,
    val payments: List<PaymentRecordDto> = emptyList()
)

@Serializable
data class PaymentRecordDto(
    val paymentId: String = "",
    val planName: String = "",
    val amountPaise: Int = 0,
    val status: String = "",
    val paidAt: String = ""
)

// ── Verification Status ───────────────────────────────────────────────────────

@Serializable
data class VerificationStatusResponse(
    val success: Boolean = false,
    val status: String = "UNVERIFIED",   // UNVERIFIED | PENDING | VERIFIED | REJECTED
    val message: String? = null,
    val reviewedAt: String? = null
)
