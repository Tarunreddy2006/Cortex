"""
Cortex Core - Connector Interfaces Package
"""

from cortex_core.connector_interfaces.base_interface import BaseConnector
from cortex_core.connector_interfaces.registry import ConnectorRegistry

__all__ = [
    "BaseConnector",
    "ConnectorRegistry",
]
