package com.myfintrack.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.myfintrack.app.data.FinanceDatabase.Companion.C_ACCOUNT_ID
import com.myfintrack.app.data.FinanceDatabase.Companion.C_AMOUNT
import com.myfintrack.app.data.FinanceDatabase.Companion.C_CATEGORY
import com.myfintrack.app.data.FinanceDatabase.Companion.C_CREATED_AT
import com.myfintrack.app.data.FinanceDatabase.Companion.C_ID
import com.myfintrack.app.data.FinanceDatabase.Companion.C_NOTE
import com.myfintrack.app.data.FinanceDatabase.Companion.C_OCCURRED_AT
import com.myfintrack.app.data.FinanceDatabase.Companion.C_PAYMENT_METHOD
import com.myfintrack.app.data.FinanceDatabase.Companion.C_TITLE
import com.myfintrack.app.data.FinanceDatabase.Companion.C_TO_ACCOUNT_ID
import com.myfintrack.app.data.FinanceDatabase.Companion.C_TYPE
import com.myfintrack.app.data.FinanceDatabase.Companion.C_UPDATED_AT
import com.myfintrack.app.data.FinanceDatabase.Companion.T_ACCOUNTS
import com.myfintrack.app.data.FinanceDatabase.Companion.T_TRANSACTIONS
import com.myfintrack.app.domain.Money
import com.myfintrack.app.model.Category
import com.myfintrack.app.model.PaymentMethod
import com.myfintrack.app.model.Transaction
import com.myfintrack.app.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Single source of truth for transactions. All disk work runs on [Dispatchers.IO].
 * After every write the in-memory [transactions] flow is reloaded from the database,
 * so the UI always shows what is actually persisted.
 */
class FinanceRepository private constructor(context: Context) {

    private val helper = FinanceDatabase(context.applicationContext)

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    /** All transactions, newest first. */
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _transactions.value = queryAll()
        _loaded.value = true
    }

    /** Inserts (id == 0) or updates a transaction. Returns its id. */
    suspend fun save(tx: Transaction): Long = withContext(Dispatchers.IO) {
        validate(tx)
        val now = System.currentTimeMillis()
        val values = toContentValues(tx).apply { put(C_UPDATED_AT, now) }
        val db = helper.writableDatabase
        val id = if (tx.id == 0L) {
            values.put(C_CREATED_AT, now)
            db.insertOrThrow(T_TRANSACTIONS, null, values)
        } else {
            val rows = db.update(T_TRANSACTIONS, values, "$C_ID = ?", arrayOf(tx.id.toString()))
            check(rows == 1) { "Transaction ${tx.id} no longer exists" }
            tx.id
        }
        _transactions.value = queryAll()
        id
    }

    /** Deletes a transaction and returns the deleted row (for undo), or null if missing. */
    suspend fun delete(id: Long): Transaction? = withContext(Dispatchers.IO) {
        val existing = _transactions.value.firstOrNull { it.id == id }
        helper.writableDatabase.delete(T_TRANSACTIONS, "$C_ID = ?", arrayOf(id.toString()))
        _transactions.value = queryAll()
        existing
    }

    /** Re-inserts a previously deleted transaction with its original id and timestamps. */
    suspend fun restore(tx: Transaction) = withContext(Dispatchers.IO) {
        validate(tx)
        val values = toContentValues(tx).apply {
            put(C_ID, tx.id)
            put(C_CREATED_AT, tx.createdAt)
            put(C_UPDATED_AT, tx.updatedAt)
        }
        helper.writableDatabase.insertOrThrow(T_TRANSACTIONS, null, values)
        _transactions.value = queryAll()
    }

    /** Permanently removes every transaction and account on this device. */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_TRANSACTIONS, null, null)
            db.delete(T_ACCOUNTS, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _transactions.value = queryAll()
    }

    private fun validate(tx: Transaction) {
        require(tx.amountPaise in 1..Money.MAX_PAISE) { "Amount out of range" }
        require(tx.title.isNotBlank()) { "Title is required" }
        require(tx.category.type == tx.type) { "Category does not match transaction type" }
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
        val toAccount = tx.toAccountId
        if (toAccount == null) putNull(C_TO_ACCOUNT_ID) else put(C_TO_ACCOUNT_ID, toAccount)
    }

    private fun queryAll(): List<Transaction> {
        val result = ArrayList<Transaction>()
        helper.readableDatabase.query(
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
