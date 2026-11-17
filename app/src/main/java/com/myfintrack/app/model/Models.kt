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
    HOME, HISTORY, MONEY, ANALYSIS, SETTINGS
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
    /**
     * Income/expense: the account the money went into / came out of (optional).
     * Transfer: the source account (required).
     */
    val accountId: Long? = null,
    /** Transfer only: the destination account (required for transfers, null otherwise). */
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

// ---------------------------------------------------------------------------------------------
// Accounts
// ---------------------------------------------------------------------------------------------

enum class AccountType(val label: String) {
    BANK("Bank account"),
    SAVINGS("Savings"),
    CASH("Cash"),
    WALLET("Wallet / UPI"),
    CREDIT_CARD("Credit card"),
    OTHER("Other");

    /** Liabilities (money owed) are entered as a positive "amount owed" and stored negative. */
    val isLiability: Boolean get() = this == CREDIT_CARD

    companion object {
        fun fromKey(key: String?): AccountType = entries.firstOrNull { it.name == key } ?: OTHER
    }
}

data class Account(
    val id: Long = 0L,
    val name: String,
    val type: AccountType,
    /**
     * Balance when tracking started, in paise. Signed: negative means money owed
     * (e.g. an outstanding credit card bill).
     */
    val openingBalancePaise: Long = 0L,
    val archived: Boolean = false,
    val createdAt: Long = 0L
)

/** Current balance = opening balance + linked income − linked expenses ± transfers. */
data class AccountBalance(val account: Account, val balancePaise: Long)

// ---------------------------------------------------------------------------------------------
// Budgets
// ---------------------------------------------------------------------------------------------

/** A monthly spending limit. [category] == null is the overall budget for all expenses. */
data class Budget(
    val id: Long = 0L,
    val category: Category?,
    val limitPaise: Long,
    val createdAt: Long = 0L
)

enum class BudgetStatus { ON_TRACK, NEAR_LIMIT, OVER_LIMIT }

data class BudgetProgress(
    val budget: Budget,
    val spentPaise: Long,
    /** Remaining for the month; negative when over the limit. */
    val remainingPaise: Long,
    /** Spent as whole percent of the limit (can exceed 100). */
    val percentUsed: Int,
    val status: BudgetStatus,
    /** Remaining split evenly over the days left in the month (incl. today); 0 when over. */
    val perDayLeftPaise: Long
)

// ---------------------------------------------------------------------------------------------
// Goals
// ---------------------------------------------------------------------------------------------

/**
 * A savings goal. Money "added" to a goal is an allocation: it stays in the user's accounts
 * and is never counted as an expense or a transfer.
 */
data class Goal(
    val id: Long = 0L,
    val name: String,
    val targetPaise: Long,
    /** Optional target date as epoch day (LocalDate.toEpochDay()). */
    val targetEpochDay: Long? = null,
    val createdAt: Long = 0L
)

/** Positive = money set aside for the goal, negative = taken back out. */
data class GoalContribution(
    val id: Long = 0L,
    val goalId: Long,
    val amountPaise: Long,
    val occurredAt: Long,
    val note: String = ""
)

enum class GoalStatus { IN_PROGRESS, COMPLETED, OVERDUE }

data class GoalProgress(
    val goal: Goal,
    val savedPaise: Long,
    val remainingPaise: Long,
    /** 0..100 (capped). */
    val percent: Int,
    val status: GoalStatus,
    /** Amount to set aside per month to reach the target date; null without a date or when done. */
    val monthlyNeededPaise: Long?,
    /** Whole months left until the target date (min 1), null without a date. */
    val monthsLeft: Int?
)
