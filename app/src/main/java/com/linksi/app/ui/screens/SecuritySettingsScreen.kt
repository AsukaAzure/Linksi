package com.linksi.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
            // Universal Lock Section
            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.universal_lock)) },
                        supportingContent = {
                            Text(
                                stringResource(R.string.universal_lock_desc),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.VerifiedUser, null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = state.universalLock,
                                onCheckedChange = { viewModel.setUniversalLock(it) }
                            )
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.app_security),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (state.universalLock) 1f else 0.38f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    // Enable App Lock
                    ListItem(
                        headlineContent = { 
                            Text(
                                stringResource(R.string.app_lock),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.universalLock) 1f else 0.38f)
                            ) 
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.app_lock_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.universalLock) 1f else 0.38f)
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.Lock, null,
                                tint = if (state.universalLock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = state.isSecurityEnabled,
                                enabled = state.universalLock,
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
                        headlineContent = { 
                            Text(
                                stringResource(R.string.biometric_unlock),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.universalLock && state.isSecurityEnabled) 1f else 0.38f)
                            ) 
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.biometric_unlock_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.universalLock && state.isSecurityEnabled) 1f else 0.38f)
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.Fingerprint, null,
                                tint = if (state.universalLock && state.isSecurityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = state.isBiometricEnabled,
                                onCheckedChange = { viewModel.setBiometricEnabled(it) },
                                enabled = state.universalLock && state.isSecurityEnabled
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
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.universalLock && state.isSecurityEnabled) 1f else 0.38f)
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.Password, null,
                                tint = if (state.universalLock && state.isSecurityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.universalLock && state.isSecurityEnabled) 1f else 0.38f)
                            )
                        },
                        modifier = Modifier.clickable(enabled = state.universalLock && state.isSecurityEnabled) { showPinSetup = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Lock Delay
                    ListItem(
                        headlineContent = { 
                            Text(
                                stringResource(R.string.lock_delay),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.universalLock && state.isSecurityEnabled) 1f else 0.38f)
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.universalLock && state.isSecurityEnabled) 1f else 0.38f)
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.Timer, null,
                                tint = if (state.universalLock && state.isSecurityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.universalLock && state.isSecurityEnabled) 1f else 0.38f)
                            )
                        },
                        modifier = Modifier.clickable(enabled = state.universalLock && state.isSecurityEnabled) { showDelayPicker = true }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.locked_folders),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (state.universalLock) 1f else 0.38f)
                    )
                    Switch(
                        checked = state.folderLockEnabled,
                        enabled = state.universalLock,
                        onCheckedChange = { viewModel.setFolderLockEnabled(it) },
                        modifier = Modifier.scale(0.8f)
                    )
                }
                SettingsCard {
                    if (state.folders.isEmpty()) {
                        ListItem(
                            headlineContent = { 
                                Text(
                                    stringResource(R.string.no_folders_created),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.universalLock && state.folderLockEnabled) 1f else 0.38f)
                                ) 
                            },
                            leadingContent = { 
                                Icon(
                                    Icons.Outlined.Folder, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.universalLock && state.folderLockEnabled) 1f else 0.38f)
                                ) 
                            }
                        )
                    } else {
                        state.folders.forEachIndexed { index, folder ->
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        folder.name,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.universalLock && state.folderLockEnabled) 1f else 0.38f)
                                    ) 
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Outlined.Folder, null,
                                        tint = try {
                                            Color(android.graphics.Color.parseColor(folder.color)).copy(alpha = if (state.universalLock && state.folderLockEnabled) 1f else 0.38f)
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = if (state.universalLock && state.folderLockEnabled) 1f else 0.38f)
                                        }
                                    )
                                },
                                trailingContent = {
                                    Switch(
                                        checked = folder.isLocked,
                                        enabled = state.universalLock && state.folderLockEnabled,
                                        onCheckedChange = { 
                                            if (it && state.pin.isEmpty()) {
                                                showPinSetup = true
                                            } else {
                                                viewModel.toggleFolderLock(folder.id, it)
                                            }
                                        }
                                    )
                                }
                            )
                            if (index < state.folders.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
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
