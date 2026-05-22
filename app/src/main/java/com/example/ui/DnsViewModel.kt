package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.DnsServer
import com.example.data.model.QueryLog
import com.example.data.network.DnsResolverResult
import com.example.data.repository.DnsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DnsViewModel(private val repository: DnsRepository) : ViewModel() {

    // Exposure of Room tables
    val allServers: StateFlow<List<DnsServer>> = repository.allServers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentQueryLogs: StateFlow<List<QueryLog>> = repository.recentQueryLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Form states for adding custom server
    private val _newServerName = MutableStateFlow("")
    val newServerName = _newServerName.asStateFlow()

    private val _newServerType = MutableStateFlow("DOH") // "DOH" or "DOT"
    val newServerType = _newServerType.asStateFlow()

    private val _newServerEndpoint = MutableStateFlow("")
    val newServerEndpoint = _newServerEndpoint.asStateFlow()

    private val _newServerPort = MutableStateFlow("853")
    val newServerPort = _newServerPort.asStateFlow()

    private val _serverFormError = MutableStateFlow<String?>(null)
    val serverFormError = _serverFormError.asStateFlow()

    // Query terminal sandbox state
    private val _queryDomain = MutableStateFlow("google.com")
    val queryDomain = _queryDomain.asStateFlow()

    private val _queryType = MutableStateFlow("A") // "A", "AAAA", "CNAME", "MX", "TXT", "NS"
    val queryType = _queryType.asStateFlow()

    private val _isQuerying = MutableStateFlow(false)
    val isQuerying = _isQuerying.asStateFlow()

    private val _lastQueryResult = MutableStateFlow<DnsResolverResult?>(null)
    val lastQueryResult = _lastQueryResult.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedDefaults()
        }
    }

    // Setters
    fun setNewServerName(value: String) { _newServerName.value = value }
    fun setNewServerType(value: String) { _newServerType.value = value }
    fun setNewServerEndpoint(value: String) { _newServerEndpoint.value = value }
    fun setNewServerPort(value: String) { _newServerPort.value = value }
    fun setQueryDomain(value: String) { _queryDomain.value = value }
    fun setQueryType(value: String) { _queryType.value = value }

    // Actions
    fun selectActiveServer(id: Long) {
        viewModelScope.launch {
            repository.selectActiveServer(id)
        }
    }

    fun addCustomServer() {
        val name = _newServerName.value.trim()
        val type = _newServerType.value.trim().uppercase()
        val endpoint = _newServerEndpoint.value.trim()
        val portStr = _newServerPort.value.trim()

        if (name.isEmpty()) {
            _serverFormError.value = "Server Name is required."
            return
        }
        if (endpoint.isEmpty()) {
            _serverFormError.value = "Endpoint URL or Hostname is required."
            return
        }

        val port = if (type == "DOT") {
            val p = portStr.toIntOrNull()
            if (p == null || p < 1 || p > 65535) {
                _serverFormError.value = "Enter a valid port (1 - 65535)."
                return
            }
            p
        } else {
            null
        }

        _serverFormError.value = null

        viewModelScope.launch {
            val newServer = DnsServer(
                name = name,
                type = type,
                endpoint = endpoint,
                port = port,
                isCustom = true,
                isActive = false
            )
            repository.insertServer(newServer)
            
            // Clear form inputs
            _newServerName.value = ""
            _newServerEndpoint.value = ""
            _newServerPort.value = "853"
        }
    }

    fun deleteServer(server: DnsServer) {
        viewModelScope.launch {
            repository.deleteServer(server)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearQueryLogs()
        }
    }

    fun runDnsQuery() {
        val domain = _queryDomain.value.trim()
        val type = _queryType.value.trim()

        if (domain.isEmpty()) return

        _isQuerying.value = true
        _lastQueryResult.value = null

        viewModelScope.launch {
            val result = repository.executeQuery(domain, type)
            _lastQueryResult.value = result
            _isQuerying.value = false
        }
    }
}

class DnsViewModelFactory(private val repository: DnsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DnsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DnsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
