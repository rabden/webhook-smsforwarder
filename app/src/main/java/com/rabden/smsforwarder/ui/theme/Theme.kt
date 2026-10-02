package com.rabden.smsforwarder.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Material 3 Expressive Shapes
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentNeon,
    onPrimary = Void,
    primaryContainer = AccentSubtle,
    onPrimaryContainer = AccentNeon,
    secondary = TextSecondary,
    onSecondary = Void,
    secondaryContainer = SubtleSurface,
    onSecondaryContainer = TextPrimary,
    tertiary = Color(0xFFA5B4FC), // Indigo accent
    onTertiary = Void,
    tertiaryContainer = Color(0xFF312E81).copy(alpha = 0.35f),
    onTertiaryContainer = Color(0xFFC7D2FE),
    background = Void,
    surface = DarkSurface,
    surfaceVariant = ElevatedSurface,
    surfaceContainerLowest = Color(0xFF050505),
    surfaceContainerLow = Void,
    surfaceContainer = ElevatedSurface,
    surfaceContainerHigh = SubtleSurface,
    surfaceContainerHighest = Color(0xFF262626),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Void,
    errorContainer = Color(0xFF7F1D1D).copy(alpha = 0.4f),
    onErrorContainer = Color(0xFFFECACA),
    outline = TextMuted,
    outlineVariant = Color(0xFF27272A)
)

private val LightColorScheme = lightColorScheme(
    primary = AccentNeon,
    onPrimary = Void,
    primaryContainer = AccentSubtle,
    onPrimaryContainer = AccentNeon,
    secondary = TextSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFF6366F1),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEEF2FF),
    onTertiaryContainer = Color(0xFF3730A3),
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = LightBackground,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceVariant,
    surfaceContainerHighest = Color(0xFFE2E8F0),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun SmsForwarderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ExpressiveShapes,
        content = content
    )
}
