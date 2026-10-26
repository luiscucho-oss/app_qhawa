package com.cucho.qhawa

import android.graphics.Color
import android.content.Intent
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.cucho.qhawa.core.notifications.EnvironmentalNotifications
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.core.navigation.QhawaNavHost

class MainActivity : ComponentActivity() {
    private var notificationRequest by mutableLongStateOf(0)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == EnvironmentalNotifications.ACTION_ADVICE) notificationRequest++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == EnvironmentalNotifications.ACTION_ADVICE) notificationRequest++
        // La app usa tema claro, así que los íconos de las barras del sistema van oscuros.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val authRepository = (application as QhawaApplication).container.authRepository
        setContent {
            QhawaTheme {
                QhawaNavHost(authRepository = authRepository, notificationRequest = notificationRequest)
            }
        }
    }
}
