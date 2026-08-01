package com.linksi.app.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.linksi.app.domain.model.AI_MODELS
import com.linksi.app.domain.model.AiModel
import com.linksi.app.domain.model.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

object AiModelRegistry {
    private val MODELS_KEY = stringPreferencesKey("discovered_ai_models")

    fun getModels(context: Context): Flow<List<AiModel>> {
        return context.dataStore.data.map { prefs ->
            val json = prefs[MODELS_KEY]
            if (json.isNullOrBlank()) {
                AI_MODELS
            } else {
                try {
                    parseModels(json)
                } catch (e: Exception) {
                    AI_MODELS
                }
            }
        }
    }

    suspend fun updateModels(context: Context, newModels: List<AiModel>) {
        val existing = getModels(context).first().toMutableList()
        
        newModels.forEach { newModel ->
            val index = existing.indexOfFirst { (it.modelId == newModel.modelId) && (it.provider == newModel.provider) }
            if (index != -1) {
                existing[index] = newModel
            } else {
                existing.add(newModel)
            }
        }

        val json = serializeModels(existing)
        context.dataStore.edit { it[MODELS_KEY] = json }
    }

    suspend fun refreshAll(context: Context, apiKeys: Map<AiProvider, String>) {
        val service = AiOrganizerService()
        apiKeys.forEach { (provider, key) ->
            if (key.isNotBlank()) {
                try {
                    val discovered = when (provider) {
                        AiProvider.OPENAI -> service.discoverOpenAiModels(key)
                        AiProvider.ANTHROPIC -> service.discoverAnthropicModels(key)
                        AiProvider.GEMINI -> service.discoverGeminiModels(key)
                        AiProvider.DEEPSEEK -> service.discoverDeepSeekModels(key)
                        AiProvider.GROK -> service.discoverGrokModels(key)
                    }
                    updateModels(context, discovered)
                } catch (e: Exception) {
                    // ignore
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
                modelId = obj.getString("modelId")
            ))
        }
        return list
    }
}
