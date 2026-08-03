package com.linksi.app.ui.screens

import com.linksi.app.R
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
import com.linksi.app.domain.model.AI_MODELS
import com.linksi.app.domain.model.AiProvider
import com.linksi.app.domain.model.Link
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
    val apiKeys: Map<AiProvider, String> = emptyMap(),
    val availableModels: List<com.linksi.app.domain.model.AiModel> = com.linksi.app.domain.model.AI_MODELS,
    val modelStatus: SettingsViewModel.ModelStatus = SettingsViewModel.ModelStatus.UNKNOWN,
    val isTestingModel: Boolean = false,
    val selectedLanguage: String = "",
    val isSecurityEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val lockDelay: Long = 0L, // 0 means immediate
    val pin: String = "",
    val folders: List<com.linksi.app.domain.model.Folder> = emptyList(),
    val universalLock: Boolean = false,
    val folderLockEnabled: Boolean = false,
    val trashBinEnabled: Boolean = true,
    val globalPreventScreenshot: Boolean = false,
    val exportIncludeLocked: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: LinkRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    enum class ModelStatus { UNKNOWN, ACTIVE, ERROR }
    private val _uiState = MutableStateFlow(SettingsUiState())
    private val USE_IN_APP_BROWSER = booleanPreferencesKey("use_in_app_browser")
    val uiState = _uiState.asStateFlow()

    init {
        observeModels()
        viewModelScope.launch {
            combine(
                repository.getAllLinks(),
                repository.getAllFolders()
            ) { links, folders ->
                _uiState.update {
                    it.copy(
                        totalLinks = links.size,
                        totalFolders = folders.size,
                        totalFavorites = links.count { l -> l.isFavorite },
                        folders = folders
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
                        apiKeys          = keys,
                        selectedLanguage = prefs[APP_LANGUAGE] ?: "",
                        isSecurityEnabled = prefs[SECURITY_LOCK_ENABLED] ?: false,
                        isBiometricEnabled = prefs[SECURITY_BIOMETRIC_ENABLED] ?: false,
                        lockDelay = prefs[SECURITY_LOCK_DELAY] ?: 0L,
                        pin = prefs[SECURITY_PIN] ?: "",
                        universalLock = prefs[SECURITY_UNIVERSAL_LOCK] ?: false,
                        folderLockEnabled = prefs[SECURITY_FOLDER_LOCK_ENABLED] ?: false,
                        trashBinEnabled = prefs[TRASH_BIN_ENABLED] ?: true,
                        globalPreventScreenshot = prefs[GLOBAL_PREVENT_SCREENSHOT] ?: false,
                        exportIncludeLocked = prefs[EXPORT_INCLUDE_LOCKED] ?: false
                    )
                }
            }
        }
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        _uiState.update { it.copy(currentVersion = versionName ?: "1.0.0") }
    }

    private fun observeModels() {
        viewModelScope.launch {
            AiModelRegistry.getModels(context).collect { models ->
                _uiState.update { it.copy(availableModels = models) }
            }
        }
    }

    fun refreshModels() {
        viewModelScope.launch {
            AiModelRegistry.refreshAll(context, _uiState.value.apiKeys)
        }
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
                        updateCheckError = context.getString(R.string.update_check_error, e.message)
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
                val includeLocked = _uiState.value.exportIncludeLocked
                val links = repository.getAllLinks(!includeLocked).first()
                val folders = repository.getAllFolders().first().let { 
                    if (!includeLocked) it.filter { f -> !f.isLocked } else it
                }
                val json = exportLinksToJson(links, folders)
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray())
                }
                _uiState.update { it.copy(message = context.getString(R.string.exported_links, links.size)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = context.getString(R.string.export_failed, e.message)) }
            }
        }
    }

    fun exportCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val links = repository.getAllLinks(!_uiState.value.exportIncludeLocked).first()
                val csv = exportLinksToCsv(links)
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(csv.toByteArray())
                }
                _uiState.update { it.copy(message = context.getString(R.string.exported_links_csv, links.size)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = context.getString(R.string.export_failed, e.message)) }
            }
        }
    }

    fun exportHtml(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val links = repository.getAllLinks(!_uiState.value.exportIncludeLocked).first()
                val html = exportLinksToHtml(links)
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(html.toByteArray())
                }
                _uiState.update { it.copy(message = context.getString(R.string.exported_links_html, links.size)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = context.getString(R.string.export_failed, e.message)) }
            }
        }
    }

    fun importFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isImporting = true, importPhase = context.getString(R.string.reading_file_phase)) }

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
                    importPhase = context.getString(R.string.importing_links_phase),
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
                    importPhase = context.getString(R.string.fetching_metadata_phase),
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

                val message = if (duplicateCount > 0) {
                    context.getString(R.string.import_message_with_duplicates, importedCount, duplicateCount)
                } else {
                    context.getString(R.string.import_message_success, importedCount)
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
                    message = context.getString(R.string.import_failed_msg, e.message)
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
            refreshModels()
        }
    }

    fun setSelectedModel(modelId: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[AI_SELECTED_MODEL] = modelId }
            _uiState.update { it.copy(selectedModelId = modelId) }
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[APP_LANGUAGE] = languageCode }
            _uiState.update { it.copy(selectedLanguage = languageCode) }
        }
    }

    fun setSecurityEnabled(enabled: Boolean) {
        if (!_uiState.value.universalLock) return
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[SECURITY_LOCK_ENABLED] = enabled
                if (enabled) {
                    // Prevent immediate lock when enabling while app is active
                    prefs[LAST_APP_PAUSE_TIME] = Long.MAX_VALUE
                }
            }
            if (enabled && _uiState.value.pin.isEmpty()) {
                _uiState.update { it.copy(message = context.getString(R.string.pin_must_be_4_digits)) }
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[SECURITY_BIOMETRIC_ENABLED] = enabled }
        }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[SECURITY_PIN] = pin
                // Also reset pause time when setting/changing pin to prevent instant lock
                if (prefs[SECURITY_LOCK_ENABLED] == true) {
                    prefs[LAST_APP_PAUSE_TIME] = Long.MAX_VALUE
                }
            }
        }
    }

    fun setLockDelay(delayMs: Long) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[SECURITY_LOCK_DELAY] = delayMs
                // Reset pause time to prevent instant lock when changing delay while app is active
                if (prefs[SECURITY_LOCK_ENABLED] == true) {
                    prefs[LAST_APP_PAUSE_TIME] = Long.MAX_VALUE
                }
            }
        }
    }

    fun toggleFolderLock(folderId: Long, isLocked: Boolean) {
        viewModelScope.launch {
            repository.toggleFolderLock(folderId, isLocked)
        }
    }

    fun setUniversalLock(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[SECURITY_UNIVERSAL_LOCK] = enabled
                // If turning off universal lock, we must also disable the sub-locks
                if (!enabled) {
                    prefs[SECURITY_LOCK_ENABLED] = false
                    prefs[SECURITY_FOLDER_LOCK_ENABLED] = false
                }
            }
        }
    }

    fun setFolderLockEnabled(enabled: Boolean) {
        if (!_uiState.value.universalLock) return // Cannot enable if universal lock is off
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[SECURITY_FOLDER_LOCK_ENABLED] = enabled
            }
        }
    }

    fun setTrashBinEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[TRASH_BIN_ENABLED] = enabled }
            _uiState.update { it.copy(trashBinEnabled = enabled) }
        }
    }

    fun setGlobalPreventScreenshot(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[GLOBAL_PREVENT_SCREENSHOT] = enabled }
            _uiState.update { it.copy(globalPreventScreenshot = enabled) }
        }
    }

    fun setExportIncludeLocked(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[EXPORT_INCLUDE_LOCKED] = enabled }
            _uiState.update { it.copy(exportIncludeLocked = enabled) }
        }
    }

    fun testSelectedModel() {
        viewModelScope.launch {
            val model = _uiState.value.availableModels.find { it.id == _uiState.value.selectedModelId } ?: return@launch
            val apiKey = _uiState.value.apiKeys[model.provider] ?: ""

            if (apiKey.isBlank()) {
                _uiState.update { it.copy(modelStatus = ModelStatus.ERROR) }
                return@launch
            }

            _uiState.update { it.copy(isTestingModel = true, modelStatus = ModelStatus.UNKNOWN) }

            try {
                val service = AiOrganizerService()
                val result = service.generateOrganizePlan(
                    links = listOf(
                        Link(
                            id = 1, url = "https://google.com",
                            title = "Google", domain = "google.com"
                        )
                    ),
                    existingFolders = emptyList(),
                    model = model,
                    apiKey = apiKey
                )
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(
                            isTestingModel = false,
                            modelStatus = ModelStatus.ACTIVE
                        )}
                    },
                    onFailure = { e ->
                        val isQuotaError = e.message?.contains("429") == true ||
                                e.message?.contains("quota", ignoreCase = true) == true ||
                                e.message?.contains("rate limit", ignoreCase = true) == true ||
                                e.message?.contains("insufficient", ignoreCase = true) == true

                        _uiState.update { it.copy(
                            isTestingModel = false,
                            modelStatus = ModelStatus.ERROR,
                            // Show specific error in supporting text
                            updateCheckError = when {
                                isQuotaError -> context.getString(R.string.quota_exceeded_error)
                                e.message?.contains("401") == true -> context.getString(R.string.invalid_api_key_error)
                                e.message?.contains("403") == true -> context.getString(R.string.unauthorized_api_key_error)
                                else -> context.getString(R.string.generic_error_prefix, e.message?.take(60))
                            }
                        )}
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isTestingModel = false,
                    modelStatus = ModelStatus.ERROR,
                    updateCheckError = context.getString(R.string.generic_error_prefix, e.message?.take(60))
                )}
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
    fun dismissImportResult() = _uiState.update { it.copy(importResult = null) }
}