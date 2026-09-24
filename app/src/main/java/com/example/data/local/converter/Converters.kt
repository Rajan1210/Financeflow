package com.example.data.local.converter

import androidx.room.TypeConverter
import com.example.domain.model.BankType
import com.example.domain.model.InvestmentType
import com.example.domain.model.TransactionCategory
import com.example.domain.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = try {
        TransactionType.valueOf(value)
    } catch (e: Exception) {
        TransactionType.EXPENSE
    }

    @TypeConverter
    fun fromTransactionCategory(value: TransactionCategory): String = value.name

    @TypeConverter
    fun toTransactionCategory(value: String): TransactionCategory = try {
        TransactionCategory.valueOf(value)
    } catch (e: Exception) {
        TransactionCategory.OTHERS
    }

    @TypeConverter
    fun fromBankType(value: BankType): String = value.name

    @TypeConverter
    fun toBankType(value: String): BankType = try {
        BankType.valueOf(value)
    } catch (e: Exception) {
        BankType.OTHER
    }

    @TypeConverter
    fun fromInvestmentType(value: InvestmentType): String = value.name

    @TypeConverter
    fun toInvestmentType(value: String): InvestmentType = try {
        InvestmentType.valueOf(value)
    } catch (e: Exception) {
        InvestmentType.OTHER
    }
}
