package com.cortex.app.connectors

import android.content.Context
import com.cortex.app.database.CortexDatabase
import com.cortex.app.database.entities.MemoryEntity
import com.cortex.app.database.entities.TimelineEventEntity
import com.cortex.app.privacy.ContentSanitizer
import com.cortex.app.privacy.FinancialExclusionPolicy
import com.cortex.app.python.PythonBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Standardized Ingestion Pipeline:
 * AUTHORIZED CONNECTOR -> PRIVACY GATE -> DATA NORMALIZATION -> SOURCE VALIDATION
 * -> CONTENT CLASSIFICATION -> MEMORY/KNOWLEDGE PROCESSING -> LOCAL STORAGE
 */
class MemoryIngestionService(private val context: Context) {

    private val db = CortexDatabase.getInstance(context)
    private val pythonBridge = PythonBridge(context)

    suspend fun ingestFromConnector(
        content: String,
        sourceConnector: String,
        sourceRecordId: String? = null,
        isUserExplicit: Boolean = false,
        timestampMs: Long = System.currentTimeMillis()
    ): Boolean = withContext(Dispatchers.IO) {
        // STEP 1: Privacy Gate - Non-negotiable Financial Exclusion
        if (FinancialExclusionPolicy.isFinancialPackage(sourceConnector)) {
            return@withContext false
        }
        if (ContentSanitizer.containsFinancialContent(content)) {
            return@withContext false
        }

        // STEP 2: Idempotent Ingestion Check
        if (sourceRecordId != null) {
            val existing = db.memoryDao().getAllMemories().firstOrNull { it.sourceRecordId == sourceRecordId }
            if (existing != null) {
                return@withContext false // Already ingested
            }
        }

        // STEP 3: Process through Python Memory Engine
        val pyInput = JSONObject().apply {
            put("content", content)
            put("source_connector", sourceConnector)
            put("source_record_id", sourceRecordId ?: JSONObject.NULL)
            put("is_user_explicit", isUserExplicit)
        }

        val pyResultJson = pythonBridge.processMemory(pyInput.toString())
        val resultObj = JSONObject(pyResultJson)

        if (!resultObj.optBoolean("success", false)) {
            return@withContext false
        }

        val recordObj = resultObj.optJSONObject("record") ?: return@withContext false
        val memoryId = recordObj.optString("id")
        val category = recordObj.optString("category", "PERSONAL")
        val confidence = recordObj.optString("confidence_status", "EXTRACTED")
        val tagsArr = recordObj.optJSONArray("tags")
        val tagsJson = tagsArr?.toString() ?: "[]"

        // STEP 4: Persist Memory in Room Database
        val memoryEntity = MemoryEntity(
            id = memoryId,
            content = content.trim(),
            category = category,
            sourceConnector = sourceConnector,
            sourceRecordId = sourceRecordId,
            confidenceStatus = confidence,
            approvalStatus = "APPROVED",
            isPinned = false,
            tagsJson = tagsJson,
            relatedMemoryIdsJson = "[]",
            createdAt = timestampMs,
            updatedAt = timestampMs
        )
        db.memoryDao().insertOrUpdate(memoryEntity)

        // STEP 5: Ingest into Chronological Personal Timeline
        val timelineEntity = TimelineEventEntity(
            id = "tl_${memoryId}",
            timestampMs = timestampMs,
            eventType = "MEMORY",
            title = "New $category Memory",
            summary = content.take(120),
            sourceConnector = sourceConnector,
            referenceId = memoryId,
            isSignificant = true
        )
        db.timelineDao().insert(timelineEntity)

        true
    }

    private fun computeHash(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
