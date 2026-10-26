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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.linksi.app.R
import com.linksi.app.domain.model.AiProvider
import com.linksi.app.ui.components.ExpressiveSettingsCard
import com.linksi.app.ui.components.IconContainer

// Display names per provider — single source for the AI settings UI.
internal val AiProvider.displayName: String
    get() = when (this) {
        AiProvider.OPENAI -> "OpenAI"
        AiProvider.ANTHROPIC -> "Anthropic"
        AiProvider.GEMINI -> "Google Gemini"
        AiProvider.DEEPSEEK -> "DeepSeek"
        AiProvider.GROK -> "Grok (xAI)"
        AiProvider.OPENROUTER -> "OpenRouter"
    }

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

            // Enable toggle
            item {
                ExpressiveSettingsCard {
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
                }
            }

            // Model section
            item { AiSectionLabel(stringResource(R.string.ai_section_model)) }
            item {
                ExpressiveSettingsCard {
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
                            when (state.modelStatus) {
                                SettingsViewModel.ModelStatus.ACTIVE -> Text(
                                    stringResource(R.string.ai_model_active, selectedModel?.name ?: ""),
                                    color = Color(0xFF22C55E),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                SettingsViewModel.ModelStatus.ERROR -> Text(
                                    state.updateCheckError ?: stringResource(R.string.ai_model_error),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                SettingsViewModel.ModelStatus.UNKNOWN -> Text(
                                    selectedModel?.let { "${it.name} · ${it.provider.displayName}" }
                                        ?: stringResource(R.string.ai_model_select),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = if (state.aiEnabled) 1f else 0.38f
                                    )
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (state.isRefreshingModels) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(
                                        onClick = { viewModel.refreshModels() },
                                        enabled = state.aiEnabled,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Refresh,
                                            stringResource(R.string.ai_refresh_models),
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = if (state.aiEnabled) 1f else 0.38f
                                            )
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Outlined.ChevronRight, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = if (state.aiEnabled) 1f else 0.38f
                                    )
                                )
                            }
                        },
                        modifier = Modifier.clickable(enabled = state.aiEnabled) { showModelPicker = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            // API keys section
            item { AiSectionLabel(stringResource(R.string.ai_section_api_keys)) }
            item {
                ExpressiveSettingsCard {
                    var selectedApiKeyProvider by remember { mutableStateOf<AiProvider?>(null) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiProvider.entries.chunked(2).forEach { rowProviders ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowProviders.forEach { provider ->
                                    val hasKey = !state.apiKeys[provider].isNullOrBlank()
                                    val modelCount = state.availableModels.count { it.provider == provider }
                                    ProviderTile(
                                        providerName = provider.displayName,
                                        hasKey = hasKey,
                                        modelCount = modelCount,
                                        enabled = state.aiEnabled,
                                        modifier = Modifier.weight(1f),
                                        onClick = { selectedApiKeyProvider = provider }
                                    )
                                }
                                if (rowProviders.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    selectedApiKeyProvider?.let { provider ->
                        ApiKeySheet(
                            provider = provider,
                            currentKey = state.apiKeys[provider] ?: "",
                            currentUrl = state.apiBaseUrls[provider] ?: "",
                            onSave = { key, url -> viewModel.setApiCredentials(provider, key, url) },
                            onClear = { viewModel.clearApiKey(provider) },
                            onDismiss = { selectedApiKeyProvider = null }
                        )
                    }
                }
            }

            // Organize action
            if (state.aiEnabled) {
                item {
                    ExpressiveSettingsCard {
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
            configuredProviders = state.apiKeys.filterValues { it.isNotBlank() }.keys,
            onModelSelected = { viewModel.setSelectedModel(it) },
            onDismiss = { showModelPicker = false }
        )
    }
}

@Composable
private fun AiSectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun ProviderTile(
    providerName: String,
    hasKey: Boolean,
    modelCount: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.5f else 0.2f),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    Icons.Outlined.SmartToy, null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)
                )
                if (hasKey) {
                    Icon(
                        Icons.Outlined.CheckCircle, null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF22C55E)
                    )
                }
            }
            Text(
                providerName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)
            )
            Text(
                when {
                    hasKey && modelCount > 0 -> stringResource(R.string.ai_models_available, modelCount)
                    hasKey -> stringResource(R.string.ai_key_configured)
                    else -> stringResource(R.string.ai_add_key)
                },
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (hasKey) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeySheet(
    provider: AiProvider,
    currentKey: String,
    currentUrl: String,
    onSave: (String, String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf(currentKey) }
    var urlInput by remember { mutableStateOf(currentUrl) }
    var showKey by remember { mutableStateOf(false) }

    val providerName = provider.displayName

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismissWithAnimation() {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Configure $providerName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("API Key") },
                placeholder = { Text("Paste API key here") },
                singleLine = true,
                visualTransformation = if (showKey)
                    androidx.compose.ui.text.input.VisualTransformation.None
                else
                    androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("Custom API Base URL (Optional)") },
                placeholder = { Text("https://api.example.com/v1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentKey.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            onClear()
                            dismissWithAnimation()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear Key")
                    }
                }

                Button(
                    onClick = {
                        onSave(keyInput.trim(), urlInput.trim())
                        dismissWithAnimation()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = keyInput.isNotBlank()
                ) {
                    Text("Save")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
