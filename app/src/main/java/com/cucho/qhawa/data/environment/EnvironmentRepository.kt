package com.cucho.qhawa.data.environment

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

enum class DeviceConnection { LOADING, DISCONNECTED, WAITING, CONNECTED }

/** PM must be named by the eventual sensor adapter (PM1, PM2.5, PM10…). No assumed sensor. */
data class EnvironmentalReading(
    val timestamp: Long,
    val pm: Double?,
    val temperature: Double?,
    val humidity: Double?,
    val pmLabel: String = "PM",
    val simulated: Boolean = false,
    val sequence: Long? = null,
    val uptimeMs: Long? = null,
    val demoState: String? = null,

)

data class EnvironmentState(
    val connection: DeviceConnection = DeviceConnection.DISCONNECTED,
    val readings: List<EnvironmentalReading> = emptyList(),
    val brokerConnected: Boolean = false,
    val detail: String = "Sin conexión al dispositivo",
    val source: String = "Dispositivo",

)

interface EnvironmentRepository {
    val state: StateFlow<EnvironmentState>
}

/** Replace through AppContainer when the physical device's documented protocol is available. */
class UnconfiguredEnvironmentRepository : EnvironmentRepository {
    override val state: StateFlow<EnvironmentState> = MutableStateFlow(EnvironmentState())
}

/** Synthetic fixtures only. Never emitted by the physical repository. */
fun demoReadings(now: Long): List<EnvironmentalReading> = (0 until 120).map { index ->
    val date = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.HOUR_OF_DAY, -(119 - index) * 6)
    }
    EnvironmentalReading(date.timeInMillis, 8.0 + index % 13, 20.0 + index % 8 * 0.5, 45.0 + index % 17)
}
