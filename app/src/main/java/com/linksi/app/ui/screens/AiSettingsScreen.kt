package com.linksi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linksi.app.R
import com.linksi.app.domain.model.AiProvider
import com.linksi.app.ui.components.ExpressiveSettingsCard
import com.linksi.app.ui.components.IconContainer

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
                            Icons.Outlined.AutoAwesome,
                            null,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.ai_organizer),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        stringResource(R.string.ai_organizer_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }

            item {
                ExpressiveSettingsCard {
                    // Enable toggle
                    ListItem(
                        headlineContent = { 
                            Text(
                                stringResource(R.string.ai_organizer),
                                fontWeight = FontWeight.SemiBold
                            ) 
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.ai_organizer_subtitle),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingContent = {
                            IconContainer(Icons.Outlined.AutoAwesome)
                        },
                        trailingContent = {
                            Switch(
                                checked = state.aiEnabled,
                                onCheckedChange = { viewModel.setAiEnabled(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Model selector
                    val selectedModel = state.availableModels.find { it.id == state.selectedModelId }
                    ListItem(
                        headlineContent = { 
                            Text(
                                stringResource(R.string.ai_model),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.aiEnabled) 1f else 0.38f)
                            ) 
                        },
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
                                    style = MaterialTheme.typography.labelMedium
                                )
                                SettingsViewModel.ModelStatus.ERROR -> Text(
                                    state.updateCheckError ?: stringResource(R.string.ai_model_error),
                                    color = color,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                SettingsViewModel.ModelStatus.UNKNOWN -> Text(
                                    selectedModel?.name ?: stringResource(R.string.ai_model_select),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = color
                                )
                            }
                        },
                        leadingContent = {
                            IconContainer(
                                icon = Icons.Outlined.SmartToy,
                                enabled = state.aiEnabled
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.aiEnabled) 1f else 0.38f)
                            )
                        },
                        modifier = Modifier.clickable(enabled = state.aiEnabled) { showModelPicker = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // API Key
                    if (selectedModel != null) {
                        ApiKeyItem(
                            provider = selectedModel.provider,
                            currentKey = state.apiKeys[selectedModel.provider] ?: "",
                            onSave = { key -> viewModel.setApiKey(selectedModel.provider, key) },
                            enabled = state.aiEnabled
                        )
                    }

                    if (state.aiEnabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Open organizer
                        ListItem(
                            headlineContent = { 
                                Text(
                                    stringResource(R.string.organize_my_links),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ) 
                            },
                            supportingContent = {
                                Text(
                                    stringResource(R.string.organize_now),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingContent = {
                                IconContainer(
                                    icon = Icons.Outlined.AutoFixHigh,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                Icon(
                                    Icons.Outlined.ChevronRight, null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable(onClick = onOpenOrganizer),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }

    if (showModelPicker) {
        ModelPickerSheet(
            currentModelId = state.selectedModelId,
            models = state.availableModels,
            onModelSelected = { viewModel.setSelectedModel(it) },
            onDismiss = { showModelPicker = false }
        )
    }
}
