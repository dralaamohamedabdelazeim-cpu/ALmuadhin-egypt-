package com.example.almuadhin.data.repo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fused = LocationServices.getFusedLocationProviderClient(context)
    private val prefs = context.getSharedPreferences("location_cache", Context.MODE_PRIVATE)

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): android.location.Location? {
        val cachedLat = prefs.getFloat("lat", 0f)
        val cachedLng = prefs.getFloat("lng", 0f)
        val cacheValid = cachedLat != 0f && cachedLng != 0f

        if (cacheValid) {
            val cached = android.location.Location("cache")
            cached.latitude = cachedLat.toDouble()
            cached.longitude = cachedLng.toDouble()
            return cached
        }
        return fetchAndCache()
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchAndCache(): android.location.Location? =
        suspendCancellableCoroutine { cont ->
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 10000L
            )
            .setMaxUpdates(1)
            .setWaitForAccurateLocation(true)
            .setMinUpdateDistanceMeters(0f)
            .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fused.removeLocationUpdates(this)
                    result.lastLocation?.let { save(it) }
                    cont.resume(result.lastLocation)
                }
            }
            fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
            cont.invokeOnCancellation { fused.removeLocationUpdates(callback) }
        }

    private fun save(loc: android.location.Location) {
        prefs.edit()
            .putFloat("lat", loc.latitude.toFloat())
            .putFloat("lng", loc.longitude.toFloat())
            .apply()
    }
}
