package com.cucho.qhawa.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cucho.qhawa.core.designsystem.component.QhawaMascotComponent
import com.cucho.qhawa.core.designsystem.component.QhawaMascotState
import com.cucho.qhawa.core.designsystem.theme.qhawaScreenBackground
import com.cucho.qhawa.data.environment.*
import com.cucho.qhawa.data.habits.pmAdviceLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

private val HistoryGreen = Color(0xFF007958)
private val HistoryMint = Color(0xFFCEF7E9)
private val HistoryInk = Color(0xFF172033)
private val HistoryMuted = Color(0xFF536170)
private fun historyNumber(value: Double?) = value?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "—"
private fun historyDate(time: Long, pattern: String = "dd MMM · HH:mm") =
    SimpleDateFormat(pattern, Locale.forLanguageTag("es")).format(Date(time))

private enum class HistoryMetric(val title: String, val unit: String, val icon: ImageVector) {
    AIR("Calidad del aire", "µg/m³", Icons.Default.Air),
    PM10("PM10", "µg/m³", Icons.Default.Air),
    TEMPERATURE("Temperatura", "°C", Icons.Default.DeviceThermostat),
    HUMIDITY("Humedad", "%", Icons.Default.Opacity);

    fun value(reading: EnvironmentalReading) = when (this) {
        AIR -> reading.pm
        PM10 -> reading.pm10
        TEMPERATURE -> reading.temperature
        HUMIDITY -> reading.humidity
    }
}

