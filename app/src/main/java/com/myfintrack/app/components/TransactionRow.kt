package com.myfintrack.app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfintrack.app.domain.Money
import com.myfintrack.app.model.Transaction
import com.myfintrack.app.model.TransactionType
import com.myfintrack.app.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
private val DAY_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val FULL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val WEEKDAY_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

/** "Today", "Yesterday", "12 Nov" (this year) or "12 Nov 2025". */
fun relativeDayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    date.year == today.year -> DAY_MONTH_FORMAT.format(date)
    else -> FULL_DATE_FORMAT.format(date)
}

fun longDayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    else -> WEEKDAY_DATE_FORMAT.format(date)
}

fun formatTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    TIME_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

fun transactionDateLabel(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    return relativeDayLabel(date) + ", " + formatTime(epochMillis, zone)
}

/**
 * One transaction. [showDate] = false is used in History where rows are grouped under a date header.
 * [accountNames] (account id -> name) shows the linked account, or "From → To" for transfers.
 */
@Composable
fun TransactionRow(
    transaction: Transaction,
    hideAmounts: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
    accountNames: Map<Long, String> = emptyMap()
) {
    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> FinAccent
        TransactionType.EXPENSE -> FinExpense
        TransactionType.TRANSFER -> FinTransfer
    }
    val amountText = when {
        hideAmounts -> Money.MASK
        transaction.type == TransactionType.TRANSFER -> Money.format(transaction.amountPaise)
        else -> Money.formatSigned(transaction.amountPaise, transaction.type == TransactionType.INCOME)
    }
    val secondary = if (showDate) transactionDateLabel(transaction.occurredAt) else formatTime(transaction.occurredAt)
    val typeWord = when (transaction.type) {
        TransactionType.INCOME -> "Income"
        TransactionType.EXPENSE -> "Expense"
        TransactionType.TRANSFER -> "Transfer"
    }
    val fromName = transaction.accountId?.let { accountNames[it] }
    val toName = transaction.toAccountId?.let { accountNames[it] }
    val context = if (transaction.type == TransactionType.TRANSFER) {
        "${fromName ?: "?"} → ${toName ?: "?"}"
    } else {
        listOfNotNull(transaction.category.label, fromName).joinToString(" · ")
    }
    val trailing = if (transaction.type == TransactionType.TRANSFER) "Transfer" else transaction.paymentMethod.label
    val spokenContext = if (transaction.type == TransactionType.TRANSFER) {
        "from ${fromName ?: "unknown account"} to ${toName ?: "unknown account"}"
    } else context
    val spoken = "$typeWord, ${transaction.title}, " +
        (if (hideAmounts) "amount hidden" else Money.format(transaction.amountPaise)) +
        ", $spokenContext, $secondary"

    FinSurfaceCard(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = spoken },
        onClick = onClick,
        contentPadding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryBadge(transaction.category)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    color = FinTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$context · $secondary",
                    color = FinTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountText,
                    color = amountColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = trailing,
                    color = FinTextMuted,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}
