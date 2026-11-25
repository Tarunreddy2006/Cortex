package com.cortex.app.database.dao

import androidx.room.*
import com.cortex.app.database.entities.ProjectDecisionEntity
import com.cortex.app.database.entities.ProjectEntity
import com.cortex.app.database.entities.ProjectMilestoneEntity

@Dao
interface ProjectDao {
    // Projects
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    suspend fun getAllProjects(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String): Int

    // Milestones
    @Query("SELECT * FROM project_milestones WHERE projectId = :projectId ORDER BY targetDateMs ASC")
    suspend fun getMilestonesForProject(projectId: String): List<ProjectMilestoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMilestone(milestone: ProjectMilestoneEntity)

    @Query("UPDATE project_milestones SET isCompleted = :completed, completedAtMs = :completedAtMs WHERE id = :id")
    suspend fun setMilestoneCompleted(id: String, completed: Boolean, completedAtMs: Long? = if (completed) System.currentTimeMillis() else null)

    @Query("DELETE FROM project_milestones WHERE id = :id")
    suspend fun deleteMilestone(id: String): Int

    // Decisions
    @Query("SELECT * FROM project_decisions WHERE projectId = :projectId ORDER BY decidedAt DESC")
    suspend fun getDecisionsForProject(projectId: String): List<ProjectDecisionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDecision(decision: ProjectDecisionEntity)

    @Query("DELETE FROM project_decisions WHERE id = :id")
    suspend fun deleteDecision(id: String): Int
}
