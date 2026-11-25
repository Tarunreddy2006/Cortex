"""
Cortex Core - Date and Time Normalizer
"""

from datetime import datetime, timezone
import re
from typing import Optional


class DateParser:
    """Parses various date and time formats into unified UTC ISO-8601 strings or epoch ms."""

    @staticmethod
    def now_utc_iso() -> str:
        return datetime.now(timezone.utc).isoformat()

    @staticmethod
    def now_epoch_ms() -> int:
        return int(datetime.now(timezone.utc).timestamp() * 1000)

    @staticmethod
    def epoch_ms_to_iso(epoch_ms: int) -> str:
        try:
            dt = datetime.fromtimestamp(epoch_ms / 1000.0, tz=timezone.utc)
            return dt.isoformat()
        except (ValueError, OSError):
            return datetime.now(timezone.utc).isoformat()

    @staticmethod
    def parse_iso(iso_str: str) -> Optional[int]:
        """Convert ISO 8601 string to epoch ms."""
        try:
            clean = iso_str.replace("Z", "+00:00")
            dt = datetime.fromisoformat(clean)
            return int(dt.timestamp() * 1000)
        except Exception:
            return None
