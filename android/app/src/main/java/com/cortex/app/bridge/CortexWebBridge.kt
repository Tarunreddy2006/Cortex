package com.cortex.app.bridge

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.cortex.app.MainActivity
import com.cortex.app.connectors.ConnectorRegistry
import com.cortex.app.connectors.MemoryIngestionService
import com.cortex.app.connectors.usage.UsageStatsConnector
import com.cortex.app.database.CortexDatabase
import com.cortex.app.database.entities.*
import com.cortex.app.discovery.AppDiscoveryService
import com.cortex.app.permissions.PermissionManager
import com.cortex.app.privacy.FinancialExclusionPolicy
import com.cortex.app.python.PythonBridge
import com.cortex.app.search.UniversalSearchService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class CortexWebBridge(
    private val activity: Activity,
    private val webView: WebView
) {
    private val context: Context = activity.applicationContext
    private val db = CortexDatabase.getInstance(context)
    private val discoveryService = AppDiscoveryService(context)
    private val connectorRegistry = ConnectorRegistry(context)
    private val usageConnector = UsageStatsConnector(context)
    private val memoryService = MemoryIngestionService(context)
    private val searchService = UniversalSearchService(context)
    private val pythonBridge = PythonBridge(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun postToJs(callbackId: String, responseJson: String) {
        mainHandler.post {
            val safeScript = "window.CortexBridgeCallbacks && window.CortexBridgeCallbacks.handle('$callbackId', $responseJson);"
            webView.evaluateJavascript(safeScript, null)
        }
    }

    // ==========================================
    // PHASE 1: FOUNDATION & CONNECTORS
    // ==========================================

    @JavascriptInterface
    fun getInstalledApps(callbackId: String) {
        scope.launch {
            try {
                val profiles = discoveryService.discoverInstalledApps()
                val array = JSONArray()
                for (p in profiles) {
                    val obj = JSONObject().apply {
                        put("packageName", p.packageName)
                        put("appName", p.appName)
                        put("category", p.category.name)
                        put("isSystemApp", p.isSystemApp)
                        put("isEnabled", p.isEnabled)
                        put("supportedCapabilities", JSONArray(p.supportedCapabilities))
                        put("iconBase64", p.iconBase64 ?: JSONObject.NULL)
                        put("versionName", p.versionName ?: "")
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to discover apps").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun saveAppSelections(selectionsJson: String, callbackId: String) {
        scope.launch {
            try {
                val json = JSONObject(selectionsJson)
                val map = mutableMapOf<String, Boolean>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = json.getBoolean(key)
                }
                discoveryService.saveSelections(map)
                postToJs(callbackId, BridgeResponse.ok("Selections saved successfully").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to save selections").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getPermissionStatus(callbackId: String) {
        scope.launch {
            val state = PermissionManager.checkAllPermissions(context)
            val obj = JSONObject().apply {
                put("notificationListener", state.notificationListener)
                put("usageStats", state.usageStats)
                put("readCalendar", state.readCalendar)
                put("readContacts", state.readContacts)
                put("postNotifications", state.postNotifications)
                put("allGranted", state.areAllGranted())
                put("pendingCount", state.getPendingCount())
            }
            postToJs(callbackId, BridgeResponse.ok(obj).toJsonString())
        }
    }

    @JavascriptInterface
    fun openPermissionSettings(permissionType: String) {
        mainHandler.post {
            when (permissionType) {
                "notificationListener" -> {
                    activity.startActivity(PermissionManager.getNotificationListenerSettingsIntent())
                }
                "usageStats" -> {
                    activity.startActivity(PermissionManager.getUsageStatsSettingsIntent(activity.packageName))
                }
                "readCalendar", "readContacts", "postNotifications" -> {
                    if (activity is MainActivity) {
                        activity.requestRuntimePermissions()
                    }
                }
            }
        }
    }

    @JavascriptInterface
    fun getConnectors(callbackId: String) {
        scope.launch {
            try {
                val connectors = connectorRegistry.getConnectorsList()
                val array = JSONArray()
                for (c in connectors) {
                    val obj = JSONObject().apply {
                        put("id", c.id)
                        put("displayName", c.displayName)
                        put("category", c.category)
                        put("targetPackage", c.targetPackage ?: JSONObject.NULL)
                        put("capabilities", JSONArray(c.capabilities))
                        put("isEnabled", c.isEnabled)
                        put("isAuthorized", c.isAuthorized)
                        put("lastSyncTimestamp", c.lastSyncTimestamp ?: JSONObject.NULL)
                        put("lastSyncStatus", c.lastSyncStatus)
                        put("recordsCount", c.recordsCount)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get connectors").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun syncConnector(connectorId: String, callbackId: String) {
        scope.launch {
            try {
                val success = connectorRegistry.syncConnector(connectorId)
                if (success) {
                    postToJs(callbackId, BridgeResponse.ok("Sync completed").toJsonString())
                } else {
                    postToJs(callbackId, BridgeResponse.err("Sync failed").toJsonString())
                }
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Sync error").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun deleteConnectorData(connectorId: String, callbackId: String) {
        scope.launch {
            try {
                val count = connectorRegistry.deleteConnectorData(connectorId)
                db.memoryDao().deleteMemoriesBySource(connectorId)
                db.knowledgeDao().deleteDocumentsBySource(connectorId)
                postToJs(callbackId, BridgeResponse.ok("Deleted $count records").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Deletion error").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getUsageStats(callbackId: String) {
        scope.launch {
            try {
                val summaries = usageConnector.getUsageSummaries()
                val array = JSONArray()
                for (s in summaries) {
                    val obj = JSONObject().apply {
                        put("packageName", s.packageName)
                        put("appName", s.appName)
                        put("dailyMinutes", s.dailyMinutes)
                        put("weeklyMinutes", s.weeklyMinutes)
                        put("lastTimeUsedMs", s.lastTimeUsedMs)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Usage stats error").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getFinancialExclusions(callbackId: String) {
        scope.launch {
            try {
                val exclusions = db.financialExclusionDao().getAllExclusions()
                val array = JSONArray()
                for (ex in exclusions) {
                    val obj = JSONObject().apply {
                        put("packageName", ex.packageName)
                        put("appName", ex.appName)
                        put("isPermanent", ex.isPermanent)
                        put("addedByUser", ex.addedByUser)
                        put("reason", ex.reason)
                        put("createdAt", ex.createdAt)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Exclusions error").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun addCustomFinancialExclusion(packageName: String, reason: String, callbackId: String) {
        scope.launch {
            try {
                val cleanPkg = packageName.trim()
                if (cleanPkg.isBlank()) {
                    postToJs(callbackId, BridgeResponse.err("Package name cannot be empty").toJsonString())
                    return@launch
                }
                val entity = FinancialExclusionEntity(
                    packageName = cleanPkg,
                    appName = FinancialExclusionPolicy.getKnownAppName(cleanPkg),
                    isPermanent = false,
                    addedByUser = true,
                    reason = reason.ifBlank { "User custom financial exclusion" }
                )
                db.financialExclusionDao().insert(entity)
                db.appSelectionDao().setEnabled(cleanPkg, false)
                postToJs(callbackId, BridgeResponse.ok("Excluded package $cleanPkg").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to add exclusion").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun removeCustomFinancialExclusion(packageName: String, callbackId: String) {
        scope.launch {
            try {
                val removed = db.financialExclusionDao().removeUserExclusion(packageName.trim())
                if (removed > 0) {
                    postToJs(callbackId, BridgeResponse.ok("Custom exclusion removed").toJsonString())
                } else {
                    postToJs(callbackId, BridgeResponse.err("Cannot remove permanent exclusion").toJsonString())
                }
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to remove exclusion").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getSyncHistory(callbackId: String) {
        scope.launch {
            try {
                val history = db.syncHistoryDao().getRecentSyncs(50)
                val array = JSONArray()
                for (h in history) {
                    val obj = JSONObject().apply {
                        put("id", h.id)
                        put("connectorId", h.connectorId)
                        put("status", h.status)
                        put("startedAtMs", h.startedAtMs)
                        put("completedAtMs", h.completedAtMs)
                        put("recordsProcessed", h.recordsProcessed)
                        put("recordsAdded", h.recordsAdded)
                        put("errorMessage", h.errorMessage ?: JSONObject.NULL)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get sync history").toJsonString())
            }
        }
    }

    // ==========================================
    // PHASE 2: PERSONAL MEMORY ENGINE
    // ==========================================

    @JavascriptInterface
    fun getMemories(category: String, callbackId: String) {
        scope.launch {
            try {
                val memories = if (category.isBlank() || category == "ALL") {
                    db.memoryDao().getAllMemories()
                } else {
                    db.memoryDao().getMemoriesByCategory(category)
                }
                val array = JSONArray()
                for (m in memories) {
                    val obj = JSONObject().apply {
                        put("id", m.id)
                        put("content", m.content)
                        put("category", m.category)
                        put("sourceConnector", m.sourceConnector)
                        put("confidenceStatus", m.confidenceStatus)
                        put("approvalStatus", m.approvalStatus)
                        put("isPinned", m.isPinned)
                        put("tags", JSONArray(m.tagsJson))
                        put("createdAt", m.createdAt)
                        put("updatedAt", m.updatedAt)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get memories").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun addMemory(content: String, category: String, isPinned: Boolean, callbackId: String) {
        scope.launch {
            try {
                val success = memoryService.ingestFromConnector(
                    content = content,
                    sourceConnector = "user_explicit",
                    isUserExplicit = true
                )
                if (success) {
                    val all = db.memoryDao().getAllMemories()
                    val latest = all.maxByOrNull { it.createdAt }
                    if (latest != null && (category.isNotBlank() || isPinned)) {
                        val updated = latest.copy(
                            category = if (category.isNotBlank()) category else latest.category,
                            isPinned = isPinned
                        )
                        db.memoryDao().insertOrUpdate(updated)
                    }
                    postToJs(callbackId, BridgeResponse.ok("Memory saved").toJsonString())
                } else {
                    postToJs(callbackId, BridgeResponse.err("Memory rejected (check financial policy or content)").toJsonString())
                }
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to add memory").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun togglePinMemory(id: String, isPinned: Boolean, callbackId: String) {
        scope.launch {
            try {
                db.memoryDao().setPinned(id, isPinned)
                postToJs(callbackId, BridgeResponse.ok("Pin status updated").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to toggle pin").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun deleteMemory(id: String, callbackId: String) {
        scope.launch {
            try {
                db.memoryDao().deleteMemory(id)
                postToJs(callbackId, BridgeResponse.ok("Memory deleted").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to delete memory").toJsonString())
            }
        }
    }

    // ==========================================
    // PHASE 2: KNOWLEDGE VAULT (Docs, Notes, Collections)
    // ==========================================

    @JavascriptInterface
    fun getDocuments(collectionId: String, callbackId: String) {
        scope.launch {
            try {
                val docs = if (collectionId.isBlank() || collectionId == "ALL") {
                    db.knowledgeDao().getAllDocuments()
                } else {
                    db.knowledgeDao().getDocumentsByCollection(collectionId)
                }
                val array = JSONArray()
                for (d in docs) {
                    val obj = JSONObject().apply {
                        put("id", d.id)
                        put("title", d.title)
                        put("fileName", d.fileName)
                        put("fileType", d.fileType)
                        put("fileSizeBytes", d.fileSizeBytes)
                        put("summary", d.summary ?: "")
                        put("category", d.category)
                        put("collectionId", d.collectionId ?: "")
                        put("tags", JSONArray(d.tagsJson))
                        put("importedAt", d.importedAt)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get documents").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getNotes(collectionId: String, callbackId: String) {
        scope.launch {
            try {
                val notes = if (collectionId.isBlank() || collectionId == "ALL") {
                    db.knowledgeDao().getAllNotes()
                } else {
                    db.knowledgeDao().getNotesByCollection(collectionId)
                }
                val array = JSONArray()
                for (n in notes) {
                    val obj = JSONObject().apply {
                        put("id", n.id)
                        put("title", n.title)
                        put("content", n.content)
                        put("collectionId", n.collectionId ?: "")
                        put("tags", JSONArray(n.tagsJson))
                        put("updatedAt", n.updatedAt)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get notes").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun saveNote(id: String, title: String, content: String, collectionId: String, callbackId: String) {
        scope.launch {
            try {
                val noteId = if (id.isBlank()) "note_${System.currentTimeMillis()}" else id
                val note = NoteEntity(
                    id = noteId,
                    title = title.ifBlank { "Untitled Note" },
                    content = content,
                    collectionId = if (collectionId.isBlank()) null else collectionId,
                    tagsJson = "[]",
                    linkedDocumentIdsJson = "[]",
                    updatedAt = System.currentTimeMillis()
                )
                db.knowledgeDao().insertOrUpdateNote(note)

                // Add to timeline
                db.timelineDao().insert(
                    TimelineEventEntity(
                        id = "tl_$noteId",
                        timestampMs = System.currentTimeMillis(),
                        eventType = "NOTE",
                        title = "Saved Note: ${note.title}",
                        summary = content.take(100),
                        sourceConnector = "knowledge_vault",
                        referenceId = noteId,
                        isSignificant = true
                    )
                )

                postToJs(callbackId, BridgeResponse.ok("Note saved").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to save note").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun deleteNote(id: String, callbackId: String) {
        scope.launch {
            try {
                db.knowledgeDao().deleteNote(id)
                postToJs(callbackId, BridgeResponse.ok("Note deleted").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to delete note").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getCollections(callbackId: String) {
        scope.launch {
            try {
                val collections = db.knowledgeDao().getAllCollections()
                val array = JSONArray()
                for (c in collections) {
                    val obj = JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("description", c.description ?: "")
                        put("colorHex", c.colorHex)
                        put("icon", c.icon)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get collections").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun addCollection(name: String, description: String, colorHex: String, icon: String, callbackId: String) {
        scope.launch {
            try {
                val col = CollectionEntity(
                    id = "col_${System.currentTimeMillis()}",
                    name = name.trim(),
                    description = description.ifBlank { null },
                    colorHex = colorHex.ifBlank { "#3B82F6" },
                    icon = icon.ifBlank { "folder" }
                )
                db.knowledgeDao().insertOrUpdateCollection(col)
                postToJs(callbackId, BridgeResponse.ok("Collection created").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to add collection").toJsonString())
            }
        }
    }

    // ==========================================
    // PHASE 2: ACADEMIC INTELLIGENCE
    // ==========================================

    @JavascriptInterface
    fun getAcademicProfile(callbackId: String) {
        scope.launch {
            try {
                val profile = db.academicDao().getProfile()
                val obj = if (profile != null) {
                    JSONObject().apply {
                        put("institution", profile.institution)
                        put("degree", profile.degree)
                        put("department", profile.department)
                        put("semester", profile.semester)
                        put("academicYear", profile.academicYear)
                    }
                } else {
                    JSONObject().apply {
                        put("institution", "Infinix Tech University")
                        put("degree", "Bachelor of Technology")
                        put("department", "Computer Science & Engineering")
                        put("semester", 6)
                        put("academicYear", "2026-2027")
                    }
                }
                postToJs(callbackId, BridgeResponse.ok(obj).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get academic profile").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun saveAcademicProfile(institution: String, degree: String, department: String, semester: Int, academicYear: String, callbackId: String) {
        scope.launch {
            try {
                val profile = AcademicProfileEntity(
                    institution = institution,
                    degree = degree,
                    department = department,
                    semester = semester,
                    academicYear = academicYear
                )
                db.academicDao().insertOrUpdateProfile(profile)
                postToJs(callbackId, BridgeResponse.ok("Academic profile saved").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to save profile").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getSubjects(callbackId: String) {
        scope.launch {
            try {
                val subjects = db.academicDao().getAllSubjects()
                val array = JSONArray()
                for (s in subjects) {
                    val obj = JSONObject().apply {
                        put("id", s.id)
                        put("subjectCode", s.subjectCode)
                        put("subjectName", s.subjectName)
                        put("facultyName", s.facultyName ?: "")
                        put("credits", s.credits)
                        put("colorHex", s.colorHex)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get subjects").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun saveSubject(id: String, code: String, name: String, faculty: String, credits: Int, colorHex: String, callbackId: String) {
        scope.launch {
            try {
                val subId = if (id.isBlank()) "sub_${System.currentTimeMillis()}" else id
                val subject = AcademicSubjectEntity(
                    id = subId,
                    subjectCode = code.trim().uppercase(),
                    subjectName = name.trim(),
                    facultyName = faculty.ifBlank { null },
                    credits = credits,
                    colorHex = colorHex.ifBlank { "#3B82F6" }
                )
                db.academicDao().insertOrUpdateSubject(subject)
                postToJs(callbackId, BridgeResponse.ok("Subject saved").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to save subject").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun deleteSubject(id: String, callbackId: String) {
        scope.launch {
            try {
                db.academicDao().deleteSubject(id)
                postToJs(callbackId, BridgeResponse.ok("Subject deleted").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to delete subject").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getTimetable(dayOfWeek: Int, callbackId: String) {
        scope.launch {
            try {
                val entries = if (dayOfWeek > 0) {
                    db.academicDao().getTimetableForDay(dayOfWeek)
                } else {
                    db.academicDao().getFullTimetable()
                }
                val subjectsMap = db.academicDao().getAllSubjects().associateBy { it.id }

                val array = JSONArray()
                for (t in entries) {
                    val sub = subjectsMap[t.subjectId]
                    val obj = JSONObject().apply {
                        put("id", t.id)
                        put("dayOfWeek", t.dayOfWeek)
                        put("subjectId", t.subjectId)
                        put("subjectName", sub?.subjectName ?: "Unknown Subject")
                        put("subjectCode", sub?.subjectCode ?: "")
                        put("colorHex", sub?.colorHex ?: "#3B82F6")
                        put("startTime", t.startTime)
                        put("endTime", t.endTime)
                        put("room", t.room ?: "")
                        put("scheduleType", t.scheduleType)
                        put("verificationStatus", t.verificationStatus)
                        put("notes", t.notes ?: "")
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get timetable").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun saveTimetableEntry(id: String, dayOfWeek: Int, subjectId: String, startTime: String, endTime: String, room: String, type: String, callbackId: String) {
        scope.launch {
            try {
                val entryId = if (id.isBlank()) "tt_${System.currentTimeMillis()}" else id
                val entry = TimetableEntity(
                    id = entryId,
                    dayOfWeek = dayOfWeek,
                    subjectId = subjectId,
                    startTime = startTime,
                    endTime = endTime,
                    room = room.ifBlank { null },
                    scheduleType = type.ifBlank { "REGULAR" },
                    verificationStatus = "CONFIRMED"
                )
                db.academicDao().insertOrUpdateTimetable(entry)
                postToJs(callbackId, BridgeResponse.ok("Timetable entry saved").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to save timetable").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun deleteTimetableEntry(id: String, callbackId: String) {
        scope.launch {
            try {
                db.academicDao().deleteTimetableEntry(id)
                postToJs(callbackId, BridgeResponse.ok("Timetable entry removed").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to delete entry").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getAssignments(callbackId: String) {
        scope.launch {
            try {
                val assignments = db.academicDao().getAllAssignments()
                val subjectsMap = db.academicDao().getAllSubjects().associateBy { it.id }

                val array = JSONArray()
                for (a in assignments) {
                    val sub = subjectsMap[a.subjectId]
                    val obj = JSONObject().apply {
                        put("id", a.id)
                        put("subjectId", a.subjectId)
                        put("subjectName", sub?.subjectName ?: "Coursework")
                        put("title", a.title)
                        put("description", a.description ?: "")
                        put("dueDateMs", a.dueDateMs)
                        put("status", a.status)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get assignments").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun saveAssignment(id: String, subjectId: String, title: String, description: String, dueDateMs: Long, status: String, callbackId: String) {
        scope.launch {
            try {
                val aId = if (id.isBlank()) "assign_${System.currentTimeMillis()}" else id
                val assignment = AcademicAssignmentEntity(
                    id = aId,
                    subjectId = subjectId,
                    title = title,
                    description = description.ifBlank { null },
                    dueDateMs = dueDateMs,
                    status = status.ifBlank { "PENDING" }
                )
                db.academicDao().insertOrUpdateAssignment(assignment)

                // Add to timeline
                db.timelineDao().insert(
                    TimelineEventEntity(
                        id = "tl_$aId",
                        timestampMs = System.currentTimeMillis(),
                        eventType = "ACADEMIC",
                        title = "Assignment: $title",
                        summary = "Due: ${java.util.Date(dueDateMs)}",
                        sourceConnector = "academic_module",
                        referenceId = aId,
                        isSignificant = true
                    )
                )

                postToJs(callbackId, BridgeResponse.ok("Assignment saved").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to save assignment").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun updateAssignmentStatus(id: String, status: String, callbackId: String) {
        scope.launch {
            try {
                db.academicDao().updateAssignmentStatus(id, status)
                postToJs(callbackId, BridgeResponse.ok("Assignment status updated").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to update assignment").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getExaminations(callbackId: String) {
        scope.launch {
            try {
                val exams = db.academicDao().getAllExaminations()
                val subjectsMap = db.academicDao().getAllSubjects().associateBy { it.id }

                val array = JSONArray()
                for (ex in exams) {
                    val sub = subjectsMap[ex.subjectId]
                    val obj = JSONObject().apply {
                        put("id", ex.id)
                        put("subjectId", ex.subjectId)
                        put("subjectName", sub?.subjectName ?: "Exam")
                        put("title", ex.title)
                        put("examDateMs", ex.examDateMs)
                        put("durationMinutes", ex.durationMinutes)
                        put("room", ex.room ?: "")
                        put("syllabusCoverage", ex.syllabusCoverage ?: "")
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get examinations").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun saveExamination(id: String, subjectId: String, title: String, examDateMs: Long, duration: Int, room: String, coverage: String, callbackId: String) {
        scope.launch {
            try {
                val eId = if (id.isBlank()) "exam_${System.currentTimeMillis()}" else id
                val exam = AcademicExaminationEntity(
                    id = eId,
                    subjectId = subjectId,
                    title = title,
                    examDateMs = examDateMs,
                    durationMinutes = duration,
                    room = room.ifBlank { null },
                    syllabusCoverage = coverage.ifBlank { null }
                )
                db.academicDao().insertOrUpdateExamination(exam)
                postToJs(callbackId, BridgeResponse.ok("Examination scheduled").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to save examination").toJsonString())
            }
        }
    }

    // ==========================================
    // PHASE 2: LIFE MANAGEMENT (Tasks & Goals)
    // ==========================================

    @JavascriptInterface
    fun getTasks(callbackId: String) {
        scope.launch {
            try {
                val tasks = db.lifeManagementDao().getAllTasks()
                val array = JSONArray()
                for (t in tasks) {
                    val obj = JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("description", t.description ?: "")
                        put("priority", t.priority)
                        put("status", t.status)
                        put("category", t.category)
                        put("dueDateMs", t.dueDateMs ?: JSONObject.NULL)
                        put("isRecurring", t.isRecurring)
                        put("projectId", t.projectId ?: JSONObject.NULL)
                        put("createdAt", t.createdAt)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get tasks").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun saveTask(id: String, title: String, description: String, priority: String, category: String, dueDateMs: Long, projectId: String, callbackId: String) {
        scope.launch {
            try {
                val tId = if (id.isBlank()) "task_${System.currentTimeMillis()}" else id
                val task = TaskEntity(
                    id = tId,
                    title = title,
                    description = description.ifBlank { null },
                    priority = priority.ifBlank { "MEDIUM" },
                    status = "PENDING",
                    category = category.ifBlank { "PERSONAL" },
                    dueDateMs = if (dueDateMs > 0) dueDateMs else null,
                    projectId = if (projectId.isBlank()) null else projectId
                )
                db.lifeManagementDao().insertOrUpdateTask(task)

                // Add to timeline
                db.timelineDao().insert(
                    TimelineEventEntity(
                        id = "tl_$tId",
                        timestampMs = System.currentTimeMillis(),
                        eventType = "TASK",
                        title = "Task: $title",
                        summary = "Priority: $priority, Category: $category",
                        sourceConnector = "life_management",
                        referenceId = tId,
                        isSignificant = true
                    )
                )

                postToJs(callbackId, BridgeResponse.ok("Task saved").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to save task").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun updateTaskStatus(id: String, status: String, callbackId: String) {
        scope.launch {
            try {
                db.lifeManagementDao().updateTaskStatus(id, status)
                postToJs(callbackId, BridgeResponse.ok("Task updated").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to update task").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun deleteTask(id: String, callbackId: String) {
        scope.launch {
            try {
                db.lifeManagementDao().deleteTask(id)
                postToJs(callbackId, BridgeResponse.ok("Task deleted").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to delete task").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getGoals(callbackId: String) {
        scope.launch {
            try {
                val goals = db.lifeManagementDao().getAllGoals()
                val array = JSONArray()
                for (g in goals) {
                    val obj = JSONObject().apply {
                        put("id", g.id)
                        put("title", g.title)
                        put("description", g.description ?: "")
                        put("goalType", g.goalType)
                        put("targetDateMs", g.targetDateMs ?: JSONObject.NULL)
                        put("progressPercent", g.progressPercent)
                        put("status", g.status)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get goals").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun saveGoal(id: String, title: String, description: String, goalType: String, targetDateMs: Long, progressPercent: Int, callbackId: String) {
        scope.launch {
            try {
                val gId = if (id.isBlank()) "goal_${System.currentTimeMillis()}" else id
                val goal = GoalEntity(
                    id = gId,
                    title = title,
                    description = description.ifBlank { null },
                    goalType = goalType.ifBlank { "PERSONAL" },
                    targetDateMs = if (targetDateMs > 0) targetDateMs else null,
                    progressPercent = progressPercent.coerceIn(0, 100),
                    status = if (progressPercent >= 100) "ACHIEVED" else "ACTIVE"
                )
                db.lifeManagementDao().insertOrUpdateGoal(goal)
                postToJs(callbackId, BridgeResponse.ok("Goal saved").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to save goal").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun updateGoalProgress(id: String, percent: Int, callbackId: String) {
        scope.launch {
            try {
                db.lifeManagementDao().updateGoalProgress(id, percent)
                postToJs(callbackId, BridgeResponse.ok("Goal progress updated").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to update goal").toJsonString())
            }
        }
    }

    // ==========================================
    // PHASE 2: PROJECT INTELLIGENCE
    // ==========================================

    @JavascriptInterface
    fun getProjects(callbackId: String) {
        scope.launch {
            try {
                val projects = db.projectDao().getAllProjects()
                val array = JSONArray()
                for (p in projects) {
                    val milestones = db.projectDao().getMilestonesForProject(p.id)
                    val decisions = db.projectDao().getDecisionsForProject(p.id)

                    val obj = JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("description", p.description ?: "")
                        put("objective", p.objective ?: "")
                        put("status", p.status)
                        put("techStack", JSONArray(p.techStackJson))
                        put("progressPercent", p.progressPercent)
                        put("milestonesCount", milestones.size)
                        put("completedMilestonesCount", milestones.count { it.isCompleted })
                        put("decisionsCount", decisions.size)
                        put("updatedAt", p.updatedAt)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get projects").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun saveProject(id: String, name: String, description: String, objective: String, status: String, techStackJson: String, progressPercent: Int, callbackId: String) {
        scope.launch {
            try {
                val pId = if (id.isBlank()) "proj_${System.currentTimeMillis()}" else id
                val project = ProjectEntity(
                    id = pId,
                    name = name,
                    description = description.ifBlank { null },
                    objective = objective.ifBlank { null },
                    status = status.ifBlank { "ACTIVE" },
                    techStackJson = techStackJson.ifBlank { "[]" },
                    progressPercent = progressPercent.coerceIn(0, 100),
                    updatedAt = System.currentTimeMillis()
                )
                db.projectDao().insertOrUpdateProject(project)

                // Add to timeline
                db.timelineDao().insert(
                    TimelineEventEntity(
                        id = "tl_$pId",
                        timestampMs = System.currentTimeMillis(),
                        eventType = "PROJECT",
                        title = "Project: $name",
                        summary = description.take(100),
                        sourceConnector = "project_module",
                        referenceId = pId,
                        isSignificant = true
                    )
                )

                postToJs(callbackId, BridgeResponse.ok("Project saved").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to save project").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun deleteProject(id: String, callbackId: String) {
        scope.launch {
            try {
                db.projectDao().deleteProject(id)
                postToJs(callbackId, BridgeResponse.ok("Project deleted").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to delete project").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getProjectMilestones(projectId: String, callbackId: String) {
        scope.launch {
            try {
                val milestones = db.projectDao().getMilestonesForProject(projectId)
                val array = JSONArray()
                for (m in milestones) {
                    val obj = JSONObject().apply {
                        put("id", m.id)
                        put("projectId", m.projectId)
                        put("title", m.title)
                        put("targetDateMs", m.targetDateMs ?: JSONObject.NULL)
                        put("isCompleted", m.isCompleted)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get milestones").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun saveProjectMilestone(id: String, projectId: String, title: String, targetDateMs: Long, isCompleted: Boolean, callbackId: String) {
        scope.launch {
            try {
                val mId = if (id.isBlank()) "ms_${System.currentTimeMillis()}" else id
                val milestone = ProjectMilestoneEntity(
                    id = mId,
                    projectId = projectId,
                    title = title,
                    targetDateMs = if (targetDateMs > 0) targetDateMs else null,
                    isCompleted = isCompleted
                )
                db.projectDao().insertOrUpdateMilestone(milestone)
                postToJs(callbackId, BridgeResponse.ok("Milestone saved").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to save milestone").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun toggleMilestone(id: String, isCompleted: Boolean, callbackId: String) {
        scope.launch {
            try {
                db.projectDao().setMilestoneCompleted(id, isCompleted)
                postToJs(callbackId, BridgeResponse.ok("Milestone updated").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to toggle milestone").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getProjectDecisions(projectId: String, callbackId: String) {
        scope.launch {
            try {
                val decisions = db.projectDao().getDecisionsForProject(projectId)
                val array = JSONArray()
                for (d in decisions) {
                    val obj = JSONObject().apply {
                        put("id", d.id)
                        put("projectId", d.projectId)
                        put("title", d.title)
                        put("rationale", d.rationale)
                        put("status", d.status)
                        put("decidedAt", d.decidedAt)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get decisions").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun saveProjectDecision(id: String, projectId: String, title: String, rationale: String, status: String, callbackId: String) {
        scope.launch {
            try {
                val dId = if (id.isBlank()) "dec_${System.currentTimeMillis()}" else id
                val decision = ProjectDecisionEntity(
                    id = dId,
                    projectId = projectId,
                    title = title,
                    rationale = rationale,
                    status = status.ifBlank { "ACCEPTED" }
                )
                db.projectDao().insertOrUpdateDecision(decision)
                postToJs(callbackId, BridgeResponse.ok("Decision recorded").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to record decision").toJsonString())
            }
        }
    }

    // ==========================================
    // PHASE 2: CHRONOLOGICAL PERSONAL TIMELINE
    // ==========================================

    @JavascriptInterface
    fun getTimeline(limit: Int, offset: Int, filterType: String, callbackId: String) {
        scope.launch {
            try {
                val events = if (filterType.isBlank() || filterType == "ALL") {
                    db.timelineDao().getTimeline(limit.coerceAtLeast(10), offset)
                } else {
                    db.timelineDao().getTimelineByType(filterType, limit.coerceAtLeast(10))
                }
                val array = JSONArray()
                for (ev in events) {
                    val obj = JSONObject().apply {
                        put("id", ev.id)
                        put("timestampMs", ev.timestampMs)
                        put("eventType", ev.eventType)
                        put("title", ev.title)
                        put("summary", ev.summary ?: "")
                        put("sourceConnector", ev.sourceConnector)
                        put("referenceId", ev.referenceId ?: JSONObject.NULL)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get timeline").toJsonString())
            }
        }
    }

    // ==========================================
    // PHASE 2: UNIVERSAL SEARCH
    // ==========================================

    @JavascriptInterface
    fun searchUniversal(query: String, callbackId: String) {
        scope.launch {
            try {
                val results = searchService.searchAll(query)
                val array = JSONArray()
                for (res in results) {
                    val obj = JSONObject().apply {
                        put("id", res.id)
                        put("type", res.type)
                        put("title", res.title)
                        put("snippet", res.snippet)
                        put("sourceConnector", res.sourceConnector)
                        put("timestampMs", res.timestampMs)
                        put("matchLevel", res.matchLevel)
                    }
                    array.put(obj)
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Search failed").toJsonString())
            }
        }
    }

    // ==========================================
    // PHASE 2: GOOGLE INTEGRATION DIAGNOSTICS
    // ==========================================

    @JavascriptInterface
    fun getGoogleDiagnostics(callbackId: String) {
        scope.launch {
            try {
                val obj = JSONObject().apply {
                    put("packageName", "com.cortex.app")
                    put("keystoreSha1", "27:D8:78:6A:62:87:A3:4D:FF:3E:6F:BF:76:F6:61:F2:42:B8:16:BD")
                    put("oauthRedirectUri", "com.cortex.app://oauth2callback")
                    put("gmailStatus", "AWAITING_GOOGLE_CLOUD_CONFIG")
                    put("driveStatus", "AWAITING_GOOGLE_CLOUD_CONFIG")
                    put("classroomStatus", "AWAITING_GOOGLE_CLOUD_CONFIG")
                    put("calendarStatus", "FUNCTIONAL_VIA_DEVICE_PROVIDER")
                    put("missingRequirements", JSONArray().apply {
                        put("1. Enable Gmail API, Google Drive API, and Classroom API in Google Cloud Console.")
                        put("2. Create an OAuth 2.0 Client ID for Android using package name 'com.cortex.app' and SHA-1 '27:D8:78:6A:62:87:A3:4D:FF:3E:6F:BF:76:F6:61:F2:42:B8:16:BD'.")
                        put("3. Add your personal Google account to the OAuth Consent Screen 'Test Users' list.")
                    })
                }
                postToJs(callbackId, BridgeResponse.ok(obj).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get diagnostics").toJsonString())
            }
        }
    }

    // ==========================================
    // PHASE 3: INTELLIGENCE, REASONING & AUTOMATION
    // ==========================================

    private suspend fun buildDatabaseSnapshot(): JSONObject {
        val snapshot = JSONObject()
        val memories = db.memoryDao().getAllMemories()
        val memArr = JSONArray()
        for (m in memories) {
            memArr.put(JSONObject().apply {
                put("id", m.id)
                put("content", m.content)
                put("category", m.category)
                put("is_pinned", m.isPinned)
                put("tags", try { JSONArray(m.tagsJson) } catch (e: Exception) { JSONArray() })
            })
        }
        snapshot.put("memories", memArr)

        val notes = db.knowledgeDao().getAllNotes()
        val notesArr = JSONArray()
        for (n in notes) {
            notesArr.put(JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("content", n.content)
            })
        }
        snapshot.put("notes", notesArr)

        val docs = db.knowledgeDao().getAllDocuments()
        val docsArr = JSONArray()
        for (d in docs) {
            docsArr.put(JSONObject().apply {
                put("id", d.id)
                put("title", d.title)
                put("extracted_text", d.extractedText)
            })
        }
        snapshot.put("documents", docsArr)

        val tasks = db.lifeManagementDao().getAllTasks()
        val tasksArr = JSONArray()
        for (t in tasks) {
            tasksArr.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("description", t.description ?: "")
                put("priority", t.priority)
                put("status", t.status)
                put("category", t.category)
                put("dueDateMs", t.dueDateMs ?: 0L)
            })
        }
        snapshot.put("tasks", tasksArr)

        val projects = db.projectDao().getAllProjects()
        val projsArr = JSONArray()
        for (p in projects) {
            projsArr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("description", p.description ?: "")
                put("objective", p.objective ?: "")
                put("status", p.status)
                put("progress_percent", p.progressPercent)
            })
        }
        snapshot.put("projects", projsArr)

        val timetable = db.academicDao().getFullTimetable()
        val ttArr = JSONArray()
        for (tt in timetable) {
            ttArr.put(JSONObject().apply {
                put("id", tt.id)
                put("day_of_week", tt.dayOfWeek)
                put("subject_id", tt.subjectId)
                put("start_time", tt.startTime)
                put("end_time", tt.endTime)
                put("room", tt.room ?: "")
            })
        }
        snapshot.put("timetable", ttArr)

        val subjects = db.academicDao().getAllSubjects()
        val subjArr = JSONArray()
        for (s in subjects) {
            subjArr.put(JSONObject().apply {
                put("id", s.id)
                put("code", s.subjectCode)
                put("name", s.subjectName)
                put("faculty", s.facultyName ?: "")
                put("credits", s.credits)
            })
        }
        snapshot.put("subjects", subjArr)

        val assignments = db.academicDao().getAllAssignments()
        val asgnArr = JSONArray()
        for (a in assignments) {
            asgnArr.put(JSONObject().apply {
                put("id", a.id)
                put("title", a.title)
                put("description", a.description ?: "")
                put("dueDateMs", a.dueDateMs)
                put("priority", "MEDIUM")
                put("status", a.status)
            })
        }
        snapshot.put("assignments", asgnArr)

        val exams = db.academicDao().getAllExaminations()
        val examArr = JSONArray()
        for (e in exams) {
            examArr.put(JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("exam_date_ms", e.examDateMs)
                put("duration", e.durationMinutes)
                put("room", e.room ?: "")
                put("syllabus", e.syllabusCoverage ?: "")
            })
        }
        snapshot.put("examinations", examArr)

        val goals = db.lifeManagementDao().getAllGoals()
        val goalArr = JSONArray()
        for (g in goals) {
            goalArr.put(JSONObject().apply {
                put("id", g.id)
                put("title", g.title)
                put("description", g.description ?: "")
                put("goal_type", g.goalType)
                put("progress_percent", g.progressPercent)
                put("status", g.status)
            })
        }
        snapshot.put("goals", goalArr)

        return snapshot
    }

    @JavascriptInterface
    fun askCortex(conversationId: String, query: String, callbackId: String) {
        scope.launch {
            try {
                var convId = conversationId
                if (convId.isBlank() || convId == "null") {
                    convId = "conv_${System.currentTimeMillis()}"
                    val title = if (query.length > 30) query.take(30) + "..." else query
                    db.conversationDao().insertConversation(
                        ConversationEntity(id = convId, title = title)
                    )
                }

                // 1. Record User Message
                val userMsgId = "msg_${System.currentTimeMillis()}_u"
                db.conversationDao().insertMessage(
                    ConversationMessageEntity(
                        id = userMsgId,
                        conversationId = convId,
                        sender = "USER",
                        text = query,
                        createdAt = System.currentTimeMillis()
                    )
                )

                // 2. Fetch History & Database Snapshot
                val history = db.conversationDao().getMessages(convId)
                val histArr = JSONArray()
                for (h in history) {
                    histArr.put(JSONObject().apply {
                        put("sender", h.sender)
                        put("text", h.text)
                        put("intent", h.intent)
                        put("referenced_project", h.referencedProject ?: JSONObject.NULL)
                    })
                }

                val snapshot = buildDatabaseSnapshot()

                val pyInput = JSONObject().apply {
                    put("query", query)
                    put("conversation_history", histArr)
                    put("database_snapshot", snapshot)
                }

                // 3. Call Python Intelligence Assistant
                val pyRespStr = pythonBridge.askCortex(pyInput.toString())
                val pyResp = JSONObject(pyRespStr)

                if (pyResp.optBoolean("success", false)) {
                    val data = pyResp.getJSONObject("data")
                    val answerText = data.optString("answer", "")
                    val intent = data.optString("intent", "GENERAL")
                    val refProj = if (data.has("referenced_project") && !data.isNull("referenced_project")) data.getString("referenced_project") else null
                    val citations = data.optJSONArray("citations") ?: JSONArray()
                    val proposedActions = data.optJSONArray("proposed_actions") ?: JSONArray()

                    // Record Assistant Message
                    val asstMsgId = "msg_${System.currentTimeMillis()}_a"
                    db.conversationDao().insertMessage(
                        ConversationMessageEntity(
                            id = asstMsgId,
                            conversationId = convId,
                            sender = "CORTEX",
                            text = answerText,
                            sourcesJson = citations.toString(),
                            intent = intent,
                            referencedProject = refProj,
                            createdAt = System.currentTimeMillis()
                        )
                    )

                    // Insert proposed actions if any
                    for (i in 0 until proposedActions.length()) {
                        val pa = proposedActions.getJSONObject(i)
                        db.proposedActionDao().insertAction(
                            ProposedActionEntity(
                                id = pa.optString("id", "act_${System.currentTimeMillis()}_$i"),
                                actionType = pa.optString("action_type", "CREATE_TASK"),
                                title = pa.optString("title", "Proposed Action"),
                                description = pa.optString("description", ""),
                                payloadJson = pa.optJSONObject("payload")?.toString() ?: "{}",
                                status = "PROPOSED",
                                sourceContext = "Ask Cortex"
                            )
                        )
                    }

                    // Return clean response with conversationId
                    val resultObj = JSONObject().apply {
                        put("conversationId", convId)
                        put("answer", answerText as Any)
                        put("intent", intent as Any)
                        put("citations", citations as Any)
                        put("confidence", data.optDouble("confidence", 0.9))
                        put("has_personal_records", data.optBoolean("has_personal_records", false))
                        put("provider", (data.optString("provider", "LOCAL_AI")) as Any)
                        put("proposed_actions", proposedActions as Any)
                    }
                    postToJs(callbackId, BridgeResponse.ok(resultObj).toJsonString())
                } else {
                    val err = pyResp.optString("error", "AI reasoning error")
                    postToJs(callbackId, BridgeResponse.err(err).toJsonString())
                }
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Assistant error").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getConversations(callbackId: String) {
        scope.launch {
            try {
                val list = db.conversationDao().getAllConversations()
                val array = JSONArray()
                for (c in list) {
                    array.put(JSONObject().apply {
                        put("id", c.id)
                        put("title", c.title)
                        put("createdAt", c.createdAt)
                        put("updatedAt", c.updatedAt)
                    })
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get conversations").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getConversationMessages(conversationId: String, callbackId: String) {
        scope.launch {
            try {
                val messages = db.conversationDao().getMessages(conversationId)
                val array = JSONArray()
                for (m in messages) {
                    array.put(JSONObject().apply {
                        put("id", m.id)
                        put("conversationId", m.conversationId)
                        put("sender", m.sender)
                        put("text", m.text)
                        put("sourcesJson", m.sourcesJson)
                        put("intent", m.intent)
                        put("createdAt", m.createdAt)
                    })
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get messages").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun deleteConversation(conversationId: String, callbackId: String) {
        scope.launch {
            try {
                db.conversationDao().deleteMessages(conversationId)
                db.conversationDao().deleteConversation(conversationId)
                postToJs(callbackId, BridgeResponse.ok("Conversation deleted").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to delete conversation").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun generateBriefing(briefingType: String, callbackId: String) {
        scope.launch {
            try {
                val snapshot = buildDatabaseSnapshot()
                snapshot.put("type", briefingType)
                val pyRespStr = pythonBridge.generateBriefing(snapshot.toString())
                val pyResp = JSONObject(pyRespStr)
                if (pyResp.optBoolean("success", false)) {
                    postToJs(callbackId, BridgeResponse.ok(pyResp.getJSONObject("data")).toJsonString())
                } else {
                    postToJs(callbackId, BridgeResponse.err(pyResp.optString("error", "Briefing failed")).toJsonString())
                }
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Briefing error").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun generateStudyPlan(days: Int, dailyHours: Double, callbackId: String) {
        scope.launch {
            try {
                val snapshot = buildDatabaseSnapshot()
                val pyInput = JSONObject().apply {
                    put("days", days)
                    put("daily_hours", dailyHours)
                    put("examinations", snapshot.optJSONArray("examinations") ?: JSONArray())
                    put("assignments", snapshot.optJSONArray("assignments") ?: JSONArray())
                    put("subjects", snapshot.optJSONArray("subjects") ?: JSONArray())
                }
                val pyRespStr = pythonBridge.generateStudyPlan(pyInput.toString())
                val pyResp = JSONObject(pyRespStr)
                if (pyResp.optBoolean("success", false)) {
                    postToJs(callbackId, BridgeResponse.ok(pyResp.getJSONObject("data")).toJsonString())
                } else {
                    postToJs(callbackId, BridgeResponse.err(pyResp.optString("error", "Study plan generation failed")).toJsonString())
                }
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Study plan error").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun generateDailyPlan(callbackId: String) {
        scope.launch {
            try {
                val snapshot = buildDatabaseSnapshot()
                val pyInput = JSONObject().apply {
                    put("tasks", snapshot.optJSONArray("tasks") ?: JSONArray())
                    put("timetable", snapshot.optJSONArray("timetable") ?: JSONArray())
                }
                val pyRespStr = pythonBridge.generateDailyPlan(pyInput.toString())
                val pyResp = JSONObject(pyRespStr)
                if (pyResp.optBoolean("success", false)) {
                    postToJs(callbackId, BridgeResponse.ok(pyResp.getJSONObject("data")).toJsonString())
                } else {
                    postToJs(callbackId, BridgeResponse.err(pyResp.optString("error", "Daily plan generation failed")).toJsonString())
                }
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Daily plan error").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun rescheduleTask(taskId: String, deferDays: Int, callbackId: String) {
        scope.launch {
            try {
                val tasks = db.lifeManagementDao().getAllTasks()
                val tasksArr = JSONArray()
                for (t in tasks) {
                    tasksArr.put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("dueDateMs", t.dueDateMs ?: 0L)
                    })
                }
                val pyInput = JSONObject().apply {
                    put("task_id", taskId)
                    put("tasks", tasksArr)
                    put("defer_days", deferDays)
                }
                val pyRespStr = pythonBridge.rescheduleTask(pyInput.toString())
                val pyResp = JSONObject(pyRespStr)
                if (pyResp.optBoolean("success", false)) {
                    val data = pyResp.getJSONObject("data")
                    // If successfully proposed, insert into proposed_actions
                    val actionId = "act_resched_${System.currentTimeMillis()}"
                    db.proposedActionDao().insertAction(
                        ProposedActionEntity(
                            id = actionId,
                            actionType = "RESCHEDULE_TASK",
                            title = "Reschedule Task: ${data.optString("title")}",
                            description = data.optString("message"),
                            payloadJson = JSONObject().apply {
                                put("task_id", taskId)
                                put("new_due_ms", data.optLong("proposed_due_ms"))
                            }.toString(),
                            status = "PROPOSED",
                            sourceContext = "Planner Assistant"
                        )
                    )
                    postToJs(callbackId, BridgeResponse.ok(data).toJsonString())
                } else {
                    postToJs(callbackId, BridgeResponse.err(pyResp.optString("error", "Reschedule failed")).toJsonString())
                }
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Reschedule error").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun evaluateDecision(statement: String, alternativesJson: String, callbackId: String) {
        scope.launch {
            try {
                val alts = try { JSONArray(alternativesJson) } catch (e: Exception) { JSONArray() }
                val snapshot = buildDatabaseSnapshot()
                val pyInput = JSONObject().apply {
                    put("statement", statement)
                    put("alternatives", alts)
                    put("memories", snapshot.optJSONArray("memories") ?: JSONArray())
                    put("notes", snapshot.optJSONArray("notes") ?: JSONArray())
                }
                val pyRespStr = pythonBridge.evaluateDecision(pyInput.toString())
                val pyResp = JSONObject(pyRespStr)
                if (pyResp.optBoolean("success", false)) {
                    val data = pyResp.getJSONObject("data")
                    if (data.optBoolean("error", false)) {
                        postToJs(callbackId, BridgeResponse.err(data.optString("message")).toJsonString())
                        return@launch
                    }

                    // Save into decision_records
                    val decId = "dec_${System.currentTimeMillis()}"
                    db.decisionRecordDao().insertDecision(
                        DecisionRecordEntity(
                            id = decId,
                            statement = statement,
                            alternativesJson = alternativesJson,
                            advantagesJson = data.optJSONArray("4_advantages")?.toString() ?: "[]",
                            disadvantagesJson = data.optJSONArray("5_disadvantages")?.toString() ?: "[]",
                            risksJson = data.optJSONArray("6_risks")?.toString() ?: "[]",
                            unknownInfoJson = data.optJSONArray("7_unknown_information")?.toString() ?: "[]",
                            assumptionsJson = data.optJSONArray("8_assumptions")?.toString() ?: "[]",
                            recommendation = data.optString("9_recommendation"),
                            conditionsJson = data.optJSONArray("10_conditions_that_change_recommendation")?.toString() ?: "[]",
                            status = "EVALUATED"
                        )
                    )
                    postToJs(callbackId, BridgeResponse.ok(data).toJsonString())
                } else {
                    postToJs(callbackId, BridgeResponse.err(pyResp.optString("error", "Evaluation failed")).toJsonString())
                }
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Decision evaluation error").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getDecisions(callbackId: String) {
        scope.launch {
            try {
                val list = db.decisionRecordDao().getAllDecisions()
                val array = JSONArray()
                for (d in list) {
                    array.put(JSONObject().apply {
                        put("id", d.id)
                        put("statement", d.statement)
                        put("recommendation", d.recommendation)
                        put("status", d.status)
                        put("createdAt", d.createdAt)
                    })
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get decisions").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun deleteDecision(decisionId: String, callbackId: String) {
        scope.launch {
            try {
                db.decisionRecordDao().deleteDecision(decisionId)
                postToJs(callbackId, BridgeResponse.ok("Decision deleted").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to delete decision").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getProposedActions(callbackId: String) {
        scope.launch {
            try {
                val list = db.proposedActionDao().getAllActions()
                val array = JSONArray()
                for (a in list) {
                    array.put(JSONObject().apply {
                        put("id", a.id)
                        put("actionType", a.actionType)
                        put("title", a.title)
                        put("description", a.description)
                        put("payloadJson", a.payloadJson)
                        put("status", a.status)
                        put("createdAt", a.createdAt)
                        put("executedAt", a.executedAt ?: JSONObject.NULL)
                    })
                }
                postToJs(callbackId, BridgeResponse.ok(array).toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to get proposed actions").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun approveAction(actionId: String, callbackId: String) {
        scope.launch {
            try {
                val actions = db.proposedActionDao().getAllActions()
                val target = actions.firstOrNull { it.id == actionId }
                if (target == null) {
                    postToJs(callbackId, BridgeResponse.err("Action $actionId not found").toJsonString())
                    return@launch
                }

                // Execute action payload safely into local database
                val payload = JSONObject(target.payloadJson)
                when (target.actionType) {
                    "CREATE_TASK" -> {
                        val taskId = "task_${System.currentTimeMillis()}"
                        db.lifeManagementDao().insertOrUpdateTask(
                            TaskEntity(
                                id = taskId,
                                title = payload.optString("title", "New Task"),
                                description = payload.optString("description", ""),
                                priority = payload.optString("priority", "MEDIUM"),
                                status = "PENDING",
                                category = payload.optString("category", "PERSONAL"),
                                dueDateMs = payload.optLong("due_date_ms", System.currentTimeMillis() + 86400000L)
                            )
                        )
                    }
                    "RESCHEDULE_TASK" -> {
                        val taskId = payload.optString("task_id")
                        val newDue = payload.optLong("new_due_ms")
                        val t = db.lifeManagementDao().getAllTasks().firstOrNull { it.id == taskId }
                        if (t != null) {
                            db.lifeManagementDao().insertOrUpdateTask(t.copy(dueDateMs = newDue))
                        }
                    }
                    "SCHEDULE_STUDY_BLOCK" -> {
                        val subj = payload.optString("subject", "Study Block")
                        db.lifeManagementDao().insertOrUpdateTask(
                            TaskEntity(
                                id = "task_study_${System.currentTimeMillis()}",
                                title = "Study Session: $subj",
                                description = payload.optString("topic", "Core Review"),
                                priority = "HIGH",
                                status = "PENDING",
                                category = "ACADEMIC",
                                dueDateMs = System.currentTimeMillis() + 86400000L
                            )
                        )
                    }
                }

                // Mark action APPROVED
                db.proposedActionDao().updateActionStatus(actionId, "APPROVED", System.currentTimeMillis())
                postToJs(callbackId, BridgeResponse.ok("Action approved and executed safely").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to approve action").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun rejectAction(actionId: String, callbackId: String) {
        scope.launch {
            try {
                db.proposedActionDao().updateActionStatus(actionId, "REJECTED", System.currentTimeMillis())
                postToJs(callbackId, BridgeResponse.ok("Action rejected").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Failed to reject action").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getProactiveAlerts(callbackId: String) {
        scope.launch {
            try {
                val snapshot = buildDatabaseSnapshot()
                val pyRespStr = pythonBridge.getProactiveAlerts(snapshot.toString())
                val pyResp = JSONObject(pyRespStr)
                if (pyResp.optBoolean("success", false)) {
                    val alertsArr = pyResp.getJSONArray("data")
                    // Persist new alerts into proactive_alerts
                    for (i in 0 until alertsArr.length()) {
                        val a = alertsArr.getJSONObject(i)
                        db.proactiveAlertDao().insertAlert(
                            ProactiveAlertEntity(
                                id = "alert_${System.currentTimeMillis()}_$i",
                                alertKey = a.optString("key", "alert_$i"),
                                alertType = a.optString("type", "NOTICE"),
                                title = a.optString("title", "Notice"),
                                description = a.optString("description", ""),
                                priority = a.optString("priority", "MEDIUM"),
                                referenceId = if (a.has("reference_id") && !a.isNull("reference_id")) a.getString("reference_id") else null,
                                actionSuggestion = if (a.has("action_suggestion") && !a.isNull("action_suggestion")) a.getString("action_suggestion") else null
                            )
                        )
                    }
                    postToJs(callbackId, BridgeResponse.ok(alertsArr).toJsonString())
                } else {
                    postToJs(callbackId, BridgeResponse.err(pyResp.optString("error", "Alert check failed")).toJsonString())
                }
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Alert check error").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun dismissProactiveAlert(alertId: String, callbackId: String) {
        scope.launch {
            try {
                db.proactiveAlertDao().dismissAlert(alertId)
                postToJs(callbackId, BridgeResponse.ok("Alert dismissed").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Dismiss failed").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun getLocalAiStatus(callbackId: String) {
        scope.launch {
            try {
                val pyRespStr = pythonBridge.getLocalAiStatus("{}")
                val pyResp = JSONObject(pyRespStr)
                if (pyResp.optBoolean("success", false)) {
                    postToJs(callbackId, BridgeResponse.ok(pyResp.getJSONObject("data")).toJsonString())
                } else {
                    postToJs(callbackId, BridgeResponse.err(pyResp.optString("error", "Failed to get AI status")).toJsonString())
                }
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Local AI status error").toJsonString())
            }
        }
    }

    @JavascriptInterface
    fun reasonMemories(action: String, targetMemoryJson: String, callbackId: String) {
        scope.launch {
            try {
                val snapshot = buildDatabaseSnapshot()
                val pyInput = JSONObject().apply {
                    put("action", action)
                    put("memories", snapshot.optJSONArray("memories") ?: JSONArray())
                    put("target_memory", try { JSONObject(targetMemoryJson) } catch (e: Exception) { JSONObject() })
                }
                val pyRespStr = pythonBridge.reasonMemories(pyInput.toString())
                val pyResp = JSONObject(pyRespStr)
                if (pyResp.optBoolean("success", false)) {
                    postToJs(callbackId, BridgeResponse.ok(pyResp.get("data")).toJsonString())
                } else {
                    postToJs(callbackId, BridgeResponse.err(pyResp.optString("error", "Memory reasoning failed")).toJsonString())
                }
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Memory reasoning error").toJsonString())
            }
        }
    }

    // ==========================================
    // SYSTEM & NUCLEAR WIPE
    // ==========================================

    @JavascriptInterface
    fun wipeAllData(callbackId: String) {
        scope.launch {
            try {
                db.collectedRecordDao().deleteAll()
                db.syncHistoryDao().deleteAll()
                db.timelineDao().deleteAll()
                db.conversationDao().deleteMessages("all")
                postToJs(callbackId, BridgeResponse.ok("All data wiped successfully").toJsonString())
            } catch (e: Exception) {
                postToJs(callbackId, BridgeResponse.err(e.message ?: "Wipe failed").toJsonString())
            }
        }
    }
}
