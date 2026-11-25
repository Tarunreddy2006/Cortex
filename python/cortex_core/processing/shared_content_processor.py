"""
Cortex Core - Shared Content & Document Import Processing
"""

from typing import Optional, Tuple
from cortex_core.models.document import DocumentRecord
from cortex_core.models.link import LinkRecord
from cortex_core.normalization.normalizer import Normalizer
from cortex_core.normalization.date_parser import DateParser


class SharedContentProcessor:
    """Processes content shared via Android ACTION_SEND intents or Storage Access Framework."""

    @classmethod
    def process_shared_link(
        cls,
        url: str,
        title: Optional[str] = None,
        source_app: Optional[str] = None
    ) -> Tuple[Optional[LinkRecord], bool, str]:
        if not url:
            return None, False, "URL is empty"

        record_id = f"link_{abs(hash(url))}"
        record = LinkRecord(
            id=record_id,
            source_connector="android_share_target",
            url=url,
            title=title or url,
            source_application=source_app,
            timestamp_ms=DateParser.now_epoch_ms()
        )
        normalized = Normalizer.normalize(record)
        return normalized, True, "Shared link accepted"

    @classmethod
    def process_imported_document(
        cls,
        doc_id: str,
        file_name: str,
        mime_type: str,
        file_size_bytes: int,
        local_path: str,
        extracted_text: Optional[str] = None,
        source_app: Optional[str] = None
    ) -> Tuple[Optional[DocumentRecord], bool, str]:
        if not file_name:
            return None, False, "File name is required"

        record = DocumentRecord(
            id=f"doc_{doc_id}",
            source_connector="document_saf_importer",
            document_id=doc_id,
            original_uri=local_path,
            file_name=file_name,
            mime_type=mime_type,
            file_size_bytes=file_size_bytes,
            local_storage_path=local_path,
            extracted_text=extracted_text,
            source_application=source_app,
            imported_at_ms=DateParser.now_epoch_ms()
        )
        normalized = Normalizer.normalize(record)
        return normalized, True, "Document accepted"
