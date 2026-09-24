package com.example

import com.example.data.sms.SmsDetectionType
import com.example.data.sms.SmsTransactionParser
import com.example.domain.model.TransactionCategory
import com.example.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

class FinanceUnitTest {

    @Test
    fun testUpiDebitSmsParsing() {
        val sms = "Rs. 750.00 debited from HDFC Bank A/C XX3104 on 24-Sep-26 to Swiggy UPI Ref: 6291039481. Bal: Rs. 97,650.00"
        val parsed = SmsTransactionParser.parse(sms, "VK-HDFCBK")

        assertTrue("Should be recognized as a valid transaction", parsed.isValidTransaction)
        assertNotNull("Transaction entity should not be null", parsed.transaction)

        val tx = parsed.transaction!!
        assertEquals(750.0, tx.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(TransactionCategory.FOOD, tx.category)
        assertEquals("HDFC", tx.bank)
        assertTrue(tx.merchant.contains("Swiggy", ignoreCase = true))
        assertNotNull(tx.smsHash)
    }

    @Test
    fun testSalaryCreditSmsParsing() {
        val sms = "Rs 85,000.00 credited to Axis Bank A/C XX7712 on 01-Sep-26 by Acme Corp Tech Salary. Total Bal: Rs 1,12,400.00"
        val parsed = SmsTransactionParser.parse(sms, "AX-AXISBK")

        assertTrue(parsed.isValidTransaction)
        val tx = parsed.transaction!!
        assertEquals(85000.0, tx.amount, 0.01)
        assertEquals(TransactionType.INCOME, tx.type)
        assertEquals(TransactionCategory.SALARY, tx.category)
        assertEquals(SmsDetectionType.SALARY_CREDIT, parsed.detectionType)
    }

    @Test
    fun testCreditCardSpendParsing() {
        val sms = "Transaction of INR 1,500.00 spent on ICICI Bank Credit Card ending 9201 at Indian Oil Petrol on 22-Sep-26."
        val parsed = SmsTransactionParser.parse(sms, "AD-ICICIB")

        assertTrue(parsed.isValidTransaction)
        val tx = parsed.transaction!!
        assertEquals(1500.0, tx.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(TransactionCategory.FUEL, tx.category)
    }

    @Test
    fun testAtmWithdrawalParsing() {
        val sms = "Rs. 2,000.00 withdrawn at ATM from Kotak Bank A/C XX6621 on 21-Sep-26."
        val parsed = SmsTransactionParser.parse(sms, "VM-KOTAKB")

        assertTrue(parsed.isValidTransaction)
        val tx = parsed.transaction!!
        assertEquals(2000.0, tx.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(TransactionCategory.TRANSFER, tx.category)
        assertEquals(SmsDetectionType.ATM_WITHDRAWAL, parsed.detectionType)
    }

    @Test
    fun testOtpMessageIsIgnored() {
        val otpSms = "Dear customer, your OTP for net banking login is 492104. Valid for 10 mins. Do not share OTP with anyone."
        val parsed = SmsTransactionParser.parse(otpSms, "AD-HDFCBK")

        assertFalse("OTP message should be ignored", parsed.isValidTransaction)
        assertNull(parsed.transaction)
    }

    @Test
    fun testPromotionalMessageIsIgnored() {
        val promoSms = "Get flat 20% discount on credit card EMI purchases at Myntra this weekend! Click bit.ly/offers to apply."
        val parsed = SmsTransactionParser.parse(promoSms, "VM-OFFER")

        assertFalse("Promotional message should be ignored", parsed.isValidTransaction)
    }

    @Test
    fun testSmsHashConsistencyForDeduplication() {
        val amount = 500.0
        val type = TransactionType.EXPENSE
        val ref = "REF12345"
        val time = 1727164800000L
        val bank = "SBI"

        val hash1 = SmsTransactionParser.generateSmsHash(amount, type, ref, time, bank)
        val hash2 = SmsTransactionParser.generateSmsHash(amount, type, ref, time + 5000L, bank) // within same time bucket

        assertEquals("Hashes within 10-minute duplicate window should be identical", hash1, hash2)
    }
}
