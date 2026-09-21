package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.PermissionRegistry
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
    assertEquals("PermScript Studio", appName)
  }

  @Test
  fun `permission registry has all permissions`() {
    assertTrue(PermissionRegistry.ALL_PERMISSIONS.isNotEmpty())
    assertTrue(PermissionRegistry.ALL_PERMISSIONS.any { it.id == "camera" })
    assertTrue(PermissionRegistry.ALL_PERMISSIONS.any { it.id == "mic" })
    assertTrue(PermissionRegistry.ALL_PERMISSIONS.any { it.id == "fine_location" })
  }
}

