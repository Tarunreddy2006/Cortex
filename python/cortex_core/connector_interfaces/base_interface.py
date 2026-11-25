"""
Cortex Core - Base Connector Interface
Defines the contract for all Cortex connectors.
"""

from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional
from cortex_core.models.connector import ConnectorRecord, CapabilityType, SyncStatus
from cortex_core.models.base import BaseRecord


class BaseConnector(ABC):
    """Abstract Base Class for all digital life connectors."""

    def __init__(self, connector_id: str, display_name: str, target_package: Optional[str] = None):
        self.connector_id = connector_id
        self.display_name = display_name
        self.target_package = target_package
        self.status = SyncStatus.IDLE
        self.is_enabled = False

    @abstractmethod
    def get_capabilities(self) -> List[CapabilityType]:
        """Return the list of capabilities supported by this connector."""
        pass

    @abstractmethod
    def to_record(self) -> ConnectorRecord:
        """Export connector profile."""
        pass
