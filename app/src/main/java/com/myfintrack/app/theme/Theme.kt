package com.myfintrack.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.myfintrack.app.model.ThemeMode

data class FinColors(
    val bg: Color,
    val surface: Color,
    val card: Color,
    val cardHover: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val borderLight: Color
)

val DarkFinColors = FinColors(
    bg = Color(0xFF0B0F14),
    surface = Color(0xFF101720),
    card = Color(0xFF141D27),
    cardHover = Color(0xFF192532),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    border = Color(0xFF1F2A37),
    borderLight = Color(0xFF2D3B4C)
)

val LightFinColors = FinColors(
    bg = Color(0xFFF1F5F9),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    cardHover = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    border = Color(0xFFCBD5E1),
    borderLight = Color(0xFF94A3B8)
)

val LocalFinColors = staticCompositionLocalOf { DarkFinColors }

val FinBg: Color @Composable @ReadOnlyComposable get() = LocalFinColors.current.bg
val FinSurface: Color @Composable @ReadOnlyComposable get() = LocalFinColors.current.surface
val FinCard: Color @Composable @ReadOnlyComposable get() = LocalFinColors.current.card
val FinCardHover: Color @Composable @ReadOnlyComposable get() = LocalFinColors.current.cardHover
val FinTextPrimary: Color @Composable @ReadOnlyComposable get() = LocalFinColors.current.textPrimary
val FinTextSecondary: Color @Composable @ReadOnlyComposable get() = LocalFinColors.current.textSecondary
val FinTextMuted: Color @Composable @ReadOnlyComposable get() = LocalFinColors.current.textMuted
val FinBorder: Color @Composable @ReadOnlyComposable get() = LocalFinColors.current.border
val FinBorderLight: Color @Composable @ReadOnlyComposable get() = LocalFinColors.current.borderLight

// Semantic colours (constant across themes)
val FinAccent = Color(0xFF22C55E)
val FinAccentGlow = Color(0x2622C55E)
val FinExpense = Color(0xFFEF6464)
val FinExpenseGlow = Color(0x26EF6464)
val FinTransfer = Color(0xFF60A5FA)
val FinWarning = Color(0xFFF59E0B)

// Category colours
val CategoryFood = Color(0xFFF59E0B)
val CategoryGroceries = Color(0xFF84CC16)
val CategoryTransport = Color(0xFF3B82F6)
val CategoryFuel = Color(0xFFF97316)
val CategoryShopping = Color(0xFFF43F5E)
val CategoryBills = Color(0xFFEAB308)
val CategoryRent = Color(0xFF8B5CF6)
val CategoryHealth = Color(0xFF14B8A6)
val CategoryEducation = Color(0xFF06B6D4)
val CategoryEntertainment = Color(0xFFEC4899)
val CategoryTravel = Color(0xFF0EA5E9)
val CategoryPersonalCare = Color(0xFFD946EF)
val CategoryFriends = Color(0xFFA855F7)
val CategoryOthers = Color(0xFF64748B)
val CategoryIncome = Color(0xFF22C55E)

private val DarkColorScheme = darkColorScheme(
    primary = FinAccent,
    onPrimary = Color(0xFF052E16),
    primaryContainer = FinAccentGlow,
    onPrimaryContainer = FinAccent,
    secondary = FinTransfer,
    onSecondary = Color.Black,
    error = FinExpense,
    onError = Color.White,
    background = Color(0xFF0B0F14),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF101720),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF141D27),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF0B0F14),
    surfaceContainerLow = Color(0xFF101720),
    surfaceContainer = Color(0xFF141D27),
    surfaceContainerHigh = Color(0xFF192532),
    surfaceContainerHighest = Color(0xFF192532),
    outline = Color(0xFF2D3B4C),
    outlineVariant = Color(0xFF1F2A37)
)

private val LightColorScheme = lightColorScheme(
    primary = FinAccent,
    onPrimary = Color.White,
    primaryContainer = FinAccentGlow,
    onPrimaryContainer = FinAccent,
    secondary = FinTransfer,
    onSecondary = Color.White,
    error = FinExpense,
    onError = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color(0xFFF8FAFC),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1F5F9),
    surfaceContainerHighest = Color(0xFFF1F5F9),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

private val FinTypography = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 13.sp),
    bodySmall = TextStyle(fontSize = 11.sp),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
)

@Composable
fun MyFinTrackTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    },
    content: @Composable () -> Unit
) {
    val finColors = if (darkTheme) DarkFinColors else LightFinColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalFinColors provides finColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FinTypography,
            content = content
        )
    }
}
