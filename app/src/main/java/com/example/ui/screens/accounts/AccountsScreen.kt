package com.example.ui.screens.accounts

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CreditCardEntity
import com.example.domain.model.BankType
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsState()
    val creditCards by viewModel.creditCards.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currency = userProfile?.currencySymbol ?: "₹"

    var selectedTab by remember { mutableStateOf(0) } // 0: Bank Accounts, 1: Credit Cards
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    var selectedAccountForEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedCardForEdit by remember { mutableStateOf<CreditCardEntity?>(null) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("accounts_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Accounts & Cards", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(
                        onClick = { showTransferDialog = true },
                        modifier = Modifier.testTag("transfer_money_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.SwapHoriz, contentDescription = "Transfer")
                    }
                    IconButton(
                        onClick = {
                            if (selectedTab == 0) showAddAccountDialog = true
                            else showAddCardDialog = true
                        },
                        modifier = Modifier.testTag("add_account_or_card_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Bank & Cash (${accounts.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Credit Cards (${creditCards.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                if (selectedTab == 0) {
                    // Total Bank Balance
                    item {
                        val total = accounts.sumOf { it.balance }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Total Liquid Balance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$currency${String.format(Locale.getDefault(), "%,.2f", total)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Button(
                                    onClick = { showTransferDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Filled.SwapHoriz, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Transfer")
                                }
                            }
                        }
                    }

                    items(accounts, key = { it.id }) { acc ->
                        AccountItemCard(
                            account = acc,
                            currencySymbol = currency,
                            onClick = { selectedAccountForEdit = acc }
                        )
                    }
                } else {
                    // Credit Cards Overview
                    item {
                        val totalLimit = creditCards.sumOf { it.totalLimit }
                        val used = creditCards.sumOf { it.usedAmount }
                        val avail = (totalLimit - used).coerceAtLeast(0.0)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Total Credit Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "Total Limit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "$currency${String.format(Locale.getDefault(), "%,.0f", totalLimit)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text(text = "Used Credit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "$currency${String.format(Locale.getDefault(), "%,.0f", used)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                                    }
                                    Column {
                                        Text(text = "Available", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "$currency${String.format(Locale.getDefault(), "%,.0f", avail)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                                    }
                                }
                            }
                        }
                    }

                    items(creditCards, key = { it.id }) { card ->
                        CreditCardItem(
                            card = card,
                            currencySymbol = currency,
                            onClick = { selectedCardForEdit = card }
                        )
                    }
                }
            }
        }
    }

    // Edit Account Dialog
    selectedAccountForEdit?.let { acc ->
        EditAccountDialog(
            account = acc,
            currencySymbol = currency,
            onDismiss = { selectedAccountForEdit = null },
            onUpdate = { updated ->
                viewModel.updateAccount(updated)
                selectedAccountForEdit = null
            },
            onDelete = {
                viewModel.deleteAccount(acc)
                selectedAccountForEdit = null
            }
        )
    }

    // Edit Credit Card Dialog
    selectedCardForEdit?.let { card ->
        EditCreditCardDialog(
            card = card,
            currencySymbol = currency,
            onDismiss = { selectedCardForEdit = null },
            onUpdate = { updated ->
                viewModel.updateCreditCard(updated)
                selectedCardForEdit = null
            },
            onDelete = {
                viewModel.deleteCreditCard(card)
                selectedCardForEdit = null
            }
        )
    }

    // Add Account Dialog
    if (showAddAccountDialog) {
        AddAccountDialog(
            currencySymbol = currency,
            onDismiss = { showAddAccountDialog = false },
            onAdd = { name, bank, accNum, balance, isCash ->
                viewModel.addAccount(name, bank, accNum, balance, isCash)
                showAddAccountDialog = false
            }
        )
    }

    // Add Credit Card Dialog
    if (showAddCardDialog) {
        AddCreditCardDialog(
            currencySymbol = currency,
            onDismiss = { showAddCardDialog = false },
            onAdd = { name, bank, lastFour, limit, used, due, minDue, bill ->
                viewModel.addCreditCard(name, bank, lastFour, limit, used, due, minDue, bill)
                showAddCardDialog = false
            }
        )
    }

    // Transfer Money Dialog
    if (showTransferDialog && accounts.size >= 2) {
        TransferMoneyDialog(
            accounts = accounts,
            currencySymbol = currency,
            onDismiss = { showTransferDialog = false },
            onTransfer = { sourceId, destId, amount, note ->
                viewModel.transferMoney(sourceId, destId, amount, note)
                showTransferDialog = false
            }
        )
    }
}

@Composable
fun AccountItemCard(
    account: AccountEntity,
    currencySymbol: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (account.isCash) IncomeGreen.copy(alpha = 0.15f)
                        else TealDark.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (account.isCash) Icons.Filled.Wallet else Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = if (account.isCash) IncomeGreen else TealDark
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (account.isCash) "Physical Cash" else "A/C: ${account.accountNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", account.balance)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Edit balance",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CreditCardItem(
    card: CreditCardEntity,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val usedRatio = (card.usedAmount / card.totalLimit.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val avail = (card.totalLimit - card.usedAmount).coerceAtLeast(0.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF1E1B4B))
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = card.cardName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "•••• •••• •••• ${card.lastFourDigits}",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TealDark)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = card.bank.shortCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Used Amount", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text(
                            text = "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", card.usedAmount)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF87171)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Available Limit", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text(
                            text = "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", avail)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { usedRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (usedRatio > 0.8f) ExpenseRed else MintTertiary,
                    trackColor = Color(0xFF334155)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Due: ${card.dueDate}",
                        fontSize = 11.sp,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Bill: $currencySymbol${String.format(Locale.getDefault(), "%,.0f", card.billAmount)}",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun EditAccountDialog(
    account: AccountEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onUpdate: (AccountEntity) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var balanceText by remember { mutableStateOf(account.balance.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Balance ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceText.toDoubleOrNull() ?: account.balance
                    onUpdate(account.copy(name = name, balance = bal))
                }
            ) {
                Text("Save")
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
fun EditCreditCardDialog(
    card: CreditCardEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onUpdate: (CreditCardEntity) -> Unit,
    onDelete: () -> Unit
) {
    var usedText by remember { mutableStateOf(card.usedAmount.toString()) }
    var limitText by remember { mutableStateOf(card.totalLimit.toString()) }
    var dueText by remember { mutableStateOf(card.dueDate) }
    var billText by remember { mutableStateOf(card.billAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit ${card.cardName}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = usedText,
                    onValueChange = { usedText = it },
                    label = { Text("Used Amount ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Total Limit ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueText,
                    onValueChange = { dueText = it },
                    label = { Text("Due Date (e.g. 15th Oct)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = billText,
                    onValueChange = { billText = it },
                    label = { Text("Current Bill Amount ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val used = usedText.toDoubleOrNull() ?: card.usedAmount
                    val limit = limitText.toDoubleOrNull() ?: card.totalLimit
                    val bill = billText.toDoubleOrNull() ?: card.billAmount
                    onUpdate(
                        card.copy(
                            usedAmount = used,
                            totalLimit = limit,
                            dueDate = dueText,
                            billAmount = bill
                        )
                    )
                }
            ) {
                Text("Save")
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
fun AddAccountDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAdd: (String, BankType, String, Double, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") }
    var isCash by remember { mutableStateOf(false) }
    var selectedBank by remember { mutableStateOf(BankType.SBI) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isCash, onCheckedChange = { isCash = it })
                    Text("Cash Wallet")
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name (e.g. Axis Salary)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isCash) {
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("Account Number / Masked (e.g. XX1234)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Current Balance ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceText.toDoubleOrNull() ?: 0.0
                    val bank = if (isCash) BankType.CASH else selectedBank
                    onAdd(name.ifBlank { "Account" }, bank, accountNumber, bal, isCash)
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddCreditCardDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAdd: (String, BankType, String, Double, Double, String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var lastFour by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }
    var usedText by remember { mutableStateOf("0") }
    var dueText by remember { mutableStateOf("20th of month") }
    var minDueText by remember { mutableStateOf("500") }
    var billText by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Credit Card", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Card Name (e.g. HDFC Regalia)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lastFour,
                    onValueChange = { lastFour = it.take(4) },
                    label = { Text("Last 4 Digits") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Total Credit Limit ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = usedText,
                    onValueChange = { usedText = it },
                    label = { Text("Used Amount ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueText,
                    onValueChange = { dueText = it },
                    label = { Text("Due Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitText.toDoubleOrNull() ?: 100000.0
                    val used = usedText.toDoubleOrNull() ?: 0.0
                    val minDue = minDueText.toDoubleOrNull() ?: (used * 0.05)
                    val bill = billText.toDoubleOrNull() ?: used
                    onAdd(name.ifBlank { "Credit Card" }, BankType.OTHER, lastFour, limit, used, dueText, minDue, bill)
                }
            ) {
                Text("Add Card")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TransferMoneyDialog(
    accounts: List<AccountEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onTransfer: (Long, Long, Double, String) -> Unit
) {
    var sourceId by remember { mutableStateOf(accounts.first().id) }
    var destId by remember { mutableStateOf(accounts.last().id) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Transfer Between Accounts", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "From Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                accounts.forEach { acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sourceId = acc.id },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sourceId == acc.id, onClick = { sourceId = acc.id })
                        Text("${acc.name} ($currencySymbol${String.format(Locale.getDefault(), "%,.0f", acc.balance)})")
                    }
                }

                Divider()

                Text(text = "To Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                accounts.filter { it.id != sourceId }.forEach { acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { destId = acc.id },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = destId == acc.id, onClick = { destId = acc.id })
                        Text("${acc.name} ($currencySymbol${String.format(Locale.getDefault(), "%,.0f", acc.balance)})")
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Transfer Amount ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && sourceId != destId) {
                        onTransfer(sourceId, destId, amount, note)
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0 && sourceId != destId
            ) {
                Text("Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
