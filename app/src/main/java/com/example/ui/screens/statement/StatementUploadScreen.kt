package com.example.ui.screens.statement

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountEntity
import com.example.data.statement.PdfExtractResult
import com.example.data.statement.PdfExtractorHelper
import com.example.domain.model.BankType
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

enum class UploadStep {
    SELECT_BANK,
    SELECT_ACCOUNT,
    PICK_AND_EXTRACT,
    EXTRACTED_SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementUploadScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onTextExtracted: ((bank: BankType, account: AccountEntity, extractedText: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    var currentStep by remember { mutableStateOf(UploadStep.SELECT_BANK) }
    var selectedBank by remember { mutableStateOf<BankType?>(null) }
    var selectedAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedPdfFileName by remember { mutableStateOf<String?>(null) }

    var isProcessingPdf by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Password Dialog State
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Inline Account Creation Dialog
    var showCreateAccountDialog by remember { mutableStateOf(false) }

    // Extracted Output info
    var extractedTextResult by remember { mutableStateOf<String?>(null) }
    var extractedPageCount by remember { mutableIntStateOf(0) }

    // Function to run extraction
    fun processExtraction(bytes: ByteArray, password: String? = null) {
        coroutineScope.launch {
            isProcessingPdf = true
            errorMessage = null
            val result = PdfExtractorHelper.extractText(context, bytes, password)
            isProcessingPdf = false

            when (result) {
                is PdfExtractResult.Success -> {
                    showPasswordDialog = false
                    passwordInput = ""
                    passwordError = null
                    extractedTextResult = result.text
                    extractedPageCount = result.pageCount
                    currentStep = UploadStep.EXTRACTED_SUCCESS

                    val bank = selectedBank ?: BankType.OTHER
                    val account = selectedAccount
                    if (account != null && onTextExtracted != null) {
                        onTextExtracted(bank, account, result.text)
                    }
                }
                is PdfExtractResult.PasswordRequired -> {
                    showPasswordDialog = true
                    if (result.isIncorrectAttempt) {
                        passwordError = "Incorrect password. Most banks use your PAN, Date of Birth (DDMMYYYY), or mobile number."
                    } else {
                        passwordError = null
                    }
                }
                is PdfExtractResult.Error -> {
                    errorMessage = result.message
                }
            }
        }
    }

    // PDF SAF Document Picker
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPdfUri = uri
            // Read file name if possible
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        selectedPdfFileName = it.getString(nameIndex)
                    }
                }
            }
            coroutineScope.launch {
                isProcessingPdf = true
                val bytes = PdfExtractorHelper.readBytesFromUri(context, uri)
                selectedPdfBytes = bytes
                if (bytes == null) {
                    isProcessingPdf = false
                    errorMessage = "Could not read selected PDF file."
                } else {
                    processExtraction(bytes)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Bank Statement",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (currentStep) {
                                UploadStep.SELECT_BANK -> onBack()
                                UploadStep.SELECT_ACCOUNT -> currentStep = UploadStep.SELECT_BANK
                                UploadStep.PICK_AND_EXTRACT -> currentStep = UploadStep.SELECT_ACCOUNT
                                UploadStep.EXTRACTED_SUCCESS -> onBack()
                            }
                        },
                        modifier = Modifier.testTag("statement_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Step Progress Indicator
            StepProgressBar(currentStep = currentStep)

            Spacer(modifier = Modifier.height(8.dp))

            errorMessage?.let { err ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = currentStep,
                label = "statement_step_anim"
            ) { step ->
                when (step) {
                    UploadStep.SELECT_BANK -> {
                        BankSelectionView(
                            selectedBank = selectedBank,
                            onSelectBank = { bank ->
                                selectedBank = bank
                                // Auto-match existing account with chosen bank if available
                                val matched = accounts.firstOrNull { it.bank == bank }
                                if (matched != null) {
                                    selectedAccount = matched
                                }
                                currentStep = UploadStep.SELECT_ACCOUNT
                            }
                        )
                    }
                    UploadStep.SELECT_ACCOUNT -> {
                        AccountSelectionView(
                            bank = selectedBank ?: BankType.OTHER,
                            accounts = accounts,
                            selectedAccount = selectedAccount,
                            onSelectAccount = { acc ->
                                selectedAccount = acc
                                currentStep = UploadStep.PICK_AND_EXTRACT
                            },
                            onCreateAccountClick = {
                                showCreateAccountDialog = true
                            }
                        )
                    }
                    UploadStep.PICK_AND_EXTRACT -> {
                        PdfUploadView(
                            bank = selectedBank ?: BankType.OTHER,
                            account = selectedAccount,
                            fileName = selectedPdfFileName,
                            isProcessing = isProcessingPdf,
                            onPickPdfClick = {
                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                            }
                        )
                    }
                    UploadStep.EXTRACTED_SUCCESS -> {
                        ExtractedSuccessView(
                            bank = selectedBank ?: BankType.OTHER,
                            account = selectedAccount,
                            pageCount = extractedPageCount,
                            charCount = extractedTextResult?.length ?: 0,
                            samplePreview = extractedTextResult?.take(500) ?: "",
                            onUploadAnother = {
                                selectedPdfBytes = null
                                selectedPdfFileName = null
                                extractedTextResult = null
                                currentStep = UploadStep.SELECT_BANK
                            }
                        )
                    }
                }
            }
        }
    }

    // Step 4: Password Dialog
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                isProcessingPdf = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Encrypted Document",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Password Protected PDF",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "This e-statement is encrypted. Enter the PDF password to decrypt and extract transactions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val hint = when (selectedBank) {
                        BankType.SBI -> "SBI Hint: Last 5 digits of registered mobile + DOB (DDMM)"
                        BankType.HDFC -> "HDFC Hint: Customer ID or PAN (in uppercase)"
                        BankType.ICICI -> "ICICI Hint: First 4 letters of name + DOB (DDMM)"
                        BankType.AXIS -> "Axis Hint: First 4 letters of name + DOB (DDMMYYYY)"
                        BankType.KOTAK -> "Kotak Hint: Customer CRN number"
                        else -> "Hint: Often your PAN in CAPS, Date of Birth (DDMMYYYY), or mobile number."
                    }

                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = null
                        },
                        label = { Text("PDF Password") },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        singleLine = true,
                        isError = passwordError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (passwordInput.isNotBlank() && selectedPdfBytes != null) {
                                    processExtraction(selectedPdfBytes!!, passwordInput)
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_password_input")
                    )

                    passwordError?.let { err ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordInput.isNotBlank() && selectedPdfBytes != null) {
                            processExtraction(selectedPdfBytes!!, passwordInput)
                        }
                    },
                    enabled = passwordInput.isNotBlank() && !isProcessingPdf,
                    modifier = Modifier.testTag("pdf_password_unlock_button")
                ) {
                    if (isProcessingPdf) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Unlock & Read")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        isProcessingPdf = false
                        passwordInput = ""
                        passwordError = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Step 2 Inline Dialog: Create New Account
    if (showCreateAccountDialog) {
        var accName by remember { mutableStateOf("${selectedBank?.displayName ?: "Bank"} Account") }
        var accNumber by remember { mutableStateOf("XX" + (1000..9999).random()) }
        var accBalanceText by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showCreateAccountDialog = false },
            title = { Text("Create New Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = accName,
                        onValueChange = { accName = it },
                        label = { Text("Account Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = accNumber,
                        onValueChange = { accNumber = it },
                        label = { Text("Account Number / Last 4 Digits") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = accBalanceText,
                        onValueChange = { accBalanceText = it },
                        label = { Text("Current Balance (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bal = accBalanceText.toDoubleOrNull() ?: 0.0
                        val bank = selectedBank ?: BankType.OTHER
                        viewModel.addAccount(
                            name = accName.ifBlank { "${bank.displayName} Account" },
                            bank = bank,
                            accountNumber = accNumber.ifBlank { "XX0000" },
                            balance = bal,
                            isCash = false
                        )
                        showCreateAccountDialog = false
                    }
                ) {
                    Text("Create Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StepProgressBar(currentStep: UploadStep) {
    val steps = listOf("1. Bank", "2. Account", "3. PDF File", "4. Extract")
    val activeIndex = when (currentStep) {
        UploadStep.SELECT_BANK -> 0
        UploadStep.SELECT_ACCOUNT -> 1
        UploadStep.PICK_AND_EXTRACT -> 2
        UploadStep.EXTRACTED_SUCCESS -> 3
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, title ->
            val isCurrent = index == activeIndex
            val isDone = index < activeIndex

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDone -> IncomeGreen
                                isCurrent -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Done",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = (index + 1).toString(),
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isCurrent || isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BankSelectionView(
    selectedBank: BankType?,
    onSelectBank: (BankType) -> Unit
) {
    val banks = listOf(
        BankType.SBI,
        BankType.HDFC,
        BankType.ICICI,
        BankType.AXIS,
        BankType.KOTAK,
        BankType.PNB,
        BankType.BOB,
        BankType.IDFC,
        BankType.CITI,
        BankType.OTHER
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Select Bank or Institution",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            text = "Choose your bank to apply the optimized e-statement layout parser.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(banks) { bank ->
                val isSelected = bank == selectedBank
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectBank(bank) }
                        .testTag("bank_option_${bank.name}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder().takeIf { !isSelected }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    when (bank) {
                                        BankType.SBI -> Color(0xFF0284C7)
                                        BankType.HDFC -> Color(0xFF1E3A8A)
                                        BankType.ICICI -> Color(0xFFB91C1C)
                                        BankType.AXIS -> Color(0xFF831843)
                                        BankType.KOTAK -> Color(0xFFDC2626)
                                        BankType.PNB -> Color(0xFFD97706)
                                        BankType.BOB -> Color(0xFFEA580C)
                                        BankType.IDFC -> Color(0xFF7C2D12)
                                        BankType.CITI -> Color(0xFF0369A1)
                                        else -> Color(0xFF4B5563)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = bank.shortCode.take(3),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bank.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (bank == BankType.OTHER) "Generic table & AI assisted fallback" else "Supported statement format",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Select",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSelectionView(
    bank: BankType,
    accounts: List<AccountEntity>,
    selectedAccount: AccountEntity?,
    onSelectAccount: (AccountEntity) -> Unit,
    onCreateAccountClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Select Account for ${bank.displayName}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            text = "Imported transactions and balance updates will be linked to this account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (accounts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No accounts found. Create your account to link this statement.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onCreateAccountClick) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Account")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(accounts) { acc ->
                    val isSelected = acc.id == selectedAccount?.id
                    val isMatchingBank = acc.bank == bank

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectAccount(acc) }
                            .testTag("account_option_${acc.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (acc.isCash) Icons.Filled.Payments else Icons.Filled.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (isMatchingBank) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(IncomeGreen.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "MATCH",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = IncomeGreen
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${acc.bank.displayName} • ${acc.accountNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Current Balance: ₹${acc.balance}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCreateAccountClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_new_account_inline_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Account Inline")
            }
        }
    }
}

@Composable
private fun PdfUploadView(
    bank: BankType,
    account: AccountEntity?,
    fileName: String?,
    isProcessing: Boolean,
    onPickPdfClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Import Target",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${bank.displayName} ➔ ${account?.name ?: "Selected Account"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                account?.let {
                    Text(
                        text = "Account: ${it.accountNumber} | Current Bal: ₹${it.balance}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PictureAsPdf,
                contentDescription = "PDF Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Select PDF Statement",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select your official bank or loan e-statement PDF.\nPassword-protected statements are fully supported.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (fileName != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isProcessing) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Opening PDF & extracting text with PDFBox...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Button(
                onClick = onPickPdfClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("select_pdf_file_btn")
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (fileName != null) "Choose Different PDF" else "Choose PDF Statement",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ExtractedSuccessView(
    bank: BankType,
    account: AccountEntity?,
    pageCount: Int,
    charCount: Int,
    samplePreview: String,
    onUploadAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(IncomeGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Success",
                tint = IncomeGreen,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PDF Extracted Successfully!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Extracted $pageCount pages ($charCount characters) for ${bank.displayName}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Statement Raw Text Sample:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = samplePreview.ifBlank { "No text content found." },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 15,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onUploadAnother,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload Another Statement")
        }
    }
}
