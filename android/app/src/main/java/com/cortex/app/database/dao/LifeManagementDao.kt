package com.cortex.app.database.dao

import androidx.room.*
import com.cortex.app.database.entities.GoalEntity
import com.cortex.app.database.entities.TaskEntity

@Dao
interface LifeManagementDao {
    // Tasks
    @Query("SELECT * FROM tasks ORDER BY CASE priority WHEN 'URGENT' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END, dueDateMs ASC")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status != 'COMPLETED' AND status != 'CANCELLED' ORDER BY CASE priority WHEN 'URGENT' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END, dueDateMs ASC")
    suspend fun getPendingTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY dueDateMs ASC")
    suspend fun getTasksByProject(projectId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, completedAtMs = :completedAtMs WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: String, completedAtMs: Long? = if (status == "COMPLETED") System.currentTimeMillis() else null)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: String): Int

    // Goals
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    suspend fun getAllGoals(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGoal(goal: GoalEntity)

    @Query("UPDATE goals SET progressPercent = :percent, status = CASE WHEN :percent >= 100 THEN 'ACHIEVED' ELSE 'ACTIVE' END WHERE id = :id")
    suspend fun updateGoalProgress(id: String, percent: Int)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: String): Int
}
