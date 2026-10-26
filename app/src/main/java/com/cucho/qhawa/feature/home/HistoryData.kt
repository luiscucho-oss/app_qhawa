package com.cucho.qhawa.feature.home

import com.cucho.qhawa.data.environment.EnvironmentalReading
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

/** Rangos de visualización: Wokwi conserva su perfil demostrativo, sin inferir salud. */
internal fun historyPmBand(pm: Double?, wokwi: Boolean): Int? = pm?.takeIf { it.isFinite() }?.let {
    when {
        if (wokwi) it < 25 else it <= 12 -> 0
        if (wokwi) it < 75 else it <= 35 -> 1
        else -> 2
    }
}

internal data class HistoryWeekSummary(val recordedDays: Int, val lowDays: Int)

internal fun historyWeekSummary(readings: List<EnvironmentalReading>, now: Long, wokwi: Boolean): HistoryWeekSummary {
    val days = historyReadings(readings, HistoryPeriod.WEEK, now)
        .filter { it.pm?.isFinite() == true }
        .groupBy { historyDayStart(it.timestamp) }
        .values
    return HistoryWeekSummary(days.size, days.count { day -> historyPmBand(day.map { it.pm!! }.average(), wokwi) == 0 })
}

/** Exporta todas las lecturas del período, no los puntos reducidos para dibujar. */
internal fun historyCsv(readings: List<EnvironmentalReading>, demo: Boolean): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT)
    fun text(value: String): String {
        val safe = if (value.firstOrNull() in listOf('=', '+', '-', '@')) "'$value" else value
        return "\"${safe.replace("\"", "\"\"")}\""
    }
    return buildString {
        append('\uFEFF')
        append("fecha_recepcion,origen,variable_pm,pm_ug_m3,temperatura_c,humedad_pct,estado_led\r\n")
        readings.sortedBy { it.timestamp }.forEach { reading ->
            append(listOf(
                text(dateFormat.format(Date(reading.timestamp))),
                text(if (demo) "Ejemplo local" else if (reading.simulated) "Simulación Wokwi MQTT" else "Dispositivo"),
                text(reading.pmLabel), reading.pm?.toString().orEmpty(),
                reading.temperature?.toString().orEmpty(), reading.humidity?.toString().orEmpty(),
                text(reading.demoState.orEmpty()),
            ).joinToString(","))
            append("\r\n")
        }
    }
}
