package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VidaQRLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = RescueCoral,
    onSecondary = Color.White,
    secondaryContainer = RescueCoralContainer,
    onSecondaryContainer = OnRescueCoralContainer,
    tertiary = GuatemalaJade,
    onTertiary = Color.White,
    tertiaryContainer = GuatemalaJadeContainer,
    onTertiaryContainer = Color(0xFF064E3B),
    background = AlabasterBg,
    onBackground = InkDark,
    surface = CrispSurface,
    onSurface = InkDark,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = SlateOutline,
    error = RescueCoralDark,
    onError = Color.White
)

private val VidaQRDarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = Color(0xFF00373C),
    primaryContainer = EmeraldDarkContainer,
    onPrimaryContainer = Color(0xFFB5F2F6),
    secondary = RescueDarkCoral,
    onSecondary = Color(0xFF5F0A12),
    secondaryContainer = RescueDarkContainer,
    onSecondaryContainer = Color(0xFFFFDAD9),
    tertiary = GuatemalaJade,
    onTertiary = Color(0xFF003824),
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = GuatemalaJadeContainer,
    background = SlateDarkBg,
    onBackground = InkLight,
    surface = SlateDarkSurface,
    onSurface = InkLight,
    surfaceVariant = SlateDarkVariant,
    onSurfaceVariant = InkLightMuted,
    outline = Color(0xFF334155),
    error = RescueDarkCoral,
    onError = Color(0xFF5F0A12)
)

val VidaQRShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun VidaQRTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VidaQRDarkColorScheme else VidaQRLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = VidaQRTypography,
        shapes = VidaQRShapes,
        content = content
    )
}
