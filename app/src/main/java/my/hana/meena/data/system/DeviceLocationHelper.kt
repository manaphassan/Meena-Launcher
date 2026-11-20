package my.hana.meena.data.system

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.abs

import android.location.Address
import android.location.Geocoder
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 4.0f,
    val isGpsActive: Boolean = false,
    val locationName: String = "detecting location...",
    val formattedCoordinates: String = "3.1466° n, 101.7112° e",
    val dmsCoordinates: String = "3°08'48\" n, 101°42'40\" e",
    /** State and postcode from the phone's address lookup, lowercase; null until known. */
    val region: String? = null,
    /** JAKIM prayer zone worked out from the address lookup; null when unknown or unclear. */
    val solatZone: String? = null,
    /** When the phone took this fix (Location.getTime); 0 for the built-in default. */
    val fixTimeMillis: Long = 0L
)

/** Chooses among the phone's saved fixes, kept free of Android so it can be unit-tested. */
object LocationPick {
    /** Saved fixes older than this are only used when there is nothing newer. */
    const val RECENT_MS = 10 * 60 * 1000L

    /** The sharpest fix from the last [RECENT_MS], else the newest one. */
    fun <T> best(fixes: List<T>, nowMillis: Long, timeOf: (T) -> Long, accuracyOf: (T) -> Float): T? {
        val recent = fixes.filter { nowMillis - timeOf(it) <= RECENT_MS }
        return recent.minByOrNull(accuracyOf) ?: fixes.maxByOrNull(timeOf)
    }
}

object DeviceLocationHelper {
    private const val TAG = "DeviceLocationHelper"

    // Default coordinates (Kuala Lumpur center)
    const val DEFAULT_LAT = 3.1466
    const val DEFAULT_LNG = 101.7112

    private val _locationFlow = MutableStateFlow(
        DeviceLocation(
            latitude = DEFAULT_LAT,
            longitude = DEFAULT_LNG,
            accuracyMeters = 10f,
            isGpsActive = false,
            locationName = "kuala lumpur",
            formattedCoordinates = formatCoordinates(DEFAULT_LAT, DEFAULT_LNG),
            dmsCoordinates = formatDms(DEFAULT_LAT, DEFAULT_LNG)
        )
    )
    val locationFlow: StateFlow<DeviceLocation> = _locationFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeListener: LocationListener? = null

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            hasPreciseLocation(context)

