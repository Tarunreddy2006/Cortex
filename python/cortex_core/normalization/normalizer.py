"""
Cortex Core - Normalizer Pipeline
"""

from typing import Optional
from cortex_core.models.base import BaseRecord
from cortex_core.models.notification import NotificationRecord
from cortex_core.models.email import EmailRecord
from cortex_core.models.calendar_event import CalendarEventRecord
from cortex_core.models.contact import ContactRecord
from cortex_core.models.document import DocumentRecord
from cortex_core.models.link import LinkRecord
from cortex_core.normalization.text_cleaner import TextCleaner
from cortex_core.normalization.date_parser import DateParser


class Normalizer:
    """Applies canonical normalization rules across Cortex records."""

    @classmethod
    def normalize(cls, record: BaseRecord) -> BaseRecord:
        if isinstance(record, NotificationRecord):
            record.title = TextCleaner.clean(record.title)
            record.text = TextCleaner.clean(record.text)
            record.text = TextCleaner.redact_credit_cards(record.text)
            if not record.created_at:
                record.created_at = DateParser.epoch_ms_to_iso(record.timestamp_ms)

        elif isinstance(record, EmailRecord):
            record.subject = TextCleaner.clean(record.subject)
            record.snippet = TextCleaner.clean(record.snippet)
            if record.body_text:
                record.body_text = TextCleaner.truncate(TextCleaner.clean(record.body_text), 10000)
            if not record.created_at:
                record.created_at = DateParser.epoch_ms_to_iso(record.timestamp_ms)

        elif isinstance(record, CalendarEventRecord):
            record.title = TextCleaner.clean(record.title)
            if record.description:
                record.description = TextCleaner.clean(record.description)
            if record.location:
                record.location = TextCleaner.clean(record.location)
            if not record.created_at:
                record.created_at = DateParser.epoch_ms_to_iso(record.start_time_ms)

        elif isinstance(record, ContactRecord):
            record.display_name = TextCleaner.clean(record.display_name)
            # Normalize phone numbers: remove spaces and hyphens
            record.phone_numbers = [
                "".join(ch for ch in p if ch.isdigit() or ch == "+")
                for p in record.phone_numbers
                if p
            ]
            record.email_addresses = [e.strip().lower() for e in record.email_addresses if e]

        elif isinstance(record, DocumentRecord):
            record.file_name = TextCleaner.clean(record.file_name)
            if record.extracted_text:
                record.extracted_text = TextCleaner.truncate(record.extracted_text, 20000)

        elif isinstance(record, LinkRecord):
            record.url = record.url.strip()
            if record.title:
                record.title = TextCleaner.clean(record.title)

        return record
