package com.example.model

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class PermissionCategory(val title: String, val icon: String) {
    HARDWARE("Camera & Hardware", "camera"),
    AUDIO("Audio & Microphone", "mic"),
    LOCATION("Location & GPS", "location"),
    NOTIFICATIONS("Notifications", "notifications"),
    SENSORS("Sensors & Motion", "sensor"),
    CONTACTS_CALENDAR("Contacts & Calendar", "contacts"),
    CONNECTIVITY("Network & Bluetooth", "network"),
    STORAGE("Media & Storage", "storage")
}

data class PermissionItem(
    val id: String,
    val permissionManifestKey: String,
    val name: String,
    val description: String,
    val category: PermissionCategory,
    val isDangerous: Boolean = true,
    val minSdk: Int = 0,
    val maxSdk: Int = Int.MAX_VALUE
) {
    fun isSupportedOnCurrentDevice(): Boolean {
        return Build.VERSION.SDK_INT >= minSdk && Build.VERSION.SDK_INT <= maxSdk
    }

    fun isGranted(context: Context): Boolean {
        if (!isSupportedOnCurrentDevice()) return true
        // Normal permissions like VIBRATE, INTERNET are automatically granted if in manifest
        if (!isDangerous) return true
        return ContextCompat.checkSelfPermission(context, permissionManifestKey) == PackageManager.PERMISSION_GRANTED
    }
}

object PermissionRegistry {
    val ALL_PERMISSIONS = listOf(
        // Camera & Torch
        PermissionItem(
            id = "camera",
            permissionManifestKey = Manifest.permission.CAMERA,
            name = "Camera Access",
            description = "Access camera feed, photo capture, and QR scanner in scripts",
            category = PermissionCategory.HARDWARE
        ),
        PermissionItem(
            id = "flashlight",
            permissionManifestKey = "android.permission.FLASHLIGHT",
            name = "Flashlight / Torch",
            description = "Turn device LED flashlight torch on and off programmatically",
            category = PermissionCategory.HARDWARE,
            isDangerous = false
        ),
        PermissionItem(
            id = "vibrate",
            permissionManifestKey = Manifest.permission.VIBRATE,
            name = "Vibrator / Haptics",
            description = "Custom haptic pulses, buzzers, and vibration patterns",
            category = PermissionCategory.HARDWARE,
            isDangerous = false
        ),

        // Audio
        PermissionItem(
            id = "mic",
            permissionManifestKey = Manifest.permission.RECORD_AUDIO,
            name = "Record Audio / Microphone",
            description = "Microphone access for live decibel meter, sound visualizer, audio recorder",
            category = PermissionCategory.AUDIO
        ),
        PermissionItem(
            id = "audio_settings",
            permissionManifestKey = Manifest.permission.MODIFY_AUDIO_SETTINGS,
            name = "Modify Audio Settings",
            description = "Adjust audio routing, speakerphone, and volume levels",
            category = PermissionCategory.AUDIO,
            isDangerous = false
        ),

        // Location
        PermissionItem(
            id = "fine_location",
            permissionManifestKey = Manifest.permission.ACCESS_FINE_LOCATION,
            name = "Precise GPS Location",
            description = "High accuracy GPS latitude, longitude, altitude, speed, and heading",
            category = PermissionCategory.LOCATION
        ),
        PermissionItem(
            id = "coarse_location",
            permissionManifestKey = Manifest.permission.ACCESS_COARSE_LOCATION,
            name = "Approximate Location",
            description = "Network and cell tower based location coordinates",
            category = PermissionCategory.LOCATION
        ),

        // Notifications
        PermissionItem(
            id = "post_notifications",
            permissionManifestKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.POST_NOTIFICATIONS
            } else {
                "android.permission.POST_NOTIFICATIONS"
            },
            name = "Post Notifications",
            description = "Send system status bar notifications and heads-up alerts from code",
            category = PermissionCategory.NOTIFICATIONS,
            minSdk = 33
        ),

        // Sensors & Motion
        PermissionItem(
            id = "body_sensors",
            permissionManifestKey = Manifest.permission.BODY_SENSORS,
            name = "Body Sensors",
            description = "Access biometric and body sensors if available on device",
            category = PermissionCategory.SENSORS
        ),
        PermissionItem(
            id = "activity_recognition",
            permissionManifestKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Manifest.permission.ACTIVITY_RECOGNITION
            } else {
                "android.permission.ACTIVITY_RECOGNITION"
            },
            name = "Activity Recognition",
            description = "Pedometer step detection and physical movement detection",
            category = PermissionCategory.SENSORS,
            minSdk = 29
        ),

        // Contacts & Calendar
        PermissionItem(
            id = "read_contacts",
            permissionManifestKey = Manifest.permission.READ_CONTACTS,
            name = "Read Contacts",
            description = "Inspect phonebook contacts and telephone numbers directly in code",
            category = PermissionCategory.CONTACTS_CALENDAR
        ),
        PermissionItem(
            id = "read_calendar",
            permissionManifestKey = Manifest.permission.READ_CALENDAR,
            name = "Read Calendar",
            description = "Query calendar events, meetings, and schedules",
            category = PermissionCategory.CONTACTS_CALENDAR
        ),

        // Connectivity & Bluetooth
        PermissionItem(
            id = "bluetooth_connect",
            permissionManifestKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Manifest.permission.BLUETOOTH_CONNECT
            } else {
                Manifest.permission.BLUETOOTH
            },
            name = "Bluetooth Connect",
            description = "Query paired Bluetooth devices and check wireless connectivity",
            category = PermissionCategory.CONNECTIVITY,
            minSdk = 31
        ),
        PermissionItem(
            id = "bluetooth_scan",
            permissionManifestKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Manifest.permission.BLUETOOTH_SCAN
            } else {
                Manifest.permission.BLUETOOTH_ADMIN
            },
            name = "Bluetooth Scan",
            description = "Discover nearby BLE beacons and Bluetooth peripherals",
            category = PermissionCategory.CONNECTIVITY,
            minSdk = 31
        ),
        PermissionItem(
            id = "internet",
            permissionManifestKey = Manifest.permission.INTERNET,
            name = "Internet Access",
            description = "Fetch REST APIs, WebSockets, external assets, and cloud endpoints",
            category = PermissionCategory.CONNECTIVITY,
            isDangerous = false
        ),
        PermissionItem(
            id = "network_state",
            permissionManifestKey = Manifest.permission.ACCESS_NETWORK_STATE,
            name = "Network State",
            description = "Check WiFi vs Cellular connectivity and link speeds",
            category = PermissionCategory.CONNECTIVITY,
            isDangerous = false
        ),

        // Media / Storage
        PermissionItem(
            id = "read_media_images",
            permissionManifestKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            },
            name = "Media Images",
            description = "Access user gallery images and photo assets for canvas manipulation",
            category = PermissionCategory.STORAGE
        ),
        PermissionItem(
            id = "read_media_audio",
            permissionManifestKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            },
            name = "Media Audio",
            description = "Read local audio tracks and sound effects",
            category = PermissionCategory.STORAGE
        )
    )

    fun getRuntimePermissionsToRequest(): List<String> {
        return ALL_PERMISSIONS
            .filter { it.isDangerous && it.isSupportedOnCurrentDevice() }
            .map { it.permissionManifestKey }
            .distinct()
    }
}
