package com.martenworld.app.data.connection

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Connections are stored one JSON file each under `files/connections/`, matching the
 * "your data is files" principle. The in-memory list is the read model.
 */
@Singleton
class ConnectionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyStore: ApiKeyStore,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val dir: File get() = File(context.filesDir, "connections").apply { mkdirs() }
    private val activePrefs get() = context.getSharedPreferences("connections", Context.MODE_PRIVATE)

    private val _connections = MutableStateFlow<List<Connection>>(emptyList())
    val connections: StateFlow<List<Connection>> = _connections.asStateFlow()

    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        val loaded = dir.listFiles { f -> f.extension == "json" }
            .orEmpty()
            .mapNotNull { file ->
                runCatching { json.decodeFromString<Connection>(file.readText()) }.getOrNull()
            }
            .sortedBy { it.name.lowercase() }
        _connections.value = loaded
        _activeId.value = activePrefs.getString(KEY_ACTIVE, null)?.takeIf { id -> loaded.any { it.id == id } }
            ?: loaded.firstOrNull()?.id
    }

    fun get(id: String): Connection? = _connections.value.firstOrNull { it.id == id }

    fun active(): Connection? = _activeId.value?.let(::get)

    suspend fun save(connection: Connection, apiKey: String?) = withContext(Dispatchers.IO) {
        val now = Instant.now().toString()
        val stamped = connection.copy(
            createdAt = connection.createdAt.ifBlank { now },
            updatedAt = now,
        )
        File(dir, "${stamped.id}.json").writeText(json.encodeToString(Connection.serializer(), stamped))
        apiKeyStore.set(stamped.id, apiKey)
        load()
        if (_activeId.value == null) setActive(stamped.id)
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        File(dir, "$id.json").delete()
        apiKeyStore.remove(id)
        load()
    }

    fun setActive(id: String) {
        activePrefs.edit().putString(KEY_ACTIVE, id).apply()
        _activeId.value = id
    }

    fun apiKey(id: String): String? = apiKeyStore.get(id)

    private companion object {
        const val KEY_ACTIVE = "active_id"
    }
}
