package com.cucho.qhawa

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.data.environment.*
import com.cucho.qhawa.feature.home.HomeScreen
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import java.io.File

class EnvironmentalNotificationsTest {
    @get:Rule val compose = createComposeRule()
    private fun sample(temp: Double = 24.0, hum: Double = 50.0) = EnvironmentalReading(System.currentTimeMillis(),
        10.0, temp, hum, "PM2.5", true, System.nanoTime(), System.nanoTime(), "VERDE")

    @Test fun bannerOverlaysContentResetsTimerForSameColorAndOpensAdvice() {
        val samples = MutableStateFlow(EnvironmentState(DeviceConnection.CONNECTED, listOf(sample()), true))
        val repository = object : EnvironmentRepository { override val state = samples }
        compose.setContent { QhawaTheme {
            HomeScreen("Prueba de banner", null, {}, "notification-test", true, repository)
        } }
        compose.waitForIdle()
        compose.runOnIdle { samples.value = samples.value.copy(readings = listOf(sample(temp = 29.0))) }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("environment-alert-banner").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("environment-alert-banner").assertIsDisplayed()
        SystemClock.sleep(4000)
        compose.runOnIdle { samples.value = samples.value.copy(readings = listOf(sample(temp = 29.0, hum = 70.0))) }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Humedad: 70.0", substring = true).fetchSemanticsNodes().isNotEmpty() }
        SystemClock.sleep(2500) // Past the first event's 6 s expiry, but before the second expiry.
        compose.onNodeWithTag("environment-alert-banner").assertIsDisplayed()
        screenshot("banner-warning")
        compose.onNodeWithText("Toca para ver consejos").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Consejos para hoy").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("environment-alert-banner").assertDoesNotExist()
    }

    private fun screenshot(name: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val folder = File(context.getExternalFilesDir(null), "notification-evidence").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
