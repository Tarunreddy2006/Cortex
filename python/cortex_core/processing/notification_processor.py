"""
Cortex Core - Notification Processing Pipeline
"""

import hashlib
import re
from typing import Optional, Tuple
from cortex_core.models.notification import NotificationRecord
from cortex_core.validation.financial_validator import FinancialExclusionValidator
from cortex_core.normalization.normalizer import Normalizer


TASK_PATTERNS = [
    re.compile(r"\b(meet(ing)?|call|appointment|zoom|teams|google meet)\b", re.IGNORECASE),
    re.compile(r"\b(due|deadline|submit|assignment|exam|quiz)\b", re.IGNORECASE),
    re.compile(r"\b(tomorrow|tonight|today|at \d{1,2}(:\d{2})?\s*(am|pm))\b", re.IGNORECASE),
    re.compile(r"\b(reminder|don't forget|remember to)\b", re.IGNORECASE),
]


class NotificationProcessor:
    """Processes notifications captured from Android NotificationListenerService."""

    @staticmethod
    def generate_dedup_hash(package_name: str, title: str, text: str, timestamp_ms: int) -> str:
        """
        Create a deduplication hash based on package, content, and 60-second time window.
        Prevents duplicate persistent storage for repeated notification alerts.
        """
        window_minutes = timestamp_ms // 60000
        raw = f"{package_name.strip()}|{title.strip()}|{text.strip()}|{window_minutes}"
        return hashlib.sha256(raw.encode("utf-8")).hexdigest()

    @staticmethod
    def extract_task_or_event(title: str, text: str) -> Optional[str]:
        """Detect action items, calendar appointments, and deadlines from notification content."""
        combined = f"{title} {text}"
        for pattern in TASK_PATTERNS:
            match = pattern.search(combined)
            if match:
                return f"Triggered by '{match.group(0)}'"
        return None

    @classmethod
    def process(
        cls,
        package_name: str,
        app_name: str,
        title: str,
        text: str,
        timestamp_ms: int,
        is_grouped: bool = False,
        group_key: Optional[str] = None,
        custom_denylist: Optional[set] = None
    ) -> Tuple[Optional[NotificationRecord], bool, str]:
        """
        Full notification ingestion pipeline:
        1. Package check against financial exclusions.
        2. Content check against financial regexes.
        3. Dedup hash generation.
        4. Entity/task extraction.
        5. Normalization.

        Returns: (NotificationRecord or None, is_accepted, reason)
        """
        # Step 1 & 2: Financial Check
        if FinancialExclusionValidator.is_financial_package(package_name, custom_denylist):
            return None, False, f"Package {package_name} is on permanent financial denylist"

        combined_text = f"{title} {text}".strip()
        is_fin, reason = FinancialExclusionValidator.contains_financial_content(combined_text)
        if is_fin:
            return None, False, f"Content excluded by financial policy: {reason}"

        # Step 3: Deduplication hash
        dedup_hash = cls.generate_dedup_hash(package_name, title, text, timestamp_ms)

        # Step 4: Task extraction
        extracted_task = cls.extract_task_or_event(title, text)

        record_id = f"notif_{dedup_hash[:16]}"
        record = NotificationRecord(
            id=record_id,
            source_connector="notification_listener",
            package_name=package_name,
            app_name=app_name,
            title=title,
            text=text,
            timestamp_ms=timestamp_ms,
            is_grouped=is_grouped,
            group_key=group_key,
            deduplication_hash=dedup_hash,
            extracted_action_or_event=extracted_task
        )

        # Step 5: Normalize
        normalized = Normalizer.normalize(record)
        return normalized, True, "Accepted and normalized"
