package com.cucho.qhawa.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val QhawaColorScheme = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = BlueContainer,
    onPrimaryContainer = OnBlueContainer,
    secondary = Green,
    onSecondary = Color.White,
    secondaryContainer = GreenContainer,
    onSecondaryContainer = OnGreenContainer,
    tertiary = Orange,
    onTertiary = Color.White,
    tertiaryContainer = OrangeContainer,
    onTertiaryContainer = OnOrangeContainer,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedContainer,
    onErrorContainer = OnErrorRedContainer,
    background = SurfaceBase,
    onBackground = Ink,
    surface = SurfaceBase,
    onSurface = Ink,
    surfaceVariant = SurfaceHighest,
    onSurfaceVariant = InkVariant,
    surfaceContainerLowest = SurfaceLowest,
    surfaceContainerLow = SurfaceLow,
    surfaceContainer = SurfaceMid,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHighest,
    outline = OutlineGray,
    outlineVariant = OutlineGrayVariant,
)

/**
 * Tema de Qhawa. Todas las pantallas se dibujan dentro de él.
 * No usa los colores dinámicos del teléfono, para que la app se vea igual en todos.
 */
@Composable
fun QhawaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QhawaColorScheme,
        typography = QhawaTypography,
        shapes = QhawaShapes,
        content = content,
    )
}
