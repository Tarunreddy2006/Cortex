"""
Cortex Core - Memory Record Model
Phase 2: Personal Memory Engine
"""

from dataclasses import dataclass, field
from enum import Enum
from typing import List, Optional
from cortex_core.models.base import BaseRecord


class MemoryCategory(str, Enum):
    PERSONAL = "PERSONAL"
    EPISODIC = "EPISODIC"
    ACADEMIC = "ACADEMIC"
    PROJECT = "PROJECT"
    KNOWLEDGE = "KNOWLEDGE"


class ConfidenceStatus(str, Enum):
    USER_EXPLICIT = "USER_EXPLICIT"
    CONFIRMED = "CONFIRMED"
    EXTRACTED = "EXTRACTED"
    AI_INFERRED = "AI_INFERRED"


class ApprovalStatus(str, Enum):
    APPROVED = "APPROVED"
    PENDING_REVIEW = "PENDING_REVIEW"
    DISMISSED = "DISMISSED"


@dataclass
class MemoryRecord(BaseRecord):
    """Represents a structured, contextual memory entry in Cortex."""
    content: str = ""
    category: MemoryCategory = MemoryCategory.PERSONAL
    confidence_status: ConfidenceStatus = ConfidenceStatus.USER_EXPLICIT
    approval_status: ApprovalStatus = ApprovalStatus.APPROVED
    is_pinned: bool = False
    source_record_id: Optional[str] = None
    tags: List[str] = field(default_factory=list)
    related_memory_ids: List[str] = field(default_factory=list)
    review_date_ms: Optional[int] = None
