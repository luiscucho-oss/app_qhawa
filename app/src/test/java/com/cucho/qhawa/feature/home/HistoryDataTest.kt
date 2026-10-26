package com.cucho.qhawa.feature.home

import com.cucho.qhawa.data.environment.EnvironmentalReading
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class HistoryDataTest {
    private val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 25, 15, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    private fun reading(time: Long, pm: Double? = 10.0) = EnvironmentalReading(time, pm, null, null)

    @Test fun periodsUseCalendarDaysExcludeFutureAndKeepReadingsSorted() {
        val start = historyDayStart(now, 6)
        val records = listOf(reading(now), reading(start - 1), reading(start), reading(now + 1))
        assertEquals(listOf(start, now), historyReadings(records, HistoryPeriod.WEEK, now).map { it.timestamp })
        assertEquals(listOf(now), historyReadings(records, HistoryPeriod.TODAY, now).map { it.timestamp })
        assertEquals(3, historyReadings(records, HistoryPeriod.MONTH, now).size)
    }

    @Test fun summaryCountsOnlyMeasuredDaysAndUsesDailyAverages() {
        val yesterday = historyDayStart(now, 1) + 1000
        val records = listOf(reading(now, 5.0), reading(now - 1000, 45.0), reading(yesterday, 10.0), reading(historyDayStart(now, 2), null))
        assertEquals(HistoryWeekSummary(2, 1), historyWeekSummary(records, now, wokwi = true))
        assertEquals(HistoryWeekSummary(0, 0), historyWeekSummary(emptyList(), now, wokwi = false))
    }

    @Test fun wokwiKeepsItsDemonstrationThresholdsAndMissingIsNotGood() {
        assertEquals(0, historyPmBand(24.9, true))
        assertEquals(1, historyPmBand(25.0, true))
        assertEquals(2, historyPmBand(75.0, true))
        assertEquals(1, historyPmBand(19.0, false))
        assertNull(historyPmBand(null, true))
    }

    @Test fun csvPreservesOriginMissingSensorsAndAllRows() {
        val record = reading(now).copy(pmLabel = "PM,\"2.5\"", simulated = true, demoState = "VERDE")
        val csv = historyCsv(listOf(record), false)
        assertTrue(csv.startsWith("\uFEFFfecha_recepcion"))
        assertTrue(csv.contains("\"Simulación Wokwi MQTT\""))
        assertTrue(csv.contains("\"PM,\"\"2.5\"\"\""))
        assertTrue(csv.contains(",10.0,,,\"VERDE\""))
        assertEquals(182, historyCsv(List(181) { record }, true).trimEnd().lines().size)
        assertTrue(historyCsv(listOf(record.copy(pmLabel = "=1+1")), true).contains("\"'=1+1\""))
    }
}
