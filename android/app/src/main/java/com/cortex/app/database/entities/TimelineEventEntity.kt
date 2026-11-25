package com.cortex.app.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timeline_events",
    indices = [
        Index(value = ["timestampMs"]),
        Index(value = ["eventType"]),
        Index(value = ["sourceConnector"]),
        Index(value = ["isSignificant"])
    ]
)
data class TimelineEventEntity(
    @PrimaryKey val id: String,
    val timestampMs: Long,
    val eventType: String, // MEMORY, TASK, DOCUMENT, EMAIL, CALENDAR, PROJECT, ACADEMIC
    val title: String,
    val summary: String? = null,
    val sourceConnector: String,
    val referenceId: String? = null,
    val isSignificant: Boolean = true
)
