package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dns_servers")
data class DnsServer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String, // "DOH" or "DOT"
    val endpoint: String, // URL for DoH, Hostname/IP for DoT
    val port: Int? = null, // Usually 853 for DoT
    val isCustom: Boolean = false,
    val isActive: Boolean = false
)
