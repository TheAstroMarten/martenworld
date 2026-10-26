package com.martenworld.app.data.connection

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A saved OpenAI-compatible endpoint. The API key is never part of this file;
 * it lives in [ApiKeyStore], keyed by [id].
 */
@Serializable
data class Connection(
    val schemaVersion: Int = 1,
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    /** Base URL up to and including the API version, e.g. `https://api.openai.com/v1`. */
    val baseUrl: String = "",
    val model: String = "",
    val contextSize: Int = 8192,
    val temperature: Double = 0.8,
    val maxTokens: Int = 512,
    val customHeaders: Map<String, String> = emptyMap(),
    val createdAt: String = "",
    val updatedAt: String = "",
) {
    val normalizedBaseUrl: String get() = baseUrl.trim().trimEnd('/')
}
