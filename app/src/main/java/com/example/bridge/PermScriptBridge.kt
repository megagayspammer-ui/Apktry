package com.example.bridge

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CombinedVibration
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.model.LogLevel
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.Locale

class PermScriptBridge(
    private val context: Context,
    private val webViewRef: WeakReference<WebView>,
    private val onLogReceived: (LogLevel, String) -> Unit,
    private val onRequestPermissionFromJs: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var textToSpeech: TextToSpeech? = TextToSpeech(context, this)
    private var isTtsReady = false

    private val sensorHelper = SensorHelper(context) { type, x, y, z ->
        postToWebView("if (window.onSensorData) { window.onSensorData('$type', $x, $y, $z); }")
    }

    private val audioMeterHelper = AudioMeterHelper(context) { db, amp ->
        postToWebView("if (window.onSoundMeter) { window.onSoundMeter($db, $amp); }")
    }

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            postToWebView("if (window.onLocationResult) { window.onLocationResult(${loc.latitude}, ${loc.longitude}, ${loc.accuracy}, ${loc.altitude}, ${loc.speed}); }")
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    init {
        createNotificationChannel()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
            isTtsReady = true
        }
    }

    private fun postToWebView(js: String) {
        mainHandler.post {
            val webView = webViewRef.get() ?: return@post
            webView.evaluateJavascript(js, null)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PermScript Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications dispatched by in-app scripts"
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    // ================== JAVASCRIPT API ==================

    @JavascriptInterface
    fun toast(message: String) {
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        onLogReceived(LogLevel.INFO, "Toast: $message")
    }

    @JavascriptInterface
    fun log(message: String) {
        onLogReceived(LogLevel.INFO, message)
    }

    @JavascriptInterface
    fun warn(message: String) {
        onLogReceived(LogLevel.WARN, message)
    }

    @JavascriptInterface
    fun error(message: String) {
        onLogReceived(LogLevel.ERROR, message)
    }

    @JavascriptInterface
    fun vibrate(durationMs: Long) {
        val safeMs = durationMs.coerceIn(10L, 5000L)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.vibrate(CombinedVibration.createParallel(VibrationEffect.createOneShot(safeMs, VibrationEffect.DEFAULT_AMPLITUDE)))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(safeMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(safeMs)
                }
            }
            onLogReceived(LogLevel.INFO, "Vibrated for ${safeMs}ms")
        } catch (e: Exception) {
            onLogReceived(LogLevel.ERROR, "Vibration failed: ${e.message}")
        }
    }

    @JavascriptInterface
    fun vibratePattern(patternJson: String) {
        try {
            val array = JSONArray(patternJson)
            val timings = LongArray(array.length())
            for (i in 0 until array.length()) {
                timings[i] = array.getLong(i)
            }
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(timings, -1)
            }
            onLogReceived(LogLevel.INFO, "Vibrated custom pattern")
        } catch (e: Exception) {
            onLogReceived(LogLevel.ERROR, "Invalid pattern: ${e.message}")
        }
    }

    @JavascriptInterface
    fun notify(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                onLogReceived(LogLevel.WARN, "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val notificationId = (System.currentTimeMillis() % 100000).toInt()
            nm?.notify(notificationId, builder.build())
            onLogReceived(LogLevel.INFO, "Dispatched notification: '$title'")
        } catch (e: Exception) {
            onLogReceived(LogLevel.ERROR, "Notification dispatch failed: ${e.message}")
        }
    }

    @JavascriptInterface
    fun speak(text: String) {
        if (isTtsReady && textToSpeech != null) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PermScriptTTS")
            onLogReceived(LogLevel.INFO, "TTS speaking: '$text'")
        } else {
            onLogReceived(LogLevel.WARN, "TTS engine not ready")
        }
    }

    @JavascriptInterface
    fun stopSpeaking() {
        textToSpeech?.stop()
    }

    @JavascriptInterface
    fun toggleFlashlight(enable: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, enable)
            onLogReceived(LogLevel.INFO, "Flashlight set to $enable")
        } catch (e: Exception) {
            onLogReceived(LogLevel.ERROR, "Flashlight error: ${e.message}")
        }
    }

    @JavascriptInterface
    fun getBatteryInfo(): String {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val temp = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0

        val json = JSONObject().apply {
            put("level", pct)
            put("isCharging", isCharging)
            put("temperatureCelsius", temp)
        }
        return json.toString()
    }

    @JavascriptInterface
    fun getNetworkInfo(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val net = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(net)

        val isConnected = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false

        val type = when {
            isWifi -> "WiFi"
            isCellular -> "Cellular"
            isConnected -> "Ethernet/Other"
            else -> "Offline"
        }

        val json = JSONObject().apply {
            put("isConnected", isConnected)
            put("type", type)
            put("isWifi", isWifi)
            put("isCellular", isCellular)
        }
        return json.toString()
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        val displayMetrics = context.resources.displayMetrics
        val json = JSONObject().apply {
            put("model", Build.MODEL)
            put("manufacturer", Build.MANUFACTURER)
            put("brand", Build.BRAND)
            put("device", Build.DEVICE)
            put("androidVersion", Build.VERSION.RELEASE)
            put("sdkInt", Build.VERSION.SDK_INT)
            put("screenWidthDp", displayMetrics.widthPixels / displayMetrics.density)
            put("screenHeightDp", displayMetrics.heightPixels / displayMetrics.density)
        }
        return json.toString()
    }

    @JavascriptInterface
    fun getLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            onLogReceived(LogLevel.WARN, "Location permission not granted")
            return
        }

        mainHandler.post {
            try {
                val provider = when {
                    hasFine && locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true -> LocationManager.GPS_PROVIDER
                    hasCoarse && locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true -> LocationManager.NETWORK_PROVIDER
                    else -> LocationManager.PASSIVE_PROVIDER
                }

                // Send last known location immediately if available
                val lastLoc = locationManager?.getLastKnownLocation(provider)
                if (lastLoc != null) {
                    postToWebView("if (window.onLocationResult) { window.onLocationResult(${lastLoc.latitude}, ${lastLoc.longitude}, ${lastLoc.accuracy}, ${lastLoc.altitude}, ${lastLoc.speed}); }")
                }

                locationManager?.requestSingleUpdate(provider, locationListener, Looper.getMainLooper())
                onLogReceived(LogLevel.INFO, "Requesting single location update using $provider")
            } catch (e: Exception) {
                onLogReceived(LogLevel.ERROR, "Location error: ${e.message}")
            }
        }
    }

    @JavascriptInterface
    fun startSensorStream() {
        mainHandler.post {
            sensorHelper.start()
            onLogReceived(LogLevel.INFO, "Sensor stream started")
        }
    }

    @JavascriptInterface
    fun stopSensorStream() {
        mainHandler.post {
            sensorHelper.stop()
            onLogReceived(LogLevel.INFO, "Sensor stream stopped")
        }
    }

    @JavascriptInterface
    fun startSoundMeter() {
        mainHandler.post {
            audioMeterHelper.start()
            onLogReceived(LogLevel.INFO, "Sound meter started")
        }
    }

    @JavascriptInterface
    fun stopSoundMeter() {
        mainHandler.post {
            audioMeterHelper.stop()
            onLogReceived(LogLevel.INFO, "Sound meter stopped")
        }
    }

    @JavascriptInterface
    fun getContacts(limit: Int): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            onLogReceived(LogLevel.WARN, "READ_CONTACTS permission not granted")
            return "[]"
        }

        val list = JSONArray()
        try {
            val cr = context.contentResolver
            val cursor = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                var count = 0
                val safeLimit = limit.coerceIn(1, 100)
                while (it.moveToNext() && count < safeLimit) {
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val phone = if (phoneIdx >= 0) it.getString(phoneIdx) ?: "" else ""
                    val item = JSONObject().apply {
                        put("name", name)
                        put("phone", phone)
                    }
                    list.put(item)
                    count++
                }
            }
            onLogReceived(LogLevel.INFO, "Loaded ${list.length()} contacts")
        } catch (e: Exception) {
            onLogReceived(LogLevel.ERROR, "Failed to load contacts: ${e.message}")
        }
        return list.toString()
    }

    @JavascriptInterface
    fun getCalendarEvents(limit: Int): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            onLogReceived(LogLevel.WARN, "READ_CALENDAR permission not granted")
            return "[]"
        }

        val list = JSONArray()
        try {
            val cr = context.contentResolver
            val cursor = cr.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART, CalendarContract.Events.EVENT_LOCATION),
                null,
                null,
                "${CalendarContract.Events.DTSTART} DESC"
            )

            cursor?.use {
                val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)
                val locIdx = it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                var count = 0
                val safeLimit = limit.coerceIn(1, 100)
                while (it.moveToNext() && count < safeLimit) {
                    val title = if (titleIdx >= 0) it.getString(titleIdx) ?: "Untitled Event" else "Untitled Event"
                    val start = if (startIdx >= 0) it.getLong(startIdx) else 0L
                    val loc = if (locIdx >= 0) it.getString(locIdx) ?: "" else ""
                    val item = JSONObject().apply {
                        put("title", title)
                        put("startTime", start)
                        put("location", loc)
                    }
                    list.put(item)
                    count++
                }
            }
            onLogReceived(LogLevel.INFO, "Loaded ${list.length()} calendar events")
        } catch (e: Exception) {
            onLogReceived(LogLevel.ERROR, "Failed to load calendar events: ${e.message}")
        }
        return list.toString()
    }

    @JavascriptInterface
    fun copyToClipboard(text: String) {
        mainHandler.post {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("PermScript", text)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun getClipboardText(): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            return clip.getItemAt(0).text?.toString() ?: ""
        }
        return ""
    }

    @JavascriptInterface
    fun checkPermission(permissionKey: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permissionKey) == PackageManager.PERMISSION_GRANTED
    }

    @JavascriptInterface
    fun requestPermission(permissionKey: String) {
        mainHandler.post {
            onRequestPermissionFromJs(permissionKey)
        }
    }

    fun onDestroy() {
        sensorHelper.stop()
        audioMeterHelper.stop()
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (e: Exception) {}
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    companion object {
        const val CHANNEL_ID = "permscript_channel"
    }
}
