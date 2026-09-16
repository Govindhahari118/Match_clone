package com.match.app.data.repo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.firebase.functions.FirebaseFunctions
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.UserEntity
import com.match.app.data.remote.FirestoreProfileService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A privacy-safe nearby result. Exact coordinates and precise distances never become UI data. */
data class NearbyProfile(
    val userId: Long,
    val firebaseUid: String,
    val displayName: String,
    val age: Int,
    val profession: String,
    val city: String,
    val photoUrl: String,
    val isVerified: Boolean,
    val isPremium: Boolean,
    /** Coarse representative retained for existing card rendering; not the computed exact distance. */
    val distanceKm: Double,
    val distanceLabel: String
)

data class NearbySharingStatus(
    val sharing: Boolean,
    val updatedAtMillis: Long,
    val expiresAtMillis: Long
)

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao,
    private val profileService: FirestoreProfileService
) {
    private val functions = FirebaseFunctions.getInstance()
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasPreciseLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun isLocationServiceEnabled(): Boolean = LocationManagerCompat.isLocationEnabled(locationManager)

    suspend fun getSharingStatus(): NearbySharingStatus = withContext(Dispatchers.IO) {
        val response = functions.getHttpsCallable("getNearbyStatus").call().await()
        @Suppress("UNCHECKED_CAST")
        val data = response.data as? Map<String, Any?> ?: error("Invalid Nearby status response")
        NearbySharingStatus(
            sharing = data["sharing"] as? Boolean ?: false,
            updatedAtMillis = (data["updatedAtMillis"] as? Number)?.toLong() ?: 0L,
            expiresAtMillis = (data["expiresAtMillis"] as? Number)?.toLong() ?: 0L
        )
    }

    suspend fun currentLocation(): Location {
        check(hasLocationPermission()) { "Location permission is required" }
        check(isLocationServiceEnabled()) { "Turn on device location to use Nearby" }

        val provider = when {
            hasPreciseLocationPermission() && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> throw IllegalStateException("No location provider is available")
        }

        return suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    provider,
                    cancellationSignal,
                    ContextCompat.getMainExecutor(context)
                ) { location ->
                    if (!continuation.isActive) return@getCurrentLocation
                    if (location != null) continuation.resume(location)
                    else continuation.resumeWithException(IllegalStateException("Unable to determine your current location"))
                }
            } catch (error: SecurityException) {
                continuation.resumeWithException(error)
            }
        }
    }

    suspend fun refreshAndFindNearby(radiusKm: Int): List<NearbyProfile> = withContext(Dispatchers.IO) {
        val location = currentLocation()
        updateRemoteLocation(location)
        findNearby(radiusKm)
    }

    suspend fun findNearby(radiusKm: Int): List<NearbyProfile> = withContext(Dispatchers.IO) {
        val safeRadius = radiusKm.coerceIn(5, 100)
        val response = functions.getHttpsCallable("nearbyProfiles")
            .call(mapOf("radiusKm" to safeRadius))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = response.data as? Map<String, Any?> ?: error("Invalid nearby response")
        val rawProfiles = data["profiles"] as? List<*> ?: emptyList<Any>()

        rawProfiles.mapNotNull { raw ->
            @Suppress("UNCHECKED_CAST")
            val entry = raw as? Map<String, Any?> ?: return@mapNotNull null
            val uid = entry["uid"] as? String ?: return@mapNotNull null
            val distanceLabel = entry["distanceBucket"] as? String ?: return@mapNotNull null
            val remote = runCatching { profileService.fetchProfile(uid) }.getOrNull() ?: return@mapNotNull null
            val cached = cacheRemoteProfile(remote)
            NearbyProfile(
                userId = cached.id,
                firebaseUid = uid,
                displayName = cached.displayName,
                age = cached.age,
                profession = cached.profession,
                city = cached.city,
                photoUrl = cached.photoUrl,
                isVerified = cached.isVerified,
                isPremium = cached.isPremium,
                distanceKm = representativeDistance(distanceLabel),
                distanceLabel = distanceLabel
            )
        }
    }

    suspend fun stopSharingLocation() = withContext(Dispatchers.IO) {
        functions.getHttpsCallable("clearMyLocation").call().await()
        Unit
    }

    private suspend fun updateRemoteLocation(location: Location) {
        functions.getHttpsCallable("updateMyLocation")
            .call(
                mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "accuracy" to location.accuracy.toDouble()
                )
            )
            .await()
    }

    private fun representativeDistance(label: String): Double = when (label) {
        "Less than 1 km away" -> 0.5
        "About 1 km away" -> 1.0
        "About 2–5 km away" -> 2.0
        "About 5–10 km away" -> 5.0
        "About 10–25 km away" -> 10.0
        "About 25–50 km away" -> 25.0
        else -> 50.0
    }

    private suspend fun cacheRemoteProfile(remote: UserEntity): UserEntity {
        val uid = remote.firebaseUid
        val existing = userDao.findByFirebaseUid(uid)
        if (existing != null) {
            val merged = remote.copy(
                id = existing.id,
                email = existing.email,
                passwordHash = "",
                isSeed = false
            )
            userDao.update(merged)
            return merged
        }
        val cached = remote.copy(
            email = "$uid@cache.invalid",
            passwordHash = "",
            isSeed = false
        )
        val id = userDao.insert(cached)
        return cached.copy(id = id)
    }
}
