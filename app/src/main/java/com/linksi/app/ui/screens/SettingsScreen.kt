package com.linksi.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.materialIcon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linksi.app.domain.model.AI_MODELS
import com.linksi.app.domain.model.AiProvider
import com.linksi.app.utils.exportFileName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showModelPicker by remember { mutableStateOf(false) }
    var showAiOrganizer by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    // Handle system back button
    BackHandler(enabled = !showAiOrganizer && !showImportExport) {
        onBack()
    }

    // File launchers
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportJson(context, it) } }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportCsv(context, it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importFile(context, it) } }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = com.linksi.app.R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, stringResource(id = com.linksi.app.R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Import & Export ───────────────────────────────────
            item {
                SectionHeader(stringResource(id = com.linksi.app.R.string.import_export), Icons.Outlined.SwapHoriz)
            }
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.FolderOpen,
                        title = stringResource(id = com.linksi.app.R.string.import_export),
                        subtitle = stringResource(id = com.linksi.app.R.string.import_export_subtitle),
                        onClick = { showImportExport = true },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // ── AI Organizer ──────────────────────────────────────
            item {
                SectionHeader(stringResource(id = com.linksi.app.R.string.ai_organizer), Icons.Outlined.AutoAwesome)
            }
            item {
                SettingsCard {
                    // Enable toggle
                    ListItem(
                        headlineContent = { Text(stringResource(id = com.linksi.app.R.string.ai_organizer)) },
                        supportingContent = {
                            Text(
                                stringResource(id = com.linksi.app.R.string.ai_organizer_subtitle),
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

                    if (state.aiEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Model selector
                        val selectedModel = AI_MODELS.find { it.id == state.selectedModelId }
                        ListItem(
                            headlineContent = { Text(stringResource(id = com.linksi.app.R.string.ai_model)) },
                            supportingContent = {
                            when (state.modelStatus) {
                                SettingsViewModel.ModelStatus.ACTIVE -> Text(
                                    stringResource(id = com.linksi.app.R.string.ai_model_active, selectedModel?.name ?: ""),
                                    color = Color(0xFF22C55E),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                SettingsViewModel.ModelStatus.ERROR -> Text(
                                    state.updateCheckError ?: stringResource(id = com.linksi.app.R.string.ai_model_error),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                SettingsViewModel.ModelStatus.UNKNOWN -> Text(
                                    selectedModel?.name ?: stringResource(id = com.linksi.app.R.string.ai_model_select),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                            leadingContent = {
                                Icon(
                                    Icons.Outlined.SmartToy, null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                Icon(
                                    Icons.Outlined.ChevronRight, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.clickable { showModelPicker = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // API Key — only for selected provider
                        if (selectedModel != null) {
                            val providerName = when (selectedModel.provider) {
                                AiProvider.OPENAI -> "OpenAI"
                                AiProvider.ANTHROPIC -> "Anthropic"
                                AiProvider.GEMINI -> "Google Gemini"
                                AiProvider.DEEPSEEK -> "DeepSeek"
                                AiProvider.GROK -> "Grok (xAI)"
                            }
                            ApiKeyItem(
                                provider = selectedModel.provider,
                                currentKey = state.apiKeys[selectedModel.provider] ?: "",
                                onSave = { key -> viewModel.setApiKey(selectedModel.provider, key) }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Open organizer
                        ListItem(
                            headlineContent = { Text(stringResource(id = com.linksi.app.R.string.organize_my_links)) },
                            supportingContent = {
                                Text(
                                    stringResource(id = com.linksi.app.R.string.organize_now),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Outlined.AutoFixHigh, null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                Icon(
                                    Icons.Outlined.ChevronRight, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.clickable { showAiOrganizer = true }
                        )
                    }
                }
            }

            // ── Browser ───────────────────────────────────────────
            item {
                SectionHeader(stringResource(id = com.linksi.app.R.string.browser), Icons.Outlined.Language)
            }
            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text(stringResource(id = com.linksi.app.R.string.in_app_browser)) },
                        supportingContent = {
                            Text(
                                if (state.useInAppBrowser)
                                    stringResource(id = com.linksi.app.R.string.in_app_browser_on)
                                else
                                    stringResource(id = com.linksi.app.R.string.in_app_browser_off),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(
                                if (state.useInAppBrowser) Icons.Outlined.OpenInBrowser
                                else Icons.Outlined.Launch,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = state.useInAppBrowser,
                                onCheckedChange = { viewModel.toggleInAppBrowser(it) }
                            )
                        }
                    )
                }
            }


            // ── General ───────────────────────────────────────────
            item {
                SectionHeader(stringResource(id = com.linksi.app.R.string.general), Icons.Outlined.Settings)
            }
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Language,
                        title = stringResource(id = com.linksi.app.R.string.language),
                        subtitle = when(state.selectedLanguage) {
                            "en" -> "English"
                            "es" -> "Spanish"
                            "ru" -> "Russian"
                            else -> stringResource(id = com.linksi.app.R.string.system_default)
                        },
                        onClick = { showLanguagePicker = true },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // ── Updates ───────────────────────────────────────────
            item {
                SectionHeader(stringResource(id = com.linksi.app.R.string.updates), Icons.Outlined.SystemUpdate)
            }
            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text(stringResource(id = com.linksi.app.R.string.version)) },
                        supportingContent = {
                            Text(
                                stringResource(id = com.linksi.app.R.string.current) + ": ${state.currentVersion}" +
                                        if (state.latestVersion.isNotBlank())
                                            " · " + stringResource(id = com.linksi.app.R.string.latest) + ": ${state.latestVersion}"
                                        else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.Info, null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            when {
                                state.isCheckingUpdate -> {
                                    CircularProgressIndicator(
                                        Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }

                                state.updateAvailable -> {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            stringResource(id = com.linksi.app.R.string.update_available),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp, vertical = 4.dp
                                            )
                                        )
                                    }
                                }

                                state.latestVersion.isNotBlank() -> {
                                    Icon(
                                        Icons.Outlined.CheckCircle, null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = {
                            Text(
                                if (state.updateAvailable) stringResource(id = com.linksi.app.R.string.download_update)
                                else stringResource(id = com.linksi.app.R.string.check_for_updates)
                            )
                        },
                        supportingContent = {
                            state.updateCheckError?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        leadingContent = {
                            Icon(
                                if (state.updateAvailable) Icons.Outlined.Download
                                else Icons.Outlined.Refresh,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable {
                            if (state.updateAvailable) {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/AsukaAzure/Linksi/releases/latest")
                                )
                                context.startActivity(intent)
                            } else {
                                viewModel.checkForUpdate()
                            }
                        }
                    )
                }
            }

            // ── Stats ─────────────────────────────────────────────
            item {
                SectionHeader(stringResource(id = com.linksi.app.R.string.stats), Icons.Outlined.BarChart)
            }
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(stringResource(id = com.linksi.app.R.string.total_links), "${state.totalLinks}")
                        StatItem(stringResource(id = com.linksi.app.R.string.folders), "${state.totalFolders}")
                        StatItem(stringResource(id = com.linksi.app.R.string.favorites), "${state.totalFavorites}")
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(id = com.linksi.app.R.string.made_with),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "❤️",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        stringResource(id = com.linksi.app.R.string.by),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Anush",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/AsukaAzure/")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ── Import Result Dialog ──────────────────────────────────
    state.importResult?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::dismissImportResult,
            icon = {
                Icon(
                    Icons.Outlined.CheckCircle, null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(stringResource(id = com.linksi.app.R.string.import_complete)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(id = com.linksi.app.R.string.imported_links_from, result.count, result.source))
                    if (result.folders.isNotEmpty())
                        Text(stringResource(id = com.linksi.app.R.string.folders_also_imported, result.folders.size))
                    if (state.duplicateCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Info, null,
                                    Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    stringResource(id = com.linksi.app.R.string.duplicates_skipped, state.duplicateCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::dismissImportResult) { Text(stringResource(id = com.linksi.app.R.string.done)) }
            }
        )
    }

    // ── Model Picker ──────────────────────────────────────────
    if (showModelPicker) {
        ModelPickerSheet(
            currentModelId = state.selectedModelId,
            onModelSelected = { viewModel.setSelectedModel(it) },
            onDismiss = { showModelPicker = false }
        )
    }

    // ── Language Picker ───────────────────────────────────────
    if (showLanguagePicker) {
        LanguagePickerSheet(
            currentLanguageCode = state.selectedLanguage,
            onLanguageSelected = { 
                viewModel.setLanguage(it)
                showLanguagePicker = false
            },
            onDismiss = { showLanguagePicker = false }
        )
    }

    // ── AI Organizer overlay ──────────────────────────────────
    AnimatedVisibility(
        visible = showAiOrganizer,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        BackHandler { showAiOrganizer = false }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AiOrganizerScreen(onBack = { showAiOrganizer = false })
        }
    }

    // ── Import Export overlay ─────────────────────────────────
    AnimatedVisibility(
        visible = showImportExport,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        BackHandler { showImportExport = false }
        ImportExportScreen(
            state = state,
            onBack = { showImportExport = false },
            exportJsonLauncher = exportJsonLauncher,
            exportCsvLauncher = exportCsvLauncher,
            importLauncher = importLauncher,
            exportFileName = ::exportFileName
        )
    }
}

// ── Section Header ────────────────────────────────────────────
@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Icon(
            icon, null,
            Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = trailingContent,
        modifier = androidx.compose.ui.Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun BrowserInstructionItem(browser: String, steps: String) {
    val parts = steps.split("\n or \n")
    ListItem(
        headlineContent = { Text(browser, style = MaterialTheme.typography.titleSmall) },
        supportingContent = {
            Column {
                parts.forEachIndexed { index, part ->
                    Text(part.trim(), style = MaterialTheme.typography.bodySmall)
                    if (index < parts.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        },
        leadingContent = { Icon(Icons.Outlined.Info, null) }
    )
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value, style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(
    currentLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                stringResource(id = com.linksi.app.R.string.select_language),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            val languages = listOf(
                "" to stringResource(id = com.linksi.app.R.string.system_default),
                "en" to "English",
                "es" to "Spanish",
                "ru" to "Russian"
            )

            languages.forEach { (code, name) ->
                ListItem(
                    headlineContent = { Text(name) },
                    leadingContent = {
                        RadioButton(
                            selected = currentLanguageCode == code,
                            onClick = { onLanguageSelected(code) }
                        )
                    },
                    modifier = Modifier.clickable { onLanguageSelected(code) }
                )
            }
        }
    }
}

@Composable
fun ApiKeyItem(
    provider: AiProvider,
    currentKey: String,
    onSave: (String) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }

    val providerName = when (provider) {
        AiProvider.OPENAI -> "OpenAI"
        AiProvider.ANTHROPIC -> "Anthropic"
        AiProvider.GEMINI -> "Google Gemini"
        AiProvider.DEEPSEEK -> "DeepSeek"
        AiProvider.GROK -> "Grok (xAI)"
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                providerName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (currentKey.isNotBlank()) {
                Icon(
                    Icons.Outlined.CheckCircle, null,
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            TextButton(
                onClick = { editing = !editing; keyInput = currentKey }
            ) {
                Text(if (editing) stringResource(id = com.linksi.app.R.string.cancel) else if (currentKey.isBlank()) stringResource(id = com.linksi.app.R.string.add) else stringResource(id = com.linksi.app.R.string.edit))
            }
        }

        if (editing) {
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                placeholder = { Text(stringResource(id = com.linksi.app.R.string.paste_api_key)) },
                singleLine = true,
                visualTransformation = if (showKey)
                    androidx.compose.ui.text.input.VisualTransformation.None
                else
                    androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Outlined.VisibilityOff
                                else Icons.Outlined.Visibility,
                                null
                            )
                        }
                        IconButton(
                            onClick = {
                                onSave(keyInput.trim())
                                editing = false
                            },
                            enabled = keyInput.isNotBlank()
                        ) {
                            Icon(
                                Icons.Outlined.Check, null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        } else if (currentKey.isNotBlank()) {
            Text(
                "••••••••${currentKey.takeLast(4)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}