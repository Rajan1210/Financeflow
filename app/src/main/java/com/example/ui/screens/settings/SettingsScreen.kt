package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }

    val isPinSet = userProfile?.isPinEnabled == true
    val isBiometricEnabled = userProfile?.isBiometricEnabled == true
    val currentCurrency = userProfile?.currencySymbol ?: "₹"

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Settings & Security", fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Preferences Section
            item {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        // Currency Selector
                        SettingRow(
                            icon = Icons.Filled.CurrencyRupee,
                            title = "Default Currency",
                            subtitle = "Currently set to $currentCurrency",
                            onClick = { showCurrencyDialog = true }
                        )

                        Divider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Theme Mode
                        val themeTitle = when (userProfile?.isDarkMode) {
                            true -> "Dark Theme"
                            false -> "Light Theme"
                            null -> "Follow System"
                        }
                        SettingRow(
                            icon = Icons.Filled.DarkMode,
                            title = "Appearance",
                            subtitle = themeTitle,
                            onClick = {
                                val next = when (userProfile?.isDarkMode) {
                                    null -> true
                                    true -> false
                                    false -> null
                                }
                                viewModel.updateThemeMode(next)
                            }
                        )
                    }
                }
            }

            // Security Section
            item {
                Text(
                    text = "Security & Privacy",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        // PIN Lock
                        SettingRow(
                            icon = Icons.Filled.Lock,
                            title = "App PIN Lock",
                            subtitle = if (isPinSet) "Active (Tap to change or remove)" else "Disabled (Tap to set PIN)",
                            onClick = { showPinDialog = true }
                        )

                        Divider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Biometrics
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Biometric Login",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Unlock with fingerprint or face",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { viewModel.toggleBiometric(it) }
                            )
                        }
                    }
                }
            }

            // Backup & Data Section
            item {
                Text(
                    text = "Data & Cloud Sync",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingRow(
                            icon = Icons.Filled.CloudSync,
                            title = "Supabase Cloud Sync",
                            subtitle = "Sync architecture ready • Tap to inspect config",
                            onClick = { showCloudSyncDialog = true }
                        )

                        Divider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingRow(
                            icon = Icons.Filled.Download,
                            title = "Export Local Backup (JSON)",
                            subtitle = "Export all accounts, transactions & investments",
                            onClick = {
                                viewModel.exportData { json ->
                                    exportedJsonText = json
                                    showExportDialog = true
                                }
                            }
                        )

                        Divider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingRow(
                            icon = Icons.Filled.Upload,
                            title = "Restore Backup",
                            subtitle = "Restore database from JSON string",
                            onClick = { showImportDialog = true }
                        )
                    }
                }
            }

            // Notifications & Alerts Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "System Alerts & Notifications (${notifications.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (notifications.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearNotifications() }) {
                            Text("Clear All")
                        }
                    }
                }
            }

            if (notifications.isEmpty()) {
                item {
                    Text(
                        text = "No notification alerts recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(notifications, key = { it.id }) { notif ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (notif.type) {
                                            "SALARY" -> IncomeGreen.copy(alpha = 0.15f)
                                            "CREDIT_CARD", "EMI" -> WarningAmber.copy(alpha = 0.15f)
                                            else -> ExpenseRed.copy(alpha = 0.15f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (notif.type) {
                                        "SALARY" -> Icons.Filled.MonetizationOn
                                        "CREDIT_CARD" -> Icons.Filled.CreditCard
                                        "EMI" -> Icons.Filled.CalendarMonth
                                        else -> Icons.Filled.NotificationsActive
                                    },
                                    contentDescription = null,
                                    tint = when (notif.type) {
                                        "SALARY" -> IncomeGreen
                                        "CREDIT_CARD", "EMI" -> WarningAmber
                                        else -> ExpenseRed
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = notif.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = notif.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Currency Dialog
    if (showCurrencyDialog) {
        val currencies = listOf("₹" to "INR (₹)", "$" to "USD ($)", "€" to "EUR (€)", "£" to "GBP (£)", "¥" to "JPY (¥)", "C$" to "CAD (C$)", "A$" to "AUD (A$)")
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text(text = "Choose Default Currency", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    currencies.forEach { (symbol, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateCurrency(symbol)
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentCurrency == symbol,
                                onClick = {
                                    viewModel.updateCurrency(symbol)
                                    showCurrencyDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) { Text("Close") }
            }
        )
    }

    // Set PIN Dialog
    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(text = if (isPinSet) "Update or Remove PIN" else "Set 4-Digit Security PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        label = { Text("4-digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            viewModel.setPin(pinInput)
                            showPinDialog = false
                        }
                    },
                    enabled = pinInput.length == 4
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                if (isPinSet) {
                    TextButton(
                        onClick = {
                            viewModel.disablePin()
                            showPinDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
                    ) {
                        Text("Disable PIN")
                    }
                } else {
                    TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(text = "Backup Ready", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Your financial database has been exported as JSON format:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJsonText.take(500) + if (exportedJsonText.length > 500) "..." else "",
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("FinPulse Backup", exportedJsonText))
                        showExportDialog = false
                    }
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Close") }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        var importJsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(text = "Restore Database Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Paste your previously exported FinPulse JSON backup below:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        placeholder = { Text("Paste JSON here...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importData(importJsonInput) {
                            showImportDialog = false
                        }
                    },
                    enabled = importJsonInput.isNotBlank()
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Cloud Sync Dialog
    if (showCloudSyncDialog) {
        AlertDialog(
            onDismissRequest = { showCloudSyncDialog = false },
            title = { Text(text = "Supabase Cloud Sync", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FinPulse Cloud Sync Architecture is enabled for multi-device sync with Supabase PostgreSQL.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Divider()
                    Text(
                        text = "• Endpoint: https://finpulse-cloud.supabase.co\n• Mode: Offline-First with Room SQLite Local Cache\n• Realtime: Enabled\n• Encryption: AES-GCM Client Side",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showCloudSyncDialog = false }) { Text("Got It") }
            }
        )
    }
}

@Composable
fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
