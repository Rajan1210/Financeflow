package com.example.data.statement

import com.example.domain.model.BankType
import com.example.domain.model.TransactionCategory
import com.example.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

data class ParsedStatementRow(
    val date: Long,
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val referenceId: String?,
    val balanceAfter: Double?
)

interface BankStatementParser {
    fun parse(text: String): List<ParsedStatementRow>
}

object StatementParserUtils {
    private val DATE_FORMATS = listOf(
        "dd-MM-yyyy",
        "dd/MM/yyyy",
        "dd.MM.yyyy",
        "dd-MMM-yyyy",
        "dd/MMM/yyyy",
        "dd MMM yyyy",
        "dd-MMM-yy",
        "dd/MM/yy",
        "dd-MM-yy",
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "d-MMM-yyyy",
        "d/MMM/yyyy",
        "d MMM yyyy",
        "d/M/yyyy",
        "d-M-yyyy"
    )

    fun parseDate(dateStr: String): Long? {
        val clean = dateStr.trim().replace(Regex("[^0-9a-zA-Z/-]"), "-")
        for (pattern in DATE_FORMATS) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                    isLenient = false
                }
                val date = sdf.parse(clean)
                if (date != null) {
                    val cal = Calendar.getInstance().apply { time = date }
                    // Adjust 2-digit year (e.g. 26 -> 2026)
                    if (cal.get(Calendar.YEAR) < 100) {
                        cal.set(Calendar.YEAR, 2000 + cal.get(Calendar.YEAR))
                    }
                    return cal.timeInMillis
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun cleanAmount(amountStr: String?): Double? {
        if (amountStr.isNullOrBlank()) return null
        val cleaned = amountStr.replace(",", "").replace("₹", "").replace("Rs.", "", ignoreCase = true).trim()
        return cleaned.toDoubleOrNull()
    }

    fun extractReferenceId(text: String): String? {
        val patterns = listOf(
            Regex("""(?:UPI|Ref|Txn|Reference|Chq|UTR|RRN|IMPS|NEFT)[:/\s-]+([A-Za-z0-9]{6,25})""", RegexOption.IGNORE_CASE),
            Regex("""\b([0-9]{12})\b"""),
            Regex("""\b([A-Za-z0-9]{10,20})\b""")
        )
        for (p in patterns) {
            val m = p.find(text)
            if (m != null) {
                val ref = m.groupValues[1]
                if (!ref.equals("NOT_FOUND", ignoreCase = true)) {
                    return ref
                }
            }
        }
        return null
    }

    fun autoCategorize(description: String, type: TransactionType): TransactionCategory {
        val upper = description.uppercase()
        if (type == TransactionType.INCOME) {
            return when {
                upper.contains("SALARY") || upper.contains("PAYROLL") || upper.contains("STIPEND") ||
                        upper.contains("WAGES") || upper.contains("CORP TECH") -> TransactionCategory.SALARY
                upper.contains("DIVIDEND") || upper.contains("INTEREST") || upper.contains("PROFIT") -> TransactionCategory.INVESTMENT
                upper.contains("REFUND") || upper.contains("CASHBACK") -> TransactionCategory.OTHERS
                else -> TransactionCategory.OTHERS
            }
        }

        return when {
            // Food & Dining
            upper.contains("SWIGGY") || upper.contains("ZOMATO") || upper.contains("RESTAURANT") ||
                    upper.contains("CAFE") || upper.contains("MCDONALD") || upper.contains("STARBUCKS") ||
                    upper.contains("DOMINO") || upper.contains("KFC") || upper.contains("PIZZA") ||
                    upper.contains("FOOD") || upper.contains("BURGER") || upper.contains("BAKERY") -> TransactionCategory.FOOD

            // Shopping
            upper.contains("AMAZON") || upper.contains("FLIPKART") || upper.contains("MYNTRA") ||
                    upper.contains("ZARA") || upper.contains("H&M") || upper.contains("RETAIL") ||
                    upper.contains("DMART") || upper.contains("MEESHO") || upper.contains("AJIO") ||
                    upper.contains("SHOP") || upper.contains("STORE") || upper.contains("MALL") -> TransactionCategory.SHOPPING

            // Travel & Transport
            upper.contains("UBER") || upper.contains("OLA") || upper.contains("RAPIDO") ||
                    upper.contains("IRCTC") || upper.contains("MAKEMYTRIP") || upper.contains("CLEARTRIP") ||
                    upper.contains("INDIGO") || upper.contains("AIR INDIA") || upper.contains("METRO") ||
                    upper.contains("FASTAG") || upper.contains("TOLL") || upper.contains("CAB") -> TransactionCategory.TRAVEL

            // Fuel
            upper.contains("PETROL") || upper.contains("FUEL") || upper.contains("INDIAN OIL") ||
                    upper.contains("BHARAT PET") || upper.contains("BPCL") || upper.contains("HPCL") ||
                    upper.contains("SHELL") || upper.contains("IOCL") || upper.contains("GAS STATION") -> TransactionCategory.FUEL

            // Medical & Health
            upper.contains("HOSPITAL") || upper.contains("CLINIC") || upper.contains("PHARMACY") ||
                    upper.contains("APOLLO") || upper.contains("MEDPLUS") || upper.contains("NETMEDS") ||
                    upper.contains("1MG") || upper.contains("DOCTOR") || upper.contains("HEALTH") -> TransactionCategory.MEDICAL

            // Bills & Utilities
            upper.contains("ELECTRICITY") || upper.contains("WATER") || upper.contains("GAS") ||
                    upper.contains("BESCOM") || upper.contains("TATA POWER") || upper.contains("FIBER") ||
                    upper.contains("BROADBAND") || upper.contains("BILL") || upper.contains("BILLDESK") -> TransactionCategory.BILLS

            // Mobile & Recharge
            upper.contains("RECHARGE") || upper.contains("AIRTEL") || upper.contains("JIO") ||
                    upper.contains("VODAFONE") || upper.contains("VI PREPAID") || upper.contains("BSNL") -> TransactionCategory.RECHARGE

            // Rent & Housing
            upper.contains("RENT") || upper.contains("MAINTENANCE") || upper.contains("SOCIETY") ||
                    upper.contains("HOUSING") -> TransactionCategory.RENT

            // Entertainment
            upper.contains("NETFLIX") || upper.contains("SPOTIFY") || upper.contains("PRIME") ||
                    upper.contains("HOTSTAR") || upper.contains("CINEMA") || upper.contains("PVR") ||
                    upper.contains("INOX") || upper.contains("BOOKMYSHOW") || upper.contains("MOVIE") ||
                    upper.contains("THEATRE") || upper.contains("YOUTUBE") || upper.contains("SONY LIV") -> TransactionCategory.ENTERTAINMENT

            // Investment
            upper.contains("ZERODHA") || upper.contains("GROWW") || upper.contains("ANGEL") ||
                    upper.contains("MUTUAL FUND") || upper.contains("SIP") || upper.contains("UPSTOX") ||
                    upper.contains("SECURITIES") || upper.contains("GOLD") || upper.contains("NPS") ||
                    upper.contains("PPF") || upper.contains("FD ") || upper.contains("DEPOSIT") -> TransactionCategory.INVESTMENT

            // Transfer / Cash
            upper.contains("ATM") || upper.contains("CASH WDL") || upper.contains("WITHDRAWAL") ||
                    upper.contains("SELF TRANSFER") || upper.contains("TRANSFER TO") -> TransactionCategory.TRANSFER

            else -> TransactionCategory.OTHERS
        }
    }

    fun categorize(description: String, type: TransactionType = TransactionType.EXPENSE): TransactionCategory {
        return autoCategorize(description, type)
    }
}

/**
 * SBI Statement Parser
 * Standard layout:
 * Txn Date | Value Date | Description / Narration | Ref No./Cheque No. | Debit | Credit | Balance
 */
object SbiStatementParser : BankStatementParser {
    // Regex for date at beginning of line: e.g. 24-Sep-2026, 24/09/2026, 24 Sep 2026
    private val DATE_REGEX = Regex("""^(\d{1,2}[-/. ](?:[A-Za-z]{3}|\d{1,2})[-/. ]\d{2,4})""")
    private val AMOUNTS_AT_END = Regex("""([\d,]+\.\d{2})\s+([\d,]+\.\d{2})(?:\s+([\d,]+\.\d{2}))?\s*$""")

