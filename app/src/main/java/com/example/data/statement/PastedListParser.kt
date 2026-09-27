package com.example.data.statement

import com.example.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

data class PastedParseResult(
    val validRows: List<ParsedStatementRow>,
    val unparseableLines: List<UnparseableStatementLine>
)

data class UnparseableStatementLine(
    val rawLine: String,
    val reason: String
)

object PastedListParser {
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).apply {
        isLenient = false
    }

    fun parse(pastedText: String): PastedParseResult {
        val lines = pastedText.lines()
        val validRows = mutableListOf<ParsedStatementRow>()
        val unparseable = mutableListOf<UnparseableStatementLine>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            // Skip headers or prompt echoes if user copied them
            if (line.startsWith("DATE|TYPE|AMOUNT", ignoreCase = true) ||
                line.startsWith("Rules:", ignoreCase = true) ||
                line.startsWith("Here is my statement:", ignoreCase = true) ||
                line.startsWith("I have a bank", ignoreCase = true)) {
                continue
            }

            val parts = line.split("|").map { it.trim() }
            if (parts.size != 6) {
                unparseable.add(
                    UnparseableStatementLine(
                        rawLine = line,
                        reason = "Expected 6 fields separated by '|', found ${parts.size}"
                    )
                )
                continue
            }

            val dateStr = parts[0]
            val typeStr = parts[1].uppercase(Locale.ENGLISH)
            val amountStr = parts[2].replace(",", "").trim()
            val bankOrLender = parts[3]
            val payeeOrMerchant = parts[4]
            val description = parts[5]

            // Validate TYPE
            val type = when (typeStr) {
                "DEBIT" -> TransactionType.EXPENSE
                "CREDIT" -> TransactionType.INCOME
                else -> null
            }
            if (type == null) {
                unparseable.add(
                    UnparseableStatementLine(
                        rawLine = line,
                        reason = "Type must be exactly DEBIT or CREDIT (found: '$typeStr')"
                    )
                )
                continue
            }

            // Validate AMOUNT
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amountStr.equals("NOT_FOUND", ignoreCase = true) || amount <= 0.0) {
                unparseable.add(
                    UnparseableStatementLine(
                        rawLine = line,
                        reason = "Invalid amount: '$amountStr'"
                    )
                )
                continue
            }

            // Validate DATE
            val parsedDate = try {
                dateFormat.parse(dateStr)?.time
            } catch (e: Exception) {
                null
            }
            if (parsedDate == null || dateStr.equals("NOT_FOUND", ignoreCase = true)) {
                unparseable.add(
                    UnparseableStatementLine(
                        rawLine = line,
                        reason = "Invalid date format: '$dateStr' (must be DD-MM-YYYY)"
                    )
                )
                continue
            }

            val resolvedMerchant = if (payeeOrMerchant.isNotBlank() && !payeeOrMerchant.equals("NOT_FOUND", ignoreCase = true)) {
                payeeOrMerchant
            } else if (description.isNotBlank() && !description.equals("NOT_FOUND", ignoreCase = true)) {
                description
            } else {
                bankOrLender
            }

            val fullDescription = buildString {
                append(resolvedMerchant)
                if (description.isNotBlank() && !description.equals("NOT_FOUND", ignoreCase = true) && description != resolvedMerchant) {
                    append(" - ").append(description)
                }
                if (bankOrLender.isNotBlank() && !bankOrLender.equals("NOT_FOUND", ignoreCase = true)) {
                    append(" (").append(bankOrLender).append(")")
                }
            }

            validRows.add(
                ParsedStatementRow(
                    date = parsedDate,
                    type = type,
                    amount = amount,
                    description = fullDescription,
                    referenceId = null,
                    balanceAfter = null
                )
            )
        }

        return PastedParseResult(validRows, unparseable)
    }
}
