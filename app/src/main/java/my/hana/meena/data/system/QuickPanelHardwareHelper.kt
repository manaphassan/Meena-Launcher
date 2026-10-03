package my.hana.meena.data.system

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QuickPanelWifiState(
    val isConnected: Boolean = false,
    val label: String = "offline"
)

data class QuickPanelBluetoothState(
    val isEnabled: Boolean = false,
    val label: String = "off"
)

/**
 * Manages real-time Android hardware state detection and quick actions for the Action Center:
 * - Wi-Fi connection monitoring & Quick Settings Panel intent
 * - Bluetooth state observation & Bluetooth settings intent
 * - Real hardware camera torch (LED flashlight) toggle & state callback
 */
object QuickPanelHardwareHelper {

    private val _wifiState = MutableStateFlow(QuickPanelWifiState())
    val wifiState: StateFlow<QuickPanelWifiState> = _wifiState.asStateFlow()

    private val _bluetoothState = MutableStateFlow(QuickPanelBluetoothState())
    val bluetoothState: StateFlow<QuickPanelBluetoothState> = _bluetoothState.asStateFlow()

    private val _isTorchActive = MutableStateFlow(false)
    val isTorchActive: StateFlow<Boolean> = _isTorchActive.asStateFlow()

    private var isInitialized = false
    private var flashCameraId: String? = null
    private var cameraManager: CameraManager? = null

    fun initialize(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        isInitialized = true

        // 1. Live Wi-Fi Connectivity Monitoring
        setupWifiMonitoring(appContext)

        // 2. Live Bluetooth State Monitoring
        setupBluetoothMonitoring(appContext)

        // 3. Live Hardware Flashlight / Torch Monitoring
        setupTorchMonitoring(appContext)
    }

    private fun setupWifiMonitoring(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

        // Initial snapshot
        updateWifiState(cm)

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        try {
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _wifiState.value = QuickPanelWifiState(isConnected = true, label = "connected")
                }

                override fun onLost(network: Network) {
                    _wifiState.value = QuickPanelWifiState(isConnected = false, label = "offline")
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    _wifiState.value = QuickPanelWifiState(
                        isConnected = hasWifi,
                        label = if (hasWifi && isValidated) "connected" else if (hasWifi) "ready" else "offline"
                    )
                }
            })
        } catch (e: Exception) {
            updateWifiState(cm)
        }
    }

    private fun updateWifiState(cm: ConnectivityManager) {
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        _wifiState.value = QuickPanelWifiState(
            isConnected = isWifi,
            label = if (isWifi) "connected" else "offline"
        )
    }

    fun openWifiPanel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {}
        }
    }

    private fun setupBluetoothMonitoring(context: Context) {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bm?.adapter

        // Initial check
        val initialEnabled = try {
            adapter?.isEnabled == true
        } catch (e: SecurityException) {
            false
        }
        _bluetoothState.value = QuickPanelBluetoothState(
            isEnabled = initialEnabled,
            label = if (initialEnabled) "ready" else "off"
        )

        // Broadcast receiver for Bluetooth on/off transitions
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        try {
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                        when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                            BluetoothAdapter.STATE_ON -> {
                                _bluetoothState.value = QuickPanelBluetoothState(isEnabled = true, label = "ready")
                            }
                            BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                                _bluetoothState.value = QuickPanelBluetoothState(isEnabled = false, label = "off")
                            }
                        }
                    }
                }
            }, filter)
        } catch (_: Exception) {}
    }

    fun openBluetoothPanel(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle gracefully
        }
    }

    private fun setupTorchMonitoring(context: Context) {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        cameraManager = cm

        try {
            val list = cm.cameraIdList
            flashCameraId = list.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                        chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: list.firstOrNull()

            cm.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId == flashCameraId) {
                        _isTorchActive.value = enabled
                    }
                }

                override fun onTorchModeUnavailable(cameraId: String) {
                    if (cameraId == flashCameraId) {
                        _isTorchActive.value = false
                    }
                }
            }, Handler(Looper.getMainLooper()))
        } catch (_: Exception) {}
    }

    fun toggleTorch(context: Context) {
        val cm = cameraManager ?: (context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager)
        val id = flashCameraId ?: return
        try {
            val newState = !_isTorchActive.value
            cm?.setTorchMode(id, newState)
            _isTorchActive.value = newState
        } catch (e: Exception) {
            // Torch in use by another app or device flash unavailable
        }
    }
}
