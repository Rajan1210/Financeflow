package com.example.ui.screens.budgets

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.BudgetEntity
import com.example.domain.model.TransactionCategory
import com.example.domain.model.TransactionType
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currency = userProfile?.currencySymbol ?: "₹"

    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var selectedBudgetForEdit by remember { mutableStateOf<BudgetEntity?>(null) }

    // Calculate spend per category for current month
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfMonth = calendar.timeInMillis

    val categorySpentMap = remember(transactions) {
        transactions
            .filter { it.date >= startOfMonth && it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val totalBudget = budgets.sumOf { it.allocatedAmount }
    val totalSpent = budgets.sumOf { categorySpentMap[it.category] ?: 0.0 }
    val overallRatio = if (totalBudget > 0) (totalSpent / totalBudget).toFloat() else 0f

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("budgets_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Monthly Budgets", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(
                        onClick = { showAddBudgetDialog = true },
                        modifier = Modifier.testTag("add_budget_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Budget")
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Overall Budget Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Month Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val percent = (overallRatio * 100).toInt()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            percent >= 100 -> ExpenseRed.copy(alpha = 0.15f)
                                            percent >= 80 -> WarningAmber.copy(alpha = 0.15f)
                                            else -> IncomeGreen.copy(alpha = 0.15f)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$percent% Used",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        percent >= 100 -> ExpenseRed
                                        percent >= 80 -> WarningAmber
                                        else -> IncomeGreen
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { overallRatio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = when {
                                overallRatio >= 1.0f -> ExpenseRed
                                overallRatio >= 0.8f -> WarningAmber
                                else -> IncomeGreen
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Total Spent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$currency${String.format(Locale.getDefault(), "%,.0f", totalSpent)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Total Budget", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$currency${String.format(Locale.getDefault(), "%,.0f", totalBudget)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Categories Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category Limits (${budgets.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAddBudgetDialog = true }) {
                        Text("+ New Category")
                    }
                }
            }

            if (budgets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No category budgets set. Tap + to set a limit.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(budgets, key = { it.id }) { budget ->
                    val spent = categorySpentMap[budget.category] ?: 0.0
                    val ratio = (spent / budget.allocatedAmount.coerceAtLeast(1.0)).toFloat()
                    val percent = (ratio * 100).toInt()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBudgetForEdit = budget },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIconBadge(category = budget.category, size = 40)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = budget.category.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "$currency${String.format(Locale.getDefault(), "%,.0f", spent)} of $currency${String.format(Locale.getDefault(), "%,.0f", budget.allocatedAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "$percent%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = when {
                                        percent >= 100 -> ExpenseRed
                                        percent >= 90 -> WarningAmber
                                        percent >= 80 -> WarningAmber
                                        else -> IncomeGreen
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { ratio.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = when {
                                    percent >= 100 -> ExpenseRed
                                    percent >= 80 -> WarningAmber
                                    else -> IncomeGreen
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            if (percent >= 80) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = when {
                                        percent >= 100 -> "🚨 Overspent by $currency${String.format(Locale.getDefault(), "%,.0f", (spent - budget.allocatedAmount))}"
                                        percent >= 90 -> "⚠️ Critical: 90% limit reached"
                                        else -> "⚠️ Warning: 80% limit reached"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (percent >= 100) ExpenseRed else WarningAmber
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Budget Dialog
    if (showAddBudgetDialog) {
        AddBudgetDialog(
            currencySymbol = currency,
            onDismiss = { showAddBudgetDialog = false },
            onAdd = { cat, amt ->
                viewModel.addOrUpdateBudget(cat, amt)
                showAddBudgetDialog = false
            }
        )
    }

    selectedBudgetForEdit?.let { b ->
        EditBudgetDialog(
            budget = b,
            currencySymbol = currency,
            onDismiss = { selectedBudgetForEdit = null },
            onUpdate = { amt ->
                viewModel.addOrUpdateBudget(b.category, amt)
                selectedBudgetForEdit = null
            },
            onDelete = {
                viewModel.deleteBudget(b)
                selectedBudgetForEdit = null
            }
        )
    }
}

@Composable
fun AddBudgetDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAdd: (TransactionCategory, Double) -> Unit
) {
    var selectedCat by remember { mutableStateOf(TransactionCategory.FOOD) }
    var amountText by remember { mutableStateOf("10000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Set Category Budget", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Select Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransactionCategory.entries.filter { it != TransactionCategory.SALARY }.forEach { cat ->
                        FilterChip(
                            selected = selectedCat == cat,
                            onClick = { selectedCat = cat },
                            label = { Text(cat.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monthly Limit ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onAdd(selectedCat, amt)
                    }
                }
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditBudgetDialog(
    budget: BudgetEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onUpdate: (Double) -> Unit,
    onDelete: () -> Unit
) {
    var amountText by remember { mutableStateOf(budget.allocatedAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit ${budget.category.displayName} Budget", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monthly Limit ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: budget.allocatedAmount
                    onUpdate(amt)
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
            ) {
                Text("Remove")
            }
        }
    )
}