    override fun parse(text: String): List<ParsedStatementRow> {
        val results = mutableListOf<ParsedStatementRow>()
        val lines = text.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("Txn Date", ignoreCase = true) || trimmed.startsWith("Date", ignoreCase = true)) {
                continue
            }

            val dateMatch = DATE_REGEX.find(trimmed) ?: continue
            val dateStr = dateMatch.groupValues[1]
            val timestamp = StatementParserUtils.parseDate(dateStr) ?: continue

            // Check if amounts match at end of row
            val amountMatch = AMOUNTS_AT_END.find(trimmed)
            if (amountMatch != null) {
                val g1 = StatementParserUtils.cleanAmount(amountMatch.groupValues[1])
                val g2 = StatementParserUtils.cleanAmount(amountMatch.groupValues[2])
                val g3 = if (amountMatch.groupValues.size > 3) StatementParserUtils.cleanAmount(amountMatch.groupValues[3]) else null

                var debit: Double? = null
                var credit: Double? = null
                var balance: Double? = null

                if (g3 != null) {
                    // g1=Debit, g2=Credit, g3=Balance
                    debit = if (g1 != null && g1 > 0) g1 else null
                    credit = if (g2 != null && g2 > 0) g2 else null
                    balance = g3
                } else if (g1 != null && g2 != null) {
                    balance = g2
                    // Determine if g1 is Debit or Credit based on description keywords
                    val isCredit = trimmed.contains(" BY ", ignoreCase = true) ||
                            trimmed.contains("CREDIT", ignoreCase = true) ||
                            trimmed.contains("CR/", ignoreCase = true) ||
                            trimmed.contains("SALARY", ignoreCase = true)
                    if (isCredit) credit = g1 else debit = g1
                }

                val finalType = if (credit != null && credit > 0) TransactionType.INCOME else TransactionType.EXPENSE
                val finalAmount = credit ?: debit ?: 0.0

                if (finalAmount > 0.0) {
                    val middleText = trimmed.substring(dateMatch.range.last + 1, amountMatch.range.first).trim()
                    val refId = StatementParserUtils.extractReferenceId(middleText)
                    val desc = cleanDescription(middleText)
                    results.add(
                        ParsedStatementRow(
                            date = timestamp,
                            type = finalType,
                            amount = finalAmount,
                            description = desc,
                            referenceId = refId,
                            balanceAfter = balance
                        )
                    )
                }
            }
        }
        return results
    }

    private fun cleanDescription(desc: String): String {
        return desc.replace(Regex("""^\d{1,2}[-/. ](?:[A-Za-z]{3}|\d{1,2})[-/. ]\d{2,4}\s*"""), "") // Remove second date if present
            .replace(Regex("""\s+"""), " ")
            .trim()
            .ifBlank { "SBI Transaction" }
    }
}

