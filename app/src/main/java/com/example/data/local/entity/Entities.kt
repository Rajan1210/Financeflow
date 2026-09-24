package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.BankType
import com.example.domain.model.InvestmentType
import com.example.domain.model.TransactionCategory
import com.example.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["smsHash"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val merchant: String,
    val description: String,
    val date: Long,
    val bank: String,
    val accountId: Long? = null,
    val referenceId: String? = null,
    val smsBody: String? = null,
    val isFromSms: Boolean = false,
    val smsHash: String? = null
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val bank: BankType,
    val accountNumber: String,
    val balance: Double,
    val isCash: Boolean = false,
    val colorHex: String = "#10B981"
)

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardName: String,
    val bank: BankType,
    val lastFourDigits: String,
    val totalLimit: Double,
    val usedAmount: Double,
    val dueDate: String,
    val minDue: Double,
    val billAmount: Double
)

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["category", "monthYear"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: TransactionCategory,
    val monthYear: String,
    val allocatedAmount: Double
)

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: InvestmentType,
    val investedAmount: Double,
    val currentValue: Double,
    val notes: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Long = 1,
    val username: String = "User",
    val email: String = "user@finpulse.app",
    val pinHash: String? = null,
    val isPinEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val currencySymbol: String = "₹",
    val isDarkMode: Boolean? = null
)

@Entity(tableName = "notifications")
data class NotificationAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String,
    val isRead: Boolean = false
)