    /** True when the person chose "precise" (Android 12+ lets them keep it approximate). */
    fun hasPreciseLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Android's page for Meena, where location can be switched to precise. */
    fun openAppSettings(context: Context) {
        try {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }

    fun getCurrentLocation(context: Context): DeviceLocation {
        return _locationFlow.value
    }

    fun refreshLocation(context: Context) {
        if (!hasLocationPermission(context)) {
            Log.d(TAG, "Location access not granted yet")
            return
        }

        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return

            // 1. The phone's saved fixes: the sharpest recent one, else the newest.
            val known = listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            ).mapNotNull { provider ->
                try {
                    locationManager.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    Log.w(TAG, "Security exception reading $provider", e)
                    null
                } catch (e: Exception) {
                    Log.w(TAG, "Error checking $provider", e)
                    null
                }
            }
            val bestLocation = LocationPick.best(known, System.currentTimeMillis(), { it.time }, { it.accuracy })

            if (bestLocation != null) {
                applyLocation(context, bestLocation)
            }

            // 2a. Android 11+: ask once for a fresh fix, through the fused provider where there is one
            // (https://developer.android.com/reference/android/location/LocationManager#getCurrentLocation).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val provider = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        locationManager.hasProvider(LocationManager.FUSED_PROVIDER) &&
                        locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    hasPreciseLocation(context) && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    else -> null
                }
                if (provider != null) {
                    try {
                        locationManager.getCurrentLocation(provider, null, ContextCompat.getMainExecutor(context)) { fresh ->
                            if (fresh != null) applyLocation(context, fresh)
                        }
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Security exception requesting current location", e)
                    }
                }
                return
            }

            // 2b. Older Android: a quick single-shot listener to get a fresh fix
            if (activeListener == null) {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        applyLocation(context, location)
                        try {
                            locationManager.removeUpdates(this)
                        } catch (e: Exception) {
                            // Ignore
                        }
                        activeListener = null
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                activeListener = listener

                // Request on network or gps provider
                val mainHandler = Handler(Looper.getMainLooper())
                mainHandler.post {
                    try {
                        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
                        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper())
                        }
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Security exception requesting single update", e)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error requesting single update", e)
                    }
                }

                // Safety timeout to remove listener after 15 seconds
                mainHandler.postDelayed({
                    if (activeListener != null) {
                        try {
                            locationManager.removeUpdates(listener)
                        } catch (e: Exception) {
                            // Ignore
                        }
                        activeListener = null
                    }
                }, 15000L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in refreshLocation", e)
        }
    }

    private fun applyLocation(context: Context, location: Location) {
        val lat = location.latitude
        val lng = location.longitude
        val formatted = formatCoordinates(lat, lng)
        val dms = formatDms(lat, lng)
        val previous = _locationFlow.value

        _locationFlow.value = DeviceLocation(
            latitude = lat,
            longitude = lng,
            accuracyMeters = location.accuracy,
            isGpsActive = true,
            // Keep the last name while the new one is looked up, instead of flashing a placeholder.
            locationName = if (previous.isGpsActive) previous.locationName else "current location",
            formattedCoordinates = formatted,
            dmsCoordinates = dms,
            region = if (previous.isGpsActive) previous.region else null,
            solatZone = if (previous.isGpsActive) previous.solatZone else null,
            fixTimeMillis = location.time
        )

        // Asynchronously resolve locality
        scope.launch {
            val place = resolveLocationName(context, lat, lng)
            _locationFlow.value = _locationFlow.value.copy(
                locationName = place.name,
                region = place.region,
                solatZone = place.solatZone
            )
        }
    }

    private data class ResolvedPlace(val name: String, val region: String?, val solatZone: String?)

    /** Area name ("sub-area, town"), region ("state postcode", lowercase) and the prayer zone. */
    private suspend fun resolveLocationName(context: Context, lat: Double, lng: Double): ResolvedPlace {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val address: Address? = addresses?.firstOrNull()
                if (address != null) {
                    val locality = address.locality ?: address.subAdminArea ?: address.adminArea
                    val subLocality = address.subLocality
                    val result = when {
                        !subLocality.isNullOrBlank() && !locality.isNullOrBlank() -> "$subLocality, $locality"
                        !locality.isNullOrBlank() -> locality
                        else -> address.countryName ?: "current location"
                    }
                    val region = listOfNotNull(
                        address.adminArea?.takeIf { it.isNotBlank() && it != locality },
                        address.postalCode?.takeIf { it.isNotBlank() }
                    ).joinToString(" ").ifBlank { null }
                    val zone = SolatZoneMatcher.zoneFor(
                        state = address.adminArea,
                        places = listOf(address.subAdminArea, address.locality, address.subLocality)
                    )
                    ResolvedPlace(result.lowercase(), region?.lowercase(), zone)
                } else {
                    ResolvedPlace(formatCoordinates(lat, lng), null, null)
                }
            } catch (e: Exception) {
                ResolvedPlace(formatCoordinates(lat, lng), null, null)
            }
        }
    }

    fun formatCoordinates(lat: Double, lng: Double): String {
        val latDir = if (lat >= 0) "n" else "s"
        val lngDir = if (lng >= 0) "e" else "w"
        return String.format(Locale.US, "%.4f° %s, %.4f° %s", abs(lat), latDir, abs(lng), lngDir)
    }

    fun formatDms(lat: Double, lng: Double): String {
        val latDir = if (lat >= 0) "n" else "s"
        val lngDir = if (lng >= 0) "e" else "w"

        val latAbs = abs(lat)
        val latDeg = latAbs.toInt()
        val latMin = ((latAbs - latDeg) * 60).toInt()
        val latSec = ((latAbs - latDeg - latMin / 60.0) * 3600).toInt()

        val lngAbs = abs(lng)
        val lngDeg = lngAbs.toInt()
        val lngMin = ((lngAbs - lngDeg) * 60).toInt()
        val lngSec = ((lngAbs - lngDeg - lngMin / 60.0) * 3600).toInt()

        return String.format(
            Locale.US,
            "%d°%02d'%02d\" %s, %d°%02d'%02d\" %s",
            latDeg, latMin, latSec, latDir,
            lngDeg, lngMin, lngSec, lngDir
        )
    }

    fun openInExternalMaps(context: Context, lat: Double, lng: Double, label: String = "location") {
        try {
            val encodedLabel = Uri.encode(label)
            val uri = "geo:$lat,$lng?q=$lat,$lng($encodedLabel)".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val webUri = "https://www.openstreetmap.org/?mlat=$lat&mlon=$lng#map=15/$lat/$lng".toUri()
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }
}

