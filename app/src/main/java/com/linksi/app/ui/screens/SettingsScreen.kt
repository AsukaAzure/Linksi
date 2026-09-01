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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.materialIcon
import androidx.compose.material3.RadioButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    var showAiOrganizer by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showAiSettings by remember { mutableStateOf(false) }
    var showThemeSettings by remember { mutableStateOf(false) }
    var showSecuritySettings by remember { mutableStateOf(false) }
    var showSecurityAuth by remember { mutableStateOf(false) }
    var showTrashBin by remember { mutableStateOf(false) }

    // Handle system back button
    BackHandler(enabled = !showAiOrganizer && !showImportExport && !showAiSettings && !showThemeSettings && !showSecuritySettings && !showSecurityAuth && !showTrashBin) {
        onBack()
    }

    BackHandler(enabled = showSecurityAuth) {
        showSecurityAuth = false
    }

    // File launchers
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportJson(context, it) } }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportCsv(context, it) } }

    val exportHtmlLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri -> uri?.let { viewModel.exportHtml(context, it) } }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Data & Tools ──────────────────────────────────────
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
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(
                        icon = Icons.Outlined.AutoAwesome,
                        title = stringResource(id = com.linksi.app.R.string.ai_organizer),
                        subtitle = stringResource(id = com.linksi.app.R.string.ai_organizer_subtitle),
                        onClick = { showAiSettings = true },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(
                        icon = Icons.Outlined.DeleteOutline,
                        title = stringResource(id = com.linksi.app.R.string.trash_bin),
                        subtitle = stringResource(id = com.linksi.app.R.string.trash_bin_subtitle),
                        onClick = { showTrashBin = true },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // ── App Preferences ───────────────────────────────────
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Lock,
                        title = stringResource(id = com.linksi.app.R.string.security),
                        subtitle = stringResource(id = com.linksi.app.R.string.app_lock_subtitle),
                        onClick = {
                            if (state.universalLock && state.pin.isNotEmpty()) {
                                showSecurityAuth = true
                            } else {
                                showSecuritySettings = true
                            }
                        },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
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
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = state.useInAppBrowser,
                                onCheckedChange = { viewModel.toggleInAppBrowser(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(
                        icon = Icons.Outlined.Palette,
                        title = stringResource(id = com.linksi.app.R.string.theme),
                        subtitle = when(state.themeMode) {
                            "light" -> stringResource(id = com.linksi.app.R.string.light)
                            "dark" -> stringResource(id = com.linksi.app.R.string.dark)
                            else -> stringResource(id = com.linksi.app.R.string.system)
                        },
                        onClick = { showThemeSettings = true },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(
                        icon = Icons.Outlined.Language,
                        title = stringResource(id = com.linksi.app.R.string.language),
                        subtitle = when(state.selectedLanguage) {
                            "en" -> "English"
                            "es" -> "Spanish"
                            "ru" -> "Russian"
                            "zh" -> "Chinese (Simplified)"
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
                                tint = MaterialTheme.colorScheme.onSurface
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
                                tint = MaterialTheme.colorScheme.onSurface
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
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            // ── Stats ─────────────────────────────────────────────
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
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
                        " ❤️ ",
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

    // ── Theme Settings overlay ──────────────────────────────
    AnimatedVisibility(
        visible = showThemeSettings,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        BackHandler { showThemeSettings = false }
        ThemeSettingsScreen(
            currentThemeMode = state.themeMode,
            useAmoled = state.useAmoled,
            useDynamicColor = state.useDynamicColor,
            showQuickFilters = state.showQuickFilters,
            onThemeSelected = { viewModel.setThemeMode(it) },
            onAmoledToggled = { viewModel.setAmoledEnabled(it) },
            onDynamicColorToggled = { viewModel.setDynamicColorEnabled(it) },
            onQuickFiltersToggled = { viewModel.setShowQuickFilters(it) },
            onBack = { showThemeSettings = false }
        )
    }

    // ── AI Settings overlay ──────────────────────────────────
    AnimatedVisibility(
        visible = showAiSettings,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        BackHandler { 
            if (showAiOrganizer) showAiOrganizer = false 
            else showAiSettings = false 
        }
        AiSettingsScreen(
            onBack = { showAiSettings = false },
            onOpenOrganizer = { showAiOrganizer = true }
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

    // ── Security Settings overlay ─────────────────────────────
    AnimatedVisibility(
        visible = showSecuritySettings,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        BackHandler { showSecuritySettings = false }
        SecuritySettingsScreen(
            onBack = { showSecuritySettings = false }
        )
    }

    // ── Security Auth Overlay ────────────────────────────────
    AnimatedVisibility(
        visible = showSecurityAuth,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LockScreen(
                savedPin = state.pin,
                isBiometricEnabled = state.isBiometricEnabled,
                onUnlock = {
                    showSecurityAuth = false
                    showSecuritySettings = true
                }
            )

            // Close button for auth
            IconButton(
                onClick = { showSecurityAuth = false },
                modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
            ) {
                Icon(Icons.Outlined.Close, stringResource(id = com.linksi.app.R.string.cancel))
            }
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
            exportHtmlLauncher = exportHtmlLauncher,
            importLauncher = importLauncher,
            exportFileName = ::exportFileName,
            onToggleIncludeLocked = viewModel::setExportIncludeLocked,
            onMinimizeImport = viewModel::minimizeImport,
            onDismissImportResult = viewModel::dismissImportResult
        )
    }

    // ── Trash Bin overlay ─────────────────────────────────────
    AnimatedVisibility(
        visible = showTrashBin,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        BackHandler { showTrashBin = false }
        TrashBinScreen(
            onBack = { showTrashBin = false }
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
            tint = MaterialTheme.colorScheme.onSurface
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp) // Pop-up style rounded corners
    ) {
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
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface)
        },
        trailingContent = trailingContent,
        modifier = androidx.compose.ui.Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockDelayPickerSheet(
    currentDelay: Long,
    onDelaySelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Text(
                stringResource(id = com.linksi.app.R.string.lock_delay),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            val options = listOf(
                0L to stringResource(id = com.linksi.app.R.string.immediately),
                60000L to stringResource(id = com.linksi.app.R.string.after_1_minute),
                300000L to stringResource(id = com.linksi.app.R.string.after_5_minutes),
                1800000L to stringResource(id = com.linksi.app.R.string.after_30_minutes)
            )

            options.forEach { (delay, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = {
                        RadioButton(
                            selected = currentDelay == delay,
                            onClick = { onDelaySelected(delay) }
                        )
                    },
                    modifier = Modifier.clickable { onDelaySelected(delay) }
                )
            }
        }
    }
}

@Composable
fun PinSetupDialog(
    onPinSaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (step == 1) stringResource(id = com.linksi.app.R.string.set_pin)
                else stringResource(id = com.linksi.app.R.string.confirm_pin)
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val currentVal = if (step == 1) pin else confirmPin
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index < currentVal.length) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                if (error != null) {
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Simple Numpad Grid
                val numbers = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "delete")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    numbers.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { num ->
                                if (num.isEmpty()) {
                                    Spacer(Modifier.size(48.dp))
                                } else if (num == "delete") {
                                    IconButton(
                                        onClick = {
                                            if (step == 1 && pin.isNotEmpty()) pin = pin.dropLast(1)
                                            else if (step == 2 && confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Outlined.Backspace, null)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                if (step == 1) {
                                                    if (pin.length < 4) pin += num
                                                    if (pin.length == 4) step = 2
                                                } else {
                                                    if (confirmPin.length < 4) confirmPin += num
                                                    if (confirmPin.length == 4) {
                                                        if (pin == confirmPin) {
                                                            onPinSaved(pin)
                                                        } else {
                                                            error = context.getString(com.linksi.app.R.string.pins_dont_match)
                                                            confirmPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(num, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = com.linksi.app.R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(
    currentLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
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
                "ru" to "Russian",
                "zh" to "Chinese (Simplified)"
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
    onSave: (String) -> Unit,
    enabled: Boolean = true
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

    val alpha = if (enabled) 1f else 0.38f

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                providerName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
            if (currentKey.isNotBlank()) {
                Icon(
                    Icons.Outlined.CheckCircle, null,
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                )
            }
            TextButton(
                onClick = { editing = !editing; keyInput = currentKey },
                enabled = enabled
            ) {
                Text(if (editing) stringResource(id = com.linksi.app.R.string.cancel) else if (currentKey.isBlank()) stringResource(id = com.linksi.app.R.string.add) else stringResource(id = com.linksi.app.R.string.edit))
            }
        }

        if (editing && enabled) {
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
        }
    }
}
