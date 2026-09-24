package com.example.ui.screens.analytics

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
import com.example.domain.model.TransactionCategory
import com.example.domain.model.TransactionType
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.SimpleBarChart
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar
import java.util.Locale

enum class ReportPeriod(val title: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_WEEK("This Week"),
    THIS_YEAR("This Year")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val aiInsights by viewModel.aiInsights.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currency = userProfile?.currencySymbol ?: "₹"

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.THIS_MONTH) }

    // Filter transactions based on selected period
    val periodTransactions = remember(allTransactions, selectedPeriod) {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        when (selectedPeriod) {
            ReportPeriod.THIS_MONTH -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                allTransactions.filter { it.date >= calendar.timeInMillis }
            }
            ReportPeriod.LAST_MONTH -> {
                calendar.timeInMillis = now
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                val end = calendar.timeInMillis
                allTransactions.filter { it.date in start until end }
            }
            ReportPeriod.THIS_WEEK -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                allTransactions.filter { it.date >= calendar.timeInMillis }
            }
            ReportPeriod.THIS_YEAR -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                allTransactions.filter { it.date >= calendar.timeInMillis }
            }
        }
    }

    val totalIncome = periodTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = periodTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    val categoryExpenses = remember(periodTransactions) {
        periodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    // Daily Average
    val daysCount = when (selectedPeriod) {
        ReportPeriod.THIS_WEEK -> 7
        ReportPeriod.THIS_MONTH -> 30
        ReportPeriod.LAST_MONTH -> 30
        ReportPeriod.THIS_YEAR -> 365
    }
    val avgDailySpend = totalExpense / daysCount

    // Highest single expense and income
    val highestExpenseTx = periodTransactions.filter { it.type == TransactionType.EXPENSE }.maxByOrNull { it.amount }
    val highestIncomeTx = periodTransactions.filter { it.type == TransactionType.INCOME }.maxByOrNull { it.amount }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Reports & Analytics", fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Period Selector Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.title, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Income vs Expense Comparison Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Cash Flow Comparison",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        SimpleBarChart(
                            income = totalIncome,
                            expense = totalExpense,
                            currencySymbol = currency
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        val net = totalIncome - totalExpense
                        Text(
                            text = "Net Cash Surplus: ${if (net >= 0) "+" else ""}$currency${String.format(Locale.getDefault(), "%,.0f", net)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (net >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }

            // Category Breakdown Donut Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Expense by Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        CategoryDonutChart(
                            categoryTotals = categoryExpenses,
                            currencySymbol = currency
                        )
                    }
                }
            }

            // Key Financial Metrics Card (Daily Avg, Highest Expense, Highest Income)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Spending Highlights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Daily Avg Spend", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$currency${String.format(Locale.getDefault(), "%,.0f", avgDailySpend)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(text = "Highest Expense", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = highestExpenseTx?.let { "$currency${String.format(Locale.getDefault(), "%,.0f", it.amount)}" } ?: "None",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Highest Income", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = highestIncomeTx?.let { "$currency${String.format(Locale.getDefault(), "%,.0f", it.amount)}" } ?: "None",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                        }
                    }
                }
            }

            // AI Insights Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Financial Insights & Recommendations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (aiInsights.isEmpty()) {
                item {
                    Text(
                        text = "No insights generated yet. Continue recording transactions to build personalized financial intelligence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(aiInsights) { insight ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (insight.type) {
                                "WARNING" -> WarningAmber.copy(alpha = 0.08f)
                                "SAVING" -> IncomeGreen.copy(alpha = 0.08f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (insight.type) {
                                        "WARNING" -> Icons.Filled.Warning
                                        "SAVING" -> Icons.Filled.Savings
                                        "BUDGET" -> Icons.Filled.PieChart
                                        else -> Icons.Filled.TipsAndUpdates
                                    },
                                    contentDescription = null,
                                    tint = when (insight.type) {
                                        "WARNING" -> WarningAmber
                                        "SAVING" -> IncomeGreen
                                        "BUDGET" -> TealDark
                                        else -> PurpleAccent
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = insight.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = insight.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
