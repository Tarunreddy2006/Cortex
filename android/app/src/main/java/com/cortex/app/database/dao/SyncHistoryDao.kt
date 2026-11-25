package com.cortex.app.database.dao

import androidx.room.*
import com.cortex.app.database.entities.SyncHistoryEntity

@Dao
interface SyncHistoryDao {
    @Query("SELECT * FROM sync_history ORDER BY startedAtMs DESC LIMIT :limit")
    suspend fun getRecentSyncs(limit: Int = 30): List<SyncHistoryEntity>

    @Query("SELECT * FROM sync_history WHERE connectorId = :connectorId ORDER BY startedAtMs DESC LIMIT 1")
    suspend fun getLastSyncForConnector(connectorId: String): SyncHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SyncHistoryEntity): Long

    @Query("DELETE FROM sync_history WHERE connectorId = :connectorId")
    suspend fun deleteHistoryForConnector(connectorId: String): Int

    @Query("DELETE FROM sync_history")
    suspend fun deleteAll(): Int
}
