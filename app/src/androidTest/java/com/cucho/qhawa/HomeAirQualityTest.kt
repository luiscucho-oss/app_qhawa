package com.cucho.qhawa

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.data.environment.DeviceConnection
import com.cucho.qhawa.data.environment.EnvironmentRepository
import com.cucho.qhawa.data.environment.EnvironmentState
import com.cucho.qhawa.data.environment.EnvironmentalReading
import com.cucho.qhawa.data.habits.PmAdviceLevel
import com.cucho.qhawa.feature.home.HomeScreen
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeAirQualityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun liveLevelsUpdateBubbleBackdropAndPmMetricAndTappingKeepsTheLevelVisible() {
        val source = MutableStateFlow(EnvironmentState())
        val repository = object : EnvironmentRepository { override val state = source }
        compose.setContent {
            QhawaTheme { HomeScreen("Prueba", null, {}, "air-quality-test", true, repository) }
        }
        compose.assertHomeAirQuality(null)
        compose.onNodeWithText("Sesión local").assertIsDisplayed()
        compose.onNodeWithText("Prototipo Qhawa · MQTT").assertIsDisplayed()
        compose.onNodeWithContentDescription("Explorar datos de ejemplo").assertIsOff()
        compose.onNodeWithText("Conoce el ambiente de tu hogar.").assertDoesNotExist()
        compose.onNodeWithText("01 · MEDIR").assertDoesNotExist()

        val ranges = listOf(12.0 to PmAdviceLevel.GREEN, 35.0 to PmAdviceLevel.YELLOW, 35.1 to PmAdviceLevel.RED)
        for ((pm, level) in ranges) {
            compose.runOnIdle {
                val sample = EnvironmentalReading(System.currentTimeMillis(), pm, 24.0, 55.0, "PM2.5")
                source.value = EnvironmentState(DeviceConnection.CONNECTED, lastReceived = sample, lastPm25 = sample)
            }
            compose.assertHomeAirQuality(level)
        }
        compose.onNodeWithContentDescription("Qhawa, mascota interactiva").performClick()
        compose.onNodeWithText("Toca Historial para explorar las lecturas.").assertIsDisplayed()
        compose.assertHomeAirQuality(PmAdviceLevel.RED)

        compose.runOnIdle { source.value = source.value.copy(connection = DeviceConnection.WAITING) }
        compose.assertHomeAirQuality(null)
    }
}

internal fun ComposeTestRule.assertHomeAirQuality(level: PmAdviceLevel?) {
    val (message, background, textColor) = when (level) {
        PmAdviceLevel.GREEN -> Triple("Calidad del aire: buena", Color(0xFFDFF3E8), Color(0xFF135A40))
        PmAdviceLevel.YELLOW -> Triple("Calidad del aire: moderada", Color(0xFFFFE7CD), Color(0xFF805400))
        PmAdviceLevel.RED -> Triple("Calidad del aire: mala", Color(0xFFFCE2E0), Color(0xFF8E2521))
        null -> Triple("Esperando datos del sensor", Color(0xFFECEFF3), Color(0xFF162B29))
    }
    onNodeWithTag("home-air-quality").performScrollTo()
    val text = onNodeWithText(message).assertIsDisplayed().captureToImage().toPixelMap()
    val expectedTextColor = textColor.toArgb()
    assertTrue((0 until text.height).any { y -> (0 until text.width).any { x -> text[x, y].toArgb() == expectedTextColor } })
    onNodeWithText(level?.label ?: "Sin clasificar").assertExists()
    val section = onNodeWithTag("home-air-quality").captureToImage().toPixelMap()
    assertEquals(background.toArgb(), section[section.width / 2, 3].toArgb())
}
