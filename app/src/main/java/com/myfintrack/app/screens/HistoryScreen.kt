package com.myfintrack.app.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class HistoryTypeFilter(val label: String) {
    ALL("All"), EXPENSE("Expenses"), INCOME("Income"), TRANSFER("Transfers")
}

enum class HistoryPeriod(val label: String) { ALL_TIME("All time"), THIS_MONTH("This month"), LAST_MONTH("Last month") }

private data class DayGroup(val date: LocalDate, val items: List<Transaction>, val netPaise: Long)

@Composable
fun HistoryScreen(
    transactions: List<Transaction>,
    loaded: Boolean,
    hideAmounts: Boolean,
    onAddTransaction: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    accountNames: Map<Long, String> = emptyMap()
) {
    var query by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf(HistoryTypeFilter.ALL) }
    var period by rememberSaveable { mutableStateOf(HistoryPeriod.ALL_TIME) }
    val zone = remember { ZoneId.systemDefault() }

    val filtered = remember(transactions, query, typeFilter, period, accountNames) {
        val q = query.trim()
        val range = when (period) {
            HistoryPeriod.ALL_TIME -> null
            HistoryPeriod.THIS_MONTH -> FinanceCalculator.monthRange(YearMonth.now(zone), zone)
            HistoryPeriod.LAST_MONTH -> FinanceCalculator.monthRange(YearMonth.now(zone).minusMonths(1), zone)
        }
        transactions.filter { tx ->
            val typeOk = when (typeFilter) {
                HistoryTypeFilter.ALL -> true
                HistoryTypeFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
                HistoryTypeFilter.INCOME -> tx.type == TransactionType.INCOME
                HistoryTypeFilter.TRANSFER -> tx.type == TransactionType.TRANSFER
            }
            val periodOk = range == null || tx.occurredAt in range
            val queryOk = q.isEmpty() ||
                tx.title.contains(q, ignoreCase = true) ||
                tx.category.label.contains(q, ignoreCase = true) ||
                tx.note.contains(q, ignoreCase = true) ||
                tx.paymentMethod.label.contains(q, ignoreCase = true) ||
                (tx.accountId?.let { accountNames[it] }?.contains(q, ignoreCase = true) == true) ||
                (tx.toAccountId?.let { accountNames[it] }?.contains(q, ignoreCase = true) == true)
            typeOk && periodOk && queryOk
        }
    }
    val totals = remember(filtered) { FinanceCalculator.totals(filtered) }
    val groups = remember(filtered) {
        filtered
            .groupBy { FinanceCalculator.localDate(it.occurredAt, zone) }
            .map { (date, items) -> DayGroup(date, items, FinanceCalculator.totals(items).netPaise) }
    }
    val today = LocalDate.now(zone)
    val filtersActive = query.isNotBlank() || typeFilter != HistoryTypeFilter.ALL || period != HistoryPeriod.ALL_TIME

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "History",
                color = FinTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search title, category, account, note", color = FinTextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = FinTextSecondary) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = FinTextSecondary)
                        }
                    }
                },
                singleLine = true,
                colors = finTextFieldColors(FinAccent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryTypeFilter.entries.forEach { f ->
                    FinChip(label = f.label, selected = typeFilter == f, onClick = { typeFilter = f })
                }
                VerticalDivider(modifier = Modifier.height(32.dp).align(Alignment.CenterVertically), color = FinBorder)
                HistoryPeriod.entries.forEach { p ->
                    FinChip(label = p.label, selected = period == p, onClick = { period = p })
                }
            }

            if (filtered.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "${filtered.size} transaction${if (filtered.size == 1) "" else "s"}",
                        color = FinTextSecondary,
                        fontSize = 11.sp
                    )
                    if (typeFilter == HistoryTypeFilter.ALL || typeFilter == HistoryTypeFilter.INCOME) {
                        Text(
                            "In " + Money.display(totals.incomePaise, hideAmounts, withDecimals = false),
                            color = FinAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (typeFilter == HistoryTypeFilter.ALL || typeFilter == HistoryTypeFilter.EXPENSE) {
                        Text(
                            "Out " + Money.display(totals.expensePaise, hideAmounts, withDecimals = false),
                            color = FinExpense,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        when {
            !loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FinAccent)
            }

            transactions.isEmpty() -> EmptyState(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = "No transactions yet",
                message = "Everything you add will appear here, grouped by day.",
                actionLabel = "Add transaction",
                onAction = onAddTransaction
            )

            filtered.isEmpty() -> EmptyState(
                icon = Icons.Filled.Search,
                title = "No matches",
                message = "Try a different search or filter.",
                actionLabel = if (filtersActive) "Clear filters" else null,
                onAction = {
                    query = ""
                    typeFilter = HistoryTypeFilter.ALL
                    period = HistoryPeriod.ALL_TIME
                }
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groups.forEach { group ->
                    item(key = "day_${group.date.toEpochDay()}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                longDayLabel(group.date, today),
                                color = FinTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.semantics { heading() }
                            )
                            // Day net = income − expenses; transfers never change it.
                            if (!hideAmounts && group.items.any { it.type != TransactionType.TRANSFER }) {
                                Text(
                                    Money.formatSigned(group.netPaise, group.netPaise >= 0L, withDecimals = false),
                                    color = if (group.netPaise >= 0L) FinAccent else FinExpense,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    items(group.items, key = { "tx_${it.id}" }) { tx ->
                        TransactionRow(
                            transaction = tx,
                            hideAmounts = hideAmounts,
                            onClick = { onTransactionClick(tx) },
                            showDate = false,
                            accountNames = accountNames
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FinChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = FinCard,
            labelColor = FinTextSecondary,
            selectedContainerColor = FinAccent.copy(alpha = 0.18f),
            selectedLabelColor = FinAccent
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = FinBorder,
            selectedBorderColor = FinAccent.copy(alpha = 0.5f)
        )
    )
}
