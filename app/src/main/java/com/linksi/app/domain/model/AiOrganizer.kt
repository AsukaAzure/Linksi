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
    OPENAI, ANTHROPIC, GEMINI, DEEPSEEK, GROK
}

val AI_MODELS = listOf(
    AiModel("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", AiProvider.ANTHROPIC, "claude-3-5-sonnet-20241022", isLatest = true, isRecommended = true),
    AiModel("gpt-4o", "GPT-4o", AiProvider.OPENAI, "gpt-4o", isLatest = true, isRecommended = true),
    AiModel("gemini-1.5-pro", "Gemini 1.5 Pro", AiProvider.GEMINI, "gemini-1.5-pro", isLatest = true, isRecommended = true),
    AiModel("deepseek-chat", "DeepSeek Chat", AiProvider.DEEPSEEK, "deepseek-chat", isLatest = true, isRecommended = true),
    AiModel("grok-beta", "Grok Beta", AiProvider.GROK, "grok-beta", isLatest = true, isRecommended = true)
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
