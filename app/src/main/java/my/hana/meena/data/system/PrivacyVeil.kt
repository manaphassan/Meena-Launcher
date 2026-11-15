package my.hana.meena.data.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * Hides chats, email, alerts, notes and money on the home pages, for when someone is looking over
 * your shoulder. Shaking the phone switches it on and off. Kept in memory only, so Meena always
 * starts with everything showing.
 */
object PrivacyVeil {
    private val _hidden = MutableStateFlow(false)
    val hidden: StateFlow<Boolean> = _hidden.asStateFlow()

    fun toggle(): Boolean {
        _hidden.value = !_hidden.value
        return _hidden.value
    }

    fun show() {
        _hidden.value = false
    }
}

/**
 * Spots a deliberate shake in accelerometer readings: two strong jolts (over [THRESHOLD_G] times
 * gravity) within [WINDOW_MS], then a [COOLDOWN_MS] pause so one shake counts once. A single bump,
 * walking or putting the phone down stays well under the threshold. Free of Android for unit tests.
 */
class ShakeDetector(private val onShake: () -> Unit) {
    private var firstJoltAt = -1L
    private var lastShakeAt = -COOLDOWN_MS

    /** [x], [y], [z] in m/s², as SensorEvent.values gives them; [nowMs] any steady clock. */
    fun onReading(x: Float, y: Float, z: Float, nowMs: Long) {
        val g = sqrt(x * x + y * y + z * z) / GRAVITY
        if (g < THRESHOLD_G) return
        if (nowMs - lastShakeAt < COOLDOWN_MS) return
        if (firstJoltAt >= 0 && nowMs - firstJoltAt in MIN_GAP_MS..WINDOW_MS) {
            firstJoltAt = -1L
            lastShakeAt = nowMs
            onShake()
        } else if (firstJoltAt < 0 || nowMs - firstJoltAt > WINDOW_MS) {
            firstJoltAt = nowMs
        }
    }

    companion object {
        const val GRAVITY = 9.80665f
        const val THRESHOLD_G = 2.5f
        const val MIN_GAP_MS = 80L
        const val WINDOW_MS = 600L
        const val COOLDOWN_MS = 1200L
    }
}
