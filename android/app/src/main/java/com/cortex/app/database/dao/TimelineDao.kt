package com.cortex.app.database.dao

import androidx.room.*
import com.cortex.app.database.entities.TimelineEventEntity

@Dao
interface TimelineDao {
    @Query("SELECT * FROM timeline_events ORDER BY timestampMs DESC LIMIT :limit OFFSET :offset")
    suspend fun getTimeline(limit: Int = 100, offset: Int = 0): List<TimelineEventEntity>

    @Query("SELECT * FROM timeline_events WHERE eventType = :type ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun getTimelineByType(type: String, limit: Int = 50): List<TimelineEventEntity>

    @Query("SELECT * FROM timeline_events WHERE timestampMs >= :startTimeMs AND timestampMs <= :endTimeMs ORDER BY timestampMs DESC")
    suspend fun getTimelineByRange(startTimeMs: Long, endTimeMs: Long): List<TimelineEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: TimelineEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<TimelineEventEntity>)

    @Query("DELETE FROM timeline_events WHERE id = :id")
    suspend fun deleteEvent(id: String): Int

    @Query("DELETE FROM timeline_events")
    suspend fun deleteAll(): Int
}
