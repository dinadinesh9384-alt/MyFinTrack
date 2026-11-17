package com.myfintrack.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local, on-device SQLite database. Uses the Android framework SQLite API directly
 * (no annotation processor), so no extra build plugin is required.
 *
 * Schema rules:
 *  - Amounts are INTEGER paise.
 *  - Times are INTEGER epoch milliseconds.
 *  - Every future schema change must bump [DB_VERSION] and add an explicit, non-destructive
 *    migration in [onUpgrade]. Never drop user tables.
 */
internal class FinanceDatabase(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Accounts table is created now so the transactions table can reference it.
        // Account management UI is delivered in Step 3.
        db.execSQL(
            """
            CREATE TABLE $T_ACCOUNTS (
                $C_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $C_NAME TEXT NOT NULL,
                $C_ACCOUNT_TYPE TEXT NOT NULL,
                $C_OPENING_BALANCE INTEGER NOT NULL DEFAULT 0,
                $C_ARCHIVED INTEGER NOT NULL DEFAULT 0,
                $C_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $T_TRANSACTIONS (
                $C_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $C_TYPE TEXT NOT NULL,
                $C_AMOUNT INTEGER NOT NULL CHECK ($C_AMOUNT > 0),
                $C_TITLE TEXT NOT NULL,
                $C_CATEGORY TEXT NOT NULL,
                $C_PAYMENT_METHOD TEXT NOT NULL,
                $C_NOTE TEXT NOT NULL DEFAULT '',
                $C_OCCURRED_AT INTEGER NOT NULL,
                $C_ACCOUNT_ID INTEGER REFERENCES $T_ACCOUNTS($C_ID) ON DELETE SET NULL,
                $C_TO_ACCOUNT_ID INTEGER REFERENCES $T_ACCOUNTS($C_ID) ON DELETE SET NULL,
                $C_CREATED_AT INTEGER NOT NULL,
                $C_UPDATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_tx_occurred_at ON $T_TRANSACTIONS($C_OCCURRED_AT)")
        db.execSQL("CREATE INDEX idx_tx_account ON $T_TRANSACTIONS($C_ACCOUNT_ID)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 is the first schema. Add explicit step-by-step migrations here, e.g.
        // if (oldVersion < 2) { db.execSQL("CREATE TABLE budgets (...)") }
    }

    companion object {
        const val DB_NAME = "myfintrack.db"
        const val DB_VERSION = 1

        const val T_ACCOUNTS = "accounts"
        const val T_TRANSACTIONS = "transactions"

        const val C_ID = "id"
        const val C_NAME = "name"
        const val C_ACCOUNT_TYPE = "account_type"
        const val C_OPENING_BALANCE = "opening_balance_paise"
        const val C_ARCHIVED = "archived"

        const val C_TYPE = "type"
        const val C_AMOUNT = "amount_paise"
        const val C_TITLE = "title"
        const val C_CATEGORY = "category"
        const val C_PAYMENT_METHOD = "payment_method"
        const val C_NOTE = "note"
        const val C_OCCURRED_AT = "occurred_at"
        const val C_ACCOUNT_ID = "account_id"
        const val C_TO_ACCOUNT_ID = "to_account_id"
        const val C_CREATED_AT = "created_at"
        const val C_UPDATED_AT = "updated_at"
    }
}
