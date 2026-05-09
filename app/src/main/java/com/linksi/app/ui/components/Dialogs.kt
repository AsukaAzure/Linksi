package com.linksi.app.ui.components

import android.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.room.util.TableInfo
import com.linksi.app.domain.model.*
import androidx.compose.material.icons.outlined.*

data class FolderIconOption(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

val FOLDER_ICONS = listOf(
    FolderIconOption("folder",   Icons.Outlined.Folder),
    FolderIconOption("work",     Icons.Outlined.Work),
    FolderIconOption("bookmark", Icons.Outlined.Bookmark),
    FolderIconOption("star",     Icons.Outlined.Star),
    FolderIconOption("heart",    Icons.Outlined.FavoriteBorder),
    FolderIconOption("code",     Icons.Outlined.Code),
    FolderIconOption("school",   Icons.Outlined.School),
    FolderIconOption("movie",    Icons.Outlined.Movie),
    FolderIconOption("music",    Icons.Outlined.MusicNote),
    FolderIconOption("shopping", Icons.Outlined.ShoppingCart),
    FolderIconOption("travel",   Icons.Outlined.Flight),
    FolderIconOption("food",     Icons.Outlined.Restaurant),
    FolderIconOption("health",   Icons.Outlined.HealthAndSafety),
    FolderIconOption("news",     Icons.Outlined.Newspaper),
    FolderIconOption("game",     Icons.Outlined.SportsEsports),
    FolderIconOption("finance",  Icons.Outlined.AttachMoney),
    FolderIconOption("science",  Icons.Outlined.Science),
    FolderIconOption("design",   Icons.Outlined.Brush),
    FolderIconOption("home",     Icons.Outlined.Home),
    FolderIconOption("photo",    Icons.Outlined.Photo),
)

fun iconFromName(name: String) =
    FOLDER_ICONS.find { it.name == name }?.icon ?: Icons.Outlined.Folder

// ─────────────────────────────────────────────────────────────
// Add Link Dialog
// ─────────────────────────────────────────────────────────────
@Composable
fun AddLinkDialog(
    folders: List<Folder>,
    isFetchingMetadata: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (url: String, folderId: Long?) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Link, null) },
        title = { Text("Save Link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    placeholder = { Text("https://example.com") },
                    leadingIcon = { Icon(Icons.Outlined.Link, null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Folder picker
                // AFTER
                Text("Folder", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedFolderId == null,
                            onClick = { selectedFolderId = null },
                            label = { Text("None") }
                        )
                    }
                    items(folders) { folder ->
                        FilterChip(
                            selected = selectedFolderId == folder.id,
                            onClick = { selectedFolderId = folder.id },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        iconFromName(folder.icon), null,
                                        Modifier.size(14.dp),
                                        tint = Color(android.graphics.Color.parseColor(folder.color))
                                    )
                                    Text(folder.name)
                                }
                            }
                        )
                    }
                    if (folders.isEmpty()) {
                        item {
                            Text(
                                "No folders yet — create one first",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                if (isFetchingMetadata) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Fetching preview…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (url.isNotBlank()) onConfirm(url.trim(), selectedFolderId) },
                enabled = url.isNotBlank() && !isFetchingMetadata
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

val FOLDER_COLORS = listOf("#6366F1","#8B5CF6","#EC4899","#EF4444","#F59E0B","#10B981","#06B6D4","#3B82F6","#84CC16","#F97316")

@Composable
fun AddFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("folder") }
    var selectedColor by remember { mutableStateOf(FOLDER_COLORS[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.CreateNewFolder, null) },
        title = { Text("New Folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icon", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FOLDER_ICONS) { option ->
                        Surface(
                            shape = CircleShape,
                            color = if (selectedIcon == option.name)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { selectedIcon = option.name }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    option.icon, null,
                                    Modifier.size(20.dp),
                                    tint = if (selectedIcon == option.name)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FOLDER_COLORS) { color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .then(
                                    if (color == selectedColor)
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedIcon, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────────────────────
// Folder Picker Dialog
// ─────────────────────────────────────────────────────────────
@Composable
fun FolderPickerDialog(
    folders: List<Folder>,
    currentFolderId: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Folder") },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("No folder") },
                    leadingContent = { Icon(Icons.Outlined.FolderOff, null) },
                    trailingContent = { if (currentFolderId == null) Icon(Icons.Filled.Check, null) },
                    modifier = Modifier.clickable { onSelect(null) }
                )
                HorizontalDivider()
                folders.forEach { folder ->
                    ListItem(
                        headlineContent = { Text(folder.name) },
                        leadingContent = { Icon(iconFromName(folder.icon), null, Modifier.size(16.dp),
                            tint = Color(android.graphics.Color.parseColor(folder.color))) },
                        trailingContent = { if (folder.id == currentFolderId) Icon(Icons.Filled.Check, null) },
                        modifier = Modifier.clickable { onSelect(folder.id) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────────────────────
// Sort Bottom Sheet
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: SortOption,
    onSortSelect: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 32.dp)) {
            Text(
                "Sort by",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            SortOption.values().forEach { option ->
                ListItem(
                    headlineContent = { Text(option.label()) },
                    leadingContent = { Icon(option.icon(), null) },
                    trailingContent = { if (option == currentSort) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { onSortSelect(option) }
                )
            }
        }
    }
}

private fun SortOption.label() = when (this) {
    SortOption.DATE_NEWEST -> "Newest first"
    SortOption.DATE_OLDEST -> "Oldest first"
    SortOption.TITLE_AZ    -> "Title A → Z"
    SortOption.TITLE_ZA    -> "Title Z → A"
    SortOption.DOMAIN      -> "By domain"
}

private fun SortOption.icon() = when (this) {
    SortOption.DATE_NEWEST, SortOption.DATE_OLDEST -> Icons.Outlined.Schedule
    SortOption.TITLE_AZ, SortOption.TITLE_ZA       -> Icons.Outlined.SortByAlpha
    SortOption.DOMAIN                               -> Icons.Outlined.Language
}

@Composable
fun EditLinkDialog(
    link: Link,
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onConfirm: (Link) -> Unit
) {
    var url by remember { mutableStateOf(link.url) }
    var title by remember { mutableStateOf(link.title) }
    var description by remember { mutableStateOf(link.description) }
    var selectedFolderId by remember { mutableStateOf(link.folderId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Edit, null) },
        title = { Text("Edit Link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("URL") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (folders.isNotEmpty()) {
                    Text("Folder", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(selected = selectedFolderId == null,
                                onClick = { selectedFolderId = null },
                                label = { Text("None") })
                        }
                        items(folders) {folder ->
                            FilterChip(selected = selectedFolderId == folder.id,
                                onClick = { selectedFolderId = folder.id },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(iconFromName(folder.icon), null, Modifier.size(14.dp),
                                            tint = Color(android.graphics.Color.parseColor(folder.color)))
                                        Text(folder.name)
                                    } })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(link.copy(url = url.trim(), title = title.trim(),
                    description = description.trim(), folderId = selectedFolderId))
            }, enabled = url.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditFolderDialog(
    folder: Folder,
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf(folder.name) }
    var selectedIcon by remember { mutableStateOf(folder.icon) }
    var selectedColor by remember { mutableStateOf(folder.color) }
    var iconSearch by remember { mutableStateOf("") }

    val filteredIcons = remember(iconSearch) {
        if (iconSearch.isBlank()) FOLDER_ICONS
        else FOLDER_ICONS.filter { it.name.contains(iconSearch, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Edit, null) },
        title = { Text("Edit Folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icon", style = MaterialTheme.typography.labelMedium)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredIcons) { option ->
                        Surface(
                            shape = CircleShape,
                            color = if (selectedIcon == option.name)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .clickable { selectedIcon = option.name }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    option.icon, null,
                                    Modifier.size(20.dp),
                                    tint = if (selectedIcon == option.name)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FOLDER_COLORS) { color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .then(
                                    if (color == selectedColor)
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim(), selectedIcon, selectedColor)
                },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}