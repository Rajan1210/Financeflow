package com.example.data.sms

import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.BankType
import com.example.domain.model.SmsDetectionType
import com.example.domain.model.TransactionCategory
import com.example.domain.model.TransactionType
import java.security.MessageDigest
import java.util.Locale
import java.util.regex.Pattern

data class ParsedSmsResult(
    val isValidTransaction: Boolean,
    val transaction: TransactionEntity? = null,
    val detectionType: SmsDetectionType = SmsDetectionType.UNKNOWN,
    val reasonIfNotValid: String? = null
)

object SmsTransactionParser {

    private val OTP_PATTERNS = listOf(
        "\\botp\\b",
        "one[- ]time password",
        "verification code",
        "secret code",
        "do not share",
        "is your code",
        "security code"
    ).map { Pattern.compile(it, Pattern.CASE_INSENSITIVE) }

    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:rs\.?|inr|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)|([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:debited|credited|spent|withdrawn)""",
        Pattern.CASE_INSENSITIVE
    )

    private val ACCOUNT_PATTERN = Pattern.compile(
        """(?:a/c|acct|account|card)\s*(?:no\.?)?\s*(?:ending\s*)?(?:[xX*]+)?([0-9]{3,4})""",
        Pattern.CASE_INSENSITIVE
    )

    private val UPI_REF_PATTERN = Pattern.compile(
        """(?:upi ref(?: no)?\.?|ref(?: no)?\.?|rrn)\s*[:\s]?\s*([0-9a-zA-Z]+)""",
        Pattern.CASE_INSENSITIVE
    )

    private val MERCHANT_TO_PATTERN = Pattern.compile(
        """(?:to|towards|at|for|vpa|info)\s+([A-Za-z0-9\s._&-]{2,30}?)(?:\s+on|\s+ref|\s+avail|\s+bal|\s+upi|\.|\,|$)""",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(smsBody: String, sender: String = "", timestamp: Long = System.currentTimeMillis()): ParsedSmsResult {
        val cleanBody = smsBody.trim()

        // 1. Check if it's an OTP or purely promotional message
        if (isOtpOrPromo(cleanBody)) {
            return ParsedSmsResult(
                isValidTransaction = false,
                reasonIfNotValid = "Ignored: OTP / Security / Promotional message"
            )
        }

        // 2. Extract Amount
        val amount = extractAmount(cleanBody)
        if (amount == null || amount <= 0.0) {
            return ParsedSmsResult(
                isValidTransaction = false,
                reasonIfNotValid = "No valid transaction amount found in SMS"
            )
        }

        // 3. Determine Transaction Type & Detection Type
        val lowerBody = cleanBody.lowercase(Locale.ROOT)
        val (txType, detectionType) = detectTransactionType(lowerBody)

        if (txType == null) {
            return ParsedSmsResult(
                isValidTransaction = false,
                reasonIfNotValid = "Could not identify debit/credit transaction type"
            )
        }

        // 4. Extract Bank info
        val bank = detectBank(sender, cleanBody)

        // 5. Extract Account/Card snippet
        val accountSnippet = extractAccountSnippet(cleanBody)

        // 6. Extract Reference ID
        val refId = extractReferenceId(cleanBody)

        // 7. Extract Merchant & Auto Categorize
        val rawMerchant = extractMerchant(cleanBody, detectionType)
        val category = autoCategorize(cleanBody, rawMerchant, detectionType, txType)

        // 8. Generate unique hash for deduplication
        val hash = generateSmsHash(amount, txType, refId ?: rawMerchant, timestamp, bank)

        val transaction = TransactionEntity(
            amount = amount,
            type = txType,
            category = category,
            merchant = rawMerchant,
            description = cleanBody.take(150),
            date = timestamp,
            bank = bank,
            referenceId = refId ?: ("SMS-" + hash.take(8)),
            smsBody = cleanBody,
            isFromSms = true,
            smsHash = hash
        )

        return ParsedSmsResult(
            isValidTransaction = true,
            transaction = transaction,
            detectionType = detectionType
        )
    }

    private fun isOtpOrPromo(body: String): Boolean {
        // If it explicitly asks for OTP/code and DOES NOT contain debited/credited
        val hasOtpPattern = OTP_PATTERNS.any { it.matcher(body).find() }
        val lower = body.lowercase(Locale.ROOT)
        val hasFinancialKeywords = lower.contains("debited") || lower.contains("credited") || lower.contains("spent") || lower.contains("withdrawn")

        if (hasOtpPattern && !hasFinancialKeywords) {
            return true
        }

        // Promo messages without any monetary transaction
        if ((lower.contains("pre-approved") || lower.contains("apply now") || lower.contains("congratulations! you are eligible")) &&
            !hasFinancialKeywords
        ) {
            return true
        }

        return false
    }

    private fun extractAmount(body: String): Double? {
        val matcher = AMOUNT_PATTERN.matcher(body)
        while (matcher.find()) {
            val group1 = matcher.group(1)
            val group2 = matcher.group(2)
            val amountStr = (group1 ?: group2)?.replace(",", "")?.trim()
            val parsed = amountStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0.0) {
                return parsed
            }
        }
        return null
    }

    private fun detectTransactionType(lowerBody: String): Pair<TransactionType?, SmsDetectionType> {
        return when {
            lowerBody.contains("salary") && (lowerBody.contains("credited") || lowerBody.contains("deposited")) ->
                Pair(TransactionType.INCOME, SmsDetectionType.SALARY_CREDIT)

            lowerBody.contains("refund") && (lowerBody.contains("credited") || lowerBody.contains("reversed")) ->
                Pair(TransactionType.INCOME, SmsDetectionType.REFUND)

            lowerBody.contains("interest") && lowerBody.contains("credited") ->
                Pair(TransactionType.INCOME, SmsDetectionType.INTEREST_CREDIT)

            lowerBody.contains("atm") && (lowerBody.contains("withdrawn") || lowerBody.contains("withdrawal") || lowerBody.contains("debited")) ->
                Pair(TransactionType.EXPENSE, SmsDetectionType.ATM_WITHDRAWAL)

            lowerBody.contains("emi") && (lowerBody.contains("debited") || lowerBody.contains("deducted") || lowerBody.contains("paid")) ->
                Pair(TransactionType.EXPENSE, SmsDetectionType.EMI_DEBIT)

            (lowerBody.contains("credit card") || lowerBody.contains("card ending")) && (lowerBody.contains("spent") || lowerBody.contains("debited") || lowerBody.contains("used")) ->
                Pair(TransactionType.EXPENSE, SmsDetectionType.CREDIT_CARD_SPEND)

            (lowerBody.contains("upi") || lowerBody.contains("vpa")) && (lowerBody.contains("debited") || lowerBody.contains("sent") || lowerBody.contains("paid")) ->
                Pair(TransactionType.EXPENSE, SmsDetectionType.UPI_PAYMENT)

            lowerBody.contains("credited") || lowerBody.contains("received") || lowerBody.contains("deposited") ->
                Pair(TransactionType.INCOME, SmsDetectionType.BANK_CREDIT)

            lowerBody.contains("debited") || lowerBody.contains("spent") || lowerBody.contains("paid") || lowerBody.contains("deducted") || lowerBody.contains("transferred to") ->
                Pair(TransactionType.EXPENSE, SmsDetectionType.BANK_DEBIT)

            else -> Pair(null, SmsDetectionType.UNKNOWN)
        }
    }

    private fun detectBank(sender: String, body: String): String {
        val s = (sender + " " + body).uppercase(Locale.ROOT)
        return when {
            s.contains("HDFC") -> "HDFC"
            s.contains("SBI") || s.contains("STATE BANK") -> "SBI"
            s.contains("ICICI") -> "ICICI"
            s.contains("AXIS") -> "AXIS"
            s.contains("KOTAK") -> "KOTAK"
            s.contains("PNB") || s.contains("PUNJAB") -> "PNB"
            s.contains("PAYTM") -> "Paytm"
            s.contains("BOB") || s.contains("BARODA") -> "Bank of Baroda"
            s.contains("IDFC") -> "IDFC First"
            s.contains("CITI") -> "Citibank"
            else -> "Bank Account"
        }
    }

    private fun extractAccountSnippet(body: String): String? {
        val matcher = ACCOUNT_PATTERN.matcher(body)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    private fun extractReferenceId(body: String): String? {
        val matcher = UPI_REF_PATTERN.matcher(body)
        if (matcher.find()) {
            return matcher.group(1)?.trim()
        }
        return null
    }

    private fun extractMerchant(body: String, detectionType: SmsDetectionType): String {
        val matcher = MERCHANT_TO_PATTERN.matcher(body)
        if (matcher.find()) {
            val raw = matcher.group(1)?.trim() ?: ""
            val cleaned = cleanMerchantString(raw)
            if (cleaned.isNotBlank() && cleaned.length >= 2) {
                return cleaned
            }
        }

        return when (detectionType) {
            SmsDetectionType.ATM_WITHDRAWAL -> "ATM Cash Out"
            SmsDetectionType.SALARY_CREDIT -> "Employer Salary"
            SmsDetectionType.EMI_DEBIT -> "Loan / EMI Payment"
            SmsDetectionType.REFUND -> "Merchant Refund"
            SmsDetectionType.INTEREST_CREDIT -> "Bank Interest"
            SmsDetectionType.CREDIT_CARD_SPEND -> "Card Purchase"
            SmsDetectionType.UPI_PAYMENT -> "UPI Transfer"
            else -> "Bank Transaction"
        }
    }

    private fun cleanMerchantString(raw: String): String {
        return raw.replace(Regex("(?i)^(vpa|the|a|an|m/s|dr|mr)\\s+"), "")
            .replace(Regex("[^a-zA-Z0-9 &._-]"), "")
            .trim()
            .take(25)
    }

    private fun autoCategorize(
        body: String,
        merchant: String,
        detectionType: SmsDetectionType,
        txType: TransactionType
    ): TransactionCategory {
        if (detectionType == SmsDetectionType.SALARY_CREDIT) return TransactionCategory.SALARY
        if (detectionType == SmsDetectionType.ATM_WITHDRAWAL) return TransactionCategory.TRANSFER
        if (detectionType == SmsDetectionType.INTEREST_CREDIT) return TransactionCategory.INVESTMENT
        if (txType == TransactionType.INCOME) return TransactionCategory.OTHERS

        val text = (body + " " + merchant).lowercase(Locale.ROOT)

        return when {
            text.containsAny("swiggy", "zomato", "restaurant", "cafe", "mcdonald", "domino", "burger", "pizza", "starbucks", "kfc", "dine", "food", "tea", "coffee", "baker") ->
                TransactionCategory.FOOD

            text.containsAny("amazon", "flipkart", "myntra", "ajio", "meesho", "zara", "decathlon", "retail", "mart", "supermarket", "grocer", "blinkit", "zepto", "instamart", "bigbasket") ->
                TransactionCategory.SHOPPING

            text.containsAny("uber", "ola", "rapido", "irctc", "makemytrip", "flight", "indigo", "airindia", "metro", "toll", "fastag", "bus", "train", "redbus") ->
                TransactionCategory.TRAVEL

            text.containsAny("petrol", "diesel", "fuel", "iocl", "bpcl", "hpcl", "shell", "cng", "gas station") ->
                TransactionCategory.FUEL

            text.containsAny("apollo", "pharmeasy", "1mg", "hospital", "clinic", "pharmacy", "medplus", "medical", "doctor", "health") ->
                TransactionCategory.MEDICAL

            text.containsAny("electricity", "bescom", "tneb", "water", "gas bill", "billdesk", "power", "utility", "broadband", "act corp") ->
                TransactionCategory.BILLS

            text.containsAny("airtel", "jio", "vodafone", "vi ", "prepaid", "dth", "recharge", "postpaid") ->
                TransactionCategory.RECHARGE

            text.containsAny("rent", "nobroker", "landlord", "flat rent", "housing") ->
                TransactionCategory.RENT

            text.containsAny("netflix", "spotify", "hotstar", "pvr", "inox", "cinema", "movie", "bookmyshow", "prime video", "entertainment", "game", "steam") ->
                TransactionCategory.ENTERTAINMENT

            text.containsAny("groww", "zerodha", "coin", "kuvera", "mutual fund", "sip", "stocks", "upstox", "indmoney", "smallcase", "gold") ->
                TransactionCategory.INVESTMENT

            text.containsAny("emi", "loan", "bajaj finserv", "credit card payment", "hdfc bank card") ->
                TransactionCategory.BILLS

            text.containsAny("transfer", "sent to", "paid to friend", "family", "self") ->
                TransactionCategory.TRANSFER

            else -> TransactionCategory.OTHERS
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }

    fun generateSmsHash(
        amount: Double,
        type: TransactionType,
        refOrMerchant: String,
        timestamp: Long,
        bank: String
    ): String {
        // Approximate time window (rounded to nearest 10 minutes) so identical retries produce the exact same hash
        val timeBucket = timestamp / 600000L
        val raw = "${amount}_${type.name}_${refOrMerchant.lowercase(Locale.ROOT).trim()}_${timeBucket}_${bank}"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
