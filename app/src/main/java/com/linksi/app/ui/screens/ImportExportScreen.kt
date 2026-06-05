package com.linksi.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    onBack: () -> Unit,
    exportJsonLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    exportCsvLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    exportFileName: (String) -> String
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import & Export") },
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Export
            item { SectionHeader("Export", Icons.Outlined.FileDownload) }
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

            // Import
            item { SectionHeader("Import", Icons.Outlined.FileUpload) }
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
                        subtitle = "Import from Chrome, Firefox, Safari HTML export",
                        onClick = { importLauncher.launch(arrayOf("text/html", "text/plain", "*/*")) }
                    )
                }
            }

            // Browser instructions
            item { SectionHeader("How to export from browsers", Icons.Outlined.Info) }
            item {
                SettingsCard {
                    BrowserInstructionItem(
                        browser = "Chrome",
                        steps = "Menu (⋮) → Bookmarks → Bookmark manager → Menu (⋮) → Export bookmarks"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    BrowserInstructionItem(
                        browser = "Firefox",
                        steps = "Menu → Bookmarks → Manage bookmarks → Import & Backup → Export HTML"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    BrowserInstructionItem(
                        browser = "Safari (Mac)",
                        steps = "File → Export Bookmarks → save as HTML → transfer to phone"
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}