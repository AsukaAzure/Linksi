package com.linksi.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linksi.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPinSetup by remember { mutableStateOf(false) }
    var showDelayPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.security)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsCard {
                    // Enable App Lock
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.app_lock)) },
                        supportingContent = {
                            Text(
                                stringResource(R.string.app_lock_subtitle),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.Lock, null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = state.isSecurityEnabled,
                                onCheckedChange = { 
                                    viewModel.setSecurityEnabled(it)
                                    if (it && state.pin.isEmpty()) showPinSetup = true
                                }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Biometric Unlock
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.biometric_unlock)) },
                        supportingContent = {
                            Text(
                                stringResource(R.string.biometric_unlock_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.isSecurityEnabled) 1f else 0.38f)
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.Fingerprint, null,
                                tint = if (state.isSecurityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = state.isBiometricEnabled,
                                onCheckedChange = { viewModel.setBiometricEnabled(it) },
                                enabled = state.isSecurityEnabled
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Change PIN
                    ListItem(
                        headlineContent = {
                            Text(
                                if (state.pin.isEmpty()) stringResource(R.string.set_pin)
                                else stringResource(R.string.change_pin),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.isSecurityEnabled) 1f else 0.38f)
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.Password, null,
                                tint = if (state.isSecurityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.isSecurityEnabled) 1f else 0.38f)
                            )
                        },
                        modifier = Modifier.clickable(enabled = state.isSecurityEnabled) { showPinSetup = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Lock Delay
                    ListItem(
                        headlineContent = { 
                            Text(
                                stringResource(R.string.lock_delay),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.isSecurityEnabled) 1f else 0.38f)
                            ) 
                        },
                        supportingContent = {
                            val delayText = when (state.lockDelay) {
                                0L -> stringResource(R.string.immediately)
                                60000L -> stringResource(R.string.after_1_minute)
                                300000L -> stringResource(R.string.after_5_minutes)
                                1800000L -> stringResource(R.string.after_30_minutes)
                                else -> stringResource(R.string.immediately)
                            }
                            Text(
                                delayText, 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.isSecurityEnabled) 1f else 0.38f)
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.Timer, null,
                                tint = if (state.isSecurityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.isSecurityEnabled) 1f else 0.38f)
                            )
                        },
                        modifier = Modifier.clickable(enabled = state.isSecurityEnabled) { showDelayPicker = true }
                    )
                }
            }
        }
    }

    if (showPinSetup) {
        PinSetupDialog(
            onPinSaved = {
                viewModel.setPin(it)
                showPinSetup = false
            },
            onDismiss = { showPinSetup = false }
        )
    }

    if (showDelayPicker) {
        LockDelayPickerSheet(
            currentDelay = state.lockDelay,
            onDelaySelected = {
                viewModel.setLockDelay(it)
                showDelayPicker = false
            },
            onDismiss = { showDelayPicker = false }
        )
    }
}
