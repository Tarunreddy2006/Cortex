package com.cortex.app.connectors.gmail

import android.content.Context
import com.cortex.app.database.CortexDatabase
import com.cortex.app.database.entities.CollectedRecordEntity
import com.cortex.app.privacy.ContentSanitizer
import com.cortex.app.privacy.FinancialExclusionPolicy
import com.cortex.app.python.PythonBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Dedicated Read-Only Gmail Connector.
 * Scope: https://www.googleapis.com/auth/gmail.readonly (minimum necessary read-only scope).
 * Enforces financial content exclusion before persistence.
 */
class GmailConnector(private val context: Context) {

    private val db = CortexDatabase.getInstance(context)
    private val pythonBridge = PythonBridge(context)

    companion object {
        const val GMAIL_READONLY_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
    }

    suspend fun processFetchedEmail(
        messageId: String,
        threadId: String,
        sender: String,
        subject: String,
        snippet: String,
        timestampMs: Long,
        bodyText: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        // 1. Strict financial content check
        if (ContentSanitizer.containsFinancialContent("$subject $snippet", sender)) {
            // Discard financial statements, alerts, and OTPs
            return@withContext false
        }

        // 2. Check deduplication
        val recordId = "email_$messageId"
        if (db.collectedRecordDao().hasRecordWithHash(recordId)) {
            return@withContext false
        }

        // 3. Save to database
        val payload = JSONObject().apply {
            put("message_id", messageId)
            put("thread_id", threadId)
            put("sender", sender)
            put("subject", subject)
            put("snippet", snippet)
            put("timestamp_ms", timestampMs)
        }

        db.collectedRecordDao().insert(
            CollectedRecordEntity(
                id = recordId,
                sourceConnector = "gmail",
                packageName = "com.google.android.gm",
                recordType = "EMAIL",
                title = subject,
                summary = "From $sender: ${snippet.take(100)}",
                payloadJson = payload.toString(),
                dedupHash = recordId,
                timestampMs = timestampMs
            )
        )
        true
    }
}
