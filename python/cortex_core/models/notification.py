"""
Cortex Core - Notification Record Model
"""

from dataclasses import dataclass
from typing import Optional
from cortex_core.models.base import BaseRecord


@dataclass
class NotificationRecord(BaseRecord):
    """Represents a sanitized notification captured from an authorized application."""
    package_name: str = ""
    app_name: str = ""
    title: str = ""
    text: str = ""
    timestamp_ms: int = 0
    is_grouped: bool = False
    group_key: Optional[str] = None
    deduplication_hash: str = ""
    extracted_action_or_event: Optional[str] = None
