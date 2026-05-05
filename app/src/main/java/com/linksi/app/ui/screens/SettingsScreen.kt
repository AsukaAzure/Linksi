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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linksi.app.utils.exportFileName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    BackHandler { onBack() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Export JSON launcher
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportJson(context, it) } }

    // Export CSV launcher
    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportCsv(context, it) } }

    // Import launcher — accepts JSON and HTML
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
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Export section ────────────────────────────────
            item {
                Text("Export", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.FileDownload,
                        title = "Export as Linksi JSON",
                        subtitle = "Full backup — reimport on any device with Linksi",
                        onClick = { exportJsonLauncher.launch(exportFileName("json")) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Outlined.TableChart,
                        title = "Export as CSV",
                        subtitle = "Open in Excel, Google Sheets, or any spreadsheet app",
                        onClick = { exportCsvLauncher.launch(exportFileName("csv")) }
                    )
                }
            }

            // ── Import section ────────────────────────────────
            item {
                Text("Import", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.FileUpload,
                        title = "Import Linksi backup",
                        subtitle = "Restore from a .json file exported from Linksi",
                        onClick = { importLauncher.launch(arrayOf("application/json")) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Outlined.Language,
                        title = "Import browser bookmarks",
                        subtitle = "Import from Chrome, Firefox, Safari — export bookmarks as HTML first",
                        onClick = { importLauncher.launch(arrayOf("text/html", "text/plain", "*/*")) }
                    )
                }
            }

            // ── How to export from browser ────────────────────
            item {
                Text("How to export browser bookmarks \n(desktop browser)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                SettingsCard {
                    BrowserInstructionItem(
                        browser = "Chrome",
                        steps = "Menu (⋮) → Bookmarks → Bookmark manager → Menu (⋮) → Export bookmarks \n or \n Press Ctrl+Shift+O To open Bookmark Manager"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    BrowserInstructionItem(
                        browser = "Firefox",
                        steps = "Menu → Bookmarks → Manage bookmarks → Import & Backup → Export HTML \n or \n Press Ctrl+Shift+O To open Bookmark Manager"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    BrowserInstructionItem(
                        browser = "Safari (Mac)",
                        steps = "File → Export Bookmarks → save as HTML → transfer to phone"
                    )
                }
            }

            // ── Stats ─────────────────────────────────────────
            item {
                Text("Stats", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp))
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
                        "Made with ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "❤️",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        " by ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Anush",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AsukaAzure/"))
                            context.startActivity(intent)
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

        }
    }

    // Import result dialog
    state.importResult?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::dismissImportResult,
            icon = { Icon(Icons.Outlined.CheckCircle, null) },
            title = { Text("Import complete") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Successfully imported ${result.count} links from ${result.source}.")
                    if (result.folders.isNotEmpty())
                        Text("${result.folders.size} folders also imported.")
                }
            },
            confirmButton = {
                Button(onClick = viewModel::dismissImportResult) { Text("Done") }
            }
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
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        },
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
        Text(value, style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}