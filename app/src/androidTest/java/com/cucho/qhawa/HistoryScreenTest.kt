package com.cucho.qhawa

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.data.environment.*
import com.cucho.qhawa.feature.home.HomeScreen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun screenshot(name: String) {
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "history-evidence")
        folder.mkdirs()
        File(folder, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun emptyMqttThenNewReadingsUpdateHistoryWithoutChangingScreens() {
        val flow = MutableStateFlow(EnvironmentState(connection = DeviceConnection.WAITING, brokerConnected = true))
        val repository = object : EnvironmentRepository { override val state = flow }
        compose.setContent {
            QhawaTheme { HomeScreen("Prueba", null, {}, "history-test", true, repository) }
        }
        compose.onNodeWithText("Historial").performClick()
        compose.onNodeWithText("Sin lecturas en este período").assertIsDisplayed()
        compose.onNodeWithText("Descargar reporte simple").performScrollTo().assertIsNotEnabled()
        val now = System.currentTimeMillis()
        compose.runOnIdle {
            val records = listOf(
                EnvironmentalReading(now - 6000, 10.0, 24.0, 56.0, "PM2.5", 12.0),
                EnvironmentalReading(now - 1000, 25.0, null, null, "PM2.5", 27.0),
            )
            flow.value = EnvironmentState(DeviceConnection.CONNECTED, records, true, "Recibiendo lecturas del prototipo")
        }
        compose.onNodeWithText("Descargar reporte simple").assertIsEnabled()
        val date = SimpleDateFormat("dd MMM · HH:mm", Locale.forLanguageTag("es")).format(Date(now - 1000))
        val pm25 = String.format(Locale.getDefault(), "%.1f", 25.0)
        val pm10 = String.format(Locale.getDefault(), "%.1f", 27.0)
        compose.onAllNodesWithText(date, substring = true).assertCountEquals(1)
        compose.onNodeWithText("$pm25 µg/m³", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Bajo ≤12").assertExists()
        compose.onNodeWithText("Ver PM10").performScrollTo().performClick()
        compose.onAllNodesWithText(date, substring = true).assertCountEquals(1)
        compose.onNodeWithText("$pm10 µg/m³", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Temperatura").performScrollTo().performClick()
        compose.onNodeWithText("Sin lecturas en este período").assertDoesNotExist()
        compose.onNodeWithText("Temperatura hoy").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Volver a calidad del aire").performClick()
        compose.onNodeWithText("Calidad del aire hoy").assertIsDisplayed()
        compose.onNodeWithContentDescription("Período del historial").performScrollTo()
        screenshot("mqtt-hoy")
        compose.onNodeWithContentDescription("Explorar datos de ejemplo").performClick()
        compose.onNodeWithText("Datos de ejemplo").assertExists()
        compose.onNodeWithText("Semana").performClick()
        compose.onNodeWithText("Calidad del aire").assertExists()
        screenshot("ejemplo-semana")
        compose.onNodeWithText("Mes").performClick()
        compose.onNodeWithContentDescription("Consultar lectura del historial").performScrollTo().assertExists()
        compose.onNodeWithText("Hoy").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Período del historial").performScrollTo()
        screenshot("ejemplo-hoy")
        compose.onNodeWithText("Descargar reporte simple").performScrollTo().assertIsEnabled()
        screenshot("ejemplo-resumen")
    }
}
