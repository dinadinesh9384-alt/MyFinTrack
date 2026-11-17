package com.myfintrack.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfintrack.app.components.*
import com.myfintrack.app.domain.Money
import com.myfintrack.app.domain.PlanningCalculator
import com.myfintrack.app.model.Account
import com.myfintrack.app.model.AccountBalance
import com.myfintrack.app.model.Budget
import com.myfintrack.app.model.BudgetProgress
import com.myfintrack.app.model.BudgetStatus
import com.myfintrack.app.model.Category
import com.myfintrack.app.model.Goal
import com.myfintrack.app.model.GoalProgress
import com.myfintrack.app.model.GoalStatus
import com.myfintrack.app.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class MoneySection(val label: String) { ACCOUNTS("Accounts"), BUDGETS("Budgets"), GOALS("Goals") }

private val GOAL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** Which editor (if any) is open on the Money screen. */
private sealed interface MoneyEditor {
    data class AccountEdit(val account: Account?) : MoneyEditor
    data class BudgetEdit(val budget: Budget?) : MoneyEditor
    data class GoalEdit(val goal: Goal?) : MoneyEditor
    data class Contribution(val progress: GoalProgress, val withdraw: Boolean) : MoneyEditor
}

@Composable
fun MoneyScreen(
    section: MoneySection,
    onSectionChange: (MoneySection) -> Unit,
    loaded: Boolean,
    accountBalances: List<AccountBalance>,
    unlinkedNetPaise: Long,
    budgetProgress: List<BudgetProgress>,
    goalProgress: List<GoalProgress>,
    hideAmounts: Boolean,
    onTransfer: () -> Unit,
    onSaveAccount: (Account, SaveResult) -> Unit,
    onDeleteAccount: (Account, SaveResult) -> Unit,
    onSaveBudget: (Budget, SaveResult) -> Unit,
    onDeleteBudget: (Budget, SaveResult) -> Unit,
    onSaveGoal: (Goal, SaveResult) -> Unit,
    onDeleteGoal: (Goal, SaveResult) -> Unit,
    onContribute: (goalId: Long, amountPaise: Long, SaveResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var editor by remember { mutableStateOf<MoneyEditor?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Money",
                color = FinTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            SectionTabs(selected = section, onSelect = onSectionChange)
        }

        if (!loaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FinAccent)
            }
        } else LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (section) {
                MoneySection.ACCOUNTS -> accountsSection(
                    balances = accountBalances,
                    unlinkedNetPaise = unlinkedNetPaise,
                    hidden = hideAmounts,
                    onAdd = { editor = MoneyEditor.AccountEdit(null) },
                    onEdit = { editor = MoneyEditor.AccountEdit(it) },
                    onTransfer = onTransfer
                )

                MoneySection.BUDGETS -> budgetsSection(
                    progress = budgetProgress,
                    hidden = hideAmounts,
                    onAdd = { editor = MoneyEditor.BudgetEdit(null) },
                    onEdit = { editor = MoneyEditor.BudgetEdit(it) }
                )

                MoneySection.GOALS -> goalsSection(
                    goals = goalProgress,
                    hidden = hideAmounts,
                    onAdd = { editor = MoneyEditor.GoalEdit(null) },
                    onEdit = { editor = MoneyEditor.GoalEdit(it) },
                    onContribute = { progress, withdraw -> editor = MoneyEditor.Contribution(progress, withdraw) }
                )
            }
        }
    }

    when (val current = editor) {
        null -> Unit
        is MoneyEditor.AccountEdit -> AccountEditorSheet(
            existing = current.account,
            onDismiss = { editor = null },
            onSave = onSaveAccount,
            onDelete = onDeleteAccount
        )
        is MoneyEditor.BudgetEdit -> BudgetEditorSheet(
            existing = current.budget,
            usedScopes = budgetProgress.map { it.budget.category }.toSet(),
            onDismiss = { editor = null },
            onSave = onSaveBudget,
            onDelete = onDeleteBudget
        )
        is MoneyEditor.GoalEdit -> GoalEditorSheet(
            existing = current.goal,
            onDismiss = { editor = null },
            onSave = onSaveGoal,
            onDelete = onDeleteGoal
        )
        is MoneyEditor.Contribution -> GoalContributionDialog(
            goalName = current.progress.goal.name,
            withdraw = current.withdraw,
            maxWithdrawPaise = current.progress.savedPaise.coerceAtLeast(0L),
            onDismiss = { editor = null },
            onSubmit = { amount, done -> onContribute(current.progress.goal.id, amount, done) }
        )
    }
}

