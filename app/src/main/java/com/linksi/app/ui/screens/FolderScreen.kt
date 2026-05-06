package com.linksi.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linksi.app.domain.model.Folder
import com.linksi.app.domain.model.Link
import com.linksi.app.ui.components.*
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }

    BackHandler {
        if (selectedFolder != null) selectedFolder = null
        else onBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Folders list ──────────────────────────────────────
        FolderListScreen(
            folders = state.folders,
            onFolderClick = { selectedFolder = it },
            onAddFolder = viewModel::showAddFolderDialog,
            onDeleteFolder = { viewModel.deleteFolder(it) },
            onBack = onBack,
            viewModel = viewModel
        )

        // ── Folder detail slides in from right ────────────────
        AnimatedVisibility(
            visible = selectedFolder != null,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            selectedFolder?.let { folder ->
                FolderDetailScreen(
                    folder = folder,
                    viewModel = viewModel,
                    onBack = { selectedFolder = null }
                )
            }
        }
    }

    if (state.showAddFolderDialog) {
        AddFolderDialog(
            onDismiss = viewModel::hideAddFolderDialog,
            onConfirm = { name, icon, color ->
                viewModel.addFolder(name, icon, color)
                viewModel.hideAddFolderDialog()
            }
        )
    }
}

// ── Folders List ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderListScreen(
    folders: List<Folder>,
    onFolderClick: (Folder) -> Unit,
    onAddFolder: () -> Unit,
    onDeleteFolder: (Folder) -> Unit,
    onBack: () -> Unit,
    viewModel: HomeViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle folder delete undo locally — not in HomeScreen
    LaunchedEffect(state.snackbarMessage) {
        if (state.snackbarMessage == "UNDO_FOLDER_DELETE") {
            val folderName = state.lastDeletedFolder?.name ?: "Folder"
            val linkCount = state.lastDeletedFolderLinks.size
            val result = snackbarHostState.showSnackbar(
                message = "\"$folderName\" and $linkCount links deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoFolderDelete()
            }
            viewModel.dismissSnackbar()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Folders", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAddFolder) {
                        Icon(Icons.Filled.Add, "Add folder")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (folders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.FolderOpen, null,
                        Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No folders yet",
                        style = MaterialTheme.typography.titleMedium)
                    Button(onClick = onAddFolder) {
                        Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create folder")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(folders) { folder ->
                    FolderListItem(
                        folder = folder,
                        onClick = { onFolderClick(folder) },
                        onDelete = { onDeleteFolder(folder) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderListItem(folder: Folder, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(folder.name, style = MaterialTheme.typography.titleMedium)
        },
        leadingContent = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(android.graphics.Color.parseColor(folder.color)).copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        iconFromName(folder.icon), null,
                        Modifier.size(22.dp),
                        tint = Color(android.graphics.Color.parseColor(folder.color))
                    )
                }
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "${folder.linkCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Outlined.ChevronRight, null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = { showDeleteDialog = true }
        ),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete folder?") },
            text = {
                Text(
                    "\"${folder.name}\" will be deleted. "
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
// ── Folder Detail ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folder: Folder,
    viewModel: HomeViewModel,
    onBack: () -> Unit,

) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searchExpanded by remember { mutableStateOf(false) }  // add this

    BackHandler(enabled = isSelectionMode) {
        selectedIds = emptySet()
    }

    val folderLinks = remember(state.links, folder.id, searchQuery) {
        state.links
            .filter { it.folderId == folder.id }
            .filter {
                if (searchQuery.isBlank()) true
                else it.title.contains(searchQuery, ignoreCase = true) ||
                        it.url.contains(searchQuery, ignoreCase = true) ||
                        it.domain.contains(searchQuery, ignoreCase = true)
            }
    }

    Scaffold(
        topBar = {
            Column {
                // Normal top bar — always visible
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                iconFromName(folder.icon), null,
                                Modifier.size(20.dp),
                                tint = Color(android.graphics.Color.parseColor(folder.color))
                            )
                            Text(folder.name, style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isSelectionMode) selectedIds = emptySet()
                            else onBack()
                        }) {
                            Icon(Icons.Outlined.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // Animated search + bulk row — same as HomeScreen
                var searchExpanded by remember { mutableStateOf(false) }

                LaunchedEffect(isSelectionMode) {
                    if (!isSelectionMode) searchExpanded = false
                }

                val bulkWeight by animateFloatAsState(
                    targetValue = when {
                        !isSelectionMode -> 0f
                        searchExpanded -> 0.25f
                        else -> 0.300f
                    },
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    label = "bulkWeight"
                )

                val searchWeight by animateFloatAsState(
                    targetValue = when {
                        !isSelectionMode -> 1f
                        searchExpanded -> 0.75f
                        else -> 0.06f
                    },
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    label = "searchWeight"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bulk bar — same as HomeScreen
                    if (bulkWeight > 0f) {
                        Row(
                            modifier = Modifier
                                .weight(bulkWeight.coerceAtLeast(0.001f))
                                .height(56.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50.dp))
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(50.dp))
                                .clickable { if (searchExpanded) searchExpanded = false },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { selectedIds = emptySet() }) {
                                Icon(Icons.Filled.Close, "Cancel", Modifier.size(18.dp))
                            }

                            if (isSelectionMode) {
                                var showFolderPicker by remember { mutableStateOf(false) }

                                if (searchExpanded) {
                                    Text(
                                        "${selectedIds.size}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                } else {
                                    Text(
                                        "${selectedIds.size} selected",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip
                                    )
                                    TextButton(onClick = {
                                        selectedIds = folderLinks.map { it.id }.toSet()
                                    }) { Text("All") }
                                    IconButton(onClick = { showFolderPicker = true }) {
                                        Icon(Icons.Outlined.FolderOpen, "Move", Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            val linksToDelete = folderLinks.filter { it.id in selectedIds }
                                            linksToDelete.forEach { viewModel.deleteLink(it) }
                                            val result = snackbarHostState.showSnackbar(
                                                message = "${linksToDelete.size} links deleted",
                                                actionLabel = "Undo",
                                                withDismissAction = true,
                                                duration = SnackbarDuration.Long
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                linksToDelete.forEach { viewModel.restoreLink(it) }
                                            }
                                            selectedIds = emptySet()
                                        }
                                    }) {
                                        Icon(
                                            Icons.Outlined.Delete, "Delete",
                                            Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                if (showFolderPicker) {
                                    FolderPickerDialog(
                                        folders = state.folders.filter { it.id != folder.id },
                                        currentFolderId = folder.id,
                                        onSelect = { folderId ->
                                            scope.launch {
                                                val linksToMove = folderLinks.filter { it.id in selectedIds }
                                                linksToMove.forEach { viewModel.moveToFolder(it, folderId) }
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "${linksToMove.size} links moved",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Long
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    linksToMove.forEach { viewModel.moveToFolder(it, folder.id) }
                                                }
                                                selectedIds = emptySet()
                                            }
                                            showFolderPicker = false
                                        },
                                        onDismiss = { showFolderPicker = false }
                                    )
                                }
                            }
                        }
                    }

                    // Search bar — exactly same as HomeScreen
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(if (searchExpanded) "Search…" else "Search in ${folder.name}…")
                        },
                        leadingIcon = {
                            IconButton(
                                onClick = {
                                    if (isSelectionMode && !searchExpanded) {
                                        searchExpanded = true
                                    }
                                },
                                enabled = isSelectionMode && !searchExpanded
                            ) {
                                Icon(
                                    Icons.Filled.Search, "Search",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.weight(searchWeight.coerceAtLeast(0.001f)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Text(
                "${folderLinks.size} links",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            if (folderLinks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isBlank()) "No links in this folder"
                        else "No results for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folderLinks, key = { it.id }) { link ->
                        LinkCard(
                            link = link,
                            isSelected = selectedIds.contains(link.id),
                            isSelectionMode = isSelectionMode,
                            onLongPress = {
                                selectedIds = if (selectedIds.contains(link.id))
                                    selectedIds - link.id
                                else
                                    selectedIds + link.id
                            },
                            folders = state.folders,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedIds = if (selectedIds.contains(link.id))
                                        selectedIds - link.id
                                    else
                                        selectedIds + link.id
                                } else {
                                    viewModel.markAsRead(link, true)
                                    uriHandler.openUri(link.url)
                                }
                            },
                            onFavoriteToggle = { viewModel.toggleFavorite(link) },
                            onDelete = { viewModel.deleteLink(link) },
                            onMoveToFolder = { folderId -> viewModel.moveToFolder(link, folderId) },
                            onEdit = { viewModel.setEditingLink(link) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}