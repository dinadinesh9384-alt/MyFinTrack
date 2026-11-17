package com.myfintrack.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfintrack.app.model.CategorySpending
import com.myfintrack.app.theme.*
import kotlin.math.atan2

@Composable
fun SpendingOverviewSection(
    categories: List<CategorySpending>,
    totalExpense: Double,
    onCategorySelected: (CategorySpending) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FinCard, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spending Overview",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Category breakdown for November",
                        color = FinTextSecondary,
                        fontSize = 10.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(FinAccentGlow, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "₹${totalExpense.toInt()} Total",
                        color = FinAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Donut Chart & Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive Donut Chart Canvas
                Box(
                    modifier = Modifier
                        .size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(categories) {
                                detectTapGestures { tapOffset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = tapOffset.x - center.x
                                    val dy = tapOffset.y - center.y
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f

                                    // Rotate angle by 90 degrees to align with startAngle -90f
                                    val adjustedAngle = (angle + 90f) % 360f

                                    var cumulativeAngle = 0f
                                    for (cat in categories) {
                                        val sweep = (cat.percentage / 100f) * 360f
                                        if (adjustedAngle >= cumulativeAngle && adjustedAngle < cumulativeAngle + sweep) {
                                            selectedCategory = cat
                                            onCategorySelected(cat)
                                            break
                                        }
                                        cumulativeAngle += sweep
                                    }
                                }
                            }
                    ) {
                        val strokeWidth = 28f
                        val radius = (size.minDimension - strokeWidth) / 2f
                        val topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)
                        val arcSize = Size(radius * 2, radius * 2)

                        // Draw background track
                        drawArc(
                            color = FinCardHover,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )

                        var startAngle = -90f
                        categories.forEach { cat ->
                            val sweepAngle = (cat.percentage / 100f) * 360f
                            val isSelected = selectedCategory?.category == cat.category

                            drawArc(
                                color = cat.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle - 2f, // slight gap
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(
                                    width = if (isSelected) strokeWidth + 8f else strokeWidth,
                                    cap = StrokeCap.Butt
                                )
                            )
                            startAngle += sweepAngle
                        }
                    }

                    // Center Label
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (selectedCategory != null) "Selected" else "Top Cat.",
                            color = FinTextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = selectedCategory?.category ?: "Food",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedCategory?.let { "${it.percentage.toInt()}%" } ?: "35%",
                            color = selectedCategory?.color ?: CategoryFood,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Legend List
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory?.category == cat.category
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) FinCardHover else Color.Transparent)
                                .clickable {
                                    selectedCategory = cat
                                    onCategorySelected(cat)
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(cat.color, CircleShape)
                                )
                                Text(
                                    text = cat.category,
                                    color = FinTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "₹${cat.amount.toInt()}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${cat.percentage.toInt()}%",
                                    color = FinTextSecondary,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
