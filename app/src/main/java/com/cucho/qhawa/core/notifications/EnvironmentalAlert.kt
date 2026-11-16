package com.cucho.qhawa.core.notifications

import com.cucho.qhawa.data.environment.EnvironmentState
import com.cucho.qhawa.data.environment.freshReading
import com.cucho.qhawa.data.habits.PmAdviceLevel
import com.cucho.qhawa.data.habits.pmAdviceLevel

enum class AlertLevel(val color: Long) { GOOD(0xFF246540), WARNING(0xFF805400), CRITICAL(0xFFB3261E) }
data class EnvironmentalAlert(val id: Long, val level: AlertLevel, val title: String, val message: String, val sound: Boolean)
private data class Levels(val pm: AlertLevel?, val temperature: AlertLevel?, val humidity: AlertLevel?)

/** PM2.5 follows the prototype; temperature and humidity use the app’s comfort ranges. */
class EnvironmentalAlertEngine {
    private var previous: Levels? = null
    private var lastSample: Long? = null
    private var nextId = 0L
    private var lastSoundAt: Long? = null
    private var lastSoundLevel: AlertLevel? = null

    fun evaluate(state: EnvironmentState, now: Long, enabled: Boolean = true): EnvironmentalAlert? {
        val sample = state.freshReading(now)
        if (!enabled || sample == null) {
            previous = null
            lastSample = null
            return null
        }
        if (lastSample == sample.timestamp) return null
        lastSample = sample.timestamp
        val current = Levels(
            when (pmAdviceLevel(sample.pm.takeIf { sample.pmLabel == "PM2.5" })) {
                PmAdviceLevel.GREEN -> AlertLevel.GOOD
                PmAdviceLevel.YELLOW -> AlertLevel.WARNING
                PmAdviceLevel.RED -> AlertLevel.CRITICAL
                null -> null
            },
            sample.temperature?.let { if (it in 18.0..25.0) AlertLevel.GOOD else AlertLevel.WARNING },
            sample.humidity?.let { if (it in 30.0..60.0) AlertLevel.GOOD else AlertLevel.WARNING },
        )
        val before = previous
        previous = current
        val changes = mutableListOf<String>()
        if (before == null || before.pm != current.pm) {
            when (current.pm) {
                AlertLevel.CRITICAL -> changes.add("PM2.5: ROJO (${sample.pm} µg/m³)")
                AlertLevel.WARNING -> changes.add("PM2.5: AMARILLO (${sample.pm} µg/m³)")
                AlertLevel.GOOD -> if (before != null) changes.add("PM2.5: VERDE (${sample.pm} µg/m³)")
                null -> if (before?.pm != null) changes.add("PM2.5: sin lectura clasificable")
            }
        }
        if (before == null || before.temperature != current.temperature) {
            when (current.temperature) {
                AlertLevel.WARNING -> changes.add("Temperatura: ${sample.temperature} °C, fuera de referencia")
                AlertLevel.GOOD -> if (before != null) changes.add("Temperatura en referencia (18–25 °C)")
                null -> if (before?.temperature != null) changes.add("Temperatura: sin lectura")
                else -> Unit
            }
        }
        if (before == null || before.humidity != current.humidity) {
            when (current.humidity) {
                AlertLevel.WARNING -> changes.add("Humedad: ${sample.humidity} %, fuera de referencia")
                AlertLevel.GOOD -> if (before != null) changes.add("Humedad en referencia (30–60 %)")
                null -> if (before?.humidity != null) changes.add("Humedad: sin lectura")
                else -> Unit
            }
        }
        if (changes.isEmpty()) return null
        val missing = current.pm == null || current.temperature == null || current.humidity == null
        val level = listOfNotNull(current.pm, current.temperature, current.humidity,
            AlertLevel.WARNING.takeIf { missing }).maxBy { it.ordinal }
        val sound = lastSoundAt == null || now - lastSoundAt!! >= 30_000 || level.ordinal > (lastSoundLevel?.ordinal ?: -1)
        if (sound) { lastSoundAt = now; lastSoundLevel = level }
        return EnvironmentalAlert(++nextId, level, "Qhawa · Cambio en las lecturas", changes.joinToString(" • "), sound)
    }
}
