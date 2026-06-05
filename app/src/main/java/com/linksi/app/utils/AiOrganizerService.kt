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
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(links, existingFolders)
            val response = when (model.provider) {
                AiProvider.OPENAI -> callOpenAi(prompt, model.modelId, apiKey)
                AiProvider.ANTHROPIC -> callAnthropic(prompt, model.modelId, apiKey)
                AiProvider.GEMINI -> callGemini(prompt, model.modelId, apiKey)
                AiProvider.DEEPSEEK -> callDeepSeek(prompt, model.modelId, apiKey)
                AiProvider.GROK -> callGrok(prompt, model.modelId, apiKey)
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
            """{"id": ${f.id}, "name": "${f.name.replace("\"", "'")}"}"""
        }.joinToString(",\n")

        val availableIcons = "folder, work, bookmark, star, heart, code, school, movie, music, shopping, travel, food, health, news, game, finance, science, design, home, photo, book, fitness, tech, nature, pets, video, article, recipe, idea, social, events, gifts, automotive, tool, family, sports, reading, writing, productivity, ai, audio, security, cloud, weather, history, archive, personal, wallet, coffee, entertainment, mobile, camera, medicine, banking, map, messaging, email, key, flag, rocket, support, wellness, celebration"
        val availableColors = "#6366F1, #8B5CF6, #EC4899, #EF4444, #F59E0B, #10B981, #06B6D4, #3B82F6, #84CC16, #F97316"

        return """
You are a smart link organizer. Organize the following saved links into folders.

EXISTING FOLDERS (use these when appropriate):
[$foldersJson]

LINKS TO ORGANIZE:
[$linksJson]

RULES:
1. Use existing folders when they fit well
2. Create new folders only when necessary — keep total new folders under 6
3. Group similar content together
4. Folder names should be short (1-3 words)
5. Every link must be assigned to exactly one folder
6. For each new folder, pick the most relatable icon and color from the provided list.

AVAILABLE ICONS:
$availableIcons

AVAILABLE COLORS:
$availableColors

Respond ONLY with valid JSON in this exact format, no other text:
{
  "assignments": [
    {"linkId": 1, "folderName": "existing or new folder name", "isExistingFolder": true, "existingFolderId": 5}
  ],
  "newFolders": [
    {"name": "New Folder Name", "icon": "icon_name", "color": "hex_color"}
  ]
}

For existing folders set isExistingFolder=true and existingFolderId=<the folder id>.
For new folders set isExistingFolder=false and existingFolderId=null.
        """.trimIndent()
    }

    private fun callOpenAi(prompt: String, modelId: String, apiKey: String): String {
        val url = java.net.URL("https://api.openai.com/v1/chat/completions")
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

    private fun callAnthropic(prompt: String, modelId: String, apiKey: String): String {
        val url = java.net.URL("https://api.anthropic.com/v1/messages")
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

    private fun callGemini(prompt: String, modelId: String, apiKey: String): String {
        val url = java.net.URL(
            "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"
        )
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

    private fun callDeepSeek(prompt: String, modelId: String, apiKey: String): String {
        val url = java.net.URL("https://api.deepseek.com/chat/completions")
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

    private fun callGrok(prompt: String, modelId: String, apiKey: String): String {
        val url = java.net.URL("https://api.x.ai/v1/chat/completions")
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
        body: String,
        headers: Map<String, String>,
        parseResponse: (String) -> String
    ): String {
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30000
            readTimeout = 60000
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        connection.outputStream.use { it.write(body.toByteArray()) }
        val responseCode = connection.responseCode
        val responseText = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().readText()
        } else {
            val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw Exception("HTTP $responseCode: $error")
        }
        return parseResponse(responseText)
    }

    fun parseAiResponse(
        response: String,
        links: List<Link>,
        existingFolders: List<Folder>
    ): OrganizePlan {
        // Strip markdown code blocks if present
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
                            icon = item.optString("icon", "folder"),
                            color = item.optString("color", "#6366F1")
                        )
                    )
                } else if (item is String) {
                    // Fallback for older models/unexpected formats
                    newFolders.add(NewFolderPlan(item, "folder", "#6366F1"))
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