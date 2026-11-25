package com.cortex.app.connectors.whatsapp

import android.content.Context
import android.net.Uri
import com.cortex.app.database.CortexDatabase
import com.cortex.app.database.entities.CollectedRecordEntity
import com.cortex.app.python.PythonBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class WhatsAppChatImporter(private val context: Context) {

    private val db = CortexDatabase.getInstance(context)
    private val pythonBridge = PythonBridge(context)

    suspend fun importChatFile(uri: Uri, fileName: String): JSONObject = withContext(Dispatchers.IO) {
        val stringBuilder = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }

            val chatContent = stringBuilder.toString()
            val pyResultJson = pythonBridge.parseWhatsAppExport(chatContent, fileName)
            val resultObj = JSONObject(pyResultJson)

            if (resultObj.optBoolean("success", false)) {
                val dataObj = resultObj.optJSONObject("data") ?: JSONObject()
                val totalMsgs = dataObj.optInt("total_messages", 0)
                val skippedFin = dataObj.optInt("skipped_financial_messages", 0)

                val recordId = "wa_import_${System.currentTimeMillis()}"
                db.collectedRecordDao().insert(
                    CollectedRecordEntity(
                        id = recordId,
                        sourceConnector = "whatsapp",
                        packageName = "com.whatsapp",
                        recordType = "CONVERSATION_EXPORT",
                        title = "WhatsApp Chat: $fileName",
                        summary = "$totalMsgs messages imported ($skippedFin financial filtered)",
                        payloadJson = dataObj.toString(),
                        dedupHash = null,
                        timestampMs = System.currentTimeMillis()
                    )
                )
                resultObj
            } else {
                resultObj
            }
        } catch (e: Exception) {
            JSONObject().apply {
                put("success", false)
                put("error", e.message)
            }
        }
    }
}
