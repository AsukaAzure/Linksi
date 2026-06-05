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
    val errorMessage: String? = null,
    val applyProgress: Int = 0,
    val applyTotal: Int = 0,

    // Revert
    val lastSession: AiOrganizerSession? = null,
    val hasRevertableSession: Boolean = false
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

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[AI_ENABLED] = enabled }
        }
    }

    fun setSelectedModel(modelId: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[AI_SELECTED_MODEL] = modelId }
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
            val model = AI_MODELS.find { it.id == state.selectedModelId }
                ?: return@launch
            val apiKey = state.apiKeys[model.provider] ?: ""

            if (apiKey.isBlank()) {
                _uiState.update {
                    it.copy(
                        step = AiOrganizerStep.ERROR,
                        errorMessage = "No API key set for ${model.provider.name}. Add it in AI Settings."
                    )
                }
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
                _uiState.update {
                    it.copy(
                        step = AiOrganizerStep.ERROR,
                        errorMessage = "No links to organize."
                    )
                }
                return@launch
            }

            val result = service.generateOrganizePlan(
                links = linksToOrganize,
                existingFolders = folders,
                model = model,
                apiKey = apiKey
            )

            result.fold(
                onSuccess = { response ->
                    try {
                        val plan = service.parseAiResponse(response, linksToOrganize, folders)
                        _uiState.update {
                            it.copy(
                                step = AiOrganizerStep.PREVIEW,
                                plan = plan
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                step = AiOrganizerStep.ERROR,
                                errorMessage = "Failed to parse AI response: ${e.message}"
                            )
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            step = AiOrganizerStep.ERROR,
                            errorMessage = e.message ?: "Unknown error"
                        )
                    }
                }
            )
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
            plan.newFoldersToCreate.forEach { name ->
                // Pick a color and icon automatically
                val colorOptions = listOf(
                    "#6366F1", "#8B5CF6", "#EC4899", "#EF4444",
                    "#F59E0B", "#10B981", "#06B6D4", "#3B82F6", "#84CC16", "#F97316"
                )
                val iconOptions = listOf(
                    "folder", "work", "bookmark", "star", "code",
                    "school", "movie", "music", "shopping", "travel",
                    "food", "health", "news", "game", "design", "home"
                )
                val index = newFolderIdMap.size
                val color = colorOptions[index % colorOptions.size]
                val icon = iconOptions[index % iconOptions.size]

                val newId = repository.insertFolder(
                    Folder(name = name, icon = icon, color = color)
                )
                newFolderIdMap[name] = newId
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
                createdFolderNames = plan.newFoldersToCreate
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
        if (_uiState.value.step == AiOrganizerStep.DONE) {
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
            val allFolders = repository.getAllFolders().first()
            session.createdFolderNames.forEach { name ->
                val folder = allFolders.find { it.name == name }
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