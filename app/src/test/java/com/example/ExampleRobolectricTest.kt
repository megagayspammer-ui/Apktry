package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.DeviceType
import com.example.model.NearbyDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NearMap Radar", appName)
  }

  @Test
  fun `nearby device custom nickname and distance math`() {
    val device = NearbyDevice(
      id = "AA:BB:CC:DD:EE:FF",
      rawName = "Wireless Headset",
      customAlias = "My Beats Studio",
      deviceType = DeviceType.BLUETOOTH_LE,
      rssi = -60,
      bearingDegrees = 45f,
      txPower = -59
    )

    // Check alias priority
    assertEquals("My Beats Studio", device.displayName)
    assertTrue(device.hasCustomName)

    // Check distance is calculated within realistic range [0.3m, 60m]
    assertTrue(device.estimatedDistanceMeters > 0.3)
    assertTrue(device.estimatedDistanceMeters < 5.0)
  }
}
