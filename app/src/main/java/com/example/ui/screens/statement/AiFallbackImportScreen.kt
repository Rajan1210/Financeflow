package com.example.ui.screens.statement

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.statement.ParsedStatementRow
import com.example.data.statement.PastedListParser
import com.example.data.statement.PastedParseResult
import com.example.data.statement.StatementParserUtils
import com.example.data.statement.UnparseableStatementLine
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiFallbackImportScreen(
    viewModel: FinanceViewModel,
    account: AccountEntity,
    rawExtractedText: String?,
    onBack: () -> Unit,
    onSuccessImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var pastedInput by remember { mutableStateOf("") }
    var parseResult by remember { mutableStateOf<PastedParseResult?>(null) }
    var selectedRows by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var duplicateCount by remember { mutableIntStateOf(0) }
    var isImporting by remember { mutableStateOf(false) }

    val fullPromptTemplate = remember(rawExtractedText) {
        val statementPart = if (!rawExtractedText.isNullOrBlank()) {
            rawExtractedText.trim()
        } else {
            "[paste your statement text or describe the screenshot here]"
        }
        """I have a bank/loan statement. Extract every transaction from it and output ONLY a list in this exact format, one line per transaction, nothing else (no headers, no explanation):

DATE|TYPE|AMOUNT|BANK_OR_LENDER|PAYEE_OR_MERCHANT|DESCRIPTION

Rules:
- DATE format: DD-MM-YYYY
- TYPE must be exactly DEBIT or CREDIT
- AMOUNT as plain number, no currency symbol, no commas (e.g. 1500.50)
- If any field cannot be determined, write NOT_FOUND in that field only
- Do not skip any transaction, even if some fields are NOT_FOUND
- Do not add totals, summaries, or any extra text before/after the list

Here is my statement:
$statementPart"""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI-Assisted Import", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "If automatic parsing failed, you can prompt any AI (ChatGPT, Claude, Gemini) with this format and paste the result below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Step 1: Prompt Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1. AI Prompt Template",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            FilledTonalButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("AI Statement Prompt", fullPromptTemplate)
                                    clipboard.setPrimaryClip(clip)
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("copy_ai_prompt_btn")
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Prompt", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = fullPromptTemplate,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                fontSize = 11.sp,
                                maxLines = 10
                            )
                        }
                    }
                }
            }

            // Step 2: Paste Input
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "2. Paste AI Output Here",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = pastedInput,
                            onValueChange = { pastedInput = it },
                            placeholder = { Text("e.g. 15-08-2023|DEBIT|450.00|HDFC|Swiggy|Food order") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 220.dp)
                                .testTag("ai_pasted_input"),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val result = PastedListParser.parse(pastedInput)
                                coroutineScope.launch {
                                    val (nonDups, dups) = viewModel.filterDuplicateStatementRows(result.validRows)
                                    parseResult = PastedParseResult(nonDups, result.unparseableLines)
                                    duplicateCount = dups
                                    selectedRows = nonDups.indices.toSet()
                                }
                            },
                            enabled = pastedInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parse_pasted_list_btn")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Parse Pasted List")
                        }
                    }
                }
            }

            // Parse Results
            val result = parseResult
            if (result != null) {
                if (duplicateCount > 0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$duplicateCount duplicate transaction(s) already exist and were skipped.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                if (result.validRows.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Parsed Transactions (${result.validRows.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = {
                                    selectedRows = if (selectedRows.size == result.validRows.size) emptySet() else result.validRows.indices.toSet()
                                }
                            ) {
                                Text(if (selectedRows.size == result.validRows.size) "Deselect All" else "Select All")
                            }
                        }
                    }

                    items(result.validRows.indices.toList()) { index ->
                        val row = result.validRows[index]
                        val isChecked = selectedRows.contains(index)
                        val category = StatementParserUtils.categorize(row.description)
                        val dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(row.date))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pasted_row_card_$index"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = if (isChecked) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedRows = if (checked) selectedRows + index else selectedRows - index
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = row.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = dateFormatted,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = category.displayName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (row.type == com.example.domain.model.TransactionType.EXPENSE) "-" else "+"}₹${String.format(Locale.getDefault(), "%,.2f", row.amount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (row.type == com.example.domain.model.TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                                    )
                                    Text(
                                        text = if (row.type == com.example.domain.model.TransactionType.EXPENSE) "DEBIT" else "CREDIT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (row.type == com.example.domain.model.TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                val rowsToImport = result.validRows.filterIndexed { idx, _ -> selectedRows.contains(idx) }
                                isImporting = true
                                viewModel.confirmStatementImport(account, rowsToImport) {
                                    isImporting = false
                                    onSuccessImport()
                                }
                            },
                            enabled = selectedRows.isNotEmpty() && !isImporting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("confirm_pasted_import_btn")
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Filled.Done, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add ${selectedRows.size} Transactions to ${account.name}")
                            }
                        }
                    }
                }

                // Unparseable Lines Section
                if (result.unparseableLines.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Could not import — check manually (${result.unparseableLines.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "The following lines could not be reliably parsed into transactions:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                result.unparseableLines.forEach { unp ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = unp.rawLine,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Reason: ${unp.reason}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
