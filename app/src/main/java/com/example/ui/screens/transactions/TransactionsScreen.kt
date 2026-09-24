package com.example.ui.screens.transactions

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.TransactionCategory
import com.example.domain.model.TransactionType
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.TransactionRow
import com.example.ui.theme.*
import com.example.ui.viewmodel.DateFilterType
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currency = userProfile?.currencySymbol ?: "₹"

    var selectedTransactionForDetail by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Transactions",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_transaction_top_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Transaction")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .padding(bottom = 70.dp)
                    .testTag("add_tx_fab")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("search_tx_input"),
                placeholder = { Text("Search merchant, note, bank...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Date Filters Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateFilterType.entries.forEach { filter ->
                    val isSelected = selectedDateFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setDateFilter(filter) },
                        label = {
                            Text(
                                text = when (filter) {
                                    DateFilterType.ALL -> "All Time"
                                    DateFilterType.TODAY -> "Today"
                                    DateFilterType.YESTERDAY -> "Yesterday"
                                    DateFilterType.THIS_WEEK -> "This Week"
                                    DateFilterType.THIS_MONTH -> "This Month"
                                    DateFilterType.LAST_MONTH -> "Last Month"
                                },
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            // Categories Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text("All Categories", fontSize = 12.sp) }
                )
                TransactionCategory.entries.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.setCategoryFilter(if (isSelected) null else cat)
                        },
                        label = { Text(cat.displayName, fontSize = 12.sp) }
                    )
                }
            }

            // Type Filter Chips (All, Expense, Income, Transfer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTypeFilter == null,
                    onClick = { viewModel.setTypeFilter(null) },
                    label = { Text("All Types", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedTypeFilter == TransactionType.EXPENSE,
                    onClick = {
                        viewModel.setTypeFilter(if (selectedTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE)
                    },
                    label = { Text("Expense", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedTypeFilter == TransactionType.INCOME,
                    onClick = {
                        viewModel.setTypeFilter(if (selectedTypeFilter == TransactionType.INCOME) null else TransactionType.INCOME)
                    },
                    label = { Text("Income", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedTypeFilter == TransactionType.TRANSFER,
                    onClick = {
                        viewModel.setTypeFilter(if (selectedTypeFilter == TransactionType.TRANSFER) null else TransactionType.TRANSFER)
                    },
                    label = { Text("Transfer", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Results count
            Text(
                text = "${filteredTransactions.size} transactions found",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Transaction list
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No transactions found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Try adjusting your search or filters, or add a new transaction.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        TransactionRow(
                            transaction = tx,
                            currencySymbol = currency,
                            onClick = { selectedTransactionForDetail = tx }
                        )
                    }
                }
            }
        }
    }

    // Detail & Edit Transaction Dialog
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

    // Add Transaction Dialog
    if (showAddDialog) {
        AddTransactionModal(
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
fun TransactionDetailDialog(
    transaction: TransactionEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onUpdate: (TransactionEntity) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var merchant by remember { mutableStateOf(transaction.merchant) }
    var amountText by remember { mutableStateOf(transaction.amount.toString()) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var description by remember { mutableStateOf(transaction.description) }

    val dateFormatted = remember(transaction.date) {
        SimpleDateFormat("EEE, dd MMM yyyy - hh:mm a", Locale.getDefault()).format(Date(transaction.date))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Edit Transaction" else "Transaction Details",
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { isEditing = !isEditing }) {
                    Icon(
                        imageVector = if (isEditing) Icons.Filled.Close else Icons.Filled.Edit,
                        contentDescription = "Edit"
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text("Merchant / Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount ($currencySymbol)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Category",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    // Category dropdown or horizontal selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TransactionCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName, fontSize = 11.sp) }
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryIconBadge(category = transaction.category, size = 48)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = transaction.merchant,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", transaction.amount)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (transaction.type) {
                                    TransactionType.EXPENSE -> ExpenseRed
                                    TransactionType.INCOME -> IncomeGreen
                                    TransactionType.TRANSFER -> TealSecondary
                                }
                            )
                        }
                    }

                    Divider()

                    DetailRow(label = "Category", value = transaction.category.displayName)
                    DetailRow(label = "Bank / Source", value = transaction.bank)
                    DetailRow(label = "Date & Time", value = dateFormatted)
                    if (!transaction.referenceId.isNullOrBlank()) {
                        DetailRow(label = "Reference ID", value = transaction.referenceId)
                    }
                    if (transaction.isFromSms) {
                        DetailRow(label = "Source", value = "Imported from SMS")
                    }
                    if (transaction.description.isNotBlank()) {
                        DetailRow(label = "Note", value = transaction.description)
                    }
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                Button(
                    onClick = {
                        val parsed = amountText.toDoubleOrNull() ?: transaction.amount
                        onUpdate(
                            transaction.copy(
                                merchant = merchant,
                                amount = parsed,
                                category = selectedCategory,
                                description = description
                            )
                        )
                    }
                ) {
                    Text("Save Changes")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
            ) {
                Text("Delete")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AddTransactionModal(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAdd: (Double, TransactionType, TransactionCategory, String, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var bankText by remember { mutableStateOf("HDFC Bank") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf(TransactionCategory.FOOD) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Add New Transaction", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                    label = { Text("Merchant / Party") },
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

                Text(text = "Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransactionCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName, fontSize = 11.sp) }
                        )
                    }
                }

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
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
