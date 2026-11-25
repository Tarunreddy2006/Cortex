package com.cortex.app.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memories",
    indices = [
        Index(value = ["category"]),
        Index(value = ["sourceConnector"]),
        Index(value = ["isPinned"]),
        Index(value = ["approvalStatus"]),
        Index(value = ["createdAt"])
    ]
)
data class MemoryEntity(
    @PrimaryKey val id: String,
    val content: String,
    val category: String, // PERSONAL, EPISODIC, ACADEMIC, PROJECT, KNOWLEDGE
    val sourceConnector: String,
    val sourceRecordId: String? = null,
    val confidenceStatus: String = "USER_EXPLICIT", // USER_EXPLICIT, CONFIRMED, EXTRACTED, AI_INFERRED
    val approvalStatus: String = "APPROVED", // APPROVED, PENDING_REVIEW, DISMISSED
    val isPinned: Boolean = false,
    val tagsJson: String = "[]",
    val relatedMemoryIdsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val reviewDate: Long? = null
)
