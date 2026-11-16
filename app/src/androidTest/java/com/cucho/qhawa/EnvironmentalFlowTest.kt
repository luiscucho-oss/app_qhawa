package com.cucho.qhawa

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.data.environment.EnvironmentRepository
import com.cucho.qhawa.data.environment.EnvironmentState
import com.cucho.qhawa.data.habits.dailyAdvice
import kotlinx.coroutines.flow.MutableStateFlow
import com.cucho.qhawa.feature.home.HomeScreen
import org.junit.Rule
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

class EnvironmentalFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun disconnectedDemoHistoryAndDailyActionAreInteractive() {
        val testUserId = "instrumentation-${System.nanoTime()}"
        val repository = object : EnvironmentRepository { override val state = MutableStateFlow(EnvironmentState()) }
        compose.setContent {
            QhawaTheme {
                HomeScreen(userName = "Prueba", userEmail = "prueba@example.com", onSignOutClick = {},
                    userId = testUserId, isDemoSession = true,
                    environmentRepository = repository)
            }
        }
        compose.onNodeWithText("Dispositivo desconectado").assertExists()
        compose.onNodeWithContentDescription("Explorar datos de ejemplo").assertIsOff().performClick()
        compose.onNodeWithText("Datos de ejemplo").assertExists()
        compose.onNode(hasText("Ejemplo") and hasAnyAncestor(hasTestTag("home-air-quality"))).assertDoesNotExist()
        compose.onNodeWithText("Vista de demostración").assertExists()
        compose.onNodeWithText("Material Particulado (PM10)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Temperatura").assertExists()
        compose.onNodeWithText("Humedad").assertExists()
        compose.onNodeWithText("Material Particulado (PM2.5)").assertIsDisplayed()
        val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "mascot-evidence")
        folder.mkdirs()
        File(folder, "05-metricas-completas.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Historial").performClick()
        compose.onNodeWithText("Semana").performClick()
        compose.onNodeWithText("Datos de ejemplo").assertExists()
        compose.onNodeWithContentDescription("Explorar datos de ejemplo").assertIsOn()
        compose.onNodeWithText("PM2.5 · µg/m³").assertExists()
        compose.onNodeWithText("Hábitos").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Completar").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Completar").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("¡Completado hoy!").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("¡Completado hoy!").assertIsNotEnabled()
        compose.onNodeWithText("Consejos").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText(dailyAdvice.first().title).fetchSemanticsNodes().isNotEmpty() }
        // Completing the habit does not falsely mark a different recommendation as completed.
        compose.onNodeWithText(dailyAdvice.first().title).performScrollTo().performClick()
        compose.onNodeWithText("Marcar como hecho").assertIsEnabled().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Acción registrada").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Marcar como hecho").assertDoesNotExist()
    }
}
