package com.hashcore.pool.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// P2PMine brand — deep charcoal + orange accent (matches website / logo)
val HashCoreBlack = Color(0xFF020617)      // slate-950 (site theme-color)
val HashCoreTrueBlack = Color(0xFF000000)  // AMOLED pure black
val HashCoreSurface = Color(0xFF0F172A)    // slate-900
val HashCoreCard = Color(0xFF111827)
val HashCoreElevated = Color(0xFF1E293B)
val HashCoreBorder = Color(0xFF334155)
val HashCoreAccent = Color(0xFFF97316)     // orange-500
val HashCoreAccentDim = Color(0xFFEA580C)  // orange-600
val HashCoreBlue = Color(0xFF3B82F6)
val HashCorePurple = Color(0xFF2D1B4E)     // logo purple
val HashCoreWarning = Color(0xFFF59E0B)
val HashCoreError = Color(0xFFEF4444)
val HashCoreSuccess = Color(0xFF22C55E)
val HashCoreTextPrimary = Color(0xFFF1F5F9)
val HashCoreTextSecondary = Color(0xFF94A3B8)
val HashCoreTextMuted = Color(0xFF64748B)

private fun darkColors(amoled: Boolean) = darkColorScheme(
    primary = HashCoreAccent,
    onPrimary = Color(0xFF1C0A00),
    primaryContainer = HashCoreAccentDim,
    onPrimaryContainer = HashCoreTextPrimary,
    secondary = HashCorePurple,
    onSecondary = Color.White,
    background = if (amoled) HashCoreTrueBlack else HashCoreBlack,
    onBackground = HashCoreTextPrimary,
    surface = if (amoled) HashCoreTrueBlack else HashCoreSurface,
    onSurface = HashCoreTextPrimary,
    surfaceVariant = if (amoled) Color(0xFF0A0A0A) else HashCoreCard,
    onSurfaceVariant = HashCoreTextSecondary,
    outline = HashCoreBorder,
    error = HashCoreError,
    onError = Color.White,
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

@Composable
fun HashCoreTheme(
    darkTheme: Boolean = true,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme()
    @Suppress("UNUSED_VARIABLE")
    val ignored = darkTheme

    MaterialTheme(
        colorScheme = darkColors(amoledBlack),
        typography = AppTypography,
        content = content,
    )
}
