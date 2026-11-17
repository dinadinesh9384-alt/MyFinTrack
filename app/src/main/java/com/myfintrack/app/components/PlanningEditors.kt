package com.myfintrack.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfintrack.app.domain.Money
import com.myfintrack.app.model.Account
import com.myfintrack.app.model.AccountType
import com.myfintrack.app.model.Budget
import com.myfintrack.app.model.Category
import com.myfintrack.app.model.Goal
import com.myfintrack.app.model.TransactionType
import com.myfintrack.app.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

val AccountType.icon: ImageVector
    get() = when (this) {
        AccountType.BANK -> Icons.Filled.AccountBalance
        AccountType.SAVINGS -> Icons.Filled.Savings
        AccountType.CASH -> Icons.Filled.Payments
        AccountType.WALLET -> Icons.Filled.AccountBalanceWallet
        AccountType.CREDIT_CARD -> Icons.Filled.CreditCard
        AccountType.OTHER -> Icons.Filled.Category
    }

/** Callback style used by all planning editors: the result is null on success or an error. */
typealias SaveResult = (String?) -> Unit

// ---------------------------------------------------------------------------------------------
// Shared building blocks
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FinSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = FinBorderLight) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = FinTextPrimary, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = FinTextSecondary)
                }
            }
            content()
        }
    }
}

/** Rupee amount input; accepts up to 9 integer digits and 2 decimals while typing. */
@Composable
fun RupeeAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    val helper: String? = error ?: supportingText
    val helperColor = if (error != null) FinExpense else FinTextMuted
    OutlinedTextField(
        value = value,
        onValueChange = { if (Money.isAcceptableWhileTyping(it)) onValueChange(it) },
        label = { Text(label) },
        prefix = { Text("₹ ", color = FinTextSecondary) },
        isError = error != null,
        supportingText = if (helper != null) {
            { Text(helper, color = helperColor) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        colors = finTextFieldColors(accent),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun SaveButton(label: String, accent: Color, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF052E16)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun DangerButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = FinExpense),
        border = BorderStroke(1.dp, FinExpense.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) Text(error, color = FinExpense, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = FinExpense, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = FinTextSecondary) } },
        containerColor = FinCard
    )
}

@Composable
private fun SelectChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = FinCard,
            labelColor = FinTextSecondary,
            selectedContainerColor = accent.copy(alpha = 0.18f),
            selectedLabelColor = accent
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = FinBorder,
            selectedBorderColor = accent.copy(alpha = 0.5f)
        )
    )
}

// ---------------------------------------------------------------------------------------------
// Account editor
// ---------------------------------------------------------------------------------------------

/**
 * Create / edit an account. For credit cards the user enters the amount currently owed,
 * which is stored as a negative opening balance.
 */
