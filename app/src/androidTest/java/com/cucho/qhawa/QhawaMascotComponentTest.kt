package com.cucho.qhawa

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import com.cucho.qhawa.core.designsystem.component.QhawaMascotComponent
import com.cucho.qhawa.core.designsystem.component.QhawaMascotState
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.data.environment.UnconfiguredEnvironmentRepository
import com.cucho.qhawa.feature.home.HomeScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

class QhawaMascotComponentTest {
    @get:Rule val compose = createComposeRule()

    private fun mascot() = compose.onNodeWithContentDescription("Qhawa, mascota interactiva")
    private fun hasPose(description: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, description)

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "mascot-evidence")
        folder.mkdirs()
        File(folder, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun touchChangesMessageCelebratesAndReturnsToParentPose() {
        val message = mutableStateOf("Hola")
        var clicks = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            QhawaTheme {
                QhawaMascotComponent(QhawaMascotState.IDLE, message.value, onMascotClick = {
                    clicks++
                    message.value = "Nuevo consejo"
                })
            }
        }
        compose.mainClock.advanceTimeBy(2000)
        mascot().performClick()
        compose.mainClock.advanceTimeBy(100)
        mascot().assert(hasPose("Feliz"))
        compose.mainClock.advanceTimeBy(2500)
        mascot().assert(hasPose("En reposo"))
        compose.onNodeWithText("Nuevo consejo").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun rapidTouchesAndExternalAlertRestoreTheLatestParentState() {
        val state = mutableStateOf(QhawaMascotState.GREETING)
        val message = mutableStateOf("Hola")
        compose.mainClock.autoAdvance = false
        compose.setContent {
            QhawaTheme { QhawaMascotComponent(state.value, message.value) }
        }
        compose.mainClock.advanceTimeBy(2000)
        repeat(3) {
            mascot().performClick()
            compose.mainClock.advanceTimeBy(100)
        }
        compose.runOnIdle {
            state.value = QhawaMascotState.ALERT
            message.value = "Revisa las lecturas"
        }
        compose.mainClock.advanceTimeBy(2500)
        mascot().assert(hasPose("Atención"))
        compose.onNodeWithText("Revisa las lecturas").assertIsDisplayed()
        compose.runOnIdle { message.value = "" }
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Revisa las lecturas").assertDoesNotExist()
    }

    @Test fun allFourNavigationTabsOfferContextAndInteractiveTips() {
        compose.setContent {
            QhawaTheme {
                HomeScreen(
                    userName = "Prueba", userEmail = null, onSignOutClick = {},
                    userId = "mascot-test-${System.nanoTime()}", isDemoSession = true,
                    environmentRepository = UnconfiguredEnvironmentRepository(),
                )
            }
        }
        compose.onNodeWithText("Sin conexión MQTT. Conservaré el historial.").assertExists()
        screenshot("01-inicio")
        compose.onNodeWithText("Historial").performClick()
        compose.onNodeWithText("Cuando lleguen lecturas, las exploraremos aquí.").assertExists()
        screenshot("02-historial")
        mascot().performClick()
        compose.onNodeWithText("Prueba el filtro Hoy para ver los registros recientes.").assertExists()
        compose.onNodeWithText("Hábitos").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("¿Listos para completar el hábito de hoy?").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("¿Listos para completar el hábito de hoy?").assertExists()
        screenshot("03-habitos")
        compose.onNodeWithText("Marcar como realizada").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("¡Completado hoy!").fetchSemanticsNodes().isNotEmpty() }
        mascot().performScrollTo().assert(hasPose("Feliz"))
        compose.onNodeWithText("¡Qhawa está feliz! Excelente trabajo.").assertExists()
        screenshot("03-habitos-completado")
        compose.onNodeWithText("Consejos").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Marcar como hecho").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Aquí tienes algunas ideas para hoy.").assertExists()
        screenshot("04-consejos")
        compose.onNodeWithText("Marcar como hecho").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("¡Recomendación completada!").fetchSemanticsNodes().isNotEmpty() }
        mascot().performScrollTo().assert(hasPose("Feliz"))
        compose.onNodeWithText("¡Genial! Cada acción cuenta.").assertExists()
        screenshot("04-consejos-completado")
        compose.onNodeWithText("Inicio").performClick()
        compose.onNodeWithText("Sin conexión MQTT. Conservaré el historial.").assertExists()
    }
}
