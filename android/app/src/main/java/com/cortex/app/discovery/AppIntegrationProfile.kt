package com.cortex.app.discovery

data class AppIntegrationProfile(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val supportedCapabilities: List<String>,
    val iconBase64: String?,
    val versionName: String? = null
)
