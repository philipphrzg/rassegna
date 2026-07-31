package it.pietro.rassegna.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF14303C),
    onPrimary = Color(0xFFF7F7F5),
    secondary = Color(0xFF5B6B72),
    background = Color(0xFFFBFAF8),
    onBackground = Color(0xFF11171A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF11171A),
    surfaceVariant = Color(0xFFEDEEEC),
    onSurfaceVariant = Color(0xFF4A5459),
    outlineVariant = Color(0xFFDDE0DE)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FBDC9),
    onPrimary = Color(0xFF0B1418),
    secondary = Color(0xFF9DAAB0),
    background = Color(0xFF0E1214),
    onBackground = Color(0xFFEDEFEE),
    surface = Color(0xFF161C1F),
    onSurface = Color(0xFFEDEFEE),
    surfaceVariant = Color(0xFF222A2E),
    onSurfaceVariant = Color(0xFFB3BEC3),
    outlineVariant = Color(0xFF2C3438)
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp
    )
)

@Composable
fun RassegnaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
