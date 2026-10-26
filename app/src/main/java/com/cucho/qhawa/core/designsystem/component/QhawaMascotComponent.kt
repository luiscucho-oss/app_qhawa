package com.cucho.qhawa.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cucho.qhawa.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Poses de Qhawa: reposo, guiño de saludo, felicidad y tristeza (atención/alerta). */
enum class QhawaMascotState(@param:DrawableRes val drawableRes: Int) {
    IDLE(R.drawable.qhawa_mascot_idle),
    GREETING(R.drawable.qhawa_mascot_greeting),
    HAPPY(R.drawable.qhawa_mascot_happy),
    ALERT(R.drawable.qhawa_mascot_alert),
}

/**
 * Mascota sin dependencias de navegación, sensores ni lógica de negocio.
 *
 * El padre controla [state] y [message]; [reactionKey] permite repetir la reacción incluso con
 * el mismo texto. Cada toque avisa inmediatamente mediante [onMascotClick] y muestra HAPPY
 * durante un instante antes de recuperar la pose del padre. Los toques rápidos y los nuevos
 * mensajes cancelan la secuencia anterior: una sola corrutina controla los Animatable.
 * Mantener esta llamada en la misma posición de la composición conserva las transiciones
 * al navegar. [mascotSize] controla la imagen, con espacio adicional para saltar.
 */
@Composable
fun QhawaMascotComponent(
    state: QhawaMascotState,
    message: String,
    modifier: Modifier = Modifier,
    onMascotClick: () -> Unit = {},
    reactionKey: Any? = Unit,
    mascotSize: Dp = 148.dp,
) {
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    var taps by remember { mutableIntStateOf(0) }
    var handledTaps by remember { mutableIntStateOf(0) }
    var reactingToTouch by remember { mutableStateOf(false) }
    val jump = remember { Animatable(0f) } // dp: se convierten en píxeles al dibujar.
    val shake = remember { Animatable(0f) }
    val squeeze = remember { Animatable(1f) }

    LaunchedEffect(state, message, reactionKey, taps) {
        val touched = taps != handledTaps
        handledTaps = taps
        reactingToTouch = touched
        jump.snapTo(0f)
        shake.snapTo(0f)
        squeeze.snapTo(1f)
        try {
            when {
                touched -> coroutineScope {
                    launch {
                        squeeze.animateTo(0.86f, tween(85))
                        squeeze.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                    launch {
                        jump.animateTo(-12f, tween(120))
                        jump.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                }
                state == QhawaMascotState.ALERT -> {
                    for (offset in listOf(-5f, 5f, -4f, 4f, 0f)) {
                        shake.animateTo(offset, tween(65))
                    }
                }
                else -> {
                    jump.animateTo(if (state == QhawaMascotState.HAPPY) -18f else -7f, tween(160))
                    jump.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
            }
            if (touched) delay(650)
        } finally {
            // También se restaura si se navega durante una reacción.
            reactingToTouch = false
        }
    }

    val displayedState = if (reactingToTouch) QhawaMascotState.HAPPY else state
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AnimatedVisibility(visible = message.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
            Surface(
                shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                AnimatedContent(
                    targetState = message,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                    label = "Qhawa dialogue",
                ) { text ->
                    Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Box(
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp).size(mascotSize)
                .semantics {
                    contentDescription = "Qhawa, mascota interactiva"
                    stateDescription = when (displayedState) {
                        QhawaMascotState.IDLE -> "En reposo"
                        QhawaMascotState.GREETING -> "Saludando"
                        QhawaMascotState.HAPPY -> "Feliz"
                        QhawaMascotState.ALERT -> "Atención"
                    }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "Jugar con Qhawa y escuchar otro consejo",
                ) {
                    scope.launch {
                        taps++
                        onMascotClick()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            // La respiración sólo existe en reposo/saludo; las reacciones puntuales la suspenden.
            val breathing = if (!reactingToTouch &&
                (state == QhawaMascotState.IDLE || state == QhawaMascotState.GREETING)
            ) rememberQhawaBreathing() else null
            AnimatedContent(
                targetState = displayedState,
                transitionSpec = {
                    ((fadeIn(tween(260)) + scaleIn(tween(300), initialScale = 0.9f))
                        togetherWith fadeOut(tween(160))).using(SizeTransform(clip = false))
                },
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer {
                    translationY = (jump.value + (breathing?.value ?: 0f)).dp.toPx()
                    translationX = shake.value.dp.toPx()
                    scaleY = squeeze.value
                    scaleX = 2f - squeeze.value
                    transformOrigin = TransformOrigin(0.5f, 1f)
                },
                label = "Qhawa pose",
            ) { pose ->
                Image(
                    painter = painterResource(pose.drawableRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(mascotSize),
                )
            }
        }
    }
}

@Composable
private fun rememberQhawaBreathing(): androidx.compose.runtime.State<Float> {
    val transition = rememberInfiniteTransition(label = "Qhawa breathing")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(tween(1700), RepeatMode.Reverse),
        label = "Qhawa float",
    )
}
