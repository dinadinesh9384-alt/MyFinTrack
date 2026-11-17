package com.myfintrack.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.myfintrack.app.model.Transaction
import com.myfintrack.app.model.TransactionType
import com.myfintrack.app.theme.*

@Composable
fun AddTransactionDialog(
    initialType: TransactionType,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(if (initialType == TransactionType.INCOME) "Income" else "Food") }
    var type by remember { mutableStateOf(initialType) }
    var paymentMethod by remember { mutableStateOf("UPI") }

    val categories = if (type == TransactionType.INCOME) {
        listOf("Income", "Salary", "Bonus", "Freelance", "Investment")
    } else {
        listOf("Food", "Transport", "Shopping", "Friend", "Bills", "Entertainment", "Health")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = FinCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, FinBorder, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (type == TransactionType.INCOME) "Add Income" else "Add Expense",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Type Toggle Button (Income vs Expense)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FinBg, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (type == TransactionType.INCOME) FinAccent else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                type = TransactionType.INCOME
                                category = "Income"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Income",
                            color = if (type == TransactionType.INCOME) Color.Black else FinTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (type == TransactionType.EXPENSE) FinExpense else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                type = TransactionType.EXPENSE
                                category = "Food"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expense",
                            color = if (type == TransactionType.EXPENSE) Color.White else FinTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Description", color = FinTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (type == TransactionType.INCOME) FinAccent else FinExpense,
                        unfocusedBorderColor = FinBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)", color = FinTextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (type == TransactionType.INCOME) FinAccent else FinExpense,
                        unfocusedBorderColor = FinBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection Chips
                Text("Category", color = FinTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (type == TransactionType.INCOME) FinAccent.copy(alpha = 0.2f) else FinExpense.copy(alpha = 0.2f),
                                selectedLabelColor = if (type == TransactionType.INCOME) FinAccent else FinExpense,
                                containerColor = FinBg,
                                labelColor = FinTextSecondary
                            )
                        )
                    }
                }

                // Buttons (Cancel & Save)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = FinTextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amount > 0) {
                                onSave(
                                    Transaction(
                                        id = System.currentTimeMillis().toString(),
                                        title = title.trim(),
                                        category = category,
                                        date = "Today",
                                        amount = amount,
                                        type = type,
                                        paymentMethod = paymentMethod
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == TransactionType.INCOME) FinAccent else FinExpense,
                            contentColor = if (type == TransactionType.INCOME) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Transaction", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
