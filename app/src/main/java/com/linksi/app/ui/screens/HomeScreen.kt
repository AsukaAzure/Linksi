package com.linksi.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linksi.app.domain.model.*
import com.linksi.app.ui.components.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    var showFolders by remember { mutableStateOf(false) }
    var browserUrl by remember { mutableStateOf<String?>(null) }
    var browserTitle by remember { mutableStateOf("") }
    var offsetY by remember { mutableStateOf(3000f) }
    val screenHeightPx = LocalConfiguration.current.screenHeightDp.toFloat()

    BackHandler(enabled = state.isSelectionMode) {
        viewModel.clearSelection()
    }
    BackHandler(enabled = browserUrl != null) {
        offsetY = screenHeightPx
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { message ->
            when (message) {
                "UNDO_DELETE" -> {
                    val result = snackbarHostState.showSnackbar(
                        message = if (state.lastDeletedLinks.size > 1)
                            "${state.lastDeletedLinks.size} links deleted"
                        else "Link deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDeleted()
                    }
                    viewModel.dismissSnackbar()
                }
                "UNDO_MOVE" -> {
                    val result = snackbarHostState.showSnackbar(
                        message = if (state.lastMovedLinks.size > 1)
                            "${state.lastMovedLinks.size} links moved"
                        else "Link moved",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoMove()
                    }
                    viewModel.dismissSnackbar()
                }
                "UNDO_FOLDER_DELETE" -> {
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
                    return@let
                }
                else -> {
                    snackbarHostState.showSnackbar(message)
                    viewModel.dismissSnackbar()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                LinksTopBar(
                    selectedFolderId = state.selectedFolderId,
                    folders = state.folders,
                    viewMode = viewMode,
                    onViewModeToggle = {
                        viewMode = if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                    },
                    onSortClick = { showSortMenu = true },
//                    onAddFolder = viewModel::showAddFolderDialog,
                    onFoldersClick = { showFolders = true },
                    onSettingsClick = { showSettings = true }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = viewModel::showAddLinkDialog,
                    icon = { Icon(Icons.Filled.Add, "Add Link") },
                    text = { Text("Save Link") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                var searchExpanded by remember { mutableStateOf(false) }

                // Reset searchExpanded when leaving selection mode
                LaunchedEffect(state.isSelectionMode) {
                    if (!state.isSelectionMode) {
                        searchExpanded = false
                    }
                }

                // bulkWeight: animates 0f → 0.30f on entry, 0.30f → 0f on exit
                val bulkWeight by animateFloatAsState(
                    targetValue = when {
                        !state.isSelectionMode -> 0f
                        searchExpanded -> 0.25f
                        else -> 0.300f
                    },
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    label = "bulkWeight"
                )

                // searchWeight: animates 0.001f → 1f on exit
                val searchWeight by animateFloatAsState(
                    targetValue = when {
                        !state.isSelectionMode -> 1f
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
                    // ── Bulk bar ──────────────────────────────────────────────
                    // KEY FIX: guard on bulkWeight > 0f, NOT on state.isSelectionMode
                    // This keeps the bulk bar alive in composition during the exit
                    // animation so it can animate its weight back to 0f smoothly.
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
                            IconButton(onClick = {
                                viewModel.clearSelection()
                                // don't reset searchExpanded here —
                                // LaunchedEffect above handles it after isSelectionMode flips
                            }) {
                                Icon(Icons.Filled.Close, "Cancel", Modifier.size(18.dp))
                            }

                            // Only show inner content while actually in selection mode
                            // (during exit animation the bar is shrinking so content
                            //  would be clipped anyway — hiding it avoids jank)
                            if (state.isSelectionMode) {
                                var showFolderPicker by remember { mutableStateOf(false) }

                                if (searchExpanded) {
                                    Text(
                                        "${state.selectedIds.size}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                } else {
                                    Text(
                                        "${state.selectedIds.size} selected",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip
                                    )
                                    TextButton(onClick = viewModel::selectAll) { Text("All") }
                                    IconButton(onClick = { showFolderPicker = true }) {  // wired up now
                                        Icon(
                                            Icons.Outlined.FolderOpen,
                                            "Move",
                                            Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = viewModel::deleteSelected) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            "Delete",
                                            Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                if (showFolderPicker) {
                                    FolderPickerDialog(
                                        folders = state.folders,
                                        currentFolderId = null,
                                        onSelect = { folderId ->
                                            viewModel.moveSelectedToFolder(folderId)
                                            showFolderPicker = false
                                        },
                                        onDismiss = { showFolderPicker = false }
                                    )
                                }
                            }
                        }
                    }

                    // ── Search bar — ALWAYS in layout ─────────────────────────
                    // Never conditionally removed. Weight animates between
                    // ~0 (icon only, selection mode) and 1f (full width, normal).
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = {
                            Text(if (searchExpanded) "Search…" else "Search links, domains…")
                        },
                        leadingIcon = {
                            IconButton(
                                onClick = {
                                    if (state.isSelectionMode && !searchExpanded) {
                                        searchExpanded = true
                                    }
                                },
                                enabled = state.isSelectionMode && !searchExpanded
                            ) {
                                Icon(
                                    Icons.Filled.Search, "Search",
                                    modifier = Modifier.padding(start = 8.dp))
                            }
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Filled.Clear, "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        // Weight smoothly animates: ~0 → 1f on exit from selection mode
                        modifier = Modifier.weight(searchWeight.coerceAtLeast(0.001f)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // Folder chips + filter chips
                FolderAndFilterRow(
                    folders = state.folders,
                    selectedFolderId = state.selectedFolderId,
                    selectedFilter = state.filterOption,
                    onFolderSelect = viewModel::selectFolder,
                    onFilterSelect = viewModel::setFilter
                )

                // Stats bar
                if (state.links.isNotEmpty()) {
                    StatsBar(
                        count = state.links.size,
                        unread = state.links.count { !it.isRead },
                        favorites = state.links.count { it.isFavorite }
                    )
                }

                // Content
                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (state.links.isEmpty()) {
                    EmptyState(
                        hasSearch = state.searchQuery.isNotBlank(),
                        onAddLink = viewModel::showAddLinkDialog
                    )
                } else {
                    when (viewMode) {
                        ViewMode.LIST -> LinksList(
                            links = state.links,
                            folders = state.folders,
                            selectedIds = state.selectedIds,
                            isSelectionMode = state.isSelectionMode,
                            onLongPress = viewModel::toggleSelction,
                            onLinkClick = { link ->
                                if (state.isSelectionMode) {
                                    viewModel.toggleSelction(link.id)
                                } else {
                                    viewModel.markAsRead(link, true)
                                    if (state.useInAppBrowser) {
                                        browserUrl = link.url
                                        browserTitle = link.title
                                    } else {
                                        uriHandler.openUri(link.url)
                                    }
                                }
                            },
                            onFavoriteToggle = viewModel::toggleFavorite,
                            onDelete = viewModel::deleteLink,
                            onMoveToFolder = { link, folderId ->
                                viewModel.moveToFolder(link, folderId)
                            },
                            onEdit = viewModel::setEditingLink
                        )

                        ViewMode.GRID -> LinksGrid(
                            links = state.links,
                            selectedIds = state.selectedIds,
                            isSelectionMode = state.isSelectionMode,
                            onLongPress = viewModel::toggleSelction,
                            onLinkClick = { link ->
                                if (state.isSelectionMode) {
                                    viewModel.toggleSelction(link.id)
                                } else {
                                    viewModel.markAsRead(link, true)
                                    if (state.useInAppBrowser) {
                                        browserUrl = link.url
                                        browserTitle = link.title
                                    } else {
                                        uriHandler.openUri(link.url)
                                    }
                                }
                            },
                            onFavoriteToggle = viewModel::toggleFavorite,
                            onDelete = viewModel::deleteLink
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            SettingsScreen(onBack = { showSettings = false })
        }

        AnimatedVisibility(
            visible = showFolders,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            FoldersScreen(onBack = { showFolders = false })
        }

        //browser

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

// When browserUrl is set, slide up
        LaunchedEffect(browserUrl) {
            if (browserUrl != null) {
                offsetY = screenHeightPx  // start from bottom
                offsetY = 0f              // slide up
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
                                if (offsetY > 120f) {
                                    offsetY = screenHeightPx
                                } else {
                                    offsetY = 0f
                                }
                            },
                            onDismiss = {
                                offsetY = screenHeightPx
                            }
                        )
                    }
                }
            }
        }
    }

    state.editingLink?.let { link ->
        EditLinkDialog(
            link = link,
            folders = state.folders,
            onDismiss = { viewModel.setEditingLink(null) },
            onConfirm = { updated ->
                viewModel.updateLink(updated)
                viewModel.setEditingLink(null)
            }
        )
    }

    if (state.showAddLinkDialog) {
        AddLinkDialog(
            folders = state.folders,
            isFetchingMetadata = state.isFetchingMetadata,
            onDismiss = viewModel::hideAddLinkDialog,
            onConfirm = { url, folderId ->
                viewModel.addLink(url, folderId)
                viewModel.hideAddLinkDialog()
            }
        )
    }

    if (state.showAddFolderDialog) {
        AddFolderDialog(
            onDismiss = viewModel::hideAddFolderDialog,
            onConfirm = { name, emoji, color ->
                viewModel.addFolder(name, emoji, color)
                viewModel.hideAddFolderDialog()
            }
        )
    }

    if (showSortMenu) {
        SortBottomSheet(
            currentSort = state.sortOption,
            onSortSelect = {
                viewModel.setSort(it)
                showSortMenu = false
            },
            onDismiss = { showSortMenu = false }
        )
    }
}

enum class ViewMode { LIST, GRID }

@Composable
fun StatsBar(count: Int, unread: Int, favorites: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatChip(label = "$count saved", icon = Icons.Outlined.Link)
        StatChip(label = "$unread unread", icon = Icons.Outlined.FiberNew)
        StatChip(label = "$favorites faved", icon = Icons.Outlined.Favorite)
    }
}

@Composable
fun StatChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FolderAndFilterRow(
    folders: List<Folder>,
    selectedFolderId: Long?,
    selectedFilter: FilterOption,
    onFolderSelect: (Long?) -> Unit,
    onFilterSelect: (FilterOption) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        item {
            FilterChip(
                selected = selectedFolderId == null && selectedFilter == FilterOption.ALL,
                onClick = { onFolderSelect(null); onFilterSelect(FilterOption.ALL) },
                label = { Text("All") },
                leadingIcon = { Icon(Icons.Outlined.AllInclusive, null, Modifier.size(16.dp)) }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == FilterOption.FAVORITES,
                onClick = { onFilterSelect(FilterOption.FAVORITES) },
                label = { Text("Favorites") },
                leadingIcon = { Icon(Icons.Outlined.Favorite, null, Modifier.size(16.dp)) }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == FilterOption.UNREAD,
                onClick = { onFilterSelect(FilterOption.UNREAD) },
                label = { Text("Unread") },
                leadingIcon = { Icon(Icons.Outlined.FiberNew, null, Modifier.size(16.dp)) }
            )
        }
        items(folders) { folder ->
            FilterChip(
                selected = selectedFolderId == folder.id,
                onClick = { onFolderSelect(folder.id) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            iconFromName(folder.icon), null, Modifier.size(14.dp),
                            tint = Color(android.graphics.Color.parseColor(folder.color))
                        )
                        Text(folder.name)
                    }
                }
            )
        }
    }
}

@Composable
fun LinksList(
    links: List<Link>,
    folders: List<Folder>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onLongPress: (Long) -> Unit,
    onLinkClick: (Link) -> Unit,
    onFavoriteToggle: (Link) -> Unit,
    onDelete: (Link) -> Unit,
    onMoveToFolder: (Link, Long?) -> Unit,
    onEdit: (Link) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(links, key = { it.id }) { link ->
            LinkCard(
                link = link,
                isSelected = selectedIds.contains(link.id),
                isSelectionMode = isSelectionMode,
                onLongPress = { onLongPress(link.id) },
                folders = folders,
                onClick = { onLinkClick(link) },
                onFavoriteToggle = { onFavoriteToggle(link) },
                onDelete = { onDelete(link) },
                onMoveToFolder = { folderId -> onMoveToFolder(link, folderId) },
                onEdit = { onEdit(link) }
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun LinksGrid(
    links: List<Link>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onLongPress: (Long) -> Unit,
    onLinkClick: (Link) -> Unit,
    onFavoriteToggle: (Link) -> Unit,
    onDelete: (Link) -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        items(links, key = { it.id }) { link ->
            LinkGridCard(
                link = link,
                isSelected = selectedIds.contains(link.id),
                isSelectionMode = isSelectionMode,
                onLongPress = { onLongPress(link.id) },
                onClick = { onLinkClick(link) },
                onFavoriteToggle = { onFavoriteToggle(link) }
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun EmptyState(hasSearch: Boolean, onAddLink: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = if (hasSearch) "🔍" else "🔗", fontSize = 64.sp)
            Text(
                text = if (hasSearch) "No links found" else "No links yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (hasSearch) "Try a different search" else "Save your first link!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasSearch) {
                Button(onClick = onAddLink) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save a Link")
                }
            }
        }
    }
}

@Composable
fun BulkActionBar(
    selectedCount: Int,
    folders: List<Folder>,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Long?) -> Unit,
    onCancel: () -> Unit
) {
    var showFolderPicker by remember { mutableStateOf(false) }

    Surface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, color = MaterialTheme.colorScheme.primary, RoundedCornerShape(25.dp))
                .background(
                    color = MaterialTheme.colorScheme.background,
                    RoundedCornerShape(25.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, "Cancel")
            }
            Text(
                "$selectedCount selected",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onSelectAll) { Text("All") }
            TextButton(onClick = { showFolderPicker = true }) {
                Icon(Icons.Outlined.FolderOpen, "Move")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete, "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            folders = folders,
            currentFolderId = null,
            onSelect = { folderId -> onMove(folderId); showFolderPicker = false },
            onDismiss = { showFolderPicker = false }
        )
    }
}