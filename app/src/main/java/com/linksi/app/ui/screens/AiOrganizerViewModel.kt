package com.linksi.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linksi.app.data.repository.LinkRepository
import com.linksi.app.domain.model.*
import com.linksi.app.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

enum class AiOrganizerStep {
    IDLE,
    SELECT_SCOPE,
    GENERATING,
    PREVIEW,
    APPLYING,
    DONE,
    ERROR
}

data class AiOrganizerUiState(
    val isEnabled: Boolean = false,
    val selectedModelId: String = "claude3sonnet",
    val apiKeys: Map<AiProvider, String> = emptyMap(),

    // Organize flow
    val step: AiOrganizerStep = AiOrganizerStep.IDLE,
    val selectedScope: String = OrganizeScope.UNORGANIZED,
    val plan: OrganizePlan? = null,
    val folders: List<Folder> = emptyList(),
    val errorMessage: String? = null,
    val applyProgress: Int = 0,
    val applyTotal: Int = 0,
    val batchSize: Int = 32,
    val batchProgress: String = "",

    // Revert
    val lastSession: AiOrganizerSession? = null,
    val hasRevertableSession: Boolean = false,

    val snackbarMessage: String? = null,

    // testing
    val modelStatus: SettingsViewModel.ModelStatus = SettingsViewModel.ModelStatus.UNKNOWN,
    val isTestingModel: Boolean = false,
    val testErrorMessage: String? = null,
)

