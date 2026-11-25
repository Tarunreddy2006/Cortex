package com.cortex.app.database.dao

import androidx.room.*
import com.cortex.app.database.entities.MemoryEntity

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY isPinned DESC, createdAt DESC")
    suspend fun getAllMemories(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY isPinned DESC, createdAt DESC")
    suspend fun getMemoriesByCategory(category: String): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE isPinned = 1 ORDER BY createdAt DESC")
    suspend fun getPinnedMemories(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    suspend fun getMemoryById(id: String): MemoryEntity?

    @Query("SELECT * FROM memories WHERE content LIKE '%' || :query || '%' OR tagsJson LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun searchMemories(query: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(memory: MemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(memories: List<MemoryEntity>)

    @Query("UPDATE memories SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: String, isPinned: Boolean)

    @Query("UPDATE memories SET approvalStatus = :status WHERE id = :id")
    suspend fun setApprovalStatus(id: String, status: String)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemory(id: String): Int

    @Query("DELETE FROM memories WHERE sourceConnector = :sourceConnector")
    suspend fun deleteMemoriesBySource(sourceConnector: String): Int
}
