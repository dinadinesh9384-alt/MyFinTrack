package com.myfintrack.app.domain

import com.myfintrack.app.model.Category
import com.myfintrack.app.model.PaymentMethod
import com.myfintrack.app.model.Transaction
import com.myfintrack.app.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class FinanceCalculatorTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private var nextId = 1L

    private fun tx(
        type: TransactionType,
        paise: Long,
        at: LocalDateTime,
        category: Category = Category.defaultFor(type)
    ) = Transaction(
        id = nextId++,
        type = type,
        amountPaise = paise,
        title = "t",
        category = category,
        paymentMethod = PaymentMethod.UPI,
        occurredAt = at.atZone(zone).toInstant().toEpochMilli()
    )

    @Test
    fun transfersAreNeverIncomeOrExpense() {
        val d = LocalDateTime.of(2026, 11, 10, 10, 0)
        val totals = FinanceCalculator.totals(
            listOf(
                tx(TransactionType.INCOME, 100_000, d),
                tx(TransactionType.EXPENSE, 40_000, d),
                tx(TransactionType.TRANSFER, 500_000, d)
            )
        )
        assertEquals(100_000L, totals.incomePaise)
        assertEquals(40_000L, totals.expensePaise)
        assertEquals(60_000L, totals.netPaise)
        assertEquals(60, totals.savingsRatePercent)
    }

    @Test
    fun monthFilterRespectsBoundariesInLocalZone() {
        val list = listOf(
            tx(TransactionType.EXPENSE, 100, LocalDateTime.of(2026, 10, 31, 23, 59)),
            tx(TransactionType.EXPENSE, 200, LocalDateTime.of(2026, 11, 1, 0, 0)),
            tx(TransactionType.EXPENSE, 300, LocalDateTime.of(2026, 11, 30, 23, 59)),
            tx(TransactionType.EXPENSE, 400, LocalDateTime.of(2026, 12, 1, 0, 0))
        )
        val nov = FinanceCalculator.forMonth(list, YearMonth.of(2026, 11), zone)
        assertEquals(listOf(200L, 300L), nov.map { it.amountPaise })
    }

    @Test
    fun categoryBreakdownIsSortedAndUsesRealAmounts() {
        val d = LocalDateTime.of(2026, 11, 5, 12, 0)
        val breakdown = FinanceCalculator.categoryBreakdown(
            listOf(
                tx(TransactionType.EXPENSE, 30_000, d, Category.FOOD),
                tx(TransactionType.EXPENSE, 10_000, d, Category.TRANSPORT),
                tx(TransactionType.EXPENSE, 20_000, d, Category.FOOD),
                tx(TransactionType.INCOME, 999_999, d, Category.SALARY)
            )
        )
        assertEquals(listOf(Category.FOOD, Category.TRANSPORT), breakdown.map { it.category })
        assertEquals(50_000L, breakdown[0].amountPaise)
        assertEquals(0.8333f, breakdown[0].fraction, 0.001f)
    }

    @Test
    fun dailyExpenseBucketsByDay() {
        val daily = FinanceCalculator.dailyExpense(
            listOf(
                tx(TransactionType.EXPENSE, 100, LocalDateTime.of(2026, 11, 1, 9, 0)),
                tx(TransactionType.EXPENSE, 50, LocalDateTime.of(2026, 11, 1, 20, 0)),
                tx(TransactionType.INCOME, 9_999, LocalDateTime.of(2026, 11, 2, 9, 0)),
                tx(TransactionType.EXPENSE, 70, LocalDateTime.of(2026, 11, 30, 9, 0))
            ),
            YearMonth.of(2026, 11),
            zone
        )
        assertEquals(30, daily.size)
        assertEquals(150L, daily[0])
        assertEquals(0L, daily[1])
        assertEquals(70L, daily[29])
    }

    @Test
    fun expenseChangeComparesSamePeriodOfLastMonth() {
        val list = listOf(
            tx(TransactionType.EXPENSE, 10_000, LocalDateTime.of(2026, 10, 5, 10, 0)),
            tx(TransactionType.EXPENSE, 99_000, LocalDateTime.of(2026, 10, 25, 10, 0)), // after same period
            tx(TransactionType.EXPENSE, 15_000, LocalDateTime.of(2026, 11, 5, 10, 0))
        )
        val now = LocalDateTime.of(2026, 11, 17, 12, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(50, FinanceCalculator.expenseChangeVsLastMonth(list, now, zone))
    }

    @Test
    fun expenseChangeIsNullWithoutPreviousData() {
        val now = LocalDateTime.of(2026, 11, 17, 12, 0).atZone(zone).toInstant().toEpochMilli()
        assertNull(FinanceCalculator.expenseChangeVsLastMonth(emptyList(), now, zone))
    }
}
