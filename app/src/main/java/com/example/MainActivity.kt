package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TransactionEntity
import com.example.ui.components.AppNavDestination
import com.example.ui.components.FinPulseBottomBar
import com.example.ui.screens.accounts.AccountsScreen
import com.example.ui.screens.analytics.AnalyticsScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.budgets.BudgetsScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.investments.InvestmentsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.sms.SmsSyncScreen
import com.example.ui.screens.transactions.TransactionDetailDialog
import com.example.ui.screens.transactions.TransactionsScreen
import com.example.ui.theme.FinPulseTheme
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val isSessionUnlocked by viewModel.isSessionUnlocked.collectAsStateWithLifecycle()

            val systemDark = isSystemInDarkTheme()
            val isDark = userProfile?.isDarkMode ?: systemDark

            FinPulseTheme(darkTheme = isDark) {
                val snackbarHostState = remember { SnackbarHostState() }

                // Listen for Toast/Snackbar events
                LaunchedEffect(Unit) {
                    viewModel.toastMessage.collectLatest { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                // If PIN is enabled and app is locked, present the AuthScreen
                val isLocked = (userProfile?.isPinEnabled == true) && !isSessionUnlocked

                if (isLocked) {
                    AuthScreen(
                        viewModel = viewModel,
                        onSuccess = { viewModel.unlockBiometric() }
                    )
                } else {
                    MainAppScaffold(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    viewModel: FinanceViewModel,
    snackbarHostState: SnackbarHostState
) {
    var currentDestination by remember { mutableStateOf(AppNavDestination.DASHBOARD) }
    var selectedTransactionForDetail by remember { mutableStateOf<com.example.data.local.entity.TransactionEntity?>(null) }
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val currency = userProfile?.currencySymbol ?: "₹"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            FinPulseBottomBar(
                currentRoute = currentDestination.route,
                onNavigate = { dest -> currentDestination = dest }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                AppNavDestination.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigate = { dest -> currentDestination = dest },
                    onSelectTransaction = { tx -> selectedTransactionForDetail = tx }
                )
                AppNavDestination.TRANSACTIONS -> TransactionsScreen(
                    viewModel = viewModel
                )
                AppNavDestination.SMS_SYNC -> SmsSyncScreen(
                    viewModel = viewModel
                )
                AppNavDestination.ACCOUNTS -> AccountsScreen(
                    viewModel = viewModel
                )
                AppNavDestination.BUDGETS -> BudgetsScreen(
                    viewModel = viewModel
                )
                AppNavDestination.INVESTMENTS -> InvestmentsScreen(
                    viewModel = viewModel
                )
                AppNavDestination.ANALYTICS -> AnalyticsScreen(
                    viewModel = viewModel
                )
                AppNavDestination.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    selectedTransactionForDetail?.let { tx ->
        TransactionDetailDialog(
            transaction = tx,
            currencySymbol = currency,
            onDismiss = { selectedTransactionForDetail = null },
            onUpdate = { updated ->
                viewModel.updateTransaction(updated)
                selectedTransactionForDetail = null
            },
            onDelete = {
                viewModel.deleteTransaction(tx)
                selectedTransactionForDetail = null
            }
        )
    }
}
