package com.myfintrack.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfintrack.app.components.*
import com.myfintrack.app.domain.FinanceCalculator
import com.myfintrack.app.domain.Money
import com.myfintrack.app.model.AppSettings
import com.myfintrack.app.model.Transaction
import com.myfintrack.app.theme.*
import com.myfintrack.app.viewmodel.DashboardState
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(
    state: DashboardState,
    settings: AppSettings,
    onToggleHideBalances: () -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val hidden = settings.hideBalances
    val monthName = state.month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            HeaderSection(displayName = settings.displayName, onProfileClick = onOpenSettings)
        }

        item(key = "balance") {
            BalanceCard(
                state = state,
                hidden = hidden,
                onToggleHidden = onToggleHideBalances
            )
        }

        item(key = "summary") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryTile(
                    label = "Income",
                    caption = monthName,
                    amount = Money.display(state.monthTotals.incomePaise, hidden, withDecimals = false),
                    color = FinAccent,
                    icon = Icons.Filled.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )
                SummaryTile(
                    label = "Expenses",
                    caption = monthName,
                    amount = Money.display(state.monthTotals.expensePaise, hidden, withDecimals = false),
                    color = FinExpense,
                    icon = Icons.Filled.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Add expense", Icons.Filled.Remove, FinExpense, onAddExpense, Modifier.weight(1f))
                QuickAction("Add income", Icons.Filled.Add, FinAccent, onAddIncome, Modifier.weight(1f))
                QuickAction("History", Icons.Filled.History, FinTextSecondary, onOpenHistory, Modifier.weight(1f))
            }
        }

        item(key = "recent_header") {
            SectionHeader(
                title = "Recent transactions",
                actionLabel = if (state.transactionCount > 0) "See all" else null,
                onAction = onOpenHistory
            )
        }

        if (state.loaded && state.recent.isEmpty()) {
            item(key = "recent_empty") {
                FinSurfaceCard {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "No transactions yet",
                        message = "Add your first income or expense to start tracking your money.",
                        actionLabel = "Add expense",
                        onAction = onAddExpense
                    )
                }
            }
        } else {
            items(state.recent, key = { "tx_${it.id}" }) { tx ->
                TransactionRow(
                    transaction = tx,
                    hideAmounts = hidden,
                    onClick = { onTransactionClick(tx) }
                )
            }
        }

        if (state.transactionCount > 0) {
            item(key = "overview") {
                SpendingOverviewSection(
                    title = "Spending this month",
                    subtitle = "By category, $monthName ${state.month.year}",
                    categories = state.monthCategories,
                    totalPaise = state.monthTotals.expensePaise,
                    hideAmounts = hidden
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(displayName: String, onProfileClick: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val name = displayName.trim()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            OverlineLabel("MyFinTrack", color = FinAccent)
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (name.isEmpty()) greeting else "$greeting, ${name.substringBefore(' ')}",
                color = FinTextPrimary,
                style = MaterialTheme.typography.titleLarge
            )
        }
        val initials = name.split(' ').filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(FinCardHover)
                .border(1.dp, FinAccent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClickLabel = "Open settings", onClick = onProfileClick)
                .semantics { contentDescription = "Profile and settings" },
            contentAlignment = Alignment.Center
        ) {
            if (initials.isEmpty()) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = FinTextSecondary, modifier = Modifier.size(20.dp))
            } else {
                Text(initials, color = FinTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun BalanceCard(
    state: DashboardState,
    hidden: Boolean,
    onToggleHidden: () -> Unit
) {
    FinSurfaceCard(borderColor = FinAccent.copy(alpha = 0.22f), contentPadding = 20.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverlineLabel("Net balance", modifier = Modifier.weight(1f))
            IconButton(onClick = onToggleHidden, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (hidden) "Show amounts" else "Hide amounts",
                    tint = FinTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = Money.display(state.balancePaise, hidden),
            color = if (state.balancePaise < 0 && !hidden) FinExpense else FinTextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "All recorded income minus expenses",
            color = FinTextSecondary,
            style = MaterialTheme.typography.bodySmall
        )

        val hasHistory = state.lastSixMonths.any { it.totals.incomePaise > 0 || it.totals.expensePaise > 0 }
        if (state.expenseChangePercent != null || hasHistory) {
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = FinBorder)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val change = state.expenseChangePercent
                if (change != null) {
                    val lower = change <= 0
                    InfoPill(
                        text = if (change == 0) "Spending same as last month"
                        else "Spending ${abs(change)}% ${if (lower) "lower" else "higher"} vs last month",
                        color = if (lower) FinAccent else FinWarning,
                        icon = if (lower) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp
                    )
                } else {
                    Text("Monthly net, last 6 months", color = FinTextMuted, fontSize = 11.sp)
                }
                if (hasHistory) {
                    NetSparkline(state.lastSixMonths)
                }
            }
        }
    }
}

/** Six bars, one per month: height = size of net result, colour = surplus (green) or deficit (red). */
@Composable
private fun NetSparkline(months: List<FinanceCalculator.MonthTotals>) {
    val maxAbs = months.maxOfOrNull { abs(it.totals.netPaise) }?.takeIf { it > 0L } ?: 1L
    val description = "Monthly net for the last ${months.size} months"
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .height(24.dp)
            .semantics { contentDescription = description }
    ) {
        months.forEach { m ->
            val net = m.totals.netPaise
            val ratio = abs(net).toFloat() / maxAbs.toFloat()
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight(ratio.coerceIn(0.12f, 1f))
                    .background(
                        if (net >= 0L) FinAccent else FinExpense,
                        RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                    )
            )
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    caption: String,
    amount: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    FinSurfaceCard(modifier = modifier, borderColor = color.copy(alpha = 0.22f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(color.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(caption, color = FinTextMuted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(label, color = FinTextSecondary, style = MaterialTheme.typography.bodySmall)
        Text(amount, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(FinCard)
            .border(1.dp, FinBorder, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = FinTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
