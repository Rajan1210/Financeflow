package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.FinanceRepository
import com.example.data.sms.ParsedSmsResult
import com.example.data.sms.SmsReaderHelper
import com.example.data.sms.SmsSyncReport
import com.example.data.sms.SmsTransactionParser
import com.example.domain.model.BankType
import com.example.domain.model.InvestmentType
import com.example.domain.model.TransactionCategory
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class DateFilterType {
    ALL,
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH
}

data class DashboardMetrics(
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val cashBalance: Double = 0.0,
    val totalInvestmentsValue: Double = 0.0,
    val totalInvestedCost: Double = 0.0,
    val investmentProfitLoss: Double = 0.0,
    val totalCreditLimit: Double = 0.0,
    val totalCreditUsed: Double = 0.0,
    val availableCredit: Double = 0.0,
    val monthlyBudgetTotal: Double = 0.0,
    val monthlyBudgetSpent: Double = 0.0,
    val budgetProgressPercent: Float = 0f
)

data class AiFinancialInsight(
    val title: String,
    val description: String,
    val type: String, // "SAVING", "WARNING", "BUDGET", "TIP"
    val actionText: String? = null
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = FinanceRepository(database)

    // Data streams from repository
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creditCards: StateFlow<List<CreditCardEntity>> = repository.allCreditCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMonthYear: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    val budgets: StateFlow<List<BudgetEntity>> = repository.getBudgetsForMonth(currentMonthYear)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val investments: StateFlow<List<InvestmentEntity>> = repository.allInvestments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val notifications: StateFlow<List<NotificationAlertEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<TransactionCategory?>(null)
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    private val _selectedDateFilter = MutableStateFlow(DateFilterType.ALL)
    val selectedDateFilter = _selectedDateFilter.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedTypeFilter = _selectedTypeFilter.asStateFlow()

    // Authentication & App Lock
    private val _isSessionUnlocked = MutableStateFlow(false)
    val isSessionUnlocked = _isSessionUnlocked.asStateFlow()

    // Syncing state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _lastSyncReport = MutableStateFlow<SmsSyncReport?>(null)
    val lastSyncReport = _lastSyncReport.asStateFlow()

    // Status message for Snackbars
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    // Computed Dashboard Metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allTransactions,
        accounts,
        creditCards,
        budgets,
        investments
    ) { txList, accList, cardList, bgList, invList ->
        computeMetrics(txList, accList, cardList, bgList, invList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    // Filtered Transactions
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        searchQuery,
        selectedCategoryFilter,
        selectedDateFilter,
        selectedTypeFilter
    ) { txList, query, catFilter, dateFilter, typeFilter ->
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        txList.filter { tx ->
            // Search query matches merchant, description, bank, reference or category
            val matchesQuery = if (query.isBlank()) true else {
                tx.merchant.contains(query, ignoreCase = true) ||
                        tx.description.contains(query, ignoreCase = true) ||
                        tx.bank.contains(query, ignoreCase = true) ||
                        (tx.referenceId?.contains(query, ignoreCase = true) == true) ||
                        tx.category.displayName.contains(query, ignoreCase = true)
            }

            // Category filter
            val matchesCategory = catFilter == null || tx.category == catFilter

            // Type filter
            val matchesType = typeFilter == null || tx.type == typeFilter

            // Date filter
            val matchesDate = when (dateFilter) {
                DateFilterType.ALL -> true
                DateFilterType.TODAY -> {
                    calendar.timeInMillis = now
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfDay = calendar.timeInMillis
                    tx.date >= startOfDay
                }
                DateFilterType.YESTERDAY -> {
                    calendar.timeInMillis = now
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    val startOfYesterday = calendar.timeInMillis
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    val endOfYesterday = calendar.timeInMillis
                    tx.date in startOfYesterday until endOfYesterday
                }
                DateFilterType.THIS_WEEK -> {
                    calendar.timeInMillis = now
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    val startOfWeek = calendar.timeInMillis
                    tx.date >= startOfWeek
                }
                DateFilterType.THIS_MONTH -> {
                    calendar.timeInMillis = now
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    val startOfMonth = calendar.timeInMillis
                    tx.date >= startOfMonth
                }
                DateFilterType.LAST_MONTH -> {
                    calendar.timeInMillis = now
                    calendar.add(Calendar.MONTH, -1)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    val startOfLastMonth = calendar.timeInMillis
                    calendar.add(Calendar.MONTH, 1)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    val endOfLastMonth = calendar.timeInMillis
                    tx.date in startOfLastMonth until endOfLastMonth
                }
            }

            matchesQuery && matchesCategory && matchesType && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Insights computed based on current user financial behavior
    val aiInsights: StateFlow<List<AiFinancialInsight>> = combine(
        allTransactions,
        budgets,
        dashboardMetrics
    ) { txList, bgList, metrics ->
        generateAiInsights(txList, bgList, metrics)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun computeMetrics(
        txList: List<TransactionEntity>,
        accList: List<AccountEntity>,
        cardList: List<CreditCardEntity>,
        bgList: List<BudgetEntity>,
        invList: List<InvestmentEntity>
    ): DashboardMetrics {
        // Accounts & Cash
        val totalBal = accList.sumOf { it.balance }
        val cashBal = accList.filter { it.isCash }.sumOf { it.balance }

        // Monthly calculations
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        val currentMonthTxs = txList.filter { it.date >= startOfMonth }
        val mIncome = currentMonthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val mExpense = currentMonthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netSavings = mIncome - mExpense

        // Investments
        val invCost = invList.sumOf { it.investedAmount }
        val invValue = invList.sumOf { it.currentValue }
        val invProfit = invValue - invCost

        // Credit Cards
        val totalLimit = cardList.sumOf { it.totalLimit }
        val usedCredit = cardList.sumOf { it.usedAmount }
        val availCredit = (totalLimit - usedCredit).coerceAtLeast(0.0)

        // Budgets
        val budgetTotal = bgList.sumOf { it.allocatedAmount }
        val budgetSpent = currentMonthTxs
            .filter { it.type == TransactionType.EXPENSE }
            .filter { tx -> bgList.any { it.category == tx.category } }
            .sumOf { it.amount }
        val budgetPercent = if (budgetTotal > 0) (budgetSpent / budgetTotal).toFloat() else 0f

        return DashboardMetrics(
            totalBalance = totalBal,
            monthlyIncome = mIncome,
            monthlyExpense = mExpense,
            netSavings = netSavings,
            cashBalance = cashBal,
            totalInvestmentsValue = invValue,
            totalInvestedCost = invCost,
            investmentProfitLoss = invProfit,
            totalCreditLimit = totalLimit,
            totalCreditUsed = usedCredit,
            availableCredit = availCredit,
            monthlyBudgetTotal = budgetTotal,
            monthlyBudgetSpent = budgetSpent,
            budgetProgressPercent = budgetPercent
        )
    }

    private fun generateAiInsights(
        txList: List<TransactionEntity>,
        bgList: List<BudgetEntity>,
        metrics: DashboardMetrics
    ): List<AiFinancialInsight> {
        val insights = mutableListOf<AiFinancialInsight>()

        // 1. Spending Drain Analysis
        val categoryExpenses = txList.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        if (categoryExpenses.isNotEmpty()) {
            val topCategory = categoryExpenses.first()
            val percentOfTotal = if (metrics.monthlyExpense > 0) {
                ((topCategory.second / metrics.monthlyExpense) * 100).toInt()
            } else 0

            insights.add(
                AiFinancialInsight(
                    title = "Primary Spending: ${topCategory.first.displayName}",
                    description = "${topCategory.first.displayName} accounts for ${percentOfTotal}% (₹${String.format(Locale.getDefault(), "%,.0f", topCategory.second)}) of this month's expenses. Reducing discretionary dining or impulse purchases here could save ~₹${String.format(Locale.getDefault(), "%,.0f", topCategory.second * 0.2)}.",
                    type = "WARNING"
                )
            )
        }

        // 2. Budget Health Insight
        if (metrics.budgetProgressPercent >= 0.9f) {
            insights.add(
                AiFinancialInsight(
                    title = "Budget Threshold Alert (Over 90%)",
                    description = "You've utilized ${(metrics.budgetProgressPercent * 100).toInt()}% of your monthly budget limit. Slow down non-essential spends for the remaining days of this month.",
                    type = "WARNING"
                )
            )
        } else if (metrics.budgetProgressPercent > 0f) {
            insights.add(
                AiFinancialInsight(
                    title = "Healthy Budget Discipline",
                    description = "You are currently at ${(metrics.budgetProgressPercent * 100).toInt()}% of your budget. On track to retain positive surplus cash flow.",
                    type = "BUDGET"
                )
            )
        }

        // 3. Savings & Month-End Projection
        if (metrics.monthlyIncome > 0) {
            val savingsRate = ((metrics.netSavings / metrics.monthlyIncome) * 100).toInt()
            val projectedMonthEndSavings = metrics.netSavings.coerceAtLeast(0.0)
            insights.add(
                AiFinancialInsight(
                    title = "Estimated Month-End Savings: ₹${String.format(Locale.getDefault(), "%,.0f", projectedMonthEndSavings)}",
                    description = "Your current savings rate is $savingsRate%. Financial planners recommend directing at least 20% into diversified mutual funds, gold, or emergency liquid accounts.",
                    type = "SAVING"
                )
            )
        }

        // 4. Investment Growth Insight
        if (metrics.investmentProfitLoss > 0) {
            val returnPercent = if (metrics.totalInvestedCost > 0) {
                ((metrics.investmentProfitLoss / metrics.totalInvestedCost) * 100).toInt()
            } else 0
            insights.add(
                AiFinancialInsight(
                    title = "Portfolio Return: +${returnPercent}%",
                    description = "Your investment portfolio is generating a capital gain of ₹${String.format(Locale.getDefault(), "%,.0f", metrics.investmentProfitLoss)}. Compounding works best with sustained recurring SIPs.",
                    type = "TIP"
                )
            )
        } else {
            insights.add(
                AiFinancialInsight(
                    title = "Smart Wealth Tip: Set Up Automatic SIP",
                    description = "Automate investments right on salary day to build wealth consistently before spending starts.",
                    type = "TIP"
                )
            )
        }

        return insights
    }

    // Filter controls
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: TransactionCategory?) {
        _selectedCategoryFilter.value = category
    }

    fun setDateFilter(filter: DateFilterType) {
        _selectedDateFilter.value = filter
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    // Transaction CRUD
    fun addTransaction(
        amount: Double,
        type: TransactionType,
        category: TransactionCategory,
        merchant: String,
        description: String,
        bank: String,
        accountId: Long? = null
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                amount = amount,
                type = type,
                category = category,
                merchant = merchant.ifBlank { "Manual ${type.name.lowercase()}" },
                description = description,
                date = System.currentTimeMillis(),
                bank = bank,
                accountId = accountId,
                isFromSms = false
            )
            repository.insertTransaction(tx)
            _toastMessage.emit("Transaction added successfully")
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            _toastMessage.emit("Transaction updated")
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _toastMessage.emit("Transaction deleted")
        }
    }

    // Account CRUD
    fun addAccount(name: String, bank: BankType, accountNumber: String, balance: Double, isCash: Boolean) {
        viewModelScope.launch {
            val acc = AccountEntity(
                name = name,
                bank = bank,
                accountNumber = accountNumber,
                balance = balance,
                isCash = isCash
            )
            repository.insertAccount(acc)
            _toastMessage.emit("Account $name created")
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
            _toastMessage.emit("Account updated")
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            _toastMessage.emit("Account removed")
        }
    }

    fun transferMoney(sourceId: Long, destId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            val success = repository.transferBetweenAccounts(sourceId, destId, amount, note)
            if (success) {
                _toastMessage.emit("Transferred ₹$amount successfully")
            } else {
                _toastMessage.emit("Transfer failed: check account details")
            }
        }
    }

    // Credit Card CRUD
    fun addCreditCard(
        name: String,
        bank: BankType,
        lastFour: String,
        limit: Double,
        used: Double,
        due: String,
        minDue: Double,
        bill: Double
    ) {
        viewModelScope.launch {
            val card = CreditCardEntity(
                cardName = name,
                bank = bank,
                lastFourDigits = lastFour,
                totalLimit = limit,
                usedAmount = used,
                dueDate = due,
                minDue = minDue,
                billAmount = bill
            )
            repository.insertCreditCard(card)
            _toastMessage.emit("Credit card added")
        }
    }

    fun updateCreditCard(card: CreditCardEntity) {
        viewModelScope.launch {
            repository.updateCreditCard(card)
            _toastMessage.emit("Credit card updated")
        }
    }

    fun deleteCreditCard(card: CreditCardEntity) {
        viewModelScope.launch {
            repository.deleteCreditCard(card)
            _toastMessage.emit("Credit card deleted")
        }
    }

    // Budget CRUD
    fun addOrUpdateBudget(category: TransactionCategory, amount: Double) {
        viewModelScope.launch {
            val budget = BudgetEntity(
                category = category,
                monthYear = currentMonthYear,
                allocatedAmount = amount
            )
            repository.insertBudget(budget)
            _toastMessage.emit("Budget set for ${category.displayName}")
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
            _toastMessage.emit("Budget removed")
        }
    }

    // Investment CRUD
    fun addInvestment(name: String, type: InvestmentType, invested: Double, current: Double, notes: String) {
        viewModelScope.launch {
            val inv = InvestmentEntity(
                name = name,
                type = type,
                investedAmount = invested,
                currentValue = current,
                notes = notes
            )
            repository.insertInvestment(inv)
            _toastMessage.emit("Investment added")
        }
    }

    fun updateInvestment(investment: InvestmentEntity) {
        viewModelScope.launch {
            repository.updateInvestment(investment)
            _toastMessage.emit("Investment updated")
        }
    }

    fun deleteInvestment(investment: InvestmentEntity) {
        viewModelScope.launch {
            repository.deleteInvestment(investment)
            _toastMessage.emit("Investment deleted")
        }
    }

    // SMS Sync & Test Simulator
    fun syncSmsInbox(context: Context) {
        viewModelScope.launch {
            _isSyncing.value = true
            val report = repository.syncInboxSms(context)
            _lastSyncReport.value = report
            _isSyncing.value = false
            _toastMessage.emit("SMS Sync: Imported ${report.newTransactionsImported} new transactions")
        }
    }

    fun testAndImportSms(smsBody: String, sender: String = "VK-BANK"): ParsedSmsResult {
        val result = SmsTransactionParser.parse(smsBody, sender)
        if (result.isValidTransaction && result.transaction != null) {
            viewModelScope.launch {
                val res = repository.parseAndInsertSingleSms(smsBody, sender)
                if (res.isValidTransaction) {
                    _toastMessage.emit("Imported: ${result.transaction.merchant} ₹${result.transaction.amount}")
                } else {
                    _toastMessage.emit(res.reasonIfNotValid ?: "Already imported")
                }
            }
        }
        return result
    }

    fun importPresetSmsList() {
        viewModelScope.launch {
            var count = 0
            val presets = SmsReaderHelper.getPresetSmsList()
            presets.forEach { (sender, body) ->
                val res = repository.parseAndInsertSingleSms(body, sender)
                if (res.isValidTransaction) count++
            }
            _toastMessage.emit("Imported $count simulated banking SMS transactions")
        }
    }

    // PIN & Security
    fun setPin(pin: String) {
        viewModelScope.launch {
            repository.setPin(pin)
            _isSessionUnlocked.value = true
            _toastMessage.emit("Security PIN updated")
        }
    }

    fun unlockWithPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val matches = repository.verifyPin(pin)
            if (matches) {
                _isSessionUnlocked.value = true
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun unlockBiometric() {
        _isSessionUnlocked.value = true
    }

    fun disablePin() {
        viewModelScope.launch {
            repository.disablePin()
            _toastMessage.emit("PIN protection turned off")
        }
    }

    fun lockSession() {
        _isSessionUnlocked.value = false
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfileEntity()
            repository.updateProfile(current.copy(isBiometricEnabled = enabled))
            _toastMessage.emit("Biometric login ${if (enabled) "enabled" else "disabled"}")
        }
    }

    fun updateCurrency(symbol: String) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfileEntity()
            repository.updateProfile(current.copy(currencySymbol = symbol))
            _toastMessage.emit("Currency updated to $symbol")
        }
    }

    fun updateThemeMode(isDark: Boolean?) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfileEntity()
            repository.updateProfile(current.copy(isDarkMode = isDark))
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    fun exportData(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDataAsJson()
            onReady(json)
        }
    }

    fun importData(json: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.importDataFromJson(json)
            onComplete(success)
            if (success) {
                _toastMessage.emit("Data imported successfully")
            } else {
                _toastMessage.emit("Failed to import data: invalid format")
            }
        }
    }
}
