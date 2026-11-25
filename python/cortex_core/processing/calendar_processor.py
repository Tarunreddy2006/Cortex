"""
Cortex Core - Calendar Event Processing Pipeline
"""

from typing import Optional, Tuple
from cortex_core.models.calendar_event import CalendarEventRecord
from cortex_core.normalization.normalizer import Normalizer


class CalendarProcessor:
    """Processes calendar events retrieved from Android Calendar Provider or Google Calendar API."""

    @classmethod
    def process(
        cls,
        event_id: str,
        calendar_id: str,
        calendar_name: str,
        title: str,
        start_time_ms: int,
        end_time_ms: int,
        description: Optional[str] = None,
        location: Optional[str] = None,
        is_all_day: bool = False,
        rrule: Optional[str] = None,
        organizer: Optional[str] = None,
        source_connector: str = "calendar_provider"
    ) -> Tuple[Optional[CalendarEventRecord], bool, str]:
        if not title:
            return None, False, "Event title is empty"

        if end_time_ms < start_time_ms:
            end_time_ms = start_time_ms

        record = CalendarEventRecord(
            id=f"cal_{event_id}",
            source_connector=source_connector,
            event_id=event_id,
            calendar_id=calendar_id,
            calendar_name=calendar_name,
            title=title,
            description=description,
            location=location,
            start_time_ms=start_time_ms,
            end_time_ms=end_time_ms,
            is_all_day=is_all_day,
            rrule=rrule,
            organizer=organizer
        )

        normalized = Normalizer.normalize(record)
        return normalized, True, "Accepted and normalized"
