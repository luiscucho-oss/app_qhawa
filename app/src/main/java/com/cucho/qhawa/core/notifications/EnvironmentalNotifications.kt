package com.cucho.qhawa.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cucho.qhawa.MainActivity
import com.cucho.qhawa.R

class EnvironmentalNotifications(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    init {
        if (Build.VERSION.SDK_INT >= 26) {
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Alertas ambientales", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Cambios en las lecturas de Qhawa; Wokwi se identifica como simulación"
                setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
            })
        }
    }
    fun permissionRequired(): Boolean = Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    fun enabled(): Boolean = !permissionRequired() &&
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 26 || manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)

    fun status(): String = when {
        !enabled() -> "Notificaciones de Android desactivadas. El banner sigue disponible."
        Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(CHANNEL)?.sound == null -> "Notificaciones activas · Canal sin sonido"
        else -> "Notificaciones activas · Tono del sistema (según volumen y No molestar)"
    }

    fun post(alert: EnvironmentalAlert): Boolean {
        if (!enabled()) return false
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ADVICE
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(context, 20, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(alert.title).setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setColor(alert.level.color.toInt()).setContentIntent(pending)
            .setCategory(NotificationCompat.CATEGORY_STATUS).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true).setSilent(!alert.sound)
            .apply { if (Build.VERSION.SDK_INT < 26 && alert.sound) setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)) }
            .build()
        return try { manager.notify(NOTIFICATION_ID, notification); true } catch (_: SecurityException) { false }
    }
    companion object {
        const val CHANNEL = "qhawa_environment_v1"
        const val NOTIFICATION_ID = 2001
        const val ACTION_ADVICE = "com.cucho.qhawa.SHOW_ADVICE"
    }
}
