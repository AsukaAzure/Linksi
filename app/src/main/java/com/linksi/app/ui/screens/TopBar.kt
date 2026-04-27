package com.linksi.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.linksi.app.domain.model.Folder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinksTopBar(
    selectedFolderId: Long?,
    folders: List<Folder>,
    viewMode: ViewMode,
    onViewModeToggle: () -> Unit,
    onSortClick: () -> Unit,
    onAddFolder: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val title = folders.find { it.id == selectedFolderId }?.let {
        "${it.emoji} ${it.name}"
    } ?: "Linksi"

    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        actions = {
            IconButton(onClick = onViewModeToggle) {
                Icon(
                    if (viewMode == ViewMode.LIST) Icons.Outlined.GridView else Icons.Outlined.ViewList,
                    "Toggle view"
                )
            }
            IconButton(onClick = onSortClick) {
                Icon(Icons.Outlined.Sort, "Sort")
            }
            IconButton(onClick = onAddFolder) {
                Icon(Icons.Outlined.CreateNewFolder, "New folder")
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Settings, "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