@Composable
fun AccountEditorSheet(
    existing: Account?,
    onDismiss: () -> Unit,
    onSave: (Account, SaveResult) -> Unit,
    onDelete: (Account, SaveResult) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: AccountType.BANK) }
    var balanceText by remember {
        mutableStateOf(
            existing?.let {
                val shown = if (it.type.isLiability) -it.openingBalancePaise else it.openingBalancePaise
                if (shown == 0L) "" else Money.toEditableString(kotlin.math.abs(shown))
            } ?: ""
        )
    }
    // Non-liability accounts may start overdrawn (rare); liabilities may start in credit.
    var balanceNegative by remember {
        mutableStateOf(
            existing?.let { if (it.type.isLiability) it.openingBalancePaise > 0 else it.openingBalancePaise < 0 } ?: false
        )
    }
    var archived by remember { mutableStateOf(existing?.archived ?: false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    EditorSheet(title = if (existing == null) "New account" else "Edit account", onDismiss = onDismiss) {
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 40) { name = it; nameError = null } },
            label = { Text("Account name") },
            placeholder = { Text("e.g. HDFC Savings, Cash, Paytm", color = FinTextMuted) },
            isError = nameError != null,
            supportingText = if (nameError != null) {
                { Text(nameError ?: "", color = FinExpense) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            colors = finTextFieldColors(FinAccent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OverlineLabel("Type")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccountType.entries.forEach { option ->
                SelectChip(option.label, type == option, FinAccent) { type = option }
            }
        }

        RupeeAmountField(
            value = balanceText,
            onValueChange = { balanceText = it; amountError = null },
            label = if (type.isLiability) "Amount owed today" else "Current balance",
            error = amountError,
            accent = FinAccent,
            supportingText = "Balance on the day you start tracking. After that, transactions update it."
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = balanceNegative,
                onCheckedChange = { balanceNegative = it },
                colors = CheckboxDefaults.colors(checkedColor = FinAccent, uncheckedColor = FinTextSecondary)
            )
            Text(
                if (type.isLiability) "Card is in credit (bank owes me)" else "Account is overdrawn (negative)",
                color = FinTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (existing != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Archived", color = FinTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Hidden from pickers. History and balance are kept.",
                        color = FinTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = archived,
                    onCheckedChange = { archived = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = FinAccent)
                )
            }
        }

        ErrorText(saveError)

        SaveButton(if (existing == null) "Add account" else "Save changes", FinAccent, enabled = !busy) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) nameError = "Enter an account name"
            val parsed = Money.parseOptionalToPaise(balanceText)
            if (parsed is Money.ParseResult.Invalid) amountError = parsed.reason
            if (trimmed.isNotEmpty() && parsed is Money.ParseResult.Valid) {
                // Stored sign: positive = money you have, negative = money you owe.
                val entered = if (balanceNegative) -parsed.paise else parsed.paise
                val opening = if (type.isLiability) -entered else entered
                val account = (existing ?: Account(name = trimmed, type = type)).copy(
                    name = trimmed,
                    type = type,
                    openingBalancePaise = opening,
                    archived = archived
                )
                busy = true
                saveError = null
                onSave(account) { error ->
                    busy = false
                    if (error == null) onDismiss() else saveError = error
                }
            }
        }

        if (existing != null) {
            DangerButton("Delete account", enabled = !busy) { confirmDelete = true }
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "Delete account?",
            message = "\"${existing.name}\" can only be deleted if it has no transactions. " +
                "Accounts with history should be archived instead.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                busy = true
                onDelete(existing) { error ->
                    busy = false
                    if (error == null) onDismiss() else saveError = error
                }
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Budget editor
// ---------------------------------------------------------------------------------------------

/**
 * Create / edit a monthly budget. [usedCategories] are categories that already have a budget
 * (the overall budget is represented by null).
 */
@Composable
fun BudgetEditorSheet(
    existing: Budget?,
    usedScopes: Set<Category?>,
    onDismiss: () -> Unit,
    onSave: (Budget, SaveResult) -> Unit,
    onDelete: (Budget, SaveResult) -> Unit
) {
    val expenseCategories = remember { Category.forType(TransactionType.EXPENSE) }
    val available: List<Category?> = remember(usedScopes, existing) {
        (listOf<Category?>(null) + expenseCategories).filter { it !in usedScopes || (existing != null && it == existing.category) }
    }
    var scope by remember { mutableStateOf(existing?.category ?: available.firstOrNull()) }
    var limitText by remember { mutableStateOf(existing?.let { Money.toEditableString(it.limitPaise) } ?: "") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val nothingLeft = existing == null && available.isEmpty()

    EditorSheet(title = if (existing == null) "New monthly budget" else "Edit budget", onDismiss = onDismiss) {
        if (nothingLeft) {
            Text(
                "Every category already has a budget. Edit an existing budget instead.",
                color = FinTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
        OverlineLabel("Applies to")
        if (existing != null) {
            Text(existing.category?.label ?: "All expenses", color = FinTextPrimary, fontWeight = FontWeight.SemiBold)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                available.chunked(3).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { option ->
                            SelectChip(option?.label ?: "All expenses", scope == option, FinAccent) { scope = option }
                        }
                    }
                }
            }
        }

        RupeeAmountField(
            value = limitText,
            onValueChange = { limitText = it; amountError = null },
            label = "Monthly limit",
            error = amountError,
            accent = FinAccent,
            supportingText = "Resets on the 1st of every month. Transfers never count."
        )

        ErrorText(saveError)

        SaveButton(if (existing == null) "Add budget" else "Save changes", FinAccent, enabled = !busy) {
            when (val parsed = Money.parseToPaise(limitText)) {
                is Money.ParseResult.Invalid -> amountError = parsed.reason
                is Money.ParseResult.Valid -> {
                    val budget = (existing ?: Budget(category = scope, limitPaise = parsed.paise))
                        .copy(category = existing?.category ?: scope, limitPaise = parsed.paise)
                    busy = true
                    saveError = null
                    onSave(budget) { error ->
                        busy = false
                        if (error == null) onDismiss() else saveError = error
                    }
                }
            }
        }

        if (existing != null) {
            DangerButton("Remove budget", enabled = !busy) { confirmDelete = true }
        }
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "Remove budget?",
            message = "The ${existing.category?.label ?: "overall"} budget will be removed. Your transactions are not affected.",
            confirmLabel = "Remove",
            onConfirm = {
                confirmDelete = false
                busy = true
                onDelete(existing) { error ->
                    busy = false
                    if (error == null) onDismiss() else saveError = error
                }
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Goal editor
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditorSheet(
    existing: Goal?,
    onDismiss: () -> Unit,
    onSave: (Goal, SaveResult) -> Unit,
    onDelete: (Goal, SaveResult) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var targetText by remember { mutableStateOf(existing?.let { Money.toEditableString(it.targetPaise) } ?: "") }
    var targetDate by remember { mutableStateOf(existing?.targetEpochDay?.let { LocalDate.ofEpochDay(it) }) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    EditorSheet(title = if (existing == null) "New savings goal" else "Edit goal", onDismiss = onDismiss) {
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 40) { name = it; nameError = null } },
            label = { Text("Goal name") },
            placeholder = { Text("e.g. Emergency fund, New laptop", color = FinTextMuted) },
            isError = nameError != null,
            supportingText = if (nameError != null) {
                { Text(nameError ?: "", color = FinExpense) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            colors = finTextFieldColors(FinAccent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        RupeeAmountField(
            value = targetText,
            onValueChange = { targetText = it; amountError = null },
            label = "Target amount",
            error = amountError,
            accent = FinAccent
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(FinCard)
                .border(1.dp, FinBorder, RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClickLabel = "Choose target date") { showDatePicker = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = FinTextSecondary, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text("Target date (optional)", color = FinTextSecondary, style = MaterialTheme.typography.bodySmall)
                Text(
                    targetDate?.let { longDayLabel(it, LocalDate.now(zone)) } ?: "No date",
                    color = FinTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (targetDate != null) {
                TextButton(onClick = { targetDate = null }) { Text("Clear", color = FinTextSecondary, fontSize = 12.sp) }
            }
        }

        Text(
            "Money you add to a goal stays in your accounts. It is not counted as an expense.",
            color = FinTextMuted,
            style = MaterialTheme.typography.bodySmall
        )

        ErrorText(saveError)

        SaveButton(if (existing == null) "Create goal" else "Save changes", FinAccent, enabled = !busy) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) nameError = "Enter a goal name"
            val parsed = Money.parseToPaise(targetText)
            if (parsed is Money.ParseResult.Invalid) amountError = parsed.reason
            if (trimmed.isNotEmpty() && parsed is Money.ParseResult.Valid) {
                val goal = (existing ?: Goal(name = trimmed, targetPaise = parsed.paise)).copy(
                    name = trimmed,
                    targetPaise = parsed.paise,
                    targetEpochDay = targetDate?.toEpochDay()
                )
                busy = true
                saveError = null
                onSave(goal) { error ->
                    busy = false
                    if (error == null) onDismiss() else saveError = error
                }
            }
        }

        if (existing != null) {
            DangerButton("Delete goal", enabled = !busy) { confirmDelete = true }
        }
    }

    if (showDatePicker) {
        val todayUtcMillis = LocalDate.now(zone).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (targetDate ?: LocalDate.now(zone).plusMonths(6))
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayUtcMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        targetDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK", color = FinAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = FinTextSecondary) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "Delete goal?",
            message = "\"${existing.name}\" and its saving history will be removed. Your accounts and transactions are not affected.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                busy = true
                onDelete(existing) { error ->
                    busy = false
                    if (error == null) onDismiss() else saveError = error
                }
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

/**
 * Add money to or withdraw money from a goal. [maxWithdrawPaise] is what is currently saved.
 * Calls [onSubmit] with a positive (add) or negative (withdraw) amount.
 */
@Composable
fun GoalContributionDialog(
    goalName: String,
    withdraw: Boolean,
    maxWithdrawPaise: Long,
    onDismiss: () -> Unit,
    onSubmit: (Long, SaveResult) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val accent = if (withdraw) FinWarning else FinAccent

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (withdraw) "Withdraw from $goalName" else "Add to $goalName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RupeeAmountField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = "Amount",
                    error = error,
                    accent = accent,
                    supportingText = if (withdraw) "Available: ${Money.format(maxWithdrawPaise)}"
                    else "Set aside from your accounts. Not an expense."
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                when (val parsed = Money.parseToPaise(amountText)) {
                    is Money.ParseResult.Invalid -> error = parsed.reason
                    is Money.ParseResult.Valid -> {
                        if (withdraw && parsed.paise > maxWithdrawPaise) {
                            error = "You can withdraw at most ${Money.format(maxWithdrawPaise)}"
                        } else {
                            busy = true
                            onSubmit(if (withdraw) -parsed.paise else parsed.paise) { result ->
                                busy = false
                                if (result == null) onDismiss() else error = result
                            }
                        }
                    }
                }
            }) { Text(if (withdraw) "Withdraw" else "Add", color = accent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = FinTextSecondary) } },
        containerColor = FinCard
    )
}
