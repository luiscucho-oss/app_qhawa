package com.cucho.qhawa.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.cucho.qhawa.R
import com.cucho.qhawa.core.designsystem.component.QhawaTopBar
import com.cucho.qhawa.core.designsystem.theme.AccentBlue
import com.cucho.qhawa.core.designsystem.theme.AccentGreen
import com.cucho.qhawa.core.designsystem.theme.AccentOrange
import com.cucho.qhawa.core.designsystem.theme.ErrorRed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

private val tabs = listOf("Inicio", "Historial", "Mascotas", "Consejos")
private val tabIcons = listOf(QhawaIconName.Home, QhawaIconName.History, QhawaIconName.Paw, QhawaIconName.Tips)

private enum class QhawaIconName { Home, History, Paw, Tips, Wifi, Room, Leaf, Temperature, Humidity, Air, Fan, Check, Trend, Heart, Timer, Bolt, Dust, Download, ChevronDown, Filter, Bookmark }

private data class AdviceGuide(
    val category: String,
    val title: String,
    val description: String,
    val icon: QhawaIconName,
)

@Composable
fun HomeScreen(
    userName: String?,
    userEmail: String?,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        modifier = modifier,
        topBar = {
            QhawaTopBar(
                screenTitle = tabs[selectedTab],
                isDeviceOnline = true,
                userName = userName,
                userEmail = userEmail,
                onNotificationsClick = {},
                onSignOutClick = onSignOutClick,
            )
        },
        bottomBar = { HomeBottomBar(selectedTab, onSelect = { selectedTab = it }) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
        ) {
            when (selectedTab) {
                0 -> InicioContent(userName, onHistoryClick = { selectedTab = 1 }, onPetsClick = { selectedTab = 2 })
                1 -> HistoryContent()
                2 -> PetsContent()
                else -> TipsContent()
            }
        }
    }
}

@Composable
private fun HomeBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            tabs.forEachIndexed { index, label ->
                val active = index == selected
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(index) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    QhawaIcon(tabIcons[index], tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(23.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun InicioContent(userName: String?, onHistoryClick: () -> Unit, onPetsClick: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Hola, ${userName?.substringBefore(' ')?.takeIf(String::isNotBlank) ?: "Familia Morales"}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                    Spacer(Modifier.width(5.dp))
                    Text("Dispositivo Sala · Conectado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Pill("98% señal", MaterialTheme.colorScheme.secondaryContainer, QhawaIconName.Wifi)
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        QhawaIcon(QhawaIconName.Room, MaterialTheme.colorScheme.primary, Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Sala Principal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cambiar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        QhawaIcon(QhawaIconName.ChevronDown, MaterialTheme.colorScheme.primary, Modifier.size(15.dp))
                    }
                }
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerLowest,
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .48f),
                                ),
                            ),
                        ),
                ) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Pill("Calidad Óptima", MaterialTheme.colorScheme.secondaryContainer, QhawaIconName.Leaf)
                            Text("Modo Activo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier.fillMaxWidth(.82f)
                                .height(132.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFBDEAFF))
                                .padding(4.dp),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.qhawa_mascot_placeholder),
                                contentDescription = stringResource(R.string.mascot_description),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Pill("Calidad del Aire: Excelente", MaterialTheme.colorScheme.secondaryContainer, QhawaIconName.Leaf)
                        Text("El aire está limpio y fresco en la sala principal. Condiciones óptimas para tu familia y mascotas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("MÉTRICAS EN VIVO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            Text("Actualizado hace 2 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(QhawaIconName.Temperature, "Temperatura", "24.5", "°C", "Agradable", Modifier.weight(1f))
            MetricCard(QhawaIconName.Humidity, "Humedad", "56", "%", "Ideal", Modifier.weight(1f))
            MetricCard(QhawaIconName.Air, "PM 2.5", "18", "µg/m³", "Excelente", Modifier.weight(1f))
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                QhawaIcon(QhawaIconName.Fan, MaterialTheme.colorScheme.primary, Modifier.size(25.dp)); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text("Ventilación adecuada", fontWeight = FontWeight.SemiBold); Text("Flujo pasivo correcto · Todo en orden", style = MaterialTheme.typography.bodySmall) }
                QhawaIcon(QhawaIconName.Check, MaterialTheme.colorScheme.secondary, Modifier.size(22.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionCard(QhawaIconName.Trend, "Ver Historial", "Tendencias de las últimas 24 h", Modifier.weight(1f), onHistoryClick)
            ActionCard(QhawaIconName.Paw, "Cuidados Mascota", "Nivel de alérgenos bajo", Modifier.weight(1f), onPetsClick)
        }
    }
}

