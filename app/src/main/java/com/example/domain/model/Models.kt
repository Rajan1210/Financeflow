package com.example.domain.model

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class TransactionCategory(val displayName: String, val iconName: String) {
    FOOD("Food & Dining", "Restaurant"),
    SHOPPING("Shopping", "ShoppingBag"),
    TRAVEL("Travel & Transport", "DirectionsCar"),
    FUEL("Fuel", "LocalGasStation"),
    MEDICAL("Medical & Healthcare", "LocalHospital"),
    BILLS("Bills & Utilities", "ReceiptLong"),
    RECHARGE("Mobile & Internet", "PhoneAndroid"),
    RENT("Rent & Housing", "Home"),
    ENTERTAINMENT("Entertainment", "Movie"),
    SALARY("Salary & Wage", "Work"),
    INVESTMENT("Investment", "TrendingUp"),
    TRANSFER("Transfer", "SwapHoriz"),
    OTHERS("Others", "Category");

    companion object {
        fun fromString(name: String?): TransactionCategory {
            if (name == null) return OTHERS
            return try {
                valueOf(name.uppercase())
            } catch (e: Exception) {
                entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) } ?: OTHERS
            }
        }
    }
}

enum class BankType(val displayName: String, val shortCode: String) {
    SBI("State Bank of India", "SBI"),
    HDFC("HDFC Bank", "HDFC"),
    ICICI("ICICI Bank", "ICICI"),
    AXIS("Axis Bank", "AXIS"),
    KOTAK("Kotak Mahindra", "KOTAK"),
    PNB("Punjab National Bank", "PNB"),
    BOB("Bank of Baroda", "BOB"),
    IDFC("IDFC First Bank", "IDFC"),
    CITI("Citibank", "CITI"),
    CASH("Cash Wallet", "CASH"),
    OTHER("Other / Not Listed", "OTHER");

    companion object {
        fun fromString(str: String?): BankType {
            if (str == null) return OTHER
            val upper = str.uppercase()
            return when {
                upper.contains("SBI") -> SBI
                upper.contains("HDFC") -> HDFC
                upper.contains("ICICI") -> ICICI
                upper.contains("AXIS") -> AXIS
                upper.contains("KOTAK") -> KOTAK
                upper.contains("PNB") -> PNB
                upper.contains("BARODA") || upper.contains("BOB") -> BOB
                upper.contains("IDFC") -> IDFC
                upper.contains("CITI") -> CITI
                upper.contains("CASH") -> CASH
                else -> OTHER
            }
        }
    }
}

enum class InvestmentType(val displayName: String) {
    MUTUAL_FUND("Mutual Funds"),
    SIP("SIP"),
    STOCKS("Stocks"),
    GOLD("Gold"),
    FD("Fixed Deposit"),
    RD("Recurring Deposit"),
    PPF("PPF"),
    EPF("EPF"),
    NPS("NPS"),
    CRYPTO("Crypto"),
    OTHER("Other")
}

enum class SmsDetectionType {
    UPI_PAYMENT,
    BANK_DEBIT,
    BANK_CREDIT,
    CREDIT_CARD_SPEND,
    ATM_WITHDRAWAL,
    SALARY_CREDIT,
    REFUND,
    INTEREST_CREDIT,
    EMI_DEBIT,
    UNKNOWN
}
