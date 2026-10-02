package com.example.meenalauncher.data.system

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

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 4.0f,
    val isGpsActive: Boolean = false,
    val locationName: String = "Bukit Bintang, Kuala Lumpur",
    val formattedCoordinates: String = "3.1466° N, 101.7112° E",
    val dmsCoordinates: String = "3°08'48\" N, 101°42'40\" E"
)

object DeviceLocationHelper {
    private const val TAG = "DeviceLocationHelper"

    // Default Kuala Lumpur coordinates
    const val DEFAULT_LAT = 3.1466
    const val DEFAULT_LNG = 101.7112

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun getCurrentLocation(context: Context): DeviceLocation {
        if (!hasLocationPermission(context)) {
            return DeviceLocation(
                latitude = DEFAULT_LAT,
                longitude = DEFAULT_LNG,
                accuracyMeters = 10f,
                isGpsActive = false,
                locationName = "Bukit Bintang, Kuala Lumpur",
                formattedCoordinates = formatCoordinates(DEFAULT_LAT, DEFAULT_LNG),
                dmsCoordinates = formatDms(DEFAULT_LAT, DEFAULT_LNG)
            )
        }

        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                var bestLocation: Location? = null

                try {
                    val gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (gpsLoc != null) {
                        bestLocation = gpsLoc
                    }
                } catch (e: SecurityException) {
                    Log.w(TAG, "GPS location permission error", e)
                }

                try {
                    val netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (netLoc != null && (bestLocation == null || netLoc.time > bestLocation.time)) {
                        bestLocation = netLoc
                    }
                } catch (e: SecurityException) {
                    Log.w(TAG, "Network location permission error", e)
                }

                if (bestLocation != null) {
                    val lat = bestLocation.latitude
                    val lng = bestLocation.longitude
                    return DeviceLocation(
                        latitude = lat,
                        longitude = lng,
                        accuracyMeters = bestLocation.accuracy,
                        isGpsActive = true,
                        locationName = "Current GPS Location",
                        formattedCoordinates = formatCoordinates(lat, lng),
                        dmsCoordinates = formatDms(lat, lng)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
        }

        return DeviceLocation(
            latitude = DEFAULT_LAT,
            longitude = DEFAULT_LNG,
            accuracyMeters = 8f,
            isGpsActive = false,
            locationName = "Bukit Bintang, Kuala Lumpur",
            formattedCoordinates = formatCoordinates(DEFAULT_LAT, DEFAULT_LNG),
            dmsCoordinates = formatDms(DEFAULT_LAT, DEFAULT_LNG)
        )
    }

    fun formatCoordinates(lat: Double, lng: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"
        return String.format(Locale.US, "%.4f° %s, %.4f° %s", abs(lat), latDir, abs(lng), lngDir)
    }

    fun formatDms(lat: Double, lng: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"

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

    fun openInExternalMaps(context: Context, lat: Double, lng: Double, label: String = "Location") {
        try {
            val encodedLabel = Uri.encode(label)
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to browser
            val webUri = Uri.parse("https://www.openstreetmap.org/?mlat=$lat&mlon=$lng#map=15/$lat/$lng")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }
}
