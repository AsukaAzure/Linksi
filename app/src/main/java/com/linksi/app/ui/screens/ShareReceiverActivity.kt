package com.linksi.app.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linksi.app.ui.theme.LinksTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.linksi.app.ui.components.*
import com.linksi.app.utils.MetadataFetcher
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedUrl = extractUrl(intent)

        if (sharedUrl == null) {
            finish()
            return
        }

        setContent {
            LinksTheme {
                ShareReceiverSheet(
                    url = sharedUrl,
                    viewModel = viewModel,
                    onDismiss = { finish() },
                    onSaved = { finish() }
                )
            }
        }
    }

    private fun extractUrl(intent: Intent?): String? {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            return intent.getStringExtra(Intent.EXTRA_TEXT)
                ?.trim()
                ?.let { extractUrlFromText(it) }
        }
        if (intent?.action == Intent.ACTION_VIEW) {
            return intent.dataString
        }
        return null
    }

    private fun extractUrlFromText(text: String): String {
        val urlPattern = Regex("https?://[^\\s]+")
        return urlPattern.find(text)?.value ?: text
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareReceiverSheet(
    url: String,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Form state
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var reminderAt by remember { mutableStateOf<Long?>(null) }
    var expiresAt by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var previewImageUrl by remember { mutableStateOf("") }
    var imageLoadFailed by remember { mutableStateOf(false) }

    // Sub sheet visibility
    var showFolderPicker by remember { mutableStateOf(false) }
    var showReminderSheet by remember { mutableStateOf(false) }
    var showExpirySheet by remember { mutableStateOf(false) }
    var showNoteSheet by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showCreateFolder by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dateFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    // Show sheet on launch
    LaunchedEffect(Unit) { sheetState.show() }

    // Fetch metadata silently in background
    LaunchedEffect(url) {
        try {
            val meta = MetadataFetcher.fetch(url)
            editTitle = meta.title.ifBlank { extractDomainFromUrl(url) }
            editDescription = meta.description
            previewImageUrl = meta.previewImageUrl
        } catch (e: Exception) {
            editTitle = extractDomainFromUrl(url)
        }
    }

    // Temp link for edit sheet
    val tempLink = remember(editTitle, editDescription, previewImageUrl) {
        com.linksi.app.domain.model.Link(
            id = 0,
            url = url,
            title = editTitle,
            description = editDescription,
            previewImageUrl = previewImageUrl,
            faviconUrl = "https://www.google.com/s2/favicons?domain=${extractDomainFromUrl(url)}&sz=64",
            domain = extractDomainFromUrl(url)
        )
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
                .verticalScroll(rememberScrollState())
        ) {
            // ── Drag handle ───────────────────────────────────
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

            // ── Preview image ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                when {
                    editTitle.isBlank() && previewImageUrl.isBlank() -> {
                        // Fetching metadata
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
                                Text(
                                    "Fetching preview…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    previewImageUrl.isNotBlank() && !imageLoadFailed -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(previewImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onError = { imageLoadFailed = true }
                        )
                    }
                    else -> {
                        // No image — gradient with domain
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                extractDomainFromUrl(url),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Main options column ───────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Save to folder ────────────────────────────
                val selectedFolder = state.folders.find { it.id == selectedFolderId }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
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
                            null,
                            Modifier.size(22.dp),
                            tint = if (selectedFolder != null)
                                Color(android.graphics.Color.parseColor(selectedFolder.color))
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Save to folder",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                selectedFolder?.name ?: "No folder selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Outlined.ChevronRight, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Edit link ─────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditSheet = true }
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
                                editTitle.ifBlank { "Title, description, image" },
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

                // ── Reminder + Expiry row ─────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reminder
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (reminderAt != null)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showReminderSheet = true }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Notifications, null,
                                    Modifier.size(16.dp),
                                    tint = if (reminderAt != null)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Reminder",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (reminderAt != null)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
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

                    // Expiry
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (expiresAt != null)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showExpirySheet = true }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Timer, null,
                                    Modifier.size(16.dp),
                                    tint = if (expiresAt != null)
                                        MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Expiry",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (expiresAt != null)
                                        MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
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
                    // Note
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (note.isNotBlank())
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showNoteSheet = true }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Notes, null,
                                    Modifier.size(16.dp),
                                    tint = if (note.isNotBlank())
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Note",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (note.isNotBlank())
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                note.ifBlank { "Add a note" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (note.isNotBlank())
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Tags
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (tags.isNotEmpty())
                            Color(0xFF22C55E).copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTagSheet = true }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Tag, null,
                                    Modifier.size(16.dp),
                                    tint = if (tags.isNotEmpty()) Color(0xFF22C55E)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Tags",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (tags.isNotEmpty()) Color(0xFF22C55E)
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                if (tags.isEmpty()) "Add tags"
                                else tags.joinToString(", ") { "#$it" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (tags.isNotEmpty())
                                    Color(0xFF22C55E).copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Duplicate warning ─────────────────────────
                AnimatedVisibility(visible = state.snackbarMessage == "Link already saved") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Warning, null,
                                Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "This link is already saved in Linksi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Save button ───────────────────────────────
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.dismissSnackbar()
                            viewModel.addLink(
                                url = url,
                                folderId = selectedFolderId,
                                reminderAt = reminderAt,
                                titleOverride = editTitle.trim(),
                                descriptionOverride = editDescription.trim(),
                                previewImageOverride = previewImageUrl,
                                note = note,
                                tags = tags,
                                expiresAt = expiresAt
                            )
                            snapshotFlow { state.snackbarMessage }
                                .first { it != null }
                            if (state.snackbarMessage == "Link already saved") {
                                // Stay open — warning shows
                            } else {
                                sheetState.hide()
                                onSaved()
                                viewModel.dismissSnackbar()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !state.isFetchingMetadata,
                    colors = if (state.snackbarMessage == "Link already saved")
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    else ButtonDefaults.buttonColors()
                ) {
                    if (state.isFetchingMetadata) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else if (state.snackbarMessage == "Link already saved") {
                        Icon(Icons.Outlined.Warning, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Already Saved")
                    } else {
                        Icon(Icons.Filled.Bookmark, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Link", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ── Sub sheets ────────────────────────────────────────────

    if (showEditSheet) {
        LinkEditSheet(
            link = tempLink,
            folders = state.folders,
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
            folders = state.folders,
            currentFolderId = selectedFolderId,
            onSelect = { folderId ->
                selectedFolderId = folderId
                showFolderPicker = false
            },
            onDismiss = { showFolderPicker = false }
        )
    }

    if (showCreateFolder) {
        AddFolderDialog(
            onDismiss = { showCreateFolder = false },
            onConfirm = { name, icon, color ->
                viewModel.addFolder(name, icon, color)
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
            allExistingTags = state.allTags,
            onSave = { t -> tags = t; showTagSheet = false },
            onDismiss = { showTagSheet = false }
        )
    }
}

private fun extractDomainFromUrl(url: String): String {
    return try {
        url.removePrefix("https://").removePrefix("http://")
            .removePrefix("www.").substringBefore("/")
    } catch (e: Exception) {
        url
    }
}