package com.example.data.network

import com.example.data.model.DnsServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocket

class DnsResolverClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun resolve(
        server: DnsServer,
        domain: String,
        type: String
    ): DnsResolverResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val response = when (server.type.uppercase()) {
                "DOH" -> resolveDoh(server.endpoint, domain, type)
                "DOT" -> resolveDot(server.endpoint, server.port ?: 853, domain, type)
                else -> throw IllegalArgumentException("Unknown DNS Server Type: ${server.type}")
            }
            val latency = System.currentTimeMillis() - startTime
            DnsResolverResult.Success(response, latency)
        } catch (e: Throwable) {
            val latency = System.currentTimeMillis() - startTime
            DnsResolverResult.Failure(e.localizedMessage ?: "Unknown network error", latency)
        }
    }

    private fun resolveDoh(endpoint: String, domain: String, type: String): DnsPacketCoder.DnsResponse {
        val queryBytes = DnsPacketCoder.buildQuery(domain, type)
        val requestBody = queryBytes.toRequestBody("application/dns-message".toMediaType())

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .header("Accept", "application/dns-message")
            .header("Content-Type", "application/dns-message")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP Error: ${response.code} ${response.message}")
            }
            val bytes = response.body?.bytes() ?: throw Exception("Empty DNS Response Body")
            return DnsPacketCoder.parseResponse(bytes)
        }
    }

    private fun resolveDot(host: String, port: Int, domain: String, type: String): DnsPacketCoder.DnsResponse {
        val queryBytes = DnsPacketCoder.buildQuery(domain, type)
        val sslSocketFactory = javax.net.ssl.SSLSocketFactory.getDefault() as javax.net.ssl.SSLSocketFactory

        // Connect a raw socket first then handshake
        val socket = sslSocketFactory.createSocket()
        socket.connect(InetSocketAddress(host, port), 5000)
        socket.soTimeout = 5000

        val sslSocket = socket as SSLSocket
        sslSocket.startHandshake()

        sslSocket.use { conn ->
            val dos = DataOutputStream(conn.outputStream)
            val dis = DataInputStream(conn.inputStream)

            // DNS over TCP/TLS requires prepending a 2-byte length fields to every query
            dos.writeShort(queryBytes.size)
            dos.write(queryBytes)
            dos.flush()

            val respLength = dis.readUnsignedShort()
            val respBytes = ByteArray(respLength)
            dis.readFully(respBytes)

            return DnsPacketCoder.parseResponse(respBytes)
        }
    }
}

sealed class DnsResolverResult {
    data class Success(val response: DnsPacketCoder.DnsResponse, val latencyMs: Long) : DnsResolverResult()
    data class Failure(val errorMessage: String, val latencyMs: Long) : DnsResolverResult()
}
