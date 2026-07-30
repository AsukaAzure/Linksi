package com.linksi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.linksi.app.R
import com.linksi.app.utils.SecurityManager
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    savedPin: String,
    isBiometricEnabled: Boolean,
    onUnlock: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4) {
            if (enteredPin == savedPin) {
                onUnlock()
            } else {
                error = true
                delay(500)
                enteredPin = ""
                error = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (isBiometricEnabled && SecurityManager.canUseBiometric(context)) {
            SecurityManager.showBiometricPrompt(
                activity = context as FragmentActivity,
                title = context.getString(R.string.unlock),
                subtitle = context.getString(R.string.authenticate),
                onSuccess = onUnlock,
                onError = { }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(id = R.string.enter_pin),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // PIN Indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            repeat(4) { index ->
                val isFilled = index < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (error) MaterialTheme.colorScheme.error
                            else if (isFilled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Numpad
        val numpad = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("biometric", "0", "backspace")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            numpad.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { item ->
                        when (item) {
                            "biometric" -> {
                                if (isBiometricEnabled && SecurityManager.canUseBiometric(context)) {
                                    IconButton(
                                        onClick = {
                                            SecurityManager.showBiometricPrompt(
                                                activity = context as FragmentActivity,
                                                title = context.getString(R.string.unlock),
                                                subtitle = context.getString(R.string.authenticate),
                                                onSuccess = onUnlock,
                                                onError = { }
                                            )
                                        },
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Fingerprint,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(64.dp))
                                }
                            }
                            "backspace" -> {
                                IconButton(
                                    onClick = { if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1) },
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Icon(Icons.Outlined.Backspace, null)
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .clickable { if (enteredPin.length < 4) enteredPin += item }
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Medium
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
