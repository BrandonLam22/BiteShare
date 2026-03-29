package org.example.biteshare.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import org.example.biteshare.domain.GeoPoint
import kotlin.coroutines.resume

private const val LOCATION_TIMEOUT_MS = 5000L

@Composable
fun rememberAndroidLocationAccess(): LocationAccess {
    val context = LocalContext.current
    val locationManager = remember {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    val permissionState = remember { PermissionRequestState() }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionState.complete(granted)
    }

    return remember(context, locationManager, permissionState, launcher) {
        AndroidLocationAccess(
            context = context,
            locationManager = locationManager,
            permissionLauncher = launcher,
            permissionState = permissionState,
        )
    }
}

private class AndroidLocationAccess(
    private val context: Context,
    private val locationManager: LocationManager,
    private val permissionLauncher: ActivityResultLauncher<String>,
    private val permissionState: PermissionRequestState,
) : LocationAccess {

    override suspend fun requestCurrentLocation(): GeoPoint? {
        if (!ensurePermission()) return null
        val lastKnown = latestLastKnownLocation()
        val fresh = withTimeoutOrNull(LOCATION_TIMEOUT_MS) { requestSingleLocation() }
        val best = fresh ?: lastKnown
        return best?.let { GeoPoint(it.latitude, it.longitude) }
    }

    private fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private suspend fun ensurePermission(): Boolean {
        if (hasPermission()) return true
        val deferred = permissionState.begin()
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        return deferred.await()
    }

    private fun latestLastKnownLocation(): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).filter { provider -> locationManager.isProviderEnabled(provider) }

        return providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    }

    private suspend fun requestSingleLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            val provider = pickProvider()
            if (provider == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }

                override fun onProviderDisabled(provider: String) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }

            continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }

    private fun pickProvider(): String? {
        return when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER) ->
                LocationManager.PASSIVE_PROVIDER
            else -> null
        }
    }
}

private class PermissionRequestState {
    private var pending: CompletableDeferred<Boolean>? = null

    fun begin(): CompletableDeferred<Boolean> {
        val deferred = CompletableDeferred<Boolean>()
        pending = deferred
        return deferred
    }

    fun complete(granted: Boolean) {
        pending?.complete(granted)
        pending = null
    }
}
