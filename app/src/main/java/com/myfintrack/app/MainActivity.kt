package com.myfintrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfintrack.app.components.AddTransactionDialog
import com.myfintrack.app.components.ToastNotification
import com.myfintrack.app.model.*
import com.myfintrack.app.screens.AnalysisScreen
import com.myfintrack.app.screens.HistoryScreen
import com.myfintrack.app.screens.HomeScreen
import com.myfintrack.app.screens.SettingsScreen
import com.myfintrack.app.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyFinTrackTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent() {
    var currentTab by remember { mutableStateOf(NavTab.HOME) }
    var isBalanceMasked by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var dialogInitialType by remember { mutableStateOf(TransactionType.INCOME) }

    var transactions by remember { mutableStateOf(SampleData.initialTransactions) }
    val categories by remember { mutableStateOf(SampleData.categoryBreakdown) }

    // Auto dismiss Toast
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            toastMessage = null
        }
    }

    val showToast: (String) -> Unit = { msg ->
        toastMessage = msg
    }

    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    Scaffold(
        containerColor = FinBg,
        bottomBar = {
            BottomNavBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    currentTab = tab
                    if (tab != NavTab.HOME) {
                        showToast("Navigated to ${tab.name.lowercase().replaceFirstChar { it.uppercase() }} view")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavTab.HOME -> {
                    HomeScreen(
                        transactions = transactions,
                        categories = categories,
                        isBalanceMasked = isBalanceMasked,
                        onToggleBalanceMask = {
                            isBalanceMasked = !isBalanceMasked
                            showToast(if (isBalanceMasked) "Balance masked for privacy" else "Balance now visible")
                        },
                        onNotificationClick = {
                            showToast("Notifications: You have 1 unread update")
                        },
                        onQuickAction = { action ->
                            if (action == "Add Income") {
                                dialogInitialType = TransactionType.INCOME
                                showAddDialog = true
                            } else if (action == "Add Expense") {
                                dialogInitialType = TransactionType.EXPENSE
                                showAddDialog = true
                            } else if (action == "View History") {
                                currentTab = NavTab.HISTORY
                            }
                        },
                        onSeeAllHistory = {
                            currentTab = NavTab.HISTORY
                        },
                        onCategorySelected = { cat ->
                            showToast("Category ${cat.category}: ₹${cat.amount.toInt()} (${cat.percentage.toInt()}%)")
                        }
                    )
                }

                NavTab.HISTORY -> {
                    HistoryScreen(
                        transactions = transactions,
                        onAddTransaction = {
                            dialogInitialType = TransactionType.EXPENSE
                            showAddDialog = true
                        }
                    )
                }

                NavTab.ANALYSIS -> {
                    AnalysisScreen(
                        categories = categories,
                        totalExpense = totalExpense,
                        onCategorySelected = { cat ->
                            showToast("Category ${cat.category}: ₹${cat.amount.toInt()} (${cat.percentage.toInt()}%)")
                        }
                    )
                }

                NavTab.SETTINGS -> {
                    SettingsScreen(
                        isBalanceMasked = isBalanceMasked,
                        onToggleBalanceMask = {
                            isBalanceMasked = !isBalanceMasked
                            showToast(if (isBalanceMasked) "Balance masked for privacy" else "Balance now visible")
                        },
                        onActionToast = showToast
                    )
                }
            }

            // Top Toast Overlay
            ToastNotification(
                message = toastMessage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )
        }

        // Add Transaction Dialog Modal
        if (showAddDialog) {
            AddTransactionDialog(
                initialType = dialogInitialType,
                onDismiss = { showAddDialog = false },
                onSave = { newTx ->
                    transactions = listOf(newTx) + transactions
                    showAddDialog = false
                    showToast("${newTx.category} transaction added!")
                }
            )
        }
    }
}

@Composable
private fun BottomNavBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit
) {
    Surface(
        color = FinBg,
        tonalElevation = 8.dp,
        modifier = Modifier.border(1.dp, FinBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavNavItem(
                title = "Home",
                icon = Icons.Default.Home,
                isSelected = currentTab == NavTab.HOME,
                onClick = { onTabSelected(NavTab.HOME) }
            )
            NavNavItem(
                title = "History",
                icon = Icons.Default.Receipt,
                isSelected = currentTab == NavTab.HISTORY,
                onClick = { onTabSelected(NavTab.HISTORY) }
            )
            NavNavItem(
                title = "Analysis",
                icon = Icons.Default.PieChart,
                isSelected = currentTab == NavTab.ANALYSIS,
                onClick = { onTabSelected(NavTab.ANALYSIS) }
            )
            NavNavItem(
                title = "Settings",
                icon = Icons.Default.Settings,
                isSelected = currentTab == NavTab.SETTINGS,
                onClick = { onTabSelected(NavTab.SETTINGS) }
            )
        }
    }
}

@Composable
private fun NavNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.height(56.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) FinAccent else FinTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = if (isSelected) FinAccent else FinTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                if (isSelected) {
                    Spacer(Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(FinAccent, CircleShape)
                    )
                }
            }
        }
    }
}
