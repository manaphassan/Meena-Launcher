package my.hana.meena.data.system

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Qibla and compass maths, kept free of Android so they can be unit-tested. */
object Qibla {
    /** The Kaaba, Makkah (21°25′21″ N, 39°49′34″ E). */
    const val KAABA_LAT = 21.4225
    const val KAABA_LNG = 39.8262

    /** Great-circle bearing from a place to the Kaaba, degrees clockwise from true north (0 to 360). */
    fun bearing(lat: Double, lng: Double): Double {
        val p1 = Math.toRadians(lat)
        val p2 = Math.toRadians(KAABA_LAT)
        val dl = Math.toRadians(KAABA_LNG - lng)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return normalize(Math.toDegrees(atan2(y, x)))
    }

    fun normalize(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0

    /** Moves [from] towards [to] by [alpha] the short way round, so 359 to 1 doesn't spin. */
    fun smooth(from: Double, to: Double, alpha: Double): Double {
        val diff = ((to - from + 540.0) % 360.0) - 180.0
        return normalize(from + diff * alpha)
    }

    /** Smallest angle between two headings, 0 to 180. */
    fun difference(a: Double, b: Double): Double = kotlin.math.abs(((a - b + 540.0) % 360.0) - 180.0)
}

/**
 * Which way the top of the phone points, from the rotation vector sensor, corrected from
 * magnetic to true north with the declination at the last fix. No permission needed.
 * https://developer.android.com/reference/android/hardware/SensorManager#getOrientation(float[],%20float[])
 * https://developer.android.com/reference/android/hardware/GeomagneticField
 */
class CompassHeading(
    context: Context,
    private val onHeading: (Double) -> Unit,
    private val onUnsteady: (Boolean) -> Unit
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val matrix = FloatArray(9)
    private val angles = FloatArray(3)
    private var smoothed: Double? = null
    private var lastSent: Double? = null
    private var declination = 0f

    val isAvailable: Boolean get() = sensor != null

    fun setPlace(lat: Double, lng: Double, timeMillis: Long) {
        declination = runCatching { GeomagneticField(lat.toFloat(), lng.toFloat(), 0f, timeMillis).declination }.getOrDefault(0f)
    }

    fun start() {
        sensor ?: return
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        smoothed = null
        lastSent = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(matrix, event.values)
        SensorManager.getOrientation(matrix, angles)
        val heading = Qibla.normalize(Math.toDegrees(angles[0].toDouble()) + declination)
        val next = smoothed?.let { Qibla.smooth(it, heading, 0.2) } ?: heading
        smoothed = next
        // Only redraw the map when it moved enough to see.
        if (lastSent == null || Qibla.difference(lastSent!!, next) >= 1.0) {
            lastSent = next
            onHeading(next)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        onUnsteady(accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW)
    }
}
