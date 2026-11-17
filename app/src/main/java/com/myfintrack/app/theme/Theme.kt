package com.myfintrack.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Surfaces (darkest to lightest)
val FinBg = Color(0xFF0B0F14)
val FinSurface = Color(0xFF101720)
val FinCard = Color(0xFF141D27)
val FinCardHover = Color(0xFF192532)

// Semantic colours
val FinAccent = Color(0xFF22C55E)
val FinAccentGlow = Color(0x2622C55E)
val FinExpense = Color(0xFFEF6464)
val FinExpenseGlow = Color(0x26EF6464)
val FinTransfer = Color(0xFF60A5FA)
val FinWarning = Color(0xFFF59E0B)

// Text
val FinTextPrimary = Color(0xFFF8FAFC)
val FinTextSecondary = Color(0xFF94A3B8)
val FinTextMuted = Color(0xFF64748B)

// Borders
val FinBorder = Color(0xFF1F2A37)
val FinBorderLight = Color(0xFF2D3B4C)

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
    background = FinBg,
    onBackground = FinTextPrimary,
    surface = FinSurface,
    onSurface = FinTextPrimary,
    surfaceVariant = FinCard,
    onSurfaceVariant = FinTextSecondary,
    surfaceContainerLowest = FinBg,
    surfaceContainerLow = FinSurface,
    surfaceContainer = FinCard,
    surfaceContainerHigh = FinCardHover,
    surfaceContainerHighest = FinCardHover,
    outline = FinBorderLight,
    outlineVariant = FinBorder
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
fun MyFinTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = FinTypography,
        content = content
    )
}