@Composable
private fun HistoryContent() {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("Hoy", "Semana", "Mes").forEachIndexed { index, label ->
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                        .background(if (index == 1) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = if (index == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (index == 1) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.qhawa_mascot_placeholder), contentDescription = stringResource(R.string.mascot_description), contentScale = ContentScale.Crop, modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                        Spacer(Modifier.width(5.dp))
                        Text("MONITOR PRINCIPAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Excelente", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(5.dp))
                        Text("(19 µg/m³)", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 3.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        QhawaIcon(QhawaIconName.Leaf, MaterialTheme.colorScheme.secondary, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Aire limpio y saludable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                        QhawaIcon(QhawaIconName.Air, MaterialTheme.colorScheme.secondary, Modifier.padding(7.dp).size(22.dp))
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Calidad del aire hoy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Nivel de pureza durante el día", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Pill("Óptimo", MaterialTheme.colorScheme.secondaryContainer)
                }
                Row(Modifier.padding(top = 12.dp, start = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                    Spacer(Modifier.width(5.dp)); Text("Verde = Aire puro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(13.dp))
                    Box(Modifier.size(9.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                    Spacer(Modifier.width(5.dp)); Text("Amarillo = Aceptable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(16.dp))
                AirChart(Modifier.fillMaxWidth().height(126.dp))
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("Mañana", "Mediodía", "Tarde", "Noche").forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(QhawaIconName.Temperature, "Temperatura", "24.5", "°C", "Agradable", Modifier.weight(1f))
            MetricCard(QhawaIconName.Humidity, "Humedad", "56", "%", "Ideal", Modifier.weight(1f))
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp)) {
                    QhawaIcon(QhawaIconName.Check, MaterialTheme.colorScheme.secondary, Modifier.padding(9.dp).size(24.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("RESUMEN DE LA SEMANA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Text("6 de 7 días con aire puro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tu hogar mantuvo una ventilación saludable toda la semana.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            QhawaIcon(QhawaIconName.Download, MaterialTheme.colorScheme.onPrimary, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Descargar reporte simple")
        }
    }
}

@Composable
private fun PetsContent() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("SALUD AMBIENTAL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Bienestar de Mascotas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Pill("Modo Peludo Activo", MaterialTheme.colorScheme.secondaryContainer) }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.qhawa_mascot_placeholder), contentDescription = stringResource(R.string.mascot_description), modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(18.dp)))
                Spacer(Modifier.height(9.dp)); Pill("Ambiente seguro para tus mascotas", MaterialTheme.colorScheme.secondaryContainer, QhawaIconName.Heart)
                Text("¡Qhawa está feliz!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Text("Los niveles de polvo y partículas están en mínimos. Los ojos y vías respiratorias de tu peludo están protegidos hoy.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp)); Row(verticalAlignment = Alignment.CenterVertically) { QhawaIcon(QhawaIconName.Heart, MaterialTheme.colorScheme.secondary, Modifier.size(17.dp)); Text("  Estrés respiratorio  0%     ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary); QhawaIcon(QhawaIconName.Fan, MaterialTheme.colorScheme.primary, Modifier.size(17.dp)); Text("  Ventilación ideal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary) }
            }
        }
        Text("Indicadores de sensibilidad", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        InfoCard(QhawaIconName.Dust, "Sensibilidad al polvo", "Partículas PM2.5 y PM10 controladas", "Nivel Muy Bajo")
        InfoCard(QhawaIconName.Temperature, "Confort térmico", "Perfecto para perros y gatos de pelaje", "24.5°C")
        InfoCard(QhawaIconName.Humidity, "Humedad ambiental", "Previene resequedad en pelaje y ojos", "56%")
    }
}

@Composable
private fun TipsContent() {
    var selectedGuideCategory by rememberSaveable { mutableStateOf("Todos") }
    val guideCategories = listOf("Todos", "Cocina", "Plantas", "Limpieza", "Humedad")
    val guides = listOf(
        AdviceGuide("Cocina", "Enciende el extractor al cocinar", "Reduce el humo y las partículas en el ambiente.", QhawaIconName.Fan),
        AdviceGuide("Plantas", "Añade una planta purificadora", "Algunas plantas ayudan a mantener un hogar más fresco.", QhawaIconName.Leaf),
        AdviceGuide("Limpieza", "Elige productos de limpieza suaves", "Evita fragancias fuertes y aerosoles en espacios cerrados.", QhawaIconName.Check),
        AdviceGuide("Humedad", "Ventila los espacios húmedos", "Ayuda a prevenir la acumulación de humedad.", QhawaIconName.Humidity),
    )
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                QhawaIcon(QhawaIconName.Tips, MaterialTheme.colorScheme.secondary, Modifier.size(34.dp)); Spacer(Modifier.width(12.dp)); Column { Text("CONSEJOS DE QHAWA  ·  En vivo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary); Text("Acciones personalizadas para la calidad actual de tu aire", fontWeight = FontWeight.SemiBold) }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row { Text("Meta semanal\nHábitos aplicados", modifier = Modifier.weight(1f)); Text("3 / 5", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp)); Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)) { Box(Modifier.fillMaxWidth(.6f).height(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary)) }
                Spacer(Modifier.height(8.dp)); Text("¡Excelente! Has reducido un 18% los alérgenos ambientales.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(Modifier.padding(14.dp)) {
                Pill("Acción inmediata recomendada", MaterialTheme.colorScheme.secondaryContainer, QhawaIconName.Bolt)
                Text("Ventila la habitación 15 min", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                Text("El aire exterior está limpio hoy. Abrir las ventanas matutinas renovará el oxígeno y reducirá el dióxido de carbono (CO₂) acumulado mientras duermes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp)); Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { QhawaIcon(QhawaIconName.Timer, MaterialTheme.colorScheme.onPrimary, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Iniciar recordatorio de 15 min") }
                Row(Modifier.align(Alignment.CenterHorizontally).padding(8.dp), verticalAlignment = Alignment.CenterVertically) { QhawaIcon(QhawaIconName.Check, MaterialTheme.colorScheme.secondary, Modifier.size(16.dp)); Text("  Hecho", color = MaterialTheme.colorScheme.secondary) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            QhawaIcon(QhawaIconName.Filter, MaterialTheme.colorScheme.primary, Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
            Text("Guías por Categoría", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("4 disponibles", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            guideCategories.forEach { category ->
                val selected = category == selectedGuideCategory
                Text(
                    text = category,
                    modifier = Modifier.clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable { selectedGuideCategory = category }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        guides.filter { selectedGuideCategory == "Todos" || it.category == selectedGuideCategory }
            .forEach { guide ->
                GuideCard(guide)
        }
    }
}

@Composable
private fun GuideCard(guide: AdviceGuide) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column(Modifier.fillMaxWidth().padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                    QhawaIcon(guide.icon, MaterialTheme.colorScheme.primary, Modifier.padding(9.dp).size(21.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(guide.category.uppercase(), modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 7.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text("Impacto Alto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                    }
                }
                QhawaIcon(QhawaIconName.Bookmark, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp))
            }
            Text(guide.title, modifier = Modifier.padding(start = 1.dp, top = 9.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(guide.description, modifier = Modifier.padding(start = 1.dp, top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AirChart(modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.secondary
    val pointFill = MaterialTheme.colorScheme.surface
    val chartBackground = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .55f)
    Canvas(modifier.clip(RoundedCornerShape(12.dp)).background(chartBackground)) {
        val points = listOf(.0f to .68f, .18f to .72f, .36f to .53f, .52f to .34f, .68f to .45f, .83f to .72f, 1f to .42f)
        val path = Path().apply { points.forEachIndexed { i, p -> val x = p.first * size.width; val y = p.second * size.height; if (i == 0) moveTo(x, y) else lineTo(x, y) } }
        drawPath(path, lineColor, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        points.forEach { (x, y) -> drawCircle(pointFill, 5.dp.toPx(), Offset(x * size.width, y * size.height)); drawCircle(lineColor, 3.dp.toPx(), Offset(x * size.width, y * size.height)) }
    }
}

@Composable
private fun MetricCard(icon: QhawaIconName, label: String, value: String, unit: String, note: String, modifier: Modifier = Modifier) {
    val statusColor = metricStatusColor(icon, value.toFloatOrNull())
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(statusColor))
            Column(Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QhawaIcon(icon, MaterialTheme.colorScheme.primary, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.Bottom) { Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text(unit, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 4.dp)) }
                Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

private fun metricStatusColor(icon: QhawaIconName, value: Float?): Color {
    if (value == null) return AccentBlue
    return when (icon) {
        QhawaIconName.Temperature -> when {
            value < 18f -> AccentBlue
            value <= 27f -> AccentGreen
            value <= 32f -> AccentOrange
            else -> ErrorRed
        }
        QhawaIconName.Humidity -> when {
            value < 30f -> AccentOrange
            value <= 60f -> AccentBlue
            value <= 75f -> AccentOrange
            else -> ErrorRed
        }
        QhawaIconName.Air -> when {
            value <= 35f -> AccentGreen
            value <= 55f -> AccentOrange
            else -> ErrorRed
        }
        else -> AccentGreen
    }
}

@Composable
private fun Pill(text: String, color: androidx.compose.ui.graphics.Color, icon: QhawaIconName? = null) {
    Row(
        modifier = Modifier.clip(CircleShape).background(color).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            QhawaIcon(icon, MaterialTheme.colorScheme.primary, Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ActionCard(icon: QhawaIconName, title: String, detail: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) { QhawaIcon(icon, MaterialTheme.colorScheme.primary, Modifier.size(22.dp)); Text(title, fontWeight = FontWeight.SemiBold); Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun InfoCard(icon: QhawaIconName, title: String, detail: String, badge: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            QhawaIcon(icon, MaterialTheme.colorScheme.primary, Modifier.size(22.dp)); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun QhawaIcon(name: QhawaIconName, tint: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val image = when (name) {
        QhawaIconName.Home -> Icons.Filled.Home
        QhawaIconName.History -> Icons.Filled.History
        QhawaIconName.Paw -> Icons.Filled.Pets
        QhawaIconName.Tips -> Icons.Filled.Lightbulb
        QhawaIconName.Wifi -> Icons.Filled.Wifi
        QhawaIconName.Room -> Icons.Filled.Place
        QhawaIconName.Leaf -> Icons.Filled.Eco
        QhawaIconName.Temperature -> Icons.Filled.Thermostat
        QhawaIconName.Humidity -> Icons.Filled.WaterDrop
        QhawaIconName.Air, QhawaIconName.Fan -> Icons.Filled.Air
        QhawaIconName.Check -> Icons.Filled.Check
        QhawaIconName.Trend -> Icons.Filled.TrendingUp
        QhawaIconName.Heart -> Icons.Filled.Favorite
        QhawaIconName.Timer -> Icons.Filled.Schedule
        QhawaIconName.Bolt -> Icons.Filled.Bolt
        QhawaIconName.Dust -> Icons.Filled.Grain
        QhawaIconName.Download -> Icons.Filled.FileDownload
        QhawaIconName.ChevronDown -> Icons.Filled.ExpandMore
        QhawaIconName.Filter -> Icons.Filled.FilterList
        QhawaIconName.Bookmark -> Icons.Filled.BookmarkBorder
    }
    Icon(imageVector = image, contentDescription = null, tint = tint, modifier = modifier)
}
