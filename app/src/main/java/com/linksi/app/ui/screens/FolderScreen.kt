package com.linksi.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }
    // Keep track of the folder even when selectedFolder becomes null to allow exit animation
    val activeFolder = remember(selectedFolder) {
        if (selectedFolder != null) selectedFolder else null
    }
    // We actually need to keep the last non-null folder for the exit animation
    var lastFolder by remember { mutableStateOf<Folder?>(null) }
    LaunchedEffect(selectedFolder) {
        if (selectedFolder != null) lastFolder = selectedFolder
    }

    var browserUrl by remember { mutableStateOf<String?>(null) }
    var browserTitle by remember { mutableStateOf("") }            // add

    BackHandler {
        if (browserUrl != null) {
            browserUrl = null  // handled by browser's own back
        } else if (selectedFolder != null) {
            selectedFolder = null
        } else {
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FolderListScreen(
            folders = state.folders,
            onFolderClick = { selectedFolder = it },
            onAddFolder = viewModel::showAddFolderDialog,
            onDeleteFolder = { viewModel.deleteFolder(it) },
            onBack = onBack,
            viewModel = viewModel
        )

        AnimatedVisibility(
            visible = selectedFolder != null,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            lastFolder?.let { folder ->
                FolderDetailScreen(
                    folder = folder,
                    viewModel = viewModel,
                    onBack = { selectedFolder = null },
                    onOpenBrowser = { url, title ->
                        browserUrl = url
                        browserTitle = title
                    }
                )
            }
        }

        // ── Browser overlay — same as HomeScreen ──────────────
        var offsetY by remember { mutableStateOf(3000f) }
        val screenHeightPx = LocalConfiguration.current.screenHeightDp.toFloat()

        val animatedOffset by animateFloatAsState(
            targetValue = offsetY,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "browserOffset",
            finishedListener = { final ->
                if (final >= screenHeightPx) {
                    browserUrl = null
                    offsetY = screenHeightPx
                }
            }
        )

        LaunchedEffect(browserUrl) {
            if (browserUrl != null) {
                offsetY = screenHeightPx
                offsetY = 0f
            }
        }

        if (browserUrl != null || animatedOffset < screenHeightPx) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp)
                    .offset(y = animatedOffset.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 8.dp
                ) {
                    browserUrl?.let { url ->
                        InAppBrowser(
                            url = url,
                            title = browserTitle,
                            onDrag = { dragAmount ->
                                offsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                            },
                            onDragEnd = {
                                if (offsetY > 120f) offsetY = screenHeightPx
                                else offsetY = 0f
                            },
                            onDismiss = { offsetY = screenHeightPx }
                        )
                    }
                }
            }
        }
        // ── End Browser ───────────────────────────────────────
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
    var editingFolder by remember { mutableStateOf<Folder?>(null) }
    var viewMode = state.folderViewMode
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOption = state.folderSortOption

    val sortedFolders = remember(folders, sortOption) {
        when (sortOption) {
            FolderSortOption.NAME_AZ -> folders.sortedBy { it.name.lowercase() }
            FolderSortOption.NAME_ZA -> folders.sortedByDescending { it.name.lowercase() }
            FolderSortOption.NEWEST -> folders.sortedByDescending { it.createdAt }
            FolderSortOption.OLDEST -> folders.sortedBy { it.createdAt }
            FolderSortOption.MOST_LINKS -> folders.sortedByDescending { it.linkCount }
        }
    }

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
                    // Sort button
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Outlined.Sort, "Sort")
                    }
                    // Toggle view
                    IconButton(onClick = {
                        viewModel.setFolderViewMode(
                            if (viewMode == FolderViewMode.LIST) FolderViewMode.GRID else FolderViewMode.LIST
                        )
                    }) {
                        Icon(
                            if (viewMode == FolderViewMode.LIST)
                                Icons.Outlined.GridView
                            else Icons.Outlined.ViewList,
                            "Toggle view"
                        )
                    }
                    // Add folder
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
            when (viewMode) {
                FolderViewMode.LIST -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(sortedFolders, key = { it.id }) { folder ->
                            var swipeConfirmed by remember { mutableStateOf(false) }
                            val dismissState = rememberSwipeToDismissBoxState(
                                positionalThreshold = { it * 0.4f },
                                confirmValueChange = { value ->
                                    when (value) {
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            if (swipeConfirmed) {
                                                editingFolder = folder
                                                swipeConfirmed = false
                                            }
                                            false
                                        }
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            if (swipeConfirmed) {
                                                swipeConfirmed = false
                                                onDeleteFolder(folder)
                                                true
                                            } else false
                                        }
                                        else -> false
                                    }
                                }
                            )
                            LaunchedEffect(dismissState.progress) {
                                swipeConfirmed = dismissState.progress >= 0.4f
                            }
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val direction = dismissState.dismissDirection
                                    val isDelete = direction == SwipeToDismissBoxValue.EndToStart
                                    val isEdit = direction == SwipeToDismissBoxValue.StartToEnd
                                    val bgColor by animateColorAsState(
                                        targetValue = when {
                                            isDelete && swipeConfirmed ->
                                                MaterialTheme.colorScheme.errorContainer
                                            isDelete ->
                                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                            isEdit && swipeConfirmed ->
                                                MaterialTheme.colorScheme.primaryContainer
                                            isEdit ->
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else -> MaterialTheme.colorScheme.surface
                                        }, label = "folder_swipe_bg"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(bgColor)
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = if (isDelete)
                                            Alignment.CenterEnd else Alignment.CenterStart
                                    ) {
                                        if (isEdit) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Outlined.Edit, null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                                Text(
                                                    if (swipeConfirmed) "Release to edit" else "Edit",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        } else if (isDelete) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    if (swipeConfirmed) Icons.Outlined.Delete
                                                    else Icons.Outlined.Lock,
                                                    null,
                                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                                Text(
                                                    if (swipeConfirmed) "Release to delete"
                                                    else "Keep swiping…",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            ) {
                                FolderListItem(
                                    folder = folder,
                                    onClick = { onFolderClick(folder) },
                                    onDelete = { onDeleteFolder(folder) },
                                    onEdit = { editingFolder = it }
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                FolderViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(sortedFolders, key = { it.id }) { folder ->
                            FolderGridCard(
                                folder = folder,
                                onClick = { onFolderClick(folder) },
                                onEdit = { editingFolder = folder },
                                onDelete = { onDeleteFolder(folder) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Sort bottom sheet
    if (showSortMenu) {
        ModalBottomSheet(
            onDismissRequest = { showSortMenu = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Text("Sort folders",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                HorizontalDivider()
                FolderSortOption.values().forEach { option ->
                    ListItem(
                        headlineContent = { Text(option.label) },
                        leadingContent = { Icon(option.icon, null) },
                        trailingContent = {
                            if (option == sortOption)
                                Icon(Icons.Filled.Check, null,
                                    tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.clickable {
                            viewModel.setFolderSortOption(option)
                            showSortMenu = false
                        }
                    )
                }
            }
        }
    }

    editingFolder?.let { folder ->
        EditFolderDialog(
            folder = folder,
            onDismiss = { editingFolder = null },
            onConfirm = { name, icon, color ->
                viewModel.updateFolder(folder.copy(name = name, icon = icon, color = color))
                editingFolder = null
            }
        )
    }
}

// ── Enums ─────────────────────────────────────────────────────
enum class FolderViewMode { LIST, GRID }

enum class FolderSortOption(val label: String,
                            val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    NAME_AZ("Name A → Z", Icons.Outlined.SortByAlpha),
    NAME_ZA("Name Z → A", Icons.Outlined.SortByAlpha),
    NEWEST("Newest first", Icons.Outlined.Schedule),
    OLDEST("Oldest first", Icons.Outlined.Schedule),
    MOST_LINKS("Most links", Icons.Outlined.Link)
}

// ── Grid card ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderGridCard(
    folder: Folder,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Folder icon with color background
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(android.graphics.Color.parseColor(folder.color))
                        .copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            iconFromName(folder.icon), null,
                            Modifier.size(24.dp),
                            tint = Color(android.graphics.Color.parseColor(folder.color))
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // 3-dot menu
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Filled.MoreVert, null, Modifier.size(16.dp))
                }
            }

            Text(
                folder.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                "${folder.linkCount} links",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Options sheet on long press or 3-dot
    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Handle
                Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center) {
                    Surface(Modifier.width(40.dp).height(4.dp), RoundedCornerShape(2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)) {}
                }

                // Folder header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(android.graphics.Color.parseColor(folder.color))
                            .copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(iconFromName(folder.icon), null,
                                Modifier.size(22.dp),
                                tint = Color(android.graphics.Color.parseColor(folder.color)))
                        }
                    }
                    Column {
                        Text(folder.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text("${folder.linkCount} links",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider()
                Spacer(Modifier.height(4.dp))

                OptionsFullRow(
                    icon = Icons.Outlined.Edit,
                    title = "Edit folder",
                    onClick = { showMenu = false; onEdit() }
                )

                OptionsFullRow(
                    icon = Icons.Outlined.Delete,
                    title = "Delete folder",
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = { showMenu = false; onDelete() }
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderListItem(
    folder: Folder,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (Folder) -> Unit
) {
//    var showToolTip by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
            onLongClick = {
                Toast.makeText(
                    context,
                    "Swipe the folder to edit or delete",
                    Toast.LENGTH_SHORT
                ).show()
            }
        ),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )

//    if (showMenu) {
//        AlertDialog(
//            onDismissRequest = { showMenu = false },
//            title = { Text(folder.name) },
//            text = {
//                Column {
//                    ListItem(
//                        headlineContent = { Text("Edit folder") },
//                        leadingContent = { Icon(Icons.Outlined.Edit, null) },
//                        modifier = Modifier.clickable {
//                            showMenu = false
//                            onEdit(folder)
//                        }
//                    )
//                    HorizontalDivider()
//                    ListItem(
//                        headlineContent = {
//                            Text("Delete folder", color = MaterialTheme.colorScheme.error)
//                        },
//                        leadingContent = {
//                            Icon(
//                                Icons.Outlined.Delete, null,
//                                tint = MaterialTheme.colorScheme.error
//                            )
//                        },
//                        modifier = Modifier.clickable {
//                            showMenu = false
//                            onDelete()
//                        }
//                    )
//                }
//            },
//            confirmButton = {
//                TextButton(onClick = { showMenu = false }) { Text("Cancel") }
//            }
//        )
//    }
}

// ── Folder Detail ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folder: Folder,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onOpenBrowser: (url: String, title: String) -> Unit
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
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(50.dp)
                                )
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    RoundedCornerShape(50.dp)
                                )
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
                                        Icon(
                                            Icons.Outlined.FolderOpen,
                                            "Move",
                                            Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            val linksToDelete =
                                                folderLinks.filter { it.id in selectedIds }
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
                                                val linksToMove =
                                                    folderLinks.filter { it.id in selectedIds }
                                                linksToMove.forEach {
                                                    viewModel.moveToFolder(
                                                        it,
                                                        folderId
                                                    )
                                                }
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "${linksToMove.size} links moved",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Long
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    linksToMove.forEach {
                                                        viewModel.moveToFolder(
                                                            it,
                                                            folder.id
                                                        )
                                                    }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                                    if (viewModel.uiState.value.useInAppBrowser) {
                                        onOpenBrowser(link.url, link.title)
                                    } else {
                                        uriHandler.openUri(link.url)
                                    }
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