package com.martenworld.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martenworld.app.data.connection.Connection
import com.martenworld.app.data.connection.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    private val repository: ConnectionRepository,
) : ViewModel() {

    val connections: StateFlow<List<Connection>> = repository.connections
    val activeId: StateFlow<String?> = repository.activeId

    init {
        viewModelScope.launch { repository.load() }
    }

    fun setActive(id: String) = repository.setActive(id)

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}
