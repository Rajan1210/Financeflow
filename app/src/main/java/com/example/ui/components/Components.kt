package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.BankType
import com.example.domain.model.TransactionCategory
import com.example.domain.model.TransactionType
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class AppNavDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("dashboard", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    TRANSACTIONS("transactions", "Transactions", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    SMS_SYNC("sms_sync", "SMS Sync", Icons.Filled.Sms, Icons.Outlined.Sms),
    BUDGETS("budgets", "Budgets", Icons.Filled.PieChart, Icons.Outlined.PieChartOutline),
    ACCOUNTS("accounts", "Accounts", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    INVESTMENTS("investments", "Invest", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp),
    ANALYTICS("analytics", "Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun FinPulseBottomBar(
    currentRoute: String,
    onNavigate: (AppNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryDestinations = listOf(
        AppNavDestination.DASHBOARD,
        AppNavDestination.TRANSACTIONS,
        AppNavDestination.SMS_SYNC,
        AppNavDestination.ACCOUNTS,
        AppNavDestination.ANALYTICS
    )

    NavigationBar(
        modifier = modifier
            .navigationBarsPadding()
            .testTag("bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        primaryDestinations.forEach { dest ->
            val selected = currentRoute == dest.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(dest) },
                icon = {
                    Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = dest.title,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = dest.title,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ),
                modifier = Modifier.testTag("nav_item_${dest.route}")
            )
        }
    }
}

@Composable
fun CategoryIconBadge(
    category: TransactionCategory,
    size: Int = 42,
    modifier: Modifier = Modifier
) {
    val (icon, bgColor, iconColor) = when (category) {
        TransactionCategory.FOOD -> Triple(Icons.Filled.Restaurant, Color(0xFFFEE2E2), Color(0xFFDC2626))
        TransactionCategory.SHOPPING -> Triple(Icons.Filled.ShoppingBag, Color(0xFFE0E7FF), Color(0xFF4F46E5))
        TransactionCategory.TRAVEL -> Triple(Icons.Filled.DirectionsCar, Color(0xFFFEF3C7), Color(0xFFD97706))
        TransactionCategory.FUEL -> Triple(Icons.Filled.LocalGasStation, Color(0xFFFED7AA), Color(0xFFEA580C))
        TransactionCategory.MEDICAL -> Triple(Icons.Filled.LocalHospital, Color(0xFFFCE7F3), Color(0xFFDB2777))
        TransactionCategory.BILLS -> Triple(Icons.Filled.ReceiptLong, Color(0xFFE0F2FE), Color(0xFF0284C7))
        TransactionCategory.RECHARGE -> Triple(Icons.Filled.PhoneAndroid, Color(0xFFEDE9FE), Color(0xFF7C3AED))
        TransactionCategory.RENT -> Triple(Icons.Filled.Home, Color(0xFFDCFCE7), Color(0xFF16A34A))
        TransactionCategory.ENTERTAINMENT -> Triple(Icons.Filled.Movie, Color(0xFFF3E8FF), Color(0xFF9333EA))
        TransactionCategory.SALARY -> Triple(Icons.Filled.Work, Color(0xFFD1FAE5), Color(0xFF059669))
        TransactionCategory.INVESTMENT -> Triple(Icons.Filled.TrendingUp, Color(0xFFCCFBF1), Color(0xFF0D9488))
        TransactionCategory.TRANSFER -> Triple(Icons.Filled.SwapHoriz, Color(0xFFF1F5F9), Color(0xFF475569))
        TransactionCategory.OTHERS -> Triple(Icons.Filled.Category, Color(0xFFF1F5F9), Color(0xFF64748B))
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category.displayName,
            tint = iconColor,
            modifier = Modifier.size((size * 0.55f).dp)
        )
    }
}

@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    currencySymbol: String = "₹",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val isTransfer = transaction.type == TransactionType.TRANSFER
    val sign = when (transaction.type) {
        TransactionType.EXPENSE -> "-"
        TransactionType.INCOME -> "+"
        TransactionType.TRANSFER -> "⇄ "
    }
    val amountColor = when (transaction.type) {
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.INCOME -> IncomeGreen
        TransactionType.TRANSFER -> TealSecondary
    }

    val dateStr = remember(transaction.date) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(transaction.date))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tx_card_${transaction.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(category = transaction.category)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (transaction.isFromSms) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "SMS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.bank,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign$currencySymbol${String.format(Locale.getDefault(), "%,.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Text(
                    text = transaction.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun SimpleBarChart(
    income: Double,
    expense: Double,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    val total = (income + expense).coerceAtLeast(1.0)
    val incomePercent = (income / total).toFloat()
    val expensePercent = (expense / total).toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(IncomeGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Income: $currencySymbol${String.format(Locale.getDefault(), "%,.0f", income)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ExpenseRed)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Expense: $currencySymbol${String.format(Locale.getDefault(), "%,.0f", expense)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (income > 0) {
                Box(
                    modifier = Modifier
                        .weight(incomePercent.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(IncomeGreen)
                )
            }
            if (expense > 0) {
                Box(
                    modifier = Modifier
                        .weight(expensePercent.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(ExpenseRed)
                )
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    categoryTotals: List<Pair<TransactionCategory, Double>>,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    if (categoryTotals.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No expense data to display chart",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val totalExpense = categoryTotals.sumOf { it.second }.coerceAtLeast(1.0)
    val colors = listOf(
        Color(0xFFEF4444),
        Color(0xFF3B82F6),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899),
        Color(0xFF14B8A6),
        Color(0xFFF97316)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Custom Canvas Donut
        Box(
            modifier = Modifier
                .size(130.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                categoryTotals.take(8).forEachIndexed { index, pair ->
                    val sweepAngle = ((pair.second / totalExpense) * 360f).toFloat()
                    val sliceColor = colors[index % colors.size]
                    drawArc(
                        color = sliceColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", totalExpense)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Legend list
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categoryTotals.take(4).forEachIndexed { index, pair ->
                val percent = ((pair.second / totalExpense) * 100).toInt()
                val color = colors[index % colors.size]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pair.first.displayName,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "$percent%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
