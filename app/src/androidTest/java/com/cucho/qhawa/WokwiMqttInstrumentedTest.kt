package com.cucho.qhawa

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.data.environment.DeviceConnection
import com.cucho.qhawa.data.environment.MqttEnvironmentRepository
import com.cucho.qhawa.feature.home.HomeScreen
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in device test: -Pandroid.testInstrumentationRunnerArguments.liveMqtt=true */
class WokwiMqttInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun runningWokwiAppearsOnHomeScreen() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveMqtt") == "true")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = MqttEnvironmentRepository(context)
        compose.setContent {
            QhawaTheme {
                HomeScreen(userName = "Prueba MQTT", userEmail = null, onSignOutClick = {},
                    userId = "mqtt-instrumented-test", isDemoSession = true, environmentRepository = repository)
            }
        }
        compose.waitUntil(timeoutMillis = 60_000) {
            repository.state.value.connection == DeviceConnection.CONNECTED && repository.state.value.readings.lastOrNull()?.simulated == true
        }
        compose.onNodeWithText("Wokwi en vivo · Simulación", substring = false).assertExists()
        compose.onNodeWithText("PM2.5").assertExists()
        compose.onNodeWithText("Temperatura").assertExists()
        compose.onNodeWithText("Humedad").assertExists()
        compose.onNodeWithText("Actuar · Ver consejos").performScrollTo()
        compose.waitForIdle()
        File(context.filesDir, "mqtt-home-proof.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
