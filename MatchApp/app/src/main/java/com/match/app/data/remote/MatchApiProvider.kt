package com.match.app.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.match.app.data.session.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds [MatchApi] instances on demand against the user-configurable base URL
 * stored in [SessionStore].  Rebuilding is cheap and side-steps the need for a
 * scoped Retrofit instance whenever the URL changes.
 */
@Singleton
class MatchApiProvider @Inject constructor(
    private val session: SessionStore
) {
    init { instanceOrNull = this }

    companion object {
        /** Weak static ref so non-Hilt components (FCM service) can reach the provider. */
        @Volatile var instanceOrNull: MatchApiProvider? = null
            private set
    }
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            // NOTE: Certificate pinning will be added here once the real backend domain
            // and its TLS certificate SHA-256 pins are confirmed. Using real pins on an
            // unknown/placeholder domain would cause all API calls to fail with SSLException.
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun api(): MatchApi {
        val base = session.apiBaseUrl.first().trimEnd('/') + "/"
        return Retrofit.Builder()
            .baseUrl(base)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MatchApi::class.java)
    }
}
