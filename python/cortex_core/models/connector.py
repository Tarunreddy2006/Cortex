"""
Cortex Core - Connector Record Model
"""

from dataclasses import dataclass, field
from enum import Enum
from typing import List, Optional
from cortex_core.models.base import BaseRecord


class CapabilityType(str, Enum):
    OFFICIAL_API = "OFFICIAL_API"
    SYSTEM_PROVIDER = "SYSTEM_PROVIDER"
    NOTIFICATION = "NOTIFICATION"
    SHARE_IMPORT = "SHARE_IMPORT"
    UNSUPPORTED = "UNSUPPORTED"


class SyncStatus(str, Enum):
    IDLE = "IDLE"
    SYNCING = "SYNCING"
    SUCCESS = "SUCCESS"
    ERROR = "ERROR"
    PENDING_PERMISSION = "PENDING_PERMISSION"
    DISABLED = "DISABLED"


@dataclass
class ConnectorRecord(BaseRecord):
    """Represents an integration profile for a service or application."""
    connector_id: str = ""
    display_name: str = ""
    target_package: Optional[str] = None
    capabilities: List[CapabilityType] = field(default_factory=list)
    status: SyncStatus = SyncStatus.IDLE
    is_authorized: bool = False
    is_enabled: bool = False
    last_sync_timestamp: Optional[str] = None
    last_sync_error: Optional[str] = None
    total_records_imported: int = 0
