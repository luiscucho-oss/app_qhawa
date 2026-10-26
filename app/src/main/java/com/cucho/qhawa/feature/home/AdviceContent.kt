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

private sealed interface AdviceCard {
    data class Ready(val index: Int) : AdviceCard
    data class Waiting(val availableAt: Long) : AdviceCard
}

@Composable
internal fun AdviceContent(
    progress: WellnessSnapshot, now: Long, saving: Boolean,
    reading: EnvironmentalReading?, demo: Boolean,
    mascot: @Composable () -> Unit,
    onHistory: () -> Unit, onComplete: () -> Unit,
) {
    val ink = Color(0xFF18333B)
    Text("Consejos para hoy", style = MaterialTheme.typography.headlineMedium, color = ink)
    Text("Una recomendación a la vez, a tu ritmo", style = MaterialTheme.typography.bodyMedium, color = ink)
    mascot()
    val card = if (progress.adviceReady(now)) AdviceCard.Ready(progress.adviceIndex) else AdviceCard.Waiting(progress.adviceAvailableAt)
    AnimatedContent(
        targetState = card,
        transitionSpec = {
            (fadeIn(tween(280, delayMillis = 160)) + expandVertically(tween(280))) togetherWith
                (fadeOut(tween(180)) + shrinkVertically(tween(220)))
        }, label = "Recomendación y espera persistida",
    ) { target ->
        when (target) {
            is AdviceCard.Ready -> {
                val recommendation = dailyAdvice[target.index]
                val palette = listOf(Color(0xFFCEF5E7), Color(0xFFFFE7C6), Color(0xFFDBEAFF), Color(0xFFEDE1FB))
                val icons = listOf(Icons.Default.CleaningServices, Icons.Default.Timeline, Icons.Default.Air, Icons.Default.Window)
                Surface(shape = RoundedCornerShape(26.dp), color = palette[target.index % palette.size]) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.8f)) {
                            Icon(icons[target.index % icons.size], null, Modifier.padding(12.dp).size(28.dp), tint = Color(0xFF006955))
                        }
                        Text("TU SIGUIENTE PEQUEÑA ACCIÓN", style = MaterialTheme.typography.labelSmall, color = Color(0xFF285846))
                        Text(recommendation.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ink)
                        Text(recommendation.description, style = MaterialTheme.typography.bodyMedium, color = ink)
                        Button(onClick = onComplete, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006C59))) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (saving) "Guardando…" else "Marcar como hecho")
                        }
                    }
                }
            }
            is AdviceCard.Waiting -> {
                val seconds = ((target.availableAt - now).coerceAtLeast(0) + 999) / 1000
                val remaining = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
                Surface(shape = RoundedCornerShape(26.dp), color = Color(0xFFFFE8BD)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Default.TaskAlt, null, Modifier.size(38.dp), tint = Color(0xFF765000))
                        Text("¡Recomendación completada!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ink)
                        Text("Próximo consejo en $remaining", style = MaterialTheme.typography.titleLarge, color = Color(0xFF765000))
                        val time = SimpleDateFormat("HH:mm:ss", LocalConfiguration.current.locales[0]).format(Date(target.availableAt))
                        Text("Disponible a las $time. Puedes cerrar la app o cambiar de pantalla: la espera se conserva.",
                            style = MaterialTheme.typography.bodyMedium, color = ink)
                    }
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
                reading == null -> "Aún no hay lecturas disponibles. Estas acciones son generales."
                reading.simulated -> "Wokwi publica una simulación por MQTT. Puedes comparar sus registros en Historial."
                else -> "Consulta el historial para comparar las lecturas antes y después de una acción."
            }, style = MaterialTheme.typography.bodyMedium, color = ink)
            OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Revisar historial") }
        }
    }
}
