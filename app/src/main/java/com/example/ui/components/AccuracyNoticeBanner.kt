package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.RadarNeonCyan
import com.example.ui.theme.StudioAmber

@Composable
fun AccuracyNoticeBanner(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            color = Color(0xFF1E2433),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .border(1.dp, StudioAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .clickable { onOpenDetails() }
                .testTag("accuracy_notice_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Notice",
                    tint = StudioAmber,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Estimated Proximity Radar",
                        color = StudioAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Distances are calculated from RF signal levels (RSSI). Walls and physical barriers cause natural variances.",
                        color = DarkTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                TextButton(
                    onClick = onOpenDetails,
                    modifier = Modifier.testTag("banner_details_button")
                ) {
                    Text("Info", fontSize = 11.sp, color = RadarNeonCyan)
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("banner_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AccuracyDetailsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = RadarNeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RF Positioning & Accuracy",
                    color = DarkTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccuracyItem(
                    title = "1. Path Loss & Signal Math",
                    desc = "Bluetooth and Wi-Fi distance is computed via the Log-Distance Path Loss Model [d = 10^((TxPower - RSSI)/(10*n))]. A decrease of ~6 dBm corresponds roughly to doubling the distance."
                )

                AccuracyItem(
                    title = "2. Physical Obstacle Attenuation",
                    desc = "Concrete walls, metal doors, glass, and even human body water content absorb 2.4 GHz and 5 GHz signals, making a device on the other side of a wall appear further away than it is in straight line-of-sight."
                )

                AccuracyItem(
                    title = "3. Compass & Bearing Alignment",
                    desc = "When Compass Sync is active, rotating your phone rotates the radar map using the device's internal magnetometer and orientation sensors."
                )

                AccuracyItem(
                    title = "4. Persistent Custom Nicknames",
                    desc = "You can assign friendly names (e.g., 'Ben's Laptop', 'Living Room AP', 'My Headphones') to any device. These are saved in your device's local database and automatically applied whenever the signal is received."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = RadarNeonCyan),
                modifier = Modifier.testTag("disclaimer_ok_button")
            ) {
                Text("Got it", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF131B2E),
        modifier = Modifier.testTag("accuracy_dialog")
    )
}

@Composable
private fun AccuracyItem(title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1C2541))
            .padding(10.dp)
    ) {
        Text(
            text = title,
            color = RadarNeonCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = desc,
            color = DarkTextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}
