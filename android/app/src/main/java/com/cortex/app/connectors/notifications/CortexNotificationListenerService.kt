package com.cortex.app.connectors.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.cortex.app.database.CortexDatabase
import com.cortex.app.database.entities.CollectedRecordEntity
import com.cortex.app.privacy.ContentSanitizer
import com.cortex.app.privacy.FinancialExclusionPolicy
import com.cortex.app.python.PythonBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.security.MessageDigest

class CortexNotificationListenerService : NotificationListenerService() {

    private val TAG = "CortexNotificationSvc"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var db: CortexDatabase
    private lateinit var pythonBridge: PythonBridge

    override fun onCreate() {
        super.onCreate()
        db = CortexDatabase.getInstance(applicationContext)
        pythonBridge = PythonBridge(applicationContext)
        Log.i(TAG, "CortexNotificationListenerService started")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val timestamp = sbn.postTime

        serviceScope.launch {
            // STEP 1: Inspect source package identifier
            // STEP 2: Check the permanent financial denylist first
            if (FinancialExclusionPolicy.isFinancialPackage(packageName)) {
                // Non-negotiable exclusion: Do not read, log or persist
                return@launch
            }

            // STEP 3: Check user's selected application allowlist
            val isEnabled = db.appSelectionDao().isAppEnabled(packageName)
            if (isEnabled != true) {
                // Application not authorized by user
                return@launch
            }

            // STEP 4: Only then extract permitted notification content
            val notification = sbn.notification ?: return@launch
            val extras = notification.extras ?: return@launch

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            val effectiveText = if (!bigText.isNullOrBlank()) bigText else text

            if (title.isBlank() && effectiveText.isBlank()) return@launch

            // STEP 5: Apply additional financial content exclusions
            if (ContentSanitizer.containsFinancialContent("$title $effectiveText")) {
                // Contains sensitive financial trace, OTP or banking info
                return@launch
            }

            // Deduplication check (1-minute window hash)
            val windowMinutes = timestamp / 60000
            val dedupHash = computeHash("$packageName|$title|$effectiveText|$windowMinutes")
            if (db.collectedRecordDao().hasRecordWithHash(dedupHash)) {
                // Duplicate notification alert
                return@launch
            }

            // Group metadata
            val isGrouped = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
            val groupKey = sbn.groupKey

            // Process through Python processing engine
            val pyResultJson = pythonBridge.processNotification(
                packageName = packageName,
                appName = FinancialExclusionPolicy.getKnownAppName(packageName),
                title = title,
                text = effectiveText,
                timestampMs = timestamp,
                isGrouped = isGrouped,
                groupKey = groupKey
            )

            val pyObj = JSONObject(pyResultJson)
            if (pyObj.optBoolean("success", false)) {
                val recordObj = pyObj.optJSONObject("record")
                val recordId = recordObj?.optString("id") ?: "notif_${System.currentTimeMillis()}"

                // STEP 6: Store permitted normalized information in Room
                val entity = CollectedRecordEntity(
                    id = recordId,
                    sourceConnector = "notifications",
                    packageName = packageName,
                    recordType = "NOTIFICATION",
                    title = title,
                    summary = effectiveText.take(150),
                    payloadJson = recordObj?.toString() ?: "{}",
                    dedupHash = dedupHash,
                    timestampMs = timestamp
                )
                db.collectedRecordDao().insert(entity)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Handled cleanly; Cortex does not delete records on notification dismissal
    }

    private fun computeHash(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
