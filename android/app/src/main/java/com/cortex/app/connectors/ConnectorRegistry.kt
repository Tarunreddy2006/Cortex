package com.cortex.app.connectors

import android.content.Context
import com.cortex.app.connectors.calendar.CalendarConnector
import com.cortex.app.connectors.contacts.ContactsConnector
import com.cortex.app.database.CortexDatabase
import com.cortex.app.database.entities.SyncHistoryEntity
import com.cortex.app.permissions.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ConnectorItem(
    val id: String,
    val displayName: String,
    val category: String,
    val targetPackage: String?,
    val capabilities: List<String>,
    val isEnabled: Boolean,
    val isAuthorized: Boolean,
    val lastSyncTimestamp: Long?,
    val lastSyncStatus: String,
    val recordsCount: Int
)

class ConnectorRegistry(private val context: Context) {

    private val db = CortexDatabase.getInstance(context)

    suspend fun getConnectorsList(): List<ConnectorItem> = withContext(Dispatchers.IO) {
        val permState = PermissionManager.checkAllPermissions(context)
        val recordDao = db.collectedRecordDao()
        val syncDao = db.syncHistoryDao()

        val list = mutableListOf<ConnectorItem>()

        // 1. App Inventory
        val invCount = db.appSelectionDao().getAllSelections().size
        list.add(
            ConnectorItem(
                id = "app_inventory",
                displayName = "Application Inventory",
                category = "SYSTEM",
                targetPackage = null,
                capabilities = listOf("SYSTEM_PROVIDER"),
                isEnabled = true,
                isAuthorized = true,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastSyncStatus = "SUCCESS",
                recordsCount = invCount
            )
        )

        // 2. Notifications
        val notifCount = recordDao.getRecordCountForConnector("notifications")
        val notifSync = syncDao.getLastSyncForConnector("notifications")
        list.add(
            ConnectorItem(
                id = "notifications",
                displayName = "Notification Listener",
                category = "SYSTEM",
                targetPackage = null,
                capabilities = listOf("NOTIFICATION"),
                isEnabled = permState.notificationListener,
                isAuthorized = permState.notificationListener,
                lastSyncTimestamp = notifSync?.completedAtMs,
                lastSyncStatus = if (permState.notificationListener) "ACTIVE" else "PENDING_PERMISSION",
                recordsCount = notifCount
            )
        )

        // 3. Application Usage
        val usageSync = syncDao.getLastSyncForConnector("usage_stats")
        list.add(
            ConnectorItem(
                id = "usage_stats",
                displayName = "Application Usage Statistics",
                category = "SYSTEM",
                targetPackage = null,
                capabilities = listOf("SYSTEM_PROVIDER"),
                isEnabled = permState.usageStats,
                isAuthorized = permState.usageStats,
                lastSyncTimestamp = usageSync?.completedAtMs,
                lastSyncStatus = if (permState.usageStats) "ACTIVE" else "PENDING_PERMISSION",
                recordsCount = 0
            )
        )

        // 4. Calendar
        val calCount = recordDao.getRecordCountForConnector("calendar")
        val calSync = syncDao.getLastSyncForConnector("calendar")
        list.add(
            ConnectorItem(
                id = "calendar",
                displayName = "Calendar Events",
                category = "SYSTEM",
                targetPackage = null,
                capabilities = listOf("SYSTEM_PROVIDER", "OFFICIAL_API"),
                isEnabled = permState.readCalendar,
                isAuthorized = permState.readCalendar,
                lastSyncTimestamp = calSync?.completedAtMs,
                lastSyncStatus = if (permState.readCalendar) (calSync?.status ?: "IDLE") else "PENDING_PERMISSION",
                recordsCount = calCount
            )
        )

        // 5. Contacts
        val cntCount = recordDao.getRecordCountForConnector("contacts")
        val cntSync = syncDao.getLastSyncForConnector("contacts")
        list.add(
            ConnectorItem(
                id = "contacts",
                displayName = "Contacts Provider",
                category = "SYSTEM",
                targetPackage = null,
                capabilities = listOf("SYSTEM_PROVIDER"),
                isEnabled = permState.readContacts,
                isAuthorized = permState.readContacts,
                lastSyncTimestamp = cntSync?.completedAtMs,
                lastSyncStatus = if (permState.readContacts) (cntSync?.status ?: "IDLE") else "PENDING_PERMISSION",
                recordsCount = cntCount
            )
        )

        // 6. Share & Document Import
        val shareCount = recordDao.getRecordCountForConnector("sharing_saf")
        list.add(
            ConnectorItem(
                id = "sharing_saf",
                displayName = "Share & Document Import",
                category = "SYSTEM",
                targetPackage = null,
                capabilities = listOf("SHARE_IMPORT"),
                isEnabled = true,
                isAuthorized = true,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastSyncStatus = "ACTIVE",
                recordsCount = shareCount
            )
        )

        // 7. Gmail
        val gmailCount = recordDao.getRecordCountForConnector("gmail")
        val gmailSync = syncDao.getLastSyncForConnector("gmail")
        list.add(
            ConnectorItem(
                id = "gmail",
                displayName = "Gmail (Read-Only)",
                category = "GOOGLE",
                targetPackage = "com.google.android.gm",
                capabilities = listOf("OFFICIAL_API", "NOTIFICATION"),
                isEnabled = false,
                isAuthorized = false,
                lastSyncTimestamp = gmailSync?.completedAtMs,
                lastSyncStatus = "OAUTH_REQUIRED",
                recordsCount = gmailCount
            )
        )

        // 8. Google Drive
        val driveCount = recordDao.getRecordCountForConnector("google_drive")
        list.add(
            ConnectorItem(
                id = "google_drive",
                displayName = "Google Drive (Selected Files)",
                category = "GOOGLE",
                targetPackage = "com.google.android.apps.docs",
                capabilities = listOf("OFFICIAL_API", "SHARE_IMPORT"),
                isEnabled = true,
                isAuthorized = true,
                lastSyncTimestamp = null,
                lastSyncStatus = "ACTIVE",
                recordsCount = driveCount
            )
        )

        // 9. Google Classroom
        val classCount = recordDao.getRecordCountForConnector("google_classroom")
        list.add(
            ConnectorItem(
                id = "google_classroom",
                displayName = "Google Classroom (Read-Only)",
                category = "GOOGLE",
                targetPackage = "com.google.android.apps.classroom",
                capabilities = listOf("OFFICIAL_API", "NOTIFICATION"),
                isEnabled = false,
                isAuthorized = false,
                lastSyncTimestamp = null,
                lastSyncStatus = "OAUTH_REQUIRED",
                recordsCount = classCount
            )
        )

        // 10. WhatsApp
        val waCount = recordDao.getRecordCountForConnector("whatsapp")
        list.add(
            ConnectorItem(
                id = "whatsapp",
                displayName = "WhatsApp",
                category = "COMMUNICATION",
                targetPackage = "com.whatsapp",
                capabilities = listOf("NOTIFICATION", "SHARE_IMPORT"),
                isEnabled = permState.notificationListener,
                isAuthorized = permState.notificationListener,
                lastSyncTimestamp = null,
                lastSyncStatus = if (permState.notificationListener) "ACTIVE" else "PENDING_PERMISSION",
                recordsCount = waCount
            )
        )

        list
    }

    suspend fun syncConnector(connectorId: String): Boolean = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        var added = 0
        var status = "SUCCESS"
        var errorMsg: String? = null

        try {
            when (connectorId) {
                "calendar" -> {
                    val events = CalendarConnector(context).syncUpcomingEvents()
                    added = events.size
                }
                "contacts" -> {
                    val contacts = ContactsConnector(context).syncContacts()
                    added = contacts.size
                }
                else -> {
                    // Manual sync for other connectors
                    added = 0
                }
            }
        } catch (e: Exception) {
            status = "ERROR"
            errorMsg = e.message
        }

        db.syncHistoryDao().insert(
            SyncHistoryEntity(
                connectorId = connectorId,
                status = status,
                startedAtMs = start,
                completedAtMs = System.currentTimeMillis(),
                recordsProcessed = added,
                recordsAdded = added,
                errorMessage = errorMsg
            )
        )

        status == "SUCCESS"
    }

    suspend fun deleteConnectorData(connectorId: String): Int = withContext(Dispatchers.IO) {
        val deleted = db.collectedRecordDao().deleteRecordsByConnector(connectorId)
        db.syncHistoryDao().deleteHistoryForConnector(connectorId)
        deleted
    }
}
