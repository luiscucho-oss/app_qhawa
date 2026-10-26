package com.cucho.qhawa.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import android.media.RingtoneManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.cucho.qhawa.core.designsystem.theme.qhawaScreenBackground
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cucho.qhawa.core.designsystem.component.QhawaTopBar
import com.cucho.qhawa.data.environment.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val tabs = listOf("Inicio", "Historial", "Hábitos", "Consejos")
private val tabIcons = listOf(Icons.Default.Home, Icons.Default.Timeline, Icons.Default.LocalFireDepartment, Icons.Default.Lightbulb)
private fun timestamp(value: Long): String = SimpleDateFormat("dd MMM · HH:mm", Locale.forLanguageTag("es")).format(Date(value))

@Composable
fun HomeScreen(
    userName: String?,
    userEmail: String?,
    onSignOutClick: () -> Unit,
    userId: String,
    isDemoSession: Boolean,
    environmentRepository: EnvironmentRepository,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var demonstration by rememberSaveable { mutableStateOf(false) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    val realState by environmentRepository.state.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
    val demo = remember { demoReadings(System.currentTimeMillis()) }
    val readings = if (demonstration) demo else realState.readings.sortedBy { it.timestamp }
    val context = LocalContext.current

    val currentReading = readings.lastOrNull()
    var previousState by remember { mutableStateOf<AlertState?>(null) }
    var activeAlert by remember { mutableStateOf<MetricLevel?>(null) }
    var alertMessage by remember { mutableStateOf("") }

    LaunchedEffect(currentReading) {
        if (currentReading != null) {
            val newState = AlertState(
                pm = getPmLevel(currentReading.pm),
                temp = getTempLevel(currentReading.temperature),
                hum = getHumLevel(currentReading.humidity)
            )

            if (previousState != null && newState != previousState) {
                val changes = mutableListOf<String>()
                if (newState.pm != previousState!!.pm) {
                    when (newState.pm) {
                        MetricLevel.CRITICAL -> changes.add("PM2.5 alto")
                        MetricLevel.WARNING -> changes.add("PM2.5 moderado")
                        MetricLevel.GOOD -> changes.add("PM2.5 excelente")
                    }
                }
                if (newState.temp != previousState!!.temp) {
                    if (newState.temp != MetricLevel.GOOD) changes.add("Temperatura inusual")
                    else changes.add("Temperatura óptima")
                }
                if (newState.hum != previousState!!.hum) {
                    if (newState.hum != MetricLevel.GOOD) changes.add("Humedad inusual")
                    else changes.add("Humedad ideal")
                }

                if (changes.isNotEmpty()) {
                    playBellSound(context)
                    activeAlert = listOf(newState.pm, newState.temp, newState.hum).maxByOrNull { it.weight } ?: MetricLevel.GOOD
                    alertMessage = changes.joinToString(" • ")
                }
            }
            previousState = newState
        }
    }

    LaunchedEffect(activeAlert) {
        if (activeAlert != null) {
            delay(6000)
            activeAlert = null
        }
    }

    Scaffold(
        modifier = modifier.qhawaScreenBackground(),
        containerColor = Color.Transparent,
        topBar = {
            QhawaTopBar(tabs[selected], !demonstration && realState.connection == DeviceConnection.CONNECTED,
                userName, userEmail,
                onNotificationsClick = { notice = realState.detail },
                onSignOutClick = onSignOutClick)
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White.copy(alpha = 0.88f), tonalElevation = 0.dp) {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(selected = selected == index, onClick = { selected = index },
                        icon = { Icon(tabIcons[index], contentDescription = null) }, label = { Text(title) })
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            AnimatedVisibility(
                visible = activeAlert != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
            ) {
                activeAlert?.let { level ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = 3 // Consejos
                                activeAlert = null
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = level.color),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Column(Modifier.weight(1f)) {
                                Text(level.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(alertMessage, style = MaterialTheme.typography.bodySmall, color = Color.White)
                                Text("Toca aquí para actuar ➔", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }

            if (selected == 1) {
                HistoryScreen(
                    readings = readings,
                    demo = demonstration,
                    now = now,
                    environment = realState,
                    onDemoChange = { demonstration = it },
                )
            } else if (selected == 2 || selected == 3) {
                WellnessRoute(
                    selectedTab = selected,
                    userKey = "${if (isDemoSession) "demo" else "account"}:$userId",
                    reading = currentReading,
                    demo = demonstration,
                    alert = if (activeAlert != null && activeAlert != MetricLevel.GOOD) alertMessage else null,
                    onHistory = { selected = 1 },
                )
            } else Column(Modifier.widthIn(max = 620.dp).fillMaxWidth().verticalScroll(rememberScrollState(selected)).padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isDemoSession) Text("SESIÓN DE DEMOSTRACIÓN · Almacenamiento local", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                // Una instancia estable acompaña las cuatro vistas y anima los cambios de pose.
                QhawaHomeCompanion(
                    selectedTab = selected,
                    reading = currentReading,
                    connection = realState.connection,
                    demo = demonstration,
                    now = now,
                    habitDone = false,
                    adviceDone = false,
                    alert = if (activeAlert != null && activeAlert != MetricLevel.GOOD) alertMessage else null,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Explorar datos de ejemplo", style = MaterialTheme.typography.labelLarge)
                        Text(if (demonstration) "Ejemplos locales · MQTT sigue disponible al desactivarlos" else "Wokwi en vivo · Datos simulados recibidos por MQTT", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = demonstration, onCheckedChange = { demonstration = it }, modifier = Modifier.semantics { contentDescription = "Explorar datos de ejemplo" })
                }
                if (!demonstration) {
                    Panel("Wokwi · MQTT", Icons.Default.Wifi) {
                        Text(realState.detail, style = MaterialTheme.typography.bodyMedium)
                        Text("Broker: ${if (realState.brokerConnected) "conectado" else "sin conexión"} · ${WokwiMqtt.HOST}:1883", style = MaterialTheme.typography.labelSmall)
                        Text("Potenciómetro → PM2.5 · DHT22 → temperatura y humedad. Simulación, no mediciones de tu hogar.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                when (selected) {
                    0 -> Overview(userName, readings.lastOrNull(), realState.connection, demonstration, now, { selected = 1 }, { selected = 3 })
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    notice?.let { message ->
        AlertDialog(onDismissRequest = { notice = null }, title = { Text("Avisos de Qhawa") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { notice = null }) { Text("Entendido") } })
    }
}

@Composable
private fun Heading(eyebrow: String, title: String, subtitle: String) {
    Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Panel(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
            content()
        }
    }
}

@Composable
private fun Overview(name: String?, reading: EnvironmentalReading?, connection: DeviceConnection, demo: Boolean, now: Long, history: () -> Unit, advice: () -> Unit) {
    Heading("01 · Medir", "Hola, ${name?.substringBefore(' ')?.takeIf { it.isNotBlank() } ?: "bienvenido"}", "Conoce el ambiente de tu hogar.")

    val old = reading != null && (now - reading.timestamp > WokwiMqtt.STALE_MS || connection != DeviceConnection.CONNECTED)

    val status = when {
        demo -> "Vista de demostración"
        connection == DeviceConnection.LOADING -> "Cargando lecturas"
        connection == DeviceConnection.DISCONNECTED -> "Dispositivo desconectado"
        connection == DeviceConnection.WAITING -> "Esperando lecturas de Wokwi"
        reading == null -> "Esperando lecturas"
        old -> "Lectura desactualizada"
        reading.simulated -> "Wokwi en vivo · Simulación"
        else -> "Lecturas disponibles"
    }

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (connection == DeviceConnection.LOADING && !demo) {
                CircularProgressIndicator(Modifier.size(24.dp))
            } else {
                Icon(if (connection == DeviceConnection.CONNECTED || demo) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(status, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (reading == null) "Última lectura: sin registros" else "${if (demo) "Ejemplo" else "Recibido en el teléfono"}: ${timestamp(reading.timestamp)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    val pmValue = reading?.pm
    val pmAnimated by animateFloatAsState(targetValue = pmValue?.toFloat() ?: 0f, label = "pm")
    val pmString = pmValue?.let { String.format(Locale.getDefault(), "%.1f", pmAnimated) } ?: "—"
    val pmStatus = pmValue?.let { if (it <= 12) "Excelente" else if (it <= 35) "Moderado" else "Malo" } ?: "—"

    val tempValue = reading?.temperature
    val tempAnimated by animateFloatAsState(targetValue = tempValue?.toFloat() ?: 0f, label = "temp")
    val tempString = tempValue?.let { String.format(Locale.getDefault(), "%.1f", tempAnimated) } ?: "—"
    val tempStatus = tempValue?.let { if (it < 18) "Frío" else if (it <= 25) "Óptimo" else "Cálido" } ?: "—"

    val humValue = reading?.humidity
    val humAnimated by animateFloatAsState(targetValue = humValue?.toFloat() ?: 0f, label = "hum")
    val humString = humValue?.let { String.format(Locale.getDefault(), "%.1f", humAnimated) } ?: "—"
    val humStatus = humValue?.let { if (it < 30) "Seco" else if (it <= 60) "Ideal" else "Húmedo" } ?: "—"

    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text("MÉTRICAS EN VIVO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        val timeAgo = if (reading == null) "Sin datos" else {
            val diffMs = now - reading.timestamp
            val mins = diffMs / 60_000
            if (mins < 1) "Actualizado ahora" else "hace $mins min"
        }
        Text(timeAgo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }

    val metrics = listOf(
        MetricCardData(tempString, "°C", "Temperatura", Icons.Default.DeviceThermostat, tempStatus, Color(0xFFF57C00)),
        MetricCardData(humString, "%", "Humedad", Icons.Default.Opacity, humStatus, Color(0xFF29B6F6)),
        MetricCardData(pmString, "µg/m³", reading?.pmLabel ?: "PM 2.5", Icons.Default.Air, pmStatus, Color(0xFF66BB6A)),
    )
    // Las tres métricas comparten el ancho disponible. Con texto grande se apilan
    // para conservar etiquetas, valores y unidades sin recortes ni scroll horizontal.
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 300.dp || fontScale > 1.2f) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                metrics.forEach { MetricCard(it, Modifier.fillMaxWidth(), expanded = true) }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                metrics.forEach { MetricCard(it, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }

    if (!demo && reading != null) {
        val hasSimulation = reading.simulated
        val hasDemoState = reading.demoState != null
        val hasMissingSensors = reading.temperature == null || reading.humidity == null

        if (hasSimulation || hasDemoState || hasMissingSensors) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (hasSimulation) {
                        Text("PM2.5 simulado por el potenciómetro de Wokwi (0–150 µg/m³).", style = MaterialTheme.typography.labelMedium)
                    }
                    if (hasDemoState) {
                        Text("LED de Wokwi: ${reading.demoState} · Secuencia ${reading.sequence}", style = MaterialTheme.typography.labelMedium)
                        Text("Límites demo: verde <25, amarillo 25–<75, rojo ≥75. No son una clasificación sanitaria.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (hasMissingSensors) {
                        Text("DHT22: sin lectura válida. Temperatura y humedad se muestran como —.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    } else if (!demo) {
        Text("Esperando el valor publicado por Wokwi.", style = MaterialTheme.typography.bodySmall)
    } else {
        Text("Ejemplo local. No corresponde a tu hogar.", style = MaterialTheme.typography.bodySmall)
    }

    Button(onClick = history, modifier = Modifier.fillMaxWidth()) { Text("Comprender · Ver historial") }
    OutlinedButton(onClick = advice, modifier = Modifier.fillMaxWidth()) { Text("Actuar · Ver consejos") }
}

private enum class MetricLevel(val color: Color, val title: String, val weight: Int) {
    GOOD(Color(0xFF2E7D32), "¡El ambiente ha mejorado!", 0),
    WARNING(Color(0xFFF57C00), "Cambio Ambiental", 1),
    CRITICAL(Color(0xFFC62828), "¡Alerta Ambiental!", 2)
}

private data class AlertState(val pm: MetricLevel, val temp: MetricLevel, val hum: MetricLevel)

private fun getPmLevel(pm: Double?) = if (pm == null) MetricLevel.GOOD else if (pm <= 12) MetricLevel.GOOD else if (pm <= 35) MetricLevel.WARNING else MetricLevel.CRITICAL
private fun getTempLevel(temp: Double?) = if (temp == null) MetricLevel.GOOD else if (temp in 18.0..25.0) MetricLevel.GOOD else MetricLevel.WARNING
private fun getHumLevel(hum: Double?) = if (hum == null) MetricLevel.GOOD else if (hum in 30.0..60.0) MetricLevel.GOOD else MetricLevel.WARNING

private fun playBellSound(context: android.content.Context) {
    try {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(context, uri)
        r.play()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private data class MetricCardData(
    val value: String,
    val unit: String,
    val label: String,
    val icon: ImageVector,
    val status: String,
    val accentColor: Color,
)

@Composable
private fun MetricCard(metric: MetricCardData, modifier: Modifier = Modifier, expanded: Boolean = false) {
    val statusColor = when (metric.status) {
        "—" -> MaterialTheme.colorScheme.onSurfaceVariant
        "Malo", "Alto", "Frío", "Seco", "Cálido", "Húmedo" -> Color(0xFFAC3028)
        "Moderado" -> Color(0xFF805400)
        else -> Color(0xFF246540)
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(shape = RoundedCornerShape(10.dp), color = metric.accentColor.copy(alpha = 0.12f)) {
                Icon(metric.icon, null, tint = metric.accentColor, modifier = Modifier.padding(6.dp).size(20.dp))
            }
            Text(metric.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (expanded) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(metric.value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(metric.unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column {
                    Text(metric.value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(metric.unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(metric.status, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.SemiBold)
        }
    }
}
