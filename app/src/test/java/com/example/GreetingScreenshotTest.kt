package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.DeviceType
import com.example.model.NearbyDevice
import com.example.ui.components.DeviceListView
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleDevices = listOf(
      NearbyDevice(
        id = "C4:29:96:A1:B2:10",
        rawName = "MacBook Pro",
        customAlias = "Ben's Workstation",
        deviceType = DeviceType.BLUETOOTH_LE,
        rssi = -55,
        bearingDegrees = 30f
      ),
      NearbyDevice(
        id = "A0:32:89:FE:44:22",
        rawName = "Home_Mesh_5G",
        customAlias = "Living Room Router",
        deviceType = DeviceType.WIFI_AP,
        rssi = -62,
        bearingDegrees = 180f
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        DeviceListView(
          devices = sampleDevices,
          onSelectDevice = {},
          onOpenRename = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
