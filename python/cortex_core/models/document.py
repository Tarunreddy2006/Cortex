"""
Cortex Core - Document Record Model
"""

from dataclasses import dataclass
from typing import Optional
from cortex_core.models.base import BaseRecord


@dataclass
class DocumentRecord(BaseRecord):
    """Represents an imported document, file, or PDF in Cortex private storage."""
    document_id: str = ""
    original_uri: str = ""
    file_name: str = ""
    mime_type: str = ""
    file_size_bytes: int = 0
    local_storage_path: str = ""
    content_summary: Optional[str] = None
    extracted_text: Optional[str] = None
    source_application: Optional[str] = None
    imported_at_ms: int = 0
