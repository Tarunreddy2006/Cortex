package com.cortex.app.database.dao

import androidx.room.*
import com.cortex.app.database.entities.AppSelectionEntity

@Dao
interface AppSelectionDao {
    @Query("SELECT * FROM app_selections ORDER BY appName ASC")
    suspend fun getAllSelections(): List<AppSelectionEntity>

    @Query("SELECT * FROM app_selections WHERE isEnabled = 1")
    suspend fun getEnabledSelections(): List<AppSelectionEntity>

    @Query("SELECT * FROM app_selections WHERE packageName = :packageName LIMIT 1")
    suspend fun getSelection(packageName: String): AppSelectionEntity?

    @Query("SELECT isEnabled FROM app_selections WHERE packageName = :packageName LIMIT 1")
    suspend fun isAppEnabled(packageName: String): Boolean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: AppSelectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(entities: List<AppSelectionEntity>)

    @Query("UPDATE app_selections SET isEnabled = :isEnabled, updatedAt = :timestamp WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, isEnabled: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM app_selections WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
