package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceType
import com.example.model.NearbyDevice
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.NamedDeviceGreen
import com.example.ui.theme.RadarNeonCyan
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.WifiOrange

@Composable
fun DeviceListView(
    devices: List<NearbyDevice>,
    onSelectDevice: (NearbyDevice) -> Unit,
    onOpenRename: (NearbyDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    if (devices.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = DarkTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No devices detected matching filter",
                    color = DarkTextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Try adjusting the range, signal filter, or search term",
                    color = DarkTextSecondary.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("devices_lazy_column"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices, key = { it.id }) { device ->
                DeviceListItem(
                    device = device,
                    onClick = { onSelectDevice(device) },
                    onRename = { onOpenRename(device) }
                )
            }
        }
    }
}

@Composable
private fun DeviceListItem(
    device: NearbyDevice,
    onClick: () -> Unit,
    onRename: () -> Unit
) {
    val borderColor = when {
        device.hasCustomName -> NamedDeviceGreen.copy(alpha = 0.4f)
        device.deviceType.isWifi -> WifiOrange.copy(alpha = 0.2f)
        else -> Color(0xFF26334D)
    }

    Surface(
        color = Color(0xFF141C2E),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("device_card_${device.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val iconVector = when (device.deviceType) {
                DeviceType.WIFI_AP -> Icons.Default.Wifi
                DeviceType.BEACON -> Icons.Default.Sensors
                else -> Icons.Default.Bluetooth
            }

            val iconColor = when {
                device.hasCustomName -> NamedDeviceGreen
                device.deviceType.isWifi -> WifiOrange
                else -> RadarNeonCyan
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.displayName,
                        color = DarkTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (device.hasCustomName) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Custom Alias",
                            tint = NamedDeviceGreen,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.deviceType.label,
                        color = DarkTextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = " • ",
                        color = DarkTextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = device.id,
                        color = DarkTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (!device.notes.isNullOrBlank()) {
                    Text(
                        text = "Note: ${device.notes}",
                        color = NamedDeviceGreen.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Distance & Signal
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "~${device.estimatedDistanceMeters}m",
                    color = RadarNeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${device.rssi} dBm",
                    color = if (device.rssi >= -65) RadarNeonCyan else DarkTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Quick Rename Icon
            IconButton(
                onClick = onRename,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("quick_rename_${device.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename",
                    tint = if (device.hasCustomName) NamedDeviceGreen else DarkTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
