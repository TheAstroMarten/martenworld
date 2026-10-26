package com.martenworld.app.llm

import com.martenworld.app.data.connection.Connection
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

class ApiException(val status: Int, val detail: String) : Exception("HTTP $status: $detail")

/**
 * Talks to any `/v1/chat/completions`-shaped endpoint. The caller supplies the
 * connection and key so the client itself holds no state.
 */
@Singleton
class OpenAiCompatibleClient @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    private val http = HttpClient(OkHttp) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            connectTimeoutMillis = 30_000
            requestTimeoutMillis = 10 * 60_000
            socketTimeoutMillis = 10 * 60_000
        }
    }

    suspend fun listModels(connection: Connection, apiKey: String?): List<ModelInfo> {
        val response = http.get("${connection.normalizedBaseUrl}/models") {
            applyAuth(connection, apiKey)
        }
        if (!response.status.isSuccess()) throw response.toApiException()
        return response.body<ModelListResponse>().data.sortedBy { it.id }
    }

    fun streamChat(
        connection: Connection,
        apiKey: String?,
        messages: List<ChatMessage>,
    ): Flow<StreamEvent> = flow {
        val request = ChatCompletionRequest(
            model = connection.model,
            messages = messages,
            stream = true,
            temperature = connection.temperature,
            maxTokens = connection.maxTokens,
        )
        http.preparePost("${connection.normalizedBaseUrl}/chat/completions") {
            applyAuth(connection, apiKey)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Accept, "text/event-stream")
            setBody(request)
        }.execute { response ->
            if (!response.status.isSuccess()) throw response.toApiException()
            val channel: ByteReadChannel = response.bodyAsChannel()
            val parser = SseParser()
            var finished = false
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                val data = parser.feed(line) ?: continue
                if (data == SseParser.DONE) break
                val chunk = runCatching { json.decodeFromString<ChatCompletionChunk>(data) }.getOrNull() ?: continue
                for (choice in chunk.choices) {
                    choice.delta.content?.takeIf { it.isNotEmpty() }?.let { emit(StreamEvent.Delta(it)) }
                    choice.finishReason?.let {
                        emit(StreamEvent.Finished(it))
                        finished = true
                    }
                }
            }
            if (!finished) emit(StreamEvent.Finished(null))
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyAuth(connection: Connection, apiKey: String?) {
        if (!apiKey.isNullOrBlank()) header(HttpHeaders.Authorization, "Bearer $apiKey")
        connection.customHeaders.forEach { (k, v) -> header(k, v) }
    }

    private suspend fun HttpResponse.toApiException(): ApiException {
        val text = runCatching { bodyAsText() }.getOrDefault("")
        val detail = runCatching {
            val root = json.parseToJsonElement(text).jsonObject
            root["error"]?.let { err ->
                runCatching { err.jsonObject["message"]?.jsonPrimitive?.content }.getOrNull()
                    ?: err.jsonPrimitive.content
            } ?: root["message"]?.jsonPrimitive?.content
        }.getOrNull() ?: text.take(500).ifBlank { status.description }
        return ApiException(status.value, detail)
    }
}
