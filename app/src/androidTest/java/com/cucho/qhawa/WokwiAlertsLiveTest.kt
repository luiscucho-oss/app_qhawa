package com.cucho.qhawa

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.core.notifications.EnvironmentalNotifications
import com.cucho.qhawa.data.environment.*
import com.cucho.qhawa.feature.home.HomeScreen
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Real Wokwi publisher required. Set its potentiometer above 50% before running. */
class WokwiAlertsLiveTest {
    @get:Rule val compose = createComposeRule()
    @Test fun liveRedWokwiReadingShowsBannerAndPostsSystemNotification() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveMqtt") == "true")
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(EnvironmentalNotifications.NOTIFICATION_ID)
        val repository = MqttEnvironmentRepository(context)
        compose.setContent { QhawaTheme {
            HomeScreen("Verificación Wokwi", null, {}, "wokwi-live-alert-test", true, repository)
        } }
        compose.waitUntil(60_000) {
            repository.state.value.connection == DeviceConnection.CONNECTED &&
                repository.state.value.readings.lastOrNull()?.demoState == "ROJO"
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("environment-alert-banner").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("environment-alert-banner").assertIsDisplayed()
        compose.onNodeWithText("Wokwi · Alerta roja").assertIsDisplayed()
        val folder = File(context.getExternalFilesDir(null), "notification-evidence").apply { mkdirs() }
        File(folder, "wokwi-live-red.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val notification = manager.activeNotifications.single { it.id == EnvironmentalNotifications.NOTIFICATION_ID }.notification
        assertEquals("Wokwi · Alerta roja", notification.extras.getString("android.title"))
        val channel = if (Build.VERSION.SDK_INT >= 26) manager.getNotificationChannel(EnvironmentalNotifications.CHANNEL) else null
        val audio = context.getSystemService(AudioManager::class.java)
        File(folder, "wokwi-notification-status.txt").writeText("""
            publisher=Wokwi (live MQTT)
            broker=${WokwiMqtt.HOST}
            reading=${repository.state.value.readings.last()}
            notificationTitle=${notification.extras.getString("android.title")}
            channelImportance=${channel?.importance}
            channelSound=${channel?.sound}
            ringerMode=${audio.ringerMode}
            notificationVolume=${audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION)}
            interruptionFilter=${manager.currentInterruptionFilter}
        """.trimIndent())
    }
}
