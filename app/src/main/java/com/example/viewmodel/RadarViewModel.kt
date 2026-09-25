package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.RadarApplication
import com.example.data.DeviceAliasEntity
import com.example.model.DeviceType
import com.example.model.NearbyDevice
import com.example.scanner.CompassSensor
import com.example.scanner.RfScannerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DeviceFilter(val label: String) {
    ALL("All Devices"),
    BLUETOOTH("Bluetooth"),
    WIFI("Wi-Fi"),
    NAMED_ONLY("Named Only")
}

data class RadarUiState(
    val devices: List<NearbyDevice> = emptyList(),
    val allDetectedCount: Int = 0,
    val bluetoothCount: Int = 0,
    val wifiCount: Int = 0,
    val namedCount: Int = 0,
    val azimuthDegrees: Float = 0f,
    val isScanning: Boolean = false,
    val hardwareStatus: String = "Ready",
    val filter: DeviceFilter = DeviceFilter.ALL,
    val searchQuery: String = "",
    val rssiThreshold: Int = -95,
    val maxRangeMeters: Float = 25f,
    val isCompassEnabled: Boolean = true,
    val selectedDevice: NearbyDevice? = null,
    val isRenamingDevice: NearbyDevice? = null,
    val showDisclaimerDialog: Boolean = false,
    val showAllAliasesDialog: Boolean = false,
    val isDisclaimerBannerVisible: Boolean = true,
    val allSavedAliases: List<DeviceAliasEntity> = emptyList()
)

class RadarViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RadarApplication
    private val aliasRepository = app.aliasRepository

    private val compassSensor = CompassSensor(application)
    private val scannerManager = RfScannerManager(application, viewModelScope)

    private val _filter = MutableStateFlow(DeviceFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _rssiThreshold = MutableStateFlow(-95)
    private val _maxRangeMeters = MutableStateFlow(25f)
    private val _isCompassEnabled = MutableStateFlow(true)
    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    private val _renamingDeviceId = MutableStateFlow<String?>(null)
    private val _showDisclaimerDialog = MutableStateFlow(false)
    private val _showAllAliasesDialog = MutableStateFlow(false)
    private val _isDisclaimerBannerVisible = MutableStateFlow(true)

    val uiState: StateFlow<RadarUiState> = combine(
        scannerManager.rawDevices,
        aliasRepository.allAliases,
        compassSensor.azimuthDegrees,
        scannerManager.isScanning,
        scannerManager.hardwareStatus,
        _filter,
        _searchQuery,
        _rssiThreshold,
        _maxRangeMeters,
        _isCompassEnabled,
        _selectedDeviceId,
        _renamingDeviceId,
        _showDisclaimerDialog,
        _showAllAliasesDialog,
        _isDisclaimerBannerVisible
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val rawDevicesMap = args[0] as Map<String, NearbyDevice>
        @Suppress("UNCHECKED_CAST")
        val savedAliases = args[1] as List<DeviceAliasEntity>
        val azimuth = args[2] as Float
        val isScanning = args[3] as Boolean
        val hwStatus = args[4] as String
        val filter = args[5] as DeviceFilter
        val query = args[6] as String
        val rssiThresh = args[7] as Int
        val maxRange = args[8] as Float
        val compassOn = args[9] as Boolean
        val selectedId = args[10] as String?
        val renamingId = args[11] as String?
        val showDisclaimer = args[12] as Boolean
        val showAliases = args[13] as Boolean
        val bannerVisible = args[14] as Boolean

        val aliasMap = savedAliases.associateBy { it.deviceAddress }

        // Merge raw devices with aliases
        val mergedDevices = rawDevicesMap.values.map { raw ->
            val alias = aliasMap[raw.id]
            raw.copy(
                customAlias = alias?.customName,
                notes = alias?.notes,
                tagColorHex = alias?.tagColorHex,
                isPinned = alias?.isPinned ?: false
            )
        }

        val btCount = mergedDevices.count { it.deviceType.isBluetooth }
        val wifiCount = mergedDevices.count { it.deviceType.isWifi }
        val namedCount = mergedDevices.count { it.hasCustomName }

        // Filter and sort
        val filtered = mergedDevices
            .filter { device ->
                // Filter by category
                when (filter) {
                    DeviceFilter.ALL -> true
                    DeviceFilter.BLUETOOTH -> device.deviceType.isBluetooth
                    DeviceFilter.WIFI -> device.deviceType.isWifi
                    DeviceFilter.NAMED_ONLY -> device.hasCustomName
                }
            }
            .filter { device ->
                // Filter by RSSI threshold
                device.rssi >= rssiThresh
            }
            .filter { device ->
                // Filter by search query
                if (query.isBlank()) true
                else {
                    device.displayName.contains(query, ignoreCase = true) ||
                    device.id.contains(query, ignoreCase = true) ||
                    (device.notes?.contains(query, ignoreCase = true) == true)
                }
            }
            .sortedWith(
                compareByDescending<NearbyDevice> { it.isPinned }
                    .thenByDescending { it.hasCustomName }
                    .thenByDescending { it.rssi }
            )

        val selectedDev = if (selectedId != null) {
            mergedDevices.find { it.id == selectedId }
        } else null

        val renamingDev = if (renamingId != null) {
            mergedDevices.find { it.id == renamingId }
        } else null

        RadarUiState(
            devices = filtered,
            allDetectedCount = mergedDevices.size,
            bluetoothCount = btCount,
            wifiCount = wifiCount,
            namedCount = namedCount,
            azimuthDegrees = azimuth,
            isScanning = isScanning,
            hardwareStatus = hwStatus,
            filter = filter,
            searchQuery = query,
            rssiThreshold = rssiThresh,
            maxRangeMeters = maxRange,
            isCompassEnabled = compassOn,
            selectedDevice = selectedDev,
            isRenamingDevice = renamingDev,
            showDisclaimerDialog = showDisclaimer,
            showAllAliasesDialog = showAliases,
            isDisclaimerBannerVisible = bannerVisible,
            allSavedAliases = savedAliases
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RadarUiState()
    )

    init {
        compassSensor.startListening()
        // Auto-start scanning on launch
        scannerManager.startScan()
    }

    override fun onCleared() {
        super.onCleared()
        compassSensor.stopListening()
        scannerManager.stopScan()
    }

    fun toggleScan() {
        if (uiState.value.isScanning) {
            scannerManager.stopScan()
        } else {
            scannerManager.startScan()
        }
    }

    fun setFilter(filter: DeviceFilter) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setRssiThreshold(threshold: Int) {
        _rssiThreshold.value = threshold
    }

    fun setMaxRangeMeters(range: Float) {
        _maxRangeMeters.value = range
    }

    fun toggleCompass() {
        _isCompassEnabled.value = !_isCompassEnabled.value
    }

    fun selectDevice(device: NearbyDevice?) {
        _selectedDeviceId.value = device?.id
    }

    fun openRenameDialog(device: NearbyDevice?) {
        _renamingDeviceId.value = device?.id
    }

    fun closeRenameDialog() {
        _renamingDeviceId.value = null
    }

    fun saveDeviceAlias(address: String, customName: String, notes: String = "", tagColorHex: String = "") {
        viewModelScope.launch {
            if (customName.isBlank()) {
                aliasRepository.deleteAlias(address)
            } else {
                aliasRepository.saveAlias(
                    address = address,
                    customName = customName,
                    notes = notes,
                    tagColorHex = tagColorHex
                )
            }
            _renamingDeviceId.value = null
        }
    }

    fun deleteDeviceAlias(address: String) {
        viewModelScope.launch {
            aliasRepository.deleteAlias(address)
        }
    }

    fun setDisclaimerDialogVisible(show: Boolean) {
        _showDisclaimerDialog.value = show
    }

    fun setAllAliasesDialogVisible(show: Boolean) {
        _showAllAliasesDialog.value = show
    }

    fun dismissDisclaimerBanner() {
        _isDisclaimerBannerVisible.value = false
    }

    fun refreshWifiScan() {
        scannerManager.triggerSingleWifiRefresh()
    }
}
