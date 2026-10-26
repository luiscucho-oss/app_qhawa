package com.cucho.qhawa.feature.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cucho.qhawa.data.environment.EnvironmentalReading
import com.cucho.qhawa.data.habits.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun AdviceContent(
    progress: WellnessSnapshot, now: Long, saving: Boolean,
    reading: EnvironmentalReading?, demo: Boolean,
    mascot: @Composable () -> Unit,
    onHistory: () -> Unit, onComplete: (String) -> Unit,
) {
    val selection = selectPmAdvice(reading, demo)
    val ready = progress.adviceReady(now)
    val ink = Color(0xFF18333B)
    Text("Consejos para hoy", style = MaterialTheme.typography.headlineMedium, color = ink)
    Text("Cuatro consejos para el contexto actual, a tu ritmo", style = MaterialTheme.typography.bodyMedium, color = ink)
    mascot()
    Text(selection.level?.let { "${it.label} · ${it.range}. Rangos demostrativos." }
        ?: "Orientación general · Sin lectura válida de PM2.5 para asignar un nivel.",
        style = MaterialTheme.typography.bodyMedium, color = ink)
    AnimatedVisibility(
        visible = !ready,
        enter = fadeIn(tween(280)) + expandVertically(tween(280)),
        exit = fadeOut(tween(180)) + shrinkVertically(tween(220)),
    ) {
        val seconds = ((progress.adviceAvailableAt - now).coerceAtLeast(0) + 999) / 1000
        val remaining = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
        Surface(shape = RoundedCornerShape(26.dp), color = Color(0xFFFFE8BD)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Default.TaskAlt, null, Modifier.size(38.dp), tint = Color(0xFF765000))
                Text("¡Acción registrada!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ink)
                Text("Puedes registrar otra acción en $remaining", style = MaterialTheme.typography.titleLarge, color = Color(0xFF765000))
                val time = SimpleDateFormat("HH:mm:ss", LocalConfiguration.current.locales[0]).format(Date(progress.adviceAvailableAt))
                Text("Disponible a las $time. Puedes cerrar la app o cambiar de pantalla: la espera se conserva.",
                    style = MaterialTheme.typography.bodyMedium, color = ink)
            }
        }
    }
    val palette = listOf(Color(0xFFCEF5E7), Color(0xFFFFE7C6), Color(0xFFDBEAFF), Color(0xFFEDE1FB))
    val icons = listOf(Icons.Default.CleaningServices, Icons.Default.Timeline, Icons.Default.Air, Icons.Default.Window)
    selection.advice.forEachIndexed { index, recommendation ->
        Surface(shape = RoundedCornerShape(26.dp), color = palette[index % palette.size]) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.8f)) {
                    Icon(icons[index % icons.size], null, Modifier.padding(12.dp).size(28.dp), tint = Color(0xFF006955))
                }
                Text("ACCIÓN ${index + 1} DE ${selection.advice.size}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF285846))
                Text(recommendation.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ink)
                Text(recommendation.description, style = MaterialTheme.typography.bodyMedium, color = ink)
                Button(onClick = { onComplete(recommendation.id) }, enabled = !saving && ready,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006C59))) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (saving) "Guardando…" else "Marcar como hecho")
                }
            }
        }
    }

    Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFFE1EDFF)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Insights, null, Modifier.size(24.dp), tint = Color(0xFF006194))
                Text("Tus lecturas, con contexto", style = MaterialTheme.typography.titleMedium, color = ink)
            }
            Text(when {
                demo -> "Estás viendo datos de ejemplo. No describen el ambiente de tu hogar."
                selection.level == null -> "No hay una lectura válida de PM2.5. Estas acciones son generales."
                reading?.simulated == true -> "Wokwi publica una simulación por MQTT. Puedes comparar sus registros en Historial."
                else -> "Consulta el historial para comparar las lecturas antes y después de una acción."
            }, style = MaterialTheme.typography.bodyMedium, color = ink)
            Text("Qhawa no conoce la contaminación exterior ni identifica automáticamente la fuente de partículas. Registrar una acción no confirma una mejora en las lecturas.",
                style = MaterialTheme.typography.bodyMedium, color = ink)
            OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Revisar historial") }
        }
    }
}
