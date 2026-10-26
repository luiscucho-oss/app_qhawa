package com.cucho.qhawa.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cucho.qhawa.data.habits.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val HabitInk = Color(0xFF192C36)
private val HabitOrange = Color(0xFF9A4D00)
private val HabitGold = Brush.horizontalGradient(listOf(Color(0xFFF2A529), Color(0xFFFFD956)))

@Composable
internal fun HabitsContent(
    progress: WellnessSnapshot, now: Long, saving: Boolean,
    mascot: @Composable () -> Unit,
    onComplete: (String) -> Unit,
) {
    val today = dateKey(now)
    val dayCellHeight = (44 * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp
    val dates = remember(today.substring(0, 7)) { monthDays(now) }
    var selectedDate by rememberSaveable(today) { mutableStateOf(today) }
    val selectedTime = dates.firstOrNull { dateKey(it) == selectedDate } ?: now
    val assignedId = progress.assignments[selectedDate]
    val habit = dailyHabits.firstOrNull { it.id == assignedId } ?: dailyHabit(selectedTime)
    val done = selectedDate in progress.completedDays
    val stats = wellnessProgress(progress.completedDays, now)
    val streak = habitStreak(progress.completedDays, now)
    val month = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es")).format(Date(now)).replaceFirstChar { it.titlecase() }

    Text("Mis hábitos", style = MaterialTheme.typography.headlineMedium, color = HabitInk)
    Text("Una pequeña acción diferente cada día", style = MaterialTheme.typography.bodyMedium, color = HabitInk)

    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(20.dp), color = Color.Transparent) {
            Column(Modifier.background(HabitGold).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFF774000), modifier = Modifier.size(26.dp))
                Text("${stats.monthDone}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B3000))
                Text(if (stats.monthDone == 1) "día este mes" else "días este mes", color = Color(0xFF4B3000), style = MaterialTheme.typography.bodySmall)
            }
        }
        Surface(Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(20.dp), color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFDFEAE3))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFF047E71), modifier = Modifier.size(26.dp))
                Text("$streak", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = HabitInk)
                Text(if (streak == 1) "día de racha" else "días de racha", color = HabitInk, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    Surface(shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE0E8E3))) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(month, Modifier.padding(horizontal = 4.dp, vertical = 4.dp), style = MaterialTheme.typography.titleLarge, color = HabitInk)
            Row(Modifier.fillMaxWidth()) {
                listOf("L", "M", "M", "J", "V", "S", "D").forEach { weekday ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(weekday, style = MaterialTheme.typography.labelLarge, color = Color(0xFF65746E))
                    }
                }
            }
            val firstWeekday = Calendar.getInstance().apply { timeInMillis = dates.first() }.get(Calendar.DAY_OF_WEEK)
            val offset = (firstWeekday + 5) % 7
            val cells = List<Long?>(offset) { null } + dates
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { column ->
                        val time = week.getOrNull(column)
                        if (time == null) Spacer(Modifier.weight(1f).height(dayCellHeight)) else {
                            val date = dateKey(time)
                            val completed = date <= today && date in progress.completedDays
                            val future = date > today
                            val previous = week.getOrNull(column - 1)?.let { dateKey(it) in progress.completedDays && dateKey(it) <= today } == true
                            val next = week.getOrNull(column + 1)?.let { dateKey(it) in progress.completedDays && dateKey(it) <= today } == true
                            val shape = RoundedCornerShape(
                                topStart = if (completed && previous) 0.dp else 22.dp,
                                bottomStart = if (completed && previous) 0.dp else 22.dp,
                                topEnd = if (completed && next) 0.dp else 22.dp,
                                bottomEnd = if (completed && next) 0.dp else 22.dp,
                            )
                            Box(
                                Modifier.weight(1f).height(dayCellHeight)
                                    .then(if (completed) Modifier.background(HabitGold, shape) else Modifier)
                                    .then(if (date == selectedDate) Modifier.border(2.dp, Color(0xFF006194), CircleShape) else Modifier)
                                    .semantics {
                                        contentDescription = "Día $date"
                                        stateDescription = when { future -> "Bloqueado: día futuro"; completed -> "Completado"; else -> "Pendiente" }
                                        selected = date == selectedDate
                                    }
                                    .clickable(enabled = !future, role = Role.Button) { selectedDate = date },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(date.takeLast(2).toInt().toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                        color = if (future) Color(0xFF9AA49F) else if (completed) Color(0xFF583700) else HabitInk)
                                    if (future) Icon(Icons.Default.Lock, null, Modifier.size(9.dp), tint = Color(0xFF9AA49F))
                                }
                            }
                        }
                    }
                }
            }
            Text("Toca un día para ver su hábito. Los días futuros están bloqueados.",
                Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.bodySmall, color = Color(0xFF52645C))
        }
    }

    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF3F8F5)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HabitProgress("Hoy", if (stats.todayDone) 1 else 0, 1)
            HabitProgress("Esta semana", stats.weekDone, 7)
            HabitProgress("Este mes", stats.monthDone, stats.monthTotal)
        }
    }

    mascot()

    Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFFFFE9C5)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (selectedDate == today) "Tu hábito de hoy" else "Hábito del ${selectedDate.takeLast(2).toInt()}",
                style = MaterialTheme.typography.labelLarge, color = HabitOrange)
            Text(habit.title, style = MaterialTheme.typography.titleLarge, color = HabitInk)
            Text(habit.description, style = MaterialTheme.typography.bodyMedium, color = HabitInk)
            Button(
                onClick = { onComplete(selectedDate) }, enabled = !done && !saving && selectedDate <= today,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF995300)),
            ) {
                Text(when { done && selectedDate == today -> "¡Completado hoy!"; done -> "Hábito completado"; saving -> "Guardando…"; else -> "Marcar como realizada" })
            }
            Text("Tu avance refleja tu constancia, no la calidad del aire.", style = MaterialTheme.typography.bodySmall, color = HabitInk)
        }
    }
}

@Composable
private fun HabitProgress(label: String, done: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$done de $total", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { done.toFloat() / total.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth().height(7.dp), color = Color(0xFFE5A421), trackColor = Color(0xFFE1E9E3))
    }
}
