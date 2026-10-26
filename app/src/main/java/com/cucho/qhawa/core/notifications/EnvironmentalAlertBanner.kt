package com.cucho.qhawa.core.notifications

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun EnvironmentalAlertBanner(alert: EnvironmentalAlert?, onAction: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var displayed by remember { mutableStateOf(alert) }
    LaunchedEffect(alert) { if (alert != null) displayed = alert }
    AnimatedVisibility(visible = alert != null,
        enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut(), modifier = modifier) {
        displayed?.let { item ->
            Card(Modifier.fillMaxWidth().testTag("environment-alert-banner").semantics { liveRegion = LiveRegionMode.Polite },
                shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(item.level.color)),
                elevation = CardDefaults.cardElevation(8.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable(onClickLabel = "Ver consejos", onClick = onAction).padding(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.NotificationsActive, null, tint = Color.White)
                            Text(item.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(item.message, color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Text("Toca para ver consejos", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar alerta", tint = Color.White) }
                }
            }
        }
    }
}
