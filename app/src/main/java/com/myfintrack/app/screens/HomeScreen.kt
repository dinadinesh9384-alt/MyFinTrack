package com.myfintrack.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfintrack.app.components.SpendingOverviewSection
import com.myfintrack.app.model.CategorySpending
import com.myfintrack.app.model.Transaction
import com.myfintrack.app.model.TransactionType
import com.myfintrack.app.theme.*

@Composable
fun HomeScreen(
    transactions: List<Transaction>,
    categories: List<CategorySpending>,
    isBalanceMasked: Boolean,
    onToggleBalanceMask: () -> Unit,
    onNotificationClick: () -> Unit,
    onQuickAction: (String) -> Unit,
    onSeeAllHistory: () -> Unit,
    onCategorySelected: (CategorySpending) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalBalance = totalIncome - totalExpenses

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // A. Top Navigation Header
        item {
            HeaderSection(
                onNotificationClick = onNotificationClick
            )
        }

        // B. Total Balance Card
        item {
            BalanceCardSection(
                totalBalance = totalBalance,
                isMasked = isBalanceMasked,
                onToggleMask = onToggleBalanceMask
            )
        }

        // C. Income & Expense Summary Cards
        item {
            IncomeExpenseSummarySection(
                incomeAmount = totalIncome,
                expenseAmount = totalExpenses
            )
        }

        // D. Quick Actions
        item {
            QuickActionsSection(
                onAddIncome = { onQuickAction("Add Income") },
                onAddExpense = { onQuickAction("Add Expense") },
                onViewHistory = onSeeAllHistory
            )
        }

        // E. Recent Transactions
        item {
            RecentTransactionsHeader(onSeeAll = onSeeAllHistory)
        }

        items(transactions.take(5)) { transaction ->
            TransactionItemRow(transaction = transaction)
        }

        // F. Monthly Spending Overview (Donut Chart)
        item {
            SpendingOverviewSection(
                categories = categories,
                totalExpense = totalExpenses,
                onCategorySelected = onCategorySelected
            )
        }
    }
}

@Composable
private fun HeaderSection(
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(FinAccentGlow, RoundedCornerShape(16.dp))
                    .border(1.dp, FinAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Wallet",
                    tint = FinAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = "MYFINTRACK",
                    color = FinAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Good Evening! 👋",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Here's your financial overview.",
                    color = FinTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Notification Bell with Badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(FinCard, RoundedCornerShape(12.dp))
                    .border(1.dp, FinBorder, RoundedCornerShape(12.dp))
                    .clickable { onNotificationClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = FinTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .background(FinAccent, CircleShape)
                )
            }

            // User Profile Avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FinCardHover)
                    .border(1.dp, FinAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "JD",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun BalanceCardSection(
    totalBalance: Double,
    isMasked: Boolean,
    onToggleMask: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = FinCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FinAccent.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TOTAL BALANCE",
                        color = FinTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .background(FinAccentGlow, RoundedCornerShape(12.dp))
                            .border(1.dp, FinAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Active",
                            color = FinAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onToggleMask,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isMasked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Balance",
                        tint = FinTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = if (isMasked) "••••••••" else "₹${String.format("%,.2f", totalBalance)}",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )

            Text(
                text = "Available across 3 accounts",
                color = FinTextSecondary,
                fontSize = 11.sp
            )

            HorizontalDivider(color = FinBorder.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .background(FinAccentGlow, RoundedCornerShape(8.dp))
                        .border(1.dp, FinAccent.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = FinAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "+12.4% vs last month",
                        color = FinAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Mini Sparkline Bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(20.dp)
                ) {
                    val heights = listOf(8.dp, 12.dp, 6.dp, 16.dp, 14.dp, 20.dp)
                    heights.forEachIndexed { index, h ->
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(h)
                                .background(
                                    if (index == heights.lastIndex) FinAccent else FinAccent.copy(alpha = 0.4f),
                                    RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeExpenseSummarySection(
    incomeAmount: Double,
    expenseAmount: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Income Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FinCard),
            modifier = Modifier
                .weight(1f)
                .border(1.dp, FinAccent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(FinAccentGlow, RoundedCornerShape(10.dp))
                            .border(1.dp, FinAccent.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Income",
                            tint = FinAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "This Month",
                        color = FinTextSecondary,
                        fontSize = 9.sp,
                        modifier = Modifier
                            .background(FinCardHover, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text("Total Income", color = FinTextSecondary, fontSize = 11.sp)
                Text(
                    text = "₹${String.format("%,.0f", incomeAmount)}",
                    color = FinAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Expense Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FinCard),
            modifier = Modifier
                .weight(1f)
                .border(1.dp, FinExpense.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(FinExpenseGlow, RoundedCornerShape(10.dp))
                            .border(1.dp, FinExpense.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Expense",
                            tint = FinExpense,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "This Month",
                        color = FinTextSecondary,
                        fontSize = 9.sp,
                        modifier = Modifier
                            .background(FinCardHover, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text("Total Expenses", color = FinTextSecondary, fontSize = 11.sp)
                Text(
                    text = "₹${String.format("%,.0f", expenseAmount)}",
                    color = FinExpense,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onViewHistory: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "QUICK ACTIONS",
            color = FinTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Add Income Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(FinAccentGlow, RoundedCornerShape(18.dp))
                    .border(1.dp, FinAccent.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .clickable { onAddIncome() }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(FinAccent, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Income",
                            tint = Color.Black
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Add Income", color = FinAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Add Expense Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(FinExpenseGlow, RoundedCornerShape(18.dp))
                    .border(1.dp, FinExpense.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .clickable { onAddExpense() }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(FinExpense, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Add Expense",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Add Expense", color = FinExpense, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // View History Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(FinCard, RoundedCornerShape(18.dp))
                    .border(1.dp, FinBorder, RoundedCornerShape(18.dp))
                    .clickable { onViewHistory() }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(FinCardHover, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View History",
                            tint = FinTextSecondary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("View History", color = FinTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsHeader(onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recent Transactions",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onSeeAll) {
            Text("See All", color = FinAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = FinAccent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun TransactionItemRow(transaction: Transaction) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FinCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FinBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val (icon, iconColor) = getTransactionIcon(transaction.category)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, iconColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = transaction.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = transaction.category,
                            color = FinTextSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .background(FinBg, RoundedCornerShape(4.dp))
                                .border(1.dp, FinBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                        Text("•", color = FinTextSecondary, fontSize = 10.sp)
                        Text(transaction.date, color = FinTextSecondary, fontSize = 10.sp)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val isIncome = transaction.type == TransactionType.INCOME
                Text(
                    text = "${if (isIncome) "+" else "-"}₹${String.format("%,.0f", transaction.amount)}",
                    color = if (isIncome) FinAccent else FinExpense,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = transaction.paymentMethod,
                    color = FinTextSecondary,
                    fontSize = 9.sp
                )
            }
        }
    }
}

private fun getTransactionIcon(category: String): Pair<ImageVector, Color> {
    return when (category.lowercase()) {
        "income", "salary" -> Icons.Default.Work to Color(0xFF10B981)
        "food" -> Icons.Default.LocalCafe to Color(0xFFF59E0B)
        "transport" -> Icons.Default.DirectionsBus to Color(0xFF3B82F6)
        "friend" -> Icons.Default.Group to Color(0xFFA855F7)
        "shopping" -> Icons.Default.ShoppingCart to Color(0xFFF43F5E)
        else -> Icons.Default.ReceiptLong to Color(0xFF64748B)
    }
}
