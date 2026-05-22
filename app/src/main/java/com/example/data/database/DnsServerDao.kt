package com.example.data.database

import androidx.room.*
import com.example.data.model.DnsServer
import kotlinx.coroutines.flow.Flow

@Dao
interface DnsServerDao {
    @Query("SELECT * FROM dns_servers ORDER BY id ASC")
    fun getAllServers(): Flow<List<DnsServer>>

    @Query("SELECT * FROM dns_servers WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveServer(): DnsServer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: DnsServer): Long

    @Delete
    suspend fun deleteServer(server: DnsServer)

    @Query("UPDATE dns_servers SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE dns_servers SET isActive = 1 WHERE id = :id")
    suspend fun activateServer(id: Long)

    @Transaction
    suspend fun selectActiveServer(id: Long) {
        deactivateAll()
        activateServer(id)
    }

    @Query("SELECT COUNT(*) FROM dns_servers")
    suspend fun getServerCount(): Int
}
