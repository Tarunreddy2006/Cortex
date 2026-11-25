package com.cortex.app.connectors.classroom

import android.content.Context
import com.cortex.app.database.CortexDatabase
import com.cortex.app.database.entities.CollectedRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Dedicated Read-Only Google Classroom Connector.
 * Scopes:
 * - https://www.googleapis.com/auth/classroom.courses.readonly
 * - https://www.googleapis.com/auth/classroom.coursework.me.readonly
 * - https://www.googleapis.com/auth/classroom.announcements.readonly
 * Retrieves academic courses, assignments, and announcements.
 */
class GoogleClassroomConnector(private val context: Context) {

    private val db = CortexDatabase.getInstance(context)

    companion object {
        const val CLASSROOM_COURSES_READONLY = "https://www.googleapis.com/auth/classroom.courses.readonly"
        const val CLASSROOM_COURSEWORK_READONLY = "https://www.googleapis.com/auth/classroom.coursework.me.readonly"
        const val CLASSROOM_ANNOUNCEMENTS_READONLY = "https://www.googleapis.com/auth/classroom.announcements.readonly"
    }

    suspend fun saveClassroomCourse(
        courseId: String,
        courseName: String,
        section: String?,
        descriptionHeading: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val recordId = "classroom_course_$courseId"
        val payload = JSONObject().apply {
            put("course_id", courseId)
            put("course_name", courseName)
            put("section", section)
            put("description_heading", descriptionHeading)
        }

        db.collectedRecordDao().insert(
            CollectedRecordEntity(
                id = recordId,
                sourceConnector = "google_classroom",
                packageName = "com.google.android.apps.classroom",
                recordType = "ACADEMIC_COURSE",
                title = courseName,
                summary = "Section: ${section ?: "General"} - ${descriptionHeading ?: ""}",
                payloadJson = payload.toString(),
                dedupHash = recordId,
                timestampMs = System.currentTimeMillis()
            )
        )
        true
    }

    suspend fun saveCoursework(
        courseworkId: String,
        courseId: String,
        courseName: String,
        title: String,
        dueDateMs: Long?,
        description: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val recordId = "classroom_work_$courseworkId"
        val payload = JSONObject().apply {
            put("coursework_id", courseworkId)
            put("course_id", courseId)
            put("course_name", courseName)
            put("title", title)
            put("due_date_ms", dueDateMs)
            put("description", description)
        }

        db.collectedRecordDao().insert(
            CollectedRecordEntity(
                id = recordId,
                sourceConnector = "google_classroom",
                packageName = "com.google.android.apps.classroom",
                recordType = "ACADEMIC_ASSIGNMENT",
                title = "$courseName: $title",
                summary = "Due: ${dueDateMs?.let { java.util.Date(it).toString() } ?: "No deadline"}",
                payloadJson = payload.toString(),
                dedupHash = recordId,
                timestampMs = System.currentTimeMillis()
            )
        )
        true
    }
}
