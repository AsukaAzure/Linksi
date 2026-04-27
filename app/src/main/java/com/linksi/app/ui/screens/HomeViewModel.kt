package com.linksi.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linksi.app.data.repository.LinkRepository
import com.linksi.app.domain.model.*
import com.linksi.app.utils.MetadataFetcher
import com.linksi.app.utils.extractDomain
import com.linksi.app.utils.isValidUrl
import com.linksi.app.utils.normalizeUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val totalCount: Int = 0,
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LinkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeData()
        loadFolders()
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

    fun addLink(url: String, folderId: Long? = null, tags: List<String> = emptyList()) {
        val normalized = normalizeUrl(url)
        if (!isValidUrl(normalized)) {
            _uiState.update { it.copy(snackbarMessage = "Invalid URL") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingMetadata = true) }
            val meta = MetadataFetcher.fetch(normalized)
            val link = Link(
                url = normalized,
                title = meta.title.ifBlank { extractDomain(normalized) },
                description = meta.description,
                faviconUrl = meta.faviconUrl,
                previewImageUrl = meta.previewImageUrl,
                domain = meta.domain.ifBlank { extractDomain(normalized) },
                folderId = folderId,
                tags = tags
            )
            repository.insertLink(link)
            _uiState.update { it.copy(isFetchingMetadata = false, snackbarMessage = "Link saved ✓") }
        }
    }

    fun updateLink(link: Link) {
        viewModelScope.launch {
            repository.updateLink(link)
            _uiState.update { it.copy(snackbarMessage = "Link updated") }
        }
    }

    fun deleteLink(link: Link) {
        viewModelScope.launch {
            repository.deleteLink(link)
            _uiState.update { it.copy(snackbarMessage = "Link deleted") }
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

    fun addFolder(name: String, emoji: String, color: String) {
        viewModelScope.launch {
            repository.insertFolder(Folder(name = name, emoji = emoji, color = color))
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
        }
    }

    fun dismissSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    fun showAddLinkDialog() = _uiState.update { it.copy(showAddLinkDialog = true) }
    fun hideAddLinkDialog() = _uiState.update { it.copy(showAddLinkDialog = false) }
    fun showAddFolderDialog() = _uiState.update { it.copy(showAddFolderDialog = true) }
    fun hideAddFolderDialog() = _uiState.update { it.copy(showAddFolderDialog = false) }
    fun setEditingLink(link: Link?) = _uiState.update { it.copy(editingLink = link) }

    private fun sortLinks(links: List<Link>, sort: SortOption) = when (sort) {
        SortOption.DATE_NEWEST -> links.sortedByDescending { it.createdAt }
        SortOption.DATE_OLDEST -> links.sortedBy { it.createdAt }
        SortOption.TITLE_AZ    -> links.sortedBy { it.title.lowercase() }
        SortOption.TITLE_ZA    -> links.sortedByDescending { it.title.lowercase() }
        SortOption.DOMAIN      -> links.sortedBy { it.domain }
    }
    fun toggleSelction(id: Long) {
        val current = _uiState.value.selectedIds
        val updated = if (current.contains(id)) current - id else current + id
        _uiState.update { it.copy(
            selectedIds = updated,
            isSelectionMode = updated.isNotEmpty()
        )}
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
            _uiState.value.selectedIds.forEach { repository.deleteLink(Link(id = it, url = "")) }
            _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false,
                snackbarMessage = "Links deleted") }
        }
    }

    fun moveSelectedToFolder(folderId: Long?) {
        viewModelScope.launch {
            _uiState.value.selectedIds.forEach { repository.moveToFolder(it, folderId) }
            _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false,
                snackbarMessage = "Links moved") }
        }
    }
}
