package com.match.app.data.remote

import retrofit2.http.*

/**
 * Retrofit interface to the Express backend.
 */
interface MatchApi {
    @GET("api/health")            suspend fun health(): HealthResponse
    @GET("api/meta/regions")      suspend fun regions(): RegionsResponse
    @GET("api/community/groups")  suspend fun communityGroups(): CommunityGroupsResponse

    // ── Auth ─────────────────────────────────────────────────────────────
    @POST("api/auth/google")      suspend fun googleAuth(@Body body: GoogleAuthRequest): AuthTokenResponse
    @POST("api/auth/phone/send")  suspend fun sendPhoneOtp(@Body body: PhoneOtpRequest): SimpleResponse
    @POST("api/auth/phone/verify")suspend fun verifyPhoneOtp(@Body body: PhoneOtpVerifyRequest): AuthTokenResponse

    // ── Account ───────────────────────────────────────────────────────────
    @DELETE("api/account")        suspend fun deleteAccount(): SimpleResponse
    @GET("api/account/export")    suspend fun exportAccountData(): AccountExportResponse

    // ── Profiles ─────────────────────────────────────────────────────────
    @GET("api/profiles")          suspend fun getProfiles(@Query("page") page: Int = 1, @Query("limit") limit: Int = 20): ProfilesResponse
    @GET("api/profiles/{id}")     suspend fun getProfile(@Path("id") id: Long): ProfileResponse
    @PUT("api/profiles/{id}")     suspend fun updateProfile(@Path("id") id: Long, @Body body: UpdateProfileRequest): ProfileResponse
    @GET("api/profiles/{id}/views") suspend fun getProfileViews(@Path("id") id: Long): ProfileViewsResponse

    // ── Matches ───────────────────────────────────────────────────────────
    @GET("api/matches")           suspend fun getMatches(@Query("page") page: Int = 1): ProfilesResponse
    @GET("api/matches/daily")     suspend fun getDailyMatches(): ProfilesResponse

    // ── Likes / Interests ─────────────────────────────────────────────────
    @POST("api/likes/{targetId}") suspend fun sendLike(@Path("targetId") targetId: Long): SimpleResponse
    @DELETE("api/likes/{targetId}") suspend fun removeLike(@Path("targetId") targetId: Long): SimpleResponse
    @GET("api/likes/received")    suspend fun getLikesReceived(): LikesResponse
    @GET("api/likes/sent")        suspend fun getLikesSent(): LikesResponse

    // ── Shortlists ────────────────────────────────────────────────────────
    @POST("api/shortlists/{targetId}") suspend fun addShortlist(@Path("targetId") targetId: Long): SimpleResponse
    @DELETE("api/shortlists/{targetId}") suspend fun removeShortlist(@Path("targetId") targetId: Long): SimpleResponse

    // ── Photos ───────────────────────────────────────────────────────────
    @Multipart
    @POST("api/photos")
    suspend fun uploadPhoto(
        @retrofit2.http.Part photo: okhttp3.MultipartBody.Part,
        @retrofit2.http.Part("isPrimary") isPrimary: okhttp3.RequestBody
    ): PhotoUploadResponse
    @DELETE("api/photos/{photoId}") suspend fun deletePhoto(@Path("photoId") photoId: Long): SimpleResponse

    // ── Payments ─────────────────────────────────────────────────────────
    @POST("api/payments/verify")  suspend fun verifyPayment(@Body body: PaymentVerifyRequest): PaymentVerifyResponse
    @GET("api/payments/history")  suspend fun getPaymentHistory(): PaymentHistoryResponse

    // ── Verification / KYC ───────────────────────────────────────────────
    @Multipart
    @POST("api/verify/document")
    suspend fun uploadVerificationDoc(
        @retrofit2.http.Part doc: okhttp3.MultipartBody.Part,
        @retrofit2.http.Part("docType") docType: okhttp3.RequestBody
    ): SimpleResponse
    @GET("api/verify/status")     suspend fun getVerificationStatus(): VerificationStatusResponse

    // ── Device token ──────────────────────────────────────────────────────
    @POST("api/device/fcm-token") suspend fun registerFcmToken(@Body body: FcmTokenRequest): SimpleResponse

    // ── Safety ────────────────────────────────────────────────────────────
    @POST("api/report/profile")   suspend fun reportProfile(@Body body: ReportProfileRequest): SimpleResponse
    @POST("api/block/{targetId}") suspend fun blockUser(@Path("targetId") targetId: Long): SimpleResponse
    @DELETE("api/block/{targetId}") suspend fun unblockUser(@Path("targetId") targetId: Long): SimpleResponse
}

