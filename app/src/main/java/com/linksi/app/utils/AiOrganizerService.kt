package com.linksi.app.utils

import com.linksi.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AiOrganizerService {

    suspend fun generateOrganizePlan(
        links: List<Link>,
        existingFolders: List<Folder>,
        model: AiModel,
        apiKey: String,
        baseUrl: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(links, existingFolders)
            val response = when (model.provider) {
                AiProvider.OPENAI -> callOpenAi(prompt, model.modelId, apiKey, baseUrl)
                AiProvider.ANTHROPIC -> callAnthropic(prompt, model.modelId, apiKey, baseUrl)
                AiProvider.GEMINI -> callGemini(prompt, model.modelId, apiKey, baseUrl)
                AiProvider.DEEPSEEK -> callDeepSeek(prompt, model.modelId, apiKey, baseUrl)
                AiProvider.GROK -> callGrok(prompt, model.modelId, apiKey, baseUrl)
            }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(links: List<Link>, folders: List<Folder>): String {
        val linksJson = links.mapIndexed { i, link ->
            """{"index": $i, "id": ${link.id}, "title": "${link.title.replace("\"", "'")}", "domain": "${link.domain}", "url": "${link.url}"}"""
        }.joinToString(",\n")

        val foldersJson = folders.map { f ->
            """{"id": ${f.id}, "name": "${f.name.replace("\"", "'")}", "parentId": ${f.parentId ?: "null"}}"""
        }.joinToString(",\n")

        return """
You are a smart link organizer. Organize the following saved links into folders.
The app supports nested folders (folders within folders).

EXISTING FOLDERS (with their parent IDs):
[$foldersJson]

LINKS TO ORGANIZE:
[$linksJson]

RULES:
1. Use existing folders when they fit well.
2. Create new folders only when necessary — keep total new folders under 6.
3. Group similar content together.
4. Folder names should be short (1-3 words).
5. Every link must be assigned to exactly one folder.
6. You can create sub-folders by setting a parentId for new folders (referencing an existing folder's ID or another new folder's name/temporary ID).

Respond ONLY with valid JSON in this exact format, no other text:
{
  "assignments": [
    {"linkId": 1, "folderName": "existing or new folder name", "isExistingFolder": true, "existingFolderId": 5}
  ],
  "newFolders": [
    {"name": "New Folder Name", "parentId": null}
  ]
}

For existing folders, set isExistingFolder=true and existingFolderId=<the folder id>.
For new folders, set isExistingFolder=false and existingFolderId=null.
If a new folder is a sub-folder, set parentId to the ID of the existing parent folder OR the name of another new folder being created.
        """.trimIndent()
    }

    private fun callOpenAi(prompt: String, modelId: String, apiKey: String, baseUrl: String): String {
        val endpoint = if (baseUrl.isNotBlank()) "${baseUrl.removeSuffix("/")}/chat/completions" else "https://api.openai.com/v1/chat/completions"
        val url = java.net.URL(endpoint)
        val body = JSONObject().apply {
            put("model", modelId)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.3)
        }.toString()

        return makeHttpRequest(url, body, mapOf(
            "Authorization" to "Bearer $apiKey",
            "Content-Type" to "application/json"
        )) { response ->
            JSONObject(response)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }

    private fun callAnthropic(prompt: String, modelId: String, apiKey: String, baseUrl: String): String {
        val endpoint = if (baseUrl.isNotBlank()) "${baseUrl.removeSuffix("/")}/messages" else "https://api.anthropic.com/v1/messages"
        val url = java.net.URL(endpoint)
        val body = JSONObject().apply {
            put("model", modelId)
            put("max_tokens", 2000)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }.toString()

        return makeHttpRequest(url, body, mapOf(
            "x-api-key" to apiKey,
            "anthropic-version" to "2023-06-01",
            "Content-Type" to "application/json"
        )) { response ->
            JSONObject(response)
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
        }
    }

    private fun callGemini(prompt: String, modelId: String, apiKey: String, baseUrl: String): String {
        val endpoint = if (baseUrl.isNotBlank()) {
            if (baseUrl.contains("?")) baseUrl else "$baseUrl/models/$modelId:generateContent?key=$apiKey"
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"
        }
        val url = java.net.URL(endpoint)
        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
            })
        }.toString()

        return makeHttpRequest(url, body, mapOf(
            "Content-Type" to "application/json"
        )) { response ->
            JSONObject(response)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }

    private fun callDeepSeek(prompt: String, modelId: String, apiKey: String, baseUrl: String): String {
        val endpoint = if (baseUrl.isNotBlank()) "${baseUrl.removeSuffix("/")}/chat/completions" else "https://api.deepseek.com/chat/completions"
        val url = java.net.URL(endpoint)
        val body = JSONObject().apply {
            put("model", modelId)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.3)
        }.toString()

        return makeHttpRequest(url, body, mapOf(
            "Authorization" to "Bearer $apiKey",
            "Content-Type" to "application/json"
        )) { response ->
            JSONObject(response)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }

    private fun callGrok(prompt: String, modelId: String, apiKey: String, baseUrl: String): String {
        val endpoint = if (baseUrl.isNotBlank()) "${baseUrl.removeSuffix("/")}/chat/completions" else "https://api.x.ai/v1/chat/completions"
        val url = java.net.URL(endpoint)
        val body = JSONObject().apply {
            put("model", modelId)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.3)
        }.toString()

        return makeHttpRequest(url, body, mapOf(
            "Authorization" to "Bearer $apiKey",
            "Content-Type" to "application/json"
        )) { response ->
            JSONObject(response)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }

    private fun makeHttpRequest(
        url: java.net.URL,
        body: String?,
        headers: Map<String, String>,
        method: String = "POST",
        parseResponse: (String) -> String
    ): String {
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.apply {
            requestMethod = method
            doOutput = body != null
            connectTimeout = 30000
            readTimeout = 60000
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        body?.let {
            connection.outputStream.use { os -> os.write(it.toByteArray()) }
        }
        val responseCode = connection.responseCode
        val responseText = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().readText()
        } else {
            val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: ""
            val detailedMessage = try {
                val json = JSONObject(errorBody)
                when {
                    json.has("error") -> {
                        val errorObj = json.get("error")
                        if (errorObj is JSONObject) errorObj.optString("message", errorBody)
                        else if (errorObj is String) errorObj
                        else errorBody
                    }
                    json.has("message") -> json.getString("message")
                    else -> errorBody
                }
            } catch (e: Exception) {
                errorBody
            }
            throw Exception(detailedMessage.ifBlank { "HTTP $responseCode: Unknown Error" })
        }
        return parseResponse(responseText)
    }

    suspend fun discoverOpenAiModels(apiKey: String, baseUrl: String = ""): List<AiModel> = withContext(Dispatchers.IO) {
        val endpoint = if (baseUrl.isNotBlank()) "${baseUrl.removeSuffix("/")}/models" else "https://api.openai.com/v1/models"
        val url = java.net.URL(endpoint)
        val response = makeHttpRequest(url, null, mapOf(
            "Authorization" to "Bearer $apiKey"
        ), "GET") { it }

        val json = JSONObject(response)
        val data = json.getJSONArray("data")
        val models = mutableListOf<AiModel>()
        
        val chatRegex = Regex("^(gpt|o1|o3).*", RegexOption.IGNORE_CASE)
        
        for (i in 0 until data.length()) {
            val m = data.getJSONObject(i)
            val id = m.getString("id")
            if (chatRegex.containsMatchIn(id) && !id.contains("embedding") && !id.contains("audio") && !id.contains("vision") && !id.contains("moderation") && !id.contains("realtime") && !id.contains("tts") && !id.contains("whisper") && !id.contains("dall-e")) {
                val isRec = id.contains("gpt-4o") || id.contains("o1") || id.contains("o3-mini")
                models.add(AiModel(id, id.replace("-", " ").capitalizeWords(), AiProvider.OPENAI, id, isLatest = true, isRecommended = isRec))
            }
        }
        models.sortedByDescending { it.id }
    }

    suspend fun discoverAnthropicModels(apiKey: String, baseUrl: String = ""): List<AiModel> = withContext(Dispatchers.IO) {
        val endpoint = if (baseUrl.isNotBlank()) "${baseUrl.removeSuffix("/")}/models" else "https://api.anthropic.com/v1/models"
        val url = java.net.URL(endpoint)
        val response = makeHttpRequest(url, null, mapOf(
            "x-api-key" to apiKey,
            "anthropic-version" to "2023-06-01"
        ), "GET") { it }

        val json = JSONObject(response)
        val data = json.getJSONArray("data")
        val models = mutableListOf<AiModel>()
        for (i in 0 until data.length()) {
            val m = data.getJSONObject(i)
            val id = m.getString("id")
            if (id.startsWith("claude", ignoreCase = true) && !id.contains("embedding")) {
                val displayName = m.optString("display_name", id)
                val isRec = id.contains("claude-3-5")
                models.add(AiModel(id, displayName, AiProvider.ANTHROPIC, id, isLatest = true, isRecommended = isRec))
            }
        }
        models.sortedByDescending { it.id }
    }

    suspend fun discoverGeminiModels(apiKey: String, baseUrl: String = ""): List<AiModel> = withContext(Dispatchers.IO) {
        val endpoint = if (baseUrl.isNotBlank()) {
            if (baseUrl.contains("?")) baseUrl else "$baseUrl/models?key=$apiKey"
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
        }
        val url = java.net.URL(endpoint)
        val response = makeHttpRequest(url, null, emptyMap(), "GET") { it }

        val json = JSONObject(response)
        val data = json.getJSONArray("models")
        val models = mutableListOf<AiModel>()
        for (i in 0 until data.length()) {
            val m = data.getJSONObject(i)
            val name = m.getString("name").removePrefix("models/")
            val displayName = m.optString("displayName", name)
            val supportedMethods = m.optJSONArray("supportedGenerationMethods")
            
            var supportsChat = false
            if (supportedMethods != null) {
                for (j in 0 until supportedMethods.length()) {
                    if (supportedMethods.getString(j) == "generateContent") {
                        supportsChat = true
                        break
                    }
                }
            } else {
                supportsChat = true // fallback if field missing
            }

            val isGeminiChat = name.startsWith("gemini-", ignoreCase = true) &&
                    (name.contains("flash") || name.contains("pro") || name.contains("ultra")) &&
                    !name.contains("embedding") &&
                    !name.contains("vision") &&
                    !name.contains("audio") &&
                    !name.contains("imagen") &&
                    !name.contains("gemma") &&
                    !name.contains("lyria") &&
                    !name.contains("nano")

            if (supportsChat && isGeminiChat) {
                val isRec = name.contains("flash") || name.contains("pro")
                models.add(AiModel(name, displayName, AiProvider.GEMINI, name, isLatest = true, isRecommended = isRec))
            }
        }
        models.sortedByDescending { it.id }
    }

    suspend fun discoverDeepSeekModels(apiKey: String, baseUrl: String = ""): List<AiModel> = withContext(Dispatchers.IO) {
        val endpoint = if (baseUrl.isNotBlank()) "${baseUrl.removeSuffix("/")}/models" else "https://api.deepseek.com/models"
        val url = java.net.URL(endpoint)
        val response = makeHttpRequest(url, null, mapOf(
            "Authorization" to "Bearer $apiKey"
        ), "GET") { it }

        val json = JSONObject(response)
        val data = json.getJSONArray("data")
        val models = mutableListOf<AiModel>()
        for (i in 0 until data.length()) {
            val m = data.getJSONObject(i)
            val id = m.getString("id")
            if ((id.contains("chat") || id.contains("reasoner")) && !id.contains("embedding")) {
                val isRec = id.contains("chat") || id.contains("reasoner")
                models.add(AiModel(id, id.replace("-", " ").capitalizeWords(), AiProvider.DEEPSEEK, id, isLatest = true, isRecommended = isRec))
            }
        }
        models.sortedByDescending { it.id }
    }

    suspend fun discoverGrokModels(apiKey: String, baseUrl: String = ""): List<AiModel> = withContext(Dispatchers.IO) {
        val endpoint = if (baseUrl.isNotBlank()) "${baseUrl.removeSuffix("/")}/models" else "https://api.x.ai/v1/models"
        val url = java.net.URL(endpoint)
        val response = makeHttpRequest(url, null, mapOf(
            "Authorization" to "Bearer $apiKey"
        ), "GET") { it }

        val json = JSONObject(response)
        val data = json.getJSONArray("data")
        val models = mutableListOf<AiModel>()
        for (i in 0 until data.length()) {
            val m = data.getJSONObject(i)
            val id = m.getString("id")
            if (id.contains("grok", ignoreCase = true) && !id.contains("embedding") && !id.contains("image") && !id.contains("vision")) {
                models.add(AiModel(id, id.replace("-", " ").capitalizeWords(), AiProvider.GROK, id, isLatest = true, isRecommended = true))
            }
        }
        models.sortedByDescending { it.id }
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

    fun parseAiResponse(
        response: String,
        links: List<Link>,
        existingFolders: List<Folder>
    ): OrganizePlan {
        val cleaned = response
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val json = JSONObject(cleaned)
        val assignments = json.getJSONArray("assignments")
        val newFolders = mutableListOf<NewFolderPlan>()

        json.optJSONArray("newFolders")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.get(i)
                if (item is JSONObject) {
                    newFolders.add(
                        NewFolderPlan(
                            name = item.getString("name"),
                            icon = "folder",
                            color = "#6750A4",
                            parentId = if (item.isNull("parentId")) null else item.opt("parentId")?.toString()
                        )
                    )
                } else if (item is String) {
                    newFolders.add(NewFolderPlan(item, "folder", "#6750A4", null))
                }
            }
        }

        val linkPlans = mutableListOf<LinkOrganizePlan>()

        for (i in 0 until assignments.length()) {
            val assignment = assignments.getJSONObject(i)
            val linkId = assignment.getLong("linkId")
            val folderName = assignment.getString("folderName")
            val isExisting = assignment.optBoolean("isExistingFolder", false)
            val existingFolderId = if (isExisting)
                assignment.optLong("existingFolderId", -1L).takeIf { it != -1L }
            else null

            val link = links.find { it.id == linkId } ?: continue
            val currentFolder = existingFolders.find { it.id == link.folderId }

            linkPlans.add(
                LinkOrganizePlan(
                    link = link,
                    targetFolderName = folderName,
                    targetFolderId = existingFolderId,
                    isNewFolder = !isExisting,
                    currentFolderName = currentFolder?.name
                )
            )
        }

        return OrganizePlan(
            linkPlans = linkPlans,
            newFolders = newFolders
        )
    }
}
