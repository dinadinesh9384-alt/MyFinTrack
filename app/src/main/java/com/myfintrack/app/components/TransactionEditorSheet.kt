package com.myfintrack.app.components

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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfintrack.app.domain.Money
import com.myfintrack.app.model.Category
import com.myfintrack.app.model.PaymentMethod
import com.myfintrack.app.model.Transaction
import com.myfintrack.app.model.TransactionType
import com.myfintrack.app.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** What the editor should open with: a new transaction of [initialType], or an existing one. */
data class EditorRequest(
    val initialType: TransactionType,
    val existing: Transaction? = null
)

/**
 * Add / edit transaction bottom sheet.
 * Amounts are parsed with BigDecimal into paise. Income and expense only; transfers between
 * accounts are added in Step 3 together with the accounts feature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditorSheet(
    request: EditorRequest,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    val existing = request.existing
    val zone = remember { ZoneId.systemDefault() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(existing?.type ?: request.initialType) }
    var amountText by remember { mutableStateOf(existing?.let { Money.toEditableString(it.amountPaise) } ?: "") }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: Category.defaultFor(type)) }
    var paymentMethod by remember { mutableStateOf(existing?.paymentMethod ?: PaymentMethod.UPI) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var date by remember {
        mutableStateOf(
            existing?.let { Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate() } ?: LocalDate.now(zone)
        )
    }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val accent = if (type == TransactionType.INCOME) FinAccent else FinExpense

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existing == null) "New transaction" else "Edit transaction",
                    color = FinTextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = FinTextSecondary)
                }
            }

            if (type != TransactionType.TRANSFER) {
                TypeToggle(
                    selected = type,
                    onSelect = { newType ->
                        if (newType != type) {
                            type = newType
                            category = Category.defaultFor(newType)
                        }
                    }
                )
            }

            // Amount
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FinCard, RoundedCornerShape(18.dp))
                    .border(1.dp, if (amountError != null) FinExpense else FinBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OverlineLabel("Amount")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹", color = accent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    TextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (Money.isAcceptableWhileTyping(input)) {
                                amountText = input
                                amountError = null
                            }
                        },
                        placeholder = { Text("0", color = FinTextMuted, fontSize = 30.sp, fontWeight = FontWeight.Bold) },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(color = FinTextPrimary, fontSize = 30.sp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = accent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Amount in rupees" }
                    )
                }
                amountError?.let {
                    Text(it, color = FinExpense, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Category grid
            OverlineLabel("Category")
            CategoryGrid(
                categories = Category.forType(type),
                selected = category,
                onSelect = { category = it }
            )

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 60) title = it },
                label = { Text("Title (optional)") },
                placeholder = { Text(category.label, color = FinTextMuted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                colors = finTextFieldColors(accent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Payment method
            OverlineLabel("Paid via")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = paymentMethod == method,
                        onClick = { paymentMethod = method },
                        label = { Text(method.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = FinCard,
                            labelColor = FinTextSecondary,
                            selectedContainerColor = accent.copy(alpha = 0.18f),
                            selectedLabelColor = accent
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = paymentMethod == method,
                            borderColor = FinBorder,
                            selectedBorderColor = accent.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(FinCard)
                    .border(1.dp, FinBorder, RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClickLabel = "Change date") { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = FinTextSecondary, modifier = Modifier.size(18.dp))
                Column(Modifier.weight(1f)) {
                    Text("Date", color = FinTextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text(longDayLabel(date, LocalDate.now(zone)), color = FinTextPrimary, fontWeight = FontWeight.SemiBold)
                }
                Text("Change", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 200) note = it },
                label = { Text("Note (optional)") },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = finTextFieldColors(accent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    when (val parsed = Money.parseToPaise(amountText)) {
                        is Money.ParseResult.Invalid -> amountError = parsed.reason
                        is Money.ParseResult.Valid -> {
                            val time = existing?.let {
                                Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalTime()
                            } ?: LocalTime.now(zone)
                            val occurredAt = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
                            val base = existing ?: Transaction(
                                type = type,
                                amountPaise = parsed.paise,
                                title = "",
                                category = category,
                                paymentMethod = paymentMethod,
                                occurredAt = occurredAt
                            )
                            onSave(
                                base.copy(
                                    type = type,
                                    amountPaise = parsed.paise,
                                    title = title.trim().ifEmpty { category.label },
                                    category = category,
                                    paymentMethod = paymentMethod,
                                    note = note.trim(),
                                    occurredAt = occurredAt
                                )
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = if (type == TransactionType.INCOME) Color(0xFF052E16) else Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = when {
                        existing != null -> "Save changes"
                        type == TransactionType.INCOME -> "Add income"
                        else -> "Add expense"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            if (existing != null) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FinExpense),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FinExpense.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete transaction", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showDatePicker) {
        val todayUtcMillis = LocalDate.now(zone).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtcMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
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
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete transaction?") },
            text = { Text("\"${existing.title}\" will be removed. You can undo this right after.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(existing)
                }) { Text("Delete", color = FinExpense, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = FinTextSecondary) }
            },
            containerColor = FinCard
        )
    }
}

@Composable
private fun TypeToggle(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FinCard, RoundedCornerShape(14.dp))
            .border(1.dp, FinBorder, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        listOf(TransactionType.EXPENSE to "Expense", TransactionType.INCOME to "Income").forEach { (option, label) ->
            val isSelected = option == selected
            val color = if (option == TransactionType.INCOME) FinAccent else FinExpense
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) color.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(option) }
                    .semantics { this.selected = isSelected }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) color else FinTextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<Category>,
    selected: Category,
    onSelect: (Category) -> Unit,
    columns: Int = 4
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(columns).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { cat ->
                    val isSelected = cat == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) cat.color.copy(alpha = 0.16f) else FinCard)
                            .border(
                                1.dp,
                                if (isSelected) cat.color.copy(alpha = 0.6f) else FinBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(role = Role.RadioButton) { onSelect(cat) }
                            .semantics { this.selected = isSelected }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(cat.icon, contentDescription = null, tint = cat.color, modifier = Modifier.size(20.dp))
                        Text(
                            text = cat.label,
                            color = if (isSelected) FinTextPrimary else FinTextSecondary,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // Keep cells equal width on the last row
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun finTextFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    unfocusedBorderColor = FinBorder,
    focusedLabelColor = accent,
    unfocusedLabelColor = FinTextSecondary,
    cursorColor = accent,
    focusedTextColor = FinTextPrimary,
    unfocusedTextColor = FinTextPrimary,
    focusedContainerColor = FinCard,
    unfocusedContainerColor = FinCard
)
