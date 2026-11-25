package com.cortex.app.database.dao

import androidx.room.*
import com.cortex.app.database.entities.FinancialExclusionEntity

@Dao
interface FinancialExclusionDao {
    @Query("SELECT * FROM financial_exclusions ORDER BY appName ASC")
    suspend fun getAllExclusions(): List<FinancialExclusionEntity>

    @Query("SELECT packageName FROM financial_exclusions")
    suspend fun getAllExcludedPackages(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM financial_exclusions WHERE packageName = :packageName)")
    suspend fun isExcluded(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FinancialExclusionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<FinancialExclusionEntity>)

    @Query("DELETE FROM financial_exclusions WHERE packageName = :packageName AND isPermanent = 0")
    suspend fun removeUserExclusion(packageName: String): Int
}
