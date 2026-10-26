package com.cucho.qhawa.feature.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cucho.qhawa.core.designsystem.component.QhawaMascotComponent
import com.cucho.qhawa.core.designsystem.component.QhawaMascotState
import com.cucho.qhawa.data.environment.DeviceConnection
import com.cucho.qhawa.data.environment.EnvironmentalReading
import com.cucho.qhawa.data.environment.WokwiMqtt

/** Adaptador de las cuatro pantallas: aquí vive el contenido, fuera del componente visual. */
@Composable
internal fun QhawaHomeCompanion(
    selectedTab: Int,
    reading: EnvironmentalReading?,
    connection: DeviceConnection,
    demo: Boolean,
    now: Long,
    habitDone: Boolean,
    adviceDone: Boolean,
    alert: String?,
) {
    val old = reading != null &&
        (now - reading.timestamp > WokwiMqtt.STALE_MS || connection != DeviceConnection.CONNECTED)
    val (pose, welcome) = when (selectedTab) {
        // Inicio: saludo y estado real de conexión, sin confundir simulación con datos del hogar.
        0 -> when {
            demo -> QhawaMascotState.GREETING to "¡Hola! Estoy mostrando ejemplos locales."
            connection == DeviceConnection.LOADING -> QhawaMascotState.IDLE to "Conectando con la simulación Wokwi…"
            connection == DeviceConnection.DISCONNECTED -> QhawaMascotState.ALERT to "Sin conexión MQTT. Conservaré el historial."
            connection == DeviceConnection.WAITING -> QhawaMascotState.IDLE to "MQTT conectado. Esperando nuevas lecturas de Wokwi."
            reading == null -> QhawaMascotState.IDLE to "Esperando los primeros datos."
            old -> QhawaMascotState.ALERT to "Hace rato que no recibo datos nuevos."
            reading.simulated -> QhawaMascotState.GREETING to "¡Recibí los datos de tu simulación!"
            else -> QhawaMascotState.GREETING to "Ya puedes consultar tus mediciones."
        }
        // Historial: reposo mientras el usuario explora sus registros.
        1 -> QhawaMascotState.IDLE to if (reading == null) "Cuando lleguen lecturas, las exploraremos aquí."
            else "Exploremos tus registros. Toca un gráfico para consultar una lectura."
        // Hábitos: una nueva acción provoca el salto de celebración.
        2 -> if (habitDone) QhawaMascotState.HAPPY to "¡Qhawa está feliz! Excelente trabajo."
            else QhawaMascotState.GREETING to "¿Listos para completar el hábito de hoy?"
        // Consejos: la pose de atención invita a actuar; completar cambia a felicidad.
        else -> if (adviceDone) QhawaMascotState.HAPPY to "¡Genial! Cada acción cuenta."
            else QhawaMascotState.ALERT to "Aquí tienes algunas ideas para hoy."
    }
    val baseMessage = alert?.let { "Cambio en las lecturas: $it. Revisa los consejos." } ?: welcome
    val basePose = if (alert != null) QhawaMascotState.ALERT else pose
    // Cambiar de pantalla o recibir novedades recupera el diálogo contextual.
    var tipIndex by rememberSaveable(selectedTab, baseMessage) { mutableIntStateOf(0) }
    val tips = when (selectedTab) {
        0 -> listOf("¡Miau! Toca Historial para explorar las lecturas.", "Puedes activar los datos de ejemplo para conocer la app.")
        1 -> listOf("Prueba el filtro Hoy para ver los registros recientes.", "Toca un punto del gráfico o mueve el deslizador para ver su valor.")
        2 -> listOf("La racha celebra tu constancia al registrar acciones.", "Marca el hábito cuando termines; volveré a celebrarlo contigo.")
        else -> listOf("Elige una acción y registra cuándo la completes.", "Después de actuar, consulta el historial para comparar registros.")
    }
    val message = when {
        tipIndex == 0 -> baseMessage
        alert != null -> "$baseMessage ${tips[(tipIndex - 1) % tips.size]}"
        else -> tips[(tipIndex - 1) % tips.size]
    }
    QhawaMascotComponent(
        state = basePose,
        message = message,
        modifier = Modifier.fillMaxWidth(),
        mascotSize = 124.dp,
        reactionKey = selectedTab,
        onMascotClick = { tipIndex = tipIndex % tips.size + 1 },
    )
}
