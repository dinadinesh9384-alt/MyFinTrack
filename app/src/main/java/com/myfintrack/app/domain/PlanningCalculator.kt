package com.myfintrack.app.domain

import com.myfintrack.app.model.Account
import com.myfintrack.app.model.AccountBalance
import com.myfintrack.app.model.Budget
import com.myfintrack.app.model.BudgetProgress
import com.myfintrack.app.model.BudgetStatus
import com.myfintrack.app.model.Goal
import com.myfintrack.app.model.GoalContribution
import com.myfintrack.app.model.GoalProgress
import com.myfintrack.app.model.GoalStatus
import com.myfintrack.app.model.Transaction
import com.myfintrack.app.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Pure calculations for accounts, budgets and goals. All money is integer paise.
 *
 * Invariants:
 *  - A transfer moves money between two accounts: it lowers the source and raises the
 *    destination by the same amount, so the total balance is unchanged.
 *  - Goal contributions are allocations; they never change balances, income or expenses.
 */
object PlanningCalculator {

    /** Threshold (percent of limit) from which a budget is reported as near its limit. */
    const val NEAR_LIMIT_PERCENT = 80

    /** Current balance of every account in [accounts], same order. */
    fun accountBalances(accounts: List<Account>, transactions: List<Transaction>): List<AccountBalance> {
        val delta = HashMap<Long, Long>()
        fun add(id: Long?, amount: Long) {
            if (id != null) delta[id] = (delta[id] ?: 0L) + amount
        }
        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME -> add(tx.accountId, tx.amountPaise)
                TransactionType.EXPENSE -> add(tx.accountId, -tx.amountPaise)
                TransactionType.TRANSFER -> {
                    // Only move money when both sides are known, so a broken link can never
                    // create or destroy money.
                    if (tx.accountId != null && tx.toAccountId != null) {
                        add(tx.accountId, -tx.amountPaise)
                        add(tx.toAccountId, tx.amountPaise)
                    }
                }
            }
        }
        return accounts.map { AccountBalance(it, it.openingBalancePaise + (delta[it.id] ?: 0L)) }
    }

    /**
     * Net of income and expenses that are not linked to any account (recorded before
     * accounts existed, or with "No account").
     */
    fun unlinkedNet(transactions: List<Transaction>): Long =
        FinanceCalculator.totals(transactions.filter { it.accountId == null && it.type != TransactionType.TRANSFER })
            .netPaise

    /**
     * Total money the user has: sum of all opening balances plus all income minus all expenses.
     * Transfers cancel out. Equals the sum of [accountBalances] plus [unlinkedNet].
     */
    fun totalBalance(accounts: List<Account>, transactions: List<Transaction>): Long =
        accounts.sumOf { it.openingBalancePaise } + FinanceCalculator.totals(transactions).netPaise

    /** Progress of each budget for [month]. */
    fun budgetProgress(
        budgets: List<Budget>,
        transactions: List<Transaction>,
        month: YearMonth,
        today: LocalDate,
        zone: ZoneId
    ): List<BudgetProgress> {
        val monthExpenses = FinanceCalculator.forMonth(transactions, month, zone)
            .filter { it.type == TransactionType.EXPENSE }
        val total = monthExpenses.sumOf { it.amountPaise }
        val byCategory = monthExpenses.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amountPaise } }
        val daysLeft = daysLeftInMonth(month, today)
        return budgets.map { budget ->
            val spent = if (budget.category == null) total else byCategory[budget.category] ?: 0L
            progressOf(budget, spent, daysLeft)
        }
    }

    internal fun progressOf(budget: Budget, spentPaise: Long, daysLeft: Int): BudgetProgress {
        val remaining = budget.limitPaise - spentPaise
        val percent = if (budget.limitPaise <= 0L) 0
        else ((spentPaise * 100L) / budget.limitPaise).coerceAtMost(9_999L).toInt()
        val status = when {
            spentPaise > budget.limitPaise -> BudgetStatus.OVER_LIMIT
            percent >= NEAR_LIMIT_PERCENT -> BudgetStatus.NEAR_LIMIT
            else -> BudgetStatus.ON_TRACK
        }
        val perDay = if (remaining <= 0L || daysLeft <= 0) 0L else remaining / daysLeft
        return BudgetProgress(budget, spentPaise, remaining, percent, status, perDay)
    }

    /**
     * Days left in [month] counting today. 0 for past months, full length for future months.
     */
    fun daysLeftInMonth(month: YearMonth, today: LocalDate): Int {
        val current = YearMonth.from(today)
        return when {
            month < current -> 0
            month > current -> month.lengthOfMonth()
            else -> month.lengthOfMonth() - today.dayOfMonth + 1
        }
    }

    fun goalProgress(
        goals: List<Goal>,
        contributions: List<GoalContribution>,
        today: LocalDate
    ): List<GoalProgress> {
        val saved = contributions.groupBy { it.goalId }.mapValues { (_, list) -> list.sumOf { it.amountPaise } }
        return goals.map { progressOf(it, saved[it.id] ?: 0L, today) }
    }

    internal fun progressOf(goal: Goal, savedPaise: Long, today: LocalDate): GoalProgress {
        val remaining = (goal.targetPaise - savedPaise).coerceAtLeast(0L)
        val percent = if (goal.targetPaise <= 0L) 0
        else ((savedPaise.coerceAtLeast(0L) * 100L) / goal.targetPaise).coerceAtMost(100L).toInt()
        val target = goal.targetEpochDay?.let { LocalDate.ofEpochDay(it) }
        val done = savedPaise >= goal.targetPaise
        val status = when {
            done -> GoalStatus.COMPLETED
            target != null && today.isAfter(target) -> GoalStatus.OVERDUE
            else -> GoalStatus.IN_PROGRESS
        }
        val monthsLeft = target?.let { monthsUntil(today, it) }
        val monthlyNeeded = if (done || monthsLeft == null || status == GoalStatus.OVERDUE) null
        else ceilDiv(remaining, monthsLeft.toLong())
        return GoalProgress(goal, savedPaise, remaining, percent, status, monthlyNeeded, monthsLeft)
    }

    /** Whole months from [from] to [to], rounded up, at least 1. */
    fun monthsUntil(from: LocalDate, to: LocalDate): Int {
        if (!to.isAfter(from)) return 1
        val whole = ChronoUnit.MONTHS.between(from, to)
        val rest = from.plusMonths(whole).isBefore(to)
        return (whole + if (rest) 1 else 0).toInt().coerceAtLeast(1)
    }

    private fun ceilDiv(a: Long, b: Long): Long = if (b <= 0L) a else (a + b - 1) / b
}
