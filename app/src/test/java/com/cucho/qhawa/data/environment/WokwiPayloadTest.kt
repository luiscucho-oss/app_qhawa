package com.cucho.qhawa.data.environment

import org.junit.Assert.*
import org.junit.Test

class WokwiPayloadTest {
    private val payload = """{"device_id":"macegato-demo-01","simulated":true,"pollutant":"PM2.5","value":42.3,"unit":"ug/m3","state":"AMARILLO","threshold_profile":"demo_not_health_standard","temperature_c":24.0,"humidity_pct":56.0,"dht_ok":true,"uptime_ms":5007,"sequence":1}"""

    @Test fun mapsExactSketchContractAndUsesReceiptTimeRatherThanUptimeAsDate() {
        val result = parseWokwiPayload(payload, 123456789L)!!
        assertEquals(42.3, result.pm!!, 0.0)
        assertEquals(24.0, result.temperature!!, 0.0)
        assertEquals(56.0, result.humidity!!, 0.0)
        assertEquals("PM2.5", result.pmLabel)
        assertTrue(result.simulated)
        assertEquals("AMARILLO", result.demoState)
        assertEquals(123456789L, result.timestamp)
        assertEquals(5007L, result.uptimeMs)
    }
    @Test fun dhtFailureProducesMissingTemperatureAndHumidity() {
        val result = parseWokwiPayload(payload.replace("\"dht_ok\":true", "\"dht_ok\":false")
            .replace("\"temperature_c\":24.0", "\"temperature_c\":null")
            .replace("\"humidity_pct\":56.0", "\"humidity_pct\":null"), 100)!!
        assertNull(result.temperature)
        assertNull(result.humidity)
        assertNotNull(result.pm)
    }
    @Test fun rejectsRetainedMalformedForeignAndNonSimulatedMessages() {
        assertNull(parseWokwiPayload(payload, 100, retained = true))
        listOf("broken", "{}", payload.replace("macegato-demo-01", "another-device"),
            payload.replace("\"simulated\":true", "\"simulated\":false"),
            payload.replace("ug/m3", "ppm"), payload.replace("PM2.5", "CO2"),
            payload.replace("42.3", "-1"), payload.replace("56.0", "101"),
            payload.replace("24.0", "\"24.0\""), payload.replace("5007", "1.5"),
            payload.replace("42.3", "1e999"), payload.replace("AMARILLO", "OTHER")
        ).forEach { assertNull(it, parseWokwiPayload(it, 100)) }
    }
    @Test fun duplicateIsRejectedButNextReadingAndEspRestartAreAccepted() {
        val gate = WokwiSequenceGate()
        val first = parseWokwiPayload(payload, 100)!!
        assertTrue(gate.accept(first))
        assertFalse(gate.accept(first.copy(timestamp = 200)))
        assertTrue(gate.accept(first.copy(sequence = 2, uptimeMs = 10000)))
        assertFalse(gate.accept(first.copy(sequence = 1, uptimeMs = 10000)))
        assertTrue(gate.accept(first))
    }
}
