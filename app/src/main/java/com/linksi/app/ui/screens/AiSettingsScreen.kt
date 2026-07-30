package com.linksi.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linksi.app.R
import com.linksi.app.domain.model.AI_MODELS
import com.linksi.app.domain.model.AiProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    onOpenOrganizer: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showModelPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_organizer)) },
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
                    // Enable toggle
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.ai_organizer)) },
                        supportingContent = {
                            Text(
                                stringResource(R.string.ai_organizer_subtitle),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.AutoAwesome, null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = state.aiEnabled,
                                onCheckedChange = { viewModel.setAiEnabled(it) }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Model selector
                    val selectedModel = AI_MODELS.find { it.id == state.selectedModelId }
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.ai_model)) },
                        supportingContent = {
                            val color = if (state.aiEnabled) {
                                if (state.modelStatus == SettingsViewModel.ModelStatus.ACTIVE) Color(0xFF22C55E)
                                else if (state.modelStatus == SettingsViewModel.ModelStatus.ERROR) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            }
                            
                            when (state.modelStatus) {
                                SettingsViewModel.ModelStatus.ACTIVE -> Text(
                                    stringResource(R.string.ai_model_active, selectedModel?.name ?: ""),
                                    color = color,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                SettingsViewModel.ModelStatus.ERROR -> Text(
                                    state.updateCheckError ?: stringResource(R.string.ai_model_error),
                                    color = color,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                SettingsViewModel.ModelStatus.UNKNOWN -> Text(
                                    selectedModel?.name ?: stringResource(R.string.ai_model_select),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = color
                                )
                            }
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.SmartToy, null,
                                tint = if (state.aiEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.aiEnabled) 1f else 0.38f)
                            )
                        },
                        modifier = Modifier.clickable(enabled = state.aiEnabled) { showModelPicker = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // API Key
                    if (selectedModel != null) {
                        ApiKeyItem(
                            provider = selectedModel.provider,
                            currentKey = state.apiKeys[selectedModel.provider] ?: "",
                            onSave = { key -> viewModel.setApiKey(selectedModel.provider, key) },
                            enabled = state.aiEnabled
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Open organizer
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.organize_my_links)) },
                        supportingContent = {
                            Text(
                                stringResource(R.string.organize_now),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.AutoFixHigh, null,
                                tint = if (state.aiEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.aiEnabled) 1f else 0.38f)
                            )
                        },
                        modifier = Modifier.clickable(enabled = state.aiEnabled, onClick = onOpenOrganizer)
                    )
                }
            }
        }
    }

    if (showModelPicker) {
        ModelPickerSheet(
            currentModelId = state.selectedModelId,
            onModelSelected = { viewModel.setSelectedModel(it) },
            onDismiss = { showModelPicker = false }
        )
    }
}
