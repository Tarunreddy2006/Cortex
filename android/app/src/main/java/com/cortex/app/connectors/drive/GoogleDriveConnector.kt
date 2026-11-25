package com.cortex.app.connectors.drive

import android.content.Context
import android.net.Uri
import com.cortex.app.database.CortexDatabase
import com.cortex.app.database.entities.CollectedRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Dedicated Google Drive Connector.
 * Scope: https://www.googleapis.com/auth/drive.file (per-file narrow scope).
 * Uses Storage Access Framework / Google Picker so user selects specific documents.
 */
class GoogleDriveConnector(private val context: Context) {

    private val db = CortexDatabase.getInstance(context)

    companion object {
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }

    suspend fun importSelectedDriveFile(
        fileUri: Uri,
        fileName: String,
        mimeType: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val driveDir = File(context.filesDir, "drive_docs").apply { if (!exists()) mkdirs() }
            val localFile = File(driveDir, "drive_${System.currentTimeMillis()}_$fileName")

            context.contentResolver.openInputStream(fileUri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }

            val recordId = "drive_${System.currentTimeMillis()}"
            val payload = JSONObject().apply {
                put("file_id", recordId)
                put("file_name", fileName)
                put("mime_type", mimeType)
                put("local_path", localFile.absolutePath)
                put("file_size_bytes", localFile.length())
            }

            db.collectedRecordDao().insert(
                CollectedRecordEntity(
                    id = recordId,
                    sourceConnector = "google_drive",
                    packageName = "com.google.android.apps.docs",
                    recordType = "DOCUMENT",
                    title = fileName,
                    summary = "Drive Document: $mimeType (${localFile.length() / 1024} KB)",
                    payloadJson = payload.toString(),
                    dedupHash = null,
                    timestampMs = System.currentTimeMillis()
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
