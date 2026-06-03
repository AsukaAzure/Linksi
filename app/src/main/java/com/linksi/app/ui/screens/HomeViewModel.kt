package com.linksi.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linksi.app.data.repository.LinkRepository
import com.linksi.app.domain.model.*
import com.linksi.app.utils.MetadataFetcher
import com.linksi.app.utils.extractDomain
import com.linksi.app.utils.isValidUrl
import com.linksi.app.utils.normalizeUrl
import com.linksi.app.utils.createNotificationChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.linksi.app.utils.cancelReminder
import com.linksi.app.utils.dataStore
import com.linksi.app.utils.scheduleReminder
import dagger.hilt.android.qualifiers.ApplicationContext

data class HomeUiState(
    val links: List<Link> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val searchQuery: String = "",
    val selectedFolderId: Long? = null,  // null = "All"
    val filterOption: FilterOption = FilterOption.ALL,
    val sortOption: SortOption = SortOption.DATE_NEWEST,
    val isLoading: Boolean = false,
    val isFetchingMetadata: Boolean = false,
    val showAddLinkDialog: Boolean = false,
    val showAddFolderDialog: Boolean = false,
    val editingLink: Link? = null,
    val snackbarMessage: String? = null,
    val folderSnackbarMessage: String? = null,
    val totalCount: Int = 0,
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val lastDeletedLinks: List<Link> = emptyList(),
    val lastMovedLinks: List<Link> = emptyList(),
    val lastMovedToFolderId: Long? = null,
    val lastDeletedFolder: Folder? = null,
    val lastDeletedFolderLinks: List<Link> = emptyList(),
    val useInAppBrowser: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LinkRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        createNotificationChannel(context)
        observeData()
        loadFolders()

        viewModelScope.launch {
            context.dataStore.data.collect { prefs ->
                _uiState.update {
                    it.copy(
                        useInAppBrowser = prefs[booleanPreferencesKey("use_in_app_browser")] ?: true
                    )
                }
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                _searchQuery,
                _uiState.map { it.selectedFolderId }.distinctUntilChanged(),
                _uiState.map { it.filterOption }.distinctUntilChanged()
            ) { query, folderId, filter ->
                Triple(query, folderId, filter)
            }.flatMapLatest { (query, folderId, filter) ->
                when {
                    query.isNotBlank() -> repository.searchLinks(query)
                    filter == FilterOption.FAVORITES -> repository.getFavoriteLinks()
                    filter == FilterOption.UNREAD -> repository.getUnreadLinks()
                    folderId != null -> repository.getLinksByFolder(folderId)
                    else -> repository.getAllLinks()
                }
            }.collect { links ->
                val sorted = sortLinks(links, _uiState.value.sortOption)
                _uiState.update { it.copy(links = sorted) }
            }
        }
    }

    private fun loadFolders() {
        viewModelScope.launch {
            repository.getAllFolders().collect { folders ->
                _uiState.update { it.copy(folders = folders) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectFolder(folderId: Long?) {
        _uiState.update { it.copy(selectedFolderId = folderId, filterOption = FilterOption.ALL) }
    }

    fun setFilter(filter: FilterOption) {
        _uiState.update { it.copy(filterOption = filter, selectedFolderId = null) }
    }

    fun setSort(sort: SortOption) {
        _uiState.update { state ->
            state.copy(
                sortOption = sort,
                links = sortLinks(state.links, sort)
            )
        }
    }

    fun addLink(url: String, folderId: Long? = null, reminderAt: Long? = null) {
        val normalized = normalizeUrl(url)
        if (!isValidUrl(normalized)) {
            _uiState.update { it.copy(snackbarMessage = "Invalid URL") }
            return
        }
        viewModelScope.launch {
            if (repository.isUrlAlreadySaved((normalized))) {
                _uiState.update { it.copy(snackbarMessage = "Link already saved") }
                return@launch
            }
            _uiState.update { it.copy(isFetchingMetadata = true) }
            val meta = MetadataFetcher.fetch(normalized)
            val link = Link(
                url = normalized,
                title = meta.title.ifBlank { extractDomain(normalized) },
                description = meta.description,
                faviconUrl = meta.faviconUrl,
                previewImageUrl = meta.previewImageUrl,
                domain = meta.domain.ifBlank { extractDomain(normalized) },
                reminderAt = reminderAt,
                folderId = folderId
            )
            val id = repository.insertLink(link)
            if (reminderAt != null) {
                scheduleReminder(
                    context,
                    id,
                    link.title.ifBlank { link.domain },
                    link.url,
                    reminderAt
                )
            }
            _uiState.update { it.copy(isFetchingMetadata = false, snackbarMessage = "Link saved ") }
        }
    }

    fun updateLink(link: Link) {
        viewModelScope.launch {
            repository.updateLink(link)
            cancelReminder(context, link.id)
            if (link.reminderAt != null && link.reminderAt > System.currentTimeMillis()) {
                scheduleReminder(context, link.id, link.title, link.url, link.reminderAt)
            }
            _uiState.update { it.copy(snackbarMessage = "Link updated") }
        }
    }

    fun deleteLink(link: Link) {
        viewModelScope.launch {
            repository.deleteLink(link)
            _uiState.update {
                it.copy(
                    lastDeletedLinks = listOf(link),
                    snackbarMessage = "UNDO_DELETE"
                )
            }
        }
    }

    fun undoDeleted() {
        viewModelScope.launch {
            _uiState.value.lastDeletedLinks.forEach { repository.insertLink(it) }
            _uiState.update {
                it.copy(
                    lastDeletedLinks = emptyList(),
                    snackbarMessage = "Restored ${it.lastDeletedLinks.size} links "
                )
            }
        }
    }

    fun undoMove() {
        viewModelScope.launch {
            // Restore each link to its original folder
            _uiState.value.lastMovedLinks.forEach { link ->
                repository.moveToFolder(link.id, link.folderId)
            }
            _uiState.update {
                it.copy(
                    lastMovedLinks = emptyList(),
                    lastMovedToFolderId = null,
                    snackbarMessage = "Moved back ✓"
                )
            }
        }
    }

    fun toggleFavorite(link: Link) {
        viewModelScope.launch {
            repository.toggleFavorite(link.id, !link.isFavorite)
        }
    }

    fun markAsRead(link: Link, isRead: Boolean) {
        viewModelScope.launch {
            repository.markAsRead(link.id, isRead)
        }
    }

    fun moveToFolder(link: Link, folderId: Long?) {
        viewModelScope.launch {
            repository.moveToFolder(link.id, folderId)
            _uiState.update { it.copy(snackbarMessage = "Moved to folder") }
        }
    }

    fun addFolder(name: String, icon: String, color: String) {
        viewModelScope.launch {
            val exists = _uiState.value.folders.any {
                it.name.trim().equals(name.trim(), ignoreCase = true)
            }
            if (exists) {
                _uiState.update { it.copy(folderSnackbarMessage = "Folder already exists") }
                return@launch
            }
            repository.insertFolder(Folder(name = name, icon = icon, color = color))
        }
    }

    // Replace deleteFolder
    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            // Save folder links before deleting
            val folderLinks = repository.getLinksByFolder(folder.id).first()

            // Delete all links in folder
            folderLinks.forEach { repository.deleteLink(it) }

            // Delete folder
            repository.deleteFolder(folder)

            _uiState.update {
                it.copy(
                    lastDeletedFolder = folder,
                    lastDeletedFolderLinks = folderLinks,
                    folderSnackbarMessage = "UNDO_FOLDER_DELETE"
                )
            }
        }
    }

    fun undoFolderDelete() {
        viewModelScope.launch {
            val folder = _uiState.value.lastDeletedFolder ?: return@launch
            val links = _uiState.value.lastDeletedFolderLinks

            // Restore folder with same ID
            repository.insertFolder(folder)

            // Restore all links
            links.forEach { repository.insertLink(it) }

            _uiState.update {
                it.copy(
                    lastDeletedFolder = null,
                    lastDeletedFolderLinks = emptyList(),
                    folderSnackbarMessage = "Folder restored"
                )
            }
        }
    }

    fun updateFolder(folder: Folder) {
        viewModelScope.launch {
            val exists = _uiState.value.folders.any {
                it.id != folder.id && it.name.trim().equals(folder.name.trim(), ignoreCase = true)
            }
            if (exists) {
                _uiState.update { it.copy(folderSnackbarMessage = "Folder already exists") }
                return@launch
            }
            repository.updateFolder(folder)
        }
    }

    fun dismissSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }
    fun dismissFolderSnackbar() = _uiState.update { it.copy(folderSnackbarMessage = null) }

    fun showAddLinkDialog() = _uiState.update { it.copy(showAddLinkDialog = true) }
    fun hideAddLinkDialog() = _uiState.update { it.copy(showAddLinkDialog = false) }
    fun showAddFolderDialog() = _uiState.update { it.copy(showAddFolderDialog = true) }
    fun hideAddFolderDialog() = _uiState.update { it.copy(showAddFolderDialog = false) }
    fun setEditingLink(link: Link?) = _uiState.update { it.copy(editingLink = link) }

    private fun sortLinks(links: List<Link>, sort: SortOption) = when (sort) {
        SortOption.DATE_NEWEST -> links.sortedByDescending { it.createdAt }
        SortOption.DATE_OLDEST -> links.sortedBy { it.createdAt }
        SortOption.TITLE_AZ -> links.sortedBy { it.title.lowercase() }
        SortOption.TITLE_ZA -> links.sortedByDescending { it.title.lowercase() }
        SortOption.DOMAIN -> links.sortedBy { it.domain }
    }

    fun toggleSelction(id: Long) {
        val current = _uiState.value.selectedIds
        val updated = if (current.contains(id)) current - id else current + id
        _uiState.update {
            it.copy(
                selectedIds = updated,
                isSelectionMode = updated.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        val allIds = _uiState.value.links.map { it.id }.toSet()
        _uiState.update { it.copy(selectedIds = allIds, isSelectionMode = true) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val linksToDelete = _uiState.value.links
                .filter { it.id in _uiState.value.selectedIds }

            linksToDelete.forEach { repository.deleteLink(it) }

            _uiState.update {
                it.copy(
                    selectedIds = emptySet(),
                    isSelectionMode = false,
                    lastDeletedLinks = linksToDelete,
                    snackbarMessage = "UNDO_DELETE"
                )
            }
        }
    }

    fun moveSelectedToFolder(folderId: Long?) {
        viewModelScope.launch {
            val linksToMove = _uiState.value.links
                .filter { it.id in _uiState.value.selectedIds }

            // Save original state before moving
            linksToMove.forEach { repository.moveToFolder(it.id, folderId) }

            _uiState.update {
                it.copy(
                    selectedIds = emptySet(),
                    isSelectionMode = false,
                    lastMovedLinks = linksToMove,
                    lastMovedToFolderId = folderId,
                    snackbarMessage = "UNDO_MOVE"
                )
            }
        }
    }

    fun restoreLink(link: Link) {
        viewModelScope.launch {
            repository.insertLink(link)
        }
    }
}
