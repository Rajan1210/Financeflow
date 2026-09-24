package com.example.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun AuthScreen(
    viewModel: FinanceViewModel,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val userProfile by viewModel.userProfile.collectAsState()
    val isBiometricEnabled = userProfile?.isBiometricEnabled ?: false

    val isPinSetupMode = userProfile?.pinHash == null

    fun handleDigit(d: String) {
        if (enteredPin.length < 4) {
            enteredPin += d
            errorMessage = null
            if (enteredPin.length == 4) {
                if (isPinSetupMode) {
                    viewModel.setPin(enteredPin)
                    onSuccess()
                } else {
                    viewModel.unlockWithPin(enteredPin) { success ->
                        if (success) {
                            onSuccess()
                        } else {
                            errorMessage = "Incorrect PIN. Please try again."
                            enteredPin = ""
                        }
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("auth_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "FinPulse Security",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isPinSetupMode) "Create a 4-digit PIN to secure your financial data" else "Enter your 4-digit Security PIN",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4 Pin dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isFilled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            // Error message
            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = ExpenseRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Keypad 1-9, 0, Backspace, Biometric
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    when (key) {
                                        "BIO", "DEL" -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                )
                                .clickable {
                                    when (key) {
                                        "DEL" -> handleBackspace()
                                        "BIO" -> {
                                            if (isBiometricEnabled) {
                                                viewModel.unlockBiometric()
                                                onSuccess()
                                            }
                                        }
                                        else -> handleDigit(key)
                                    }
                                }
                                .testTag("keypad_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            when (key) {
                                "DEL" -> Icon(
                                    imageVector = Icons.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                "BIO" -> {
                                    if (isBiometricEnabled) {
                                        Icon(
                                            imageVector = Icons.Filled.Fingerprint,
                                            contentDescription = "Biometric",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                else -> Text(
                                    text = key,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isPinSetupMode) {
                TextButton(
                    onClick = {
                        // Quick fallback bypass in case user forgot PIN during local session
                        viewModel.disablePin()
                        onSuccess()
                    },
                    modifier = Modifier.testTag("reset_pin_button")
                ) {
                    Text("Forgot PIN? Reset Lock")
                }
            }
        }
    }
}
