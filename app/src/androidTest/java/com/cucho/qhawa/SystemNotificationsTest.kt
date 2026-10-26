package com.cucho.qhawa

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.cucho.qhawa.core.notifications.*
import org.junit.Assert.*
import org.junit.Test

class SystemNotificationsTest {
    @Test fun systemNotificationHasChannelSoundAndAction() {
        android.util.Log.i("QhawaNotificationCheck", "notification-test-entered")
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        android.util.Log.i("QhawaNotificationCheck", "permission-granted")
        val service = EnvironmentalNotifications(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        assertTrue(service.enabled())
        android.util.Log.i("QhawaNotificationCheck", "posting-notification")
        assertTrue(service.post(EnvironmentalAlert(200, AlertLevel.CRITICAL, "Qhawa · Prueba de campana",
            "Aviso de verificación. No es una medición del ambiente.", true)))
        try {
            val active = manager.activeNotifications.single { it.id == EnvironmentalNotifications.NOTIFICATION_ID }.notification
            assertNotNull(active.contentIntent)
            assertEquals("Qhawa · Prueba de campana", active.extras.getString("android.title"))
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = manager.getNotificationChannel(EnvironmentalNotifications.CHANNEL)
                assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
                assertNotNull(channel.sound)
                assertEquals(channel.id, active.channelId)
            } else assertNotNull(active.sound)
            SystemClock.sleep(3000) // Give the Android sound service time to play before cleanup.
        } finally { manager.cancel(EnvironmentalNotifications.NOTIFICATION_ID) }
    }
}
