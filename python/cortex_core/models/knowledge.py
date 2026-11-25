"""
Cortex Core - Knowledge & Document Models
Phase 2: Knowledge Vault
"""

from dataclasses import dataclass, field
from typing import List, Optional
from cortex_core.models.base import BaseRecord


@dataclass
class DocumentItemRecord(BaseRecord):
    title: str = ""
    file_name: str = ""
    file_type: str = "TXT"  # PDF, DOCX, XLSX, PPTX, TXT, MD, IMAGE, URL, NOTE, CHAT_EXPORT
    file_size_bytes: int = 0
    storage_path: str = ""
    extracted_text: Optional[str] = None
    summary: Optional[str] = None
    category: str = "GENERAL"
    tags: List[str] = field(default_factory=list)
    collection_id: Optional[str] = None
    version: int = 1


@dataclass
class CollectionRecord(BaseRecord):
    name: str = ""
    description: Optional[str] = None
    color_hex: str = "#3B82F6"
    icon: str = "folder"


@dataclass
class NoteRecord(BaseRecord):
    title: str = ""
    content: str = ""
    collection_id: Optional[str] = None
    tags: List[str] = field(default_factory=list)
    linked_document_ids: List[str] = field(default_factory=list)