/**
 * HDFC Bank Statement Parser
 * Standard layout:
 * Date | Narration | Chq./Ref.No. | Value Dt | Withdrawal Amt. | Deposit Amt. | Closing Balance
 */
object HdfcStatementParser : BankStatementParser {
    private val DATE_REGEX = Regex("""^(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})""")
    private val AMOUNTS_REGEX = Regex("""([\d,]+\.\d{2})\s+([\d,]+\.\d{2})(?:\s+([\d,]+\.\d{2}))?\s*$""")

    override fun parse(text: String): List<ParsedStatementRow> {
        val results = mutableListOf<ParsedStatementRow>()
        val lines = text.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("Date", ignoreCase = true) || trimmed.startsWith("Narration", ignoreCase = true)) {
                continue
            }

            val dateMatch = DATE_REGEX.find(trimmed) ?: continue
            val dateStr = dateMatch.groupValues[1]
            val timestamp = StatementParserUtils.parseDate(dateStr) ?: continue

            val amountMatch = AMOUNTS_REGEX.find(trimmed)
            if (amountMatch != null) {
                val g1 = StatementParserUtils.cleanAmount(amountMatch.groupValues[1])
                val g2 = StatementParserUtils.cleanAmount(amountMatch.groupValues[2])
                val g3 = if (amountMatch.groupValues.size > 3) StatementParserUtils.cleanAmount(amountMatch.groupValues[3]) else null

                val (type, amount, balance) = when {
                    g3 != null -> {
                        // Withdrawal, Deposit, Closing Balance
                        if (g2 != null && g2 > 0) Triple(TransactionType.INCOME, g2, g3)
                        else Triple(TransactionType.EXPENSE, g1 ?: 0.0, g3)
                    }
                    g1 != null && g2 != null -> {
                        val isIncome = trimmed.contains(" CR ", ignoreCase = true) ||
                                trimmed.contains("SALARY", ignoreCase = true) ||
                                trimmed.contains("CREDIT", ignoreCase = true) ||
                                trimmed.contains("NEFT CR", ignoreCase = true) ||
                                trimmed.contains("UPI CR", ignoreCase = true)
                        Triple(if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE, g1, g2)
                    }
                    else -> Triple(TransactionType.EXPENSE, 0.0, null)
                }

                if (amount > 0.0) {
                    val middleText = trimmed.substring(dateMatch.range.last + 1, amountMatch.range.first).trim()
                    val refId = StatementParserUtils.extractReferenceId(middleText)
                    val desc = middleText.replace(Regex("""\s+"""), " ").trim().ifBlank { "HDFC Transaction" }

                    results.add(
                        ParsedStatementRow(
                            date = timestamp,
                            type = type,
                            amount = amount,
                            description = desc,
                            referenceId = refId,
                            balanceAfter = balance
                        )
                    )
                }
            }
        }
        return results
    }
}

