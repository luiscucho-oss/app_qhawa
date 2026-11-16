package com.cucho.qhawa.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cucho.qhawa.data.environment.EnvironmentalReading
import com.cucho.qhawa.data.habits.PmAdviceLevel
import com.cucho.qhawa.data.habits.selectPmAdvice

private val adviceCardColors = listOf(
    Color(0xFFDFF3E8) to Color(0xFF135A40),
    Color(0xFFE1EEFA) to Color(0xFF254F78),
    Color(0xFFFFEBC8) to Color(0xFF754800),
    Color(0xFFEFE6FA) to Color(0xFF5B397C),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdviceContent(
    reading: EnvironmentalReading?,
    demo: Boolean,
    saving: Boolean,
    mascot: @Composable () -> Unit,
    onHistory: () -> Unit,
    onComplete: (String) -> Unit,
) {
    val selection = selectPmAdvice(reading, demo)
    var openAdviceId by rememberSaveable(selection.level, demo) { mutableStateOf<String?>(null) }
    val ink = Color(0xFF18333B)
    val (background, accent) = when (selection.level) {
        PmAdviceLevel.GREEN -> Color(0xFFDFF3E8) to Color(0xFF135A40)
        PmAdviceLevel.YELLOW -> Color(0xFFFFEBC8) to Color(0xFF754800)
        PmAdviceLevel.RED -> Color(0xFFFFE4E1) to Color(0xFF8E2521)
        null -> Color(0xFFE8EEF8) to Color(0xFF304D74)
    }
    val levelText = selection.level?.let { "${it.label} · PM2.5 ${it.range}" }
        ?: "Orientación general · Sin lectura actual de PM2.5"
    val callToAction = selection.level?.action ?: "Ver qué hacer"

    Text("Consejos para hoy", style = MaterialTheme.typography.headlineMedium, color = ink)
    mascot()
    Text(levelText, style = MaterialTheme.typography.titleMedium, color = accent)
    selection.advice.forEachIndexed { index, recommendation ->
        val (cardBackground, cardAccent) = adviceCardColors[index % adviceCardColors.size]
        Surface(
            onClick = { openAdviceId = recommendation.id },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = cardBackground,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(recommendation.title, style = MaterialTheme.typography.titleLarge, color = ink)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(callToAction, style = MaterialTheme.typography.labelLarge, color = cardAccent)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp), tint = cardAccent)
                }
            }
        }
    }
    OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("Revisar historial") }

    selection.advice.firstOrNull { it.id == openAdviceId }?.let { recommendation ->
        ModalBottomSheet(onDismissRequest = { openAdviceId = null }, containerColor = background) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(levelText, style = MaterialTheme.typography.labelLarge, color = accent)
                Text(recommendation.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ink)
                Text(recommendation.description, style = MaterialTheme.typography.bodyLarge, color = ink)
                Button(
                    onClick = {
                        onComplete(recommendation.id)
                        openAdviceId = null
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                ) { Text(if (saving) "Guardando…" else "Marcar como hecho") }
                TextButton(onClick = { openAdviceId = null }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Cerrar", color = accent)
                }
            }
        }
    }
}
