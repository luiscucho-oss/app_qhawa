package com.cucho.qhawa

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.core.navigation.QhawaNavHost

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La app usa tema claro, así que los íconos de las barras del sistema van oscuros.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val authRepository = (application as QhawaApplication).container.authRepository
        setContent {
            QhawaTheme {
                QhawaNavHost(authRepository = authRepository)
            }
        }
    }
}
