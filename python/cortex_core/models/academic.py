"""
Cortex Core - Academic Models
Phase 2: Academic Intelligence
"""

from dataclasses import dataclass, field
from typing import List, Optional
from cortex_core.models.base import BaseRecord


@dataclass
class AcademicProfileRecord(BaseRecord):
    institution: str = ""
    degree: str = ""
    department: str = ""
    semester: int = 1
    academic_year: str = "2026-2027"


@dataclass
class SubjectRecord(BaseRecord):
    subject_code: str = ""
    subject_name: str = ""
    faculty_name: Optional[str] = None
    credits: int = 3
    color_hex: str = "#3B82F6"


@dataclass
class TimetableRecord(BaseRecord):
    day_of_week: int = 1  # 1=Monday..7=Sunday
    subject_id: str = ""
    start_time: str = "09:00"
    end_time: str = "10:00"
    room: Optional[str] = None
    schedule_type: str = "REGULAR"  # REGULAR, CONFIRMED, EXCEPTION, CANCELLED
    verification_status: str = "CONFIRMED"
    notes: Optional[str] = None


@dataclass
class AssignmentRecord(BaseRecord):
    subject_id: str = ""
    title: str = ""
    description: Optional[str] = None
    due_date_ms: int = 0
    status: str = "PENDING"  # PENDING, SUBMITTED, GRADED


@dataclass
class ExaminationRecord(BaseRecord):
    subject_id: str = ""
    title: str = ""
    exam_date_ms: int = 0
    duration_minutes: int = 180
    room: Optional[str] = None
    syllabus_coverage: Optional[str] = None