/** Pantalla exclusiva de Historial. Consume la misma fuente de datos que Inicio. */
@Composable
internal fun HistoryScreen(
    readings: List<EnvironmentalReading>,
    demo: Boolean,
    now: Long,
    environment: EnvironmentState,
    onDemoChange: (Boolean) -> Unit,
    forEachReading: suspend (Long, Long, (EnvironmentalReading) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var period by rememberSaveable { mutableStateOf(HistoryPeriod.TODAY) }
    var metric by rememberSaveable { mutableStateOf(HistoryMetric.AIR) }
    val filtered = remember(readings, period, now) { historyReadings(readings, period, now) }
    val latest = filtered.lastOrNull()
    val band = pmAdviceLevel(latest?.pm)?.ordinal
    val bandText = when (band) { 0 -> "Nivel bajo"; 1 -> "Moderado"; 2 -> "Elevado"; else -> "Sin lectura" }
    var tip by rememberSaveable(period, demo) { mutableIntStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var pendingReportPath by rememberSaveable { mutableStateOf("") }
    var exporting by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            exporting = true
            val reportPath = pendingReportPath
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val report = File(reportPath)
                    check(report.isFile) { "El reporte temporal no está disponible" }
                    checkNotNull(context.contentResolver.openOutputStream(uri)).use { output ->
                        report.inputStream().use { it.copyTo(output) }
                    }
                }
                .also { if (reportPath.isNotEmpty()) File(reportPath).delete() }
            }
            exporting = false
            pendingReportPath = ""
            snackbar.showSnackbar(if (result.isSuccess) "Reporte guardado" else "No se pudo guardar. Inténtalo nuevamente.")
        } else {
            val reportPath = pendingReportPath
            pendingReportPath = ""
            exporting = false
            if (reportPath.isNotEmpty()) scope.launch(Dispatchers.IO) { File(reportPath).delete() }
        }
    }

    Box(modifier.fillMaxSize().qhawaScreenBackground(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 620.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(modifier = Modifier.semantics { contentDescription = "Período del historial" },
                color = Color(0xFFF0EFFF), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    HistoryPeriod.entries.forEach { option ->
                        Surface(
                            modifier = Modifier.weight(1f).selectable(
                                selected = period == option, role = Role.RadioButton,
                                interactionSource = remember(option) { MutableInteractionSource() }, indication = null,
                            ) { period = option },
                            color = if (period == option) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = if (period == option) 1.dp else 0.dp,
                        ) {
                            Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(option.title, style = MaterialTheme.typography.labelLarge,
                                    color = if (period == option) MaterialTheme.colorScheme.primary else HistoryMuted)
                            }
                        }
                    }
                }
            }

            ExampleDataRow(demo = demo, onDemoChange = onDemoChange)

            Surface(color = Color(0xFFF0EFFF), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    QhawaMascotComponent(
                        state = when (band) { 0 -> QhawaMascotState.HAPPY; 1, 2 -> QhawaMascotState.ALERT; else -> QhawaMascotState.IDLE },
                        message = "", mascotSize = 72.dp,
                        onMascotClick = { tip = tip % 2 + 1 }, reactionKey = period,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Última lectura del período", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(if (latest == null) "Sin registros" else bandText,
                            style = MaterialTheme.typography.titleMedium, color = HistoryInk, fontWeight = FontWeight.Bold)
                        val hint = when (tip) {
                            1 -> "Prueba el filtro Hoy para ver los registros recientes."
                            2 -> "Toca el gráfico para explorar las fechas."
                            else -> null
                        }
                        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = HistoryGreen)
                    }
                }
            }

            if (!demo) {
                val fresh = latest != null && environment.freshReading(now) != null
                Text(if (fresh) "Recibiendo lecturas del prototipo · Una muestra por minuto en la gráfica"
                    else "${if (environment.brokerConnected) "MQTT conectado" else "MQTT sin conexión"} · ${if (latest == null) "Esperando registros" else "Mostrando registros guardados"}",
                    style = MaterialTheme.typography.bodySmall, color = HistoryMuted)
            }

            HistoryChart(filtered, period, metric, onMetricChange = { metric = it })

            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HistorySmallMetric("Temperatura", latest?.temperature, "°C", Icons.Default.DeviceThermostat,
                    when { latest?.temperature == null -> "Sin lectura DHT11"; latest.temperature in 18.0..25.0 -> "En rango"; else -> "Fuera de rango" },
                    Color(0xFF956000), metric == HistoryMetric.TEMPERATURE,
                    Modifier.weight(1f).fillMaxHeight()) { metric = HistoryMetric.TEMPERATURE }
                HistorySmallMetric("Humedad", latest?.humidity, "%", Icons.Default.Opacity,
                    when { latest?.humidity == null -> "Sin lectura DHT11"; latest.humidity in 30.0..60.0 -> "En rango"; else -> "Fuera de rango" },
                    Color(0xFF0086BA), metric == HistoryMetric.HUMIDITY,
                    Modifier.weight(1f).fillMaxHeight()) { metric = HistoryMetric.HUMIDITY }
            }

            val week = remember(readings, now) { historyWeekSummary(readings, now) }
            Surface(color = HistoryMint, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White) {
                        Icon(Icons.Default.Verified, null, tint = HistoryGreen, modifier = Modifier.padding(10.dp).size(22.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("RESUMEN DE LA SEMANA", style = MaterialTheme.typography.labelSmall, color = HistoryGreen)
                        Text(if (week.recordedDays == 0) "Aún no hay días registrados" else "${week.lowDays} de ${week.recordedDays} días con PM bajo",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HistoryInk)
                        Text("Promedios diarios de las muestras por minuto. ${7 - week.recordedDays} días sin registros.",
                            style = MaterialTheme.typography.bodySmall, color = HistoryMuted)
                    }
                }
            }
            Button(
                onClick = {
                    val snapshot = filtered
                    val snapshotDemo = demo
                    val from = historyDayStart(now, period.days - 1)
                    val to = now
                    val filename = "qhawa_historial_${period.name.lowercase()}_${historyDate(now, "yyyyMMdd")}.csv"
                    exporting = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                val report = File.createTempFile("qhawa-history-", ".csv", context.cacheDir)
                                try {
                                    report.bufferedWriter(Charsets.UTF_8).use { writer ->
                                        writer.writeHistoryCsv(snapshotDemo) { action ->
                                            if (snapshotDemo) snapshot.forEach(action)
                                            else forEachReading(from, to, action)
                                        }
                                    }
                                    report.absolutePath
                                } catch (error: Exception) {
                                    report.delete()
                                    throw error
                                }
                            }
                        }
                        if (result.isSuccess) {
                            pendingReportPath = result.getOrThrow()
                            export.launch(filename)
                        } else {
                            exporting = false
                            snackbar.showSnackbar("No se pudo preparar el reporte. Inténtalo nuevamente.")
                        }
                    }
                },
                enabled = filtered.isNotEmpty() && !exporting,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (exporting) "Guardando…" else "Descargar reporte simple")
            }
            Text("El reporte incluye todas las lecturas del período seleccionado y su origen.",
                style = MaterialTheme.typography.bodySmall, color = HistoryMuted)
            Spacer(Modifier.height(4.dp))
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun HistorySmallMetric(
    title: String, value: Double?, unit: String, icon: ImageVector, status: String,
    accent: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit,
) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) accent else Color(0xFFF0EDF5))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(18.dp), tint = accent)
                Text(title, style = MaterialTheme.typography.bodySmall, color = HistoryMuted)
            }
            Text("${historyNumber(value)} $unit", style = MaterialTheme.typography.headlineSmall, color = HistoryInk, fontWeight = FontWeight.Bold)
            Text(status, style = MaterialTheme.typography.bodySmall, color = accent)
        }
    }
}

