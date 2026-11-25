"""
Cortex Core - Calendar Event Record Model
"""

from dataclasses import dataclass
from typing import Optional
from cortex_core.models.base import BaseRecord


@dataclass
class CalendarEventRecord(BaseRecord):
    """Represents a calendar event retrieved from Calendar Provider or Google Calendar API."""
    event_id: str = ""
    calendar_id: str = ""
    calendar_name: str = ""
    title: str = ""
    description: Optional[str] = None
    location: Optional[str] = None
    start_time_ms: int = 0
    end_time_ms: int = 0
    is_all_day: bool = False
    rrule: Optional[str] = None
    organizer: Optional[str] = None