/**
 * ICICI Bank Statement Parser
 * Standard layout:
 * Value Date | Transaction Date | Cheque Number | Transaction Remarks | Withdrawal Amount (INR ) | Deposit Amount (INR ) | Balance (INR )
 */
object IciciStatementParser : BankStatementParser {
    private val DATE_REGEX = Regex("""^(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})""")
    private val AMOUNTS_REGEX = Regex("""([\d,]+\.\d{2})\s+([\d,]+\.\d{2})(?:\s+([\d,]+\.\d{2}))?\s*$""")

    override fun parse(text: String): List<ParsedStatementRow> {
        val results = mutableListOf<ParsedStatementRow>()
        val lines = text.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("Value Date", ignoreCase = true) || trimmed.startsWith("Tran Date", ignoreCase = true)) {
                continue
            }

            val dateMatch = DATE_REGEX.find(trimmed) ?: continue
            val dateStr = dateMatch.groupValues[1]
            val timestamp = StatementParserUtils.parseDate(dateStr) ?: continue

            val amountMatch = AMOUNTS_REGEX.find(trimmed)
            if (amountMatch != null) {
                val g1 = StatementParserUtils.cleanAmount(amountMatch.groupValues[1])
                val g2 = StatementParserUtils.cleanAmount(amountMatch.groupValues[2])
                val g3 = if (amountMatch.groupValues.size > 3) StatementParserUtils.cleanAmount(amountMatch.groupValues[3]) else null

                val (type, amount, balance) = when {
                    g3 != null -> {
                        if (g2 != null && g2 > 0) Triple(TransactionType.INCOME, g2, g3)
                        else Triple(TransactionType.EXPENSE, g1 ?: 0.0, g3)
                    }
                    g1 != null && g2 != null -> {
                        val isIncome = trimmed.contains(" CR ", ignoreCase = true) ||
                                trimmed.contains("CREDIT", ignoreCase = true) ||
                                trimmed.contains("SALARY", ignoreCase = true) ||
                                trimmed.contains("INF/", ignoreCase = true) && trimmed.contains("BY", ignoreCase = true)
                        Triple(if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE, g1, g2)
                    }
                    else -> Triple(TransactionType.EXPENSE, 0.0, null)
                }

                if (amount > 0.0) {
                    val middleText = trimmed.substring(dateMatch.range.last + 1, amountMatch.range.first).trim()
                    val refId = StatementParserUtils.extractReferenceId(middleText)
                    val desc = middleText.replace(Regex("""^\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\s*"""), "")
                        .replace(Regex("""\s+"""), " ")
                        .trim()
                        .ifBlank { "ICICI Transaction" }

                    results.add(
                        ParsedStatementRow(
                            date = timestamp,
                            type = type,
                            amount = amount,
                            description = desc,
                            referenceId = refId,
                            balanceAfter = balance
                        )
                    )
                }
            }
        }
        return results
    }
}

