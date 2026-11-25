"""
Cortex Core - Email Processing Pipeline
"""

import re
from typing import List, Optional, Tuple
from cortex_core.models.email import EmailRecord
from cortex_core.validation.financial_validator import FinancialExclusionValidator
from cortex_core.normalization.normalizer import Normalizer


ACADEMIC_PATTERNS = [
    re.compile(r"\b(classroom|assignment|homework|coursework|syllabus|grade|lecture|professor|exam)\b", re.IGNORECASE),
    re.compile(r"\b(coursera|edx|udemy|canvas|blackboard|moodle)\b", re.IGNORECASE),
    re.compile(r"\.(edu|ac\.[a-z]{2})\b", re.IGNORECASE),
]

ACTIONABLE_PATTERNS = [
    re.compile(r"\b(action required|please reply|deadline|urgent|reminder|rsvp)\b", re.IGNORECASE),
    re.compile(r"\b(schedule|meeting|invitation|confirm)\b", re.IGNORECASE),
]


class EmailProcessor:
    """Processes emails retrieved through read-only Gmail integration."""

    @staticmethod
    def is_academic(sender: str, subject: str, snippet: str) -> bool:
        combined = f"{sender} {subject} {snippet}"
        return any(p.search(combined) for p in ACADEMIC_PATTERNS)

    @staticmethod
    def is_actionable(subject: str, snippet: str) -> bool:
        combined = f"{subject} {snippet}"
        return any(p.search(combined) for p in ACTIONABLE_PATTERNS)

    @classmethod
    def process(
        cls,
        message_id: str,
        thread_id: str,
        sender: str,
        recipients: List[str],
        subject: str,
        snippet: str,
        body_text: Optional[str],
        timestamp_ms: int,
        labels: Optional[List[str]] = None,
        has_attachments: bool = False
    ) -> Tuple[Optional[EmailRecord], bool, str]:
        """
        Process email with strict financial exclusion:
        1. Financial check on sender and snippet.
        2. Academic and actionable detection.
        3. Normalization.
        """
        # Check financial rules
        is_fin, reason = FinancialExclusionValidator.contains_financial_content(snippet, sender)
        if is_fin:
            return None, False, f"Email excluded: {reason}"

        is_fin_subj, reason_subj = FinancialExclusionValidator.contains_financial_content(subject)
        if is_fin_subj:
            return None, False, f"Email subject excluded: {reason_subj}"

        academic_flag = cls.is_academic(sender, subject, snippet)
        actionable_flag = cls.is_actionable(subject, snippet)

        record = EmailRecord(
            id=f"email_{message_id}",
            source_connector="gmail_read_only",
            message_id=message_id,
            thread_id=thread_id,
            sender=sender,
            recipients=recipients or [],
            subject=subject,
            snippet=snippet,
            body_text=body_text,
            timestamp_ms=timestamp_ms,
            labels=labels or [],
            has_attachments=has_attachments,
            is_academic=academic_flag,
            is_actionable=actionable_flag
        )

        normalized = Normalizer.normalize(record)
        return normalized, True, "Accepted and normalized"
