package com.myfintrack.app.domain

import com.myfintrack.app.model.Category
import com.myfintrack.app.model.CategorySpending
import com.myfintrack.app.model.Transaction
import com.myfintrack.app.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Pure calculations over real transactions. Transfers are always excluded from
 * income/expense totals so they can never be double-counted.
 */
object FinanceCalculator {

    data class Totals(val incomePaise: Long, val expensePaise: Long) {
        val netPaise: Long get() = incomePaise - expensePaise

        /** Savings rate in whole percent, or null when there is no income. */
        val savingsRatePercent: Int?
            get() = if (incomePaise <= 0L) null else ((netPaise * 100L) / incomePaise).toInt()

        companion object {
            val ZERO = Totals(0L, 0L)
        }
    }

    data class MonthTotals(val month: YearMonth, val totals: Totals)

    fun totals(transactions: List<Transaction>): Totals {
        var income = 0L
        var expense = 0L
        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME -> income += tx.amountPaise
                TransactionType.EXPENSE -> expense += tx.amountPaise
                TransactionType.TRANSFER -> Unit // never income or expense
            }
        }
        return Totals(income, expense)
    }

    /** [start, endExclusive) in epoch millis for a calendar month in [zone]. */
    fun monthRange(month: YearMonth, zone: ZoneId): LongRange {
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start until end
    }

    fun inRange(transactions: List<Transaction>, range: LongRange): List<Transaction> =
        transactions.filter { it.occurredAt in range }

    fun forMonth(transactions: List<Transaction>, month: YearMonth, zone: ZoneId): List<Transaction> =
        inRange(transactions, monthRange(month, zone))

    fun localDate(epochMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    /** Category breakdown for [type], largest first. Fractions sum to ~1. */
    fun categoryBreakdown(
        transactions: List<Transaction>,
        type: TransactionType = TransactionType.EXPENSE
    ): List<CategorySpending> {
        val sums = LinkedHashMap<Category, Long>()
        for (tx in transactions) {
            if (tx.type != type) continue
            sums[tx.category] = (sums[tx.category] ?: 0L) + tx.amountPaise
        }
        val total = sums.values.sum()
        if (total <= 0L) return emptyList()
        return sums.entries
            .sortedByDescending { it.value }
            .map { CategorySpending(it.key, it.value, it.value.toFloat() / total.toFloat()) }
    }

    /** Expense per day of [month]; index 0 = day 1. */
    fun dailyExpense(transactions: List<Transaction>, month: YearMonth, zone: ZoneId): LongArray {
        val result = LongArray(month.lengthOfMonth())
        for (tx in forMonth(transactions, month, zone)) {
            if (tx.type != TransactionType.EXPENSE) continue
            val day = localDate(tx.occurredAt, zone).dayOfMonth
            result[day - 1] += tx.amountPaise
        }
        return result
    }

    /** Totals for the [count] months ending with [endMonth], oldest first. */
    fun monthlyTotals(
        transactions: List<Transaction>,
        endMonth: YearMonth,
        count: Int,
        zone: ZoneId
    ): List<MonthTotals> {
        val months = (count - 1 downTo 0).map { endMonth.minusMonths(it.toLong()) }
        return months.map { MonthTotals(it, totals(forMonth(transactions, it, zone))) }
    }

    /**
     * Percent change of expenses from the same period of the previous month
     * (day 1 up to the same elapsed time). Null when the previous period had no expenses.
     */
    fun expenseChangeVsLastMonth(
        transactions: List<Transaction>,
        nowMillis: Long,
        zone: ZoneId
    ): Int? {
        val thisMonth = YearMonth.from(Instant.ofEpochMilli(nowMillis).atZone(zone))
        val thisStart = monthRange(thisMonth, zone).first
        val lastRange = monthRange(thisMonth.minusMonths(1), zone)
        val elapsed = nowMillis - thisStart
        val lastEnd = minOf(lastRange.first + elapsed, lastRange.last + 1)
        val previous = totals(inRange(transactions, lastRange.first until lastEnd)).expensePaise
        if (previous <= 0L) return null
        val current = totals(inRange(transactions, thisStart..nowMillis)).expensePaise
        return (((current - previous) * 100L) / previous).toInt()
    }

    /** Percent change between two amounts, null when [previous] is zero. */
    fun percentChange(current: Long, previous: Long): Int? =
        if (previous <= 0L) null else (((current - previous) * 100L) / previous).toInt()
}