@Composable
private fun SectionTabs(selected: MoneySection, onSelect: (MoneySection) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FinCard, RoundedCornerShape(14.dp))
            .border(1.dp, FinBorder, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        MoneySection.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) FinAccent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(option) }
                    .semantics { this.selected = isSelected }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    option.label,
                    color = if (isSelected) FinAccent else FinTextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Accounts
// ---------------------------------------------------------------------------------------------

private fun LazyListScope.accountsSection(
    balances: List<AccountBalance>,
    unlinkedNetPaise: Long,
    hidden: Boolean,
    onAdd: () -> Unit,
    onEdit: (Account) -> Unit,
    onTransfer: () -> Unit
) {
    val active = balances.filter { !it.account.archived }
    val archived = balances.filter { it.account.archived }

    if (balances.isEmpty()) {
        item(key = "accounts_empty") {
            FinSurfaceCard {
                EmptyState(
                    icon = Icons.Filled.AccountBalance,
                    title = "No accounts yet",
                    message = "Add your bank accounts, cash, wallets and credit cards to see a real balance for each " +
                        "and to record transfers between them.",
                    actionLabel = "Add account",
                    onAction = onAdd
                )
            }
        }
        return
    }

    item(key = "accounts_summary") {
        val inAccounts = balances.sumOf { it.balancePaise }
        val owed = balances.filter { it.account.type.isLiability && it.balancePaise < 0 }.sumOf { -it.balancePaise }
        FinSurfaceCard(borderColor = FinAccent.copy(alpha = 0.22f), contentPadding = 20.dp) {
            OverlineLabel("Total across accounts")
            Text(
                Money.display(inAccounts, hidden),
                color = if (inAccounts < 0 && !hidden) FinExpense else FinTextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1
            )
            Text(
                "${active.size} active account${if (active.size == 1) "" else "s"}" +
                    if (archived.isNotEmpty()) " · ${archived.size} archived" else "",
                color = FinTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            if (owed > 0L || unlinkedNetPaise != 0L) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = FinBorder)
                Spacer(Modifier.height(10.dp))
            }
            if (owed > 0L) {
                SummaryLine("Owed on credit cards", Money.display(owed, hidden), FinExpense)
            }
            if (unlinkedNetPaise != 0L) {
                SummaryLine(
                    "Not linked to an account",
                    if (hidden) Money.MASK else Money.formatSigned(unlinkedNetPaise, unlinkedNetPaise >= 0),
                    FinTextSecondary
                )
                Text(
                    "Income and expenses saved without an account. Edit them to link an account.",
                    color = FinTextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    item(key = "accounts_actions") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton("Add account", Icons.Filled.Add, FinAccent, enabled = true, onClick = onAdd, modifier = Modifier.weight(1f))
            ActionButton(
                "Transfer",
                Icons.Filled.SwapHoriz,
                FinTransfer,
                enabled = active.size >= 2,
                onClick = onTransfer,
                modifier = Modifier.weight(1f)
            )
        }
    }
    if (active.size < 2) {
        item(key = "accounts_transfer_hint") {
            Text(
                "Add at least two accounts to record transfers between them.",
                color = FinTextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    items(active, key = { "acc_${it.account.id}" }) { balance ->
        AccountRow(balance, hidden, onClick = { onEdit(balance.account) })
    }

    if (archived.isNotEmpty()) {
        item(key = "accounts_archived_header") {
            SectionHeader(title = "Archived")
        }
        items(archived, key = { "acc_${it.account.id}" }) { balance ->
            AccountRow(balance, hidden, onClick = { onEdit(balance.account) })
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = FinTextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun AccountRow(balance: AccountBalance, hidden: Boolean, onClick: () -> Unit) {
    val account = balance.account
    val amount = balance.balancePaise
    val isOwed = account.type.isLiability && amount < 0
    val amountText = when {
        hidden -> Money.MASK
        isOwed -> Money.format(-amount)
        else -> Money.format(amount)
    }
    val caption = when {
        isOwed -> "Owed"
        account.type.isLiability -> "In credit"
        else -> "Balance"
    }
    val color = when {
        hidden -> FinTextPrimary
        amount < 0 -> FinExpense
        else -> FinTextPrimary
    }
    val spoken = "${account.name}, ${account.type.label}, $caption " +
        (if (hidden) "hidden" else Money.format(if (isOwed) -amount else amount)) +
        if (account.archived) ", archived" else ""

    FinSurfaceCard(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = spoken },
        onClick = onClick,
        contentPadding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val tint = if (account.type.isLiability) FinWarning else FinTransfer
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(tint.copy(alpha = 0.14f), RoundedCornerShape(13.dp))
                    .border(1.dp, tint.copy(alpha = 0.28f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(account.type.icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    account.name,
                    color = if (account.archived) FinTextSecondary else FinTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(account.type.label, color = FinTextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amountText, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(caption, color = FinTextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color,
            disabledContentColor = FinTextMuted
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) color.copy(alpha = 0.5f) else FinBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(46.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

// ---------------------------------------------------------------------------------------------
// Budgets
// ---------------------------------------------------------------------------------------------

private fun LazyListScope.budgetsSection(
    progress: List<BudgetProgress>,
    hidden: Boolean,
    onAdd: () -> Unit,
    onEdit: (Budget) -> Unit
) {
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    val daysLeft = PlanningCalculator.daysLeftInMonth(month, today)

    item(key = "budgets_header") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}",
                    color = FinTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "$daysLeft day${if (daysLeft == 1) "" else "s"} left this month",
                    color = FinTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            val allUsed = progress.size >= Category.entries.count { it.type == com.myfintrack.app.model.TransactionType.EXPENSE } + 1
            if (!allUsed) {
                TextButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = FinAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add budget", color = FinAccent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }

    if (progress.isEmpty()) {
        item(key = "budgets_empty") {
            FinSurfaceCard {
                EmptyState(
                    icon = Icons.Filled.TrackChanges,
                    title = "No budgets yet",
                    message = "Set a monthly limit for all spending or for categories like Food or Shopping. " +
                        "Progress is calculated from your real expenses.",
                    actionLabel = "Add budget",
                    onAction = onAdd
                )
            }
        }
        return
    }

    items(progress, key = { "budget_${it.budget.id}" }) { p ->
        BudgetCard(p, hidden, onClick = { onEdit(p.budget) })
    }
}

@Composable
fun BudgetCard(progress: BudgetProgress, hidden: Boolean, onClick: () -> Unit, compact: Boolean = false) {
    val category = progress.budget.category
    val title = category?.label ?: "All expenses"
    val color = when (progress.status) {
        BudgetStatus.ON_TRACK -> FinAccent
        BudgetStatus.NEAR_LIMIT -> FinWarning
        BudgetStatus.OVER_LIMIT -> FinExpense
    }
    val statusText = when {
        hidden -> when (progress.status) {
            BudgetStatus.ON_TRACK -> "On track"
            BudgetStatus.NEAR_LIMIT -> "Near limit"
            BudgetStatus.OVER_LIMIT -> "Over limit"
        }
        progress.status == BudgetStatus.OVER_LIMIT -> "Over by ${Money.format(-progress.remainingPaise, withDecimals = false)}"
        progress.status == BudgetStatus.NEAR_LIMIT ->
            "Near limit · ${Money.format(progress.remainingPaise, withDecimals = false)} left"
        progress.perDayLeftPaise > 0L ->
            "${Money.format(progress.remainingPaise, withDecimals = false)} left · " +
                "${Money.format(progress.perDayLeftPaise, withDecimals = false)}/day"
        else -> "${Money.format(progress.remainingPaise, withDecimals = false)} left"
    }
    val amounts = if (hidden) "${progress.percentUsed}% used"
    else "${Money.format(progress.spentPaise, withDecimals = false)} of ${Money.format(progress.budget.limitPaise, withDecimals = false)}"

    FinSurfaceCard(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$title budget, $amounts, ${progress.percentUsed} percent used, $statusText"
        },
        onClick = onClick,
        contentPadding = if (compact) 12.dp else 16.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (category != null) {
                CategoryBadge(category, size = 36.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(FinAccent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = FinAccent, modifier = Modifier.size(18.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = FinTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(amounts, color = FinTextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Text("${progress.percentUsed}%", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(10.dp))
        FinProgressBar(fraction = progress.percentUsed / 100f, color = color)
        Spacer(Modifier.height(6.dp))
        Text(statusText, color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------------------------------------
// Goals
// ---------------------------------------------------------------------------------------------

private fun LazyListScope.goalsSection(
    goals: List<GoalProgress>,
    hidden: Boolean,
    onAdd: () -> Unit,
    onEdit: (Goal) -> Unit,
    onContribute: (GoalProgress, Boolean) -> Unit
) {
    item(key = "goals_header") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Money you set aside stays in your accounts. Goals never count as expenses.",
                color = FinTextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            if (goals.isNotEmpty()) {
                TextButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = FinAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New goal", color = FinAccent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }

    if (goals.isEmpty()) {
        item(key = "goals_empty") {
            FinSurfaceCard {
                EmptyState(
                    icon = Icons.Filled.Savings,
                    title = "No savings goals yet",
                    message = "Create a goal such as an emergency fund or a trip, then track what you set aside for it.",
                    actionLabel = "Create goal",
                    onAction = onAdd
                )
            }
        }
        return
    }

    items(goals, key = { "goal_${it.goal.id}" }) { g ->
        GoalCard(
            progress = g,
            hidden = hidden,
            onClick = { onEdit(g.goal) },
            onAdd = { onContribute(g, false) },
            onWithdraw = { onContribute(g, true) }
        )
    }
}

@Composable
private fun GoalCard(
    progress: GoalProgress,
    hidden: Boolean,
    onClick: () -> Unit,
    onAdd: () -> Unit,
    onWithdraw: () -> Unit
) {
    val (statusLabel, statusColor) = when (progress.status) {
        GoalStatus.COMPLETED -> "Reached" to FinAccent
        GoalStatus.OVERDUE -> "Past target date" to FinExpense
        GoalStatus.IN_PROGRESS -> "In progress" to FinTransfer
    }
    val amounts = if (hidden) "${progress.percent}% saved"
    else "${Money.format(progress.savedPaise, withDecimals = false)} of ${Money.format(progress.goal.targetPaise, withDecimals = false)}"
    val targetDate = progress.goal.targetEpochDay?.let { LocalDate.ofEpochDay(it) }
    val plan = when {
        progress.status == GoalStatus.COMPLETED -> "Goal reached. Well done."
        progress.monthlyNeededPaise != null && progress.monthsLeft != null ->
            (if (hidden) "Save monthly" else "Save ${Money.format(progress.monthlyNeededPaise, withDecimals = false)}/month") +
                " for ${progress.monthsLeft} month${if (progress.monthsLeft == 1) "" else "s"}"
        progress.status == GoalStatus.OVERDUE && !hidden ->
            "${Money.format(progress.remainingPaise, withDecimals = false)} still to go"
        targetDate == null && !hidden -> "${Money.format(progress.remainingPaise, withDecimals = false)} to go · no target date"
        else -> null
    }

    FinSurfaceCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(statusColor.copy(alpha = 0.14f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Flag, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    progress.goal.name,
                    color = FinTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    targetDate?.let { "Target ${GOAL_DATE_FORMAT.format(it)}" } ?: "No target date",
                    color = FinTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            InfoPill(text = statusLabel, color = statusColor)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(amounts, color = FinTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("${progress.percent}%", color = statusColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        FinProgressBar(fraction = progress.percent / 100f, color = statusColor)
        if (plan != null) {
            Spacer(Modifier.height(6.dp))
            Text(plan, color = FinTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton("Add money", Icons.Filled.Add, FinAccent, enabled = true, onClick = onAdd, modifier = Modifier.weight(1f))
            ActionButton(
                "Withdraw",
                Icons.Filled.Remove,
                FinWarning,
                enabled = progress.savedPaise > 0L,
                onClick = onWithdraw,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
