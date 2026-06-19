package com.scanni.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.scanni.app.R

// v1.4 type — the redesign calls for Plus Jakarta Sans (Display 800/-0.5, Title
// 700, Body 500, Overline 700 caps). Until the Plus Jakarta Sans .ttf weights are
// bundled in res/font, we keep IBM Plex Sans Arabic (which also covers Arabic) and
// drive the new *scale*; swapping the typeface is then a one-line change here.
//
// TODO(v1.4): add plus_jakarta_sans_{medium,semibold,bold,extrabold}.ttf and point
// ScanniFontFamily at them (weights 500/600/700/800). Keep an Arabic family for
// Arabic content since Plus Jakarta Sans has no Arabic glyphs.
val ScanniFontFamily = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold),
)

private fun scanni(
    weight: FontWeight,
    size: Int,
    lineHeight: Int = (size * 1.3).toInt(),
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = ScanniFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

// Weight aliases matching the redesign. ExtraBold (800) degrades to Bold on the
// interim IBM Plex family and renders true once Plus Jakarta Sans is bundled.
private val Display = FontWeight.W800
private val Strong = FontWeight.W700
private val Medium = FontWeight.W500

val ScanniTypography = Typography(
    // Hero / screen titles — "Scanni", "Settings", "Your documents".
    headlineLarge = scanni(Display, 30, 36, -0.5),
    headlineMedium = scanni(Display, 25, 30, -0.5),
    headlineSmall = scanni(Display, 21, 27, -0.4),
    // Bar titles and prominent card titles.
    titleLarge = scanni(Strong, 20, 26, -0.2),
    titleMedium = scanni(Strong, 17, 22, -0.1),
    titleSmall = scanni(Strong, 15, 19),
    // Body & control labels sit at Medium per the redesign.
    bodyLarge = scanni(Medium, 15, 21),
    bodyMedium = scanni(Medium, 14, 20),
    bodySmall = scanni(Medium, 12, 16),
    // Chip/button labels (700) and the uppercase section overline (700 · +0.9).
    labelLarge = scanni(Strong, 14, 18),
    labelMedium = scanni(FontWeight.W600, 13, 16, 0.2),
    labelSmall = scanni(Strong, 12, 16, 0.9),
)
