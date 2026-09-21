package com.example.ui.docs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ApiDocItem(
    val signature: String,
    val category: String,
    val description: String,
    val sampleSnippet: String
)

val API_DOCS = listOf(
    ApiDocItem(
        signature = "Android.toast(message)",
        category = "UI",
        description = "Shows an Android native toast popup message on screen.",
        sampleSnippet = "Android.toast(\"Hello World!\");"
    ),
    ApiDocItem(
        signature = "Android.vibrate(durationMs)",
        category = "Haptics",
        description = "Triggers a haptic vibration for the specified duration (e.g. 100ms).",
        sampleSnippet = "Android.vibrate(150);"
    ),
    ApiDocItem(
        signature = "Android.vibratePattern(jsonArray)",
        category = "Haptics",
        description = "Triggers a custom waveform vibration pattern, e.g. [0, 100, 50, 200].",
        sampleSnippet = "Android.vibratePattern(JSON.stringify([0, 80, 40, 80]));"
    ),
    ApiDocItem(
        signature = "Android.notify(title, message)",
        category = "Notifications",
        description = "Dispatches a native heads-up system notification to the Android notification shade.",
        sampleSnippet = "Android.notify(\"Alert\", \"Task completed!\");"
    ),
    ApiDocItem(
        signature = "Android.speak(text)",
        category = "Speech",
        description = "Speaks any text out loud using the Android Text-to-Speech (TTS) engine.",
        sampleSnippet = "Android.speak(\"System online and operational.\");"
    ),
    ApiDocItem(
        signature = "Android.stopSpeaking()",
        category = "Speech",
        description = "Immediately cancels and stops active speech synthesis.",
        sampleSnippet = "Android.stopSpeaking();"
    ),
    ApiDocItem(
        signature = "Android.toggleFlashlight(state)",
        category = "Hardware",
        description = "Turns the camera LED flashlight on (true) or off (false).",
        sampleSnippet = "Android.toggleFlashlight(true);"
    ),
    ApiDocItem(
        signature = "Android.getBatteryInfo()",
        category = "System",
        description = "Returns JSON string with { level, isCharging, temperatureCelsius }.",
        sampleSnippet = "const bat = JSON.parse(Android.getBatteryInfo());\nconsole.log(bat.level + '%');"
    ),
    ApiDocItem(
        signature = "Android.getNetworkInfo()",
        category = "Network",
        description = "Returns JSON string with { isConnected, type, isWifi, isCellular }.",
        sampleSnippet = "const net = JSON.parse(Android.getNetworkInfo());"
    ),
    ApiDocItem(
        signature = "Android.getDeviceInfo()",
        category = "System",
        description = "Returns JSON string with { model, brand, manufacturer, androidVersion, sdkInt }.",
        sampleSnippet = "const dev = JSON.parse(Android.getDeviceInfo());"
    ),
    ApiDocItem(
        signature = "Android.getLocation()",
        category = "Location",
        description = "Requests GPS fix; calls window.onLocationResult(lat, lng, acc, alt, speed).",
        sampleSnippet = "Android.getLocation();\nwindow.onLocationResult = (lat, lng, acc) => { console.log(lat, lng); };"
    ),
    ApiDocItem(
        signature = "Android.startSensorStream()",
        category = "Sensors",
        description = "Streams accelerometer, gyro, and compass into window.onSensorData(type, x, y, z).",
        sampleSnippet = "Android.startSensorStream();\nwindow.onSensorData = (type, x, y, z) => { };"
    ),
    ApiDocItem(
        signature = "Android.stopSensorStream()",
        category = "Sensors",
        description = "Stops sensor listeners to conserve device power.",
        sampleSnippet = "Android.stopSensorStream();"
    ),
    ApiDocItem(
        signature = "Android.startSoundMeter()",
        category = "Audio",
        description = "Streams live microphone RMS decibels and amplitude to window.onSoundMeter(db, amp).",
        sampleSnippet = "Android.startSoundMeter();\nwindow.onSoundMeter = (db, amp) => { };"
    ),
    ApiDocItem(
        signature = "Android.stopSoundMeter()",
        category = "Audio",
        description = "Stops microphone recording stream.",
        sampleSnippet = "Android.stopSoundMeter();"
    ),
    ApiDocItem(
        signature = "Android.getContacts(limit)",
        category = "Contacts",
        description = "Returns JSON array of contacts [{ name, phone }] up to limit.",
        sampleSnippet = "const contacts = JSON.parse(Android.getContacts(20));"
    ),
    ApiDocItem(
        signature = "Android.getCalendarEvents(limit)",
        category = "Calendar",
        description = "Returns JSON array of calendar events [{ title, startTime, location }].",
        sampleSnippet = "const events = JSON.parse(Android.getCalendarEvents(10));"
    ),
    ApiDocItem(
        signature = "Android.copyToClipboard(text)",
        category = "Clipboard",
        description = "Copies text string into the Android system clipboard.",
        sampleSnippet = "Android.copyToClipboard(\"Saved token\");"
    ),
    ApiDocItem(
        signature = "Android.getClipboardText()",
        category = "Clipboard",
        description = "Retrieves current text contents of the system clipboard.",
        sampleSnippet = "const text = Android.getClipboardText();"
    ),
    ApiDocItem(
        signature = "Android.checkPermission(key)",
        category = "Permissions",
        description = "Checks if a specific permission is currently granted (returns boolean).",
        sampleSnippet = "const hasCam = Android.checkPermission(\"android.permission.CAMERA\");"
    ),
    ApiDocItem(
        signature = "Android.requestPermission(key)",
        category = "Permissions",
        description = "Prompts the native Android runtime permission dialog for a permission.",
        sampleSnippet = "Android.requestPermission(\"android.permission.RECORD_AUDIO\");"
    )
)

@Composable
fun ApiDocsDialog(
    onDismiss: () -> Unit,
    onInsertSnippet: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Android.* JS Bridge API", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Available in all scripts without compilation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_docs_button")) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(API_DOCS) { item ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.signature,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        item.category,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(item.sampleSnippet))
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                    Text("Copy", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                TextButton(
                                    onClick = {
                                        onInsertSnippet(item.sampleSnippet)
                                        onDismiss()
                                    }
                                ) {
                                    Text("Insert in Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_docs_btn")) {
                Text("Close")
            }
        }
    )
}
