package com.parengmark.fueltracker.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Plain [LocationManager] usage -- no Google Play Services / FusedLocationProvider
 * dependency, so this app's dependency footprint (and the risk of a Play
 * Services version conflict) stays as small as possible for a feature that
 * only needs a single rough fix, not continuous tracking.
 *
 * Every function here assumes the caller has already checked
 * ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION -- see NearbyStationsScreen.kt
 * for the runtime permission request.
 */
object LocationHelper {

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(context: Context): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        var best: Location? = null
        for (provider in manager.getProviders(true)) {
            val location = try {
                manager.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                null
            } ?: continue
            if (best == null || location.time > best!!.time) best = location
        }
        return best
    }

    @SuppressLint("MissingPermission")
    suspend fun requestSingleLocationUpdate(context: Context): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        } ?: return null

        return suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }

                @Deprecated("Deprecated in the platform, still required by the interface")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            try {
                manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            } catch (e: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
        }
    }
}
