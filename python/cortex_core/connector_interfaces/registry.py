"""
Cortex Core - Connector Registry
Central registry tracking all available connector profiles and capability tiers.
"""

from typing import Dict, List, Optional
from cortex_core.models.connector import ConnectorRecord, CapabilityType, SyncStatus


class ConnectorRegistry:
    """Manages the lifecycle and capabilities of all connectors."""

    def __init__(self):
        self._connectors: Dict[str, ConnectorRecord] = {}
        self._register_default_connectors()

    def _register_default_connectors(self):
        defaults = [
            ConnectorRecord(
                id="conn_app_inventory",
                source_connector="system",
                connector_id="app_inventory",
                display_name="Application Inventory",
                capabilities=[CapabilityType.SYSTEM_PROVIDER],
                is_authorized=True,
                is_enabled=True,
            ),
            ConnectorRecord(
                id="conn_notifications",
                source_connector="system",
                connector_id="notifications",
                display_name="Notification Listener",
                capabilities=[CapabilityType.NOTIFICATION],
                is_authorized=False,
                is_enabled=False,
            ),
            ConnectorRecord(
                id="conn_usage_stats",
                source_connector="system",
                connector_id="usage_stats",
                display_name="Application Usage Statistics",
                capabilities=[CapabilityType.SYSTEM_PROVIDER],
                is_authorized=False,
                is_enabled=False,
            ),
            ConnectorRecord(
                id="conn_calendar",
                source_connector="system",
                connector_id="calendar",
                display_name="Calendar Events",
                capabilities=[CapabilityType.SYSTEM_PROVIDER, CapabilityType.OFFICIAL_API],
                is_authorized=False,
                is_enabled=False,
            ),
            ConnectorRecord(
                id="conn_contacts",
                source_connector="system",
                connector_id="contacts",
                display_name="Contacts Provider",
                capabilities=[CapabilityType.SYSTEM_PROVIDER],
                is_authorized=False,
                is_enabled=False,
            ),
            ConnectorRecord(
                id="conn_sharing_saf",
                source_connector="system",
                connector_id="sharing_saf",
                display_name="Share & Document Import",
                capabilities=[CapabilityType.SHARE_IMPORT],
                is_authorized=True,
                is_enabled=True,
            ),
            ConnectorRecord(
                id="conn_gmail",
                source_connector="google",
                connector_id="gmail",
                display_name="Gmail (Read-Only)",
                target_package="com.google.android.gm",
                capabilities=[CapabilityType.OFFICIAL_API, CapabilityType.NOTIFICATION],
                is_authorized=False,
                is_enabled=False,
            ),
            ConnectorRecord(
                id="conn_google_drive",
                source_connector="google",
                connector_id="google_drive",
                display_name="Google Drive (Selected Files)",
                target_package="com.google.android.apps.docs",
                capabilities=[CapabilityType.OFFICIAL_API, CapabilityType.SHARE_IMPORT],
                is_authorized=False,
                is_enabled=False,
            ),
            ConnectorRecord(
                id="conn_google_classroom",
                source_connector="google",
                connector_id="google_classroom",
                display_name="Google Classroom (Read-Only)",
                target_package="com.google.android.apps.classroom",
                capabilities=[CapabilityType.OFFICIAL_API, CapabilityType.NOTIFICATION],
                is_authorized=False,
                is_enabled=False,
            ),
            ConnectorRecord(
                id="conn_whatsapp",
                source_connector="messaging",
                connector_id="whatsapp",
                display_name="WhatsApp",
                target_package="com.whatsapp",
                capabilities=[CapabilityType.NOTIFICATION, CapabilityType.SHARE_IMPORT],
                is_authorized=False,
                is_enabled=False,
            ),
        ]
        for conn in defaults:
            self._connectors[conn.connector_id] = conn

    def get_connector(self, connector_id: str) -> Optional[ConnectorRecord]:
        return self._connectors.get(connector_id)

    def list_connectors(self) -> List[ConnectorRecord]:
        return list(self._connectors.values())

    def update_status(self, connector_id: str, is_enabled: bool, is_authorized: bool, status: SyncStatus):
        if connector_id in self._connectors:
            conn = self._connectors[connector_id]
            conn.is_enabled = is_enabled
            conn.is_authorized = is_authorized
            conn.status = status
