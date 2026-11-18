package com.myfintrack.app.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfintrack.app.domain.Money
import com.myfintrack.app.model.CategorySpending
import com.myfintrack.app.theme.*
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Category breakdown card: donut + legend with share bars.
 * All values come from [categories], computed from real transactions.
 */
@Composable
fun SpendingOverviewSection(
    title: String,
    subtitle: String,
    categories: List<CategorySpending>,
    totalPaise: Long,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
    maxLegendItems: Int = 6
) {
    FinSurfaceCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = FinTextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = FinTextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            if (categories.isNotEmpty()) {
                InfoPill(
                    text = if (hideAmounts) Money.MASK else Money.format(totalPaise, withDecimals = false),
                    color = FinExpense
                )
            }
        }

        if (categories.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.PieChart,
                title = "No spending yet",
                message = "Expenses you add will be broken down by category here."
            )
        } else {
            DonutWithLegend(categories, totalPaise, hideAmounts, maxLegendItems)
        }
    }
}

@Composable
private fun DonutWithLegend(
    categories: List<CategorySpending>,
    totalPaise: Long,
    hideAmounts: Boolean,
    maxLegendItems: Int
) {
    var selectedIndex by remember(categories) { mutableStateOf(-1) }
    val sweep = remember(categories) { Animatable(0f) }
    LaunchedEffect(categories) { sweep.animateTo(1f, tween(durationMillis = 700)) }

    Column {
        Spacer(Modifier.height(16.dp))

        val selected = categories.getOrNull(selectedIndex)
        val chartDescription = categories.joinToString(prefix = "Spending by category: ") {
            "${it.category.label} ${(it.fraction * 100).roundToInt()} percent"
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .semantics { contentDescription = chartDescription },
                contentAlignment = Alignment.Center
            ) {
                val trackColor = FinCardHover
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(categories) {
                            detectTapGestures { tap ->
                                val dx = tap.x - size.width / 2f
                                val dy = tap.y - size.height / 2f
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0f) angle += 360f
                                val adjusted = (angle + 90f) % 360f
                                var cumulative = 0f
                                categories.forEachIndexed { index, cat ->
                                    val arc = cat.fraction * 360f
                                    if (adjusted >= cumulative && adjusted < cumulative + arc) {
                                        selectedIndex = if (selectedIndex == index) -1 else index
                                        return@detectTapGestures
                                    }
                                    cumulative += arc
                                }
                            }
                        }
                ) {
                    val strokeWidth = 26.dp.toPx()
                    val radius = (size.minDimension - strokeWidth - 8.dp.toPx()) / 2f
                    val topLeft = Offset(size.width / 2f - radius, size.height / 2f - radius)
                    val arcSize = Size(radius * 2, radius * 2)
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                    val gap = if (categories.size > 1) 1.5f else 0f
                    var start = -90f
                    categories.forEachIndexed { index, cat ->
                        val arc = cat.fraction * 360f * sweep.value
                        val isSelected = index == selectedIndex
                        val dimmed = selectedIndex >= 0 && !isSelected
                        drawArc(
                            color = if (dimmed) cat.category.color.copy(alpha = 0.35f) else cat.category.color,
                            startAngle = start,
                            sweepAngle = (arc - gap).coerceAtLeast(0.5f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth,
                                cap = StrokeCap.Butt
                            )
                        )
                        start += arc
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 30.dp)
                ) {
                    Text(
                        text = selected?.category?.label ?: "Total",
                        color = FinTextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            hideAmounts -> "••••"
                            selected != null -> Money.formatCompact(selected.amountPaise)
                            else -> Money.formatCompact(totalPaise)
                        },
                        color = FinTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (selected != null) {
                        Text(
                            text = "${(selected.fraction * 100).roundToInt()}%",
                            color = selected.category.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val shown = categories.take(maxLegendItems)
                shown.forEachIndexed { index, cat ->
                    LegendRow(
                        item = cat,
                        selected = index == selectedIndex,
                        hideAmounts = hideAmounts,
                        onClick = { selectedIndex = if (selectedIndex == index) -1 else index }
                    )
                }
                if (categories.size > shown.size) {
                    Text(
                        text = "+${categories.size - shown.size} more",
                        color = FinTextMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendRow(
    item: CategorySpending,
    selected: Boolean,
    hideAmounts: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) FinCardHover else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(8.dp).background(item.category.color, CircleShape))
            Text(
                text = item.category.label,
                color = FinTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (hideAmounts) "${(item.fraction * 100).roundToInt()}%" else Money.formatCompact(item.amountPaise),
                color = FinTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { item.fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = item.category.color,
            trackColor = FinCardHover
        )
    }
}
