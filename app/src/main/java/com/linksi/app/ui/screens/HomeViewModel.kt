package com.linksi.app.ui.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linksi.app.data.repository.LinkRepository
import com.linksi.app.domain.model.*
import com.linksi.app.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.linksi.app.R

data class HomeUiState(
    val links: List<Link> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val subFolders: List<Folder> = emptyList(),
    val allFolders: List<Folder> = emptyList(),
    val searchQuery: String = "",
    val selectedFolderId: Long? = null,  // null = "All"
    val filterOption: FilterOption = FilterOption.ALL,
    val sortOption: SortOption = SortOption.DATE_NEWEST,
    val isLoading: Boolean = false,
    val isRefreshingMetadata: Boolean = false,
    val isFetchingMetadata: Boolean = false,
    val isAddingLink: Boolean = false,
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
    val lastDeletedFolderTree: FolderTree? = null,
    val useInAppBrowser: Boolean = true,
    val scrollToTop: Boolean = false,
    val allTags: List<String> = emptyList(),
    val folderViewMode: FolderViewMode = FolderViewMode.LIST,
    val folderSortOption: FolderSortOption = FolderSortOption.NAME_AZ,
    val homeViewMode : ViewMode = ViewMode.LIST,
    val folderLinksViewMode: ViewMode = ViewMode.LIST,
    val folderLockEnabled: Boolean = false,
    val showQuickFilters: Boolean = true,
    val trashBinEnabled: Boolean = true,
    val globalPreventScreenshot: Boolean = false
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
        observeFolders()
        startExpiryChecker()
        loadAllTags()

        viewModelScope.launch {
            context.dataStore.data.collect { prefs ->
                val keys = mapOf(
                    AiProvider.OPENAI    to (prefs[AI_KEY_OPENAI]    ?: ""),
                    AiProvider.ANTHROPIC to (prefs[AI_KEY_ANTHROPIC] ?: ""),
                    AiProvider.GEMINI    to (prefs[AI_KEY_GEMINI]    ?: ""),
                    AiProvider.DEEPSEEK  to (prefs[AI_KEY_DEEPSEEK]  ?: ""),
                    AiProvider.GROK      to (prefs[AI_KEY_GROK]      ?: "")
                )
                _uiState.update {
                    it.copy(
                        useInAppBrowser = prefs[booleanPreferencesKey("use_in_app_browser")] ?: true,
                        sortOption = prefs[HOME_SORT_OPTION]?.let {
                            runCatching { SortOption.valueOf(it) }.getOrNull()
                        } ?: SortOption.DATE_NEWEST,
                        homeViewMode = prefs[HOME_VIEW_MODE]?.let {
                            runCatching { ViewMode.valueOf(it) }.getOrNull()
                        } ?: ViewMode.LIST,
                        folderViewMode = prefs[FOLDER_VIEW_MODE]?.let {
                            FolderViewMode.valueOf(it)
                        } ?: FolderViewMode.LIST,
                        folderSortOption = prefs[FOLDER_SORT_OPTION]?.let {
                            FolderSortOption.valueOf(it)
                        } ?: FolderSortOption.NAME_AZ,
                        folderLinksViewMode = prefs[FOLDER_LINKS_VIEW_MODE]?.let {
                            runCatching { ViewMode.valueOf(it) }.getOrNull()
                        } ?: ViewMode.LIST,
                        folderLockEnabled = prefs[SECURITY_FOLDER_LOCK_ENABLED] ?: false,
                        showQuickFilters = prefs[SHOW_QUICK_FILTERS] ?: true,
                        trashBinEnabled = prefs[TRASH_BIN_ENABLED] ?: true,
                        globalPreventScreenshot = prefs[GLOBAL_PREVENT_SCREENSHOT] ?: false
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeData() {
        viewModelScope.launch {
            combine(
                _searchQuery,
                _uiState.map { it.selectedFolderId }.distinctUntilChanged(),
                _uiState.map { it.filterOption }.distinctUntilChanged(),
                _uiState.map { it.folderLockEnabled }.distinctUntilChanged()
            ) { query, folderId, filter, folderLockEnabled ->
                DataParams(query, folderId, filter, folderLockEnabled)
            }.flatMapLatest { params ->
                when {
                    params.query.isNotBlank() -> repository.searchLinks(params.query, params.folderLockEnabled)
                    params.filter == FilterOption.FAVORITES -> repository.getFavoriteLinks(params.folderLockEnabled)
                    params.filter == FilterOption.UNREAD -> repository.getUnreadLinks(params.folderLockEnabled)
                    params.folderId != null -> repository.getLinksByFolder(params.folderId)
                    else -> repository.getAllLinks(params.folderLockEnabled)
                }
            }.collect { links ->
                val sorted = sortLinks(links, _uiState.value.sortOption)
                _uiState.update { it.copy(links = sorted) }
            }
        }
    }

    private data class DataParams(
        val query: String,
        val folderId: Long?,
        val filter: FilterOption,
        val folderLockEnabled: Boolean
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeFolders() {
        // Main folders list (filtered by parent for nesting)
        viewModelScope.launch {
            _uiState.map { it.selectedFolderId }.distinctUntilChanged()
                .flatMapLatest { folderId ->
                    repository.getFoldersByParent(folderId)
                }.collect { folders ->
                    _uiState.update { 
                        if (it.selectedFolderId == null) {
                            it.copy(folders = folders, subFolders = emptyList())
                        } else {
                            it.copy(subFolders = folders)
                        }
                    }
                }
        }

        // Full list for pickers
        viewModelScope.launch {
            repository.getAllFolders().collect { all ->
                _uiState.update { it.copy(allFolders = all) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setTags(link: Link, tags: List<String>) {
        viewModelScope.launch {
            repository.updateLink(link.copy(tags = tags))
        }
    }

    fun deleteTagGlobally(tag: String) {
        viewModelScope.launch {
            val allLinks = _uiState.value.links
            allLinks.filter { tag in it.tags }.forEach { link ->
                repository.updateLink(link.copy(tags = link.tags - tag))
            }
            _uiState.update { it.copy(snackbarMessage = context.getString(R.string.tag_deleted_globally, tag)) }
        }
    }

    fun selectFolder(folderId: Long?) {
        if (folderId != null) {
            _searchQuery.value = ""
            _uiState.update { it.copy(searchQuery = "") }
        }
        _uiState.update { it.copy(selectedFolderId = folderId, filterOption = FilterOption.ALL) }
    }

    fun setFilter(filter: FilterOption) {
        _uiState.update { it.copy(filterOption = filter, selectedFolderId = null) }
    }

    fun setSort(sort: SortOption) {
        viewModelScope.launch {
            context.dataStore.edit { it[HOME_SORT_OPTION] = sort.name }
        }
        _uiState.update {
            val sorted = sortLinks(it.links, sort)
            it.copy(sortOption = sort, links = sorted)
        }
    }

    fun addLink(
        url: String,
        folderId: Long?,
        reminderAt: Long? = null,
        note: String = "",
        tags: List<String> = emptyList(),
        expiresAt: Long? = null,
        titleOverride: String? = null,
        descriptionOverride: String? = null,
        previewImageOverride: String? = null
    ) {
        viewModelScope.launch {
            val normalizedUrl = normalizeUrl(url)
            if (repository.isUrlAlreadySaved(normalizedUrl)) {
                _uiState.update { it.copy(snackbarMessage = context.getString(R.string.link_already_saved)) }
                return@launch
            }

            val domain = extractDomain(normalizedUrl)
            val faviconUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=64"

            val link = Link(
                url = normalizedUrl,
                title = titleOverride ?: "",
                description = descriptionOverride ?: "",
                folderId = folderId,
                reminderAt = reminderAt,
                previewImageUrl = previewImageOverride ?: "",
                faviconUrl = faviconUrl,
                domain = domain,
                note = note,
                tags = tags,
                expiresAt = expiresAt
            )
            try {
                repository.insertLink(link)
                _uiState.update { it.copy(snackbarMessage = context.getString(R.string.link_saved), scrollToTop = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = context.getString(R.string.link_save_failed)) }
            }
        }
    }

    fun updateLink(link: Link) {
        viewModelScope.launch {
            repository.updateLink(link)
            _uiState.update { it.copy(snackbarMessage = context.getString(R.string.link_updated)) }
        }
    }

    fun deleteLink(link: Link) {
        viewModelScope.launch {
            if (_uiState.value.trashBinEnabled) {
                repository.moveToBin(link.id)
                _uiState.update {
                    it.copy(
                        lastDeletedLinks = listOf(link),
                        snackbarMessage = "UNDO_MOVE_TO_BIN"
                    )
                }
            } else {
                repository.deleteLink(link)
                _uiState.update {
                    it.copy(
                        lastDeletedLinks = listOf(link),
                        snackbarMessage = "UNDO_DELETE"
                    )
                }
            }
        }
    }

    fun undoDeleted() {
        viewModelScope.launch {
            _uiState.value.lastDeletedLinks.forEach {
                if (_uiState.value.trashBinEnabled) {
                    repository.restoreFromBin(it.id)
                } else {
                    repository.insertLink(it)
                }
            }
            _uiState.update { it.copy(lastDeletedLinks = emptyList(), snackbarMessage = null) }
        }
    }

    fun undoMove() {
        viewModelScope.launch {
            _uiState.value.lastMovedLinks.forEach { link ->
                repository.updateLink(link)
            }
            _uiState.update { it.copy(lastMovedLinks = emptyList(), snackbarMessage = null) }
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
            _uiState.update { it.copy(snackbarMessage = context.getString(R.string.moved_to_folder)) }
        }
    }

    fun addFolder(name: String, icon: String, color: String, parentId: Long? = null) {
        viewModelScope.launch {
            val existing = repository.getFolderByNameAndParent(name, parentId)
            if (existing != null) {
                _uiState.update { it.copy(snackbarMessage = context.getString(R.string.folder_exists)) }
                return@launch
            }
            val folder = Folder(name = name, icon = icon, color = color, parentId = parentId)
            repository.insertFolder(folder)
            hideAddFolderDialog()
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            val tree = repository.getFolderTree(folder.id)
            repository.deleteFolder(folder)
            _uiState.update {
                it.copy(
                    lastDeletedFolderTree = tree,
                    snackbarMessage = "UNDO_FOLDER_DELETE"
                )
            }
        }
    }

    fun refreshLinkMetadata(link: Link) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingMetadata = true) }
            try {
                // Try cache first for refresh? Actually refresh should probably bypass cache or update it.
                // For manual refresh, we bypass cache to get latest.
                val meta = MetadataFetcher.fetch(link.url, context)
                repository.saveMetadataToCache(link.url, meta)
                
                val updatedLink = link.copy(
                    title = meta.title.ifBlank { link.title },
                    description = meta.description.ifBlank { link.description },
                    previewImageUrl = meta.previewImageUrl.ifBlank { link.previewImageUrl },
                    faviconUrl = meta.faviconUrl.ifBlank { "https://www.google.com/s2/favicons?domain=${meta.domain}&sz=64" },
                    domain = meta.domain.ifBlank { extractDomain(link.url) }
                )
                repository.updateLink(updatedLink)
                _uiState.update {
                    it.copy(
                        isRefreshingMetadata = false,
                        snackbarMessage = context.getString(R.string.metadata_refreshed)
                    )
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e is java.net.UnknownHostException -> context.getString(R.string.error_network)
                    e.message?.contains("403") == true -> context.getString(R.string.error_blocked)
                    else -> context.getString(R.string.metadata_refresh_failed)
                }
                _uiState.update {
                    it.copy(
                        isRefreshingMetadata = false,
                        snackbarMessage = errorMsg
                    )
                }
            }
        }
    }

    fun refreshAllMetadata() {
        viewModelScope.launch {
            if (_uiState.value.isRefreshingMetadata) return@launch
            _uiState.update { it.copy(isRefreshingMetadata = true) }
            
            val links = _uiState.value.links
            if (links.isEmpty()) {
                _uiState.update { it.copy(isRefreshingMetadata = false) }
                return@launch
            }

            links.forEach { link ->
                try {
                    val meta = MetadataFetcher.fetch(link.url, context)
                    val updatedLink = link.copy(
                        title = meta.title.ifBlank { link.title },
                        description = meta.description.ifBlank { link.description },
                        previewImageUrl = meta.previewImageUrl.ifBlank { link.previewImageUrl },
                        faviconUrl = meta.faviconUrl.ifBlank { "https://www.google.com/s2/favicons?domain=${meta.domain}&sz=64" },
                        domain = meta.domain.ifBlank { extractDomain(link.url) }
                    )
                    if (updatedLink != link) {
                        repository.updateLink(updatedLink)
                    }
                } catch (e: Exception) {
                    // Ignore failures for individual links
                }
            }

            _uiState.update {
                it.copy(
                    isRefreshingMetadata = false,
                    snackbarMessage = context.getString(R.string.metadata_refreshed)
                )
            }
        }
    }

    fun undoFolderDelete() {
        viewModelScope.launch {
            _uiState.value.lastDeletedFolderTree?.let { tree ->
                repository.insertFolder(tree.rootFolder)
                tree.descendantFolders.forEach { repository.insertFolder(it) }
                tree.allLinks.forEach { repository.insertLink(it) }
            }
            _uiState.update {
                it.copy(
                    lastDeletedFolderTree = null,
                    snackbarMessage = context.getString(R.string.folder_restored)
                )
            }
        }
    }

    fun updateFolder(folder: Folder) {
        viewModelScope.launch {
            val existing = repository.getFolderByNameAndParent(folder.name, folder.parentId)
            if (existing != null && existing.id != folder.id) {
                _uiState.update { it.copy(snackbarMessage = context.getString(R.string.folder_exists)) }
                return@launch
            }
            repository.updateFolder(folder)
        }
    }

    fun setPinned(link: Link) {
        viewModelScope.launch {
            val currentPinnedCount = _uiState.value.links.count { it.isPinned }
            if (!link.isPinned && currentPinnedCount >= 5) {
                _uiState.update { it.copy(snackbarMessage = context.getString(R.string.max_pinned_reached)) }
                return@launch
            }
            repository.setPinned(link.id, !link.isPinned)
            _uiState.update { it.copy(snackbarMessage = if (!link.isPinned) context.getString(R.string.link_pinned) else context.getString(R.string.link_unpinned)) }
        }
    }

    fun consumeScrollToTop() {
        _uiState.update { it.copy(scrollToTop = false) }
    }

    fun getAllTags(): Flow<List<String>> = repository.getAllLinks().map { links ->
        links.flatMap { it.tags }.distinct().sorted()
    }

    fun loadAllTags() {
        viewModelScope.launch {
            repository.getAllLinks().collect { links ->
                val tags = links.flatMap { it.tags }.distinct().sorted()
                _uiState.update { it.copy(allTags = tags) }
            }
        }
    }

    fun setNote(link: Link, note: String) {
        viewModelScope.launch {
            repository.setNote(link.id, note)
        }
    }

    fun setExpiry(link: Link, time: Long?) {
        viewModelScope.launch {
            repository.setExpiry(link.id, time)
        }
    }

    fun setReminder(link: Link, time: Long?) {
        viewModelScope.launch {
            repository.updateLink(link.copy(reminderAt = time))
            if (time != null) {
                scheduleNotification(context, link.id, link.title, time)
            } else {
                cancelNotification(context, link.id)
            }
        }
    }

    private fun startExpiryChecker() {
        viewModelScope.launch {
            while (true) {
                val expired = repository.getExpiredLinks()
                expired.forEach { repository.deleteLink(it) }
                kotlinx.coroutines.delay(60000) // Check every minute
            }
        }
    }

    suspend fun fetchMetadata(url: String): LinkMetadata {
        val cached = repository.getMetadataFromCache(url)
        if (cached != null) return cached

        val meta = MetadataFetcher.fetch(url, context)
        repository.saveMetadataToCache(url, meta)
        return meta
    }

    fun dismissSnackbar() { _uiState.update { it.copy(snackbarMessage = null) } }
    fun dismissFolderSnackbar() { _uiState.update { it.copy(folderSnackbarMessage = null) } }

    fun showAddLinkDialog() { _uiState.update { it.copy(showAddLinkDialog = true) } }
    fun hideAddLinkDialog() { _uiState.update { it.copy(showAddLinkDialog = false) } }
    fun showAddFolderDialog() { _uiState.update { it.copy(showAddFolderDialog = true) } }
    fun hideAddFolderDialog() { _uiState.update { it.copy(showAddFolderDialog = false) } }
    fun setEditingLink(link: Link?) { _uiState.update { it.copy(editingLink = link) } }

    fun sortLinks(links: List<Link>, sort: SortOption): List<Link> {
        val pinned = links.filter { it.isPinned }.sortedByDescending { it.createdAt }
        val unpinned = links.filter { !it.isPinned }
        val sortedUnpinned = when (sort) {
            SortOption.DATE_NEWEST -> unpinned.sortedByDescending { it.createdAt }
            SortOption.DATE_OLDEST -> unpinned.sortedBy { it.createdAt }
            SortOption.TITLE_AZ -> unpinned.sortedBy { it.title.lowercase() }
            SortOption.TITLE_ZA -> unpinned.sortedByDescending { it.title.lowercase() }
            SortOption.DOMAIN -> unpinned.sortedBy { it.domain.lowercase() }
        }
        return pinned + sortedUnpinned
    }

    fun toggleSelction(id: Long) {
        _uiState.update { state ->
            val newSelected = if (state.selectedIds.contains(id)) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(selectedIds = newSelected, isSelectionMode = newSelected.isNotEmpty())
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
            val ids = _uiState.value.selectedIds
            val linksToDelete = _uiState.value.links.filter { it.id in ids }
            val useBin = _uiState.value.trashBinEnabled

            linksToDelete.forEach {
                if (useBin) repository.moveToBin(it.id)
                else repository.deleteLink(it)
            }

            _uiState.update {
                it.copy(
                    lastDeletedLinks = linksToDelete,
                    snackbarMessage = if (useBin) "UNDO_MOVE_TO_BIN" else "UNDO_DELETE",
                    selectedIds = emptySet(),
                    isSelectionMode = false
                )
            }
        }
    }

    fun moveSelectedToFolder(folderId: Long?) {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds
            val linksToMove = _uiState.value.links.filter { it.id in ids }
            linksToMove.forEach { repository.moveToFolder(it.id, folderId) }
            _uiState.update {
                it.copy(
                    lastMovedLinks = linksToMove,
                    lastMovedToFolderId = folderId,
                    snackbarMessage = "UNDO_MOVE",
                    selectedIds = emptySet(),
                    isSelectionMode = false
                )
            }
        }
    }

    fun restoreLink(link: Link) {
        viewModelScope.launch {
            repository.insertLink(link)
        }
    }

    fun setFolderViewMode(mode: FolderViewMode) {
        viewModelScope.launch { context.dataStore.edit { it[FOLDER_VIEW_MODE] = mode.name } }
    }

    fun setFolderSortOption(option: FolderSortOption) {
        viewModelScope.launch { context.dataStore.edit { it[FOLDER_SORT_OPTION] = option.name } }
    }

    fun setHomeViewMode(mode: ViewMode) {
        viewModelScope.launch { context.dataStore.edit { it[HOME_VIEW_MODE] = mode.name } }
    }

    fun setFolderLinksViewMode(mode: ViewMode) {
        viewModelScope.launch { context.dataStore.edit { it[FOLDER_LINKS_VIEW_MODE] = mode.name } }
    }

    private fun createNotificationChannel(context: Context) {
        val name = context.getString(R.string.notification_channel_name)
        val descriptionText = context.getString(R.string.notification_channel_desc)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("link_reminders", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun scheduleNotification(context: Context, linkId: Long, title: String, time: Long) {
        // Implementation for scheduling notification
    }

    private fun cancelNotification(context: Context, linkId: Long) {
        // Implementation for canceling notification
    }
}
