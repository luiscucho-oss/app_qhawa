package com.cucho.qhawa.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Fondo común, fijo al viewport: verde → verde pastel → azul suave. */
private val QhawaScreenGradient = Brush.verticalGradient(
    0f to Color(0xFF8BD6B2),
    0.46f to Color(0xFFDDF3DF),
    1f to Color(0xFFDCEFFC),
)

fun Modifier.qhawaScreenBackground(): Modifier = background(QhawaScreenGradient)
