package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.QueryLog
import kotlinx.coroutines.flow.Flow

@Dao
interface QueryLogDao {
    @Query("SELECT * FROM query_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<QueryLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: QueryLog): Long

    @Query("DELETE FROM query_logs")
    suspend fun clearLogs()
}
