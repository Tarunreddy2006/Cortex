package com.cortex.app.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["priority"]),
        Index(value = ["status"]),
        Index(value = ["category"]),
        Index(value = ["dueDateMs"]),
        Index(value = ["projectId"])
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH, URGENT
    val status: String = "PENDING",   // PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    val category: String = "PERSONAL", // ACADEMIC, PROJECT, PERSONAL, RESPONSIBILITY
    val dueDateMs: Long? = null,
    val isRecurring: Boolean = false,
    val recurrenceRule: String? = null,
    val subtasksJson: String = "[]",
    val projectId: String? = null,
    val completedAtMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "goals",
    indices = [
        Index(value = ["status"]),
        Index(value = ["goalType"])
    ]
)
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    val goalType: String = "PERSONAL", // SHORT_TERM, LONG_TERM, LEARNING, PERSONAL, PROJECT
    val targetDateMs: Long? = null,
    val progressPercent: Int = 0,
    val status: String = "ACTIVE", // ACTIVE, ACHIEVED, PAUSED
    val createdAt: Long = System.currentTimeMillis()
)
