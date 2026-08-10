package com.linksi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Expressive Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.VerifiedUser,
                            null,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.security),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        stringResource(R.string.universal_lock_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }

            // Universal Lock Section
            item {
                ExpressiveSettingsCard {
                    ListItem(
                        headlineContent = { 
                            Text(
                                stringResource(R.string.universal_lock),
                                fontWeight = FontWeight.SemiBold
                            ) 
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.universal_lock_desc),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingContent = {
                            IconContainer(Icons.Outlined.Security)
                        },
                        trailingContent = {
                            Switch(
                                checked = state.universalLock,
                                onCheckedChange = { viewModel.setUniversalLock(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    ListItem(
                        headlineContent = { 
                            Text(
                                stringResource(R.string.global_prevent_screenshot),
                                fontWeight = FontWeight.SemiBold
                            ) 
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.global_prevent_screenshot_desc),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingContent = {
                            IconContainer(Icons.Outlined.NoPhotography)
                        },
                        trailingContent = {
                            Switch(
                                checked = state.globalPreventScreenshot,
                                onCheckedChange = { viewModel.setGlobalPreventScreenshot(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            item {
                Text(
                    stringResource(R.string.app_security),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (state.universalLock) 1f else 0.38f),
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
                )
                ExpressiveSettingsCard {
                    // Enable App Lock
                    ListItem(
                        headlineContent = { 
                            Text(
                                stringResource(R.string.app_lock),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.universalLock) 1f else 0.38f)
                            ) 
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.app_lock_subtitle),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.universalLock) 1f else 0.38f)
                            )
                        },
                        leadingContent = {
                            IconContainer(
                                icon = Icons.Outlined.Lock,
                                enabled = state.universalLock
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
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Biometric Unlock
                    ListItem(
                        headlineContent = { 
                            Text(
                                stringResource(R.string.biometric_unlock),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.universalLock && state.isSecurityEnabled) 1f else 0.38f)
                            ) 
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.biometric_unlock_subtitle),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.universalLock && state.isSecurityEnabled) 1f else 0.38f)
                            )
                        },
                        leadingContent = {
                            IconContainer(
                                icon = Icons.Outlined.Fingerprint,
                                enabled = state.universalLock && state.isSecurityEnabled
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = state.isBiometricEnabled,
                                onCheckedChange = { viewModel.setBiometricEnabled(it) },
                                enabled = state.universalLock && state.isSecurityEnabled
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Change PIN
                    ListItem(
                        headlineContent = {
                            Text(
                                if (state.pin.isEmpty()) stringResource(R.string.set_pin)
                                else stringResource(R.string.change_pin),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.universalLock && state.isSecurityEnabled) 1f else 0.38f)
                            )
                        },
                        leadingContent = {
                            IconContainer(
                                icon = Icons.Outlined.Password,
                                enabled = state.universalLock && state.isSecurityEnabled
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.universalLock && state.isSecurityEnabled) 1f else 0.38f)
                            )
                        },
                        modifier = Modifier
                            .clickable(enabled = state.universalLock && state.isSecurityEnabled) { showPinSetup = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Lock Delay
                    ListItem(
                        headlineContent = { 
                            Text(
                                stringResource(R.string.lock_delay),
                                fontWeight = FontWeight.SemiBold,
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
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.universalLock && state.isSecurityEnabled) 1f else 0.38f)
                            )
                        },
                        leadingContent = {
                            IconContainer(
                                icon = Icons.Outlined.Timer,
                                enabled = state.universalLock && state.isSecurityEnabled
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.universalLock && state.isSecurityEnabled) 1f else 0.38f)
                            )
                        },
                        modifier = Modifier
                            .clickable(enabled = state.universalLock && state.isSecurityEnabled) { showDelayPicker = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.locked_folders),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (state.universalLock) 1f else 0.38f)
                    )
                    Switch(
                        checked = state.folderLockEnabled,
                        enabled = state.universalLock,
                        onCheckedChange = { viewModel.setFolderLockEnabled(it) },
                        modifier = Modifier.scale(0.8f)
                    )
                }
                ExpressiveSettingsCard {
                    if (state.folders.isEmpty()) {
                        ListItem(
                            headlineContent = { 
                                Text(
                                    stringResource(R.string.no_folders_created),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.universalLock && state.folderLockEnabled) 1f else 0.38f)
                                ) 
                            },
                            leadingContent = { 
                                IconContainer(
                                    icon = Icons.Outlined.Folder,
                                    enabled = state.universalLock && state.folderLockEnabled
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    } else {
                        state.folders.forEachIndexed { index, folder ->
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        folder.name,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.universalLock && state.folderLockEnabled) 1f else 0.38f)
                                    ) 
                                },
                                leadingContent = {
                                    IconContainer(
                                        icon = Icons.Outlined.Folder,
                                        color = try { Color(android.graphics.Color.parseColor(folder.color)) } catch(e:Exception) { MaterialTheme.colorScheme.primary },
                                        enabled = state.universalLock && state.folderLockEnabled
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
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            if (index < state.folders.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
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

@Composable
fun ExpressiveSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
fun IconContainer(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) color.copy(alpha = 0.12f)
                else color.copy(alpha = 0.05f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) color else color.copy(alpha = 0.38f)
        )
    }
}
