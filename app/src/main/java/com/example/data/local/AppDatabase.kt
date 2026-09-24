package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.converter.Converters
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import com.example.domain.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        CreditCardEntity::class,
        BudgetEntity::class,
        InvestmentEntity::class,
        UserProfileEntity::class,
        NotificationAlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun budgetDao(): BudgetDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finpulse_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            // Default User Profile
            database.userProfileDao().insertOrUpdateProfile(
                UserProfileEntity(
                    id = 1,
                    username = "Alex Morgan",
                    email = "alex@finpulse.app",
                    pinHash = null,
                    isPinEnabled = false,
                    isBiometricEnabled = false,
                    currencySymbol = "₹"
                )
            )

            // Initial Default Accounts
            val defaultAccounts = listOf(
                AccountEntity(
                    name = "SBI Savings",
                    bank = BankType.SBI,
                    accountNumber = "XX9421",
                    balance = 45250.0,
                    isCash = false,
                    colorHex = "#0284C7"
                ),
                AccountEntity(
                    name = "HDFC Salary",
                    bank = BankType.HDFC,
                    accountNumber = "XX3104",
                    balance = 98400.0,
                    isCash = false,
                    colorHex = "#2563EB"
                ),
                AccountEntity(
                    name = "ICICI Active",
                    bank = BankType.ICICI,
                    accountNumber = "XX5520",
                    balance = 21780.0,
                    isCash = false,
                    colorHex = "#DC2626"
                ),
                AccountEntity(
                    name = "Cash Wallet",
                    bank = BankType.CASH,
                    accountNumber = "CASH",
                    balance = 3450.0,
                    isCash = true,
                    colorHex = "#059669"
                )
            )
            database.accountDao().insertAccounts(defaultAccounts)

            // Initial Credit Cards
            val defaultCards = listOf(
                CreditCardEntity(
                    cardName = "HDFC Millennia",
                    bank = BankType.HDFC,
                    lastFourDigits = "4819",
                    totalLimit = 150000.0,
                    usedAmount = 28400.0,
                    dueDate = "18th Oct",
                    minDue = 1420.0,
                    billAmount = 28400.0
                ),
                CreditCardEntity(
                    cardName = "ICICI Coral",
                    bank = BankType.ICICI,
                    lastFourDigits = "9201",
                    totalLimit = 100000.0,
                    usedAmount = 14250.0,
                    dueDate = "24th Oct",
                    minDue = 750.0,
                    billAmount = 14250.0
                )
            )
            database.creditCardDao().insertCreditCards(defaultCards)

            // Sample Monthly Budgets (Current month)
            val currentMonthYear = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
            val sampleBudgets = listOf(
                BudgetEntity(category = TransactionCategory.FOOD, monthYear = currentMonthYear, allocatedAmount = 12000.0),
                BudgetEntity(category = TransactionCategory.SHOPPING, monthYear = currentMonthYear, allocatedAmount = 10000.0),
                BudgetEntity(category = TransactionCategory.FUEL, monthYear = currentMonthYear, allocatedAmount = 4500.0),
                BudgetEntity(category = TransactionCategory.BILLS, monthYear = currentMonthYear, allocatedAmount = 8000.0),
                BudgetEntity(category = TransactionCategory.ENTERTAINMENT, monthYear = currentMonthYear, allocatedAmount = 4000.0),
                BudgetEntity(category = TransactionCategory.MEDICAL, monthYear = currentMonthYear, allocatedAmount = 3000.0)
            )
            sampleBudgets.forEach { database.budgetDao().insertBudget(it) }

            // Sample Investments
            val sampleInvestments = listOf(
                InvestmentEntity(
                    name = "Nifty 50 Index Direct",
                    type = InvestmentType.MUTUAL_FUND,
                    investedAmount = 120000.0,
                    currentValue = 148500.0,
                    notes = "Monthly SIP of ₹10,000"
                ),
                InvestmentEntity(
                    name = "Tata Motors & TCS Shares",
                    type = InvestmentType.STOCKS,
                    investedAmount = 85000.0,
                    currentValue = 97400.0,
                    notes = "Long term holdings"
                ),
                InvestmentEntity(
                    name = "Digital Sovereign Gold Bond",
                    type = InvestmentType.GOLD,
                    investedAmount = 50000.0,
                    currentValue = 64200.0,
                    notes = "Tranche 2023-24"
                ),
                InvestmentEntity(
                    name = "SBI High Return FD",
                    type = InvestmentType.FD,
                    investedAmount = 200000.0,
                    currentValue = 214000.0,
                    notes = "Matures Dec 2026, 7.1% p.a."
                ),
                InvestmentEntity(
                    name = "Public Provident Fund (PPF)",
                    type = InvestmentType.PPF,
                    investedAmount = 150000.0,
                    currentValue = 168000.0,
                    notes = "Tax saving 80C"
                )
            )
            sampleInvestments.forEach { database.investmentDao().insertInvestment(it) }

            // Sample Realistic Initial Transactions
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L
            val sampleTransactions = listOf(
                TransactionEntity(
                    amount = 85000.0,
                    type = TransactionType.INCOME,
                    category = TransactionCategory.SALARY,
                    merchant = "Acme Corp Tech",
                    description = "Salary for the month credited to HDFC A/c XX3104",
                    date = now - dayMillis * 23,
                    bank = "HDFC",
                    referenceId = "SAL/20260901/8892"
                ),
                TransactionEntity(
                    amount = 450.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.FOOD,
                    merchant = "Swiggy",
                    description = "Food delivery ordered via UPI",
                    date = now - dayMillis * 1,
                    bank = "HDFC",
                    referenceId = "UPI/625341889021"
                ),
                TransactionEntity(
                    amount = 2499.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.SHOPPING,
                    merchant = "Amazon India",
                    description = "Electronics purchase on HDFC Millennia",
                    date = now - dayMillis * 2,
                    bank = "HDFC",
                    referenceId = "AMZ-99124-77"
                ),
                TransactionEntity(
                    amount = 1800.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.FUEL,
                    merchant = "Indian Oil Petrol Pump",
                    description = "Fuel filled for vehicle",
                    date = now - dayMillis * 3,
                    bank = "SBI",
                    referenceId = "POS-IOCL-8821"
                ),
                TransactionEntity(
                    amount = 1299.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.BILLS,
                    merchant = "Airtel Fiber Broadband",
                    description = "Monthly high-speed internet payment",
                    date = now - dayMillis * 5,
                    bank = "ICICI",
                    referenceId = "BB-AIR-1092"
                ),
                TransactionEntity(
                    amount = 650.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.ENTERTAINMENT,
                    merchant = "PVR Cinemas",
                    description = "Movie tickets booking",
                    date = now - dayMillis * 6,
                    bank = "HDFC",
                    referenceId = "BMS-PVR-3341"
                ),
                TransactionEntity(
                    amount = 10000.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.INVESTMENT,
                    merchant = "Groww Mutual Funds",
                    description = "SIP Auto Debit Nifty 50 Index",
                    date = now - dayMillis * 7,
                    bank = "HDFC",
                    referenceId = "SIP-DEB-77218"
                ),
                TransactionEntity(
                    amount = 2500.0,
                    type = TransactionType.INCOME,
                    category = TransactionCategory.OTHERS,
                    merchant = "Rahul Sharma",
                    description = "Repayment of weekend trip expenses via UPI",
                    date = now - dayMillis * 8,
                    bank = "SBI",
                    referenceId = "UPI/9981273612"
                ),
                TransactionEntity(
                    amount = 2000.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.TRANSFER,
                    merchant = "ATM Cash Withdrawal",
                    description = "Cash withdrawal from SBI ATM",
                    date = now - dayMillis * 9,
                    bank = "SBI",
                    referenceId = "ATM-WDL-66512"
                )
            )
            sampleTransactions.forEach { database.transactionDao().insertTransaction(it) }

            // Initial Notifications
            database.notificationDao().insertNotification(
                NotificationAlertEntity(
                    title = "Salary Credited! 🎉",
                    message = "₹85,000 was credited by Acme Corp Tech to HDFC A/c XX3104.",
                    timestamp = now - dayMillis * 23,
                    type = "SALARY",
                    isRead = false
                )
            )
            database.notificationDao().insertNotification(
                NotificationAlertEntity(
                    title = "Credit Card Bill Reminder 💳",
                    message = "HDFC Millennia bill of ₹28,400 is due on 18th Oct. Pay early to avoid interest.",
                    timestamp = now - dayMillis * 1,
                    type = "CREDIT_CARD",
                    isRead = false
                )
            )
        }
    }
}
