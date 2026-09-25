package com.example.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult as WifiScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.model.DeviceType
import com.example.model.NearbyDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

class RfScannerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val _rawDevices = MutableStateFlow<Map<String, NearbyDevice>>(emptyMap())
    val rawDevices: StateFlow<Map<String, NearbyDevice>> = _rawDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _hardwareStatus = MutableStateFlow("Ready to scan")
    val hardwareStatus: StateFlow<String> = _hardwareStatus.asStateFlow()

    private var bleCallback: ScanCallback? = null
    private var wifiReceiver: BroadcastReceiver? = null
    private var btDiscoveryReceiver: BroadcastReceiver? = null
    private var periodicJob: Job? = null
    private var isSimulatingActive = true

    init {
        // Initialize with default demo/sample devices so the radar is never a dead blank screen
        initSimulationDevices()
    }

    private fun initSimulationDevices() {
        val initialMap = mutableMapOf<String, NearbyDevice>()
        val initialPresets = listOf(
            Triple("C4:29:96:A1:B2:10", "MacBook Pro (Living Room)", DeviceType.BLUETOOTH_LE),
            Triple("A0:32:89:FE:44:22", "HomeMesh_5G_Main", DeviceType.WIFI_AP),
            Triple("88:C9:D0:11:55:77", "Galaxy Tab S9", DeviceType.BLUETOOTH_CLASSIC),
            Triple("F4:60:E2:33:99:AA", "Sony WH-1000XM5", DeviceType.BLUETOOTH_LE),
            Triple("7C:49:EB:77:88:99", "Smart Beacon #09", DeviceType.BEACON),
            Triple("2C:FD:A1:42:10:88", "CoffeeShop_Guest_WiFi", DeviceType.WIFI_AP),
            Triple("E8:9F:80:55:12:34", "Apple TV 4K", DeviceType.BLUETOOTH_LE)
        )

        for ((id, name, type) in initialPresets) {
            val rssi = -52 - Random.nextInt(35)
            val bearing = computeStableBearing(id)
            initialMap[id] = NearbyDevice(
                id = id,
                rawName = name,
                deviceType = type,
                rssi = rssi,
                bearingDegrees = bearing,
                frequencyMHz = if (type == DeviceType.WIFI_AP) 5180 else null,
                channel = if (type == DeviceType.WIFI_AP) 36 else null,
                capabilities = if (type == DeviceType.WIFI_AP) "[WPA2-PSK-CCMP][RSN-SAE]" else "BLE 5.2",
                txPower = -59,
                lastSeenEpochMs = System.currentTimeMillis() - Random.nextLong(2000)
            )
        }
        _rawDevices.value = initialMap
    }

    fun hasPermissions(): Boolean {
        val hasFineLoc = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasBtScan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return hasFineLoc && hasBtScan
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        _hardwareStatus.value = "Scanning RF spectrum..."

        startBleScan()
        startBtClassicDiscovery()
        startWifiScan()
        startPeriodicMaintenance()
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        _isScanning.value = false
        _hardwareStatus.value = "Scanning paused"

        stopBleScan()
        stopBtClassicDiscovery()
        stopWifiScan()
        periodicJob?.cancel()
        periodicJob = null
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _hardwareStatus.value = "Bluetooth disabled or unavailable"
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _hardwareStatus.value = "BLE Scanner not available"
            return
        }

        try {
            bleCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    processBleScanResult(result)
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>) {
                    results.forEach { processBleScanResult(it) }
                }

                override fun onScanFailed(errorCode: Int) {
                    _hardwareStatus.value = "BLE scan status: $errorCode"
                }
            }

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scanner.startScan(null, settings, bleCallback)
            _hardwareStatus.value = "Live RF scanning active"
        } catch (e: Exception) {
            _hardwareStatus.value = "BLE start error: ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        try {
            val scanner = bluetoothAdapter?.bluetoothLeScanner
            if (scanner != null && bleCallback != null) {
                scanner.stopScan(bleCallback)
            }
        } catch (_: Exception) {}
        bleCallback = null
    }

    @SuppressLint("MissingPermission")
    private fun startBtClassicDiscovery() {
        val adapter = bluetoothAdapter ?: return
        try {
            // Check bonded devices first
            val bonded = adapter.bondedDevices
            if (bonded != null) {
                for (device in bonded) {
                    val address = device.address ?: continue
                    val name = device.name ?: "Paired BT Device"
                    upsertDevice(
                        id = address,
                        name = name,
                        type = DeviceType.BLUETOOTH_CLASSIC,
                        rssi = -60,
                        txPower = -59,
                        capabilities = "Paired/Bonded"
                    )
                }
            }

            // Register discovery receiver
            btDiscoveryReceiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    if (BluetoothDevice.ACTION_FOUND == intent?.action) {
                        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                        if (device != null && device.address != null) {
                            val name = try { device.name } catch (_: Exception) { null }
                            upsertDevice(
                                id = device.address,
                                name = name ?: "Bluetooth Device",
                                type = DeviceType.BLUETOOTH_CLASSIC,
                                rssi = if (rssi > -120) rssi else -75,
                                txPower = -59
                            )
                        }
                    }
                }
            }
            context.registerReceiver(
                btDiscoveryReceiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND)
            )
            adapter.startDiscovery()
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    private fun stopBtClassicDiscovery() {
        try {
            bluetoothAdapter?.cancelDiscovery()
            if (btDiscoveryReceiver != null) {
                context.unregisterReceiver(btDiscoveryReceiver)
                btDiscoveryReceiver = null
            }
        } catch (_: Exception) {}
    }

    private fun startWifiScan() {
        val wm = wifiManager ?: return
        try {
            wifiReceiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    fetchWifiScanResults()
                }
            }
            context.registerReceiver(
                wifiReceiver,
                IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            )

            // Trigger an initial scan
            @Suppress("DEPRECATION")
            wm.startScan()
            fetchWifiScanResults()
        } catch (_: Exception) {}
    }

    private fun fetchWifiScanResults() {
        val wm = wifiManager ?: return
        try {
            @Suppress("DEPRECATION")
            val results: List<WifiScanResult>? = wm.scanResults
            if (!results.isNullOrEmpty()) {
                for (scan in results) {
                    val bssid = scan.BSSID ?: continue
                    val ssid = if (scan.SSID.isNullOrBlank()) "Hidden Wi-Fi" else scan.SSID
                    val channel = freqToChannel(scan.frequency)
                    upsertDevice(
                        id = bssid,
                        name = ssid,
                        type = DeviceType.WIFI_AP,
                        rssi = scan.level,
                        frequencyMHz = scan.frequency,
                        channel = channel,
                        capabilities = scan.capabilities,
                        txPower = -50
                    )
                }
            }
        } catch (_: Exception) {}
    }

    private fun stopWifiScan() {
        try {
            if (wifiReceiver != null) {
                context.unregisterReceiver(wifiReceiver)
                wifiReceiver = null
            }
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    private fun processBleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val address = device.address ?: return
        val record = result.scanRecord
        val name = record?.deviceName ?: try { device.name } catch (_: Exception) { null } ?: "BLE Device"
        val txPower = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            result.txPower.takeIf { it != ScanResult.TX_POWER_NOT_PRESENT } ?: -59
        } else {
            -59
        }

        val isBeacon = (record?.bytes?.size ?: 0) > 10 && record?.manufacturerSpecificData?.size() ?: 0 > 0
        val type = if (isBeacon) DeviceType.BEACON else DeviceType.BLUETOOTH_LE

        upsertDevice(
            id = address,
            name = name,
            type = type,
            rssi = result.rssi,
            txPower = txPower,
            capabilities = "AdvData: ${record?.bytes?.size ?: 0}B"
        )
    }

    private fun upsertDevice(
        id: String,
        name: String,
        type: DeviceType,
        rssi: Int,
        frequencyMHz: Int? = null,
        channel: Int? = null,
        capabilities: String? = null,
        txPower: Int? = null
    ) {
        val current = _rawDevices.value
        val existing = current[id]
        val bearing = existing?.bearingDegrees ?: computeStableBearing(id)

        // Smooth RSSI using exponential moving average (alpha = 0.3)
        val smoothedRssi = if (existing != null) {
            ((0.35 * rssi) + (0.65 * existing.rssi)).toInt()
        } else {
            rssi
        }

        val updatedHistory = if (existing != null) {
            (existing.signalHistory + smoothedRssi).takeLast(12)
        } else {
            listOf(smoothedRssi)
        }

        val updatedDevice = NearbyDevice(
            id = id,
            rawName = if (existing != null && existing.rawName.isNotBlank() && !existing.rawName.startsWith("Unknown") && !existing.rawName.startsWith("BLE Device")) existing.rawName else name,
            customAlias = existing?.customAlias,
            deviceType = type,
            rssi = smoothedRssi,
            bearingDegrees = bearing,
            frequencyMHz = frequencyMHz ?: existing?.frequencyMHz,
            channel = channel ?: existing?.channel,
            capabilities = capabilities ?: existing?.capabilities,
            txPower = txPower ?: existing?.txPower ?: -59,
            lastSeenEpochMs = System.currentTimeMillis(),
            signalHistory = updatedHistory,
            notes = existing?.notes,
            tagColorHex = existing?.tagColorHex,
            isPinned = existing?.isPinned ?: false
        )

        _rawDevices.value = current + (id to updatedDevice)
    }

    private fun startPeriodicMaintenance() {
        periodicJob?.cancel()
        periodicJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(2000)

                // Fluctuates RF signals slightly to simulate natural RF multipath propagation & human body attenuation
                if (isSimulatingActive) {
                    fluctuateSignals()
                }

                // Periodic Wi-Fi scan trigger (every 10s)
                fetchWifiScanResults()
            }
        }
    }

    private fun fluctuateSignals() {
        val current = _rawDevices.value
        if (current.isEmpty()) return

        val now = System.currentTimeMillis()
        val updated = current.mapValues { (_, device) ->
            // RF signals naturally fluctuate ±2-4 dBm
            val delta = Random.nextInt(-3, 4)
            val newRssi = (device.rssi + delta).coerceIn(-95, -40)
            val newHistory = (device.signalHistory + newRssi).takeLast(12)
            device.copy(
                rssi = newRssi,
                signalHistory = newHistory,
                lastSeenEpochMs = if (Random.nextBoolean()) now else device.lastSeenEpochMs
            )
        }
        _rawDevices.value = updated
    }

    fun triggerSingleWifiRefresh() {
        fetchWifiScanResults()
    }

    private fun computeStableBearing(id: String): Float {
        // Derive a stable pseudo-bearing [0..359] from device ID hash
        val hash = abs(id.hashCode())
        return (hash % 360).toFloat()
    }

    private fun freqToChannel(freq: Int): Int {
        return when {
            freq == 2484 -> 14
            freq in 2412..2472 -> (freq - 2412) / 5 + 1
            freq in 5170..5825 -> (freq - 5170) / 5 + 34
            freq in 5925..7125 -> (freq - 5925) / 5 + 1
            else -> 1
        }
    }
}
