package com.cucho.qhawa.data.environment

import org.junit.Assert.*
import org.junit.Test

class QhawaSensorPayloadTest {
    private val payload = """{"temperatura":29.6,"humedad":72.0,"pm25":1.1,"pm10":1.3}"""
    private fun parse(json: String, retained: Boolean = false) = parseQhawaSensorPayload(json.toByteArray(), 123L, retained)

    @Test fun parsesTheFourFieldsPublishedByTheFirmwareWithoutRescaling() {
        assertEquals(EnvironmentalReading(123, 1.1, 29.6, 72.0, "PM2.5", 1.3), parse(payload))
    }

    @Test fun preservesAvailableMeasurementsWhenOneSensorSendsItsSentinel() {
        val pmsOnly = parse("""{"temperatura":-1.0,"humedad":-1.0,"pm25":0.0,"pm10":1.3}""")!!
        assertEquals(0.0, pmsOnly.pm!!, 0.0)
        assertEquals(1.3, pmsOnly.pm10!!, 0.0)
        assertNull(pmsOnly.temperature)
        assertNull(pmsOnly.humidity)
        val dhtOnly = parse("""{"temperatura":29.6,"humedad":72.0,"pm25":-1.0,"pm10":-1.0}""")!!
        assertNull(dhtOnly.pm)
        assertNull(dhtOnly.pm10)
        assertEquals(29.6, dhtOnly.temperature!!, 0.0)
    }

    @Test fun rejectsRetainedMalformedAndOutOfRangeMessages() {
        assertNull(parse(payload, retained = true))
        listOf(
            "", "null", "[]", "{}", payload + "{}", payload.replace("1.1", "\"1.1\""),
            payload.replace("1.1", "-2"), payload.replace("1.1", "1e300"),
            payload.replace("72.0", "101"), payload.replace("29.6", "NaN"),
            """{"temperatura":-1,"humedad":-1,"pm25":-1,"pm10":-1}""",
        ).forEach { assertNull(it, parse(it)) }
    }
}
