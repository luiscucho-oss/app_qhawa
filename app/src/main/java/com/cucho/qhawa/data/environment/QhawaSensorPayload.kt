package com.cucho.qhawa.data.environment

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.Strictness

internal object QhawaSensorMqtt {
    const val HOST = "192.168.4.2"
    const val URI = "tcp://$HOST:1883"
    const val TOPIC = "qhawa/sensor"
    const val WIFI = "QHAWA_SENSOR"
    const val STALE_MS = 20_000L
}

private val sensorJson = GsonBuilder().setStrictness(Strictness.STRICT).create()

/** The firmware sends -1 for an unavailable measurement and omits time metadata. */
internal fun parseQhawaSensorPayload(payload: ByteArray, receivedAt: Long, retained: Boolean): EnvironmentalReading? {
    if (retained || payload.size !in 1..1024) return null
    return runCatching {
        val json = sensorJson.fromJson(payload.toString(Charsets.UTF_8), JsonObject::class.java)
        fun measurement(key: String, range: ClosedFloatingPointRange<Double>): Double? {
            val value = json.get(key)
            require(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isNumber)
            val number = value.asDouble
            if (number == -1.0) return null
            require(number in range)
            return number
        }
        val pm25 = measurement("pm25", 0.0..6553.5)
        val pm10 = measurement("pm10", 0.0..6553.5)
        val temperature = measurement("temperatura", -40.0..80.0)
        val humidity = measurement("humedad", 0.0..100.0)
        if (pm25 == null && pm10 == null && temperature == null && humidity == null) return null
        EnvironmentalReading(receivedAt, pm25, temperature, humidity, pmLabel = "PM2.5", pm10 = pm10)
    }.getOrNull()
}
