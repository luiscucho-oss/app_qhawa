package com.cucho.qhawa

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.data.environment.*
import com.cucho.qhawa.data.habits.PmAdviceLevel
import com.cucho.qhawa.data.habits.pmAdvice
import com.cucho.qhawa.data.habits.pmAdviceLevel
import com.cucho.qhawa.feature.home.HomeScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Requires the physical publisher and an Android device connected to QHAWA_SENSOR. */
class PrototypeMqttInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun physicalSampleAppearsInHomeIsPersistedAndSelectsAdvice() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("prototypeMqtt") == "true")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val startedAt = System.currentTimeMillis()
        val repository = MqttEnvironmentRepository(context)
        compose.setContent {
            QhawaTheme {
                HomeScreen("Prueba MQTT", null, {}, "prototype-mqtt", true, repository)
            }
        }
        compose.waitUntil(60_000) {
            repository.state.value.freshReading(System.currentTimeMillis())?.let { reading ->
                reading.timestamp >= startedAt && reading.pm != null && reading.pm10 != null &&
                    reading.temperature != null && reading.humidity != null
            } == true
        }
        val sample = checkNotNull(repository.state.value.latest)
        val level = checkNotNull(pmAdviceLevel(sample.pm))
        compose.assertHomeAirQuality(level)
        compose.onNodeWithText("Prototipo en vivo").assertExists()
        compose.onNodeWithText("Material Particulado (PM10)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Material Particulado (PM2.5)").assertExists()
        compose.onNodeWithText(level.label).assertExists()

        val stored = mutableListOf<EnvironmentalReading>()
        runBlocking { repository.forEachReading(startedAt, System.currentTimeMillis()) { stored += it } }
        assertTrue("The live sample was written to SQLite", sample in stored)

        compose.onNodeWithText("Historial").performClick()
        compose.onNodeWithText("Prototipo Qhawa · MQTT").assertExists()
        compose.onNodeWithText("Ver PM10").performScrollTo().performClick()
        compose.onNodeWithText("PM10 hoy").assertExists()
        compose.onNodeWithContentDescription("Gráfico de PM10.").assertExists()

        compose.onNodeWithText("Consejos").performClick()
        compose.waitUntil(10_000) {
            val currentLevel = pmAdviceLevel(repository.state.value.latest?.pm)
            currentLevel != null && compose.onAllNodesWithText(currentLevel.action).fetchSemanticsNodes().size == 4
        }
        val currentLevel = checkNotNull(pmAdviceLevel(repository.state.value.latest?.pm))
        val advice = pmAdvice.getValue(currentLevel).first()
        compose.onNodeWithText(advice.title).performScrollTo().performClick()
        compose.onNodeWithText(advice.description).assertIsDisplayed()
    }
    @Test fun physicalReadingsKeepBubbleBackdropAndMetricInSyncAcrossAllRanges() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("prototypeRanges") == "true")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = MqttEnvironmentRepository(context)
        compose.setContent {
            QhawaTheme { HomeScreen("Prueba MQTT", null, {}, "prototype-ranges", true, repository) }
        }
        val pending = PmAdviceLevel.entries.toMutableSet()
        while (pending.isNotEmpty()) {
            var observed: PmAdviceLevel? = null
            compose.waitUntil(timeoutMillis = 120_000) {
                observed = pmAdviceLevel(repository.state.value.freshPm25Reading(System.currentTimeMillis())?.pm)
                observed in pending
            }
            val level = checkNotNull(observed)
            compose.assertHomeAirQuality(level)
            pending.remove(level)
        }
    }

}
