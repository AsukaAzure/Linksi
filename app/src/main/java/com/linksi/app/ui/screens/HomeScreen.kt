package com.linksi.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.linksi.app.domain.model.*
import com.linksi.app.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var showSortMenu by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        topBar = {
            LinksTopBar(
                selectedFolderId = state.selectedFolderId,
                folders = state.folders,
                viewMode = viewMode,
                onViewModeToggle = { viewMode = if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST },
                onSortClick = { showSortMenu = true },
                onAddFolder = viewModel::showAddFolderDialog
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
            // Search bar
            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

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
                        onLinkClick = { link ->
                            viewModel.markAsRead(link, true)
                            uriHandler.openUri(link.url)
                        },
                        onFavoriteToggle = viewModel::toggleFavorite,
                        onDelete = viewModel::deleteLink,
                        onMoveToFolder = { link, folderId -> viewModel.moveToFolder(link, folderId) },
                        onEdit = viewModel::setEditingLink
                    )
                    ViewMode.GRID -> LinksGrid(
                        links = state.links,
                        onLinkClick = { link ->
                            viewModel.markAsRead(link, true)
                            uriHandler.openUri(link.url)
                        },
                        onFavoriteToggle = viewModel::toggleFavorite,
                        onDelete = viewModel::deleteLink
                    )
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

    // Dialogs
    if (state.showAddLinkDialog) {
        AddLinkDialog(
            folders = state.folders,
            isFetchingMetadata = state.isFetchingMetadata,
            onDismiss = viewModel::hideAddLinkDialog,
            onConfirm = { url, folderId, tags ->
                viewModel.addLink(url, folderId, tags)
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

    // Sort menu
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
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search links, domains, tags…") },
        leadingIcon = { Icon(Icons.Filled.Search, "Search") },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, "Clear")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
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
        // All chip
        item {
            FilterChip(
                selected = selectedFolderId == null && selectedFilter == FilterOption.ALL,
                onClick = { onFolderSelect(null); onFilterSelect(FilterOption.ALL) },
                label = { Text("All") },
                leadingIcon = { Icon(Icons.Outlined.AllInclusive, null, Modifier.size(16.dp)) }
            )
        }
        // Favorites
        item {
            FilterChip(
                selected = selectedFilter == FilterOption.FAVORITES,
                onClick = { onFilterSelect(FilterOption.FAVORITES) },
                label = { Text("Favorites") },
                leadingIcon = { Icon(Icons.Outlined.Favorite, null, Modifier.size(16.dp)) }
            )
        }
        // Unread
        item {
            FilterChip(
                selected = selectedFilter == FilterOption.UNREAD,
                onClick = { onFilterSelect(FilterOption.UNREAD) },
                label = { Text("Unread") },
                leadingIcon = { Icon(Icons.Outlined.FiberNew, null, Modifier.size(16.dp)) }
            )
        }
        // Folders
        items(folders) { folder ->
            FilterChip(
                selected = selectedFolderId == folder.id,
                onClick = { onFolderSelect(folder.id) },
                label = { Text("${folder.emoji} ${folder.name}") }
            )
        }
    }
}

@Composable
fun LinksList(
    links: List<Link>,
    folders: List<Folder>,
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
            Text(
                text = if (hasSearch) "🔍" else "🔗",
                fontSize = 64.sp
            )
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
