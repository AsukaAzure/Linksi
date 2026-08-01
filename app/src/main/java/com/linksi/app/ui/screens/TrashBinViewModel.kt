package com.linksi.app.ui.screens

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linksi.app.data.repository.LinkRepository
import com.linksi.app.domain.model.Link
import com.linksi.app.utils.TRASH_BIN_ENABLED
import com.linksi.app.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.linksi.app.R

data class TrashBinUiState(
    val links: List<Link> = emptyList(),
    val isEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val message: String? = null,
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false
)

@HiltViewModel
class TrashBinViewModel @Inject constructor(
    private val repository: LinkRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashBinUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            context.dataStore.data.map { it[TRASH_BIN_ENABLED] ?: true }.collect { enabled ->
                _uiState.update { it.copy(isEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            repository.getLinksInBin().collect { links ->
                _uiState.update { it.copy(links = links) }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[TRASH_BIN_ENABLED] = enabled }
        }
    }

    fun restoreLink(link: Link) {
        viewModelScope.launch {
            repository.restoreFromBin(link.id)
            _uiState.update { it.copy(message = context.getString(R.string.link_restored)) }
        }
    }

    fun permanentlyDeleteLink(link: Link) {
        viewModelScope.launch {
            repository.deleteLink(link)
            _uiState.update { it.copy(message = context.getString(R.string.link_deleted)) }
        }
    }

    fun restoreSelected() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds
            ids.forEach { repository.restoreFromBin(it) }
            _uiState.update { it.copy(
                selectedIds = emptySet(),
                isSelectionMode = false,
                message = context.getString(R.string.restored_links, ids.size)
            ) }
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds
            val linksToDelete = _uiState.value.links.filter { it.id in ids }
            linksToDelete.forEach { repository.deleteLink(it) }
            _uiState.update { it.copy(
                selectedIds = emptySet(),
                isSelectionMode = false,
                message = context.getString(R.string.links_permanently_deleted, ids.size)
            ) }
        }
    }

    fun emptyBin() {
        viewModelScope.launch {
            val links = _uiState.value.links
            links.forEach { repository.deleteLink(it) }
            _uiState.update { it.copy(message = context.getString(R.string.links_permanently_deleted, links.size)) }
        }
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newSelected = if (state.selectedIds.contains(id)) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(selectedIds = newSelected, isSelectionMode = newSelected.isNotEmpty())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
