package com.myfintrack.app.model

/**
 * Core domain models.
 *
 * Money is ALWAYS stored as integer minor units (paise, 1 rupee = 100 paise) in a [Long].
 * Never use Double/Float for amounts. Use [com.myfintrack.app.domain.Money] to parse and format.
 */

enum class TransactionType {
    INCOME,
    EXPENSE,

    /**
     * Movement of money between the user's own accounts.
     * Transfers are never counted as income or expense.
     */
    TRANSFER;

    companion object {
        fun fromKey(key: String?): TransactionType =
            entries.firstOrNull { it.name == key } ?: EXPENSE
    }
}

enum class NavTab {
    HOME, HISTORY, ANALYSIS, SETTINGS
}

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    UPI("UPI"),
    DEBIT_CARD("Debit card"),
    CREDIT_CARD("Credit card"),
    BANK_TRANSFER("Bank transfer"),
    WALLET("Wallet"),
    OTHER("Other");

    companion object {
        fun fromKey(key: String?): PaymentMethod =
            entries.firstOrNull { it.name == key } ?: OTHER
    }
}

enum class Category(val label: String, val type: TransactionType) {
    // Expense categories
    FOOD("Food & dining", TransactionType.EXPENSE),
    GROCERIES("Groceries", TransactionType.EXPENSE),
    TRANSPORT("Transport", TransactionType.EXPENSE),
    FUEL("Fuel", TransactionType.EXPENSE),
    SHOPPING("Shopping", TransactionType.EXPENSE),
    BILLS("Bills & utilities", TransactionType.EXPENSE),
    RENT("Rent", TransactionType.EXPENSE),
    HEALTH("Health", TransactionType.EXPENSE),
    EDUCATION("Education", TransactionType.EXPENSE),
    ENTERTAINMENT("Entertainment", TransactionType.EXPENSE),
    TRAVEL("Travel", TransactionType.EXPENSE),
    PERSONAL_CARE("Personal care", TransactionType.EXPENSE),
    FRIENDS_FAMILY("Friends & family", TransactionType.EXPENSE),
    OTHER_EXPENSE("Other", TransactionType.EXPENSE),

    // Income categories
    SALARY("Salary", TransactionType.INCOME),
    BUSINESS("Business", TransactionType.INCOME),
    FREELANCE("Freelance", TransactionType.INCOME),
    INVESTMENT("Investments", TransactionType.INCOME),
    GIFT("Gifts", TransactionType.INCOME),
    REFUND("Refunds", TransactionType.INCOME),
    OTHER_INCOME("Other", TransactionType.INCOME),

    // Transfer (not income, not expense)
    TRANSFER("Transfer", TransactionType.TRANSFER);

    companion object {
        fun forType(type: TransactionType): List<Category> = entries.filter { it.type == type }

        fun defaultFor(type: TransactionType): Category = when (type) {
            TransactionType.INCOME -> OTHER_INCOME
            TransactionType.EXPENSE -> OTHER_EXPENSE
            TransactionType.TRANSFER -> TRANSFER
        }

        fun fromKey(key: String?, type: TransactionType): Category {
            val match = entries.firstOrNull { it.name == key }
            return if (match != null && match.type == type) match else defaultFor(type)
        }
    }
}

data class Transaction(
    val id: Long = 0L,
    val type: TransactionType,
    /** Always > 0. Direction is defined by [type]. */
    val amountPaise: Long,
    val title: String,
    val category: Category,
    val paymentMethod: PaymentMethod,
    val note: String = "",
    /** Epoch milliseconds (UTC instant) of when the transaction happened. */
    val occurredAt: Long,
    /** Reserved for the accounts feature (Step 3). Null until accounts exist. */
    val accountId: Long? = null,
    /** Destination account for transfers (Step 3). */
    val toAccountId: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

/** Spending (or income) of one category within a period, computed from real transactions. */
data class CategorySpending(
    val category: Category,
    val amountPaise: Long,
    /** Share of the period total, 0f..1f. A ratio, not money. */
    val fraction: Float
)

data class AppSettings(
    val hideBalances: Boolean = false,
    val displayName: String = ""
)
