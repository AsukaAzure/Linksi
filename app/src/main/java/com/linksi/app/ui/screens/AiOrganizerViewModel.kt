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
    val apiBaseUrls: Map<AiProvider, String> = emptyMap(),
    val availableModels: List<AiModel> = AI_MODELS,

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
        observeModels()
        loadLastSession()
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
            AiModelRegistry.refreshAll(context, _uiState.value.apiKeys, _uiState.value.apiBaseUrls)
        }
    }

    // ── Settings ──────────────────────────────────────────────

    private fun loadSettings() {
        viewModelScope.launch {
            context.dataStore.data.collect { prefs ->
                val keys = mapOf(
                    AiProvider.OPENAI to SecureKeyStorage.getKey(context, AiProvider.OPENAI),
                    AiProvider.ANTHROPIC to SecureKeyStorage.getKey(context, AiProvider.ANTHROPIC),
                    AiProvider.GEMINI to SecureKeyStorage.getKey(context, AiProvider.GEMINI),
                    AiProvider.DEEPSEEK to SecureKeyStorage.getKey(context, AiProvider.DEEPSEEK),
                    AiProvider.GROK to SecureKeyStorage.getKey(context, AiProvider.GROK),
                    AiProvider.OPENROUTER to SecureKeyStorage.getKey(context, AiProvider.OPENROUTER)
                )
                val urls = mapOf(
                    AiProvider.OPENAI to (prefs[AI_URL_OPENAI] ?: ""),
                    AiProvider.ANTHROPIC to (prefs[AI_URL_ANTHROPIC] ?: ""),
                    AiProvider.GEMINI to (prefs[AI_URL_GEMINI] ?: ""),
                    AiProvider.DEEPSEEK to (prefs[AI_URL_DEEPSEEK] ?: ""),
                    AiProvider.GROK to (prefs[AI_URL_GROK] ?: ""),
                    AiProvider.OPENROUTER to (prefs[AI_URL_OPENROUTER] ?: "")
                )
                _uiState.update {
                    it.copy(
                        isEnabled = prefs[AI_ENABLED] ?: false,
                        selectedModelId = prefs[AI_SELECTED_MODEL] ?: DEFAULT_AI_MODEL_ID,
                        apiKeys = keys,
                        apiBaseUrls = urls
                    )
                }
            }
        }
    }

    fun setBatchSize(size: Int) {
        _uiState.update { it.copy(batchSize = size) }
    }

    /**
     * Resolve the selected model, healing a stored id that no longer exists
     * (renamed/deprecated model) to the best available option.
     */
    private fun resolveModel(): AiModel? {
        val state = _uiState.value
        state.availableModels.find { it.id == state.selectedModelId }?.let { return it }
        val configured = state.availableModels.filter { !state.apiKeys[it.provider].isNullOrBlank() }
        val healed = configured.find { it.isRecommended }
            ?: configured.firstOrNull()
            ?: state.availableModels.find { it.isRecommended }
            ?: state.availableModels.firstOrNull()
        healed?.let { setSelectedModel(it.id) }
        return healed
    }

    fun testModel() {
        viewModelScope.launch {
            val state = _uiState.value
            val model = resolveModel()
            if (model == null) {
                _uiState.update { it.copy(
                    modelStatus = SettingsViewModel.ModelStatus.ERROR,
                    testErrorMessage = context.getString(com.linksi.app.R.string.no_model_selected)
                )}
                return@launch
            }
            val apiKey = state.apiKeys[model.provider] ?: ""
            val baseUrl = state.apiBaseUrls[model.provider] ?: ""

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
                    apiKey = apiKey,
                    baseUrl = baseUrl
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
            msg.contains("400") && msg.contains("invalid", ignoreCase = true) -> context.getString(com.linksi.app.R.string.error_invalid_request)
            msg.contains("401") -> context.getString(com.linksi.app.R.string.invalid_api_key_error)
            msg.contains("403") -> context.getString(com.linksi.app.R.string.unauthorized_api_key_error)
            msg.contains("429") || msg.contains("quota", ignoreCase = true) ->
                context.getString(com.linksi.app.R.string.quota_exceeded_error)
            msg.contains("insufficient", ignoreCase = true) -> context.getString(com.linksi.app.R.string.error_insufficient_credits)
            msg.contains("Unable to resolve host") -> context.getString(com.linksi.app.R.string.error_network)
            msg.length > 10 && !msg.startsWith("HTTP") -> msg
            else -> context.getString(com.linksi.app.R.string.generic_error_prefix, msg.take(100))
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
            SecureKeyStorage.saveKey(context, provider, key)
            _uiState.update { it.copy(modelStatus = SettingsViewModel.ModelStatus.UNKNOWN, testErrorMessage = null, apiKeys = it.apiKeys + (provider to key)) }
            try {
                AiModelRegistry.refreshProvider(context, provider, key, _uiState.value.apiBaseUrls[provider] ?: "")
            } catch (e: Exception) {
                // Ignore background refresh failure
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
            val model = resolveModel()
            if (model == null) {
                _uiState.update { it.copy(
                    step = AiOrganizerStep.ERROR,
                    errorMessage = context.getString(com.linksi.app.R.string.no_model_selected)
                )}
                return@launch
            }
            val apiKey = state.apiKeys[model.provider] ?: ""
            val baseUrl = state.apiBaseUrls[model.provider] ?: ""

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
                    apiKey = apiKey,
                    baseUrl = baseUrl
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
                    newFolders = allNewFolders.distinctBy { it.name }
                )
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

            val snapshot = plan.linkPlans.map { lp ->
                LinkSnapshot(linkId = lp.link.id, originalFolderId = lp.link.folderId)
            }

            val newFolderIdMap = mutableMapOf<String, Long>()
            val remainingFolders = plan.newFolders.toMutableList()
            
            while (remainingFolders.isNotEmpty()) {
                val iterator = remainingFolders.iterator()
                var insertedThisPass = 0
                while (iterator.hasNext()) {
                    val nf = iterator.next()
                    val resolvedParentId = when {
                        nf.parentId == null -> null
                        newFolderIdMap.containsKey(nf.parentId) -> newFolderIdMap[nf.parentId]
                        else -> {
                            nf.parentId.toLongOrNull()
                        }
                    }

                    if (nf.parentId == null || resolvedParentId != null) {
                        val newFolder = Folder(
                            name = nf.name,
                            icon = nf.icon,
                            color = nf.color,
                            parentId = resolvedParentId
                        )
                        val newId = repository.insertFolder(newFolder)
                        newFolderIdMap[nf.name] = newId
                        iterator.remove()
                        insertedThisPass++
                    }
                }
                if (insertedThisPass == 0 && remainingFolders.isNotEmpty()) {
                    for (nf in remainingFolders) {
                        val newFolder = Folder(name = nf.name, icon = nf.icon, color = nf.color, parentId = null)
                        val newId = repository.insertFolder(newFolder)
                        newFolderIdMap[nf.name] = newId
                    }
                    remainingFolders.clear()
                }
            }

            for ((index, lp) in plan.linkPlans.withIndex()) {
                val targetFolderId = lp.targetFolderId ?: newFolderIdMap[lp.targetFolderName]
                if (targetFolderId != null) {
                    repository.moveToFolder(lp.link.id, targetFolderId)
                }
                _uiState.update { it.copy(applyProgress = index + 1) }
            }

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

    fun revertLastOrganization() {
        val session = _uiState.value.lastSession ?: return
        viewModelScope.launch {
            for (snapshot in session.movedLinks) {
                repository.moveToFolder(snapshot.linkId, snapshot.originalFolderId)
            }
            clearLastSession()
            _uiState.update {
                it.copy(
                    lastSession = null,
                    hasRevertableSession = false,
                    snackbarMessage = context.getString(com.linksi.app.R.string.undo_organization)
                )
            }
        }
    }

    private fun saveSession(session: AiOrganizerSession) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[AI_LAST_SESSION] = JSONObject().apply {
                    put("sessionId", session.sessionId)
                    put("timestamp", session.timestamp)
                    put("movedLinks", JSONArray().apply {
                        session.movedLinks.forEach { ml ->
                            put(JSONObject().apply {
                                put("linkId", ml.linkId)
                                put("originalFolderId", ml.originalFolderId ?: -1L)
                            })
                        }
                    })
                }.toString()
            }
        }
    }

    private fun loadLastSession() {
        viewModelScope.launch {
            context.dataStore.data.collect { prefs ->
                val jsonStr = prefs[AI_LAST_SESSION] ?: return@collect
                try {
                    val obj = JSONObject(jsonStr)
                    val sessionId = obj.getString("sessionId")
                    val timestamp = obj.getLong("timestamp")
                    val linksArr = obj.getJSONArray("movedLinks")
                    val movedLinks = mutableListOf<LinkSnapshot>()
                    for (i in 0 until linksArr.length()) {
                        val mlObj = linksArr.getJSONObject(i)
                        val linkId = mlObj.getLong("linkId")
                        val origId = mlObj.getLong("originalFolderId").let { if (it == -1L) null else it }
                        movedLinks.add(LinkSnapshot(linkId, origId))
                    }
                    val session = AiOrganizerSession(sessionId, timestamp, movedLinks, emptyList())
                    _uiState.update {
                        it.copy(
                            lastSession = session,
                            hasRevertableSession = true
                        )
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    private fun clearLastSession() {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs.remove(AI_LAST_SESSION)
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun onScreenOpened() {}

    fun revertLastSession() {
        revertLastOrganization()
    }

    fun resetToIdle() {
        cancelOrganize()
    }

    companion object {
        private val AI_LAST_SESSION = stringPreferencesKey("ai_last_session")
    }
}
