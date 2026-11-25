package com.cortex.app.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_exclusions")
data class FinancialExclusionEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isPermanent: Boolean,
    val addedByUser: Boolean,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
)
