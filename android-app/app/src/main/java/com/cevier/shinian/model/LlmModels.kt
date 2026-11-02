package com.cevier.shinian.model

enum class LlmProvider(
    val label: String,
    val defaultBaseUrl: String,
) {
    DEEPSEEK("DeepSeek", "https://api.deepseek.com"),
    VERCEL("Vercel AI Gateway", "https://ai-gateway.vercel.sh/v1"),
}

data class LlmConfig(
    val provider: LlmProvider = LlmProvider.DEEPSEEK,
    val apiKey: String = "",
    val baseUrl: String = LlmProvider.DEEPSEEK.defaultBaseUrl,
    val modelId: String = "",
    val modelName: String = "",
    val supportsImages: Boolean = false,
    val models: List<LlmModel> = emptyList(),
    val thinkingEnabled: Boolean = true,
) {
    val isReady: Boolean
        get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && modelId.isNotBlank()

    val configuredModels: List<LlmModel>
        get() = models.ifEmpty {
            if (modelId.isBlank()) emptyList()
            else listOf(LlmModel(modelId, modelName.ifBlank { modelId }, supportsImages))
        }
}

data class LlmModel(
    val id: String,
    val name: String = id,
    val supportsImages: Boolean = false,
)

data class ChatMessage(
    val id: Long,
    val role: String,
    val text: String,
    val imageDataUrl: String? = null,
)

data class RecentConversation(
    val messages: List<ChatMessage> = emptyList(),
    val updatedAt: Long = 0L,
)
