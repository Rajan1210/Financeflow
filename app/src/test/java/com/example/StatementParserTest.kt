package com.example

import com.example.data.statement.StatementParserRouter
import com.example.domain.model.BankType
import com.example.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

class StatementParserTest {

    @Test
    fun testHdfcStatementParsing() {
        val hdfcText = """
            HDFC BANK STATEMENT OF ACCOUNT
            Date Narration Chq./Ref.No. Value Dt Withdrawal Amt. Deposit Amt. Closing Balance
            24/09/2026 UPI-SWIGGY-BANGALORE-6291039481 6291039481 24/09/2026 750.00 97,650.00
            25/09/2026 SALARY ACME CORP TECH SAL/2026 8892104 25/09/2026 85,000.00 1,82,650.00
        """.trimIndent()

        val rows = StatementParserRouter.parse(BankType.HDFC, hdfcText)
        assertEquals(2, rows.size)

        val r1 = rows[0]
        assertEquals(750.0, r1.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, r1.type)
        assertEquals(97650.0, r1.balanceAfter ?: 0.0, 0.01)

        val r2 = rows[1]
        assertEquals(85000.0, r2.amount, 0.01)
        assertEquals(TransactionType.INCOME, r2.type)
        assertEquals(182650.0, r2.balanceAfter ?: 0.0, 0.01)
    }

    @Test
    fun testSbiStatementParsing() {
        val sbiText = """
            STATE BANK OF INDIA
            Txn Date Value Date Description Ref No./Cheque No. Debit Credit Balance
            24-Sep-2026 24-Sep-2026 TO SWIGGY UPI/6291039481 6291039481 450.00 45,250.00
            01-Sep-2026 01-Sep-2026 BY SALARY TECH CORP UPI/991204 85,000.00 1,30,250.00
        """.trimIndent()

        val rows = StatementParserRouter.parse(BankType.SBI, sbiText)
        assertEquals(2, rows.size)

        assertEquals(450.0, rows[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, rows[0].type)

        assertEquals(85000.0, rows[1].amount, 0.01)
        assertEquals(TransactionType.INCOME, rows[1].type)
    }

    @Test
    fun testEmptyOrInvalidTextReturnsEmptyListNeverThrows() {
        val emptyRows = StatementParserRouter.parse(BankType.HDFC, "")
        assertTrue(emptyRows.isEmpty())

        val garbageRows = StatementParserRouter.parse(BankType.OTHER, "Hello world, this is a random document without any transactions")
        assertTrue(garbageRows.isEmpty())
    }

    @Test
    fun testPastedListParsing() {
        val samplePasted = """
            DATE|TYPE|AMOUNT|BANK_OR_LENDER|PAYEE_OR_MERCHANT|DESCRIPTION
            15-08-2026|DEBIT|450.00|HDFC|Swiggy|Dinner delivery
            16-08-2026|CREDIT|85000.00|HDFC|Acme Tech|Monthly Salary
            INVALID_LINE_WITHOUT_PIPES
            17-08-2026|DEBIT|NOT_FOUND|SBI|Store|Shopping
        """.trimIndent()

        val result = com.example.data.statement.PastedListParser.parse(samplePasted)
        assertEquals(2, result.validRows.size)
        assertEquals(2, result.unparseableLines.size)

        assertEquals(450.0, result.validRows[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result.validRows[0].type)

        assertEquals(85000.0, result.validRows[1].amount, 0.01)
        assertEquals(TransactionType.INCOME, result.validRows[1].type)
    }
}
