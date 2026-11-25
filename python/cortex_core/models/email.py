"""
Cortex Core - Email Record Model
"""

from dataclasses import dataclass, field
from typing import List, Optional
from cortex_core.models.base import BaseRecord


@dataclass
class EmailRecord(BaseRecord):
    """Represents an email retrieved through read-only Gmail integration."""
    message_id: str = ""
    thread_id: str = ""
    sender: str = ""
    recipients: List[str] = field(default_factory=list)
    subject: str = ""
    snippet: str = ""
    body_text: Optional[str] = None
    timestamp_ms: int = 0
    labels: List[str] = field(default_factory=list)
    has_attachments: bool = False
    is_academic: bool = False
    is_actionable: bool = False
