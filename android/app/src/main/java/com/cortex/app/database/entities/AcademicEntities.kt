package com.cortex.app.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "academic_profile")
data class AcademicProfileEntity(
    @PrimaryKey val id: String = "default_academic_profile",
    val institution: String,
    val degree: String,
    val department: String,
    val semester: Int = 1,
    val academicYear: String = "2026-2027",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "academic_subjects")
data class AcademicSubjectEntity(
    @PrimaryKey val id: String,
    val subjectCode: String,
    val subjectName: String,
    val facultyName: String? = null,
    val credits: Int = 3,
    val colorHex: String = "#3B82F6"
)

@Entity(
    tableName = "timetables",
    indices = [
        Index(value = ["dayOfWeek"]),
        Index(value = ["subjectId"])
    ]
)
data class TimetableEntity(
    @PrimaryKey val id: String,
    val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, ... 7 = Sunday
    val subjectId: String,
    val startTime: String, // "09:00"
    val endTime: String,   // "10:00"
    val room: String? = null,
    val scheduleType: String = "REGULAR", // REGULAR, CONFIRMED, EXCEPTION, CANCELLED
    val verificationStatus: String = "CONFIRMED", // CONFIRMED, UNVERIFIED
    val notes: String? = null
)

@Entity(
    tableName = "academic_assignments",
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["dueDateMs"]),
        Index(value = ["status"])
    ]
)
data class AcademicAssignmentEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val title: String,
    val description: String? = null,
    val dueDateMs: Long,
    val status: String = "PENDING", // PENDING, SUBMITTED, GRADED
    val sourceConnector: String = "academic_module"
)

@Entity(
    tableName = "academic_examinations",
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["examDateMs"])
    ]
)
data class AcademicExaminationEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val title: String,
    val examDateMs: Long,
    val durationMinutes: Int = 180,
    val room: String? = null,
    val syllabusCoverage: String? = null
)
