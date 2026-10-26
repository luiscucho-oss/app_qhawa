package com.cucho.qhawa

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.data.environment.UnconfiguredEnvironmentRepository
import com.cucho.qhawa.feature.home.HomeScreen
import org.junit.Rule
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

class EnvironmentalFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun disconnectedDemoHistoryAndDailyActionAreInteractive() {
        val testUserId = "instrumentation-${System.nanoTime()}"
        val repository = UnconfiguredEnvironmentRepository()
        compose.setContent {
            QhawaTheme {
                HomeScreen(userName = "Prueba", userEmail = "prueba@example.com", onSignOutClick = {},
                    userId = testUserId, isDemoSession = true,
                    environmentRepository = repository)
            }
        }
        compose.onNodeWithText("Dispositivo desconectado").assertExists()
        compose.onNodeWithContentDescription("Explorar datos de ejemplo").performClick()
        compose.onNodeWithText("Vista de demostración").assertExists()
        // Regresión: las tres métricas deben poder leerse a la vez, sin desplazar horizontalmente.
        compose.onNodeWithText("µg/m³").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Temperatura").assertIsDisplayed()
        compose.onNodeWithText("Humedad").assertIsDisplayed()
        compose.onNodeWithText("PM").assertIsDisplayed()
        val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "mascot-evidence")
        folder.mkdirs()
        File(folder, "05-metricas-completas.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Historial").performClick()
        compose.onNodeWithText("Semana").performClick()
        compose.onNodeWithText("Datos de ejemplo").assertExists()
        compose.onNodeWithText("PM · µg/m³").assertExists()
        compose.onNodeWithText("Hábitos").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Marcar como realizada").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Marcar como realizada").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("¡Completado hoy!").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("¡Completado hoy!").assertIsNotEnabled()
        compose.onNodeWithText("Consejos").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Marcar como hecho").fetchSemanticsNodes().isNotEmpty() }
        // Completing the habit does not falsely mark a different recommendation as completed.
        compose.onNodeWithText("Marcar como hecho").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("¡Recomendación completada!").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Marcar como hecho").assertDoesNotExist()
    }
}
