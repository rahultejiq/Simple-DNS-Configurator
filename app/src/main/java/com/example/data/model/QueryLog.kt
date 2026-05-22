package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "query_logs")
data class QueryLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val domainName: String,
    val queryType: String, // "A", "AAAA", "MX", "TXT", "NS"
    val serverName: String,
    val serverType: String, // "DOH" or "DOT"
    val serverEndpoint: String,
    val latencyMs: Long,
    val responseCode: String, // NOERROR, NXDOMAIN, etc., or ERROR: <message>
    val answerSection: String, // Resolved IP addresses or domains joined by newline
    val timestamp: Long = System.currentTimeMillis()
)