/**
 * Axis Bank Statement Parser
 * Standard layout:
 * Tran Date | Value Date | Transaction Details | Chq No | Debit | Credit | Balance | Init Br
 */
object AxisStatementParser : BankStatementParser {
    private val DATE_REGEX = Regex("""^(\d{1,2}-\d{1,2}-\d{2,4})""")
    private val AMOUNTS_REGEX = Regex("""([\d,]+\.\d{2})\s+([\d,]+\.\d{2})(?:\s+([\d,]+\.\d{2}))?""")

    override fun parse(text: String): List<ParsedStatementRow> {
        val results = mutableListOf<ParsedStatementRow>()
        val lines = text.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("Tran Date", ignoreCase = true) || trimmed.startsWith("Date", ignoreCase = true)) {
                continue
            }

            val dateMatch = DATE_REGEX.find(trimmed) ?: continue
            val dateStr = dateMatch.groupValues[1]
            val timestamp = StatementParserUtils.parseDate(dateStr) ?: continue

            val amountMatch = AMOUNTS_REGEX.find(trimmed)
            if (amountMatch != null) {
                val g1 = StatementParserUtils.cleanAmount(amountMatch.groupValues[1])
                val g2 = StatementParserUtils.cleanAmount(amountMatch.groupValues[2])
                val g3 = if (amountMatch.groupValues.size > 3) StatementParserUtils.cleanAmount(amountMatch.groupValues[3]) else null

                val (type, amount, balance) = when {
                    g3 != null -> {
                        if (g2 != null && g2 > 0) Triple(TransactionType.INCOME, g2, g3)
                        else Triple(TransactionType.EXPENSE, g1 ?: 0.0, g3)
                    }
                    g1 != null && g2 != null -> {
                        val isIncome = trimmed.contains(" CR ", ignoreCase = true) ||
                                trimmed.contains("CREDIT", ignoreCase = true) ||
                                trimmed.contains("SALARY", ignoreCase = true)
                        Triple(if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE, g1, g2)
                    }
                    else -> Triple(TransactionType.EXPENSE, 0.0, null)
                }

                if (amount > 0.0) {
                    val middleText = trimmed.substring(dateMatch.range.last + 1, amountMatch.range.first).trim()
                    val refId = StatementParserUtils.extractReferenceId(middleText)
                    val desc = middleText.replace(Regex("""^\d{1,2}-\d{1,2}-\d{2,4}\s*"""), "")
                        .replace(Regex("""\s+"""), " ")
                        .trim()
                        .ifBlank { "Axis Transaction" }

                    results.add(
                        ParsedStatementRow(
                            date = timestamp,
                            type = type,
                            amount = amount,
                            description = desc,
                            referenceId = refId,
                            balanceAfter = balance
                        )
                    )
                }
            }
        }
        return results
    }
}

/**
 * Kotak Mahindra Bank Statement Parser
 * Standard layout:
 * Date | Narration | Chq/Ref No | Value Date | Withdrawal (Dr) | Deposit (Cr) | Balance
 */
object KotakStatementParser : BankStatementParser {
    private val DATE_REGEX = Regex("""^(\d{1,2}[-/. ](?:[A-Za-z]{3}|\d{1,2})[-/. ]\d{2,4})""")
    private val AMOUNTS_REGEX = Regex("""([\d,]+\.\d{2})\s+([\d,]+\.\d{2})(?:\s+([\d,]+\.\d{2}))?\s*$""")

