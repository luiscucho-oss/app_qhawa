package com.cucho.qhawa.data.habits

import java.util.Calendar

data class DailyHabit(val id: String, val title: String, val description: String)

/** Calendario v1: mantener el orden y los identificadores para conservar las asignaciones. */
val dailyHabits = listOf(
    DailyHabit("review", "Revisa tus mediciones", "Consulta temperatura, humedad y PM. Registra qué observas hoy."),
    DailyHabit("compare", "Compara dos lecturas", "Abre Historial y compara dos momentos del día."),
    DailyHabit("damp_cloth", "Limpia con un paño húmedo", "Elige una superficie pequeña y retira el polvo sin levantarlo."),
    DailyHabit("sensor_space", "Despeja el monitor", "Comprueba que ningún objeto cubra las entradas de aire del monitor."),
    DailyHabit("temperature", "Observa la temperatura", "Consulta la temperatura y anota mentalmente si cambió respecto a ayer."),
    DailyHabit("humidity", "Observa la humedad", "Consulta la humedad y revisa si hay condensación visible cerca de las ventanas."),
    DailyHabit("shoes", "Ordena la entrada", "Limpia la zona de entrada para reducir el polvo que se transporta al interior."),
    DailyHabit("cloth", "Prepara tu paño", "Deja listo un paño limpio para la próxima limpieza de superficies."),
    DailyHabit("shelf", "Cuida una repisa", "Retira el polvo de una repisa con movimientos suaves y un paño húmedo."),
    DailyHabit("window", "Revisa tus ventanas", "Observa si están limpias y despejadas; compara el ambiente antes de decidir ventilar."),
    DailyHabit("history", "Explora la semana", "Abre el filtro Semana en Historial e identifica un cambio en las lecturas."),
    DailyHabit("no_spray", "Elige una limpieza sin aerosol", "Si vas a limpiar hoy, considera aplicar el producto sobre el paño siguiendo su etiqueta."),
    DailyHabit("table", "Limpia tu mesa", "Dedica unos minutos a retirar el polvo de tu mesa de trabajo."),
    DailyHabit("ventilation", "Planifica la ventilación", "Consulta las condiciones exteriores antes de elegir un momento para abrir las ventanas."),
    DailyHabit("before_after", "Compara antes y después", "Observa las mediciones antes y después de una actividad cotidiana."),
    DailyHabit("textiles", "Revisa un textil", "Comprueba si una funda o cortina necesita limpieza según las instrucciones de su fabricante."),
    DailyHabit("filter", "Consulta tu filtro", "Si tienes un equipo con filtro, revisa cuándo corresponde su mantenimiento."),
    DailyHabit("cables", "Despeja tu escritorio", "Ordena un pequeño espacio para facilitar la limpieza sin mover el monitor."),
    DailyHabit("corners", "Limpia un rincón", "Retira el polvo de un rincón accesible con un paño húmedo."),
    DailyHabit("source", "Reconoce el origen de los datos", "Comprueba si estás viendo ejemplos locales o la simulación Wokwi."),
    DailyHabit("morning", "Revisa una lectura de la mañana", "Consulta en Historial una lectura disponible de las primeras horas del día."),
    DailyHabit("evening", "Revisa una lectura de la tarde", "Compara una lectura reciente con otra anterior, si hay registros disponibles."),
    DailyHabit("products", "Lee una etiqueta", "Revisa las indicaciones de uso y ventilación de un producto de limpieza."),
    DailyHabit("floor", "Cuida una zona del suelo", "Limpia una pequeña zona con el método indicado para ese material."),
    DailyHabit("door", "Limpia una puerta", "Retira el polvo de una superficie de la puerta con un paño apropiado."),
    DailyHabit("books", "Ordena tus libros", "Despeja una zona de libros para que la limpieza de la repisa sea más sencilla."),
    DailyHabit("pm", "Explora el gráfico de PM", "Selecciona un punto del gráfico de aire y consulta su valor y fecha."),
    DailyHabit("report", "Prepara un reporte", "Revisa el período de tu interés en Historial y prueba la descarga del reporte."),
    DailyHabit("routine", "Planifica una rutina breve", "Elige un momento del día para revisar las lecturas y realizar una pequeña acción."),
    DailyHabit("month", "Explora el mes", "Consulta el filtro Mes en Historial y observa cuántos días tienen registros."),
    DailyHabit("reflect", "Celebra tu constancia", "Revisa tu calendario y elige una acción que quieras repetir el próximo mes."),
)

fun dailyHabit(time: Long): DailyHabit {
    val calendar = Calendar.getInstance().apply { timeInMillis = time }
    val index = ((calendar.get(Calendar.YEAR).toLong() * 366 + calendar.get(Calendar.DAY_OF_YEAR) - 1) % dailyHabits.size).toInt()
    return dailyHabits[index]
}

fun monthDays(now: Long): List<Long> {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return List(calendar.getActualMaximum(Calendar.DAY_OF_MONTH)) {
        calendar.timeInMillis.also { calendar.add(Calendar.DAY_OF_MONTH, 1) }
    }
}

data class WellnessProgress(val todayDone: Boolean, val weekDone: Int, val monthDone: Int, val monthTotal: Int)

fun wellnessProgress(completed: Set<String>, now: Long): WellnessProgress {
    val monday = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -((get(Calendar.DAY_OF_WEEK) + 5) % 7))
    }.timeInMillis
    val today = dateKey(now)
    val week = (0..6).map { dateKey(dayBefore(monday, -it)) }.filter { it <= today }
    val month = monthDays(now).map(::dateKey)
    return WellnessProgress(today in completed, week.count { it in completed }, month.count { it <= today && it in completed }, month.size)
}

val dailyAdvice = listOf(
    DailyHabit("damp_cloth", "Utiliza paños húmedos", "Para no levantar polvo al limpiar, utiliza un paño húmedo en una superficie pequeña."),
    DailyHabit("compare_readings", "Observa el antes y el después", "Consulta Historial y compara los registros disponibles alrededor de una actividad cotidiana."),
    DailyHabit("clear_monitor", "Dale espacio a tu monitor", "Comprueba que las entradas de aire estén libres y que el equipo esté en un lugar estable."),
    DailyHabit("check_windows", "Elige cuándo ventilar", "Considera las condiciones exteriores y el tráfico cercano antes de abrir las ventanas."),
    DailyHabit("care_textiles", "Cuida tus textiles", "Revisa si una funda o cortina necesita limpieza y sigue las instrucciones del fabricante."),
    DailyHabit("read_labels", "Conoce tus productos", "Lee la etiqueta de un producto de limpieza y sigue sus indicaciones de uso y ventilación."),
)

const val ADVICE_WAIT_MILLIS = 5 * 60 * 1000L
