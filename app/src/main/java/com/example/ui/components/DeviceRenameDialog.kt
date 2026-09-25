package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NearbyDevice
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.NamedDeviceGreen
import com.example.ui.theme.RadarNeonCyan
import com.example.ui.theme.StudioRose

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceRenameDialog(
    device: NearbyDevice,
    onSave: (address: String, customName: String, notes: String) -> Unit,
    onDelete: (address: String) -> Unit,
    onDismiss: () -> Unit
) {
    var customName by remember(device) { mutableStateOf(device.customAlias ?: device.rawName) }
    var notes by remember(device) { mutableStateOf(device.notes ?: "") }

    val presetSuggestions = remember(device.deviceType) {
        if (device.deviceType.isWifi) {
            listOf("Home Wi-Fi", "Office AP", "Guest Network", "Extender 5G", "IoT Router", "Mobile Hotspot")
        } else {
            listOf("My Phone", "Work Laptop", "Headphones", "Smart Tag", "Living Room TV", "Car Audio", "Tablet", "Speaker")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = NamedDeviceGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Assign Custom Nickname",
                        color = DarkTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: ${device.id}",
                        color = DarkTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Name this device so you can easily recognize it on the radar map and device list:",
                    color = DarkTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Device Nickname") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NamedDeviceGreen,
                        focusedLabelColor = NamedDeviceGreen,
                        unfocusedBorderColor = Color(0xFF374151),
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_nickname_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Quick Presets:",
                    color = DarkTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (preset in presetSuggestions) {
                        FilterChip(
                            selected = customName.equals(preset, ignoreCase = true),
                            onClick = { customName = preset },
                            label = { Text(preset, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NamedDeviceGreen.copy(alpha = 0.25f),
                                selectedLabelColor = NamedDeviceGreen,
                                containerColor = Color(0xFF1C2541),
                                labelColor = DarkTextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Location, Owner, etc.)") },
                    singleLine = false,
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RadarNeonCyan,
                        focusedLabelColor = RadarNeonCyan,
                        unfocusedBorderColor = Color(0xFF374151),
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(device.id, customName, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NamedDeviceGreen),
                modifier = Modifier.testTag("save_nickname_button")
            ) {
                Text("Save Nickname", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (device.hasCustomName) {
                    TextButton(
                        onClick = { onDelete(device.id); onDismiss() },
                        colors = ButtonDefaults.textButtonColors(contentColor = StudioRose),
                        modifier = Modifier.testTag("delete_nickname_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = DarkTextSecondary)
                ) {
                    Text("Cancel")
                }
            }
        },
        containerColor = Color(0xFF131B2E),
        modifier = Modifier.testTag("rename_dialog")
    )
}
