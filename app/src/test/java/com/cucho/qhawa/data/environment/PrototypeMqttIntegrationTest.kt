package com.cucho.qhawa.data.environment

import com.cucho.qhawa.data.habits.PmAdviceLevel
import com.cucho.qhawa.data.habits.selectPmAdvice
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test

class PrototypeMqttIntegrationTest {
    @Test fun mosquittoDeliversFirmwareJsonToLatestReadingHistoryAndAdvice() = runBlocking {
        val port = System.getProperty("qhawa.testMqttPort")?.toIntOrNull()
        assumeNotNull(port)
        val uri = "tcp://127.0.0.1:$port"
        val job = SupervisorJob()
        val publisher = MqttClient(uri, "qhawa-test-${UUID.randomUUID()}", MemoryPersistence())
        val history = InMemoryHistory()
        val repository = MqttEnvironmentRepository(history, CoroutineScope(job + Dispatchers.IO), uri) { null }

        try {
            publisher.connect()
            publisher.publish(QhawaSensorMqtt.TOPIC, payload(90.0), 0, true)
            val collector = launch { repository.state.collect {} }
            withTimeout(10_000) { repository.state.first { it.connection == DeviceConnection.WAITING } }
            delay(300)
            assertNull(repository.state.value.latest)
            publisher.publish(QhawaSensorMqtt.TOPIC, """{"temperatura":29.6,"pm25":90}""".toByteArray(), 0, false)
            delay(200)
            assertTrue(history.load(0).isEmpty())

            val expected = mapOf(
                12.0 to PmAdviceLevel.GREEN, 12.1 to PmAdviceLevel.YELLOW,
                35.0 to PmAdviceLevel.YELLOW, 35.1 to PmAdviceLevel.RED,
            )
            expected.forEach { (pm25, level) ->
                publisher.publish(QhawaSensorMqtt.TOPIC, payload(pm25), 0, false)
                val state = withTimeout(5000) { repository.state.first { it.latest?.pm == pm25 } }
                val current = state.freshReading(System.currentTimeMillis())
                assertNotNull(current)
                assertEquals(current, state.readings.last())
                assertEquals(40.0, current!!.pm10!!, 0.0)
                assertEquals(29.6, current.temperature!!, 0.0)
                assertEquals(72.0, current.humidity!!, 0.0)
                assertEquals(level, selectPmAdvice(current, demo = false).level)
            }
            val exported = mutableListOf<Double?>()
            repository.forEachReading(0, Long.MAX_VALUE) { exported += it.pm }
            assertEquals(expected.keys.toList(), exported)
            collector.cancelAndJoin()
        } finally {
            job.cancelAndJoin()
            if (publisher.isConnected) {
                publisher.publish(QhawaSensorMqtt.TOPIC, byteArrayOf(), 0, true)
                publisher.disconnect()
            }
            publisher.close()
        }
    }

    private fun payload(pm25: Double) =
        """{"temperatura":29.6,"humedad":72.0,"pm25":$pm25,"pm10":40.0}""".toByteArray()

    private class InMemoryHistory : ReadingHistory {
        private val samples = mutableListOf<EnvironmentalReading>()

        @Synchronized override fun save(reading: EnvironmentalReading) {
            samples += reading
        }

        @Synchronized override fun load(now: Long) = samples.toList()

        override fun forEach(from: Long, to: Long, action: (EnvironmentalReading) -> Unit) {
            load(to).filter { it.timestamp in from..to }.forEach(action)
        }
    }
}
