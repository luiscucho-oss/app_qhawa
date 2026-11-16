package com.cucho.qhawa.feature.home

import com.cucho.qhawa.data.environment.EnvironmentalReading
import com.cucho.qhawa.data.habits.pmAdviceLevel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal enum class HistoryPeriod(val title: String, val days: Int) {
    TODAY("Hoy", 1), WEEK("Semana", 7), MONTH("Mes", 30),
}

internal fun historyDayStart(now: Long, daysAgo: Int = 0): Long = Calendar.getInstance().apply {
    timeInMillis = now
    add(Calendar.DAY_OF_YEAR, -daysAgo)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

internal fun historyReadings(readings: List<EnvironmentalReading>, period: HistoryPeriod, now: Long) =
    readings.filter { it.timestamp in historyDayStart(now, period.days - 1)..now }.sortedBy { it.timestamp }

internal data class HistoryWeekSummary(val recordedDays: Int, val lowDays: Int)

internal fun historyWeekSummary(readings: List<EnvironmentalReading>, now: Long): HistoryWeekSummary {
    val days = historyReadings(readings, HistoryPeriod.WEEK, now)
        .filter { it.pm?.isFinite() == true }
        .groupBy { historyDayStart(it.timestamp) }
        .values
    return HistoryWeekSummary(days.size, days.count { day -> pmAdviceLevel(day.map { it.pm!! }.average())?.ordinal == 0 })
}

internal suspend fun Appendable.writeHistoryCsv(
    demo: Boolean,
    forEachReading: suspend ((EnvironmentalReading) -> Unit) -> Unit,
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ROOT)
    append('\uFEFF')
    append("fecha_recepcion,origen,pm25_ug_m3,pm10_ug_m3,temperatura_c,humedad_pct,nivel_pm25\r\n")
    forEachReading { reading ->
        append(listOf(
            dateFormat.format(Date(reading.timestamp)),
            if (demo) "Ejemplo local" else "Prototipo Qhawa MQTT",
            reading.pm?.toString().orEmpty(),
            reading.pm10?.toString().orEmpty(),
            reading.temperature?.toString().orEmpty(),
            reading.humidity?.toString().orEmpty(),
            if (demo) "" else pmAdviceLevel(reading.pm)?.label.orEmpty(),
        ).joinToString(","))
        append("\r\n")
    }
}
