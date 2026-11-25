package com.cortex.app.connectors.calendar

import android.Manifest
import android.content.Context
import android.provider.CalendarContract
import com.cortex.app.database.CortexDatabase
import com.cortex.app.database.entities.CollectedRecordEntity
import com.cortex.app.permissions.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class CalendarEventItem(
    val eventId: String,
    val calendarId: String,
    val calendarName: String,
    val title: String,
    val description: String?,
    val location: String?,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val isAllDay: Boolean,
    val rrule: String?
)

class CalendarConnector(private val context: Context) {

    private val db = CortexDatabase.getInstance(context)

    suspend fun syncUpcomingEvents(lookaheadDays: Int = 30): List<CalendarEventItem> = withContext(Dispatchers.IO) {
        if (!PermissionManager.isPermissionGranted(context, Manifest.permission.READ_CALENDAR)) {
            return@withContext emptyList()
        }

        val events = mutableListOf<CalendarEventItem>()
        val now = System.currentTimeMillis()
        val future = now + (lookaheadDays * 24L * 60 * 60 * 1000)

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.RRULE
        )

        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val selectionArgs = arrayOf(now.toString(), future.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val calIdIdx = it.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID)
                val calNameIdx = it.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_DISPLAY_NAME)
                val titleIdx = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val descIdx = it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
                val locIdx = it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
                val startIdx = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val endIdx = it.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                val allDayIdx = it.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
                val rruleIdx = it.getColumnIndexOrThrow(CalendarContract.Events.RRULE)

                while (it.moveToNext()) {
                    val eventId = it.getString(idIdx) ?: continue
                    val title = it.getString(titleIdx) ?: "Untitled Event"
                    val startTime = it.getLong(startIdx)
                    val endTime = if (it.isNull(endIdx)) startTime else it.getLong(endIdx)

                    val item = CalendarEventItem(
                        eventId = eventId,
                        calendarId = it.getString(calIdIdx) ?: "",
                        calendarName = it.getString(calNameIdx) ?: "Default Calendar",
                        title = title,
                        description = it.getString(descIdx),
                        location = it.getString(locIdx),
                        startTimeMs = startTime,
                        endTimeMs = endTime,
                        isAllDay = it.getInt(allDayIdx) == 1,
                        rrule = it.getString(rruleIdx)
                    )
                    events.add(item)

                    // Persist to Room
                    val payload = JSONObject().apply {
                        put("id", "cal_$eventId")
                        put("title", item.title)
                        put("calendar_name", item.calendarName)
                        put("start_time_ms", item.startTimeMs)
                        put("end_time_ms", item.endTimeMs)
                        put("location", item.location)
                        put("is_all_day", item.isAllDay)
                    }

                    db.collectedRecordDao().insert(
                        CollectedRecordEntity(
                            id = "cal_$eventId",
                            sourceConnector = "calendar",
                            packageName = null,
                            recordType = "CALENDAR_EVENT",
                            title = item.title,
                            summary = "${item.calendarName}: ${item.location ?: "No location"}",
                            payloadJson = payload.toString(),
                            dedupHash = null,
                            timestampMs = item.startTimeMs
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        events
    }
}
