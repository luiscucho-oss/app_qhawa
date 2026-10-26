package com.cucho.qhawa.core.notifications

import com.cucho.qhawa.data.environment.*
import org.junit.Assert.*
import org.junit.Test

class EnvironmentalAlertEngineTest {
    private fun sample(time: Long, pm: Double = 10.0, temperature: Double? = 24.0, humidity: Double? = 50.0, led: String? = null) =
        EnvironmentalReading(time, pm, temperature, humidity, "PM2.5", true, time, time,
            led ?: if (pm < 25) "VERDE" else if (pm < 75) "AMARILLO" else "ROJO")
    private fun state(sample: EnvironmentalReading, connection: DeviceConnection = DeviceConnection.CONNECTED) =
        EnvironmentState(connection = connection, readings = listOf(sample), brokerConnected = true)

    @Test fun firstDangerousReadingAlertsImmediatelyButFirstGreenIsQuiet() {
        assertEquals(AlertLevel.CRITICAL, EnvironmentalAlertEngine().evaluate(state(sample(100, 110.0)), 100)!!.level)
        assertNull(EnvironmentalAlertEngine().evaluate(state(sample(100)), 100))
    }
    @Test fun mirrorsExactWokwiBoundariesAndLedRounding() {
        listOf(24.9 to AlertLevel.GOOD, 25.0 to AlertLevel.WARNING, 74.9 to AlertLevel.WARNING, 75.0 to AlertLevel.CRITICAL).forEach { (pm, expected) ->
            val engine = EnvironmentalAlertEngine()
            engine.evaluate(state(sample(1, if (expected == AlertLevel.CRITICAL) 10.0 else 110.0)), 1)
            assertEquals(expected, engine.evaluate(state(sample(2, pm)), 2)!!.level)
        }
        val rounded = EnvironmentalAlertEngine().evaluate(state(sample(1, 25.0, led = "VERDE")), 1)
        assertNull(rounded) // The circuit's LED can still be green after value rounding to 25.0.
    }
    @Test fun ignoresOldDisconnectedAndLocalExamples() {
        val engine = EnvironmentalAlertEngine()
        val red = sample(10, 110.0)
        assertNull(engine.evaluate(state(red), WokwiMqtt.STALE_MS + 11))
        assertNull(engine.evaluate(state(red, DeviceConnection.WAITING), 10))
        assertNull(engine.evaluate(state(red), 10, enabled = false))
        assertNull(engine.evaluate(state(red), 9))
    }
    @Test fun stableLevelsAndRepeatedReadingsDoNotSpam() {
        val engine = EnvironmentalAlertEngine()
        val red = sample(100, 110.0)
        assertNotNull(engine.evaluate(state(red), 100))
        assertNull(engine.evaluate(state(red), 101))
        assertNull(engine.evaluate(state(sample(200, 120.0)), 200))
    }
    @Test fun missingDhtIsWarningRatherThanRecoveryAndRestorationIsExplicit() {
        val engine = EnvironmentalAlertEngine()
        engine.evaluate(state(sample(100)), 100)
        val missing = engine.evaluate(state(sample(200, temperature = null, humidity = null)), 200)!!
        assertEquals(AlertLevel.WARNING, missing.level)
        assertTrue(missing.message.contains("sin lectura"))
        assertFalse(missing.message.contains("mejorado"))
        assertEquals(AlertLevel.GOOD, engine.evaluate(state(sample(300)), 300)!!.level)
    }
    @Test fun sameSeverityHasNewEventIdAndSoundIsRateLimitedExceptEscalation() {
        val engine = EnvironmentalAlertEngine()
        val first = engine.evaluate(state(sample(100, temperature = 29.0)), 100)!!
        val next = engine.evaluate(state(sample(200, temperature = 29.0, humidity = 70.0)), 200)!!
        assertEquals(first.level, next.level)
        assertNotEquals(first.id, next.id)
        assertTrue(first.sound)
        assertFalse(next.sound)
        val critical = engine.evaluate(state(sample(300, 110.0, 29.0, 70.0)), 300)!!
        assertTrue(critical.sound)
    }
}