    override fun parse(text: String): List<ParsedStatementRow> {
        val results = mutableListOf<ParsedStatementRow>()
        val lines = text.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("Date", ignoreCase = true) || trimmed.startsWith("Sl.", ignoreCase = true)) {
                continue
            }

            val dateMatch = DATE_REGEX.find(trimmed) ?: continue
            val dateStr = dateMatch.groupValues[1]
            val timestamp = StatementParserUtils.parseDate(dateStr) ?: continue

            val amountMatch = AMOUNTS_REGEX.find(trimmed)
            if (amountMatch != null) {
                val g1 = StatementParserUtils.cleanAmount(amountMatch.groupValues[1])
                val g2 = StatementParserUtils.cleanAmount(amountMatch.groupValues[2])
                val g3 = if (amountMatch.groupValues.size > 3) StatementParserUtils.cleanAmount(amountMatch.groupValues[3]) else null

                val (type, amount, balance) = when {
                    g3 != null -> {
                        if (g2 != null && g2 > 0) Triple(TransactionType.INCOME, g2, g3)
                        else Triple(TransactionType.EXPENSE, g1 ?: 0.0, g3)
                    }
                    g1 != null && g2 != null -> {
                        val isIncome = trimmed.contains(" CR", ignoreCase = true) ||
                                trimmed.contains("DEPOSIT", ignoreCase = true) ||
                                trimmed.contains("SALARY", ignoreCase = true)
                        Triple(if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE, g1, g2)
                    }
                    else -> Triple(TransactionType.EXPENSE, 0.0, null)
                }

                if (amount > 0.0) {
                    val middleText = trimmed.substring(dateMatch.range.last + 1, amountMatch.range.first).trim()
                    val refId = StatementParserUtils.extractReferenceId(middleText)
                    val desc = middleText.replace(Regex("""\s+"""), " ").trim().ifBlank { "Kotak Transaction" }

                    results.add(
                        ParsedStatementRow(
                            date = timestamp,
                            type = type,
                            amount = amount,
                            description = desc,
                            referenceId = refId,
                            balanceAfter = balance
                        )
                    )
                }
            }
        }
        return results
    }
}

/**
 * Generic Statement Parser
 * Robust multi-bank fallback supporting standard tabular e-statements and freeform lines
 */
object GenericStatementParser : BankStatementParser {
    // Finds any date pattern: dd-mm-yyyy, dd/mm/yyyy, dd-MMM-yyyy, etc.
    private val DATE_PATTERN = Regex("""\b(\d{1,2}[-/.](?:[A-Za-z]{3}|\d{1,2})[-/.](?:20\d{2}|\d{2,4}))\b""")
    // Extracts numeric figures with decimals (like amounts: 1,500.00, 750.50, 85000.00)
    private val MONEY_PATTERN = Regex("""(?:Rs\.?|INR|₹)?\s*(-?[\d,]+\.\d{2})\b""")

    override fun parse(text: String): List<ParsedStatementRow> {
        val results = mutableListOf<ParsedStatementRow>()
        val lines = text.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || isHeaderLine(trimmed)) continue

            val dateMatch = DATE_PATTERN.find(trimmed) ?: continue
            val dateStr = dateMatch.groupValues[1]
            val timestamp = StatementParserUtils.parseDate(dateStr) ?: continue

            val moneyMatches = MONEY_PATTERN.findAll(trimmed).toList()
            if (moneyMatches.isEmpty()) continue

            // Determine transaction type
            val isExplicitCredit = trimmed.contains(" CR", ignoreCase = true) ||
                    trimmed.contains("CREDIT", ignoreCase = true) ||
                    trimmed.contains("DEPOSIT", ignoreCase = true) ||
                    trimmed.contains("RECEIVED", ignoreCase = true) ||
                    trimmed.contains("SALARY", ignoreCase = true) ||
                    trimmed.contains("REFUND", ignoreCase = true)

