package app.constellationpulse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val PulseDarkScheme = darkColorScheme(
    primary = Color(0xFFEEDB9A),
    onPrimary = Color(0xFF11110D),
    secondary = Color(0xFF9CE0C0),
    onSecondary = Color(0xFF06140F),
    tertiary = Color(0xFFB9B5FF),
    background = Color(0xFF030405),
    onBackground = Color(0xFFF5F2E9),
    surface = Color(0xFF111311),
    surfaceVariant = Color(0xFF1B1E1A),
    onSurface = Color(0xFFF4F1E7),
    onSurfaceVariant = Color(0xFFB8B5A8),
    outline = Color(0xFF3A3C35)
)

private val PulseTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Cursive,
        fontWeight = FontWeight.Light,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Cursive,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Cursive,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Cursive,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Cursive,
        fontWeight = FontWeight.Medium,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Cursive,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 29.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Cursive,
        fontWeight = FontWeight.Normal,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Cursive,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )
)

@Composable
fun ConstellationPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PulseDarkScheme,
        typography = PulseTypography,
        shapes = Shapes(),
        content = content
    )
}
