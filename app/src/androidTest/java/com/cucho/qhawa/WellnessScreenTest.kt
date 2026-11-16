package com.cucho.qhawa

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.core.designsystem.theme.qhawaScreenBackground
import com.cucho.qhawa.data.habits.*
import com.cucho.qhawa.feature.home.WellnessRoute
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WellnessScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 26, 12, 0) }.timeInMillis

    private fun waitText(text: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun screenshot(name: String) {
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val folder = File(context.filesDir, "wellness-evidence").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun monthlyCalendarMigratesHistoryLocksFutureAndKeepsCompletionAfterReopening() {
        val user = "demo:calendar-${System.nanoTime()}"
        val yesterday = dateKey(dayBefore(now, 1))
        HabitStore(context, user).complete(dayBefore(now, 1), "review")
        val visible = mutableStateOf(true)
        compose.setContent {
            QhawaTheme {
                Box(Modifier.fillMaxSize().qhawaScreenBackground()) {
                    if (visible.value) WellnessRoute(2, user, null, true, null, {}, clock = { now })
                }
            }
        }
        waitText("Mis hábitos")
        compose.onNodeWithContentDescription("Explorar datos de ejemplo").assertDoesNotExist()
        compose.onNodeWithText("02 · Gamificación").assertDoesNotExist()
        compose.onNodeWithContentDescription("Día 2026-10-27").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Día $yesterday")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Completado"))
        compose.onNodeWithContentDescription("Fuego de la racha")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Encendido: pendiente completar hoy"))
        screenshot("00-racha-dinamica")
        val assigned = dailyHabit(now)
        compose.onNodeWithText(assigned.title).assertExists()
        compose.onNodeWithText("Ver detalle").performScrollTo().performClick()
        compose.onNodeWithText(assigned.description).assertExists()
        compose.onNodeWithText("Menos detalle").performScrollTo().performClick()
        compose.onNodeWithText(assigned.description).assertDoesNotExist()
        compose.onNodeWithText("Octubre 2026").performScrollTo()
        screenshot("01-calendario-mensual")
        compose.onNodeWithText("Completar").performScrollTo().performClick()
        waitText("¡Completado hoy!")
        compose.onNodeWithContentDescription("Fuego de la racha")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Encendido: hábito de hoy completado"))
        compose.onNodeWithContentDescription("Qhawa, mascota interactiva").performScrollTo().performClick()
        compose.onNodeWithText("¡Completado hoy!").assertIsNotEnabled()
        screenshot("02-habito-completado")
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        compose.runOnIdle { visible.value = true }
        waitText("¡Completado hoy!")
        compose.onNodeWithText(assigned.title).assertExists()
        compose.onAllNodesWithText("1 de 7").assertCountEquals(2)
        compose.onNodeWithText("2 de 31").assertExists()
        val restored = runBlocking { WellnessRepository(context, user).data.first() }
        assertEquals(setOf(yesterday, dateKey(now)), restored.completedDays)
        assertEquals(assigned.id, restored.assignments[dateKey(now)])
        assertEquals(setOf(yesterday), HabitStore(context, user).read())
    }

    @Test fun adviceCardsOpenInstructionsAndTwoActionsCanBeRecordedImmediately() {
        val user = "account:advice-${System.nanoTime()}"
        val visible = mutableStateOf(true)
        compose.setContent {
            QhawaTheme {
                Box(Modifier.fillMaxSize().qhawaScreenBackground()) {
                    if (visible.value) WellnessRoute(3, user, null, true, null, {}, clock = { now })
                }
            }
        }
        waitText(dailyAdvice.first().title)
        compose.onAllNodesWithText("Ver qué hacer").assertCountEquals(4)
        compose.onNodeWithText("Marcar como hecho").assertDoesNotExist()
        val first = dailyAdvice[0]
        val second = dailyAdvice[1]
        listOf(first, second).forEach { advice ->
            compose.onNodeWithText(advice.description).assertDoesNotExist()
            compose.onNodeWithText(advice.title).performScrollTo().performClick()
            waitText(advice.description)
            compose.onNodeWithText("Marcar como hecho").performScrollTo().assertIsEnabled().performClick()
            compose.waitUntil(10_000) {
                val progress = runBlocking { WellnessRepository(context, user).data.first() }
                "$now|${advice.id}" in progress.adviceCompletions
            }
        }
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        compose.runOnIdle { visible.value = true }
        waitText(first.title)
        val restored = runBlocking { WellnessRepository(context, user).data.first() }
        assertEquals(setOf("$now|${first.id}", "$now|${second.id}"), restored.adviceCompletions)
    }
}
