package com.cortex.app.database.dao

import androidx.room.*
import com.cortex.app.database.entities.*

@Dao
interface AcademicDao {
    // Profile
    @Query("SELECT * FROM academic_profile LIMIT 1")
    suspend fun getProfile(): AcademicProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: AcademicProfileEntity)

    // Subjects
    @Query("SELECT * FROM academic_subjects ORDER BY subjectName ASC")
    suspend fun getAllSubjects(): List<AcademicSubjectEntity>

    @Query("SELECT * FROM academic_subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): AcademicSubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSubject(subject: AcademicSubjectEntity)

    @Query("DELETE FROM academic_subjects WHERE id = :id")
    suspend fun deleteSubject(id: String): Int

    // Timetables
    @Query("SELECT * FROM timetables WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    suspend fun getTimetableForDay(dayOfWeek: Int): List<TimetableEntity>

    @Query("SELECT * FROM timetables ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getFullTimetable(): List<TimetableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTimetable(entry: TimetableEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTimetableAll(entries: List<TimetableEntity>)

    @Query("DELETE FROM timetables WHERE id = :id")
    suspend fun deleteTimetableEntry(id: String): Int

    // Assignments
    @Query("SELECT * FROM academic_assignments ORDER BY dueDateMs ASC")
    suspend fun getAllAssignments(): List<AcademicAssignmentEntity>

    @Query("SELECT * FROM academic_assignments WHERE status = 'PENDING' ORDER BY dueDateMs ASC")
    suspend fun getPendingAssignments(): List<AcademicAssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAssignment(assignment: AcademicAssignmentEntity)

    @Query("UPDATE academic_assignments SET status = :status WHERE id = :id")
    suspend fun updateAssignmentStatus(id: String, status: String)

    @Query("DELETE FROM academic_assignments WHERE id = :id")
    suspend fun deleteAssignment(id: String): Int

    // Examinations
    @Query("SELECT * FROM academic_examinations ORDER BY examDateMs ASC")
    suspend fun getAllExaminations(): List<AcademicExaminationEntity>

    @Query("SELECT * FROM academic_examinations WHERE examDateMs >= :nowMs ORDER BY examDateMs ASC")
    suspend fun getUpcomingExaminations(nowMs: Long = System.currentTimeMillis()): List<AcademicExaminationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateExamination(exam: AcademicExaminationEntity)

    @Query("DELETE FROM academic_examinations WHERE id = :id")
    suspend fun deleteExamination(id: String): Int
}
