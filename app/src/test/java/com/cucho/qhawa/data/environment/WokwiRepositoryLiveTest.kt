package com.cucho.qhawa.data.environment

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Tests the actual repository consumed by Compose, not a separate MQTT implementation. */
class WokwiRepositoryLiveTest {
    @Test fun appRepositoryReceivesLiveReadingsAndWritesHistory() = runBlocking {
        assumeTrue(java.lang.Boolean.getBoolean("qhawa.liveMqtt"))
        val saved = java.util.concurrent.CopyOnWriteArrayList<EnvironmentalReading>()
        val history = object : ReadingHistory {
            override fun load(now: Long) = emptyList<EnvironmentalReading>()
            override fun save(reading: EnvironmentalReading) { saved.add(reading) }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repository = MqttEnvironmentRepository(history, scope)
            val readings = withTimeout(60_000) {
                repository.state.filter { it.connection == DeviceConnection.CONNECTED && it.brokerConnected }
                    .mapNotNull { it.readings.lastOrNull() }.distinctUntilChangedBy { it.sequence }.take(3).toList()
            }
            assertEquals(3, readings.size)
            assertTrue(saved.size >= 3)
            assertTrue(readings.zipWithNext().all { (a, b) -> b.sequence!! > a.sequence!! })
            val minimumPm = (System.getProperty("qhawa.expectedPmMin") ?: "0").toDouble()
            readings.forEach {
                assertTrue(it.simulated)
                assertEquals("PM2.5", it.pmLabel)
                assertTrue(it.pm!! >= minimumPm)
                System.getProperty("qhawa.expectedTemperature")?.toDoubleOrNull()?.let { expected -> assertEquals(expected, it.temperature!!, 0.1) }
                System.getProperty("qhawa.expectedHumidity")?.toDoubleOrNull()?.let { expected -> assertEquals(expected, it.humidity!!, 0.1) }
            }
        } finally { scope.cancel() }
    }
    @Test fun pausedSimulationBecomesWaitingAndResumesWithoutRestartingApp() = runBlocking {
        assumeTrue(java.lang.Boolean.getBoolean("qhawa.pauseTest"))
        val history = object : ReadingHistory {
            override fun load(now: Long) = emptyList<EnvironmentalReading>()
            override fun save(reading: EnvironmentalReading) = Unit
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val phase = java.io.File("build/reports/mqtt-pause-phase.txt")
        phase.parentFile?.mkdirs()
        try {
            val repository = MqttEnvironmentRepository(history, scope)
            val observer = launch { repository.state.collect() }
            val initial = withTimeout(60_000) { repository.state.first { it.connection == DeviceConnection.CONNECTED } }
            phase.writeText("READY_TO_PAUSE")
            val waiting = withTimeout(60_000) { repository.state.first {
                it.connection == DeviceConnection.WAITING && it.brokerConnected && it.detail.contains("20 s")
            } }
            assertTrue(waiting.readings.isNotEmpty())
            phase.writeText("WAITING_CONFIRMED")
            val resumed = withTimeout(60_000) { repository.state.first { it.connection == DeviceConnection.CONNECTED } }
            assertTrue(resumed.readings.last().timestamp > initial.readings.last().timestamp)
            phase.writeText("RECOVERED_WITHOUT_APP_RESTART")
            observer.cancel()
        } finally { scope.cancel() }
    }

}
