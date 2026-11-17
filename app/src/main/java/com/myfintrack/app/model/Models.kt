package com.myfintrack.app.model

import androidx.compose.ui.graphics.Color
import com.myfintrack.app.theme.*

enum class TransactionType {
    INCOME, EXPENSE
}

enum class NavTab {
    HOME, HISTORY, ANALYSIS, SETTINGS
}

data class Transaction(
    val id: String,
    val title: String,
    val category: String,
    val date: String,
    val amount: Double,
    val type: TransactionType,
    val paymentMethod: String
)

data class CategorySpending(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

object SampleData {
    val initialTransactions = listOf(
        Transaction("1", "Salary", "Income", "Today, 09:30 AM", 12000.0, TransactionType.INCOME, "Bank Transfer"),
        Transaction("2", "Tea Shop", "Food", "Today, 04:15 PM", 20.0, TransactionType.EXPENSE, "UPI"),
        Transaction("3", "Bus Fare", "Transport", "Yesterday", 15.0, TransactionType.EXPENSE, "Cash"),
        Transaction("4", "Vishvaa", "Friend", "10 Nov", 350.0, TransactionType.EXPENSE, "UPI"),
        Transaction("5", "Grocery Store", "Shopping", "08 Nov", 850.0, TransactionType.EXPENSE, "Card")
    )

    val categoryBreakdown = listOf(
        CategorySpending("Food", 2975.0, 35f, CategoryFood),
        CategorySpending("Transport", 1275.0, 15f, CategoryTransport),
        CategorySpending("Shopping", 2125.0, 25f, CategoryShopping),
        CategorySpending("Friends", 1275.0, 15f, CategoryFriends),
        CategorySpending("Others", 850.0, 10f, CategoryOthers)
    )
}
