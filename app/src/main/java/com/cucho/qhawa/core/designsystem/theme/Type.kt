package com.cucho.qhawa.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cucho.qhawa.R

/** La app usa una sola fuente: Inter (archivos en res/font). Licencia en docs/licenses/Inter-OFL.txt. */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun style(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/**
 * Escala de textos. Ejemplos del diseño:
 * - headlineLarge: "Estado ambiental", valores grandes (24.5)
 * - headlineMedium: "¡Qhawa está feliz!"
 * - titleMedium: títulos de tarjetas
 * - bodyMedium: textos de párrafo
 * - labelMedium: títulos de sección en mayúsculas ("MÉTRICAS EN VIVO")
 */
val QhawaTypography = Typography(
    displayLarge = style(FontWeight.Bold, 40, 48, -0.5),
    displayMedium = style(FontWeight.Bold, 34, 42, -0.5),
    displaySmall = style(FontWeight.Bold, 30, 38),
    headlineLarge = style(FontWeight.Bold, 28, 36),
    headlineMedium = style(FontWeight.Bold, 24, 32),
    headlineSmall = style(FontWeight.SemiBold, 20, 28),
    titleLarge = style(FontWeight.SemiBold, 20, 28),
    titleMedium = style(FontWeight.SemiBold, 16, 24),
    titleSmall = style(FontWeight.SemiBold, 14, 20),
    bodyLarge = style(FontWeight.Normal, 16, 24),
    bodyMedium = style(FontWeight.Normal, 14, 20),
    bodySmall = style(FontWeight.Normal, 12, 16),
    labelLarge = style(FontWeight.SemiBold, 14, 20),
    labelMedium = style(FontWeight.SemiBold, 12, 16, 0.5),
    labelSmall = style(FontWeight.Medium, 11, 16, 0.5),
)
