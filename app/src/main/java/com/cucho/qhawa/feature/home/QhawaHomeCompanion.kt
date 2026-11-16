package com.cucho.qhawa.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cucho.qhawa.core.designsystem.component.QhawaMascotComponent
import com.cucho.qhawa.core.designsystem.component.QhawaMascotState
import com.cucho.qhawa.data.habits.PmAdviceLevel

private data class AirDialogue(
    val text: String,
    val background: Color,
    val textColor: Color,
    val pose: QhawaMascotState,
)

private val neutralText = Color(0xFF162B29)
private val waiting = AirDialogue("Esperando datos del sensor", Color(0xFFECEFF3), neutralText, QhawaMascotState.IDLE)
private val good = AirDialogue("Calidad del aire: buena", Color(0xFFDFF3E8), Color(0xFF135A40), QhawaMascotState.GREETING)
private val moderate = AirDialogue("Calidad del aire: moderada", Color(0xFFFFE7CD), Color(0xFF805400), QhawaMascotState.ALERT)
private val poor = AirDialogue("Calidad del aire: mala", Color(0xFFFCE2E0), Color(0xFF8E2521), QhawaMascotState.ALERT)
private val tips = listOf("Toca Historial para explorar las lecturas.", "Puedes activar los datos de ejemplo al inicio de esta pantalla.")

@Composable
internal fun QhawaHomeCompanion(level: PmAdviceLevel?, demo: Boolean, alert: Boolean) {
    val dialogue = when (level) {
        PmAdviceLevel.GREEN -> good
        PmAdviceLevel.YELLOW -> moderate
        PmAdviceLevel.RED -> poor
        null -> waiting
    }
    var tipIndex by rememberSaveable(level, demo) { mutableIntStateOf(-1) }
    val fontScale = LocalDensity.current.fontScale
    val bubbleColor = Color.White
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("home-air-quality"),
        shape = RoundedCornerShape(24.dp),
        color = dialogue.background,
        contentColor = neutralText,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(12.dp)) {
            val mascotSize = if (fontScale >= 1.5f) 64.dp else (maxWidth * 0.34f).coerceIn(72.dp, 148.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                QhawaMascotComponent(
                    state = if (alert && level != null) QhawaMascotState.ALERT else dialogue.pose,
                    message = "",
                    reactionKey = level,
                    mascotSize = mascotSize,
                    onMascotClick = { tipIndex = (tipIndex + 1) % tips.size },
                )
                Canvas(Modifier.size(width = 10.dp, height = 18.dp)) {
                    val tail = Path().apply {
                        moveTo(0f, size.height / 2)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(tail, bubbleColor)
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = bubbleColor,
                    contentColor = if (demo) neutralText else dialogue.textColor,
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            dialogue.text,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        tips.getOrNull(tipIndex)?.let { tip ->
                            Text(tip, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
