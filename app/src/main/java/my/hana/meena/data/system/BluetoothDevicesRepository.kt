package my.hana.meena.data.system

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
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

/**
 * A connected device. [battery] is a percentage and [signalDbm] the received signal strength,
 * each only when the device or Android has reported one; neither is ever estimated.
 */
data class ConnectedDevice(val name: String, val kind: String, val battery: Int? = null, val signalDbm: Int? = null)

/** Signal strength in words for people. The bands are rough [I]: Bluetooth doesn't define them. */
object BluetoothSignal {
    const val STRONG = 3
    const val GOOD = 2
    const val WEAK = 1

    fun level(dbm: Int): Int = when {
        dbm >= -60 -> STRONG
        dbm >= -75 -> GOOD
        else -> WEAK
    }
}

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

    /** Last battery level Android reported per device name (0 to 100). Never estimated. */
    private val batteryByName = ConcurrentHashMap<String, Int>()

    @Volatile
    private var isListening = false

    /**
     * Android announces a device's battery when it first reads it and whenever it changes, for
     * devices that report one to the phone (headphones over the headset link, LE battery service).
     * The action and extra are @SystemApi constants but the broadcast is delivered to apps holding
     * BLUETOOTH_CONNECT, so only their string values are used here. Source:
     * https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/main/framework/java/android/bluetooth/BluetoothDevice.java
     */
    private const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
    private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"

    /** BluetoothProfile.HID_HOST, a @SystemApi constant; its proxy answers getConnectedDevices with BLUETOOTH_CONNECT. */
    private const val PROFILE_HID_HOST = 4

    /** Standard Battery Service and Battery Level characteristic (Bluetooth SIG assigned numbers). */
    private val BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    private val BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    /** What a low-energy device last told us, by address. */
    private data class LeReading(val battery: Int?, val signalDbm: Int?, val atMillis: Long)
    private val leReadings = ConcurrentHashMap<String, LeReading>()

    /** How often each low-energy device is asked again; the widget refreshes every 30 s. */
    private const val LE_READ_EVERY_MS = 60_000L

    private val aclReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> aclDevices[device.address] = nameOf(device) to kindOf(device)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    aclDevices.remove(device.address)
                    batteryByName.remove(nameOf(device))
                }

                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                    if (level in 0..100) batteryByName[nameOf(device)] = level else batteryByName.remove(nameOf(device))
                }
            }
            if (intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) leReadings.remove(device.address)
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
            addAction(ACTION_BATTERY_LEVEL_CHANGED)
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
                val wearables =
                    runCatching { manager.getConnectedDevices(BluetoothProfile.GATT) }.getOrDefault(emptyList())
                // Bands like the Xiaomi Smart Band link as an input device (HID host).
                val inputDevices = connectedOn(context, adapter, PROFILE_HID_HOST)
                val audio = connectedOn(context, adapter, BluetoothProfile.A2DP) +
                    connectedOn(context, adapter, BluetoothProfile.HEADSET)
                val aclLinked = aclDevices.keys.mapNotNull { runCatching { adapter.getRemoteDevice(it) }.getOrNull() }
                // First device object per shown name, so readings can be matched back to a row.
                val byName = linkedMapOf<String, BluetoothDevice>()
                (audio + wearables + inputDevices + aclLinked).forEach { byName.putIfAbsent(nameOf(it), it) }
                val devices = merge(
                    wearables.map { nameOf(it) to KIND_WEARABLE } +
                            inputDevices.map { nameOf(it) to kindOf(it).let { k -> if (k == KIND_OTHER) KIND_WEARABLE else k } } +
                            aclDevices.values,
                    audio.map { nameOf(it) to KIND_AUDIO }
                )
                fun withReadings(): List<ConnectedDevice> = devices.map { device ->
                    val linked = byName[device.name]
                    val reading = linked?.let { leReadings[it.address] }
                    device.copy(
                        battery = batteryByName[device.name] ?: reading?.battery ?: linked?.let { systemBatteryLevel(it) },
                        signalDbm = reading?.signalDbm
                    )
                }
                _state.value =
                    if (devices.isEmpty()) WidgetState.Empty else WidgetState.Ready(withReadings(), System.currentTimeMillis())

                // Then ask low-energy devices that already have a link for signal and battery, one at a time.
                val leLinked = (wearables + inputDevices).distinctBy { it.address }.filter { isLowEnergy(it) }
                var readAny = false
                leLinked.forEach { device ->
                    val last = leReadings[device.address]
                    if (last != null && System.currentTimeMillis() - last.atMillis < LE_READ_EVERY_MS) return@forEach
                    leReadings[device.address] = readLowEnergy(context, device)
                    readAny = true
                }
                if (readAny && devices.isNotEmpty()) {
                    _state.value = WidgetState.Ready(withReadings(), System.currentTimeMillis())
                }
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
    private fun isLowEnergy(device: BluetoothDevice): Boolean =
        when (runCatching { device.type }.getOrNull()) {
            BluetoothDevice.DEVICE_TYPE_LE, BluetoothDevice.DEVICE_TYPE_DUAL -> true
            else -> false
        }

    /**
     * Joins the existing low-energy link as a GATT client, reads the signal strength
     * (readRemoteRssi) and, for paired devices, the standard battery level if offered, then leaves.
     * Only devices that are already connected are asked, so this never starts a new link.
     * Public APIs, BLUETOOTH_CONNECT only:
     * https://developer.android.com/reference/android/bluetooth/BluetoothGatt
     * https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback
     */
    @SuppressLint("MissingPermission")
    private suspend fun readLowEnergy(context: Context, device: BluetoothDevice): LeReading {
        val signal = AtomicReference<Int?>(null)
        // Reading from a device that isn't paired could make Android ask to pair; only ask for its signal then.
        val askBattery = runCatching { device.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)
        val battery = withTimeoutOrNull(8_000L) {
            suspendCancellableCoroutine<Int?> { cont ->
                val done = AtomicBoolean(false)
                val gattRef = AtomicReference<BluetoothGatt?>(null)
                fun finish(gatt: BluetoothGatt?, level: Int?) {
                    if (!done.compareAndSet(false, true)) return
                    runCatching { gatt?.disconnect() }
                    runCatching { gatt?.close() }
                    if (cont.isActive) cont.resume(level)
                }
                fun onBattery(gatt: BluetoothGatt, value: ByteArray?, status: Int) {
                    val level = value?.firstOrNull()?.toInt()?.and(0xFF)
                    finish(gatt, if (status == BluetoothGatt.GATT_SUCCESS && level != null && level in 0..100) level else null)
                }
                val callback = object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                        when (newState) {
                            BluetoothProfile.STATE_CONNECTED -> if (!gatt.readRemoteRssi()) {
                                if (!askBattery || !gatt.discoverServices()) finish(gatt, null)
                            }
                            BluetoothProfile.STATE_DISCONNECTED -> finish(gatt, null)
                        }
                    }

                    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
                        if (status == BluetoothGatt.GATT_SUCCESS) signal.set(rssi)
                        if (!askBattery || !gatt.discoverServices()) finish(gatt, null)
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                        val level = gatt.getService(BATTERY_SERVICE)?.getCharacteristic(BATTERY_LEVEL)
                        if (status != BluetoothGatt.GATT_SUCCESS || level == null || !gatt.readCharacteristic(level)) finish(gatt, null)
                    }

                    // Android 13+; both read callbacks may arrive, finish() only acts once.
                    override fun onCharacteristicRead(
                        gatt: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        value: ByteArray,
                        status: Int
                    ) = onBattery(gatt, value, status)

                    // Android 12 and older.
                    @Deprecated("Deprecated in Java")
                    @Suppress("DEPRECATION")
                    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) =
                        onBattery(gatt, characteristic.value, status)
                }
                cont.invokeOnCancellation { runCatching { gattRef.get()?.close() } }
                val gatt = runCatching { device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE) }.getOrNull()
                gattRef.set(gatt)
                if (gatt == null) finish(null, null)
            }
        }
        return LeReading(battery, signal.get(), System.currentTimeMillis())
    }

    /** Set once Android refuses the hidden battery call, so it isn't tried again this run. */
    @Volatile
    private var systemBatteryBlocked = false

    /**
     * The battery level Android already knows for a device (what the status bar shows for
     * headphones), so it shows right away instead of waiting for the next change broadcast.
     * BluetoothDevice.getBatteryLevel() is a @SystemApi; it is called by name and Android may
     * block it for normal apps [S], in which case this quietly returns null. Source:
     * https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/main/framework/java/android/bluetooth/BluetoothDevice.java
     */
    @SuppressLint("DiscouragedPrivateApi", "PrivateApi", "SoonBlockedPrivateApi")
    private fun systemBatteryLevel(device: BluetoothDevice): Int? {
        if (systemBatteryBlocked) return null
        return try {
            (BluetoothDevice::class.java.getMethod("getBatteryLevel").invoke(device) as? Int)?.takeIf { it in 0..100 }
        } catch (_: NoSuchMethodException) {
            systemBatteryBlocked = true
            null
        } catch (_: Exception) {
            null
        }
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
                        // Resume before closing: closeProfileProxy() calls onServiceDisconnected() right away,
                        // which used to resume first with an empty list and drop every result.
                        if (cont.isActive) cont.resume(list)
                        adapter.closeProfileProxy(p, proxy)
                    }
                    override fun onServiceDisconnected(p: Int) {
                        if (cont.isActive) cont.resume(emptyList())
                    }
                }
                if (!adapter.getProfileProxy(context, listener, profile) && cont.isActive) cont.resume(emptyList())
            }
        } ?: emptyList()
}
