package com.martenworld.app.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martenworld.app.data.connection.Connection
import com.martenworld.app.data.connection.ConnectionRepository
import com.martenworld.app.llm.ApiException
import com.martenworld.app.llm.ChatMessage
import com.martenworld.app.llm.OpenAiCompatibleClient
import com.martenworld.app.llm.StreamEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A line in the scratch scene. Not persisted yet; M1 introduces real sessions. */
data class PlayLine(
    val id: Long,
    val role: String, // "player" | "narrator"
    val text: String,
    val streaming: Boolean = false,
    val error: String? = null,
)

data class PlayState(
    val connection: Connection? = null,
    val lines: List<PlayLine> = emptyList(),
    val input: String = "",
    val generating: Boolean = false,
) {
    val canSend: Boolean get() = connection != null && !generating && input.isNotBlank()
}

/**
 * M0 smoke test for the pipeline: a bare chat with the active connection and a
 * fixed system prompt. It proves streaming works end to end before any world or
 * prompt system exists.
 */
@HiltViewModel
class PlayViewModel @Inject constructor(
    private val repository: ConnectionRepository,
    private val client: OpenAiCompatibleClient,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayState())
    val state: StateFlow<PlayState> = _state.asStateFlow()

    private var job: Job? = null
    private var nextId = 1L

    init {
        viewModelScope.launch {
            repository.load()
            repository.activeId.collect { id ->
                _state.update { it.copy(connection = id?.let(repository::get)) }
            }
        }
    }

    fun onInput(v: String) = _state.update { it.copy(input = v) }

    fun send() {
        val s = _state.value
        val connection = s.connection ?: return
        if (!s.canSend) return

        val userLine = PlayLine(nextId++, "player", s.input.trim())
        val replyId = nextId++
        _state.update {
            it.copy(
                lines = it.lines + userLine + PlayLine(replyId, "narrator", "", streaming = true),
                input = "",
                generating = true,
            )
        }

        val history = _state.value.lines
            .filter { it.id != replyId && it.error == null }
            .map { ChatMessage(if (it.role == "player") "user" else "assistant", it.text) }

        job = viewModelScope.launch {
            val messages = listOf(ChatMessage("system", SYSTEM_PROMPT)) + history
            runCatching {
                client.streamChat(connection, repository.apiKey(connection.id), messages).collect { event ->
                    when (event) {
                        is StreamEvent.Delta -> appendTo(replyId, event.text)
                        is StreamEvent.Finished -> Unit
                    }
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) return@onFailure
                val message = when (e) {
                    is ApiException -> "HTTP ${e.status}: ${e.detail}"
                    else -> e.message ?: e::class.simpleName ?: "Request failed"
                }
                _state.update { st ->
                    st.copy(lines = st.lines.map { if (it.id == replyId) it.copy(error = message) else it })
                }
            }
            finish(replyId)
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.value.lines.lastOrNull { it.streaming }?.let { finish(it.id) }
    }

    fun clear() {
        stop()
        _state.update { it.copy(lines = emptyList()) }
    }

    private fun appendTo(id: Long, text: String) = _state.update { st ->
        st.copy(lines = st.lines.map { if (it.id == id) it.copy(text = it.text + text) else it })
    }

    private fun finish(id: Long) = _state.update { st ->
        st.copy(
            generating = false,
            lines = st.lines.map { if (it.id == id) it.copy(streaming = false) else it },
        )
    }

    private companion object {
        const val SYSTEM_PROMPT =
            "You are the narrator of an interactive story. Describe what happens in response to the " +
                "player's actions in a few vivid sentences, then stop and wait for their next move. " +
                "Never speak or decide for the player."
    }
}