@HiltViewModel
class AiOrganizerViewModel @Inject constructor(
    private val repository: LinkRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiOrganizerUiState())
    val uiState = _uiState.asStateFlow()

    private val service = AiOrganizerService()

    init {
        loadSettings()
        loadLastSession()
    }

    // ── Settings ──────────────────────────────────────────────

    private fun loadSettings() {
        viewModelScope.launch {
            context.dataStore.data.collect { prefs ->
                val keys = mapOf(
                    AiProvider.OPENAI to (prefs[AI_KEY_OPENAI] ?: ""),
                    AiProvider.ANTHROPIC to (prefs[AI_KEY_ANTHROPIC] ?: ""),
                    AiProvider.GEMINI to (prefs[AI_KEY_GEMINI] ?: ""),
                    AiProvider.DEEPSEEK to (prefs[AI_KEY_DEEPSEEK] ?: ""),
                    AiProvider.GROK to (prefs[AI_KEY_GROK] ?: "")
                )
                _uiState.update {
                    it.copy(
                        isEnabled = prefs[AI_ENABLED] ?: false,
                        selectedModelId = prefs[AI_SELECTED_MODEL] ?: "claude3sonnet",
                        apiKeys = keys
                    )
                }
            }
        }
    }

    fun setBatchSize(size: Int) {
        _uiState.update { it.copy(batchSize = size) }
    }

    fun testModel() {
        viewModelScope.launch {
            val state = _uiState.value
            val model = AI_MODELS.find { it.id == state.selectedModelId } ?: return@launch
            val apiKey = state.apiKeys[model.provider] ?: ""

            if (apiKey.isBlank()) {
                _uiState.update { it.copy(
                    modelStatus = SettingsViewModel.ModelStatus.ERROR,
                    testErrorMessage = context.getString(com.linksi.app.R.string.no_api_key_for, model.provider.name)
                )}
                return@launch
            }

            _uiState.update { it.copy(
                isTestingModel = true,
                modelStatus = SettingsViewModel.ModelStatus.UNKNOWN,
                testErrorMessage = null
            )}

            try {
                val result = service.generateOrganizePlan(
                    links = listOf(
                        Link(
                            id = 1,
                            url = "https://google.com",
                            title = "Google",
                            domain = "google.com"
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
                            modelStatus = SettingsViewModel.ModelStatus.ACTIVE,
                            testErrorMessage = null
                        )}
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(
                            isTestingModel = false,
                            modelStatus = SettingsViewModel.ModelStatus.ERROR,
                            testErrorMessage = mapErrorToMessage(e)
                        )}
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isTestingModel = false,
                    modelStatus = SettingsViewModel.ModelStatus.ERROR,
                    testErrorMessage = mapErrorToMessage(e)
                )}
            }
        }
    }

    private fun mapErrorToMessage(e: Throwable): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("400") -> context.getString(com.linksi.app.R.string.error_invalid_request)
            msg.contains("401") -> context.getString(com.linksi.app.R.string.invalid_api_key_error)
            msg.contains("403") -> context.getString(com.linksi.app.R.string.unauthorized_api_key_error)
            msg.contains("429") || msg.contains("quota", ignoreCase = true) ->
                context.getString(com.linksi.app.R.string.quota_exceeded_error)
            msg.contains("insufficient", ignoreCase = true) -> context.getString(com.linksi.app.R.string.error_insufficient_credits)
            msg.contains("Unable to resolve host") -> context.getString(com.linksi.app.R.string.error_network)
            else -> context.getString(com.linksi.app.R.string.generic_error_prefix, msg.take(60))
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[AI_ENABLED] = enabled }
        }
    }

    fun setSelectedModel(modelId: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[AI_SELECTED_MODEL] = modelId }
            _uiState.update { it.copy(modelStatus = SettingsViewModel.ModelStatus.UNKNOWN, testErrorMessage = null) }
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
            _uiState.update { it.copy(modelStatus = SettingsViewModel.ModelStatus.UNKNOWN, testErrorMessage = null) }
        }
    }

    // ── Organize flow ─────────────────────────────────────────

    fun startOrganize() {
        _uiState.update { it.copy(step = AiOrganizerStep.SELECT_SCOPE) }
    }

    fun cancelOrganize() {
        _uiState.update {
            it.copy(
                step = AiOrganizerStep.IDLE,
                plan = null,
                errorMessage = null
            )
        }
    }

    fun setScope(scope: String) {
        _uiState.update { it.copy(selectedScope = scope) }
    }

    fun generatePlan() {
        viewModelScope.launch {
            val state = _uiState.value
            val model = AI_MODELS.find { it.id == state.selectedModelId } ?: return@launch
            val apiKey = state.apiKeys[model.provider] ?: ""

            if (apiKey.isBlank()) {
                _uiState.update { it.copy(
                    step = AiOrganizerStep.ERROR,
                    errorMessage = context.getString(com.linksi.app.R.string.no_api_key_for, model.provider.name)
                )}
                return@launch
            }

            _uiState.update { it.copy(step = AiOrganizerStep.GENERATING) }

            val allLinks = repository.getAllLinks().first()
            val folders = repository.getAllFolders().first()

            val linksToOrganize = when (state.selectedScope) {
                OrganizeScope.UNORGANIZED -> allLinks.filter { it.folderId == null }
                else -> allLinks
            }

            if (linksToOrganize.isEmpty()) {
                _uiState.update { it.copy(
                    step = AiOrganizerStep.ERROR,
                    errorMessage = context.getString(com.linksi.app.R.string.error_no_links_to_organize)
                )}
                return@launch
            }

            // ── Batch processing ──────────────────────────────
            val batches = linksToOrganize.chunked(state.batchSize)
            val allLinkPlans = mutableListOf<LinkOrganizePlan>()
            val allNewFolders = mutableListOf<NewFolderPlan>()
            for ((index, batch) in batches.withIndex()) {
                _uiState.update { it.copy(
                    batchProgress = if (batches.size > 1)
                        context.getString(com.linksi.app.R.string.processing_batch, index + 1, batches.size)
                    else ""
                )}

                val result = service.generateOrganizePlan(
                    links = batch,
                    existingFolders = folders,
                    model = model,
                    apiKey = apiKey
                )

                result.fold(
                    onSuccess = { response ->
                        try {
                            val plan = service.parseAiResponse(response, batch, folders)
                            allLinkPlans.addAll(plan.linkPlans)
                            allNewFolders.addAll(plan.newFolders)
                        } catch (e: Exception) {
                            _uiState.update { it.copy(
                                step = AiOrganizerStep.ERROR,
                                errorMessage = context.getString(com.linksi.app.R.string.error_failed_to_parse_ai, e.message ?: "")
                            )}
                            return@launch
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(
                            step = AiOrganizerStep.ERROR,
                            errorMessage = mapErrorToMessage(e)
                        )}
                        return@launch
                    }
                )
            }

            _uiState.update { it.copy(
                step = AiOrganizerStep.PREVIEW,
                plan = OrganizePlan(
                    linkPlans = allLinkPlans,
                    allNewFolders.distinctBy { it.name }                 )
            )}
        }
    }

    fun applyPlan() {
        viewModelScope.launch {
            val plan = _uiState.value.plan ?: return@launch
            val folders = repository.getAllFolders().first()

            _uiState.update {
                it.copy(
                    step = AiOrganizerStep.APPLYING,
                    applyTotal = plan.linkPlans.size,
                    applyProgress = 0
                )
            }

            // Save snapshot for revert BEFORE making any changes
            val snapshot = plan.linkPlans.map { lp ->
                LinkSnapshot(linkId = lp.link.id, originalFolderId = lp.link.folderId)
            }

            // Create new folders first
            val newFolderIdMap = mutableMapOf<String, Long>()
            plan.newFolders.forEach { folderPlan ->
                val newId = repository.insertFolder(
                    Folder(
                        name = folderPlan.name,
                        icon = folderPlan.icon,
                        color = folderPlan.color
                    )
                )
                newFolderIdMap[folderPlan.name] = newId
            }

            // Apply link assignments
            plan.linkPlans.forEachIndexed { index, lp ->
                val targetFolderId = if (lp.isNewFolder) {
                    newFolderIdMap[lp.targetFolderName]
                } else {
                    lp.targetFolderId
                }
                repository.moveToFolder(lp.link.id, targetFolderId)
                _uiState.update { it.copy(applyProgress = index + 1) }
            }

            // Save session for revert
            val session = AiOrganizerSession(
                sessionId = plan.sessionId,
                timestamp = System.currentTimeMillis(),
                movedLinks = snapshot,
                createdFolderNames = plan.newFolders.map { it.name }
            )
            saveSession(session)

            _uiState.update {
                it.copy(
                    step = AiOrganizerStep.DONE,
                    lastSession = session,
                    hasRevertableSession = true
                )
            }
        }
    }

    fun resetToIdle() {
        _uiState.update {
            it.copy(
                step = AiOrganizerStep.IDLE,
                plan = null,
                errorMessage = null
            )
        }
    }

    fun onScreenOpened() {
        val currentStep = _uiState.value.step
        if (currentStep == AiOrganizerStep.DONE || currentStep == AiOrganizerStep.ERROR) {
            _uiState.update {
                it.copy(
                    step = AiOrganizerStep.IDLE,
                    plan = null,
                    errorMessage = null
                )
            }
        }
    }

    // ── Revert ────────────────────────────────────────────────

    fun revertLastSession() {
        viewModelScope.launch {
            val session = _uiState.value.lastSession ?: return@launch

            // Restore each link to original folder
            session.movedLinks.forEach { snapshot ->
                repository.moveToFolder(snapshot.linkId, snapshot.originalFolderId)
            }

            // Delete folders that were created by AI
            session.createdFolderNames.forEach { name ->
                val folder = repository.getFolderByName(name)
                folder?.let { repository.deleteFolder(it) }
            }

            clearSession()
            _uiState.update {
                it.copy(
                    hasRevertableSession = false,
                    lastSession = null,
                    step = AiOrganizerStep.IDLE
                )
            }
        }
    }

    // ── Session persistence ───────────────────────────────────

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null, testErrorMessage = null) }
    }

    private val SESSION_KEY = stringPreferencesKey("ai_last_session")

    private fun saveSession(session: AiOrganizerSession) {
        viewModelScope.launch {
            try {
                val sessionKey = stringPreferencesKey("ai_last_session")
                val json = JSONObject().apply {
                    put("sessionId", session.sessionId)
                    put("timestamp", session.timestamp)
                    put("movedLinks", JSONArray(session.movedLinks.map { snap ->
                        JSONObject().apply {
                            put("linkId", snap.linkId)
                            put("originalFolderId", snap.originalFolderId ?: JSONObject.NULL)
                        }
                    }))
                    put("createdFolderNames", JSONArray(session.createdFolderNames))
                }.toString()
                context.dataStore.edit { it[sessionKey] = json }
            } catch (e: Exception) {
            }
        }
    }

    private fun loadLastSession() {
        viewModelScope.launch {
            try {
                val sessionKey = stringPreferencesKey("ai_last_session")
                val prefs = context.dataStore.data.first()
                val json = prefs[sessionKey] ?: return@launch

                val obj = JSONObject(json)
                val movedLinks = mutableListOf<LinkSnapshot>()
                val arr = obj.getJSONArray("movedLinks")
                for (i in 0 until arr.length()) {
                    val snap = arr.getJSONObject(i)
                    movedLinks.add(
                        LinkSnapshot(
                            linkId = snap.getLong("linkId"),
                            originalFolderId = if (snap.isNull("originalFolderId")) null
                            else snap.getLong("originalFolderId")
                        )
                    )
                }
                val createdNames = mutableListOf<String>()
                val namesArr = obj.getJSONArray("createdFolderNames")
                for (i in 0 until namesArr.length()) {
                    createdNames.add(namesArr.getString(i))
                }
                val session = AiOrganizerSession(
                    sessionId = obj.getString("sessionId"),
                    timestamp = obj.getLong("timestamp"),
                    movedLinks = movedLinks,
                    createdFolderNames = createdNames
                )
                _uiState.update {
                    it.copy(
                        lastSession = session,
                        hasRevertableSession = true
                    )
                }
            } catch (e: Exception) {
                // Silently ignore — no session or corrupted
            }
        }
    }

    private fun clearSession() {
        viewModelScope.launch {
            context.dataStore.edit { it.remove(SESSION_KEY) }
        }
    }
}