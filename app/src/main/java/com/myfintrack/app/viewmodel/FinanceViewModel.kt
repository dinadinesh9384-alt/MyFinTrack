package com.myfintrack.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myfintrack.app.data.CsvExporter
import com.myfintrack.app.data.FinanceRepository
import com.myfintrack.app.data.FinanceRuleException
import com.myfintrack.app.data.SettingsRepository
import com.myfintrack.app.domain.FinanceCalculator
import com.myfintrack.app.domain.PlanningCalculator
import com.myfintrack.app.model.Account
import com.myfintrack.app.model.AccountBalance
import com.myfintrack.app.model.AppSettings
import com.myfintrack.app.model.Budget
import com.myfintrack.app.model.BudgetProgress
import com.myfintrack.app.model.CategorySpending
import com.myfintrack.app.model.Goal
import com.myfintrack.app.model.GoalContribution
import com.myfintrack.app.model.GoalProgress
import com.myfintrack.app.model.Transaction
import com.myfintrack.app.model.TransactionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Everything the Home dashboard shows, computed from persisted transactions only. */
data class DashboardState(
    val loaded: Boolean = false,
    /**
     * Total balance: all account opening balances + all income − all expenses.
     * Transfers cancel out and are never counted twice.
     */
    val balancePaise: Long = 0L,
    /** Number of active (non-archived) accounts. 0 means the balance is income − expenses only. */
    val activeAccountCount: Int = 0,
    val month: YearMonth = YearMonth.now(),
    val monthTotals: FinanceCalculator.Totals = FinanceCalculator.Totals.ZERO,
    /** Expense change vs the same period last month, null when not comparable. */
    val expenseChangePercent: Int? = null,
    val recent: List<Transaction> = emptyList(),
    val monthCategories: List<CategorySpending> = emptyList(),
    val lastSixMonths: List<FinanceCalculator.MonthTotals> = emptyList(),
    val transactionCount: Int = 0,
    /** True when some income/expense entries are not linked to any account. */
    val hasUnlinkedTransactions: Boolean = false
)

