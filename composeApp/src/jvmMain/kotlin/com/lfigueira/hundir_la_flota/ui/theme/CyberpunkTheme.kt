package com.lfigueira.hundir_la_flota.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Cyberpunk / Military Palette
object CyberColors {
    val DeepNavy = Color(0xFF0A0F1E)
    val DarkSpace = Color(0xFF03050C)
    val NeonBlue = Color(0xFF00F2FF)
    val NeonGreen = Color(0xFF39FF14)
    val NeonRed = Color(0xFFFF003C)
    val MetallicGray = Color(0xFF4A4E5D)
    val TranslucentBlue = Color(0x3300F2FF)
    val TranslucentGreen = Color(0x3339FF14)
    val PanelAlpha = 0.7f
    
    // Aliases for SettingsScreen and other components
    val backgroundDark = DarkSpace
    val backgroundLight = DeepNavy
    val cyan = NeonBlue
    val cardBackground = MetallicGray.copy(alpha = 0.3f)
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.7f)
    val error = NeonRed
}

private val DarkColorScheme = darkColorScheme(
    primary = CyberColors.NeonBlue,
    secondary = CyberColors.NeonGreen,
    tertiary = CyberColors.NeonRed,
    background = CyberColors.DeepNavy,
    surface = CyberColors.DarkSpace,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

val CyberTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 2.sp,
        color = CyberColors.NeonBlue
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = CyberColors.NeonBlue
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = CyberColors.NeonGreen
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        color = Color.White
    )
)

@Composable
fun CyberpunkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = CyberTypography,
        content = content
    )
}
