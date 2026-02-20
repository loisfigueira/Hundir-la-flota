package com.lfigueira.hundir_la_flota.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Modern & Refined Palette (Ocean & Tactical)
 * Focuses on high contrast and professional warmth.
 */
object ModernColors {
    // Core Neutrals
    val DeepSpace = Color(0xFF0F1115)     // Deep background
    val MidnightAsh = Color(0xFF1A1D23)   // Card/Surface
    val SurfaceLighter = Color(0xFF252A32) // Hover/Interaction
    
    // Brand & Tactical Colors
    val OceanTeal = Color(0xFF26A69A)     // Primary action (high legibility)
    val AmberGold = Color(0xFFFFB300)     // Tactical accents
    val SunsetOrange = Color(0xFFFF7043)  // Hostile/Error
    val SlateSilver = Color(0xFF90A4AE)   // Soft secondary text
    
    // Semantic Aliases
    val primary = OceanTeal
    val secondary = AmberGold
    val tertiary = Color(0xFF81C784)      // Success / Allies
    val error = SunsetOrange
    
    val background = DeepSpace
    val surface = MidnightAsh
    val onSurface = Color(0xFFECEFF1)
    val onSurfaceVariant = Color(0xFFB0BEC5)
    
    val textPrimary = onSurface
    val textSecondary = onSurfaceVariant
}

private val ModernColorScheme = darkColorScheme(
    primary = ModernColors.primary,
    onPrimary = Color.Black,
    secondary = ModernColors.secondary,
    onSecondary = Color.Black,
    tertiary = ModernColors.tertiary,
    onTertiary = Color.Black,
    error = ModernColors.error,
    onError = Color.Black,
    background = ModernColors.background,
    onBackground = ModernColors.onSurface,
    surface = ModernColors.surface,
    onSurface = ModernColors.onSurface,
    surfaceVariant = ModernColors.SurfaceLighter,
    onSurfaceVariant = ModernColors.onSurfaceVariant,
    outline = ModernColors.onSurfaceVariant.copy(alpha = 0.5f)
)

val ModernTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 48.sp,
        letterSpacing = (-1).sp,
        color = ModernColors.secondary
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        letterSpacing = 0.sp,
        color = ModernColors.primary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = ModernColors.primary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = ModernColors.onSurface
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = ModernColors.onSurface
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace, // Keep mono for technical feel
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp
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
