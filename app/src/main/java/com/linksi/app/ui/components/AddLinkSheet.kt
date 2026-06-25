package com.linksi.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.linksi.app.domain.model.Folder
import com.linksi.app.domain.model.Link
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLinkSheet(
    folders: List<Folder>,
    allTags: List<String> = emptyList(),
    isFetchingMetadata: Boolean = false,
    onDismiss: () -> Unit,
    onCreateFolder: (String, String, String) -> Unit = { _, _, _ -> },
    onConfirm: (
        url: String,
        folderId: Long?,
        reminderAt: Long?,
        note: String,
        tags: List<String>,
        expiresAt: Long?,
        titleOverride: String,
        descriptionOverride: String,
        previewImageOverride: String
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var url by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var reminderAt by remember { mutableStateOf<Long?>(null) }
    var expiresAt by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var previewImageUrl by remember { mutableStateOf("") }
    var imageLoadFailed by remember { mutableStateOf(false) }
    var isFetchingPreview by remember { mutableStateOf(false) }

    var showFolderPicker by remember { mutableStateOf(false) }
    var showReminderSheet by remember { mutableStateOf(false) }
    var showExpirySheet by remember { mutableStateOf(false) }
    var showNoteSheet by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showCreateFolder by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            previewImageUrl = it.toString()
            imageLoadFailed = false
        }
    }

    val tempLink = remember(editTitle, editDescription, previewImageUrl, url) {
        Link(
            id = 0,
            url = url,
            title = editTitle,
            description = editDescription,
            previewImageUrl = previewImageUrl,
            faviconUrl = "",
            domain = url.removePrefix("https://").removePrefix("http://")
                .removePrefix("www.").substringBefore("/")
        )
    }

    LaunchedEffect(url) {
        if (url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))) {
            kotlinx.coroutines.delay(800) // debounce
            if (editTitle.isBlank()) { // only if not already set
                isFetchingPreview = true
                try {
                    val meta = com.linksi.app.utils.MetadataFetcher.fetch(url)
                    editTitle = meta.title.ifBlank {
                        url.removePrefix("https://").removePrefix("http://")
                            .removePrefix("www.").substringBefore("/")
                    }
                    editDescription = meta.description
                    previewImageUrl = meta.previewImageUrl
                } catch (e: Exception) { }
                isFetchingPreview = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.width(40.dp).height(4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                ) {}
            }

            // Header
            Text(
                "Save Link",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── URL input ─────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            placeholder = { Text("Paste URL here…") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Link, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Animated paste/clear button
                        val isTextEmpty = url.isBlank()

                        val rotation by animateFloatAsState(
                            targetValue = if (isTextEmpty) 0f else 45f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "icon_rotation"
                        )

                        val buttonColor by animateColorAsState(
                            targetValue = if (isTextEmpty)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                            animationSpec = tween(300),
                            label = "button_color"
                        )

                        val scale by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "button_scale"
                        )

                        Surface(
                            shape = CircleShape,
                            color = buttonColor.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(40.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .clickable {
                                    if (isTextEmpty) {
                                        // Paste from clipboard
                                        val clipText = clipboardManager.getText()?.text ?: ""
                                        if (clipText.isNotBlank()) {
                                            url = clipText
                                        }
                                    } else {
                                        // Clear
                                        url = ""
                                        editTitle = ""
                                        editDescription = ""
                                        previewImageUrl = ""
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AnimatedContent(
                                    targetState = isTextEmpty,
                                    transitionSpec = {
                                        (scaleIn(
                                            initialScale = 0.5f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) + fadeIn(tween(200))) togetherWith
                                                (scaleOut(
                                                    targetScale = 0.5f,
                                                    animationSpec = tween(150)
                                                ) + fadeOut(tween(150)))
                                    },
                                    label = "paste_clear_icon"
                                ) { empty ->
                                    if (empty) {
                                        Icon(
                                            Icons.Outlined.ContentPaste,
                                            contentDescription = "Paste",
                                            Modifier.size(18.dp),
                                            tint = buttonColor
                                        )
                                    } else {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Clear",
                                            Modifier.size(18.dp),
                                            tint = buttonColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }


                // ── Save to folder ────────────────────────────
                val selectedFolder = folders.find { it.id == selectedFolderId }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showFolderPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            if (selectedFolder != null) Icons.Outlined.Folder
                            else Icons.Outlined.FolderOpen,
                            null, Modifier.size(22.dp),
                            tint = if (selectedFolder != null)
                                Color(android.graphics.Color.parseColor(selectedFolder.color))
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Save to folder",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium)
                            Text(selectedFolder?.name ?: "No folder selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Outlined.ChevronRight, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // ── Edit link ─────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Edit link — takes most space
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { if (url.isNotBlank()) showEditSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Edit, null,
                                Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Edit link",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    editTitle.ifBlank { "Add title, description, image" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Refresh metadata — square button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (url.isNotBlank()) {
                                    scope.launch {
                                        isFetchingPreview = true
                                        editTitle = ""
                                        editDescription = ""
                                        previewImageUrl = ""
                                        try {
                                            val meta = com.linksi.app.utils.MetadataFetcher.fetch(url.trim())
                                            editTitle = meta.title.ifBlank {
                                                url.removePrefix("https://")
                                                    .removePrefix("http://")
                                                    .removePrefix("www.")
                                                    .substringBefore("/")
                                            }
                                            editDescription = meta.description
                                            previewImageUrl = meta.previewImageUrl
                                        } catch (e: Exception) { }
                                        isFetchingPreview = false
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isFetchingPreview) {
                                CircularProgressIndicator(
                                    Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Refresh, null,
                                    Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ── Reminder + Expiry row ─────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (reminderAt != null)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { showReminderSheet = true }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.Notifications, null, Modifier.size(16.dp),
                                    tint = if (reminderAt != null)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Reminder", style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (reminderAt != null)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface)
                            }
                            Text(
                                reminderAt?.let { dateFormatter.format(Date(it)) } ?: "Not set",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (reminderAt != null)
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (expiresAt != null)
                            MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { showExpirySheet = true }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.Timer, null, Modifier.size(16.dp),
                                    tint = if (expiresAt != null)
                                        MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Expiry", style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (expiresAt != null)
                                        MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onSurface)
                            }
                            Text(
                                expiresAt?.let {
                                    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(it))
                                } ?: "Not set",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (expiresAt != null)
                                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Note + Tags row ───────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (note.isNotBlank())
                            MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { showNoteSheet = true }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.Notes, null, Modifier.size(16.dp),
                                    tint = if (note.isNotBlank())
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Note", style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (note.isNotBlank())
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurface)
                            }
                            Text(note.ifBlank { "Add a note" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (note.isNotBlank())
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (tags.isNotEmpty())
                            Color(0xFF22C55E).copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { showTagSheet = true }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.Tag, null, Modifier.size(16.dp),
                                    tint = if (tags.isNotEmpty()) Color(0xFF22C55E)
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Tags", style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (tags.isNotEmpty()) Color(0xFF22C55E)
                                    else MaterialTheme.colorScheme.onSurface)
                            }
                            Text(
                                if (tags.isEmpty()) "Add tags"
                                else tags.joinToString(", ") { "#$it" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (tags.isNotEmpty())
                                    Color(0xFF22C55E).copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Fetching indicator ────────────────────────
                AnimatedVisibility(visible = isFetchingMetadata) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                        Text("Fetching preview…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Save button ───────────────────────────────
                Button(
                    onClick = {
                        if (url.isNotBlank()) {
                            onConfirm(
                                url.trim(), selectedFolderId, reminderAt,
                                note, tags, expiresAt,
                                editTitle.trim(), editDescription.trim(), previewImageUrl
                            )
                        }
                    },
                    enabled = url.isNotBlank() && !isFetchingMetadata,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Bookmark, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Link", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // Sub sheets
    if (showEditSheet) {
        LinkEditSheet(
            link = tempLink,
            folders = folders,
            onDismiss = { showEditSheet = false },
            onSave = { updated ->
                editTitle = updated.title
                editDescription = updated.description
                previewImageUrl = updated.previewImageUrl
                showEditSheet = false
            }
        )
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            folders = folders,
            currentFolderId = selectedFolderId,
            onSelect = { folderId -> selectedFolderId = folderId; showFolderPicker = false },
            onDismiss = { showFolderPicker = false }
        )
    }

    if (showCreateFolder) {
        AddFolderDialog(
            onDismiss = { showCreateFolder = false },
            onConfirm = { name, icon, color ->
                onCreateFolder(name, icon, color)
                showCreateFolder = false
            }
        )
    }

    if (showReminderSheet) {
        ReminderBottomSheet(
            currentReminder = reminderAt,
            onSet = { time -> reminderAt = time; showReminderSheet = false },
            onDismiss = { showReminderSheet = false }
        )
    }

    if (showExpirySheet) {
        ExpiryBottomSheet(
            currentExpiry = expiresAt,
            onSet = { time -> expiresAt = time; showExpirySheet = false },
            onDismiss = { showExpirySheet = false }
        )
    }

    if (showNoteSheet) {
        NoteBottomSheet(
            currentNote = note,
            onSave = { n -> note = n; showNoteSheet = false },
            onDismiss = { showNoteSheet = false }
        )
    }

    if (showTagSheet) {
        TagManagerSheet(
            currentTags = tags,
            allExistingTags = allTags,
            onSave = { t -> tags = t; showTagSheet = false },
            onDismiss = { showTagSheet = false }
        )
    }
}