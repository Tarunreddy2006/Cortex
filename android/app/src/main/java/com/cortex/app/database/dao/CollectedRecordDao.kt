package com.cortex.app.database.dao

import androidx.room.*
import com.cortex.app.database.entities.CollectedRecordEntity

@Dao
interface CollectedRecordDao {
    @Query("SELECT * FROM collected_records ORDER BY timestampMs DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecentRecords(limit: Int = 50, offset: Int = 0): List<CollectedRecordEntity>

    @Query("SELECT * FROM collected_records WHERE sourceConnector = :connectorId ORDER BY timestampMs DESC")
    suspend fun getRecordsByConnector(connectorId: String): List<CollectedRecordEntity>

    @Query("SELECT COUNT(*) FROM collected_records WHERE sourceConnector = :connectorId")
    suspend fun getRecordCountForConnector(connectorId: String): Int

    @Query("SELECT COUNT(*) FROM collected_records")
    suspend fun getTotalRecordCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM collected_records WHERE dedupHash = :hash)")
    suspend fun hasRecordWithHash(hash: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CollectedRecordEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<CollectedRecordEntity>)

    @Query("DELETE FROM collected_records WHERE sourceConnector = :connectorId")
    suspend fun deleteRecordsByConnector(connectorId: String): Int

    @Query("DELETE FROM collected_records WHERE packageName = :packageName")
    suspend fun deleteRecordsByPackage(packageName: String): Int

    @Query("DELETE FROM collected_records")
    suspend fun deleteAll(): Int
}
