"""
Cortex Core - Knowledge Vault Ingestion Pipeline
Phase 2: Document processing, text extraction, and categorization.
"""

import os
import re
from typing import List, Dict, Any, Optional, Tuple
from cortex_core.models.knowledge import DocumentItemRecord
from cortex_core.validation.financial_validator import FinancialExclusionValidator


SUPPORTED_DOCUMENT_TYPES = {
    ".pdf": "PDF",
    ".docx": "DOCX",
    ".xlsx": "XLSX",
    ".pptx": "PPTX",
    ".txt": "TXT",
    ".md": "MD",
    ".json": "JSON",
    ".csv": "CSV",
    ".png": "IMAGE",
    ".jpg": "IMAGE",
    ".jpeg": "IMAGE"
}


class KnowledgePipeline:
    """Extracts, validates, classifies, and indexes documents for the Knowledge Vault."""

    @classmethod
    def get_document_type(cls, file_name: str) -> str:
        ext = os.path.splitext(file_name)[1].lower()
        return SUPPORTED_DOCUMENT_TYPES.get(ext, "OTHER")

    @classmethod
    def extract_text(cls, file_path: str, max_chars: int = 50000) -> str:
        """
        Extract text from file.
        For plain text, markdown, CSV, JSON: reads directly with UTF-8 / latin-1 fallback.
        For binary files: extracts printable ASCII/UTF-8 text chunks safely.
        """
        if not os.path.exists(file_path):
            return ""

        try:
            # Try direct text read
            with open(file_path, "r", encoding="utf-8", errors="replace") as f:
                content = f.read(max_chars)
                # If content is mostly printable text
                printable_count = sum(1 for c in content if c.isprintable() or c in "\n\r\t")
                if printable_count / (len(content) or 1) > 0.85:
                    return content
        except Exception:
            pass

        # Binary fallback: extract string chunks
        try:
            with open(file_path, "rb") as f:
                data = f.read(max_chars * 2)
            # Find runs of 4+ printable characters
            matches = re.findall(b"[\x20-\x7E\t\n\r]{4,}", data)
            text_chunks = [m.decode("ascii", errors="ignore") for m in matches]
            return "\n".join(text_chunks)[:max_chars]
        except Exception:
            return ""

    @classmethod
    def auto_categorize(cls, file_name: str, extracted_text: str) -> str:
        combined = f"{file_name} {extracted_text}".lower()
        if any(w in combined for w in ["syllabus", "assignment", "lecture", "course", "exam", "grade", "curriculum"]):
            return "ACADEMIC"
        if any(w in combined for w in ["architecture", "spec", "design", "api", "schema", "sprint", "milestone"]):
            return "PROJECT"
        if any(w in combined for w in ["research", "paper", "journal", "abstract", "methodology", "study"]):
            return "RESEARCH"
        if any(w in combined for w in ["invoice", "receipt", "contract", "agreement", "lease"]):
            return "LEGAL_ADMIN"
        return "GENERAL"

    @classmethod
    def generate_summary(cls, text: str, max_lines: int = 3) -> str:
        lines = [line.strip() for line in text.splitlines() if len(line.strip()) > 20]
        summary_lines = lines[:max_lines]
        return " ".join(summary_lines) if summary_lines else text[:150].strip()

    @classmethod
    def ingest_document(
        cls,
        file_path: str,
        file_name: str,
        title: Optional[str] = None,
        collection_id: Optional[str] = None,
        source_connector: str = "knowledge_vault"
    ) -> Tuple[Optional[DocumentItemRecord], bool, str]:
        """
        Executes ingestion pipeline:
        IMPORT -> VALIDATE -> EXTRACT -> NORMALIZE -> CLASSIFY -> INDEX -> STORE
        """
        # Step 1: Validate file existence
        if not os.path.exists(file_path):
            return None, False, f"File not found: {file_path}"

        file_size = os.path.getsize(file_path)
        doc_type = cls.get_document_type(file_name)

        # Step 2: Extract text
        extracted = cls.extract_text(file_path)

        # Step 3: Financial exclusion check
        is_fin, reason = FinancialExclusionValidator.contains_financial_content(extracted[:5000])
        if is_fin:
            return None, False, f"Document excluded by financial policy: {reason}"

        # Step 4: Classify and summarize
        category = cls.auto_categorize(file_name, extracted)
        summary = cls.generate_summary(extracted)

        # Step 5: Tags
        words = re.findall(r"\b[a-zA-Z]{5,}\b", f"{file_name} {extracted[:3000]}")
        tags = list(dict.fromkeys([w.lower() for w in words[:5]]))

        doc_id = f"doc_{abs(hash(file_path + file_name))}"
        record = DocumentItemRecord(
            id=doc_id,
            source_connector=source_connector,
            title=title or file_name,
            file_name=file_name,
            file_type=doc_type,
            file_size_bytes=file_size,
            storage_path=file_path,
            extracted_text=extracted[:10000],  # indexable text
            summary=summary,
            category=category,
            tags=tags,
            collection_id=collection_id
        )

        return record, True, "Document ingested and indexed"
