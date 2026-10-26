package com.martenworld.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Dark-only for now. Orange is the single accent; more theming comes later.
object Palette {
    val Orange = Color(0xFFFF8A3D)
    val OrangeDim = Color(0xFFB85E22)
    val OnOrange = Color(0xFF2A1400)
    val OrangeContainer = Color(0xFF4A2A12)
    val OnOrangeContainer = Color(0xFFFFD9C2)

    val Background = Color(0xFF121212)
    val Surface = Color(0xFF161616)
    val SurfaceLow = Color(0xFF1C1C1C)
    val SurfaceHigh = Color(0xFF242424)
    val SurfaceHighest = Color(0xFF2E2E2E)
    val OnSurface = Color(0xFFECE6E1)
    val OnSurfaceVariant = Color(0xFFB4ACA5)
    val Outline = Color(0xFF5A544F)
    val OutlineVariant = Color(0xFF3A3531)

    val Error = Color(0xFFFF6B6B)
    val OnError = Color(0xFF3A0000)
    val ErrorContainer = Color(0xFF5A1A1A)
    val OnErrorContainer = Color(0xFFFFDAD6)
}

private val DarkColors = darkColorScheme(
    primary = Palette.Orange,
    onPrimary = Palette.OnOrange,
    primaryContainer = Palette.OrangeContainer,
    onPrimaryContainer = Palette.OnOrangeContainer,
    secondary = Palette.OrangeDim,
    onSecondary = Palette.OnOrange,
    secondaryContainer = Palette.SurfaceHigh,
    onSecondaryContainer = Palette.OnSurface,
    tertiary = Palette.OnSurfaceVariant,
    onTertiary = Palette.Background,
    background = Palette.Background,
    onBackground = Palette.OnSurface,
    surface = Palette.Surface,
    onSurface = Palette.OnSurface,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.OnSurfaceVariant,
    surfaceContainerLowest = Palette.Background,
    surfaceContainerLow = Palette.SurfaceLow,
    surfaceContainer = Palette.SurfaceLow,
    surfaceContainerHigh = Palette.SurfaceHigh,
    surfaceContainerHighest = Palette.SurfaceHighest,
    outline = Palette.Outline,
    outlineVariant = Palette.OutlineVariant,
    error = Palette.Error,
    onError = Palette.OnError,
    errorContainer = Palette.ErrorContainer,
    onErrorContainer = Palette.OnErrorContainer,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val AppTypography = Typography()

@Composable
fun MartenWorldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
