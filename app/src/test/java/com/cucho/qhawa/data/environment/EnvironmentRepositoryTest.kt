package com.cucho.qhawa.data.environment

import org.junit.Assert.*
import org.junit.Test

class EnvironmentRepositoryTest {
    @Test fun startsDisconnectedWithoutFabricatedMeasurements() {
        val state = EnvironmentState()
        assertEquals(DeviceConnection.DISCONNECTED, state.connection)
        assertTrue(state.readings.isEmpty())
    }
    @Test fun mostRecentValidPm25IsAvailableUntilItExpiresEvenAfterAPartialSample() {
        val valid = EnvironmentalReading(10_000, 12.0, 24.0, 56.0, "PM2.5")
        val partial = valid.copy(timestamp = 12_000, pm = null)
        val state = EnvironmentState(DeviceConnection.CONNECTED, lastReceived = partial, lastPm25 = valid)

        assertEquals(valid, state.freshPm25Reading(12_000))
        assertNull(state.freshPm25Reading(valid.timestamp + QhawaSensorMqtt.STALE_MS + 1))
        assertNull(state.copy(connection = DeviceConnection.WAITING).freshPm25Reading(12_000))
        assertNull(state.copy(lastPm25 = valid.copy(pmLabel = "PM10")).freshPm25Reading(12_000))
        assertEquals(partial, state.freshReading(12_000))
    }

    @Test fun demoIsChronologicalAndNeverContainsFutureReadings() {
        val now = 1_790_000_000_000L
        val readings = demoReadings(now)
        assertEquals(120, readings.size)
        assertTrue(readings.zipWithNext().all { (a, b) -> a.timestamp < b.timestamp })
        assertEquals(now, readings.last().timestamp)
        assertTrue(readings.all { it.humidity!! in 0.0..100.0 && it.pm!! >= 0 })
    }
}
