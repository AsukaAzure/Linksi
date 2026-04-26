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

// ─────────────────────────────────────────────────────────────
// Add Link Dialog
// ─────────────────────────────────────────────────────────────
@Composable
fun AddLinkDialog(
    folders: List<Folder>,
    isFetchingMetadata: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (url: String, folderId: Long?, tags: List<String>) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }

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
                if (folders.isNotEmpty()) {
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
                                label = { Text("${folder.emoji} ${folder.name}") }
                            )
                        }
                    }
                }

                // Tags
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("Tags (optional)") },
                    placeholder = { Text("press Enter to add") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val tag = tagInput.trim().lowercase().replace(" ", "-")
                        if (tag.isNotBlank() && !tags.contains(tag)) {
                            tags = tags + tag
                        }
                        tagInput = ""
                    }),
                    modifier = Modifier.fillMaxWidth()
                )

                if (tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tags) { tag ->
                            InputChip(
                                selected = false,
                                onClick = { tags = tags - tag },
                                label = { Text("#$tag") },
                                trailingIcon = { Icon(Icons.Filled.Close, null, Modifier.size(14.dp)) }
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
                onClick = { if (url.isNotBlank()) onConfirm(url.trim(), selectedFolderId, tags) },
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

// ─────────────────────────────────────────────────────────────
// Add Folder Dialog
// ─────────────────────────────────────────────────────────────
val FOLDER_EMOJIS = listOf("📁","🗂","📂","📚","🎵","🎬","💼","🛍","🔬","💡","🎮","📰","✈","🍔","💪","🧠","🎨","💻","🏠","⭐")
val FOLDER_COLORS = listOf("#6366F1","#8B5CF6","#EC4899","#EF4444","#F59E0B","#10B981","#06B6D4","#3B82F6","#84CC16","#F97316")

@Composable
fun AddFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📁") }
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(FOLDER_EMOJIS) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (emoji == selectedEmoji) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji)
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
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedEmoji, selectedColor) },
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
                        leadingContent = { Text(folder.emoji) },
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
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(link.tags) }

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
                                label = { Text("${folder.emoji} ${folder.name}") })
                        }
                    }
                }
                OutlinedTextField(
                    value = tagInput, onValueChange = { tagInput = it },
                    label = { Text("Add tag") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val tag = tagInput.trim().lowercase()
                        if (tag.isNotBlank() && !tags.contains(tag)) tags = tags + tag
                        tagInput = ""
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
                if (tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tags) { tag ->
                            InputChip(selected = false, onClick = { tags = tags - tag },
                                label = { Text("#$tag") },
                                trailingIcon = { Icon(Icons.Filled.Close, null, Modifier.size(14.dp)) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(link.copy(url = url.trim(), title = title.trim(),
                    description = description.trim(), folderId = selectedFolderId, tags = tags))
            }, enabled = url.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}