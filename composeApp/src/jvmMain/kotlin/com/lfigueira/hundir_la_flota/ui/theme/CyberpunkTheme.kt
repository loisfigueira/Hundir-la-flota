package com.lfigueira.hundir_la_flota.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Modern & Warm Palette
object ModernColors {
    val DeepAsh = Color(0xFF121417)
    val SurfaceDark = Color(0xFF1E2126)
    val WarmTeal = Color(0xFF4DB6AC)
    val AmberGold = Color(0xFFFFB300)
    val SunsetOrange = Color(0xFFFF7043)
    val SlateGray = Color(0xFF454B54)
    val SoftWhite = Color(0xFFE0E0E0)
    
    // Aliases
    val backgroundDark = DeepAsh
    val backgroundLight = SurfaceDark
    val primary = WarmTeal
    val secondary = AmberGold
    val cardBackground = SurfaceDark.copy(alpha = 0.8f)
    val textPrimary = SoftWhite
    val textSecondary = SoftWhite.copy(alpha = 0.6f)
    val error = SunsetOrange
}

private val ModernColorScheme = darkColorScheme(
    primary = ModernColors.WarmTeal,
    secondary = ModernColors.AmberGold,
    tertiary = ModernColors.SunsetOrange,
    background = ModernColors.DeepAsh,
    surface = ModernColors.SurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = ModernColors.SoftWhite,
    onSurface = ModernColors.SoftWhite
)

val ModernTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        letterSpacing = 0.5.sp,
        color = ModernColors.AmberGold
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = ModernColors.WarmTeal
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = ModernColors.SoftWhite
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        color = ModernColors.SoftWhite.copy(alpha = 0.9f)
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace, // Keep mono for small tactical labels
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = ModernColors.SoftWhite.copy(alpha = 0.5f)
    )
)

@Composable
fun ModernTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ModernColorScheme,
        typography = ModernTypography,
        content = content
    )
}
