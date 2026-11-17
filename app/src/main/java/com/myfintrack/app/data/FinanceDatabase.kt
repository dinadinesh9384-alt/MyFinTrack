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
        // Version 1 schema
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

        // Version 2 additions
        createVersion2Tables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Explicit, additive migrations only. Existing user data is never dropped.
        if (oldVersion < 2) {
            createVersion2Tables(db)
        }
    }

    /** v2: budgets, savings goals and goal contributions, plus a to-account index for transfers. */
    private fun createVersion2Tables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $T_BUDGETS (
                $C_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $C_BUDGET_SCOPE TEXT NOT NULL UNIQUE,
                $C_LIMIT INTEGER NOT NULL CHECK ($C_LIMIT > 0),
                $C_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $T_GOALS (
                $C_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $C_NAME TEXT NOT NULL,
                $C_TARGET INTEGER NOT NULL CHECK ($C_TARGET > 0),
                $C_TARGET_DAY INTEGER,
                $C_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $T_GOAL_CONTRIBUTIONS (
                $C_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $C_GOAL_ID INTEGER NOT NULL REFERENCES $T_GOALS($C_ID) ON DELETE CASCADE,
                $C_AMOUNT INTEGER NOT NULL CHECK ($C_AMOUNT <> 0),
                $C_OCCURRED_AT INTEGER NOT NULL,
                $C_NOTE TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_goal_contrib_goal ON $T_GOAL_CONTRIBUTIONS($C_GOAL_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tx_to_account ON $T_TRANSACTIONS($C_TO_ACCOUNT_ID)")
    }

    companion object {
        const val DB_NAME = "myfintrack.db"
        const val DB_VERSION = 2

        const val T_ACCOUNTS = "accounts"
        const val T_TRANSACTIONS = "transactions"
        const val T_BUDGETS = "budgets"
        const val T_GOALS = "goals"
        const val T_GOAL_CONTRIBUTIONS = "goal_contributions"

        /** Budget scope: an expense [com.myfintrack.app.model.Category] name, or [SCOPE_OVERALL]. */
        const val C_BUDGET_SCOPE = "scope"
        const val SCOPE_OVERALL = "OVERALL"
        const val C_LIMIT = "limit_paise"
        const val C_TARGET = "target_paise"
        const val C_TARGET_DAY = "target_epoch_day"
        const val C_GOAL_ID = "goal_id"

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
