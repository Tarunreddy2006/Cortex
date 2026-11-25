"""
Cortex Core - Link Record Model
"""

from dataclasses import dataclass
from typing import Optional
from cortex_core.models.base import BaseRecord


@dataclass
class LinkRecord(BaseRecord):
    """Represents a shared or imported web link."""
    link_id: str = ""
    url: str = ""
    title: Optional[str] = None
    description: Optional[str] = None
    source_application: Optional[str] = None
    timestamp_ms: int = 0
