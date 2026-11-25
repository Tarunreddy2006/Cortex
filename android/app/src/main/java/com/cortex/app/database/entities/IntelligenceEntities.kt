package com.cortex.app.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["updatedAt"])
    ]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "conversation_messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["createdAt"])
    ]
)
data class ConversationMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val sender: String, // "USER" or "CORTEX"
    val text: String,
    val sourcesJson: String = "[]",
    val intent: String = "GENERAL",
    val referencedProject: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "proposed_actions",
    indices = [
        Index(value = ["status"]),
        Index(value = ["actionType"]),
        Index(value = ["createdAt"])
    ]
)
data class ProposedActionEntity(
    @PrimaryKey val id: String,
    val actionType: String, // CREATE_TASK, SCHEDULE_STUDY_BLOCK, CREATE_CALENDAR_EVENT, CREATE_REMINDER, RESCHEDULE_TASK
    val title: String,
    val description: String,
    val payloadJson: String,
    val status: String = "PROPOSED", // PROPOSED, APPROVED, REJECTED, CANCELLED, EXECUTED
    val sourceContext: String = "Assistant",
    val createdAt: Long = System.currentTimeMillis(),
    val executedAt: Long? = null
)

@Entity(
    tableName = "decision_records",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class DecisionRecordEntity(
    @PrimaryKey val id: String,
    val statement: String,
    val alternativesJson: String = "[]",
    val advantagesJson: String = "[]",
    val disadvantagesJson: String = "[]",
    val risksJson: String = "[]",
    val unknownInfoJson: String = "[]",
    val assumptionsJson: String = "[]",
    val recommendation: String,
    val conditionsJson: String = "[]",
    val status: String = "EVALUATED",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "proactive_alerts",
    indices = [
        Index(value = ["alertKey"]),
        Index(value = ["isDismissed"]),
        Index(value = ["priority"]),
        Index(value = ["createdAt"])
    ]
)
data class ProactiveAlertEntity(
    @PrimaryKey val id: String,
    val alertKey: String,
    val alertType: String, // OVERDUE_TASK, UPCOMING_EXAM, ASSIGNMENT_DUE, MILESTONE_APPROACHING
    val title: String,
    val description: String,
    val priority: String = "MEDIUM", // URGENT, HIGH, MEDIUM, LOW
    val referenceId: String? = null,
    val actionSuggestion: String? = null,
    val isDismissed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
