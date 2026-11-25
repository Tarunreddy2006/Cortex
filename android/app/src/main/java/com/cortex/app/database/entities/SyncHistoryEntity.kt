package com.cortex.app.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_history")
data class SyncHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val connectorId: String,
    val status: String,
    val startedAtMs: Long,
    val completedAtMs: Long,
    val recordsProcessed: Int,
    val recordsAdded: Int,
    val errorMessage: String? = null
)
