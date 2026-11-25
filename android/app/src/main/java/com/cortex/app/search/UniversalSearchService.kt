package com.cortex.app.search

import android.content.Context
import com.cortex.app.database.CortexDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class SearchResultItem(
    val id: String,
    val type: String, // MEMORY, DOCUMENT, NOTE, ACADEMIC, PROJECT, TASK
    val title: String,
    val snippet: String,
    val sourceConnector: String,
    val timestampMs: Long,
    val matchLevel: String = "KEYWORD" // KEYWORD, FULL_TEXT, SEMANTIC
)

class UniversalSearchService(private val context: Context) {

    private val db = CortexDatabase.getInstance(context)

    suspend fun searchAll(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<SearchResultItem>()

        // 1. Search Memories
        val memories = db.memoryDao().searchMemories(q)
        for (m in memories) {
            results.add(
                SearchResultItem(
                    id = m.id,
                    type = "MEMORY",
                    title = "[${m.category}] Memory",
                    snippet = m.content,
                    sourceConnector = m.sourceConnector,
                    timestampMs = m.createdAt,
                    matchLevel = "KEYWORD"
                )
            )
        }

        // 2. Search Notes
        val notes = db.knowledgeDao().searchNotes(q)
        for (n in notes) {
            results.add(
                SearchResultItem(
                    id = n.id,
                    type = "NOTE",
                    title = n.title,
                    snippet = n.content.take(150),
                    sourceConnector = "notes",
                    timestampMs = n.updatedAt,
                    matchLevel = "FULL_TEXT"
                )
            )
        }

        // 3. Search Documents
        val docs = db.knowledgeDao().searchDocuments(q)
        for (d in docs) {
            results.add(
                SearchResultItem(
                    id = d.id,
                    type = "DOCUMENT",
                    title = d.title,
                    snippet = (d.summary ?: d.extractedText ?: d.fileName).take(150),
                    sourceConnector = d.sourceConnector,
                    timestampMs = d.importedAt,
                    matchLevel = "FULL_TEXT"
                )
            )
        }

        // 4. Search Tasks
        val tasks = db.lifeManagementDao().getAllTasks().filter {
            it.title.lowercase().contains(q) || (it.description?.lowercase()?.contains(q) == true)
        }
        for (t in tasks) {
            results.add(
                SearchResultItem(
                    id = t.id,
                    type = "TASK",
                    title = "Task: ${t.title}",
                    snippet = "${t.priority} Priority - Status: ${t.status}",
                    sourceConnector = "tasks",
                    timestampMs = t.createdAt,
                    matchLevel = "KEYWORD"
                )
            )
        }

        // 5. Search Projects
        val projects = db.projectDao().getAllProjects().filter {
            it.name.lowercase().contains(q) || (it.description?.lowercase()?.contains(q) == true)
        }
        for (p in projects) {
            results.add(
                SearchResultItem(
                    id = p.id,
                    type = "PROJECT",
                    title = "Project: ${p.name}",
                    snippet = p.description ?: (p.objective ?: "Active Project"),
                    sourceConnector = "projects",
                    timestampMs = p.updatedAt,
                    matchLevel = "KEYWORD"
                )
            )
        }

        // 6. Search Academic Subjects & Assignments
        val subjects = db.academicDao().getAllSubjects().filter {
            it.subjectName.lowercase().contains(q) || it.subjectCode.lowercase().contains(q)
        }
        for (s in subjects) {
            results.add(
                SearchResultItem(
                    id = s.id,
                    type = "ACADEMIC",
                    title = "${s.subjectCode}: ${s.subjectName}",
                    snippet = "Faculty: ${s.facultyName ?: "Not specified"} (${s.credits} Credits)",
                    sourceConnector = "academic_profile",
                    timestampMs = System.currentTimeMillis(),
                    matchLevel = "KEYWORD"
                )
            )
        }

        val assignments = db.academicDao().getAllAssignments().filter {
            it.title.lowercase().contains(q) || (it.description?.lowercase()?.contains(q) == true)
        }
        for (a in assignments) {
            results.add(
                SearchResultItem(
                    id = a.id,
                    type = "ACADEMIC",
                    title = "Assignment: ${a.title}",
                    snippet = a.description ?: "Status: ${a.status}",
                    sourceConnector = a.sourceConnector,
                    timestampMs = a.dueDateMs,
                    matchLevel = "KEYWORD"
                )
            )
        }

        // Sort: Most recent first
        results.sortedByDescending { it.timestampMs }
    }
}
