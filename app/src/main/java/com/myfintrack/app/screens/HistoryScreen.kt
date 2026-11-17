package com.myfintrack.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfintrack.app.model.Transaction
import com.myfintrack.app.model.TransactionType
import com.myfintrack.app.theme.*

@Composable
fun HistoryScreen(
    transactions: List<Transaction>,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredTransactions = transactions.filter { tx ->
        val matchesType = when (selectedFilter) {
            "Income" -> tx.type == TransactionType.INCOME
            "Expense" -> tx.type == TransactionType.EXPENSE
            else -> true
        }
        val matchesSearch = tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true)
        matchesType && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Transaction History",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredTransactions.size} transactions listed",
                    color = FinTextSecondary,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = onAddTransaction,
                colors = ButtonDefaults.buttonColors(containerColor = FinAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search transactions...", color = FinTextSecondary, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FinTextSecondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FinAccent,
                unfocusedBorderColor = FinBorder,
                focusedContainerColor = FinCard,
                unfocusedContainerColor = FinCard,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Type Filter Chips (All, Income, Expense)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("All", "Income", "Expense").forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSelected) FinAccent else FinCard, RoundedCornerShape(12.dp))
                        .border(1.dp, if (isSelected) FinAccent else FinBorder, RoundedCornerShape(12.dp))
                        .clickable { selectedFilter = filter }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) Color.Black else FinTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // List
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions found", color = FinTextSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTransactions) { tx ->
                    TransactionItemRow(transaction = tx)
                }
            }
        }
    }
}
