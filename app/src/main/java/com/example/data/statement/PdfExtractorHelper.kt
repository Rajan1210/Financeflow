package com.example.data.statement

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream

sealed class PdfExtractResult {
    data class Success(val text: String, val pageCount: Int) : PdfExtractResult()
    data class PasswordRequired(val isIncorrectAttempt: Boolean = false) : PdfExtractResult()
    data class Error(val message: String) : PdfExtractResult()
}

object PdfExtractorHelper {

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            try {
                PDFBoxResourceLoader.init(context.applicationContext)
                isInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun readBytesFromUri(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun extractText(
        context: Context,
        pdfBytes: ByteArray,
        password: String? = null
    ): PdfExtractResult = withContext(Dispatchers.IO) {
        init(context)
        var document: PDDocument? = null
        try {
            document = if (password.isNullOrEmpty()) {
                PDDocument.load(ByteArrayInputStream(pdfBytes))
            } else {
                PDDocument.load(ByteArrayInputStream(pdfBytes), password)
            }

            if (document.isEncrypted) {
                // If it is encrypted and no password or wrong decryption
                return@withContext PdfExtractResult.PasswordRequired(isIncorrectAttempt = !password.isNullOrEmpty())
            }

            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            val fullText = stripper.getText(document)
            val pages = document.numberOfPages
            PdfExtractResult.Success(text = fullText, pageCount = pages)
        } catch (e: InvalidPasswordException) {
            PdfExtractResult.PasswordRequired(isIncorrectAttempt = !password.isNullOrEmpty())
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("password", ignoreCase = true) || msg.contains("encrypted", ignoreCase = true)) {
                PdfExtractResult.PasswordRequired(isIncorrectAttempt = !password.isNullOrEmpty())
            } else {
                PdfExtractResult.Error("Failed to open PDF: ${e.localizedMessage ?: "Unknown error"}")
            }
        } finally {
            try {
                document?.close()
            } catch (e: Exception) {
                // ignore close error
            }
        }
    }
}