/** One-off message for the snackbar, optionally with an action such as Undo. */
data class UiMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FinanceRepository.get(application)
    private val settingsRepository = SettingsRepository.get(application)

    val transactions: StateFlow<List<Transaction>> = repository.transactions
    val loaded: StateFlow<Boolean> = repository.loaded
    val settings: StateFlow<AppSettings> = settingsRepository.settings

    val accounts: StateFlow<List<Account>> = repository.accounts

    /**
     * Today's date in the device zone. Re-emits when the date changes so month-based
     * figures (dashboard, budgets, goals) roll over at midnight without needing a data change.
     */
    private val currentDate: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now(ZoneId.systemDefault()))
            delay(60_000)
        }
    }.distinctUntilChanged()

    val dashboard: StateFlow<DashboardState> =
        combine(repository.transactions, repository.accounts, repository.loaded, currentDate) { txs, accs, isLoaded, _ ->
            buildDashboard(txs, accs, isLoaded)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    /** Current balance per account (archived included, the UI decides what to show). */
    val accountBalances: StateFlow<List<AccountBalance>> =
        combine(repository.accounts, repository.transactions) { accs, txs ->
            PlanningCalculator.accountBalances(accs, txs)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Income − expenses not linked to any account. */
    val unlinkedNetPaise: StateFlow<Long> =
        repository.transactions.map { PlanningCalculator.unlinkedNet(it) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** Budget progress for the current calendar month, most-used first. */
    val budgetProgress: StateFlow<List<BudgetProgress>> =
        combine(repository.budgets, repository.transactions, currentDate) { budgets, txs, today ->
            val zone = ZoneId.systemDefault()
            PlanningCalculator.budgetProgress(budgets, txs, YearMonth.from(today), today, zone)
                .sortedWith(compareBy<BudgetProgress> { it.budget.category != null }.thenByDescending { it.percentUsed })
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val goalProgress: StateFlow<List<GoalProgress>> =
        combine(repository.goals, repository.goalContributions, currentDate) { goals, contributions, today ->
            PlanningCalculator.goalProgress(goals, contributions, today)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            try {
                repository.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit(UiMessage("Could not load your data. Please restart the app."))
            }
        }
    }

    fun saveTransaction(tx: Transaction) {
        val isNew = tx.id == 0L
        viewModelScope.launch {
            try {
                repository.save(tx)
                _messages.emit(UiMessage(if (isNew) "Transaction added" else "Transaction updated"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: FinanceRuleException) {
                _messages.emit(UiMessage(e.message ?: "Could not save transaction"))
            } catch (e: Exception) {
                _messages.emit(UiMessage("Could not save transaction"))
            }
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            try {
                val deleted = repository.delete(tx.id) ?: return@launch
                _messages.emit(
                    UiMessage(
                        text = "Transaction deleted",
                        actionLabel = "Undo",
                        onAction = { restoreTransaction(deleted) }
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit(UiMessage("Could not delete transaction"))
            }
        }
    }

    private fun restoreTransaction(tx: Transaction) {
        viewModelScope.launch {
            try {
                repository.restore(tx)
                _messages.emit(UiMessage("Transaction restored"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: FinanceRuleException) {
                _messages.emit(UiMessage(e.message ?: "Could not restore transaction"))
            } catch (e: Exception) {
                _messages.emit(UiMessage("Could not restore transaction"))
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Accounts, budgets, goals. [onDone] receives null on success or a user-facing error, so an
    // open editor can stay open and show the problem instead of losing the user's input.
    // ---------------------------------------------------------------------------------------

    fun saveAccount(account: Account, onDone: (String?) -> Unit) = runEdit(
        success = if (account.id == 0L) "Account added" else "Account updated",
        failure = "Could not save account",
        onDone = onDone
    ) { repository.saveAccount(account) }

    fun deleteAccount(account: Account, onDone: (String?) -> Unit) = runEdit(
        success = "Account deleted",
        failure = "Could not delete account",
        onDone = onDone
    ) { repository.deleteAccount(account.id) }

    fun saveBudget(budget: Budget, onDone: (String?) -> Unit) = runEdit(
        success = if (budget.id == 0L) "Budget added" else "Budget updated",
        failure = "Could not save budget",
        onDone = onDone
    ) { repository.saveBudget(budget) }

    fun deleteBudget(budget: Budget, onDone: (String?) -> Unit) = runEdit(
        success = "Budget removed",
        failure = "Could not remove budget",
        onDone = onDone
    ) { repository.deleteBudget(budget.id) }

    fun saveGoal(goal: Goal, onDone: (String?) -> Unit) = runEdit(
        success = if (goal.id == 0L) "Goal created" else "Goal updated",
        failure = "Could not save goal",
        onDone = onDone
    ) { repository.saveGoal(goal) }

    fun deleteGoal(goal: Goal, onDone: (String?) -> Unit) = runEdit(
        success = "Goal deleted",
        failure = "Could not delete goal",
        onDone = onDone
    ) { repository.deleteGoal(goal.id) }

    /** [amountPaise] > 0 adds money to the goal, < 0 takes it back out. */
    fun contributeToGoal(goalId: Long, amountPaise: Long, onDone: (String?) -> Unit) = runEdit(
        success = if (amountPaise > 0) "Added to goal" else "Withdrawn from goal",
        failure = "Could not update goal",
        onDone = onDone
    ) {
        repository.addGoalContribution(
            GoalContribution(goalId = goalId, amountPaise = amountPaise, occurredAt = System.currentTimeMillis())
        )
    }

    private fun runEdit(
        success: String,
        failure: String,
        onDone: (String?) -> Unit,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
                onDone(null)
                _messages.emit(UiMessage(success))
            } catch (e: CancellationException) {
                throw e
            } catch (e: FinanceRuleException) {
                onDone(e.message ?: failure)
            } catch (e: Exception) {
                onDone(failure)
            }
        }
    }

    fun setHideBalances(hidden: Boolean) = settingsRepository.setHideBalances(hidden)

    fun setDisplayName(name: String) = settingsRepository.setDisplayName(name)

    fun setThemeMode(mode: com.myfintrack.app.model.ThemeMode) = settingsRepository.setThemeMode(mode)

    /** Writes all transactions to a document the user picked with the system file picker. */
    fun exportCsv(uri: Uri) {
        val snapshot = repository.transactions.value
        val accountNames = repository.accounts.value.associate { it.id to it.name }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stream = getApplication<Application>().contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException("Cannot open file")
                    stream.use { CsvExporter.write(it, snapshot, accountNames, ZoneId.systemDefault()) }
                }
                _messages.emit(UiMessage("Exported ${snapshot.size} transactions"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit(UiMessage("Export failed"))
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            try {
                repository.deleteAll()
                settingsRepository.clear()
                _messages.emit(UiMessage("All data deleted from this device"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit(UiMessage("Could not delete data"))
            }
        }
    }

    private fun buildDashboard(txs: List<Transaction>, accounts: List<Account>, isLoaded: Boolean): DashboardState {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val month = YearMonth.now(zone)
        val monthTxs = FinanceCalculator.forMonth(txs, month, zone)
        return DashboardState(
            loaded = isLoaded,
            balancePaise = PlanningCalculator.totalBalance(accounts, txs),
            activeAccountCount = accounts.count { !it.archived },
            month = month,
            monthTotals = FinanceCalculator.totals(monthTxs),
            expenseChangePercent = FinanceCalculator.expenseChangeVsLastMonth(txs, now, zone),
            recent = txs.take(5),
            monthCategories = FinanceCalculator.categoryBreakdown(monthTxs),
            lastSixMonths = FinanceCalculator.monthlyTotals(txs, month, 6, zone),
            transactionCount = txs.size,
            hasUnlinkedTransactions = txs.any { it.accountId == null && it.type != TransactionType.TRANSFER }
        )
    }
}
