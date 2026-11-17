package com.myfintrack.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myfintrack.app.data.CsvExporter
import com.myfintrack.app.data.FinanceRepository
import com.myfintrack.app.data.SettingsRepository
import com.myfintrack.app.domain.FinanceCalculator
import com.myfintrack.app.model.AppSettings
import com.myfintrack.app.model.CategorySpending
import com.myfintrack.app.model.Transaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.time.ZoneId

/** Everything the Home dashboard shows, computed from persisted transactions only. */
data class DashboardState(
    val loaded: Boolean = false,
    /** All-time income minus expenses (transfers excluded). */
    val balancePaise: Long = 0L,
    val month: YearMonth = YearMonth.now(),
    val monthTotals: FinanceCalculator.Totals = FinanceCalculator.Totals.ZERO,
    /** Expense change vs the same period last month, null when not comparable. */
    val expenseChangePercent: Int? = null,
    val recent: List<Transaction> = emptyList(),
    val monthCategories: List<CategorySpending> = emptyList(),
    val lastSixMonths: List<FinanceCalculator.MonthTotals> = emptyList(),
    val transactionCount: Int = 0
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

    val dashboard: StateFlow<DashboardState> =
        combine(repository.transactions, repository.loaded) { txs, isLoaded ->
            buildDashboard(txs, isLoaded)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

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
            } catch (e: Exception) {
                _messages.emit(UiMessage("Could not restore transaction"))
            }
        }
    }

    fun setHideBalances(hidden: Boolean) = settingsRepository.setHideBalances(hidden)

    fun setDisplayName(name: String) = settingsRepository.setDisplayName(name)

    /** Writes all transactions to a document the user picked with the system file picker. */
    fun exportCsv(uri: Uri) {
        val snapshot = repository.transactions.value
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stream = getApplication<Application>().contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException("Cannot open file")
                    stream.use { CsvExporter.write(it, snapshot, ZoneId.systemDefault()) }
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

    private fun buildDashboard(txs: List<Transaction>, isLoaded: Boolean): DashboardState {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val month = YearMonth.now(zone)
        val monthTxs = FinanceCalculator.forMonth(txs, month, zone)
        return DashboardState(
            loaded = isLoaded,
            balancePaise = FinanceCalculator.totals(txs).netPaise,
            month = month,
            monthTotals = FinanceCalculator.totals(monthTxs),
            expenseChangePercent = FinanceCalculator.expenseChangeVsLastMonth(txs, now, zone),
            recent = txs.take(5),
            monthCategories = FinanceCalculator.categoryBreakdown(monthTxs),
            lastSixMonths = FinanceCalculator.monthlyTotals(txs, month, 6, zone),
            transactionCount = txs.size
        )
    }
}
