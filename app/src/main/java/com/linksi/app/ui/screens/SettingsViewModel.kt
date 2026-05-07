package com.linksi.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linksi.app.data.repository.LinkRepository
import com.linksi.app.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.core.DataStore


data class SettingsUiState(
    val totalLinks: Int = 0,
    val totalFolders: Int = 0,
    val totalFavorites: Int = 0,
    val message: String? = null,
    val importResult: ImportResult? = null,
    val duplicateCount: Int = 0,
    val useInAppBrowser: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: LinkRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    private val USE_IN_APP_BROWSER = booleanPreferencesKey("use_in_app_browser")
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllLinks(),
                repository.getAllFolders()
            ) { links, folders ->
                _uiState.update { it.copy(
                    totalLinks = links.size,
                    totalFolders = folders.size,
                    totalFavorites = links.count { l -> l.isFavorite }
                )}
            }.collect()
        }
        viewModelScope.launch {
            context.dataStore.data.collect { prefs ->
                _uiState.update { it.copy(
                    useInAppBrowser = prefs[booleanPreferencesKey("use_in_app_browser")] ?: true
                )}
            }
        }
    }

    fun toggleInAppBrowser(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[USE_IN_APP_BROWSER] = enabled
            }
            _uiState.update { it.copy(useInAppBrowser = enabled) }
        }
    }

    fun exportJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val links = repository.getAllLinks().first()
                val folders = repository.getAllFolders().first()
                val json = exportLinksToJson(links, folders)
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray())
                }
                _uiState.update { it.copy(message = "Exported ${links.size} links ✓") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Export failed: ${e.message}") }
            }
        }
    }

    fun exportCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val links = repository.getAllLinks().first()
                val csv = exportLinksToCsv(links)
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(csv.toByteArray())
                }
                _uiState.update { it.copy(message = "Exported ${links.size} links as CSV ✓") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Export failed: ${e.message}") }
            }
        }
    }

    fun importFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val fileName = uri.path?.lowercase() ?: ""
                val result = when {
                    fileName.endsWith(".json") -> importFromLinksJson(context, uri)
                    else -> importFromBrowserHtml(context, uri)
                }

                // Insert folders first, build ID map
                val folderIdMap = mutableMapOf<Long, Long>()
                result.folders.forEach { folder ->
                    val newId = repository.insertFolder(folder)
                    folderIdMap[folder.id] = newId
                }

                // Insert links, track duplicates
                var importedCount = 0
                var duplicateCount = 0

                result.links.forEach { link ->
                    if (repository.isUrlAlreadySaved(link.url)) {
                        duplicateCount++
                    } else {
                        repository.insertLink(link.copy(
                            folderId = link.folderId?.let { folderIdMap[it] }
                        ))
                        importedCount++
                    }
                }

                // Build result message
                val message = buildString {
                    append("Imported $importedCount links")
                    if (duplicateCount > 0) {
                        append(" • $duplicateCount duplicates skipped")
                    }
                }

                _uiState.update { it.copy(
                    importResult = result.copy(count = importedCount),
                    duplicateCount = duplicateCount,
                    message = message
                )}

            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Import failed: ${e.message}") }
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
    fun dismissImportResult() = _uiState.update { it.copy(importResult = null) }
}