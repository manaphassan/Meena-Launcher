package my.hana.meena

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import my.hana.meena.data.preferences.SettingsRepository
import my.hana.meena.data.system.InstalledAppsRepository

/**
 * Starts the two slowest cold-start reads before the activity exists, so they overlap with
 * window creation instead of running after it: the first DataStore read (about 0.5 s cold)
 * and the installed-apps scan (about 0.4 s). Both are cached by their owners.
 */
class MeenaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            runCatching {
                val settingsRepository = SettingsRepository(this@MeenaApp)
                settingsRepository.removeGeminiLeftovers()
                settingsRepository.userSettingsFlow.first()
            }
        }
        scope.launch { runCatching { InstalledAppsRepository.loadInstalledApps(this@MeenaApp) } }
        // Link changes are only announced as they happen, so start listening as early as possible.
        runCatching { my.hana.meena.data.system.BluetoothDevicesRepository.startListening(this) }
        // Battery levels for the phone section's time-left estimate; kept on the phone only.
        runCatching { my.hana.meena.data.system.BatteryHistory.startListening(this) }
    }
}
