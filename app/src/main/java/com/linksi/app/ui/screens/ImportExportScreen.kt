package com.linksi.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.linksi.app.R
import com.linksi.app.ui.components.SimpleProgressBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    exportJsonLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    exportCsvLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    exportHtmlLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    exportFileName: (String) -> String,
    onToggleIncludeLocked: (Boolean) -> Unit,
    onMinimizeImport: () -> Unit,
    onDismissImportResult: () -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_export_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
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
            item { SectionHeader(stringResource(R.string.export), Icons.Outlined.FileDownload) }
            item {
                SettingsCard {
                    SettingsItem(
                        icon = if (state.exportIncludeLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                        title = stringResource(R.string.include_locked_links),
                        subtitle = stringResource(R.string.include_locked_links_subtitle),
                        onClick = { onToggleIncludeLocked(!state.exportIncludeLocked) },
                        trailingContent = {
                            Switch(
                                checked = state.exportIncludeLocked,
                                onCheckedChange = onToggleIncludeLocked
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Outlined.FileDownload,
                        title = stringResource(R.string.export_json_title),
                        subtitle = stringResource(R.string.export_json_subtitle),
                        onClick = { exportJsonLauncher.launch(exportFileName("json")) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Outlined.TableChart,
                        title = stringResource(R.string.export_csv_title),
                        subtitle = stringResource(R.string.export_csv_subtitle),
                        onClick = { exportCsvLauncher.launch(exportFileName("csv")) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Outlined.Language,
                        title = stringResource(R.string.export_html_title),
                        subtitle = stringResource(R.string.export_html_subtitle),
                        onClick = { exportHtmlLauncher.launch(exportFileName("html")) }
                    )
                }
            }

            // Import
            item { SectionHeader(stringResource(R.string.import_action), Icons.Outlined.FileUpload) }
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.FileUpload,
                        title = stringResource(R.string.import_json_title),
                        subtitle = stringResource(R.string.import_json_subtitle),
                        onClick = { importLauncher.launch(arrayOf("application/json")) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Outlined.Language,
                        title = stringResource(R.string.import_browser_title),
                        subtitle = stringResource(R.string.import_browser_subtitle),
                        onClick = { importLauncher.launch(arrayOf("text/html", "text/plain", "*/*")) }
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
        if (state.isImporting && !state.isImportMinimized) {
            AlertDialog(
                onDismissRequest = { /* non-dismissable */ },
                title = { Text(state.importPhase) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.importTotal > 0) {
                            val progress = state.importProgress.toFloat() / state.importTotal
                            SimpleProgressBar(
                                progress = progress,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${state.importProgress} / ${state.importTotal}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${(state.importProgress.toFloat() / state.importTotal * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            SimpleProgressBar(
                                progress = null,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Text(
                            when (state.importPhase) {
                                stringResource(R.string.fetching_metadata_phase) ->
                                    stringResource(R.string.fetching_metadata_desc)
                                else -> stringResource(R.string.please_wait)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onMinimizeImport) {
                        Text(stringResource(R.string.minimize))
                    }
                }
            )
        }
    }
}
