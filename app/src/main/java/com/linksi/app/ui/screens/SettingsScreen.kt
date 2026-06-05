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
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back")
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
                SectionHeader("Import & Export", Icons.Outlined.SwapHoriz)
            }
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.FolderOpen,
                        title = "Import & Export",
                        subtitle = "Backup, restore, or import from browsers",
                        onClick = { showImportExport = true },
                        trailingContent = {
                            Icon(Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    )
                }
            }

            // ── AI Organizer ──────────────────────────────────────
            item {
                SectionHeader("AI Organizer", Icons.Outlined.AutoAwesome)
            }
            item {
                SettingsCard {
                    // Enable toggle
                    ListItem(
                        headlineContent = { Text("AI Organizer") },
                        supportingContent = {
                            Text(
                                "Auto-organize links into folders using AI",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(Icons.Outlined.AutoAwesome, null,
                                tint = MaterialTheme.colorScheme.primary)
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
                            headlineContent = { Text("AI Model") },
                            supportingContent = {
                                Text(
                                    selectedModel?.name ?: "Select a model",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingContent = {
                                Icon(Icons.Outlined.SmartToy, null,
                                    tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingContent = {
                                Icon(Icons.Outlined.ChevronRight, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            modifier = Modifier.clickable { showModelPicker = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // API Key — only for selected provider
                        if (selectedModel != null) {
                            val providerName = when (selectedModel.provider) {
                                AiProvider.OPENAI    -> "OpenAI"
                                AiProvider.ANTHROPIC -> "Anthropic"
                                AiProvider.GEMINI    -> "Google Gemini"
                                AiProvider.DEEPSEEK  -> "DeepSeek"
                                AiProvider.GROK      -> "Grok (xAI)"
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
                            headlineContent = { Text("Organize My Links") },
                            supportingContent = {
                                Text("Start organizing now",
                                    style = MaterialTheme.typography.bodySmall)
                            },
                            leadingContent = {
                                Icon(Icons.Outlined.AutoFixHigh, null,
                                    tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingContent = {
                                Icon(Icons.Outlined.ChevronRight, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            modifier = Modifier.clickable { showAiOrganizer = true }
                        )
                    }
                }
            }

            // ── Browser ───────────────────────────────────────────
            item {
                SectionHeader("Browser", Icons.Outlined.Language)
            }
            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text("In-app browser") },
                        supportingContent = {
                            Text(
                                if (state.useInAppBrowser)
                                    "Links open inside Linksi"
                                else
                                    "Links open in your default browser",
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

            // ── Updates ───────────────────────────────────────────
            item {
                SectionHeader("Updates", Icons.Outlined.SystemUpdate)
            }
            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text("Version") },
                        supportingContent = {
                            Text(
                                "Current: ${state.currentVersion}" +
                                        if (state.latestVersion.isNotBlank())
                                            " · Latest: ${state.latestVersion}"
                                        else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(Icons.Outlined.Info, null,
                                tint = MaterialTheme.colorScheme.primary)
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
                                            "Update available",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp, vertical = 4.dp
                                            )
                                        )
                                    }
                                }
                                state.latestVersion.isNotBlank() -> {
                                    Icon(Icons.Outlined.CheckCircle, null,
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = {
                            Text(if (state.updateAvailable) "Download update"
                            else "Check for updates")
                        },
                        supportingContent = {
                            state.updateCheckError?.let {
                                Text(it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
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
                SectionHeader("Stats", Icons.Outlined.BarChart)
            }
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Total links", "${state.totalLinks}")
                        StatItem("Folders", "${state.totalFolders}")
                        StatItem("Favorites", "${state.totalFavorites}")
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // ── Import Result Dialog ──────────────────────────────────
    state.importResult?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::dismissImportResult,
            icon = { Icon(Icons.Outlined.CheckCircle, null,
                tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Import complete") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Imported ${result.count} links from ${result.source}.")
                    if (result.folders.isNotEmpty())
                        Text("${result.folders.size} folders also imported.")
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
                                Icon(Icons.Outlined.Info, null,
                                    Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text(
                                    "${state.duplicateCount} duplicate links skipped.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::dismissImportResult) { Text("Done") }
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
                Text(if (editing) "Cancel" else if (currentKey.isBlank()) "Add" else "Edit")
            }
        }

        if (editing) {
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                placeholder = { Text("Paste API key…") },
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