package com.cucho.qhawa.data.environment

import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

enum class DeviceConnection { LOADING, DISCONNECTED, WAITING, CONNECTED }

data class EnvironmentalReading(
    val timestamp: Long,
    val pm: Double?,
    val temperature: Double?,
    val humidity: Double?,
    val pmLabel: String = "PM",
    val pm10: Double? = null,
)

data class EnvironmentState(
    val connection: DeviceConnection = DeviceConnection.DISCONNECTED,
    val readings: List<EnvironmentalReading> = emptyList(),
    val brokerConnected: Boolean = false,
    val detail: String = "Sin conexión al dispositivo",
    val lastReceived: EnvironmentalReading? = null,
    val lastPm25: EnvironmentalReading? = null,
) {
    val latest: EnvironmentalReading?
        get() = lastReceived ?: readings.lastOrNull()
}

internal val EnvironmentalReading.hasValidPm25: Boolean
    get() = pmLabel == "PM2.5" && pm != null && pm.isFinite() && pm >= 0

fun EnvironmentState.freshReading(now: Long): EnvironmentalReading? = latest?.takeIf { isFresh(it, now) }

internal fun EnvironmentState.freshPm25Reading(now: Long): EnvironmentalReading? =
    (latest?.takeIf { it.hasValidPm25 } ?: lastPm25?.takeIf { it.hasValidPm25 })?.takeIf { isFresh(it, now) }

private fun EnvironmentState.isFresh(reading: EnvironmentalReading, now: Long): Boolean =
    connection == DeviceConnection.CONNECTED && now - reading.timestamp in 0..QhawaSensorMqtt.STALE_MS

interface EnvironmentRepository {
    val state: StateFlow<EnvironmentState>

    suspend fun forEachReading(from: Long, to: Long, action: (EnvironmentalReading) -> Unit) {
        state.value.readings.filter { it.timestamp in from..to }.forEach(action)
    }
}

fun demoReadings(now: Long): List<EnvironmentalReading> = (0 until 120).map { index ->
    val date = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.HOUR_OF_DAY, -(119 - index) * 6)
    }
    EnvironmentalReading(
        date.timeInMillis, 8.0 + index % 13, 20.0 + index % 8 * 0.5, 45.0 + index % 17,
        pmLabel = "PM2.5", pm10 = 12.0 + index % 18,
    )
}
