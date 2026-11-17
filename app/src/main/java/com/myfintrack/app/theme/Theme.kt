package com.myfintrack.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FinBg = Color(0xFF0B0F14)
val FinCard = Color(0xFF151C24)
val FinCardHover = Color(0xFF1C2632)
val FinAccent = Color(0xFF22C55E)
val FinAccentGlow = Color(0x2622C55E)
val FinExpense = Color(0xFFEF6464)
val FinExpenseGlow = Color(0x26EF6464)
val FinTextPrimary = Color(0xFFFFFFFF)
val FinTextSecondary = Color(0xFF9CA3AF)
val FinBorder = Color(0xFF232D39)
val FinBorderLight = Color(0xFF2D3B4C)

// Category Colors
val CategoryFood = Color(0xFFF59E0B)
val CategoryTransport = Color(0xFF3B82F6)
val CategoryShopping = Color(0xFFF43F5E)
val CategoryFriends = Color(0xFFA855F7)
val CategoryOthers = Color(0xFF64748B)

private val DarkColorScheme = darkColorScheme(
    primary = FinAccent,
    background = FinBg,
    surface = FinCard,
    onPrimary = Color.Black,
    onBackground = FinTextPrimary,
    onSurface = FinTextPrimary,
    outline = FinBorder
)

@Composable
fun MyFinTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
