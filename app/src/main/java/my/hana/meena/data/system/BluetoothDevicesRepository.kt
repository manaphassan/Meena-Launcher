package my.hana.meena.data.system

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import my.hana.meena.ui.widgets.WidgetState
import kotlin.coroutines.resume

data class ConnectedDevice(val name: String, val kind: String)

/**
 * Bluetooth devices connected right now: low-energy devices such as bands and watches (GATT),
 * plus headphones and speakers (A2DP, HEADSET). Names are only shown, never logged or sent.
 */
object BluetoothDevicesRepository {
    const val KIND_WEARABLE = "wearable"
    const val KIND_AUDIO = "audio"

    const val ERROR_OFF = "bluetooth is off"
    const val ERROR_NO_BLUETOOTH = "this phone has no bluetooth"

    private val _state = MutableStateFlow<WidgetState<List<ConnectedDevice>>>(WidgetState.NeedsSetup)
    val state: StateFlow<WidgetState<List<ConnectedDevice>>> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeJob: Job? = null

    /** Android 12+ needs BLUETOOTH_CONNECT at runtime; older versions use the install-time BLUETOOTH permission. */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // checked by hasPermission() first
    fun refresh(context: Context) {
        if (!hasPermission(context)) {
            _state.value = WidgetState.NeedsSetup
            return
        }
        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter = manager?.adapter
        if (manager == null || adapter == null) {
            _state.value = WidgetState.Error(ERROR_NO_BLUETOOTH)
            return
        }
        if (!adapter.isEnabled) {
            _state.value = WidgetState.Error(ERROR_OFF)
            return
        }
        if (activeJob?.isActive == true) return
        if (_state.value !is WidgetState.Ready) _state.value = WidgetState.Loading

        activeJob = scope.launch {
            try {
                val wearables = runCatching { manager.getConnectedDevices(BluetoothProfile.GATT) }.getOrDefault(emptyList())
                val audio = connectedOn(context, adapter, BluetoothProfile.A2DP) +
                    connectedOn(context, adapter, BluetoothProfile.HEADSET)
                val devices = merge(
                    wearables.map { nameOf(it) to KIND_WEARABLE },
                    audio.map { nameOf(it) to KIND_AUDIO }
                )
                _state.value = if (devices.isEmpty()) WidgetState.Empty else WidgetState.Ready(devices, System.currentTimeMillis())
            } catch (_: SecurityException) {
                _state.value = WidgetState.NeedsSetup
            }
        }
    }

    /** Dedupes by name (a headset shows up on both A2DP and HEADSET); audio wins over wearable. */
    fun merge(wearables: List<Pair<String, String>>, audio: List<Pair<String, String>>): List<ConnectedDevice> {
        val byName = linkedMapOf<String, String>()
        (audio + wearables).forEach { (name, kind) -> if (name !in byName) byName[name] = kind }
        return byName.map { ConnectedDevice(it.key, it.value) }.sortedBy { it.name }
    }

    @SuppressLint("MissingPermission")
    private fun nameOf(device: BluetoothDevice): String =
        runCatching { device.name }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.lowercase() ?: "unnamed device"

    @SuppressLint("MissingPermission")
    private suspend fun connectedOn(context: Context, adapter: BluetoothAdapter, profile: Int): List<BluetoothDevice> =
        withTimeoutOrNull(2_000L) {
            suspendCancellableCoroutine { cont ->
                val listener = object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(p: Int, proxy: BluetoothProfile) {
                        val list = runCatching { proxy.connectedDevices }.getOrDefault(emptyList())
                        adapter.closeProfileProxy(p, proxy)
                        if (cont.isActive) cont.resume(list)
                    }
                    override fun onServiceDisconnected(p: Int) {
                        if (cont.isActive) cont.resume(emptyList())
                    }
                }
                if (!adapter.getProfileProxy(context, listener, profile) && cont.isActive) cont.resume(emptyList())
            }
        } ?: emptyList()
}
