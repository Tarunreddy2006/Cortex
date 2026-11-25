"""
Cortex Core - Sync Event Record Model
"""

from dataclasses import dataclass
from typing import Optional
from cortex_core.models.base import BaseRecord
from cortex_core.models.connector import SyncStatus


@dataclass
class SyncEventRecord(BaseRecord):
    """Represents a background or manual synchronization event log entry."""
    event_id: str = ""
    connector_id: str = ""
    status: SyncStatus = SyncStatus.IDLE
    started_at_ms: int = 0
    completed_at_ms: int = 0
    records_processed: int = 0
    records_added: int = 0
    records_updated: int = 0
    error_message: Optional[str] = None
