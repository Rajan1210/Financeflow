package com.example.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.TransactionCategory
import com.example.domain.model.TransactionType
import com.example.ui.components.AppNavDestination
import com.example.ui.components.TransactionRow
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigate: (AppNavDestination) -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }
    val recentList = remember(transactions) { transactions.take(6) }

    var isBalanceVisible by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addDialogType by remember { mutableStateOf(TransactionType.EXPENSE) }

    val currency = userProfile?.currencySymbol ?: "₹"

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (userProfile?.username?.take(1) ?: "A").uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Welcome back,",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = userProfile?.username ?: "Alex",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigate(AppNavDestination.SMS_SYNC) },
                        modifier = Modifier.testTag("dashboard_sync_sms_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Sync SMS",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { onNavigate(AppNavDestination.SETTINGS) },
                            modifier = Modifier.testTag("dashboard_notifications_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(ExpenseRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$unreadCount",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Total Balance Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("total_balance_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0F2B28),
                                        Color(0xFF0B463B),
                                        Color(0xFF065F46)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOTAL BALANCE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFA7F3D0),
                                    letterSpacing = 1.sp
                                )
                                IconButton(
                                    onClick = { isBalanceVisible = !isBalanceVisible },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isBalanceVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Toggle Balance",
                                        tint = Color(0xFFA7F3D0)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (isBalanceVisible) {
                                    "$currency${String.format(Locale.getDefault(), "%,.2f", metrics.totalBalance)}"
                                } else "••••••••",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Mini stats inside card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Monthly Income",
                                        fontSize = 11.sp,
                                        color = Color(0xFFD1FAE5)
                                    )
                                    Text(
                                        text = "+$currency${String.format(Locale.getDefault(), "%,.0f", metrics.monthlyIncome)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Monthly Expense",
                                        fontSize = 11.sp,
                                        color = Color(0xFFD1FAE5)
                                    )
                                    Text(
                                        text = "-$currency${String.format(Locale.getDefault(), "%,.0f", metrics.monthlyExpense)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCA5A5)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Cash Wallet",
                                        fontSize = 11.sp,
                                        color = Color(0xFFD1FAE5)
                                    )
                                    Text(
                                        text = "$currency${String.format(Locale.getDefault(), "%,.0f", metrics.cashBalance)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Quick Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionButton(
                        icon = Icons.Filled.Add,
                        label = "Expense",
                        color = ExpenseRed,
                        onClick = {
                            addDialogType = TransactionType.EXPENSE
                            showAddDialog = true
                        }
                    )
                    QuickActionButton(
                        icon = Icons.Filled.ArrowDownward,
                        label = "Income",
                        color = IncomeGreen,
                        onClick = {
                            addDialogType = TransactionType.INCOME
                            showAddDialog = true
                        }
                    )
                    QuickActionButton(
                        icon = Icons.Filled.Sms,
                        label = "SMS Sync",
                        color = TealSecondary,
                        onClick = { onNavigate(AppNavDestination.SMS_SYNC) }
                    )
                    QuickActionButton(
                        icon = Icons.Filled.AccountBalanceWallet,
                        label = "Accounts",
                        color = WarningAmber,
                        onClick = { onNavigate(AppNavDestination.ACCOUNTS) }
                    )
                    QuickActionButton(
                        icon = Icons.Filled.AutoAwesome,
                        label = "AI Insights",
                        color = PurpleAccent,
                        onClick = { onNavigate(AppNavDestination.ANALYTICS) }
                    )
                }
            }

            // 3. Monthly Budget Progress Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(AppNavDestination.BUDGETS) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.PieChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Monthly Budget Progress",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            val percent = (metrics.budgetProgressPercent * 100).toInt()
                            val badgeColor = when {
                                percent >= 100 -> ExpenseRed
                                percent >= 80 -> WarningAmber
                                else -> IncomeGreen
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$percent%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val progressColor = when {
                            metrics.budgetProgressPercent >= 1.0f -> ExpenseRed
                            metrics.budgetProgressPercent >= 0.8f -> WarningAmber
                            else -> IncomeGreen
                        }

                        LinearProgressIndicator(
                            progress = { metrics.budgetProgressPercent.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Spent: $currency${String.format(Locale.getDefault(), "%,.0f", metrics.monthlyBudgetSpent)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Limit: $currency${String.format(Locale.getDefault(), "%,.0f", metrics.monthlyBudgetTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (metrics.budgetProgressPercent >= 0.8f) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (metrics.budgetProgressPercent >= 1.0f)
                                    "⚠️ You have exceeded your monthly budget!"
                                else "⚠️ You have used over 80% of your budget.",
                                color = if (metrics.budgetProgressPercent >= 1.0f) ExpenseRed else WarningAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 4. Investments & Credit Cards Dual Mini-Overview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Investments card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(AppNavDestination.INVESTMENTS) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Investments",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$currency${String.format(Locale.getDefault(), "%,.0f", metrics.totalInvestmentsValue)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val profitSign = if (metrics.investmentProfitLoss >= 0) "+" else ""
                            Text(
                                text = "$profitSign$currency${String.format(Locale.getDefault(), "%,.0f", metrics.investmentProfitLoss)}",
                                fontSize = 11.sp,
                                color = if (metrics.investmentProfitLoss >= 0) IncomeGreen else ExpenseRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Credit Cards card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(AppNavDestination.ACCOUNTS) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CreditCard,
                                    contentDescription = null,
                                    tint = TealSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Credit Cards",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Used: $currency${String.format(Locale.getDefault(), "%,.0f", metrics.totalCreditUsed)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Avail: $currency${String.format(Locale.getDefault(), "%,.0f", metrics.availableCredit)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 5. Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { onNavigate(AppNavDestination.TRANSACTIONS) },
                        modifier = Modifier.testTag("dashboard_view_all_tx")
                    ) {
                        Text("View All")
                    }
                }
            }

            // 6. Recent Transaction Items
            if (recentList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions yet. Add manually or sync SMS.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(recentList, key = { it.id }) { tx ->
                    TransactionRow(
                        transaction = tx,
                        currencySymbol = currency,
                        onClick = { onSelectTransaction(tx) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Quick Add Dialog
    if (showAddDialog) {
        QuickAddTransactionDialog(
            initialType = addDialogType,
            currencySymbol = currency,
            onDismiss = { showAddDialog = false },
            onAdd = { amount, type, category, merchant, description, bank ->
                viewModel.addTransaction(
                    amount = amount,
                    type = type,
                    category = category,
                    merchant = merchant,
                    description = description,
                    bank = bank
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun QuickAddTransactionDialog(
    initialType: TransactionType,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAdd: (Double, TransactionType, TransactionCategory, String, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedCategory by remember {
        mutableStateOf(if (initialType == TransactionType.INCOME) TransactionCategory.SALARY else TransactionCategory.FOOD)
    }
    var bankText by remember { mutableStateOf("HDFC Bank") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add ${if (selectedType == TransactionType.EXPENSE) "Expense" else "Income"}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = {
                            selectedType = TransactionType.EXPENSE
                            selectedCategory = TransactionCategory.FOOD
                        },
                        label = { Text("Expense") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = {
                            selectedType = TransactionType.INCOME
                            selectedCategory = TransactionCategory.SALARY
                        },
                        label = { Text("Income") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text("Merchant / Source") },
                    placeholder = { Text("e.g. Swiggy, Amazon, Salary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bankText,
                    onValueChange = { bankText = it },
                    label = { Text("Bank / Account") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onAdd(
                            amount,
                            selectedType,
                            selectedCategory,
                            merchantText.ifBlank { "Manual ${selectedType.name.lowercase()}" },
                            descriptionText,
                            bankText
                        )
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
