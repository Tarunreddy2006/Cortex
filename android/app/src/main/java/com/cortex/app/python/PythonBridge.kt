package com.cortex.app.python

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.cortex.app.privacy.FinancialExclusionPolicy
import org.json.JSONObject

class PythonBridge(private val context: Context) {

    private val TAG = "PythonBridge"
    private var isInitialized = false

    init {
        ensureInitialized()
    }

    private fun ensureInitialized() {
        if (isInitialized) return
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context.applicationContext))
            }
            isInitialized = true
            Log.d(TAG, "Chaquopy Python runtime initialized successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Chaquopy runtime start warning (expected in non-Chaquopy test environment): ${e.message}")
            isInitialized = false
        }
    }

    fun isFinancialPackage(packageName: String, customDenylistJson: String = "[]"): Boolean {
        if (!isInitialized) {
            return FinancialExclusionPolicy.isFinancialPackage(packageName)
        }
        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            val pyResult = module.callAttr("is_financial_package", packageName, customDenylistJson)
            pyResult.toBoolean()
        } catch (e: Exception) {
            Log.e(TAG, "Python is_financial_package error: ${e.message}")
            FinancialExclusionPolicy.isFinancialPackage(packageName)
        }
    }

    fun processNotification(
        packageName: String,
        appName: String,
        title: String,
        text: String,
        timestampMs: Long,
        isGrouped: Boolean = false,
        groupKey: String? = null,
        customDenylist: List<String> = emptyList()
    ): String {
        val inputObj = JSONObject().apply {
            put("package_name", packageName)
            put("app_name", appName)
            put("title", title)
            put("text", text)
            put("timestamp_ms", timestampMs)
            put("is_grouped", isGrouped)
            put("group_key", groupKey)
            put("custom_denylist", org.json.JSONArray(customDenylist))
        }

        if (!isInitialized) {
            val isFin = FinancialExclusionPolicy.isFinancialPackage(packageName)
            val res = JSONObject()
            if (isFin) {
                res.put("success", false)
                res.put("reason", "Excluded by permanent financial policy")
            } else {
                res.put("success", true)
                res.put("record", JSONObject().apply {
                    put("id", "notif_${System.currentTimeMillis()}")
                    put("package_name", packageName)
                    put("title", title)
                    put("text", text)
                    put("timestamp_ms", timestampMs)
                })
                res.put("reason", "Accepted (Kotlin fallback)")
            }
            return res.toString()
        }

        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            val result = module.callAttr("process_notification", inputObj.toString())
            result.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("success", false)
                put("reason", "Python execution error: ${e.message}")
            }.toString()
        }
    }

    fun parseWhatsAppExport(chatText: String, fileName: String = "chat.txt"): String {
        if (!isInitialized) {
            return JSONObject().apply {
                put("success", false)
                put("error", "Python runtime not initialized")
            }.toString()
        }
        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            val result = module.callAttr("parse_whatsapp_export", chatText, fileName)
            result.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("success", false)
                put("error", e.message)
            }.toString()
        }
    }

    fun processSharedLink(url: String, title: String, sourceApp: String): String {
        if (!isInitialized) {
            return JSONObject().apply {
                put("success", true)
                put("record", JSONObject().apply {
                    put("id", "link_${url.hashCode()}")
                    put("url", url)
                    put("title", title)
                    put("source_application", sourceApp)
                })
            }.toString()
        }
        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            val result = module.callAttr("process_shared_link", url, title, sourceApp)
            result.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("success", false)
                put("reason", e.message)
            }.toString()
        }
    }

    // Phase 2: Memory Engine
    fun processMemory(inputJson: String): String {
        if (!isInitialized) {
            val inputObj = JSONObject(inputJson)
            val content = inputObj.optString("content", "")
            return JSONObject().apply {
                put("success", true)
                put("record", JSONObject().apply {
                    put("id", "mem_${System.currentTimeMillis()}")
                    put("content", content)
                    put("category", "PERSONAL")
                    put("confidence_status", "USER_EXPLICIT")
                    put("tags", org.json.JSONArray())
                })
                put("reason", "Accepted (Kotlin fallback)")
            }.toString()
        }
        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            val result = module.callAttr("process_memory", inputJson)
            result.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("success", false)
                put("reason", e.message)
            }.toString()
        }
    }

    // Phase 2: Knowledge Pipeline
    fun ingestDocument(inputJson: String): String {
        if (!isInitialized) {
            val inputObj = JSONObject(inputJson)
            val fileName = inputObj.optString("file_name", "document")
            return JSONObject().apply {
                put("success", true)
                put("record", JSONObject().apply {
                    put("id", "doc_${System.currentTimeMillis()}")
                    put("title", fileName)
                    put("file_name", fileName)
                    put("file_type", "TXT")
                    put("category", "GENERAL")
                    put("tags", org.json.JSONArray())
                })
                put("reason", "Accepted (Kotlin fallback)")
            }.toString()
        }
        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            val result = module.callAttr("ingest_document", inputJson)
            result.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("success", false)
                put("reason", e.message)
            }.toString()
        }
    }

    // ==================== PHASE 3 INTELLIGENCE METHODS ====================

    fun askCortex(inputJson: String): String {
        if (!isInitialized) {
            return JSONObject().apply {
                put("success", true)
                put("data", JSONObject().apply {
                    put("answer", "Cortex offline intelligence running in local fallback mode.")
                    put("citations", org.json.JSONArray())
                    put("confidence", 0.8)
                    put("has_personal_records", false)
                })
            }.toString()
        }
        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            val result = module.callAttr("ask_cortex", inputJson)
            result.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("success", false)
                put("error", e.message)
            }.toString()
        }
    }

    fun generateBriefing(inputJson: String): String {
        if (!isInitialized) return "{ \"success\": false, \"error\": \"Python runtime not started\" }"
        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            module.callAttr("generate_briefing", inputJson).toString()
        } catch (e: Exception) {
            JSONObject().apply { put("success", false); put("error", e.message) }.toString()
        }
    }

    fun generateStudyPlan(inputJson: String): String {
        if (!isInitialized) return "{ \"success\": false, \"error\": \"Python runtime not started\" }"
        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            module.callAttr("generate_study_plan", inputJson).toString()
        } catch (e: Exception) {
            JSONObject().apply { put("success", false); put("error", e.message) }.toString()
        }
    }

    fun generateDailyPlan(inputJson: String): String {
        if (!isInitialized) return "{ \"success\": false, \"error\": \"Python runtime not started\" }"
        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            module.callAttr("generate_daily_plan", inputJson).toString()
        } catch (e: Exception) {
            JSONObject().apply { put("success", false); put("error", e.message) }.toString()
        }
    }

    fun rescheduleTask(inputJson: String): String {
        if (!isInitialized) return "{ \"success\": false, \"error\": \"Python runtime not started\" }"
        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            module.callAttr("reschedule_task", inputJson).toString()
        } catch (e: Exception) {
            JSONObject().apply { put("success", false); put("error", e.message) }.toString()
        }
    }

    fun evaluateDecision(inputJson: String): String {
        if (!isInitialized) return "{ \"success\": false, \"error\": \"Python runtime not started\" }"
        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            module.callAttr("evaluate_decision", inputJson).toString()
        } catch (e: Exception) {
            JSONObject().apply { put("success", false); put("error", e.message) }.toString()
        }
    }

    fun analyzeProject(inputJson: String): String {
        if (!isInitialized) return "{ \"success\": false, \"error\": \"Python runtime not started\" }"
        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            module.callAttr("analyze_project", inputJson).toString()
        } catch (e: Exception) {
            JSONObject().apply { put("success", false); put("error", e.message) }.toString()
        }
    }

    fun getProactiveAlerts(inputJson: String): String {
        if (!isInitialized) return "{ \"success\": false, \"error\": \"Python runtime not started\" }"
        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            module.callAttr("get_proactive_alerts", inputJson).toString()
        } catch (e: Exception) {
            JSONObject().apply { put("success", false); put("error", e.message) }.toString()
        }
    }

    fun getLocalAiStatus(inputJson: String = "{}"): String {
        if (!isInitialized) return "{ \"success\": true, \"data\": { \"is_offline_operational\": true, \"active_provider\": \"DETERMINISTIC_FALLBACK\" } }"
        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            module.callAttr("get_local_ai_status", inputJson).toString()
        } catch (e: Exception) {
            JSONObject().apply { put("success", false); put("error", e.message) }.toString()
        }
    }

    fun reasonMemories(inputJson: String): String {
        if (!isInitialized) return "{ \"success\": false, \"error\": \"Python runtime not started\" }"
        return try {
            val py = Python.getInstance()
            val module = py.getModule("cortex_entry")
            module.callAttr("reason_memories", inputJson).toString()
        } catch (e: Exception) {
            JSONObject().apply { put("success", false); put("error", e.message) }.toString()
        }
    }
}
