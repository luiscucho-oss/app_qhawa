package com.cucho.qhawa.feature.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val sourceLabelColor = Color(0xFF536170)

@Composable
internal fun ExampleDataRow(demo: Boolean, onDemoChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (demo) "Datos de ejemplo" else "Prototipo Qhawa · MQTT",
            Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = sourceLabelColor,
        )
        Text("Ejemplos", style = MaterialTheme.typography.labelSmall, color = sourceLabelColor)
        Switch(
            checked = demo,
            onCheckedChange = onDemoChange,
            modifier = Modifier.padding(start = 6.dp).semantics { contentDescription = "Explorar datos de ejemplo" },
        )
    }
}
