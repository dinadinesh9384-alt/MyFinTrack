package com.myfintrack.app.domain

import com.myfintrack.app.model.Account
import com.myfintrack.app.model.AccountType
import com.myfintrack.app.model.Budget
import com.myfintrack.app.model.BudgetStatus
import com.myfintrack.app.model.Category
import com.myfintrack.app.model.Goal
import com.myfintrack.app.model.GoalContribution
import com.myfintrack.app.model.GoalStatus
import com.myfintrack.app.model.PaymentMethod
import com.myfintrack.app.model.Transaction
import com.myfintrack.app.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class PlanningCalculatorTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private var nextId = 1L
    private val day = LocalDateTime.of(2026, 11, 10, 10, 0)

    private val bank = Account(id = 1, name = "Bank", type = AccountType.BANK, openingBalancePaise = 50_000_00)
    private val cash = Account(id = 2, name = "Cash", type = AccountType.CASH, openingBalancePaise = 2_000_00)
    private val card = Account(id = 3, name = "Card", type = AccountType.CREDIT_CARD, openingBalancePaise = -10_000_00)

    private fun tx(
        type: TransactionType,
        paise: Long,
        account: Long? = null,
        to: Long? = null,
        category: Category = Category.defaultFor(type),
        at: LocalDateTime = day
    ) = Transaction(
        id = nextId++,
        type = type,
        amountPaise = paise,
        title = "t",
        category = category,
        paymentMethod = PaymentMethod.UPI,
        occurredAt = at.atZone(zone).toInstant().toEpochMilli(),
        accountId = account,
        toAccountId = to
    )

    @Test
    fun transferMovesMoneyWithoutChangingTotal() {
        val accounts = listOf(bank, cash)
        val before = PlanningCalculator.totalBalance(accounts, emptyList())
        val txs = listOf(tx(TransactionType.TRANSFER, 5_000_00, account = 1, to = 2))
        val balances = PlanningCalculator.accountBalances(accounts, txs)
        assertEquals(45_000_00L, balances[0].balancePaise)
        assertEquals(7_000_00L, balances[1].balancePaise)
        assertEquals(before, PlanningCalculator.totalBalance(accounts, txs))
        assertEquals(before, balances.sumOf { it.balancePaise })
    }

    @Test
    fun incomeAndExpenseChangeLinkedAccountOnly() {
        val txs = listOf(
            tx(TransactionType.INCOME, 30_000_00, account = 1, category = Category.SALARY),
            tx(TransactionType.EXPENSE, 500_00, account = 2, category = Category.FOOD)
        )
        val balances = PlanningCalculator.accountBalances(listOf(bank, cash), txs)
        assertEquals(80_000_00L, balances[0].balancePaise)
        assertEquals(1_500_00L, balances[1].balancePaise)
    }

    @Test
    fun creditCardRepaymentIsATransferNotAnExpense() {
        val txs = listOf(
            tx(TransactionType.EXPENSE, 2_000_00, account = 3, category = Category.SHOPPING),
            tx(TransactionType.TRANSFER, 12_000_00, account = 1, to = 3)
        )
        val balances = PlanningCalculator.accountBalances(listOf(bank, card), txs)
        assertEquals(38_000_00L, balances[0].balancePaise)
        assertEquals(0L, balances[1].balancePaise) // card fully paid
        // Only the shopping counts as spending; the repayment does not.
        assertEquals(2_000_00L, FinanceCalculator.totals(txs).expensePaise)
    }

    @Test
    fun totalBalanceEqualsAccountsPlusUnlinked() {
        val accounts = listOf(bank, cash, card)
        val txs = listOf(
            tx(TransactionType.INCOME, 1_000_00, account = null),
            tx(TransactionType.EXPENSE, 300_00, account = null),
            tx(TransactionType.EXPENSE, 700_00, account = 2),
            tx(TransactionType.TRANSFER, 100_00, account = 1, to = 2)
        )
        val sumAccounts = PlanningCalculator.accountBalances(accounts, txs).sumOf { it.balancePaise }
        val unlinked = PlanningCalculator.unlinkedNet(txs)
        assertEquals(700_00L, unlinked)
        assertEquals(PlanningCalculator.totalBalance(accounts, txs), sumAccounts + unlinked)
    }

    @Test
    fun transferWithMissingSideMovesNothing() {
        val txs = listOf(tx(TransactionType.TRANSFER, 1_000_00, account = 1, to = null))
        val balances = PlanningCalculator.accountBalances(listOf(bank), txs)
        assertEquals(50_000_00L, balances[0].balancePaise)
    }

    @Test
    fun budgetProgressUsesOnlyThisMonthsExpensesInCategory() {
        val month = YearMonth.of(2026, 11)
        val today = LocalDate.of(2026, 11, 21) // 10 days left incl. today
        val txs = listOf(
            tx(TransactionType.EXPENSE, 4_000_00, category = Category.FOOD),
            tx(TransactionType.EXPENSE, 9_000_00, category = Category.FOOD, at = day.minusMonths(1)),
            tx(TransactionType.EXPENSE, 1_000_00, category = Category.SHOPPING),
            tx(TransactionType.TRANSFER, 50_000_00, account = 1, to = 2, category = Category.TRANSFER),
            tx(TransactionType.INCOME, 70_000_00, category = Category.SALARY)
        )
        val budgets = listOf(
            Budget(id = 1, category = Category.FOOD, limitPaise = 5_000_00),
            Budget(id = 2, category = null, limitPaise = 4_000_00)
        )
        val result = PlanningCalculator.budgetProgress(budgets, txs, month, today, zone)

        val food = result[0]
        assertEquals(4_000_00L, food.spentPaise)
        assertEquals(80, food.percentUsed)
        assertEquals(BudgetStatus.NEAR_LIMIT, food.status)
        assertEquals(100_00L, food.perDayLeftPaise) // ₹1,000 left over 10 days

        val overall = result[1]
        assertEquals(5_000_00L, overall.spentPaise) // transfers and income excluded
        assertEquals(BudgetStatus.OVER_LIMIT, overall.status)
        assertEquals(-1_000_00L, overall.remainingPaise)
        assertEquals(0L, overall.perDayLeftPaise)
    }

    @Test
    fun daysLeftInMonth() {
        val today = LocalDate.of(2026, 11, 30)
        assertEquals(1, PlanningCalculator.daysLeftInMonth(YearMonth.of(2026, 11), today))
        assertEquals(0, PlanningCalculator.daysLeftInMonth(YearMonth.of(2026, 10), today))
        assertEquals(31, PlanningCalculator.daysLeftInMonth(YearMonth.of(2026, 12), today))
    }

    @Test
    fun goalProgressAndMonthlyPlan() {
        val today = LocalDate.of(2026, 11, 17)
        val goal = Goal(id = 7, name = "Trip", targetPaise = 60_000_00, targetEpochDay = LocalDate.of(2027, 5, 17).toEpochDay())
        val contributions = listOf(
            GoalContribution(goalId = 7, amountPaise = 15_000_00, occurredAt = 0),
            GoalContribution(goalId = 7, amountPaise = 5_000_00, occurredAt = 0),
            GoalContribution(goalId = 7, amountPaise = -2_000_00, occurredAt = 0),
            GoalContribution(goalId = 99, amountPaise = 1_000_00, occurredAt = 0)
        )
        val p = PlanningCalculator.goalProgress(listOf(goal), contributions, today).single()
        assertEquals(18_000_00L, p.savedPaise)
        assertEquals(42_000_00L, p.remainingPaise)
        assertEquals(30, p.percent)
        assertEquals(GoalStatus.IN_PROGRESS, p.status)
        assertEquals(6, p.monthsLeft)
        assertEquals(7_000_00L, p.monthlyNeededPaise)
    }

    @Test
    fun goalCompletedAndOverdue() {
        val today = LocalDate.of(2026, 11, 17)
        val done = PlanningCalculator.progressOf(Goal(id = 1, name = "a", targetPaise = 100_00), 150_00, today)
        assertEquals(GoalStatus.COMPLETED, done.status)
        assertEquals(100, done.percent)
        assertNull(done.monthlyNeededPaise)

        val late = PlanningCalculator.progressOf(
            Goal(id = 2, name = "b", targetPaise = 100_00, targetEpochDay = LocalDate.of(2026, 11, 1).toEpochDay()),
            10_00,
            today
        )
        assertEquals(GoalStatus.OVERDUE, late.status)
        assertNull(late.monthlyNeededPaise)
    }

    @Test
    fun monthsUntilRoundsUp() {
        val from = LocalDate.of(2026, 11, 17)
        assertEquals(1, PlanningCalculator.monthsUntil(from, LocalDate.of(2026, 11, 20)))
        assertEquals(1, PlanningCalculator.monthsUntil(from, LocalDate.of(2026, 12, 17)))
        assertEquals(2, PlanningCalculator.monthsUntil(from, LocalDate.of(2026, 12, 18)))
        assertEquals(1, PlanningCalculator.monthsUntil(from, from))
    }

    @Test
    fun parseOptionalAllowsZeroAndBlank() {
        assertEquals(Money.ParseResult.Valid(0L), Money.parseOptionalToPaise(""))
        assertEquals(Money.ParseResult.Valid(0L), Money.parseOptionalToPaise("0.00"))
        assertEquals(Money.ParseResult.Valid(12_345L), Money.parseOptionalToPaise("123.45"))
        assertTrue(Money.parseOptionalToPaise("-5") is Money.ParseResult.Invalid)
    }
}
