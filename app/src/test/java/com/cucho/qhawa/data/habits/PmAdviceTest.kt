package com.cucho.qhawa.data.habits

import com.cucho.qhawa.data.environment.EnvironmentalReading
import org.junit.Assert.*
import org.junit.Test

class PmAdviceTest {
    private fun reading(pm: Double?, label: String = "PM2.5") =
        EnvironmentalReading(0, pm, null, null, pmLabel = label, simulated = true, demoState = "ROJO")

    @Test fun classifiesExactBoundariesUsingNumericValueRatherThanFirmwareState() {
        listOf(
            0.0 to PmAdviceLevel.GREEN,
            24.999 to PmAdviceLevel.GREEN,
            25.0 to PmAdviceLevel.YELLOW,
            25.001 to PmAdviceLevel.YELLOW,
            74.999 to PmAdviceLevel.YELLOW,
            75.0 to PmAdviceLevel.RED,
            75.001 to PmAdviceLevel.RED,
            150.0 to PmAdviceLevel.RED,
        ).forEach { (pm, expected) ->
            assertEquals(expected, pmAdviceLevel(pm))
            val selected = selectPmAdvice(reading(pm), demo = false)
            assertEquals(expected, selected.level)
            assertEquals(pmAdvice.getValue(expected), selected.advice)
            assertEquals(4, selected.advice.size)
        }
    }

    @Test fun missingInvalidDemoAndOtherPollutantReadingsHaveGeneralAdviceWithoutLevel() {
        val invalid = listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
        invalid.forEach { pm ->
            assertNull(pmAdviceLevel(pm))
            assertEquals(AdviceSelection(null, dailyAdvice.take(4)), selectPmAdvice(reading(pm), false))
        }
        assertNull(selectPmAdvice(null, false).level)
        assertNull(selectPmAdvice(reading(90.0), true).level)
        assertNull(selectPmAdvice(reading(90.0, "PM10"), false).level)
        assertNull(selectPmAdvice(reading(90.0, "PM"), false).level)
    }

    @Test fun changingLevelsReplacesAllFourAdviceAndReturningRestoresTheSameSet() {
        val levels = listOf(10.0, 25.0, 75.0, 10.0).map { selectPmAdvice(reading(it), false) }
        assertEquals(levels.first(), levels.last())
        val ids = levels.take(3).flatMap { it.advice }.map { it.id }
        assertEquals(12, ids.distinct().size)
    }
}
