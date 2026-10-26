package com.cucho.qhawa.data.environment

import org.junit.Assert.*
import org.junit.Test

class EnvironmentRepositoryTest {
    @Test fun startsDisconnectedWithoutFabricatedMeasurements() {
        val state = UnconfiguredEnvironmentRepository().state.value
        assertEquals(DeviceConnection.DISCONNECTED, state.connection)
        assertTrue(state.readings.isEmpty())
    }
    @Test fun demoIsChronologicalAndNeverContainsFutureReadings() {
        val now = 1_790_000_000_000L
        val readings = demoReadings(now)
        assertEquals(120, readings.size)
        assertTrue(readings.zipWithNext().all { (a, b) -> a.timestamp < b.timestamp })
        assertEquals(now, readings.last().timestamp)
        assertTrue(readings.all { it.humidity!! in 0.0..100.0 && it.pm!! >= 0 })
        assertTrue(UnconfiguredEnvironmentRepository().state.value.readings.isEmpty())
    }
}
