package com.myfintrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myfintrack.app.components.EditorRequest
import com.myfintrack.app.components.TransactionEditorSheet
import com.myfintrack.app.model.NavTab
import com.myfintrack.app.model.TransactionType
import com.myfintrack.app.screens.AnalysisScreen
import com.myfintrack.app.screens.HistoryScreen
import com.myfintrack.app.screens.HomeScreen
import com.myfintrack.app.screens.MoneyScreen
import com.myfintrack.app.screens.MoneySection
import com.myfintrack.app.screens.SettingsScreen
import com.myfintrack.app.theme.*
import com.myfintrack.app.viewmodel.FinanceViewModel

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

private data class TabSpec(val tab: NavTab, val label: String, val icon: ImageVector)

private val TABS = listOf(
    TabSpec(NavTab.HOME, "Home", Icons.Filled.Home),
    TabSpec(NavTab.HISTORY, "History", Icons.AutoMirrored.Filled.ReceiptLong),
    TabSpec(NavTab.MONEY, "Money", Icons.Filled.AccountBalanceWallet),
    TabSpec(NavTab.ANALYSIS, "Analysis", Icons.Filled.PieChart),
    TabSpec(NavTab.SETTINGS, "Settings", Icons.Filled.Settings)
)

@Composable
fun MainAppContent(viewModel: FinanceViewModel = viewModel()) {
    val transactions by viewModel.transactions.collectAsState()
    val loaded by viewModel.loaded.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val dashboard by viewModel.dashboard.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val accountBalances by viewModel.accountBalances.collectAsState()
    val unlinkedNet by viewModel.unlinkedNetPaise.collectAsState()
    val budgetProgress by viewModel.budgetProgress.collectAsState()
    val goalProgress by viewModel.goalProgress.collectAsState()
    val accountNames = remember(accounts) { accounts.associate { it.id to it.name } }

    var currentTab by rememberSaveable { mutableStateOf(NavTab.HOME) }
    var moneySection by rememberSaveable { mutableStateOf(MoneySection.ACCOUNTS) }
    var editorRequest by remember { mutableStateOf<EditorRequest?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message.text,
                actionLabel = message.actionLabel,
                withDismissAction = message.actionLabel == null,
                duration = if (message.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
        }
    }

    // System back returns to Home before leaving the app.
    BackHandler(enabled = currentTab != NavTab.HOME && editorRequest == null) {
        currentTab = NavTab.HOME
    }

    val openEditor: (TransactionType) -> Unit = { type -> editorRequest = EditorRequest(initialType = type) }
    val openMoney: (MoneySection) -> Unit = { section ->
        moneySection = section
        currentTab = NavTab.MONEY
    }

    Scaffold(
        containerColor = FinBg,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = FinCardHover,
                    contentColor = FinTextPrimary,
                    actionColor = FinAccent,
                    dismissActionContentColor = FinTextSecondary
                )
            }
        },
        floatingActionButton = {
            if (currentTab == NavTab.HOME || currentTab == NavTab.HISTORY) {
                ExtendedFloatingActionButton(
                    onClick = { openEditor(TransactionType.EXPENSE) },
                    containerColor = FinAccent,
                    contentColor = Color(0xFF052E16),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add", fontWeight = FontWeight.Bold) }
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = FinSurface, tonalElevation = 0.dp) {
                TABS.forEach { spec ->
                    NavigationBarItem(
                        selected = currentTab == spec.tab,
                        onClick = { currentTab = spec.tab },
                        icon = { Icon(spec.icon, contentDescription = null) },
                        label = { Text(spec.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FinAccent,
                            selectedTextColor = FinAccent,
                            indicatorColor = FinAccentGlow,
                            unselectedIconColor = FinTextSecondary,
                            unselectedTextColor = FinTextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (currentTab) {
            NavTab.HOME -> HomeScreen(
                state = dashboard,
                settings = settings,
                onToggleHideBalances = { viewModel.setHideBalances(!settings.hideBalances) },
                onAddIncome = { openEditor(TransactionType.INCOME) },
                onAddExpense = { openEditor(TransactionType.EXPENSE) },
                onTransfer = { openEditor(TransactionType.TRANSFER) },
                onOpenHistory = { currentTab = NavTab.HISTORY },
                onOpenSettings = { currentTab = NavTab.SETTINGS },
                onOpenAccounts = { openMoney(MoneySection.ACCOUNTS) },
                onOpenBudgets = { openMoney(MoneySection.BUDGETS) },
                onTransactionClick = { tx -> editorRequest = EditorRequest(tx.type, tx) },
                modifier = contentModifier,
                budgets = budgetProgress,
                accountNames = accountNames
            )

            NavTab.HISTORY -> HistoryScreen(
                transactions = transactions,
                loaded = loaded,
                hideAmounts = settings.hideBalances,
                onAddTransaction = { openEditor(TransactionType.EXPENSE) },
                onTransactionClick = { tx -> editorRequest = EditorRequest(tx.type, tx) },
                modifier = contentModifier,
                accountNames = accountNames
            )

            NavTab.MONEY -> MoneyScreen(
                section = moneySection,
                onSectionChange = { moneySection = it },
                loaded = loaded,
                accountBalances = accountBalances,
                unlinkedNetPaise = unlinkedNet,
                budgetProgress = budgetProgress,
                goalProgress = goalProgress,
                hideAmounts = settings.hideBalances,
                onTransfer = { openEditor(TransactionType.TRANSFER) },
                onSaveAccount = viewModel::saveAccount,
                onDeleteAccount = viewModel::deleteAccount,
                onSaveBudget = viewModel::saveBudget,
                onDeleteBudget = viewModel::deleteBudget,
                onSaveGoal = viewModel::saveGoal,
                onDeleteGoal = viewModel::deleteGoal,
                onContribute = viewModel::contributeToGoal,
                modifier = contentModifier
            )

            NavTab.ANALYSIS -> AnalysisScreen(
                transactions = transactions,
                hideAmounts = settings.hideBalances,
                onAddTransaction = { openEditor(TransactionType.EXPENSE) },
                onTransactionClick = { tx -> editorRequest = EditorRequest(tx.type, tx) },
                modifier = contentModifier
            )

            NavTab.SETTINGS -> SettingsScreen(
                settings = settings,
                transactionCount = transactions.size,
                onHideBalancesChange = viewModel::setHideBalances,
                onDisplayNameChange = viewModel::setDisplayName,
                onExportCsv = viewModel::exportCsv,
                onDeleteAllData = viewModel::deleteAllData,
                modifier = contentModifier
            )
        }
    }

    editorRequest?.let { request ->
        TransactionEditorSheet(
            request = request,
            accounts = accounts,
            onManageAccounts = {
                editorRequest = null
                openMoney(MoneySection.ACCOUNTS)
            },
            onDismiss = { editorRequest = null },
            onSave = { tx ->
                viewModel.saveTransaction(tx)
                editorRequest = null
            },
            onDelete = { tx ->
                viewModel.deleteTransaction(tx)
                editorRequest = null
            }
        )
    }
}
