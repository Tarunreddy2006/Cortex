package com.cortex.app.connectors.sharing

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cortex.app.database.CortexDatabase
import com.cortex.app.database.entities.CollectedRecordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class ShareReceiverActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            handleSend(intent, type)
        } else if (Intent.ACTION_SEND_MULTIPLE == action && type != null) {
            handleSendMultiple(intent, type)
        } else {
            finish()
        }
    }

    private fun handleSend(intent: Intent, type: String) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

        CoroutineScope(Dispatchers.IO).launch {
            val db = CortexDatabase.getInstance(applicationContext)

            if (!sharedText.isNullOrBlank()) {
                val isUrl = sharedText.trim().startsWith("http://") || sharedText.trim().startsWith("https://")
                val recordId = "share_${System.currentTimeMillis()}"

                val payload = JSONObject().apply {
                    put("id", recordId)
                    put("type", if (isUrl) "LINK" else "TEXT")
                    put("content", sharedText)
                }

                db.collectedRecordDao().insert(
                    CollectedRecordEntity(
                        id = recordId,
                        sourceConnector = "sharing_saf",
                        packageName = callingPackage,
                        recordType = if (isUrl) "LINK" else "DOCUMENT",
                        title = if (isUrl) "Shared Link" else "Shared Text Snippet",
                        summary = sharedText.take(120),
                        payloadJson = payload.toString(),
                        dedupHash = null,
                        timestampMs = System.currentTimeMillis()
                    )
                )
            }

            if (streamUri != null) {
                saveStreamUri(streamUri, type, db)
            }

            launch(Dispatchers.Main) {
                Toast.makeText(this@ShareReceiverActivity, "Imported to Cortex", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun handleSendMultiple(intent: Intent, type: String) {
        val streamUris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        if (streamUris != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = CortexDatabase.getInstance(applicationContext)
                for (uri in streamUris) {
                    saveStreamUri(uri, type, db)
                }
                launch(Dispatchers.Main) {
                    Toast.makeText(this@ShareReceiverActivity, "Imported ${streamUris.size} files to Cortex", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            finish()
        }
    }

    private suspend fun saveStreamUri(uri: Uri, mimeType: String, db: CortexDatabase) {
        try {
            val sharedDir = File(filesDir, "shared").apply { if (!exists()) mkdirs() }
            val fileName = "doc_${System.currentTimeMillis()}_${uri.lastPathSegment ?: "file"}"
            val targetFile = File(sharedDir, fileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            val recordId = "doc_${System.currentTimeMillis()}"
            val payload = JSONObject().apply {
                put("id", recordId)
                put("file_name", targetFile.name)
                put("file_path", targetFile.absolutePath)
                put("file_size_bytes", targetFile.length())
                put("mime_type", mimeType)
            }

            db.collectedRecordDao().insert(
                CollectedRecordEntity(
                    id = recordId,
                    sourceConnector = "sharing_saf",
                    packageName = callingPackage,
                    recordType = "DOCUMENT",
                    title = targetFile.name,
                    summary = "Imported $mimeType (${targetFile.length() / 1024} KB)",
                    payloadJson = payload.toString(),
                    dedupHash = null,
                    timestampMs = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
