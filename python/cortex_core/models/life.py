"""
Cortex Core - Life & Project Management Models
Phase 2: Daily Life & Project Intelligence
"""

from dataclasses import dataclass, field
from typing import List, Optional
from cortex_core.models.base import BaseRecord


@dataclass
class TaskRecord(BaseRecord):
    title: str = ""
    description: Optional[str] = None
    priority: str = "MEDIUM"  # LOW, MEDIUM, HIGH, URGENT
    status: str = "PENDING"  # PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    category: str = "PERSONAL"  # ACADEMIC, PROJECT, PERSONAL, RESPONSIBILITY
    due_date_ms: Optional[int] = None
    is_recurring: bool = False
    recurrence_rule: Optional[str] = None
    subtasks: List[dict] = field(default_factory=list)
    project_id: Optional[str] = None
    completed_at_ms: Optional[int] = None


@dataclass
class GoalRecord(BaseRecord):
    title: str = ""
    description: Optional[str] = None
    goal_type: str = "PERSONAL"  # SHORT_TERM, LONG_TERM, LEARNING, PERSONAL, PROJECT
    target_date_ms: Optional[int] = None
    progress_percent: int = 0
    status: str = "ACTIVE"  # ACTIVE, ACHIEVED, PAUSED


@dataclass
class ProjectRecord(BaseRecord):
    name: str = ""
    description: Optional[str] = None
    objective: Optional[str] = None
    status: str = "ACTIVE"  # PLANNING, ACTIVE, ON_HOLD, COMPLETED
    tech_stack: List[str] = field(default_factory=list)
    progress_percent: int = 0
