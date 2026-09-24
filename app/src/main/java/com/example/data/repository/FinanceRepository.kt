package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.sms.ParsedSmsResult
import com.example.data.sms.SmsReaderHelper
import com.example.data.sms.SmsSyncReport
import com.example.data.sms.SmsTransactionParser
import com.example.domain.model.BankType
import com.example.domain.model.InvestmentType
import com.example.domain.model.TransactionCategory
import com.example.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceRepository(private val database: AppDatabase) {

    private val transactionDao = database.transactionDao()
    private val accountDao = database.accountDao()
    private val creditCardDao = database.creditCardDao()
    private val budgetDao = database.budgetDao()
    private val investmentDao = database.investmentDao()
    private val userProfileDao = database.userProfileDao()
    private val notificationDao = database.notificationDao()

    // --- Transactions ---
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactions(limit)

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = transactionDao.insertTransaction(transaction)
        // If an account is associated or bank name matches, adjust account balance
        if (id > 0) {
            adjustAccountBalanceForTransaction(transaction)
            checkBudgetAndLargeSpendAlerts(transaction)
        }
        return id
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun clearAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    private suspend fun adjustAccountBalanceForTransaction(tx: TransactionEntity) {
        val accounts = accountDao.getAllAccounts().first()
        val targetAccount = if (tx.accountId != null) {
            accounts.firstOrNull { it.id == tx.accountId }
        } else {
            accounts.firstOrNull { it.name.contains(tx.bank, ignoreCase = true) }
        }

        targetAccount?.let { acc ->
            val delta = when (tx.type) {
                TransactionType.INCOME -> tx.amount
                TransactionType.EXPENSE -> -tx.amount
                TransactionType.TRANSFER -> -tx.amount
            }
            accountDao.updateBalance(acc.id, acc.balance + delta)
        }
    }

    private suspend fun checkBudgetAndLargeSpendAlerts(tx: TransactionEntity) {
        // Large spend alert (> ₹10,000)
        if (tx.type == TransactionType.EXPENSE && tx.amount >= 10000.0) {
            notificationDao.insertNotification(
                NotificationAlertEntity(
                    title = "Large Spend Alert ⚠️",
                    message = "A large expense of ₹${String.format(Locale.getDefault(), "%,.0f", tx.amount)} was recorded for ${tx.merchant}.",
                    type = "LARGE_SPEND"
                )
            )
        }

        // Salary credit alert
        if (tx.type == TransactionType.INCOME && tx.category == TransactionCategory.SALARY) {
            notificationDao.insertNotification(
                NotificationAlertEntity(
                    title = "Salary Received! 💰",
                    message = "Salary of ₹${String.format(Locale.getDefault(), "%,.0f", tx.amount)} credited from ${tx.merchant}.",
                    type = "SALARY"
                )
            )
        }
    }

    // --- Accounts ---
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    suspend fun insertAccount(account: AccountEntity): Long = accountDao.insertAccount(account)
    suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)
    suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)
    suspend fun updateAccountBalance(id: Long, newBalance: Double) = accountDao.updateBalance(id, newBalance)

    suspend fun transferBetweenAccounts(
        sourceAccountId: Long,
        destinationAccountId: Long,
        amount: Double,
        note: String
    ): Boolean = withContext(Dispatchers.IO) {
        val accounts = accountDao.getAllAccounts().first()
        val source = accounts.firstOrNull { it.id == sourceAccountId } ?: return@withContext false
        val dest = accounts.firstOrNull { it.id == destinationAccountId } ?: return@withContext false

        accountDao.updateBalance(source.id, source.balance - amount)
        accountDao.updateBalance(dest.id, dest.balance + amount)

        val now = System.currentTimeMillis()
        val tx = TransactionEntity(
            amount = amount,
            type = TransactionType.TRANSFER,
            category = TransactionCategory.TRANSFER,
            merchant = "Transfer: ${source.name} ➔ ${dest.name}",
            description = note.ifBlank { "Account to account transfer" },
            date = now,
            bank = source.name,
            accountId = source.id
        )
        transactionDao.insertTransaction(tx)
        true
    }

    // --- Credit Cards ---
    val allCreditCards: Flow<List<CreditCardEntity>> = creditCardDao.getAllCreditCards()

    suspend fun insertCreditCard(card: CreditCardEntity): Long = creditCardDao.insertCreditCard(card)
    suspend fun updateCreditCard(card: CreditCardEntity) = creditCardDao.updateCreditCard(card)
    suspend fun deleteCreditCard(card: CreditCardEntity) = creditCardDao.deleteCreditCard(card)

    // --- Budgets ---
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonth(monthYear)

    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    suspend fun insertBudget(budget: BudgetEntity): Long = budgetDao.insertBudget(budget)
    suspend fun updateBudget(budget: BudgetEntity) = budgetDao.updateBudget(budget)
    suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.deleteBudget(budget)

    // --- Investments ---
    val allInvestments: Flow<List<InvestmentEntity>> = investmentDao.getAllInvestments()

    suspend fun insertInvestment(investment: InvestmentEntity): Long = investmentDao.insertInvestment(investment)
    suspend fun updateInvestment(investment: InvestmentEntity) = investmentDao.updateInvestment(investment)
    suspend fun deleteInvestment(investment: InvestmentEntity) = investmentDao.deleteInvestment(investment)

    // --- User Profile & Security ---
    val userProfile: Flow<UserProfileEntity?> = userProfileDao.getUserProfile()

    suspend fun updateProfile(profile: UserProfileEntity) =
        userProfileDao.insertOrUpdateProfile(profile)

    suspend fun setPin(pin: String) {
        val current = userProfileDao.getUserProfile().first() ?: UserProfileEntity()
        val hash = hashPin(pin)
        userProfileDao.insertOrUpdateProfile(
            current.copy(
                pinHash = hash,
                isPinEnabled = true
            )
        )
    }

    suspend fun verifyPin(pin: String): Boolean {
        val current = userProfileDao.getUserProfile().first() ?: return false
        val hash = hashPin(pin)
        return current.pinHash == hash
    }

    suspend fun disablePin() {
        val current = userProfileDao.getUserProfile().first() ?: return
        userProfileDao.insertOrUpdateProfile(
            current.copy(
                pinHash = null,
                isPinEnabled = false
            )
        )
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest("FINPULSE_SALT_$pin".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // --- Notifications ---
    val allNotifications: Flow<List<NotificationAlertEntity>> = notificationDao.getAllNotifications()
    suspend fun markAllNotificationsAsRead() = notificationDao.markAllAsRead()
    suspend fun clearAllNotifications() = notificationDao.clearAllNotifications()
    suspend fun insertNotification(notification: NotificationAlertEntity) =
        notificationDao.insertNotification(notification)

    // --- SMS Sync ---
    suspend fun syncInboxSms(context: Context): SmsSyncReport {
        return SmsReaderHelper.readAndImportSms(context, transactionDao)
    }

    suspend fun parseAndInsertSingleSms(body: String, sender: String): ParsedSmsResult {
        val result = SmsTransactionParser.parse(body, sender)
        if (result.isValidTransaction && result.transaction != null) {
            val tx = result.transaction
            val hash = tx.smsHash ?: ""
            val existingCount = transactionDao.getCountBySmsHash(hash)
            if (existingCount == 0) {
                insertTransaction(tx)
                return result
            } else {
                return ParsedSmsResult(
                    isValidTransaction = false,
                    reasonIfNotValid = "Duplicate transaction already exists in database."
                )
            }
        }
        return result
    }

    // --- Backup & Restore (JSON / Cloud architecture) ---
    suspend fun exportDataAsJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "FinPulse")
        root.put("exportDate", System.currentTimeMillis())

        val txList = transactionDao.getAllTransactions().first()
        val txArray = JSONArray()
        txList.forEach {
            val obj = JSONObject()
            obj.put("amount", it.amount)
            obj.put("type", it.type.name)
            obj.put("category", it.category.name)
            obj.put("merchant", it.merchant)
            obj.put("description", it.description)
            obj.put("date", it.date)
            obj.put("bank", it.bank)
            obj.put("referenceId", it.referenceId)
            obj.put("isFromSms", it.isFromSms)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val accList = accountDao.getAllAccounts().first()
        val accArray = JSONArray()
        accList.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("bank", it.bank.name)
            obj.put("accountNumber", it.accountNumber)
            obj.put("balance", it.balance)
            obj.put("isCash", it.isCash)
            accArray.put(obj)
        }
        root.put("accounts", accArray)

        val cardList = creditCardDao.getAllCreditCards().first()
        val cardArray = JSONArray()
        cardList.forEach {
            val obj = JSONObject()
            obj.put("cardName", it.cardName)
            obj.put("bank", it.bank.name)
            obj.put("lastFourDigits", it.lastFourDigits)
            obj.put("totalLimit", it.totalLimit)
            obj.put("usedAmount", it.usedAmount)
            obj.put("dueDate", it.dueDate)
            cardArray.put(obj)
        }
        root.put("creditCards", cardArray)

        val invList = investmentDao.getAllInvestments().first()
        val invArray = JSONArray()
        invList.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("type", it.type.name)
            obj.put("investedAmount", it.investedAmount)
            obj.put("currentValue", it.currentValue)
            obj.put("notes", it.notes)
            invArray.put(obj)
        }
        root.put("investments", invArray)

        root.toString(2)
    }

    suspend fun importDataFromJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)
            if (root.has("transactions")) {
                val array = root.getJSONArray("transactions")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val tx = TransactionEntity(
                        amount = obj.getDouble("amount"),
                        type = TransactionType.valueOf(obj.getString("type")),
                        category = TransactionCategory.valueOf(obj.getString("category")),
                        merchant = obj.getString("merchant"),
                        description = obj.optString("description", ""),
                        date = obj.getLong("date"),
                        bank = obj.optString("bank", "Bank"),
                        referenceId = obj.optString("referenceId", null),
                        isFromSms = obj.optBoolean("isFromSms", false)
                    )
                    transactionDao.insertTransaction(tx)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
