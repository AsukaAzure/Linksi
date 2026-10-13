package com.linksi.app.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.linksi.app.domain.model.AiModel
import com.linksi.app.domain.model.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

object AiModelRegistry {
    private val MODELS_KEY = stringPreferencesKey("discovered_ai_models")
    private val MODELS_TIMESTAMP_KEY = longPreferencesKey("discovered_ai_models_timestamp")

    fun getModels(context: Context): Flow<List<AiModel>> {
        return context.dataStore.data.map { prefs ->
            val json = prefs[MODELS_KEY]
            if (json.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    parseModels(json)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    suspend fun updateModels(context: Context, newModels: List<AiModel>, provider: AiProvider) {
        val existing = getModels(context).first().toMutableList()
        existing.removeAll { it.provider == provider }
        existing.addAll(newModels)

        val json = serializeModels(existing)
        context.dataStore.edit { prefs ->
            prefs[MODELS_KEY] = json
            prefs[MODELS_TIMESTAMP_KEY] = System.currentTimeMillis()
        }
    }

    suspend fun refreshProvider(context: Context, provider: AiProvider, apiKey: String, baseUrl: String) {
        if (apiKey.isBlank()) return
        val service = AiOrganizerService()
        val discovered = when (provider) {
            AiProvider.OPENAI -> service.discoverOpenAiModels(apiKey, baseUrl)
            AiProvider.ANTHROPIC -> service.discoverAnthropicModels(apiKey, baseUrl)
            AiProvider.GEMINI -> service.discoverGeminiModels(apiKey, baseUrl)
            AiProvider.DEEPSEEK -> service.discoverDeepSeekModels(apiKey, baseUrl)
            AiProvider.GROK -> service.discoverGrokModels(apiKey, baseUrl)
        }
        updateModels(context, discovered, provider)
    }

    suspend fun refreshAll(context: Context, apiKeys: Map<AiProvider, String>, apiBaseUrls: Map<AiProvider, String>) {
        apiKeys.forEach { (provider, key) ->
            if (key.isNotBlank()) {
                try {
                    val baseUrl = apiBaseUrls[provider] ?: ""
                    refreshProvider(context, provider, key, baseUrl)
                } catch (e: Exception) {
                    // Ignore individual failures during background refresh
                }
            }
        }
    }

    private fun serializeModels(models: List<AiModel>): String {
        val arr = JSONArray()
        models.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("provider", m.provider.name)
                put("modelId", m.modelId)
                put("isLatest", m.isLatest)
                put("isRecommended", m.isRecommended)
            })
        }
        return arr.toString()
    }

    private fun parseModels(json: String): List<AiModel> {
        val arr = JSONArray(json)
        val list = mutableListOf<AiModel>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(AiModel(
                id = obj.getString("id"),
                name = obj.getString("name"),
                provider = AiProvider.valueOf(obj.getString("provider")),
                modelId = obj.getString("modelId"),
                isLatest = obj.optBoolean("isLatest", false),
                isRecommended = obj.optBoolean("isRecommended", false)
            ))
        }
        return list
    }
}
