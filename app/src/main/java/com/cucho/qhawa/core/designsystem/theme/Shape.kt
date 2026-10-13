package com.cucho.qhawa.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Esquinas del diseño:
 * - medium (12): botones y campos de texto
 * - large (16): tarjetas
 * - extraLarge (24): tarjeta grande de cada pantalla
 * Para etiquetas y chips redondos del todo se usa CircleShape.
 */
val QhawaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