@Composable
private fun HistoryChart(
    readings: List<EnvironmentalReading>, period: HistoryPeriod, metric: HistoryMetric,
    onMetricChange: (HistoryMetric) -> Unit,
) {
    val available = remember(readings, metric) { readings.filter { metric.value(it)?.isFinite() == true } }
    val points = remember(available) {
        if (available.size <= 180) available else (0 until 180).map { available[it * (available.size - 1) / 179] }
    }
    var selectedTime by rememberSaveable(period, metric) { mutableStateOf<Long?>(null) }
    val index = if (selectedTime == null) points.lastIndex else points.indices.minByOrNull { abs(points[it].timestamp - selectedTime!!) } ?: -1
    val selected = points.getOrNull(index)
    val min = points.minOfOrNull { metric.value(it)!! } ?: 0.0
    val max = points.maxOfOrNull { metric.value(it)!! } ?: 0.0
    val isParticulateMatter = metric == HistoryMetric.AIR || metric == HistoryMetric.PM10
    val unit = when (metric) {
        HistoryMetric.AIR -> "PM2.5 · ${metric.unit}"
        HistoryMetric.PM10 -> metric.unit
        else -> "${metric.title} · ${metric.unit}"
    }
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = HistoryMint) {
                    Icon(metric.icon, null, Modifier.padding(7.dp).size(20.dp), tint = HistoryGreen)
                }
                Column(Modifier.weight(1f)) {
                    Text(metric.title + if (period == HistoryPeriod.TODAY) " hoy" else "", style = MaterialTheme.typography.titleLarge,
                        color = HistoryInk, fontWeight = FontWeight.Bold)
                    Text(unit, style = MaterialTheme.typography.bodySmall, color = HistoryMuted)
                }
            }
            if (metric == HistoryMetric.AIR) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val labels = listOf("Bajo ≤12", "Medio ≤35", "Alto >35")
                    val colors = listOf(HistoryGreen, Color(0xFFB66B00), Color(0xFFB93436))
                    labels.forEachIndexed { i, label ->
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(6.dp).background(colors[i], RoundedCornerShape(50)))
                            Text(label, style = MaterialTheme.typography.labelSmall, color = HistoryMuted)
                        }
                    }
                }
                TextButton(onClick = { onMetricChange(HistoryMetric.PM10) }, contentPadding = PaddingValues(0.dp)) { Text("Ver PM10") }
            } else {
                TextButton(onClick = { onMetricChange(HistoryMetric.AIR) }, contentPadding = PaddingValues(0.dp)) { Text("Volver a calidad del aire") }
            }
            if (points.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Timeline, null, Modifier.size(32.dp), tint = HistoryMuted)
                    Spacer(Modifier.height(8.dp))
                    Text("Sin lecturas en este período", style = MaterialTheme.typography.titleSmall, color = HistoryInk)
                    Text("Selecciona otro período o activa los ejemplos.", style = MaterialTheme.typography.bodySmall, color = HistoryMuted)
                }
            } else {
                Canvas(Modifier.fillMaxWidth().height(108.dp)
                    .semantics {
                        contentDescription = if (isParticulateMatter) {
                            "Gráfico de ${metric.title}."
                        } else {
                            "Gráfico de ${metric.title}. ${points.size} puntos. Mínimo ${historyNumber(min)}, máximo ${historyNumber(max)} ${metric.unit}. Usa el deslizador para consultar lecturas."
                        }
                    }
                    .pointerInput(points) {
                        detectTapGestures { tap ->
                            val fraction = ((tap.x - 8.dp.toPx()) / (size.width - 16.dp.toPx())).coerceIn(0f, 1f)
                            val target = points.first().timestamp + fraction * (points.last().timestamp - points.first().timestamp)
                            selectedTime = points.minBy { abs(it.timestamp - target.toLong()) }.timestamp
                        }
                    }) {
                    val inset = 8.dp.toPx()
                    val w = size.width - inset * 2
                    val h = size.height - inset * 2
                    fun point(i: Int) = Offset(
                        inset + if (points.size == 1) w / 2 else (points[i].timestamp - points.first().timestamp).toFloat() /
                            (points.last().timestamp - points.first().timestamp).coerceAtLeast(1) * w,
                        inset + h * (1 - ((metric.value(points[i])!! - min) / (max - min).coerceAtLeast(1.0)).toFloat()),
                    )
                    repeat(3) { grid ->
                        val y = inset + h * grid / 2
                        drawLine(Color(0xFFE7EFED), Offset(inset, y), Offset(size.width - inset, y), 1.dp.toPx())
                    }
                    val line = Path().apply {
                        val first = point(0)
                        moveTo(first.x, first.y)
                        (1 until points.size).forEach { i ->
                            val a = point(i - 1)
                            val b = point(i)
                            val middle = (a.x + b.x) / 2
                            cubicTo(middle, a.y, middle, b.y, b.x, b.y)
                        }
                    }
                    val area = Path().apply {
                        addPath(line)
                        lineTo(point(points.lastIndex).x, size.height - inset)
                        lineTo(point(0).x, size.height - inset)
                        close()
                    }
                    drawPath(area, Brush.verticalGradient(listOf(Color(0xFFB8EFDD), Color(0xFFF0FCF8))))
                    drawPath(line, HistoryGreen, style = Stroke(width = 2.5.dp.toPx()))
                    if (index >= 0) {
                        drawCircle(Color.White, 5.dp.toPx(), point(index))
                        drawCircle(Color(0xFF00A5CB), 3.dp.toPx(), point(index))
                    }
                }
                if (!isParticulateMatter) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val format = if (period == HistoryPeriod.TODAY) "HH:mm" else "dd MMM"
                        Text(historyDate(points.first().timestamp, format), style = MaterialTheme.typography.labelSmall, color = HistoryMuted)
                        Text(historyDate(points.last().timestamp, format), style = MaterialTheme.typography.labelSmall, color = HistoryMuted)
                    }
                }
                selected?.let {
                    val date = historyDate(it.timestamp)
                    val label = if (isParticulateMatter) date else "${historyNumber(metric.value(it))} ${metric.unit} · $date"
                    Text(label, style = MaterialTheme.typography.labelMedium, color = HistoryGreen)
                }
                if (points.size > 1) Slider(
                    value = index.coerceAtLeast(0).toFloat(),
                    onValueChange = { selectedTime = points[it.roundToInt().coerceIn(points.indices)].timestamp },
                    valueRange = 0f..points.lastIndex.toFloat(),
                    modifier = Modifier.height(28.dp).semantics { contentDescription = "Consultar lectura del historial" },
                    colors = SliderDefaults.colors(thumbColor = HistoryGreen, activeTrackColor = HistoryGreen),
                )
            }
        }
    }
}
