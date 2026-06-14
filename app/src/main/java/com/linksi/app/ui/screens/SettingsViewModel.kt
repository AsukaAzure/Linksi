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
import com.linksi.app.domain.model.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


data class SettingsUiState(
    val totalLinks: Int = 0,
    val totalFolders: Int = 0,
    val totalFavorites: Int = 0,
    val message: String? = null,
    val importResult: ImportResult? = null,
    val duplicateCount: Int = 0,
    val useInAppBrowser: Boolean = true,
    val currentVersion: String = "",
    val latestVersion: String = "",
    val updateAvailable: Boolean = false,
    val isCheckingUpdate: Boolean = false,
    val updateCheckError: String? = null,
    val aiEnabled: Boolean = false,
    val selectedModelId: String = "",
    val importProgress: Int = 0,
    val importTotal: Int = 0,
    val isImporting: Boolean = false,
    val importPhase: String = "",
    val apiKeys: Map<AiProvider, String> = emptyMap()
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
                _uiState.update {
                    it.copy(
                        totalLinks = links.size,
                        totalFolders = folders.size,
                        totalFavorites = links.count { l -> l.isFavorite }
                    )
                }
            }.collect()
        }
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
                        useInAppBrowser  = prefs[booleanPreferencesKey("use_in_app_browser")] ?: true,
                        aiEnabled        = prefs[AI_ENABLED]        ?: false,
                        selectedModelId  = prefs[AI_SELECTED_MODEL] ?: "claude35sonnet",
                        apiKeys          = keys
                    )
                }
            }
        }
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        _uiState.update { it.copy(currentVersion = versionName ?: "1.0.0") }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true, updateCheckError = null) }
            try {
                // Fetch latest release from GitHub
                val response = withContext(Dispatchers.IO) {
                    java.net.URL("https://api.github.com/repos/AsukaAzure/Linksi/releases/latest")
                        .openConnection()
                        .apply {
                            setRequestProperty("Accept", "application/vnd.github.v3+json")
                            connectTimeout = 8000
                            readTimeout = 8000
                        }
                        .getInputStream()
                        .bufferedReader()
                        .readText()
                }

                // Parse tag_name from JSON
                val tagName = Regex(""""tag_name"\s*:\s*"([^"]+)"""")
                    .find(response)?.groupValues?.get(1) ?: ""

                val latestVersion = tagName.removePrefix("v")
                val currentVersion = _uiState.value.currentVersion

                val updateAvailable = isNewerVersion(latestVersion, currentVersion)

                _uiState.update {
                    it.copy(
                        latestVersion = latestVersion,
                        updateAvailable = updateAvailable,
                        isCheckingUpdate = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCheckingUpdate = false,
                        updateCheckError = "Could not check for updates: ${e.message}"
                    )
                }
            }
        }
    }


    private fun isNewerVersion(latest: String, current: String): Boolean {
        return try {
            val latestParts = latest.split(".").map { it.toInt() }
            val currentParts = current.split(".").map { it.toInt() }
            for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            false
        } catch (e: Exception) {
            false
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
                _uiState.update { it.copy(isImporting = true, importPhase = "Reading file…") }

                val fileName = uri.path?.lowercase() ?: ""
                val result = when {
                    fileName.endsWith(".json") -> importFromLinksJson(context, uri)
                    else -> importFromBrowserHtml(context, uri)
                }

                // Insert folders first
                val folderIdMap = mutableMapOf<Long, Long>()
                result.folders.forEach { folder ->
                    val newId = repository.insertFolder(folder)
                    folderIdMap[folder.id] = newId
                }

                // Insert links tracking duplicates
                var importedCount = 0
                var duplicateCount = 0
                val insertedLinks = mutableListOf<Pair<Long, String>>() // id + url

                _uiState.update { it.copy(
                    importPhase = "Importing links…",
                    importTotal = result.links.size,
                    importProgress = 0
                )}

                result.links.forEachIndexed { index, link ->
                    if (repository.isUrlAlreadySaved(link.url)) {
                        duplicateCount++
                    } else {
                        val newId = repository.insertLink(
                            link.copy(folderId = link.folderId?.let { folderIdMap[it] })
                        )
                        insertedLinks.add(newId to link.url)
                        importedCount++
                    }
                    _uiState.update { it.copy(importProgress = index + 1) }
                }

                // ── Fetch metadata for all imported links ─────────
                _uiState.update { it.copy(
                    importPhase = "Fetching metadata…",
                    importTotal = insertedLinks.size,
                    importProgress = 0
                )}

                insertedLinks.forEachIndexed { index, (id, url) ->
                    try {
                        val existing = repository.getLinkById(id)
                        // Only fetch if title is blank or equals domain — metadata missing
                        val needsFetch = existing?.title.isNullOrBlank() ||
                                existing?.title == existing?.domain ||
                                existing?.previewImageUrl.isNullOrBlank()

                        if (needsFetch) {
                            val meta = MetadataFetcher.fetch(url)
                            existing?.let {
                                repository.updateLink(it.copy(
                                    title = meta.title.ifBlank { it.title.ifBlank { extractDomain(url) } },
                                    description = meta.description.ifBlank { it.description },
                                    faviconUrl = meta.faviconUrl.ifBlank { it.faviconUrl },
                                    previewImageUrl = meta.previewImageUrl,
                                    domain = meta.domain.ifBlank { it.domain }
                                ))
                            }
                        }
                    } catch (e: Exception) { }
                    _uiState.update { it.copy(importProgress = index + 1) }
                }

                val message = buildString {
                    append("Imported $importedCount links")
                    if (duplicateCount > 0) append(" • $duplicateCount duplicates skipped")
                }

                _uiState.update { it.copy(
                    isImporting = false,
                    importPhase = "",
                    importProgress = 0,
                    importTotal = 0,
                    importResult = result.copy(count = importedCount),
                    duplicateCount = duplicateCount,
                    message = message
                )}

            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isImporting = false,
                    importPhase = "",
                    message = "Import failed: ${e.message}"
                )}
            }
        }
    }

    fun setAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[AI_ENABLED] = enabled }
            _uiState.update { it.copy(aiEnabled = enabled) }
        }
    }

    fun setApiKey(provider: AiProvider, key: String) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                when (provider) {
                    AiProvider.OPENAI -> prefs[AI_KEY_OPENAI] = key
                    AiProvider.ANTHROPIC -> prefs[AI_KEY_ANTHROPIC] = key
                    AiProvider.GEMINI -> prefs[AI_KEY_GEMINI] = key
                    AiProvider.DEEPSEEK -> prefs[AI_KEY_DEEPSEEK] = key
                    AiProvider.GROK -> prefs[AI_KEY_GROK] = key
                }
            }
        }
    }

    fun setSelectedModel(modelId: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[AI_SELECTED_MODEL] = modelId }
            _uiState.update { it.copy(selectedModelId = modelId) }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
    fun dismissImportResult() = _uiState.update { it.copy(importResult = null) }
}