            val isExplicitDebit = trimmed.contains(" DR", ignoreCase = true) ||
                    trimmed.contains("DEBIT", ignoreCase = true) ||
                    trimmed.contains("WITHDRAWAL", ignoreCase = true) ||
                    trimmed.contains("PAID", ignoreCase = true) ||
                    trimmed.contains("SPENT", ignoreCase = true)

            var type = when {
                isExplicitCredit && !isExplicitDebit -> TransactionType.INCOME
                else -> TransactionType.EXPENSE
            }

            var amount = 0.0
            var balance: Double? = null

            if (moneyMatches.size >= 3) {
                // Typically: Debit | Credit | Balance OR Amount | Fee | Balance
                val m1 = StatementParserUtils.cleanAmount(moneyMatches[0].groupValues[1]) ?: 0.0
                val m2 = StatementParserUtils.cleanAmount(moneyMatches[1].groupValues[1]) ?: 0.0
                val m3 = StatementParserUtils.cleanAmount(moneyMatches[2].groupValues[1])

                if (m2 > 0.0 && m1 == 0.0) {
                    type = TransactionType.INCOME
                    amount = m2
                } else if (m1 > 0.0 && m2 == 0.0) {
                    type = TransactionType.EXPENSE
                    amount = m1
                } else {
                    amount = if (m1 > 0.0) m1 else m2
                }
                balance = m3
            } else if (moneyMatches.size == 2) {
                // Amount | Balance
                val m1 = StatementParserUtils.cleanAmount(moneyMatches[0].groupValues[1]) ?: 0.0
                val m2 = StatementParserUtils.cleanAmount(moneyMatches[1].groupValues[1])
                amount = m1
                balance = m2
            } else {
                amount = StatementParserUtils.cleanAmount(moneyMatches[0].groupValues[1]) ?: 0.0
            }

            if (amount < 0) {
                amount = -amount
                type = TransactionType.EXPENSE
            }

            if (amount > 0.0) {
                // Description is whatever text isn't the date or the money values
                var desc = trimmed
                    .replace(dateMatch.value, "")
                    .replace(Regex("""(?:Rs\.?|INR|₹)?\s*-?[\d,]+\.\d{2}"""), "")
                    .replace(Regex("""\b(?:Dr|Cr|DR|CR)\b"""), "")
                    .replace(Regex("""\s+"""), " ")
                    .trim()

                if (desc.isBlank() || desc.length < 3) {
                    desc = "Statement Transaction"
                }

                val refId = StatementParserUtils.extractReferenceId(trimmed)

                results.add(
                    ParsedStatementRow(
                        date = timestamp,
                        type = type,
                        amount = amount,
                        description = desc,
                        referenceId = refId,
                        balanceAfter = balance
                    )
                )
            }
        }

        return results
    }

    private fun isHeaderLine(line: String): Boolean {
        val l = line.lowercase()
        return l.contains("page ") ||
                (l.contains("date") && l.contains("balance")) ||
                (l.contains("narration") && l.contains("amount")) ||
                (l.contains("statement of account") || l.contains("account summary"))
    }
}

/**
 * Statement Parser Router
 * Chooses the appropriate parser for the given BankType and safely returns emptyList() if no match.
 */
object StatementParserRouter {
    fun parse(bank: BankType, text: String): List<ParsedStatementRow> {
        if (text.isBlank()) return emptyList()

        return try {
            val primaryParser: BankStatementParser = when (bank) {
                BankType.SBI -> SbiStatementParser
                BankType.HDFC -> HdfcStatementParser
                BankType.ICICI -> IciciStatementParser
                BankType.AXIS -> AxisStatementParser
                BankType.KOTAK -> KotakStatementParser
                BankType.BOB,
                BankType.IDFC,
                BankType.CITI,
                BankType.PNB,
                BankType.CASH,
                BankType.OTHER -> GenericStatementParser
            }

            val parsed = primaryParser.parse(text)
            if (parsed.isNotEmpty()) {
                parsed
            } else {
                // Fallback to GenericStatementParser if bank-specific parser didn't match rows
                if (primaryParser !is GenericStatementParser) {
                    GenericStatementParser.parse(text)
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
