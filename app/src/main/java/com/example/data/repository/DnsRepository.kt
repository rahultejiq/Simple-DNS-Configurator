package com.example.data.repository

import com.example.data.database.DnsServerDao
import com.example.data.database.QueryLogDao
import com.example.data.model.DnsServer
import com.example.data.model.QueryLog
import com.example.data.network.DnsResolverClient
import com.example.data.network.DnsResolverResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class DnsRepository(
    private val serverDao: DnsServerDao,
    private val logDao: QueryLogDao,
    private val client: DnsResolverClient = DnsResolverClient()
) {

    val allServers: Flow<List<DnsServer>> = serverDao.getAllServers()
    val recentQueryLogs: Flow<List<QueryLog>> = logDao.getRecentLogs()

    suspend fun getActiveServer(): DnsServer? {
        return serverDao.getActiveServer()
    }

    suspend fun insertServer(server: DnsServer): Long {
        return serverDao.insertServer(server)
    }

    suspend fun deleteServer(server: DnsServer) {
        serverDao.deleteServer(server)
    }

    suspend fun selectActiveServer(id: Long) {
        serverDao.selectActiveServer(id)
    }

    suspend fun clearQueryLogs() {
        logDao.clearLogs()
    }

    suspend fun executeQuery(domain: String, type: String): DnsResolverResult {
        val activeServer = getActiveServer() ?: getFirstAvailableServer() ?: return DnsResolverResult.Failure(
            "No DNS Server configured or selected", 0L
        )

        val result = client.resolve(activeServer, domain, type)

        val log = when (result) {
            is DnsResolverResult.Success -> {
                val answersText = if (result.response.answers.isEmpty()) {
                    "No records resolved"
                } else {
                    result.response.answers.joinToString("\n") { record ->
                        "[${record.typeName}] ${record.name} (TTL: ${record.ttl}s) -> ${record.data}"
                    }
                }
                QueryLog(
                    domainName = domain,
                    queryType = type,
                    serverName = activeServer.name,
                    serverType = activeServer.type,
                    serverEndpoint = if (activeServer.type == "DOT") {
                        "${activeServer.endpoint}:${activeServer.port ?: 853}"
                    } else {
                        activeServer.endpoint
                    },
                    latencyMs = result.latencyMs,
                    responseCode = result.response.rCodeName,
                    answerSection = answersText
                )
            }
            is DnsResolverResult.Failure -> {
                QueryLog(
                    domainName = domain,
                    queryType = type,
                    serverName = activeServer.name,
                    serverType = activeServer.type,
                    serverEndpoint = if (activeServer.type == "DOT") {
                        "${activeServer.endpoint}:${activeServer.port ?: 853}"
                    } else {
                        activeServer.endpoint
                    },
                    latencyMs = result.latencyMs,
                    responseCode = "ERROR",
                    answerSection = result.errorMessage
                )
            }
        }
        logDao.insertLog(log)
        return result
    }

    private suspend fun getFirstAvailableServer(): DnsServer? {
        return allServers.first().firstOrNull()
    }

    suspend fun checkAndSeedDefaults() {
        if (serverDao.getServerCount() == 0) {
            val defaults = listOf(
                DnsServer(name = "Cloudflare DNS (DoH)", type = "DOH", endpoint = "https://cloudflare-dns.com/dns-query", isActive = true),
                DnsServer(name = "Google Public DNS (DoH)", type = "DOH", endpoint = "https://dns.google/dns-query"),
                DnsServer(name = "Quad9 Secure DNS (DoH)", type = "DOH", endpoint = "https://dns.quad9.net/dns-query"),
                DnsServer(name = "AdGuard AdBlock (DoH)", type = "DOH", endpoint = "https://dns.adguard-dns.com/dns-query"),
                DnsServer(name = "Cloudflare DNS (DoT)", type = "DOT", endpoint = "one.one.one.one", port = 853),
                DnsServer(name = "Google Public DNS (DoT)", type = "DOT", endpoint = "dns.google", port = 853),
                DnsServer(name = "Quad9 Secure DNS (DoT)", type = "DOT", endpoint = "dns.quad9.net", port = 853),
                DnsServer(name = "AdGuard AdBlock (DoT)", type = "DOT", endpoint = "dns.adguard-dns.com", port = 853)
            )
            for (server in defaults) {
                serverDao.insertServer(server)
            }
        }
    }
}
