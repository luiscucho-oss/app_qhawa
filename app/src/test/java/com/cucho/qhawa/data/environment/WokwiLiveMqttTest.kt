package com.cucho.qhawa.data.environment

import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Opt-in integration check. Receives the running Wokwi sketch; it never publishes fabricated data. */
class WokwiLiveMqttTest {
    @Test fun receivesThreeConsecutiveReadingsFromRunningWokwi() {
        assumeTrue("Run with -Pqhawa.liveMqtt=true while Wokwi is running", java.lang.Boolean.getBoolean("qhawa.liveMqtt"))
        val received = LinkedBlockingQueue<Pair<String, EnvironmentalReading>>()
        val client = MqttClient(WokwiMqtt.URI, "qhawa-test-${UUID.randomUUID()}", MemoryPersistence())
        try {
            client.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) = Unit
                override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
                override fun messageArrived(topic: String, message: MqttMessage) {
                    if (topic != WokwiMqtt.TOPIC) return
                    val payload = message.payload.toString(Charsets.UTF_8)
                    parseWokwiPayload(payload, System.currentTimeMillis(), message.isRetained)?.let { received.offer(payload to it) }
                }
            })
            client.connect(MqttConnectOptions().apply { connectionTimeout = 15; keepAliveInterval = 15; isCleanSession = true })
            client.subscribe(WokwiMqtt.TOPIC, 0)
            val result = (1..3).map { received.poll(30, TimeUnit.SECONDS) ?: error("No valid Wokwi reading arrived within 30 seconds") }
            assertTrue(result.zipWithNext().all { (a, b) -> b.second.sequence!! > a.second.sequence!! })
            assertTrue(result.all { it.second.simulated && it.second.pmLabel == "PM2.5" })
            val minPm = (System.getProperty("qhawa.expectedPmMin") ?: "0").toDouble()
            assertTrue("Potentiometer change must reach MQTT", result.all { it.second.pm!! >= minPm })
            System.getProperty("qhawa.expectedTemperature")?.toDoubleOrNull()?.let { expected ->
                result.forEach { assertEquals(expected, it.second.temperature!!, 0.1) }
            }
            System.getProperty("qhawa.expectedHumidity")?.toDoubleOrNull()?.let { expected ->
                result.forEach { assertEquals(expected, it.second.humidity!!, 0.1) }
            }
            File("build/reports/mqtt-live-readings.jsonl").apply { parentFile?.mkdirs(); writeText(result.joinToString("\n") { it.first } + "\n") }
        } finally {
            runCatching { client.disconnectForcibly(0, 0, false) }
            runCatching { client.close(true) }
        }
    }
}
