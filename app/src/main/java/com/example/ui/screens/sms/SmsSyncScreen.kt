package com.example.ui.screens.sms

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.sms.ParsedSmsResult
import com.example.data.sms.SmsReaderHelper
import com.example.data.sms.SmsTransactionParser
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSyncScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncReport by viewModel.lastSyncReport.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currency = userProfile?.currencySymbol ?: "₹"

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.syncSmsInbox(context)
        }
    }

    var customSmsInput by remember { mutableStateOf("") }
    var customSenderInput by remember { mutableStateOf("VK-HDFCBK") }
    var testParseResult by remember { mutableStateOf<ParsedSmsResult?>(null) }

    val presets = remember { SmsReaderHelper.getPresetSmsList() }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("sms_sync_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "SMS Transaction Engine", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Engine Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Automated Bank Tracking",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Auto-detects UPI, Cards, Net Banking & Salary",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "FinPulse securely scans incoming bank SMS messages completely offline on your device. OTPs and spam messages are strictly ignored. Transactions are deduplicated with cryptographic hashing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Permission and Sync Button
                        if (!hasPermission) {
                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.READ_SMS)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("request_sms_perm_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(imageVector = Icons.Filled.Security, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Grant SMS Permission & Sync")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.syncSmsInbox(context) },
                                enabled = !isSyncing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sync_inbox_btn")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scanning SMS Inbox...")
                                } else {
                                    Icon(imageVector = Icons.Filled.Sync, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sync Inbox Now")
                                }
                            }
                        }

                        // Last Sync report summary if available
                        syncReport?.let { report ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Sync Summary",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Scanned: ${report.totalSmsRead} SMS | Financial: ${report.financialSmsFound} | Imported: ${report.newTransactionsImported} | Skipped: ${report.duplicateSkipped}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (report.error != null) {
                                        Text(
                                            text = "Notice: ${report.error}",
                                            fontSize = 11.sp,
                                            color = WarningAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Bank SMS Simulator / Testing Playground
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SMS Parser Simulator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { viewModel.importPresetSmsList() },
                                colors = ButtonDefaults.buttonColors(containerColor = TealDark),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Import All Presets", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Test how any bank SMS is parsed in real time. Choose from realistic presets or type your own SMS below:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Preset chips
                        Text(
                            text = "Quick Presets:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        presets.take(5).forEach { (sender, sample) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        customSenderInput = sender
                                        customSmsInput = sample
                                        testParseResult = SmsTransactionParser.parse(sample, sender)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = sender,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sample,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom SMS test field
                        OutlinedTextField(
                            value = customSenderInput,
                            onValueChange = { customSenderInput = it },
                            label = { Text("Sender Header (e.g. VK-HDFCBK, SBIINB)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customSmsInput,
                            onValueChange = {
                                customSmsInput = it
                                testParseResult = if (it.isNotBlank()) {
                                    SmsTransactionParser.parse(it, customSenderInput)
                                } else null
                            },
                            label = { Text("Paste or type Bank SMS") },
                            placeholder = { Text("e.g. Rs 500 debited from SBI A/c XX1234 towards Swiggy UPI") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Parsed Result Card
                        testParseResult?.let { result ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (result.isValidTransaction)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else ExpenseRed.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (result.isValidTransaction) "Parsed Successfully" else "Ignored / Not Valid",
                                            fontWeight = FontWeight.Bold,
                                            color = if (result.isValidTransaction) IncomeGreen else ExpenseRed
                                        )
                                        Text(
                                            text = result.detectionType.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (result.isValidTransaction && result.transaction != null) {
                                        val tx = result.transaction
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CategoryIconBadge(category = tx.category, size = 36)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = tx.merchant,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Bank: ${tx.bank} • Category: ${tx.category.displayName}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = "$currency${String.format(Locale.getDefault(), "%,.2f", tx.amount)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = if (tx.type == com.example.domain.model.TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = {
                                                viewModel.testAndImportSms(customSmsInput, customSenderInput)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Import Transaction into Wallet")
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = result.reasonIfNotValid ?: "Not a debit/credit banking message",
                                            fontSize = 12.sp,
                                            color = ExpenseRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
