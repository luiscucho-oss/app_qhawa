package com.cucho.qhawa.data.environment

import com.google.gson.JsonParser

object WokwiMqtt {
    const val HOST = "test.mosquitto.org"
    const val URI = "tcp://test.mosquitto.org:1883"
    // This topic is intentionally taken from sketch.ino, NOT from the Wokwi URL's ID.
    const val TOPIC = "qhawa/demo/477386567410462721/lecturas"
    const val DEVICE_ID = "macegato-demo-01"
    const val STALE_MS = 20_000L // Sketch publishes every five seconds.
}

/** Reject foreign, malformed, retained or incompatible readings rather than displaying zeros. */
fun parseWokwiPayload(payload: String, receivedAt: Long, retained: Boolean = false): EnvironmentalReading? = runCatching {
    require(!retained && payload.length <= 4096)
    val json = JsonParser.parseString(payload).asJsonObject
    fun string(key: String): String {
        val field = json.get(key)
        require(field != null && field.isJsonPrimitive && field.asJsonPrimitive.isString)
        return field.asString
    }
    fun bool(key: String): Boolean {
        val field = json.get(key)
        require(field != null && field.isJsonPrimitive && field.asJsonPrimitive.isBoolean)
        return field.asBoolean
    }
    fun numeric(key: String): Double {
        val field = json.get(key)
        require(field != null && field.isJsonPrimitive && field.asJsonPrimitive.isNumber)
        return field.asDouble.also { require(it.isFinite()) }
    }
    fun integer(key: String): Long = numeric(key).let { require(it >= 0 && it <= 4_294_967_295.0 && it == it.toLong().toDouble()); it.toLong() }
    require(string("device_id") == WokwiMqtt.DEVICE_ID)
    require(bool("simulated"))
    require(string("pollutant") == "PM2.5" && string("unit") == "ug/m3")
    require(string("threshold_profile") == "demo_not_health_standard")
    val pm = numeric("value").also { require(it in 0.0..150.0) }
    val dhtOk = bool("dht_ok")
    val temperature = if (dhtOk) numeric("temperature_c").also { require(it in -40.0..80.0) } else null
    val humidity = if (dhtOk) numeric("humidity_pct").also { require(it in 0.0..100.0) } else null
    val state = string("state").also { require(it in setOf("VERDE", "AMARILLO", "ROJO")) }
    // The sketch rounds PM to one decimal, so its LED state may differ exactly at a rounded boundary.
    EnvironmentalReading(receivedAt, pm, temperature, humidity, "PM2.5", true,
        integer("sequence"), integer("uptime_ms"), state)
}.getOrNull()

/** QoS duplicates/out-of-order messages do not extend the freshness timer. Allows ESP32 restarts. */
class WokwiSequenceGate {
    private var sequence: Long? = null
    private var uptime: Long? = null
    fun accept(reading: EnvironmentalReading): Boolean {
        val nextSequence = reading.sequence ?: return false
        val nextUptime = reading.uptimeMs ?: return false
        val previousSequence = sequence
        val previousUptime = uptime
        if (previousSequence != null && previousUptime != null) {
            val advancing = nextSequence > previousSequence && nextUptime >= previousUptime
            val restarted = nextSequence < previousSequence && nextUptime < previousUptime
            val uptimeWrapped = nextSequence > previousSequence && previousUptime > 4_294_900_000L && nextUptime < 60_000L
            if (!advancing && !restarted && !uptimeWrapped) return false
        }
        sequence = nextSequence
        uptime = nextUptime
        return true
    }
}
