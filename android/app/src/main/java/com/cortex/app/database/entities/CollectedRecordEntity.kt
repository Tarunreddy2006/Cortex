package com.cortex.app.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "collected_records",
    indices = [
        Index(value = ["sourceConnector"]),
        Index(value = ["packageName"]),
        Index(value = ["timestampMs"]),
        Index(value = ["dedupHash"], unique = false)
    ]
)
data class CollectedRecordEntity(
    @PrimaryKey val id: String,
    val sourceConnector: String,
    val packageName: String?,
    val recordType: String,
    val title: String?,
    val summary: String?,
    val payloadJson: String,
    val dedupHash: String?,
    val timestampMs: Long,
    val createdAt: Long = System.currentTimeMillis()
)
