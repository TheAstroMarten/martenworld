package com.martenworld.app.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
)

@Serializable
data class ChatCompletionChunk(
    val id: String? = null,
    val choices: List<Choice> = emptyList(),
) {
    @Serializable
    data class Choice(
        val index: Int = 0,
        val delta: Delta = Delta(),
        @SerialName("finish_reason") val finishReason: String? = null,
    )

    @Serializable
    data class Delta(
        val role: String? = null,
        val content: String? = null,
    )
}

@Serializable
data class ModelListResponse(val data: List<ModelInfo> = emptyList())

@Serializable
data class ModelInfo(val id: String, @SerialName("owned_by") val ownedBy: String? = null)

/** Events emitted while streaming a completion. */
sealed interface StreamEvent {
    data class Delta(val text: String) : StreamEvent
    data class Finished(val reason: String?) : StreamEvent
}
