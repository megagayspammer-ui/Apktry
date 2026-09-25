package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceType
import com.example.model.NearbyDevice
import com.example.ui.theme.BeaconViolet
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.NamedDeviceGreen
import com.example.ui.theme.RadarNeonCyan
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.WifiOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailSheet(
    device: NearbyDevice,
    onDismiss: () -> Unit,
    onOpenRename: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF101726),
        dragHandle = null,
        modifier = modifier.testTag("device_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconVector = when (device.deviceType) {
                    DeviceType.WIFI_AP -> Icons.Default.Wifi
                    DeviceType.BEACON -> Icons.Default.Sensors
                    else -> Icons.Default.Bluetooth
                }

                val iconBg = when {
                    device.hasCustomName -> NamedDeviceGreen.copy(alpha = 0.2f)
                    device.deviceType.isWifi -> WifiOrange.copy(alpha = 0.2f)
                    else -> RadarNeonCyan.copy(alpha = 0.2f)
                }

                val iconTint = when {
                    device.hasCustomName -> NamedDeviceGreen
                    device.deviceType.isWifi -> WifiOrange
                    else -> RadarNeonCyan
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.displayName,
                            color = DarkTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (device.hasCustomName) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Custom Name",
                                tint = NamedDeviceGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (device.hasCustomName) {
                        Text(
                            text = "Broadcast: ${device.rawName}",
                            color = DarkTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = "ID: ${device.id}",
                        color = DarkTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("detail_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = DarkTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action: Edit / Set Custom Nickname
            Surface(
                color = Color(0xFF192238),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NamedDeviceGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (device.hasCustomName) "Custom Alias: ${device.customAlias}" else "No Custom Name Assigned",
                            color = if (device.hasCustomName) NamedDeviceGreen else DarkTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!device.notes.isNullOrBlank()) {
                            Text(
                                text = "Note: ${device.notes}",
                                color = DarkTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = onOpenRename,
                        colors = ButtonDefaults.buttonColors(containerColor = NamedDeviceGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("edit_alias_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (device.hasCustomName) "Change" else "Name It", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Distance & Signal Metric Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Distance card
                Surface(
                    color = Color(0xFF192238),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("EST. DISTANCE", color = DarkTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "~${device.estimatedDistanceMeters}",
                                color = RadarNeonCyan,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("m", color = DarkTextSecondary, fontSize = 14.sp)
                        }
                        Text(
                            text = "(${device.estimatedDistanceFeet} ft)",
                            color = DarkTextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = device.proximityLevel.label,
                            color = Color(device.proximityLevel.colorHex),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Signal Strength card
                Surface(
                    color = Color(0xFF192238),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("RF SIGNAL (RSSI)", color = DarkTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${device.rssi}",
                                color = DarkTextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("dBm", color = DarkTextSecondary, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (device.signalQualityPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (device.rssi >= -65) RadarNeonCyan else StudioAmber,
                            trackColor = Color(0xFF27354E),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${device.signalQualityPercent}% Quality",
                            color = DarkTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Signal Sparkline
            Surface(
                color = Color(0xFF192238),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("LIVE RSSI TELEMETRY (dBm)", color = DarkTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Last seen: ${device.formattedLastSeen}", color = DarkTextSecondary, fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    RssiSparkline(
                        history = device.signalHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Technical specs table
            Surface(
                color = Color(0xFF192238),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("PROTOCOL & TECHNICAL SPECS", color = DarkTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    TechRow("Protocol", device.deviceType.label)
                    TechRow("Bearing Angle", "${device.bearingDegrees.toInt()}°")
                    if (device.frequencyMHz != null) {
                        TechRow("Frequency", "${device.frequencyMHz} MHz")
                    }
                    if (device.channel != null) {
                        TechRow("Channel", "Ch ${device.channel}")
                    }
                    if (device.txPower != null) {
                        TechRow("Ref TxPower", "${device.txPower} dBm")
                    }
                    if (!device.capabilities.isNullOrBlank()) {
                        TechRow("Capabilities", device.capabilities)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TechRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = DarkTextSecondary, fontSize = 12.sp)
        Text(
            text = value,
            color = DarkTextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RssiSparkline(
    history: List<Int>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (history.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        val minRssi = -100f
        val maxRssi = -40f

        val stepX = if (history.size > 1) w / (history.size - 1) else w

        val path = Path()
        history.forEachIndexed { i, rssi ->
            val clamped = rssi.toFloat().coerceIn(minRssi, maxRssi)
            val normalizedY = 1f - ((clamped - minRssi) / (maxRssi - minRssi))
            val x = i * stepX
            val y = normalizedY * h

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            // Draw dot at each point
            drawCircle(
                color = RadarNeonCyan,
                radius = 3f,
                center = Offset(x, y)
            )
        }

        drawPath(
            path = path,
            color = RadarNeonCyan,
            style = Stroke(width = 2.5f)
        )
    }
}
