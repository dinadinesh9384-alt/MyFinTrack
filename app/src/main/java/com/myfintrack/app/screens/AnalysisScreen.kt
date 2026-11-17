package com.myfintrack.app.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfintrack.app.components.*
import com.myfintrack.app.domain.FinanceCalculator
import com.myfintrack.app.domain.Money
import com.myfintrack.app.model.Transaction
import com.myfintrack.app.model.TransactionType
import com.myfintrack.app.theme.*
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val MONTH_TITLE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

@Composable
fun AnalysisScreen(
    transactions: List<Transaction>,
    hideAmounts: Boolean,
    onAddTransaction: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    val currentMonth = YearMonth.now(zone)
    // Stored as "months since year 0" so it survives configuration changes.
    var monthIndex by rememberSaveable { mutableStateOf(currentMonth.year * 12 + currentMonth.monthValue - 1) }
    val month = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)

    val monthTxs = remember(transactions, month) { FinanceCalculator.forMonth(transactions, month, zone) }
    val totals = remember(monthTxs) { FinanceCalculator.totals(monthTxs) }
    val previousTotals = remember(transactions, month) {
        FinanceCalculator.totals(FinanceCalculator.forMonth(transactions, month.minusMonths(1), zone))
    }
    val expenseCategories = remember(monthTxs) { FinanceCalculator.categoryBreakdown(monthTxs, TransactionType.EXPENSE) }
    val incomeCategories = remember(monthTxs) { FinanceCalculator.categoryBreakdown(monthTxs, TransactionType.INCOME) }
    val daily = remember(transactions, month) { FinanceCalculator.dailyExpense(transactions, month, zone) }
    val largest = remember(monthTxs) {
        monthTxs.filter { it.type == TransactionType.EXPENSE }.sortedByDescending { it.amountPaise }.take(3)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "title") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Analysis",
                    color = FinTextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() }
                )
                IconButton(onClick = { monthIndex -= 1 }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = FinTextPrimary)
                }
                Text(MONTH_TITLE.format(month), color = FinTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                IconButton(onClick = { monthIndex += 1 }, enabled = month < currentMonth) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month",
                        tint = if (month < currentMonth) FinTextPrimary else FinTextMuted
                    )
                }
            }
        }

        if (monthTxs.isEmpty()) {
            item(key = "empty") {
                FinSurfaceCard {
                    EmptyState(
                        icon = Icons.Filled.BarChart,
                        title = "Nothing recorded in ${MONTH_TITLE.format(month)}",
                        message = if (transactions.isEmpty()) "Add transactions to see where your money goes."
                        else "Use the arrows to view another month.",
                        actionLabel = if (transactions.isEmpty()) "Add transaction" else null,
                        onAction = onAddTransaction
                    )
                }
            }
            return@LazyColumn
        }

        item(key = "totals") {
            FinSurfaceCard {
                Row(Modifier.fillMaxWidth()) {
                    StatColumn("Income", Money.display(totals.incomePaise, hideAmounts, false), FinAccent, Modifier.weight(1f))
                    StatColumn("Expenses", Money.display(totals.expensePaise, hideAmounts, false), FinExpense, Modifier.weight(1f))
                    StatColumn(
                        "Net",
                        if (hideAmounts) Money.MASK
                        else Money.formatSigned(totals.netPaise, totals.netPaise >= 0L, withDecimals = false),
                        if (totals.netPaise >= 0L) FinAccent else FinExpense,
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = FinBorder)
                Spacer(Modifier.height(12.dp))
                val rate = totals.savingsRatePercent
                Text(
                    text = when {
                        rate == null -> "No income recorded this month, so a savings rate can't be calculated."
                        rate >= 0 -> "You kept $rate% of your income this month."
                        else -> "You spent ${abs(rate)}% more than you earned this month."
                    },
                    color = FinTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                val change = FinanceCalculator.percentChange(totals.expensePaise, previousTotals.expensePaise)
                if (change != null) {
                    Spacer(Modifier.height(8.dp))
                    InfoPill(
                        text = if (change == 0) "Expenses equal to previous month"
                        else "Expenses ${abs(change)}% ${if (change < 0) "lower" else "higher"} than previous month",
                        color = if (change <= 0) FinAccent else FinWarning
                    )
                }
            }
        }

        item(key = "daily") {
            DailySpendingCard(daily = daily, month = month, hideAmounts = hideAmounts)
        }

        item(key = "expense_categories") {
            SpendingOverviewSection(
                title = "Where your money went",
                subtitle = "Expenses by category",
                categories = expenseCategories,
                totalPaise = totals.expensePaise,
                hideAmounts = hideAmounts,
                maxLegendItems = 8
            )
        }

        if (incomeCategories.isNotEmpty()) {
            item(key = "income_categories") {
                FinSurfaceCard {
                    Text("Income sources", color = FinTextPrimary, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    incomeCategories.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CategoryBadge(item.category, size = 32.dp)
                            Text(item.category.label, color = FinTextPrimary, modifier = Modifier.weight(1f))
                            Text(
                                Money.display(item.amountPaise, hideAmounts, false),
                                color = FinAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        if (largest.isNotEmpty()) {
            item(key = "largest_header") { SectionHeader("Largest expenses") }
            items(largest.size, key = { "largest_${largest[it].id}" }) { index ->
                val tx = largest[index]
                TransactionRow(transaction = tx, hideAmounts = hideAmounts, onClick = { onTransactionClick(tx) })
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = FinTextSecondary, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(2.dp))
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
private fun DailySpendingCard(daily: LongArray, month: YearMonth, hideAmounts: Boolean) {
    val maxDay = daily.maxOrNull() ?: 0L
    val peakIndex = daily.indices.maxByOrNull { daily[it] } ?: 0
    val activeDays = daily.count { it > 0L }
    val total = daily.sum()
    val description = "Daily spending chart. Highest day: ${peakIndex + 1}."

    FinSurfaceCard {
        Text("Daily spending", color = FinTextPrimary, style = MaterialTheme.typography.titleMedium)
        Text(
            text = if (activeDays == 0) "No expenses this month"
            else "Average " + Money.display(total / activeDays, hideAmounts, false) + " on days you spent",
            color = FinTextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(14.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .semantics { contentDescription = description }
        ) {
            val count = daily.size
            val slot = size.width / count
            val barWidth = (slot * 0.6f).coerceAtLeast(2f)
            daily.forEachIndexed { index, value ->
                val ratio = if (maxDay > 0L) value.toFloat() / maxDay.toFloat() else 0f
                val barHeight = if (value > 0L) (size.height * ratio).coerceAtLeast(3f) else 2f
                drawRoundRect(
                    color = when {
                        value == 0L -> FinCardHover
                        index == peakIndex -> FinExpense
                        else -> FinExpense.copy(alpha = 0.55f)
                    },
                    topLeft = Offset(index * slot + (slot - barWidth) / 2f, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("1", color = FinTextMuted, fontSize = 10.sp)
            Text("${month.lengthOfMonth()}", color = FinTextMuted, fontSize = 10.sp)
        }
        if (maxDay > 0L) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Highest: day ${peakIndex + 1}" + if (hideAmounts) "" else " (" + Money.format(maxDay, false) + ")",
                color = FinTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
