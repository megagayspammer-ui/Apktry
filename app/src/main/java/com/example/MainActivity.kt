package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.RadarScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.RadarViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: RadarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }

    @Composable
    private fun MainContent(viewModel: RadarViewModel) {
        var hasRequestedPermissions by remember { mutableStateOf(false) }

        val permissionsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { _ ->
            // Permissions granted or denied, refresh scan
            viewModel.refreshWifiScan()
        }

        LaunchedEffect(Unit) {
            if (!hasRequestedPermissions) {
                hasRequestedPermissions = true
                val requiredPerms = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requiredPerms.add(Manifest.permission.BLUETOOTH_SCAN)
                    requiredPerms.add(Manifest.permission.BLUETOOTH_CONNECT)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requiredPerms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                }

                val ungranted = requiredPerms.filter {
                    ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                }

                if (ungranted.isNotEmpty()) {
                    permissionsLauncher.launch(ungranted.toTypedArray())
                }
            }
        }

        RadarScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )
    }
}
