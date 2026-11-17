package com.myfintrack.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.myfintrack.app.data.FinanceDatabase.Companion.C_ACCOUNT_ID
import com.myfintrack.app.data.FinanceDatabase.Companion.C_ACCOUNT_TYPE
import com.myfintrack.app.data.FinanceDatabase.Companion.C_AMOUNT
import com.myfintrack.app.data.FinanceDatabase.Companion.C_ARCHIVED
import com.myfintrack.app.data.FinanceDatabase.Companion.C_BUDGET_SCOPE
import com.myfintrack.app.data.FinanceDatabase.Companion.C_CATEGORY
import com.myfintrack.app.data.FinanceDatabase.Companion.C_CREATED_AT
import com.myfintrack.app.data.FinanceDatabase.Companion.C_GOAL_ID
import com.myfintrack.app.data.FinanceDatabase.Companion.C_ID
import com.myfintrack.app.data.FinanceDatabase.Companion.C_LIMIT
import com.myfintrack.app.data.FinanceDatabase.Companion.C_NAME
import com.myfintrack.app.data.FinanceDatabase.Companion.C_NOTE
import com.myfintrack.app.data.FinanceDatabase.Companion.C_OCCURRED_AT
import com.myfintrack.app.data.FinanceDatabase.Companion.C_OPENING_BALANCE
import com.myfintrack.app.data.FinanceDatabase.Companion.C_PAYMENT_METHOD
import com.myfintrack.app.data.FinanceDatabase.Companion.C_TARGET
import com.myfintrack.app.data.FinanceDatabase.Companion.C_TARGET_DAY
import com.myfintrack.app.data.FinanceDatabase.Companion.C_TITLE
import com.myfintrack.app.data.FinanceDatabase.Companion.C_TO_ACCOUNT_ID
import com.myfintrack.app.data.FinanceDatabase.Companion.C_TYPE
import com.myfintrack.app.data.FinanceDatabase.Companion.C_UPDATED_AT
import com.myfintrack.app.data.FinanceDatabase.Companion.SCOPE_OVERALL
import com.myfintrack.app.data.FinanceDatabase.Companion.T_ACCOUNTS
import com.myfintrack.app.data.FinanceDatabase.Companion.T_BUDGETS
import com.myfintrack.app.data.FinanceDatabase.Companion.T_GOALS
import com.myfintrack.app.data.FinanceDatabase.Companion.T_GOAL_CONTRIBUTIONS
import com.myfintrack.app.data.FinanceDatabase.Companion.T_TRANSACTIONS
import com.myfintrack.app.domain.Money
import com.myfintrack.app.model.Account
import com.myfintrack.app.model.AccountType
import com.myfintrack.app.model.Budget
import com.myfintrack.app.model.Category
import com.myfintrack.app.model.Goal
import com.myfintrack.app.model.GoalContribution
import com.myfintrack.app.model.PaymentMethod
import com.myfintrack.app.model.Transaction
import com.myfintrack.app.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Thrown when a user action is not allowed; [message] is safe to show to the user. */
class FinanceRuleException(message: String) : IllegalArgumentException(message)

/**
 * Single source of truth for all finance data. All disk work runs on [Dispatchers.IO].
 * After every write the affected in-memory flow is reloaded from the database, so the UI
 * always shows what is actually persisted.
 */
class FinanceRepository private constructor(context: Context) {

