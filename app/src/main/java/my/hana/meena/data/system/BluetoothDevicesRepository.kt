package my.hana.meena.data.system

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import java.util.concurrent.ConcurrentHashMap
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
 * Bluetooth devices connected right now: low-energy devices (GATT), headphones and speakers
 * (A2DP, HEADSET), plus anything Android reports a link to while Meena runs (ACL broadcasts).
 * The ACL path is what catches bands that use the input-device (HID host) profile, which has no
 * public query API. Names are only shown, never logged or sent.
 */
object BluetoothDevicesRepository {
    const val KIND_WEARABLE = "wearable"
    const val KIND_AUDIO = "audio"
    const val KIND_OTHER = "other"

    const val ERROR_OFF = "bluetooth is off"
    const val ERROR_NO_BLUETOOTH = "this phone has no bluetooth"

    private val _state = MutableStateFlow<WidgetState<List<ConnectedDevice>>>(WidgetState.NeedsSetup)
    val state: StateFlow<WidgetState<List<ConnectedDevice>>> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeJob: Job? = null

    /** Devices with a live link reported by ACL broadcasts since Meena started: address to (name, kind). */
    private val aclDevices = ConcurrentHashMap<String, Pair<String, String>>()
    @Volatile private var isListening = false

    private val aclReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> aclDevices[device.address] = nameOf(device) to kindOf(device)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> aclDevices.remove(device.address)
            }
            refresh(context)
        }
    }

    /**
     * Registers for link changes for the life of the app. RECEIVER_EXPORTED is needed because the
     * Bluetooth stack is a privileged app, not the system UID (see the broadcasts guide); the ACL
     * actions are protected broadcasts, so other apps can't fake them [I].
     */
    fun startListening(context: Context) {
        if (isListening) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ContextCompat.registerReceiver(context.applicationContext, aclReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        isListening = true
    }

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
                    wearables.map { nameOf(it) to KIND_WEARABLE } + aclDevices.values,
                    audio.map { nameOf(it) to KIND_AUDIO }
                )
                _state.value = if (devices.isEmpty()) WidgetState.Empty else WidgetState.Ready(devices, System.currentTimeMillis())
            } catch (_: SecurityException) {
                _state.value = WidgetState.NeedsSetup
            }
        }
    }

    /** Dedupes by name (a headset shows up on several links); entries in [audio] win. */
    fun merge(wearables: List<Pair<String, String>>, audio: List<Pair<String, String>>): List<ConnectedDevice> {
        val byName = linkedMapOf<String, String>()
        (audio + wearables).forEach { (name, kind) -> if (name !in byName) byName[name] = kind }
        return byName.map { ConnectedDevice(it.key, it.value) }.sortedBy { it.name }
    }

    @SuppressLint("MissingPermission")
    private fun kindOf(device: BluetoothDevice): String =
        when (runCatching { device.bluetoothClass?.majorDeviceClass }.getOrNull()) {
            BluetoothClass.Device.Major.WEARABLE -> KIND_WEARABLE
            BluetoothClass.Device.Major.AUDIO_VIDEO -> KIND_AUDIO
            else -> KIND_OTHER
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
