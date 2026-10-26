package com.cucho.qhawa.data.habits

import com.cucho.qhawa.data.environment.EnvironmentalReading

/** Rangos demostrativos de Qhawa; no son umbrales sanitarios. */
enum class PmAdviceLevel(val label: String, val range: String) {
    GREEN("VERDE", "0 ≤ PM2.5 < 25 µg/m³"),
    YELLOW("AMARILLO", "25 ≤ PM2.5 < 75 µg/m³"),
    RED("ROJO", "PM2.5 ≥ 75 µg/m³"),
}

fun pmAdviceLevel(pm: Double?): PmAdviceLevel? = when {
    pm == null || !pm.isFinite() || pm < 0 -> null
    pm < 25 -> PmAdviceLevel.GREEN
    pm < 75 -> PmAdviceLevel.YELLOW
    else -> PmAdviceLevel.RED
}

val pmAdvice: Map<PmAdviceLevel, List<DailyHabit>> = mapOf(
    PmAdviceLevel.GREEN to listOf(
        DailyHabit("pm_green_dust", "Limpia sin levantar polvo", "Utiliza un paño ligeramente húmedo; evita sacudir o barrer en seco."),
        DailyHabit("pm_green_smoke", "Evita generar humo", "Mantén apagadas velas e incienso y evita fumar dentro."),
        DailyHabit("pm_green_cooking", "Prevén partículas al cocinar", "Utiliza la campana con salida al exterior y evita quemar los alimentos."),
        DailyHabit("pm_green_observe", "Observa tu ambiente", "Consulta las lecturas después de cocinar o limpiar para reconocer variaciones."),
    ),
    PmAdviceLevel.YELLOW to listOf(
        DailyHabit("pm_yellow_sources", "Revisa posibles fuentes", "Pausa frituras, aerosoles, sacudido de polvo o combustiones que puedan generar partículas."),
        DailyHabit("pm_yellow_ventilate", "Ventila con criterio", "Abre ventanas únicamente si el aire exterior está más limpio; considera humo y tráfico cercano."),
        DailyHabit("pm_yellow_filter", "Activa la filtración", "Si tienes un purificador adecuado para la habitación, enciéndelo siguiendo sus instrucciones; evita equipos que produzcan ozono."),
        DailyHabit("pm_yellow_compare", "Comprueba el cambio", "Compara varias lecturas posteriores a la acción; si siguen elevadas, revisa otras posibles fuentes."),
    ),
    PmAdviceLevel.RED to listOf(
        DailyHabit("pm_red_sources", "Detén la generación de partículas", "Suspende de forma segura las actividades identificadas que produzcan humo."),
        DailyHabit("pm_red_dust", "Evita levantar más polvo", "Pausa el barrido en seco, el sacudido y la aspiración sin filtro HEPA."),
        DailyHabit("pm_red_filter", "Refuerza la filtración", "Si dispones de un purificador HEPA adecuado, utiliza una velocidad alta según el fabricante y despeja sus entradas y salidas."),
        DailyHabit("pm_red_ventilate", "Decide cómo renovar el aire", "Ventila si el exterior está más limpio; si hay contaminación exterior, evita abrir hacia esa fuente y prioriza reducir emisiones interiores y filtrar."),
    ),
)

data class AdviceSelection(val level: PmAdviceLevel?, val advice: List<DailyHabit>)

fun selectPmAdvice(reading: EnvironmentalReading?, demo: Boolean): AdviceSelection {
    // Los ejemplos locales y las mediciones de otro contaminante no asignan nivel PM2.5.
    val level = if (!demo && reading?.pmLabel == "PM2.5") pmAdviceLevel(reading.pm) else null
    return AdviceSelection(level, level?.let { pmAdvice.getValue(it) } ?: dailyAdvice.take(4))
}
