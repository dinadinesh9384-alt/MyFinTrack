package com.myfintrack.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfintrack.app.components.SpendingOverviewSection
import com.myfintrack.app.model.CategorySpending
import com.myfintrack.app.theme.*

@Composable
fun AnalysisScreen(
    categories: List<CategorySpending>,
    totalExpense: Double,
    onCategorySelected: (CategorySpending) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Financial Analysis",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Detailed monthly category breakdown",
                    color = FinTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        item {
            SpendingOverviewSection(
                categories = categories,
                totalExpense = totalExpense,
                onCategorySelected = onCategorySelected
            )
        }

        item {
            // Budget Target Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FinCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FinBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "MONTHLY BUDGET TARGET",
                        color = FinTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("₹8,500 spent of ₹15,000", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("56.6%", color = FinAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    LinearProgressIndicator(
                        progress = { 0.566f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = FinAccent,
                        trackColor = FinCardHover
                    )

                    Text(
                        text = "You are well within your budget target for this month!",
                        color = FinTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
