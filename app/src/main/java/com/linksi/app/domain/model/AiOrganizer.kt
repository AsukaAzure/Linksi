package com.linksi.app.domain.model

data class AiModel(
    val id: String,
    val name: String,
    val provider: AiProvider,
    val modelId: String
)

enum class AiProvider {
    OPENAI, ANTHROPIC, GEMINI, DEEPSEEK, GROK
}

val AI_MODELS = listOf(
    // OpenAI
    AiModel("gpt4o", "GPT-4o", AiProvider.OPENAI, "gpt-4o"),
    AiModel("gpt41", "GPT-4.1", AiProvider.OPENAI, "gpt-4.1"),
    AiModel("gpt41mini", "GPT-4.1 Mini", AiProvider.OPENAI, "gpt-4.1-mini"),
    AiModel("o3", "o3", AiProvider.OPENAI, "o3"),

    // Anthropic
    AiModel(
        "claudeSonnet4",
        "Claude Sonnet 4",
        AiProvider.ANTHROPIC,
        "claude-sonnet-4"
    ),

    AiModel(
        "claudeOpus4",
        "Claude Opus 4",
        AiProvider.ANTHROPIC,
        "claude-opus-4"
    ),

    // Gemini
    AiModel(
        "gemini25pro",
        "Gemini 2.5 Pro",
        AiProvider.GEMINI,
        "gemini-2.5-pro"
    ),

    AiModel(
        "gemini25flash",
        "Gemini 2.5 Flash",
        AiProvider.GEMINI,
        "gemini-2.5-flash"
    ),

    AiModel(
        "gemini15flash",
        "Gemini 1.5 Flash",
        AiProvider.GEMINI,
        "gemini-1.5-flash"
    ),

    // DeepSeek
    AiModel(
        "deepseekv3",
        "DeepSeek V3",
        AiProvider.DEEPSEEK,
        "deepseek-chat"
    ),

    AiModel(
        "deepseekr1",
        "DeepSeek R1",
        AiProvider.DEEPSEEK,
        "deepseek-reasoner"
    ),

    // Grok
    AiModel(
        "grok3",
        "Grok 3",
        AiProvider.GROK,
        "grok-3"
    ),

    AiModel(
        "grok3mini",
        "Grok 3 Mini",
        AiProvider.GROK,
        "grok-3-mini"
    )
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

data class OrganizePlan(
    val linkPlans: List<LinkOrganizePlan>,
    val newFoldersToCreate: List<String>,
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