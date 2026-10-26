package com.cucho.qhawa

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
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
        val folder = File(context.getExternalFilesDir(null), "wellness-evidence").apply { mkdirs() }
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
        val assigned = dailyHabit(now)
        compose.onNodeWithText(assigned.title).assertExists()
        compose.onNodeWithText("Octubre 2026").performScrollTo()
        screenshot("01-calendario-mensual")
        compose.onNodeWithText("Marcar como realizada").performScrollTo().performClick()
        waitText("¡Completado hoy!")
        compose.onNodeWithContentDescription("Qhawa, mascota interactiva").performScrollTo().performClick()
        compose.onNodeWithText("¡Completado hoy!").assertIsNotEnabled()
        screenshot("02-habito-completado")
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        compose.runOnIdle { visible.value = true }
        waitText("¡Completado hoy!")
        compose.onNodeWithText(assigned.title).assertExists()
        compose.onNodeWithText("1 de 7").assertExists()
        compose.onNodeWithText("2 de 31").assertExists()
        val restored = runBlocking { WellnessRepository(context, user).data.first() }
        assertEquals(setOf(yesterday, dateKey(now)), restored.completedDays)
        assertEquals(assigned.id, restored.assignments[dateKey(now)])
        assertEquals(setOf(yesterday), HabitStore(context, user).read())
    }

    @Test fun completedAdviceDisappearsAndCooldownSurvivesNavigationAndReopening() {
        val user = "account:advice-${System.nanoTime()}"
        val tab = mutableIntStateOf(3)
        val visible = mutableStateOf(true)
        val clock = mutableLongStateOf(now)
        compose.setContent {
            QhawaTheme {
                Box(Modifier.fillMaxSize().qhawaScreenBackground()) {
                    if (visible.value) WellnessRoute(tab.intValue, user, null, true, null, {}, clock = { clock.longValue })
                }
            }
        }
        waitText("Marcar como hecho")
        compose.onNodeWithContentDescription("Explorar datos de ejemplo").assertDoesNotExist()
        compose.onNodeWithText("Marcar como hecho").performScrollTo()
        screenshot("03-consejo-colorido")
        compose.onNodeWithText("Marcar como hecho").performClick()
        waitText("¡Recomendación completada!")
        compose.waitUntil(10_000) { compose.onAllNodesWithText(dailyAdvice[0].title).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Marcar como hecho").assertDoesNotExist()
        compose.onNodeWithText("Próximo consejo en 05:00").performScrollTo().assertIsDisplayed()
        screenshot("04-espera-cinco-minutos")
        val before = runBlocking { WellnessRepository(context, user).data.first() }
        assertEquals(now + 300_000, before.adviceAvailableAt)
        compose.runOnIdle { tab.intValue = 2 }
        waitText("Mis hábitos")
        compose.runOnIdle { tab.intValue = 3 }
        waitText("Próximo consejo en 05:00")
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        compose.runOnIdle { visible.value = true }
        waitText("Próximo consejo en 05:00")
        assertEquals(before.adviceAvailableAt, runBlocking { WellnessRepository(context, user).data.first() }.adviceAvailableAt)
        compose.runOnIdle { clock.longValue = now + 299_999 }
        waitText("Próximo consejo en 00:01")
        compose.onNodeWithText("Marcar como hecho").assertDoesNotExist()
        compose.runOnIdle { clock.longValue = now + 300_000 }
        waitText(dailyAdvice[1].title)
        compose.onNodeWithText("Marcar como hecho").assertExists()
        screenshot("05-siguiente-consejo")
    }
}
