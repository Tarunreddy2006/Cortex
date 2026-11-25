"""
Cortex Core - Record Schema Validator
"""

from typing import Tuple
from cortex_core.models.base import BaseRecord
from cortex_core.models.application import ApplicationRecord
from cortex_core.models.notification import NotificationRecord
from cortex_core.models.email import EmailRecord
from cortex_core.models.calendar_event import CalendarEventRecord
from cortex_core.models.contact import ContactRecord
from cortex_core.models.document import DocumentRecord
from cortex_core.models.link import LinkRecord


class RecordValidator:
    """Validates record integrity, required fields, and boundary limits."""

    @staticmethod
    def validate(record: BaseRecord) -> Tuple[bool, str]:
        if not record.id:
            return False, "Missing record ID"
        if not record.source_connector:
            return False, "Missing source connector identifier"

        if isinstance(record, ApplicationRecord):
            if not record.package_name:
                return False, "ApplicationRecord requires package_name"
            if not record.app_name:
                return False, "ApplicationRecord requires app_name"

        elif isinstance(record, NotificationRecord):
            if not record.package_name:
                return False, "NotificationRecord requires package_name"
            if not record.title and not record.text:
                return False, "NotificationRecord must have either title or text"
            if record.timestamp_ms <= 0:
                return False, "NotificationRecord has invalid timestamp"

        elif isinstance(record, EmailRecord):
            if not record.message_id:
                return False, "EmailRecord requires message_id"
            if not record.sender:
                return False, "EmailRecord requires sender"
            if record.timestamp_ms <= 0:
                return False, "EmailRecord has invalid timestamp"

        elif isinstance(record, CalendarEventRecord):
            if not record.event_id:
                return False, "CalendarEventRecord requires event_id"
            if not record.title:
                return False, "CalendarEventRecord requires title"
            if record.end_time_ms < record.start_time_ms:
                return False, "CalendarEventRecord end_time must be >= start_time"

        elif isinstance(record, ContactRecord):
            if not record.contact_id:
                return False, "ContactRecord requires contact_id"
            if not record.display_name and not record.phone_numbers and not record.email_addresses:
                return False, "ContactRecord requires at least name, phone, or email"

        elif isinstance(record, DocumentRecord):
            if not record.document_id:
                return False, "DocumentRecord requires document_id"
            if not record.file_name:
                return False, "DocumentRecord requires file_name"

        elif isinstance(record, LinkRecord):
            if not record.link_id or not record.url:
                return False, "LinkRecord requires link_id and valid url"

        return True, "Valid"
