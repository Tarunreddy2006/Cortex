"""
Cortex Core - Record Models Package
"""

from cortex_core.models.base import BaseRecord
from cortex_core.models.application import ApplicationRecord, AppCategory
from cortex_core.models.connector import ConnectorRecord, CapabilityType, SyncStatus
from cortex_core.models.notification import NotificationRecord
from cortex_core.models.email import EmailRecord
from cortex_core.models.calendar_event import CalendarEventRecord
from cortex_core.models.contact import ContactRecord
from cortex_core.models.document import DocumentRecord
from cortex_core.models.link import LinkRecord
from cortex_core.models.sync_event import SyncEventRecord

# Phase 2 Models
from cortex_core.models.memory import MemoryRecord, MemoryCategory, ConfidenceStatus, ApprovalStatus
from cortex_core.models.academic import (
    AcademicProfileRecord,
    SubjectRecord,
    TimetableRecord,
    AssignmentRecord,
    ExaminationRecord,
)
from cortex_core.models.knowledge import DocumentItemRecord, CollectionRecord, NoteRecord
from cortex_core.models.life import TaskRecord, GoalRecord, ProjectRecord

__all__ = [
    "BaseRecord",
    "ApplicationRecord",
    "AppCategory",
    "ConnectorRecord",
    "CapabilityType",
    "SyncStatus",
    "NotificationRecord",
    "EmailRecord",
    "CalendarEventRecord",
    "ContactRecord",
    "DocumentRecord",
    "LinkRecord",
    "SyncEventRecord",
    # Phase 2
    "MemoryRecord",
    "MemoryCategory",
    "ConfidenceStatus",
    "ApprovalStatus",
    "AcademicProfileRecord",
    "SubjectRecord",
    "TimetableRecord",
    "AssignmentRecord",
    "ExaminationRecord",
    "DocumentItemRecord",
    "CollectionRecord",
    "NoteRecord",
    "TaskRecord",
    "GoalRecord",
    "ProjectRecord",
]
