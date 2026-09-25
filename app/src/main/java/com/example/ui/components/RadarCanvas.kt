package com.example.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.DeviceType
import com.example.model.NearbyDevice
import com.example.ui.theme.BeaconViolet
import com.example.ui.theme.NamedDeviceGreen
import com.example.ui.theme.RadarGridLine
import com.example.ui.theme.RadarNeonCyan
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.WifiOrange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun RadarCanvas(
    devices: List<NearbyDevice>,
    maxRangeMeters: Float,
    azimuthDegrees: Float,
    isCompassEnabled: Boolean,
    isScanning: Boolean,
    selectedDevice: NearbyDevice?,
    onSelectDevice: (NearbyDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")
    
    // Rotating sweep angle 0..360
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isScanning) 3200 else 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    // Pulse animation for blips
    val pulseRatio by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ratio"
    )

    val effectiveAzimuth = if (isCompassEnabled) azimuthDegrees else 0f

    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }

    val cardinalPaint = remember {
        Paint().apply {
            color = android.graphics.Color.parseColor("#38BDF8")
            textSize = 32f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }

    val blipTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.parseColor("#F9FAFB")
            textSize = 24f
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("radar_sonar_canvas")
                .pointerInput(devices, maxRangeMeters, effectiveAzimuth) {
                    detectTapGestures { tapOffset ->
                        val minDim = kotlin.math.min(size.width, size.height).toFloat()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = (minDim / 2f) * 0.86f
                        val touchTolerance = 48f

                        var closestDevice: NearbyDevice? = null
                        var minDistance = Float.MAX_VALUE

                        for (dev in devices) {
                            val r = ((dev.estimatedDistanceMeters / maxRangeMeters).toFloat().coerceIn(0.08f, 1f)) * radius
                            val totalAngleDeg = dev.bearingDegrees - effectiveAzimuth - 90f
                            val rad = Math.toRadians(totalAngleDeg.toDouble())
                            val x = center.x + (r * cos(rad)).toFloat()
                            val y = center.y + (r * sin(rad)).toFloat()

                            val dist = sqrt((tapOffset.x - x) * (tapOffset.x - x) + (tapOffset.y - y) * (tapOffset.y - y))
                            if (dist < touchTolerance && dist < minDistance) {
                                minDistance = dist
                                closestDevice = dev
                            }
                        }

                        if (closestDevice != null) {
                            onSelectDevice(closestDevice)
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = (size.minDimension / 2f) * 0.86f

            // 1. Draw radar circular dark background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0F1A2F), Color(0xFF070B14)),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            // 2. Draw Concentric Range Rings (4 rings)
            val ringFractions = listOf(0.25f, 0.50f, 0.75f, 1.0f)
            val dashedStroke = Stroke(
                width = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            ringFractions.forEachIndexed { index, fraction ->
                val ringRadius = maxRadius * fraction
                val isOuter = index == ringFractions.lastIndex

                drawCircle(
                    color = if (isOuter) RadarNeonCyan.copy(alpha = 0.7f) else RadarGridLine,
                    radius = ringRadius,
                    center = center,
                    style = if (isOuter) Stroke(width = 2.5f) else dashedStroke
                )

                // Range label on the vertical axis (e.g. "5m", "10m", "25m")
                val rangeAtRing = (maxRangeMeters * fraction).toInt()
                drawContext.canvas.nativeCanvas.drawText(
                    "${rangeAtRing}m",
                    center.x + 8f,
                    center.y - ringRadius + 24f,
                    textPaint
                )
            }

            // 3. Crosshairs
            drawLine(
                color = RadarGridLine.copy(alpha = 0.4f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.2f
            )
            drawLine(
                color = RadarGridLine.copy(alpha = 0.4f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.2f
            )

            // 4. Rotating sweep sector with phosphor trail
            if (isScanning) {
                rotate(degrees = sweepAngle, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to Color.Transparent,
                            0.75f to Color.Transparent,
                            0.90f to RadarNeonCyan.copy(alpha = 0.08f),
                            1f to RadarNeonCyan.copy(alpha = 0.35f),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = true,
                        topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                        size = androidx.compose.ui.geometry.Size(maxRadius * 2f, maxRadius * 2f)
                    )

                    // Sharp leading scan line
                    drawLine(
                        color = RadarNeonCyan.copy(alpha = 0.9f),
                        start = center,
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 2.5f
                    )
                }
            }

            // 5. Cardinal Directions (N, E, S, W) rotated by effective azimuth
            val cardinalRadius = maxRadius + 24f
            val cardinals = listOf(
                "N" to 0f,
                "E" to 90f,
                "S" to 180f,
                "W" to 270f
            )

            for ((label, baseAngle) in cardinals) {
                val angleDeg = baseAngle - effectiveAzimuth - 90f
                val rad = Math.toRadians(angleDeg.toDouble())
                val x = center.x + (cardinalRadius * cos(rad)).toFloat()
                val y = center.y + (cardinalRadius * sin(rad)).toFloat() + 10f
                drawContext.canvas.nativeCanvas.drawText(label, x, y, cardinalPaint)
            }

            // 6. Center device marker (User Phone)
            drawCircle(
                color = RadarNeonCyan.copy(alpha = 0.25f),
                radius = 16f * pulseRatio,
                center = center
            )
            drawCircle(
                color = RadarNeonCyan,
                radius = 8f,
                center = center
            )

            // 7. Draw Detected Device Blips
            for (device in devices) {
                val r = ((device.estimatedDistanceMeters / maxRangeMeters).toFloat().coerceIn(0.08f, 1f)) * maxRadius
                val totalAngleDeg = device.bearingDegrees - effectiveAzimuth - 90f
                val rad = Math.toRadians(totalAngleDeg.toDouble())
                val blipX = center.x + (r * cos(rad)).toFloat()
                val blipY = center.y + (r * sin(rad)).toFloat()
                val blipOffset = Offset(blipX, blipY)

                val isSelected = selectedDevice?.id == device.id

                val blipColor = when {
                    device.hasCustomName -> NamedDeviceGreen
                    device.deviceType.isWifi -> WifiOrange
                    device.deviceType == DeviceType.BEACON -> BeaconViolet
                    device.deviceType == DeviceType.BLUETOOTH_CLASSIC -> StudioIndigo
                    else -> RadarNeonCyan
                }

                // If selected, draw glowing target reticle
                if (isSelected) {
                    drawCircle(
                        color = blipColor.copy(alpha = 0.4f),
                        radius = 24f * pulseRatio,
                        center = blipOffset,
                        style = Stroke(width = 2.5f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 14f,
                        center = blipOffset,
                        style = Stroke(width = 2f)
                    )
                }

                // Outer signal glow for strong signals
                if (device.rssi >= -65) {
                    drawCircle(
                        color = blipColor.copy(alpha = 0.3f),
                        radius = 15f * pulseRatio,
                        center = blipOffset
                    )
                }

                // Core blip dot
                drawCircle(
                    color = blipColor,
                    radius = if (isSelected) 8f else 6f,
                    center = blipOffset
                )

                // Label next to blip
                val label = if (device.displayName.length > 14) {
                    device.displayName.take(12) + ".."
                } else {
                    device.displayName
                }

                val distLabel = "${device.estimatedDistanceMeters}m"
                drawContext.canvas.nativeCanvas.drawText(
                    "$label ($distLabel)",
                    blipX + 10f,
                    blipY + 4f,
                    blipTextPaint
                )
            }
        }
    }
}
