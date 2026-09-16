package com.posepilot.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Monochrome "viewfinder" palette: the camera image supplies all the colour.
 * State is shown by shape and inversion (white pill = ready) rather than hue.
 */
object Pp {
    val Ink = Color(0xFF000000)
    val Paper = Color(0xFFFFFFFF)
    val Graphite = Color(0xFF1C1C1C)
    val Line = Color(0xFF333333)
    val Smoke = Color(0xFF9A9A9A)
    val Scrim = Color(0xB3000000)
    val Ghost = Color(0x99FFFFFF)
}

private val scheme = darkColorScheme(
    primary = Pp.Paper, onPrimary = Pp.Ink,
    secondary = Pp.Smoke, onSecondary = Pp.Ink,
    background = Pp.Ink, onBackground = Pp.Paper,
    surface = Pp.Ink, onSurface = Pp.Paper,
    surfaceVariant = Pp.Graphite, onSurfaceVariant = Pp.Smoke,
    surfaceContainer = Pp.Graphite, surfaceContainerHigh = Pp.Graphite, surfaceContainerLow = Pp.Ink,
    outline = Pp.Line, outlineVariant = Pp.Line,
    error = Pp.Paper, onError = Pp.Ink,
)

private val sans = FontFamily.SansSerif
private val typography = Typography(
    displayLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 46.sp, lineHeight = 48.sp, letterSpacing = (-1.5).sp),
    headlineMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp),
)

@Composable
fun PosePilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
