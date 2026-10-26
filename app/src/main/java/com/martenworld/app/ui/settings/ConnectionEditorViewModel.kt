package com.martenworld.app.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martenworld.app.data.connection.Connection
import com.martenworld.app.data.connection.ConnectionRepository
import com.martenworld.app.llm.ApiException
import com.martenworld.app.llm.OpenAiCompatibleClient
import com.martenworld.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionEditorState(
    val id: String? = null,
    val name: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val contextSize: String = "8192",
    val temperature: String = "0.8",
    val maxTokens: String = "512",
    val models: List<String> = emptyList(),
    val testing: Boolean = false,
    val testResult: String? = null,
    val testFailed: Boolean = false,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = baseUrl.isNotBlank() && model.isNotBlank()
}

@HiltViewModel
class ConnectionEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ConnectionRepository,
    private val client: OpenAiCompatibleClient,
) : ViewModel() {

    private val existingId: String? = savedStateHandle.get<String>("id")?.takeUnless { it == Routes.CONNECTION_NEW_ID }
    private var existing: Connection? = null

    private val _state = MutableStateFlow(ConnectionEditorState(id = existingId, baseUrl = "https://api.openai.com/v1"))
    val state: StateFlow<ConnectionEditorState> = _state.asStateFlow()

    init {
        if (existingId != null) {
            viewModelScope.launch {
                repository.load()
                repository.get(existingId)?.let { c ->
                    existing = c
                    _state.update {
                        it.copy(
                            name = c.name,
                            baseUrl = c.baseUrl,
                            apiKey = repository.apiKey(c.id).orEmpty(),
                            model = c.model,
                            contextSize = c.contextSize.toString(),
                            temperature = c.temperature.toString(),
                            maxTokens = c.maxTokens.toString(),
                        )
                    }
                }
            }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v) }
    fun onBaseUrl(v: String) = _state.update { it.copy(baseUrl = v, models = emptyList(), testResult = null) }
    fun onApiKey(v: String) = _state.update { it.copy(apiKey = v, testResult = null) }
    fun onModel(v: String) = _state.update { it.copy(model = v) }
    fun onContextSize(v: String) = _state.update { it.copy(contextSize = v.filter(Char::isDigit)) }
    fun onTemperature(v: String) = _state.update { it.copy(temperature = v) }
    fun onMaxTokens(v: String) = _state.update { it.copy(maxTokens = v.filter(Char::isDigit)) }

    /** Hits `/models` with the current form values. Doubles as "test connection". */
    fun fetchModels() {
        val s = _state.value
        _state.update { it.copy(testing = true, testResult = null, testFailed = false) }
        viewModelScope.launch {
            val result = runCatching { client.listModels(draft(s), s.apiKey.ifBlank { null }) }
            result.onSuccess { models ->
                val ids = models.map { it.id }
                _state.update {
                    it.copy(
                        testing = false,
                        models = ids,
                        model = it.model.ifBlank { ids.firstOrNull().orEmpty() },
                        testResult = if (ids.isEmpty()) "Connected, but the endpoint returned no models." else "Connected. ${ids.size} models available.",
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        testing = false,
                        testFailed = true,
                        testResult = when (e) {
                            is ApiException -> "HTTP ${e.status}: ${e.detail}"
                            else -> e.message ?: e::class.simpleName ?: "Request failed"
                        },
                    )
                }
            }
        }
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            repository.save(draft(s), s.apiKey.ifBlank { null })
            _state.update { it.copy(saved = true) }
        }
    }

    private fun draft(s: ConnectionEditorState): Connection {
        val base = existing ?: Connection()
        return base.copy(
            name = s.name.trim(),
            baseUrl = s.baseUrl.trim(),
            model = s.model.trim(),
            contextSize = s.contextSize.toIntOrNull() ?: base.contextSize,
            temperature = s.temperature.toDoubleOrNull() ?: base.temperature,
            maxTokens = s.maxTokens.toIntOrNull() ?: base.maxTokens,
        )
    }
}
