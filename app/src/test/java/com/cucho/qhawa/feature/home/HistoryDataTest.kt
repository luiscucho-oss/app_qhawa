package com.cucho.qhawa.feature.home

import com.cucho.qhawa.data.environment.EnvironmentalReading
import kotlinx.coroutines.runBlocking
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
        assertEquals(HistoryWeekSummary(2, 1), historyWeekSummary(records, now))
        assertEquals(HistoryWeekSummary(0, 0), historyWeekSummary(emptyList(), now))
    }

    @Test fun csvPreservesOriginMissingSensorsAndAllRows() = runBlocking {
        val record = reading(now).copy(pm10 = 13.0)
        val physical = StringBuilder()
        physical.writeHistoryCsv(demo = false) { append -> repeat(181) { append(record) } }
        assertTrue(physical.startsWith("\uFEFFfecha_recepcion"))
        assertTrue(physical.contains("pm25_ug_m3,pm10_ug_m3"))
        assertTrue(physical.lines()[1].endsWith(",Prototipo Qhawa MQTT,10.0,13.0,,,VERDE"))
        assertEquals(182, physical.toString().trimEnd().lines().size)

        val demo = StringBuilder()
        demo.writeHistoryCsv(demo = true) { it(record.copy(pm10 = null)) }
        assertTrue(demo.lines()[1].endsWith(",Ejemplo local,10.0,,,,"))
    }
}
