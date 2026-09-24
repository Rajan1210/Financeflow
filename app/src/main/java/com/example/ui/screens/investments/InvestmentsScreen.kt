package com.example.ui.screens.investments

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
import com.example.data.local.entity.InvestmentEntity
import com.example.domain.model.InvestmentType
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val investments by viewModel.investments.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currency = userProfile?.currencySymbol ?: "₹"

    val totalInvested = investments.sumOf { it.investedAmount }
    val totalCurrent = investments.sumOf { it.currentValue }
    val totalProfitLoss = totalCurrent - totalInvested
    val totalReturnPercent = if (totalInvested > 0) ((totalProfitLoss / totalInvested) * 100).toInt() else 0

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedInvestmentForEdit by remember { mutableStateOf<InvestmentEntity?>(null) }
    var selectedTypeFilter by remember { mutableStateOf<InvestmentType?>(null) }

    val filteredInvestments = remember(investments, selectedTypeFilter) {
        if (selectedTypeFilter == null) investments
        else investments.filter { it.type == selectedTypeFilter }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("investments_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Investments & Wealth", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_investment_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Investment")
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
            // Portfolio Total Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "TOTAL PORTFOLIO VALUE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "$currency${String.format(Locale.getDefault(), "%,.2f", totalCurrent)}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Invested Capital", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$currency${String.format(Locale.getDefault(), "%,.0f", totalInvested)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Net Profit / Loss", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val profitSign = if (totalProfitLoss >= 0) "+" else ""
                                Text(
                                    text = "$profitSign$currency${String.format(Locale.getDefault(), "%,.0f", totalProfitLoss)} ($profitSign$totalReturnPercent%)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalProfitLoss >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            // Asset Type Filters
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("All Assets", fontSize = 12.sp) }
                    )
                    InvestmentType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedTypeFilter == type,
                            onClick = {
                                selectedTypeFilter = if (selectedTypeFilter == type) null else type
                            },
                            label = { Text(type.displayName, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Investments List
            if (filteredInvestments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No investments recorded for this filter. Tap + to add.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredInvestments, key = { it.id }) { inv ->
                    val pl = inv.currentValue - inv.investedAmount
                    val plPercent = if (inv.investedAmount > 0) ((pl / inv.investedAmount) * 100).toInt() else 0
                    val isProfit = pl >= 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedInvestmentForEdit = inv },
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
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (inv.type) {
                                            InvestmentType.MUTUAL_FUND, InvestmentType.SIP -> Color(0xFFCCFBF1)
                                            InvestmentType.STOCKS -> Color(0xFFE0E7FF)
                                            InvestmentType.GOLD -> Color(0xFFFEF3C7)
                                            InvestmentType.CRYPTO -> Color(0xFFFCE7F3)
                                            else -> Color(0xFFE2E8F0)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (inv.type) {
                                        InvestmentType.GOLD -> Icons.Filled.Savings
                                        InvestmentType.CRYPTO -> Icons.Filled.CurrencyBitcoin
                                        else -> Icons.Filled.TrendingUp
                                    },
                                    contentDescription = null,
                                    tint = when (inv.type) {
                                        InvestmentType.GOLD -> Color(0xFFD97706)
                                        InvestmentType.CRYPTO -> Color(0xFFBE185D)
                                        else -> Color(0xFF0F766E)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = inv.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${inv.type.displayName} • Invested: $currency${String.format(Locale.getDefault(), "%,.0f", inv.investedAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$currency${String.format(Locale.getDefault(), "%,.0f", inv.currentValue)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val sign = if (isProfit) "+" else ""
                                Text(
                                    text = "$sign$currency${String.format(Locale.getDefault(), "%,.0f", pl)} ($sign$plPercent%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isProfit) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Investment Dialog
    if (showAddDialog) {
        AddInvestmentDialog(
            currencySymbol = currency,
            onDismiss = { showAddDialog = false },
            onAdd = { name, type, invested, current, notes ->
                viewModel.addInvestment(name, type, invested, current, notes)
                showAddDialog = false
            }
        )
    }

    // Edit Investment Dialog
    selectedInvestmentForEdit?.let { inv ->
        EditInvestmentDialog(
            investment = inv,
            currencySymbol = currency,
            onDismiss = { selectedInvestmentForEdit = null },
            onUpdate = { updated ->
                viewModel.updateInvestment(updated)
                selectedInvestmentForEdit = null
            },
            onDelete = {
                viewModel.deleteInvestment(inv)
                selectedInvestmentForEdit = null
            }
        )
    }
}

@Composable
fun AddInvestmentDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAdd: (String, InvestmentType, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(InvestmentType.MUTUAL_FUND) }
    var investedText by remember { mutableStateOf("") }
    var currentText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Investment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Asset / Scheme Name") },
                    placeholder = { Text("e.g. Nifty 50 Index Fund") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "Asset Class", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InvestmentType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = investedText,
                    onValueChange = {
                        investedText = it
                        if (currentText.isBlank()) currentText = it
                    },
                    label = { Text("Invested Amount ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentText,
                    onValueChange = { currentText = it },
                    label = { Text("Current Market Value ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val inv = investedText.toDoubleOrNull() ?: 0.0
                    val cur = currentText.toDoubleOrNull() ?: inv
                    if (name.isNotBlank() && inv > 0) {
                        onAdd(name, selectedType, inv, cur, notes)
                    }
                },
                enabled = name.isNotBlank() && (investedText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditInvestmentDialog(
    investment: InvestmentEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onUpdate: (InvestmentEntity) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(investment.name) }
    var investedText by remember { mutableStateOf(investment.investedAmount.toString()) }
    var currentText by remember { mutableStateOf(investment.currentValue.toString()) }
    var notes by remember { mutableStateOf(investment.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Investment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Asset Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = investedText,
                    onValueChange = { investedText = it },
                    label = { Text("Invested Amount ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = currentText,
                    onValueChange = { currentText = it },
                    label = { Text("Current Market Value ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val inv = investedText.toDoubleOrNull() ?: investment.investedAmount
                    val cur = currentText.toDoubleOrNull() ?: investment.currentValue
                    onUpdate(
                        investment.copy(
                            name = name,
                            investedAmount = inv,
                            currentValue = cur,
                            notes = notes,
                            lastUpdated = System.currentTimeMillis()
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
