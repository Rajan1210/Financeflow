package com.example.data.sms

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SmsSyncReport(
    val totalSmsRead: Int,
    val financialSmsFound: Int,
    val newTransactionsImported: Int,
    val duplicateSkipped: Int,
    val error: String? = null
)

object SmsReaderHelper {

    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun readAndImportSms(
        context: Context,
        transactionDao: TransactionDao,
        maxCount: Int = 150
    ): SmsSyncReport = withContext(Dispatchers.IO) {
        if (!hasSmsPermission(context)) {
            return@withContext SmsSyncReport(0, 0, 0, 0, "READ_SMS permission not granted")
        }

        var totalRead = 0
        var financialFound = 0
        var imported = 0
        var skipped = 0

        try {
            val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )

            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (it.moveToNext() && totalRead < maxCount) {
                    totalRead++
                    val body = it.getString(bodyIdx) ?: ""
                    val sender = it.getString(addressIdx) ?: ""
                    val date = it.getLong(dateIdx)

                    val parseResult = SmsTransactionParser.parse(body, sender, date)
                    if (parseResult.isValidTransaction && parseResult.transaction != null) {
                        financialFound++
                        val tx = parseResult.transaction
                        val hash = tx.smsHash ?: ""
                        val existingCount = transactionDao.getCountBySmsHash(hash)
                        if (existingCount == 0) {
                            val id = transactionDao.insertTransaction(tx)
                            if (id > 0) {
                                imported++
                            } else {
                                skipped++
                            }
                        } else {
                            skipped++
                        }
                    }
                }
            }

            SmsSyncReport(
                totalSmsRead = totalRead,
                financialSmsFound = financialFound,
                newTransactionsImported = imported,
                duplicateSkipped = skipped
            )
        } catch (e: Exception) {
            SmsSyncReport(
                totalSmsRead = totalRead,
                financialSmsFound = financialFound,
                newTransactionsImported = imported,
                duplicateSkipped = skipped,
                error = e.localizedMessage ?: "Failed reading SMS"
            )
        }
    }

    // Realistic Banking SMS Presets for testing & instant sync simulation
    fun getPresetSmsList(): List<Pair<String, String>> {
        return listOf(
            "VK-HDFCBK" to "Rs. 750.00 debited from HDFC Bank A/C XX3104 on 24-Sep-26 to Swiggy UPI Ref: 6291039481. Bal: Rs. 97,650.00",
            "VM-SBIINB" to "Your A/C ending XX9421 is debited for Rs 3200.00 on 23-Sep-26 towards Amazon India. Avail Bal: Rs 42,050.00",
            "AD-ICICIB" to "Transaction of INR 1,500.00 spent on ICICI Bank Credit Card ending 9201 at Indian Oil Petrol on 22-Sep-26. Avail Limit: INR 85,750.00",
            "AX-AXISBK" to "Rs 85,000.00 credited to Axis Bank A/C XX7712 on 01-Sep-26 by Acme Corp Tech Salary. Total Bal: Rs 1,12,400.00",
            "VM-KOTAKB" to "Rs. 2,000.00 withdrawn at ATM from Kotak Bank A/C XX6621 on 21-Sep-26. Available balance Rs. 18,300.00",
            "VK-HDFCBK" to "Refund of Rs 649.00 has been credited to your HDFC A/C XX3104 on 20-Sep-26 from Blinkit. Ref: RF992817",
            "VM-SBIINB" to "Interest credited Rs 480.00 in SBI A/C XX9421 for Q2. Updated Bal: Rs 42,530.00",
            "AD-ICICIB" to "Your loan EMI of Rs. 14,500.00 is debited from ICICI A/C XX5520 on 05-Sep-26. Avail Bal: Rs 7,280.00",
            "VK-HDFCBK" to "Sent Rs. 4,500.00 to Zerodha Broking via UPI Ref 773910284 on 19-Sep-26 from HDFC Bank.",
            "AD-PROMO" to "Dear customer, your OTP for net banking login is 492104. Valid for 10 mins. Do not share OTP with anyone.",
            "VM-OFFER" to "Get flat 20% discount on credit card EMI purchases at Myntra this weekend! Click bit.ly/offers to apply."
        )
    }
}
