"""
Cortex Core - Processing Package
"""

from cortex_core.processing.notification_processor import NotificationProcessor
from cortex_core.processing.email_processor import EmailProcessor
from cortex_core.processing.calendar_processor import CalendarProcessor
from cortex_core.processing.contact_processor import ContactProcessor
from cortex_core.processing.whatsapp_export_processor import WhatsAppExportProcessor
from cortex_core.processing.shared_content_processor import SharedContentProcessor

# Phase 2 Processors
from cortex_core.processing.memory_engine import MemoryEngine
from cortex_core.processing.knowledge_pipeline import KnowledgePipeline

__all__ = [
    "NotificationProcessor",
    "EmailProcessor",
    "CalendarProcessor",
    "ContactProcessor",
    "WhatsAppExportProcessor",
    "SharedContentProcessor",
    "MemoryEngine",
    "KnowledgePipeline",
]
