package com.cortex.app.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_selections")
data class AppSelectionEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: String,
    val isEnabled: Boolean,
    val isSystemApp: Boolean,
    val supportedCapabilitiesJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)
