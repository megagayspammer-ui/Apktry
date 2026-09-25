package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

enum class DeviceType(val label: String, val category: String) {
    BLUETOOTH_LE("Bluetooth LE", "Bluetooth"),
    BLUETOOTH_CLASSIC("Bluetooth Classic", "Bluetooth"),
    WIFI_AP("Wi-Fi Access Point", "Wi-Fi"),
    BEACON("BLE Beacon", "Bluetooth");

    val isBluetooth: Boolean get() = this == BLUETOOTH_LE || this == BLUETOOTH_CLASSIC || this == BEACON
    val isWifi: Boolean get() = this == WIFI_AP
}

data class NearbyDevice(
    val id: String,
    val rawName: String,
    val customAlias: String? = null,
    val deviceType: DeviceType,
    val rssi: Int,
    val bearingDegrees: Float,
    val frequencyMHz: Int? = null,
    val channel: Int? = null,
    val capabilities: String? = null,
    val txPower: Int? = null,
    val lastSeenEpochMs: Long = System.currentTimeMillis(),
    val signalHistory: List<Int> = listOf(rssi),
    val notes: String? = null,
    val tagColorHex: String? = null,
    val isPinned: Boolean = false
) {
    val displayName: String
        get() {
            if (!customAlias.isNullOrBlank()) return customAlias
            if (rawName.isNotBlank() && !rawName.equals("Unknown Device", ignoreCase = true) && !rawName.equals("<unknown ssid>", ignoreCase = true)) {
                return rawName
            }
            val shortId = if (id.length > 5) id.takeLast(5).uppercase() else id.uppercase()
            return when (deviceType) {
                DeviceType.BLUETOOTH_LE -> "BLE Device ($shortId)"
                DeviceType.BLUETOOTH_CLASSIC -> "BT Device ($shortId)"
                DeviceType.WIFI_AP -> "Wi-Fi Network ($shortId)"
                DeviceType.BEACON -> "Beacon ($shortId)"
            }
        }

    val hasCustomName: Boolean
        get() = !customAlias.isNullOrBlank()

    /**
     * Estimated distance in meters using the standard Log-Distance Path Loss Model:
     * Distance = 10 ^ ((MeasuredPower - RSSI) / (10 * N))
     * N = path loss exponent (2.0 in free space, 2.5 - 3.2 in typical indoor RF environments).
     * MeasuredPower (RSSI at 1 meter): typical -59 to -65 dBm for BLE/Wi-Fi.
     */
    val estimatedDistanceMeters: Double
        get() {
            val refPower = txPower ?: -59
            val pathLossExponent = 2.4
            val exponent = (refPower - rssi).toDouble() / (10.0 * pathLossExponent)
            val raw = 10.0.pow(exponent)
            // Clamp to practical realistic range [0.3m, 60.0m]
            val clamped = raw.coerceIn(0.3, 60.0)
            return (clamped * 10).roundToInt() / 10.0
        }

    val estimatedDistanceFeet: Double
        get() = ((estimatedDistanceMeters * 3.28084) * 10).roundToInt() / 10.0

    /**
     * Proximity category based on RSSI strength.
     */
    val proximityLevel: ProximityLevel
        get() = when {
            rssi >= -55 -> ProximityLevel.IMMEDIATE // < 1.5m
            rssi >= -70 -> ProximityLevel.NEAR      // 1.5m - 5m
            rssi >= -85 -> ProximityLevel.MID       // 5m - 15m
            else -> ProximityLevel.FAR              // > 15m
        }

    val signalQualityPercent: Int
        get() {
            // Map -100 dBm (0%) to -40 dBm (100%)
            return ((rssi + 100).toDouble() / 60.0 * 100).roundToInt().coerceIn(0, 100)
        }

    val formattedLastSeen: String
        get() {
            val secondsAgo = (System.currentTimeMillis() - lastSeenEpochMs) / 1000
            return when {
                secondsAgo < 3 -> "Just now"
                secondsAgo < 60 -> "${secondsAgo}s ago"
                secondsAgo < 3600 -> "${secondsAgo / 60}m ago"
                else -> SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastSeenEpochMs))
            }
        }
}

enum class ProximityLevel(val label: String, val colorHex: Long) {
    IMMEDIATE("Immediate (< 1.5m)", 0xFF00F5D4),
    NEAR("Near (1.5 - 5m)", 0xFF38B000),
    MID("Mid-Range (5 - 15m)", 0xFFFFB703),
    FAR("Far (> 15m)", 0xFFFF5400)
}
