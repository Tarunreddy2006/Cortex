package com.cortex.app.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val objective: String? = null,
    val status: String = "ACTIVE", // PLANNING, ACTIVE, ON_HOLD, COMPLETED
    val techStackJson: String = "[]",
    val progressPercent: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "project_milestones",
    indices = [
        Index(value = ["projectId"])
    ]
)
data class ProjectMilestoneEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val targetDateMs: Long? = null,
    val isCompleted: Boolean = false,
    val completedAtMs: Long? = null
)

@Entity(
    tableName = "project_decisions",
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["decidedAt"])
    ]
)
data class ProjectDecisionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val rationale: String,
    val status: String = "ACCEPTED", // PROPOSED, ACCEPTED, SUPERSEDED
    val decidedAt: Long = System.currentTimeMillis()
)
