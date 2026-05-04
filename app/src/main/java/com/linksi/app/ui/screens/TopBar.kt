package com.linksi.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.linksi.app.domain.model.Folder
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.linksi.app.ui.components.iconFromName

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
    val selectedFolder = folders.find { it.id == selectedFolderId }

    TopAppBar(
        title = {
            if (selectedFolder != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        iconFromName(selectedFolder.icon),
                        null,
                        Modifier.size(20.dp),
                        tint = Color(android.graphics.Color.parseColor(selectedFolder.color))
                    )
                    Text(selectedFolder.name, style = MaterialTheme.typography.titleLarge)
                }
            } else {
                Text("Linksi", style = MaterialTheme.typography.titleLarge)
            }
        },
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
