package com.cucho.qhawa.feature.home

import androidx.compose.animation.core.animateFloatAsState
import android.Manifest
import android.os.Build
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.zIndex
import com.cucho.qhawa.core.notifications.*
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
    notificationRequest: Long = 0,
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
    val alertEngine = remember { EnvironmentalAlertEngine() }
    val notifications = remember(context) { EnvironmentalNotifications(context) }
    var notificationStatus by remember { mutableStateOf(notifications.status()) }
    var activeAlert by remember { mutableStateOf<EnvironmentalAlert?>(null) }
    val alertMessage = activeAlert?.message.orEmpty()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationStatus = notifications.status()
    }
    LaunchedEffect(notificationRequest) { if (notificationRequest > 0) selected = 3 }
    LaunchedEffect(realState, demonstration) {
        val event = alertEngine.evaluate(realState, System.currentTimeMillis(), enabled = !demonstration)
        if (demonstration || realState.connection != DeviceConnection.CONNECTED) activeAlert = null
        if (event != null) { activeAlert = event; notifications.post(event) }
    }
    LaunchedEffect(activeAlert?.id) {
        if (activeAlert != null) { delay(6000); activeAlert = null }
    }

    Scaffold(
        modifier = modifier.qhawaScreenBackground(),
        containerColor = Color.Transparent,
        topBar = {
            QhawaTopBar(tabs[selected], !demonstration && realState.connection == DeviceConnection.CONNECTED,
                userName, userEmail,
                onNotificationsClick = { notificationStatus = notifications.status(); notice = realState.detail },
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
                    alert = if (activeAlert != null && activeAlert?.level != AlertLevel.GOOD) alertMessage else null,
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
                    alert = if (activeAlert != null && activeAlert?.level != AlertLevel.GOOD) alertMessage else null,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Explorar datos de ejemplo", style = MaterialTheme.typography.labelLarge)
                        Text(if (demonstration) "Ejemplos locales · MQTT sigue disponible al desactivarlos" else "Wokwi en vivo · Datos simulados recibidos por MQTT", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = demonstration, onCheckedChange = { demonstration = it }, modifier = Modifier.semantics { contentDescription = "Explorar datos de ejemplo" })
                }
                when (selected) {
                    0 -> Overview(userName, readings.lastOrNull(), realState.connection, demonstration, now, { selected = 1 }, { selected = 3 })
                }
                Spacer(Modifier.height(8.dp))
            }
            EnvironmentalAlertBanner(activeAlert, onAction = { selected = 3; activeAlert = null },
                onDismiss = { activeAlert = null }, modifier = Modifier.align(Alignment.TopCenter).zIndex(20f).padding(16.dp))
        }
    }
    notice?.let { message ->
        AlertDialog(onDismissRequest = { notice = null }, title = { Text("Avisos de Qhawa") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message)
                Text(notificationStatus)
                Text("Las alertas se generan mientras Qhawa está abierta y recibe lecturas. Los colores de Wokwi son de demostración.")
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && notifications.permissionRequired()) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }) { Text("Permisos de notificación") }
                OutlinedButton(onClick = {
                    val test = EnvironmentalAlert(System.nanoTime(), AlertLevel.WARNING, "Qhawa · Prueba de notificación", "Prueba de sonido; no es una lectura del ambiente.", true)
                    val posted = notifications.post(test)
                    notificationStatus = if (posted) notifications.status() else "No se pudo enviar: permite las notificaciones en Android."
                    activeAlert = if (posted) test else test.copy(message = "Prueba del banner. Permite las notificaciones de Android para probar el sonido.")
                    notice = null
                }) { Text("Probar notificación y sonido") }
            }
        }, confirmButton = { TextButton(onClick = { notice = null }) { Text("Entendido") } })
    }
}

@Composable
private fun Heading(eyebrow: String, title: String, subtitle: String) {
    Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val pmStatus = if (reading?.simulated == true) reading.demoState ?: "Sin clasificar" else "Sin clasificar"

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

    Button(onClick = history, modifier = Modifier.fillMaxWidth()) { Text("Comprender · Ver historial") }
    OutlinedButton(onClick = advice, modifier = Modifier.fillMaxWidth()) { Text("Actuar · Ver consejos") }
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
        "—", "Sin clasificar" -> MaterialTheme.colorScheme.onSurfaceVariant
        "ROJO", "Malo", "Alto", "Frío", "Seco", "Cálido", "Húmedo" -> Color(0xFFAC3028)
        "AMARILLO", "Moderado" -> Color(0xFF805400)
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