    private val helper = FinanceDatabase(context.applicationContext)

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    /** All transactions, newest first. */
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    /** All accounts including archived ones, in creation order. */
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    private val _goalContributions = MutableStateFlow<List<GoalContribution>>(emptyList())
    val goalContributions: StateFlow<List<GoalContribution>> = _goalContributions.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        reloadAll()
        _loaded.value = true
    }

    // -----------------------------------------------------------------------------------------
    // Transactions
    // -----------------------------------------------------------------------------------------

    /** Inserts (id == 0) or updates a transaction. Returns its id. */
    suspend fun save(tx: Transaction): Long = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        validate(db, tx)
        val now = System.currentTimeMillis()
        val values = toContentValues(tx).apply { put(C_UPDATED_AT, now) }
        val id = if (tx.id == 0L) {
            values.put(C_CREATED_AT, now)
            db.insertOrThrow(T_TRANSACTIONS, null, values)
        } else {
            val rows = db.update(T_TRANSACTIONS, values, "$C_ID = ?", arrayOf(tx.id.toString()))
            check(rows == 1) { "Transaction ${tx.id} no longer exists" }
            tx.id
        }
        _transactions.value = queryTransactions(db)
        id
    }

    /** Deletes a transaction and returns the deleted row (for undo), or null if missing. */
    suspend fun delete(id: Long): Transaction? = withContext(Dispatchers.IO) {
        val existing = _transactions.value.firstOrNull { it.id == id }
        val db = helper.writableDatabase
        db.delete(T_TRANSACTIONS, "$C_ID = ?", arrayOf(id.toString()))
        _transactions.value = queryTransactions(db)
        existing
    }

    /**
     * Re-inserts a previously deleted transaction with its original id and timestamps.
     * If a linked account was deleted in the meantime, an income/expense keeps its data
     * without the account link; a transfer cannot be restored without both accounts.
     */
    suspend fun restore(tx: Transaction) = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        val fixed = if (tx.type != TransactionType.TRANSFER &&
            tx.accountId != null && !accountExists(db, tx.accountId)
        ) tx.copy(accountId = null) else tx
        validate(db, fixed)
        val values = toContentValues(fixed).apply {
            put(C_ID, fixed.id)
            put(C_CREATED_AT, fixed.createdAt)
            put(C_UPDATED_AT, fixed.updatedAt)
        }
        db.insertOrThrow(T_TRANSACTIONS, null, values)
        _transactions.value = queryTransactions(db)
    }

    private fun validate(db: SQLiteDatabase, tx: Transaction) {
        require(tx.amountPaise in 1..Money.MAX_PAISE) { "Amount out of range" }
        require(tx.title.isNotBlank()) { "Title is required" }
        require(tx.category.type == tx.type) { "Category does not match transaction type" }
        when (tx.type) {
            TransactionType.TRANSFER -> {
                val from = tx.accountId ?: throw FinanceRuleException("Choose the account the money leaves")
                val to = tx.toAccountId ?: throw FinanceRuleException("Choose the account the money goes to")
                if (from == to) throw FinanceRuleException("From and to accounts must be different")
                if (!accountExists(db, from) || !accountExists(db, to)) {
                    throw FinanceRuleException("One of the transfer accounts no longer exists")
                }
            }
            TransactionType.INCOME, TransactionType.EXPENSE -> {
                if (tx.toAccountId != null) throw FinanceRuleException("Only transfers have a destination account")
                val account = tx.accountId
                if (account != null && !accountExists(db, account)) {
                    throw FinanceRuleException("The selected account no longer exists")
                }
            }
        }
    }

    private fun toContentValues(tx: Transaction) = ContentValues().apply {
        put(C_TYPE, tx.type.name)
        put(C_AMOUNT, tx.amountPaise)
        put(C_TITLE, tx.title.trim())
        put(C_CATEGORY, tx.category.name)
        put(C_PAYMENT_METHOD, tx.paymentMethod.name)
        put(C_NOTE, tx.note.trim())
        put(C_OCCURRED_AT, tx.occurredAt)
        val account = tx.accountId
        if (account == null) putNull(C_ACCOUNT_ID) else put(C_ACCOUNT_ID, account)
        val toAccount = if (tx.type == TransactionType.TRANSFER) tx.toAccountId else null
        if (toAccount == null) putNull(C_TO_ACCOUNT_ID) else put(C_TO_ACCOUNT_ID, toAccount)
    }

    // -----------------------------------------------------------------------------------------
    // Accounts
    // -----------------------------------------------------------------------------------------

    suspend fun saveAccount(account: Account): Long = withContext(Dispatchers.IO) {
        val name = account.name.trim()
        if (name.isEmpty()) throw FinanceRuleException("Enter an account name")
        if (name.length > 40) throw FinanceRuleException("Account name is too long")
        if (account.openingBalancePaise !in -Money.MAX_PAISE..Money.MAX_PAISE) {
            throw FinanceRuleException("Balance is too large")
        }
        val db = helper.writableDatabase
        val duplicate = _accounts.value.any { it.id != account.id && it.name.equals(name, ignoreCase = true) }
        if (duplicate) throw FinanceRuleException("An account named \"$name\" already exists")
        val values = ContentValues().apply {
            put(C_NAME, name)
            put(C_ACCOUNT_TYPE, account.type.name)
            put(C_OPENING_BALANCE, account.openingBalancePaise)
            put(C_ARCHIVED, if (account.archived) 1 else 0)
        }
        val id = if (account.id == 0L) {
            values.put(C_CREATED_AT, System.currentTimeMillis())
            db.insertOrThrow(T_ACCOUNTS, null, values)
        } else {
            val rows = db.update(T_ACCOUNTS, values, "$C_ID = ?", arrayOf(account.id.toString()))
            check(rows == 1) { "Account ${account.id} no longer exists" }
            account.id
        }
        _accounts.value = queryAccounts(db)
        id
    }

    /**
     * Deletes an account that has no transactions. Accounts with history must be archived
     * instead, so past balances, transfers and reports stay correct.
     */
    suspend fun deleteAccount(id: Long) = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        val used = count(
            db, T_TRANSACTIONS,
            "$C_ACCOUNT_ID = ? OR $C_TO_ACCOUNT_ID = ?", arrayOf(id.toString(), id.toString())
        )
        if (used > 0) {
            throw FinanceRuleException("This account has $used transaction(s). Archive it instead of deleting.")
        }
        db.delete(T_ACCOUNTS, "$C_ID = ?", arrayOf(id.toString()))
        _accounts.value = queryAccounts(db)
    }

    private fun accountExists(db: SQLiteDatabase, id: Long): Boolean =
        count(db, T_ACCOUNTS, "$C_ID = ?", arrayOf(id.toString())) > 0

    // -----------------------------------------------------------------------------------------
    // Budgets
    // -----------------------------------------------------------------------------------------

    suspend fun saveBudget(budget: Budget): Long = withContext(Dispatchers.IO) {
        if (budget.limitPaise !in 1..Money.MAX_PAISE) throw FinanceRuleException("Enter a valid monthly limit")
        val category = budget.category
        if (category != null && category.type != TransactionType.EXPENSE) {
            throw FinanceRuleException("Budgets can only be set for expense categories")
        }
        val scope = category?.name ?: SCOPE_OVERALL
        val db = helper.writableDatabase
        val clash = _budgets.value.any { it.id != budget.id && (it.category?.name ?: SCOPE_OVERALL) == scope }
        if (clash) {
            throw FinanceRuleException(
                if (category == null) "An overall budget already exists" else "A budget for ${category.label} already exists"
            )
        }
        val values = ContentValues().apply {
            put(C_BUDGET_SCOPE, scope)
            put(C_LIMIT, budget.limitPaise)
        }
        val id = if (budget.id == 0L) {
            values.put(C_CREATED_AT, System.currentTimeMillis())
            db.insertOrThrow(T_BUDGETS, null, values)
        } else {
            val rows = db.update(T_BUDGETS, values, "$C_ID = ?", arrayOf(budget.id.toString()))
            check(rows == 1) { "Budget ${budget.id} no longer exists" }
            budget.id
        }
        _budgets.value = queryBudgets(db)
        id
    }

    suspend fun deleteBudget(id: Long) = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        db.delete(T_BUDGETS, "$C_ID = ?", arrayOf(id.toString()))
        _budgets.value = queryBudgets(db)
    }

    // -----------------------------------------------------------------------------------------
    // Goals
    // -----------------------------------------------------------------------------------------

    suspend fun saveGoal(goal: Goal): Long = withContext(Dispatchers.IO) {
        val name = goal.name.trim()
        if (name.isEmpty()) throw FinanceRuleException("Enter a goal name")
        if (name.length > 40) throw FinanceRuleException("Goal name is too long")
        if (goal.targetPaise !in 1..Money.MAX_PAISE) throw FinanceRuleException("Enter a valid target amount")
        val db = helper.writableDatabase
        val values = ContentValues().apply {
            put(C_NAME, name)
            put(C_TARGET, goal.targetPaise)
            val day = goal.targetEpochDay
            if (day == null) putNull(C_TARGET_DAY) else put(C_TARGET_DAY, day)
        }
        val id = if (goal.id == 0L) {
            values.put(C_CREATED_AT, System.currentTimeMillis())
            db.insertOrThrow(T_GOALS, null, values)
        } else {
            val rows = db.update(T_GOALS, values, "$C_ID = ?", arrayOf(goal.id.toString()))
            check(rows == 1) { "Goal ${goal.id} no longer exists" }
            goal.id
        }
        _goals.value = queryGoals(db)
        id
    }

    /** Deletes a goal and (via ON DELETE CASCADE) its contribution history. */
    suspend fun deleteGoal(id: Long) = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        db.delete(T_GOALS, "$C_ID = ?", arrayOf(id.toString()))
        _goals.value = queryGoals(db)
        _goalContributions.value = queryGoalContributions(db)
    }

    /**
     * Records money set aside for (positive) or taken back from (negative) a goal.
     * A withdrawal can never exceed what is currently saved.
     */
    suspend fun addGoalContribution(contribution: GoalContribution) = withContext(Dispatchers.IO) {
        val amount = contribution.amountPaise
        if (amount == 0L || amount !in -Money.MAX_PAISE..Money.MAX_PAISE) {
            throw FinanceRuleException("Enter a valid amount")
        }
        val db = helper.writableDatabase
        if (count(db, T_GOALS, "$C_ID = ?", arrayOf(contribution.goalId.toString())) == 0) {
            throw FinanceRuleException("This goal no longer exists")
        }
        if (amount < 0) {
            val saved = _goalContributions.value.filter { it.goalId == contribution.goalId }.sumOf { it.amountPaise }
            if (-amount > saved) throw FinanceRuleException("You can withdraw at most ${Money.format(saved)}")
        }
        val values = ContentValues().apply {
            put(C_GOAL_ID, contribution.goalId)
            put(C_AMOUNT, amount)
            put(C_OCCURRED_AT, contribution.occurredAt)
            put(C_NOTE, contribution.note.trim())
        }
        db.insertOrThrow(T_GOAL_CONTRIBUTIONS, null, values)
        _goalContributions.value = queryGoalContributions(db)
    }

    // -----------------------------------------------------------------------------------------
    // Everything
    // -----------------------------------------------------------------------------------------

    /** Permanently removes all finance data on this device. */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_GOAL_CONTRIBUTIONS, null, null)
            db.delete(T_GOALS, null, null)
            db.delete(T_BUDGETS, null, null)
            db.delete(T_TRANSACTIONS, null, null)
            db.delete(T_ACCOUNTS, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        reloadAll()
    }

    private fun reloadAll() {
        val db = helper.readableDatabase
        _accounts.value = queryAccounts(db)
        _transactions.value = queryTransactions(db)
        _budgets.value = queryBudgets(db)
        _goals.value = queryGoals(db)
        _goalContributions.value = queryGoalContributions(db)
    }

    // -----------------------------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------------------------

    private fun count(db: SQLiteDatabase, table: String, where: String, args: Array<String>): Int =
        db.rawQuery("SELECT COUNT(*) FROM $table WHERE $where", args).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }

    private fun queryTransactions(db: SQLiteDatabase): List<Transaction> {
        val result = ArrayList<Transaction>()
        db.query(
            T_TRANSACTIONS, null, null, null, null, null,
            "$C_OCCURRED_AT DESC, $C_ID DESC"
        ).use { c ->
            val iId = c.getColumnIndexOrThrow(C_ID)
            val iType = c.getColumnIndexOrThrow(C_TYPE)
            val iAmount = c.getColumnIndexOrThrow(C_AMOUNT)
            val iTitle = c.getColumnIndexOrThrow(C_TITLE)
            val iCategory = c.getColumnIndexOrThrow(C_CATEGORY)
            val iMethod = c.getColumnIndexOrThrow(C_PAYMENT_METHOD)
            val iNote = c.getColumnIndexOrThrow(C_NOTE)
            val iOccurred = c.getColumnIndexOrThrow(C_OCCURRED_AT)
            val iAccount = c.getColumnIndexOrThrow(C_ACCOUNT_ID)
            val iToAccount = c.getColumnIndexOrThrow(C_TO_ACCOUNT_ID)
            val iCreated = c.getColumnIndexOrThrow(C_CREATED_AT)
            val iUpdated = c.getColumnIndexOrThrow(C_UPDATED_AT)
            while (c.moveToNext()) {
                val type = TransactionType.fromKey(c.getString(iType))
                result.add(
                    Transaction(
                        id = c.getLong(iId),
                        type = type,
                        amountPaise = c.getLong(iAmount),
                        title = c.getString(iTitle),
                        category = Category.fromKey(c.getString(iCategory), type),
                        paymentMethod = PaymentMethod.fromKey(c.getString(iMethod)),
                        note = c.getString(iNote) ?: "",
                        occurredAt = c.getLong(iOccurred),
                        accountId = c.longOrNull(iAccount),
                        toAccountId = c.longOrNull(iToAccount),
                        createdAt = c.getLong(iCreated),
                        updatedAt = c.getLong(iUpdated)
                    )
                )
            }
        }
        return result
    }

    private fun queryAccounts(db: SQLiteDatabase): List<Account> {
        val result = ArrayList<Account>()
        db.query(T_ACCOUNTS, null, null, null, null, null, "$C_ID ASC").use { c ->
            val iId = c.getColumnIndexOrThrow(C_ID)
            val iName = c.getColumnIndexOrThrow(C_NAME)
            val iType = c.getColumnIndexOrThrow(C_ACCOUNT_TYPE)
            val iOpening = c.getColumnIndexOrThrow(C_OPENING_BALANCE)
            val iArchived = c.getColumnIndexOrThrow(C_ARCHIVED)
            val iCreated = c.getColumnIndexOrThrow(C_CREATED_AT)
            while (c.moveToNext()) {
                result.add(
                    Account(
                        id = c.getLong(iId),
                        name = c.getString(iName),
                        type = AccountType.fromKey(c.getString(iType)),
                        openingBalancePaise = c.getLong(iOpening),
                        archived = c.getInt(iArchived) != 0,
                        createdAt = c.getLong(iCreated)
                    )
                )
            }
        }
        return result
    }

    private fun queryBudgets(db: SQLiteDatabase): List<Budget> {
        val result = ArrayList<Budget>()
        db.query(T_BUDGETS, null, null, null, null, null, "$C_ID ASC").use { c ->
            val iId = c.getColumnIndexOrThrow(C_ID)
            val iScope = c.getColumnIndexOrThrow(C_BUDGET_SCOPE)
            val iLimit = c.getColumnIndexOrThrow(C_LIMIT)
            val iCreated = c.getColumnIndexOrThrow(C_CREATED_AT)
            while (c.moveToNext()) {
                val scope = c.getString(iScope)
                val category = if (scope == SCOPE_OVERALL) null
                else Category.entries.firstOrNull { it.name == scope && it.type == TransactionType.EXPENSE }
                // Skip rows whose category no longer exists rather than mislabelling them.
                if (scope != SCOPE_OVERALL && category == null) continue
                result.add(
                    Budget(
                        id = c.getLong(iId),
                        category = category,
                        limitPaise = c.getLong(iLimit),
                        createdAt = c.getLong(iCreated)
                    )
                )
            }
        }
        return result
    }

    private fun queryGoals(db: SQLiteDatabase): List<Goal> {
        val result = ArrayList<Goal>()
        db.query(T_GOALS, null, null, null, null, null, "$C_ID ASC").use { c ->
            val iId = c.getColumnIndexOrThrow(C_ID)
            val iName = c.getColumnIndexOrThrow(C_NAME)
            val iTarget = c.getColumnIndexOrThrow(C_TARGET)
            val iDay = c.getColumnIndexOrThrow(C_TARGET_DAY)
            val iCreated = c.getColumnIndexOrThrow(C_CREATED_AT)
            while (c.moveToNext()) {
                result.add(
                    Goal(
                        id = c.getLong(iId),
                        name = c.getString(iName),
                        targetPaise = c.getLong(iTarget),
                        targetEpochDay = c.longOrNull(iDay),
                        createdAt = c.getLong(iCreated)
                    )
                )
            }
        }
        return result
    }

    private fun queryGoalContributions(db: SQLiteDatabase): List<GoalContribution> {
        val result = ArrayList<GoalContribution>()
        db.query(
            T_GOAL_CONTRIBUTIONS, null, null, null, null, null,
            "$C_OCCURRED_AT DESC, $C_ID DESC"
        ).use { c ->
            val iId = c.getColumnIndexOrThrow(C_ID)
            val iGoal = c.getColumnIndexOrThrow(C_GOAL_ID)
            val iAmount = c.getColumnIndexOrThrow(C_AMOUNT)
            val iOccurred = c.getColumnIndexOrThrow(C_OCCURRED_AT)
            val iNote = c.getColumnIndexOrThrow(C_NOTE)
            while (c.moveToNext()) {
                result.add(
                    GoalContribution(
                        id = c.getLong(iId),
                        goalId = c.getLong(iGoal),
                        amountPaise = c.getLong(iAmount),
                        occurredAt = c.getLong(iOccurred),
                        note = c.getString(iNote) ?: ""
                    )
                )
            }
        }
        return result
    }

    private fun Cursor.longOrNull(index: Int): Long? = if (isNull(index)) null else getLong(index)

    companion object {
        @Volatile
        private var instance: FinanceRepository? = null

        fun get(context: Context): FinanceRepository =
            instance ?: synchronized(this) {
                instance ?: FinanceRepository(context.applicationContext).also { instance = it }
            }
    }
}
