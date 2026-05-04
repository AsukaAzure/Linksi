package com.linksi.app.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linksi.app.ui.theme.LinksTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import com.linksi.app.ui.components.AddFolderDialog
import com.linksi.app.ui.components.iconFromName

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
        // Try to extract URL from shared text that may include extra info
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
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
//    var isSaving by remember { mutableStateOf(false) }

    // Sheet drag state
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()

//    LaunchedEffect(state.isFetchingMetadata) {
//        if (isSaving && !state.isFetchingMetadata) onSaved()
//    }

    // Animate in on launch
    LaunchedEffect(Unit) {
        sheetState.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            // Draggable handle — pull down to dismiss
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ) {}
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Bookmark, "Save",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text("Save to Linksi", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider()

            // Folder selection
            var showCreateFolder by remember { mutableStateOf(false) }

            Text("Save to folder", style = MaterialTheme.typography.labelLarge)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFolderId == null,
                        onClick = { selectedFolderId = null },
                        label = { Text("📥 Inbox") }
                    )
                }
                items(state.folders) { folder ->
                    FilterChip(
                        selected = selectedFolderId == folder.id,
                        onClick = { selectedFolderId = folder.id },
                        label = { // AFTER
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    iconFromName(folder.icon), null, Modifier.size(14.dp),
                                    tint = Color(android.graphics.Color.parseColor(folder.color))
                                )
                                Text(folder.name)
                            } }
                    )
                }
                item {
                    // New folder button at end of row
                    FilterChip(
                        selected = false,
                        onClick = { showCreateFolder = true },
                        label = { Text("New folder") },
                        leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(16.dp)) }
                    )
                }
            }

// Create folder dialog
            if (showCreateFolder) {
                AddFolderDialog(
                    onDismiss = { showCreateFolder = false },
                    onConfirm = { name, emoji, color ->
                        viewModel.addFolder(name, emoji, color)
                        showCreateFolder = false
                    }
                )
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // Animate sheet down then dismiss
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        scope.launch {
                            viewModel.addLink(url, selectedFolderId)
                            snapshotFlow { state.isFetchingMetadata }
                                .first { it }
                            snapshotFlow { state.isFetchingMetadata }
                                .first { !it }
                            sheetState.hide()
                            onSaved()
                        }
//                        isSaving = true
//                        viewModel.addLink(url, selectedFolderId)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isFetchingMetadata
                ) {
                    if (state.isFetchingMetadata) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Filled.Bookmark, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Link")
                    }
                }
            }
        }
    }
}