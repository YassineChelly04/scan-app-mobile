package com.scanni.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scanni.app.domain.model.ThemeMode

// Scanni v1.4 "warm paper" palette — a calm paper-white surface, one confident
// blue and ink-dark text (see the redesign Foundations):
//   Blue #2B63E0 · Tint #EAF0FE · Paper #F6F4F0 · Ink #191B1E · Camera #0E0F12
private val Blue = Color(0xFF2B63E0)
private val BlueTint = Color(0xFFEAF0FE)
private val Paper = Color(0xFFF6F4F0)
private val Ink = Color(0xFF191B1E)
private val Muted = Color(0xFF9AA0A6)
private val Hairline = Color(0xFFECEAE4)        // subtle card / field borders
private val HairlineStrong = Color(0xFFC9C6BE)  // dashed "add" affordances

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BlueTint,
    onPrimaryContainer = Color(0xFF14306B),
    secondary = Color(0xFF41454B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = BlueTint,
    onSecondaryContainer = Color(0xFF14306B),
    tertiary = Color(0xFF2B63E0),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCE7FD),
    onTertiaryContainer = Color(0xFF0E2A66),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFEDE7),
    onSurfaceVariant = Muted,
    // Cards/sheets sit as flat white on paper; depth comes from hairlines + shadow,
    // not tonal fills — so the container roles stay near-white and warm.
    surfaceDim = Color(0xFFE7E4DE),
    surfaceBright = Color(0xFFFCFBF9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCFBF9),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFF1EFEA),
    surfaceTint = Blue,
    outline = HairlineStrong,
    outlineVariant = Hairline,
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF2F3036),
    inverseOnSurface = Color(0xFFF3F1EC),
    inversePrimary = Color(0xFFB1C5FF),
)

// Warm-neutral dark counterpart (the redesign is paper-first; this keeps the same
// blue accent on a soft near-black rather than a blue-tinted dark).
private val DarkColors = darkColorScheme(
    primary = Color(0xFFAFC6FF),
    onPrimary = Color(0xFF06245F),
    primaryContainer = Color(0xFF1F3C7A),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFC6C4BD),
    onSecondary = Color(0xFF2E302E),
    secondaryContainer = Color(0xFF1F3C7A),
    onSecondaryContainer = Color(0xFFDCE6FF),
    tertiary = Color(0xFF8FB4FF),
    onTertiary = Color(0xFF06245F),
    tertiaryContainer = Color(0xFF274A8C),
    onTertiaryContainer = Color(0xFFDCE7FD),
    background = Color(0xFF15161A),
    onBackground = Color(0xFFECEAE4),
    surface = Color(0xFF15161A),
    onSurface = Color(0xFFECEAE4),
    surfaceVariant = Color(0xFF3C3E42),
    onSurfaceVariant = Color(0xFFB6B3AC),
    surfaceDim = Color(0xFF15161A),
    surfaceBright = Color(0xFF393B40),
    surfaceContainerLowest = Color(0xFF0E0F12),
    surfaceContainerLow = Color(0xFF1B1C20),
    surfaceContainer = Color(0xFF1F2024),
    surfaceContainerHigh = Color(0xFF292B2F),
    surfaceContainerHighest = Color(0xFF34363B),
    surfaceTint = Color(0xFFAFC6FF),
    outline = Color(0xFF8E8C85),
    outlineVariant = Color(0xFF44464A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFECEAE4),
    inverseOnSurface = Color(0xFF2F3036),
    inversePrimary = Blue,
)

// Rounded, friendly shapes — soft cards and pill controls per the redesign.
val ScanniShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ScanniTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ScanniTypography,
        shapes = ScanniShapes,
        content = content,
    )
}
