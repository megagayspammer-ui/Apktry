package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.NearbyDevice
import com.example.ui.components.AccuracyDetailsDialog
import com.example.ui.components.AccuracyNoticeBanner
import com.example.ui.components.AllAliasesDialog
import com.example.ui.components.DeviceDetailSheet
import com.example.ui.components.DeviceListView
import com.example.ui.components.DeviceRenameDialog
import com.example.ui.components.RadarCanvas
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.NamedDeviceGreen
import com.example.ui.theme.RadarNeonCyan
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioDarkSurface
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.WifiOrange
import com.example.viewmodel.DeviceFilter
import com.example.viewmodel.RadarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    viewModel: RadarViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RadarNeonCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = RadarNeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "NearMap Radar",
                                    color = DarkTextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Pulsing scanning dot
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.isScanning) RadarNeonCyan else Color.Gray)
                                )
                            }
                            Text(
                                text = if (uiState.isScanning) "Active RF Scanner (${uiState.devices.size} nearby)" else "Scanning paused",
                                color = DarkTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    // Compass Sync Toggle
                    IconButton(
                        onClick = { viewModel.toggleCompass() },
                        modifier = Modifier.testTag("toggle_compass_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Compass",
                            tint = if (uiState.isCompassEnabled) RadarNeonCyan else DarkTextSecondary
                        )
                    }

                    // Saved Nicknames Dialog Button
                    IconButton(
                        onClick = { viewModel.setAllAliasesDialogVisible(true) },
                        modifier = Modifier.testTag("saved_aliases_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (uiState.namedCount > 0) {
                                    Badge(
                                        containerColor = NamedDeviceGreen,
                                        contentColor = Color.Black
                                    ) {
                                        Text("${uiState.namedCount}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Saved Names",
                                tint = if (uiState.namedCount > 0) NamedDeviceGreen else DarkTextSecondary
                            )
                        }
                    }

                    // Disclaimer & RF Science Info Button
                    IconButton(
                        onClick = { viewModel.setDisclaimerDialogVisible(true) },
                        modifier = Modifier.testTag("info_disclaimer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Accuracy Disclaimer",
                            tint = StudioAmber
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioDarkSurface
                )
            )
        },
        containerColor = StudioDarkBg,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Prominent Accuracy Notice Banner
            AccuracyNoticeBanner(
                isVisible = uiState.isDisclaimerBannerVisible,
                onDismiss = { viewModel.dismissDisclaimerBanner() },
                onOpenDetails = { viewModel.setDisclaimerDialogVisible(true) }
            )

            // Overview Metric Badges Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(label = "Total", count = uiState.allDetectedCount, color = RadarNeonCyan, modifier = Modifier.weight(1f))
                MetricChip(label = "Bluetooth", count = uiState.bluetoothCount, color = StudioIndigo, modifier = Modifier.weight(1f))
                MetricChip(label = "Wi-Fi", count = uiState.wifiCount, color = WifiOrange, modifier = Modifier.weight(1f))
                MetricChip(label = "Named", count = uiState.namedCount, color = NamedDeviceGreen, modifier = Modifier.weight(1f))
            }

            // Controls Strip: Filter Chips & Search Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (filter in DeviceFilter.values()) {
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RadarNeonCyan.copy(alpha = 0.25f),
                            selectedLabelColor = RadarNeonCyan,
                            containerColor = Color(0xFF192238),
                            labelColor = DarkTextPrimary
                        ),
                        modifier = Modifier.testTag("filter_chip_${filter.name}")
                    )
                }

                // Range options
                listOf(5f, 15f, 25f, 50f).forEach { range ->
                    FilterChip(
                        selected = uiState.maxRangeMeters == range,
                        onClick = { viewModel.setMaxRangeMeters(range) },
                        label = { Text("${range.toInt()}m", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioAmber.copy(alpha = 0.25f),
                            selectedLabelColor = StudioAmber,
                            containerColor = Color(0xFF192238),
                            labelColor = DarkTextSecondary
                        ),
                        modifier = Modifier.testTag("range_chip_${range.toInt()}m")
                    )
                }

                IconButton(
                    onClick = { isSearchExpanded = !isSearchExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (uiState.searchQuery.isNotBlank() || isSearchExpanded) RadarNeonCyan else DarkTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Search Bar when expanded
            AnimatedVisibility(visible = isSearchExpanded || uiState.searchQuery.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search by name, ID or notes...", fontSize = 12.sp) },
                        singleLine = true,
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = DarkTextSecondary)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadarNeonCyan,
                            unfocusedBorderColor = Color(0xFF374151),
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("search_input")
                    )
                }
            }

            // Tab Switcher: Radar View vs List View
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = StudioDarkSurface,
                contentColor = RadarNeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = RadarNeonCyan
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Radar Sonar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier.testTag("tab_radar")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Device List (${uiState.devices.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier.testTag("tab_list")
                )
            }

            // Content Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedTab == 0) {
                    RadarCanvas(
                        devices = uiState.devices,
                        maxRangeMeters = uiState.maxRangeMeters,
                        azimuthDegrees = uiState.azimuthDegrees,
                        isCompassEnabled = uiState.isCompassEnabled,
                        isScanning = uiState.isScanning,
                        selectedDevice = uiState.selectedDevice,
                        onSelectDevice = { dev -> viewModel.selectDevice(dev) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    DeviceListView(
                        devices = uiState.devices,
                        onSelectDevice = { dev -> viewModel.selectDevice(dev) },
                        onOpenRename = { dev -> viewModel.openRenameDialog(dev) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Bottom Scan Controls Bar
            Surface(
                color = StudioDarkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compass Azimuth display
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = if (uiState.isCompassEnabled) RadarNeonCyan else DarkTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isCompassEnabled) "Azimuth: ${uiState.azimuthDegrees.toInt()}°" else "Compass: Fixed",
                            color = DarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Wi-Fi Refresh
                        IconButton(
                            onClick = { viewModel.refreshWifiScan() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("refresh_wifi_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Wi-Fi",
                                tint = WifiOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Live Scan Play/Pause
                        Surface(
                            color = if (uiState.isScanning) RadarNeonCyan else Color(0xFF374151),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.toggleScan() }
                                .testTag("scan_toggle_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.isScanning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (uiState.isScanning) Color.Black else DarkTextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isScanning) "Pause" else "Scan",
                                    color = if (uiState.isScanning) Color.Black else DarkTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modals & Sheets
        uiState.selectedDevice?.let { device ->
            DeviceDetailSheet(
                device = device,
                onDismiss = { viewModel.selectDevice(null) },
                onOpenRename = { viewModel.openRenameDialog(device) },
                sheetState = sheetState
            )
        }

        uiState.isRenamingDevice?.let { device ->
            DeviceRenameDialog(
                device = device,
                onSave = { address, name, notes ->
                    viewModel.saveDeviceAlias(address, name, notes)
                },
                onDelete = { address ->
                    viewModel.deleteDeviceAlias(address)
                },
                onDismiss = { viewModel.closeRenameDialog() }
            )
        }

        if (uiState.showDisclaimerDialog) {
            AccuracyDetailsDialog(
                onDismiss = { viewModel.setDisclaimerDialogVisible(false) }
            )
        }

        if (uiState.showAllAliasesDialog) {
            AllAliasesDialog(
                aliases = uiState.allSavedAliases,
                onDeleteAlias = { addr -> viewModel.deleteDeviceAlias(addr) },
                onDismiss = { viewModel.setAllAliasesDialogVisible(false) }
            )
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF141C2E),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = DarkTextSecondary,
                fontSize = 10.sp
            )
        }
    }
}
