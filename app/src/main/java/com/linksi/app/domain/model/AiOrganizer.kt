package com.linksi.app.domain.model

data class AiModel(
    val id: String,
    val name: String,
    val provider: AiProvider,
    val modelId: String,
    val isLatest: Boolean = false,
    val isRecommended: Boolean = false
)

enum class AiProvider {
    OPENAI, ANTHROPIC, GEMINI, DEEPSEEK, GROK, OPENROUTER
}

// Default model used until the user picks one. Discovery replaces these
// curated entries with the provider's latest models once an API key is saved.
const val DEFAULT_AI_MODEL_ID = "gemini-2.5-flash"

// Latest-generation models only — full catalogs are intentionally not shown.
val AI_MODELS = listOf(
    // OpenAI
    AiModel("gpt-5.1", "GPT-5.1", AiProvider.OPENAI, "gpt-5.1", isLatest = true, isRecommended = true),
    AiModel("gpt-5", "GPT-5", AiProvider.OPENAI, "gpt-5", isLatest = true, isRecommended = true),
    AiModel("gpt-5-mini", "GPT-5 Mini", AiProvider.OPENAI, "gpt-5-mini", isLatest = true, isRecommended = false),
    // Anthropic
    AiModel("claude-opus-5-5", "Claude Opus 5.5", AiProvider.ANTHROPIC, "claude-opus-5-5", isLatest = true, isRecommended = true),
    AiModel("claude-sonnet-5", "Claude Sonnet 5", AiProvider.ANTHROPIC, "claude-sonnet-5", isLatest = true, isRecommended = true),
    AiModel("claude-haiku-4-5", "Claude Haiku 4.5", AiProvider.ANTHROPIC, "claude-haiku-4-5", isLatest = true, isRecommended = false),
    // Google Gemini
    AiModel("gemini-3.1-pro-preview", "Gemini 3.1 Pro", AiProvider.GEMINI, "gemini-3.1-pro-preview", isLatest = true, isRecommended = true),
    AiModel("gemini-3-flash-preview", "Gemini 3 Flash", AiProvider.GEMINI, "gemini-3-flash-preview", isLatest = true, isRecommended = true),
    AiModel("gemini-2.5-flash", "Gemini 2.5 Flash", AiProvider.GEMINI, "gemini-2.5-flash", isLatest = false, isRecommended = true),
    // DeepSeek
    AiModel("deepseek-chat", "DeepSeek Chat", AiProvider.DEEPSEEK, "deepseek-chat", isLatest = true, isRecommended = true),
    AiModel("deepseek-reasoner", "DeepSeek Reasoner", AiProvider.DEEPSEEK, "deepseek-reasoner", isLatest = true, isRecommended = false),
    // xAI Grok
    AiModel("grok-4", "Grok 4", AiProvider.GROK, "grok-4", isLatest = true, isRecommended = true),
    AiModel("grok-4-fast-reasoning", "Grok 4 Fast", AiProvider.GROK, "grok-4-fast-reasoning", isLatest = true, isRecommended = false),
    // OpenRouter
    AiModel("openai/gpt-5.1", "OpenRouter: GPT-5.1", AiProvider.OPENROUTER, "openai/gpt-5.1", isLatest = true, isRecommended = true),
    AiModel("anthropic/claude-sonnet-5", "OpenRouter: Claude Sonnet 5", AiProvider.OPENROUTER, "anthropic/claude-sonnet-5", isLatest = true, isRecommended = true),
    AiModel("google/gemini-2.5-flash", "OpenRouter: Gemini 2.5 Flash", AiProvider.OPENROUTER, "google/gemini-2.5-flash", isLatest = true, isRecommended = false)
)

class OrganizeScope {
    companion object {
        const val ALL = "all"
        const val UNORGANIZED = "unorganized"
    }
}

data class LinkOrganizePlan(
    val link: Link,
    val targetFolderName: String,
    val targetFolderId: Long?,   // null = new folder to be created
    val isNewFolder: Boolean,
    val currentFolderName: String?
)

data class NewFolderPlan(
    val name: String,
    val icon: String,
    val color: String,
    val parentId: String? = null // Can be existing ID or name of another new folder
)

data class OrganizePlan(
    val linkPlans: List<LinkOrganizePlan>,
    val newFolders: List<NewFolderPlan>,
    val sessionId: String = System.currentTimeMillis().toString()
)

data class AiOrganizerSession(
    val sessionId: String,
    val timestamp: Long,
    val movedLinks: List<LinkSnapshot>,  // state before organizing
    val createdFolderNames: List<String>
)

data class LinkSnapshot(
    val linkId: Long,
    val originalFolderId: Long?
)
