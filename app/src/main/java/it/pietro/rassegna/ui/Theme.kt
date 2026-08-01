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

// Blu inchiostro (#1C3D63): la stessa famiglia del blu notte del marchio,
// alzata di tono perche' si legga come blu e non come nero.
// Il corallo resta l'accento unico e compare solo nel simbolo del logo.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1C3D63),
    onPrimary = Color(0xFFF5F1E8),
    secondary = Color(0xFF566274),
    tertiary = Color(0xFFFF6A41),
    onTertiary = Color(0xFF2B0F06),
    background = Color(0xFFFDFBF6),
    onBackground = Color(0xFF16202E),
    surface = Color(0xFFF5F1E8),
    onSurface = Color(0xFF16202E),
    surfaceVariant = Color(0xFFEBE5D8),
    onSurfaceVariant = Color(0xFF566274),
    outlineVariant = Color(0xFFE2DBCB)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DBEE4),
    onPrimary = Color(0xFF0E1B2B),
    secondary = Color(0xFFA9B4C2),
    tertiary = Color(0xFFFF8A66),
    onTertiary = Color(0xFF2B0F06),
    background = Color(0xFF101822),
    onBackground = Color(0xFFE9EBEF),
    surface = Color(0xFF18212D),
    onSurface = Color(0xFFE9EBEF),
    surfaceVariant = Color(0xFF212C3A),
    onSurfaceVariant = Color(0xFFA9B4C2),
    outlineVariant = Color(0xFF2B3644)
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
