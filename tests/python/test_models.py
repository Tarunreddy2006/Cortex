"""
Unit tests for Cortex Python Models
"""

import unittest
from cortex_core.models.base import BaseRecord
from cortex_core.models.application import ApplicationRecord, AppCategory
from cortex_core.models.connector import ConnectorRecord, CapabilityType, SyncStatus
from cortex_core.models.notification import NotificationRecord
from cortex_core.models.email import EmailRecord
from cortex_core.models.calendar_event import CalendarEventRecord
from cortex_core.models.contact import ContactRecord
from cortex_core.models.document import DocumentRecord
from cortex_core.models.link import LinkRecord
from cortex_core.models.sync_event import SyncEventRecord


class TestModels(unittest.TestCase):

    def test_base_record_serialization(self):
        record = BaseRecord(id="rec_1", source_connector="test_conn")
        d = record.to_dict()
        self.assertEqual(d["id"], "rec_1")
        self.assertEqual(d["source_connector"], "test_conn")
        self.assertIn("created_at", d)

        json_str = record.to_json()
        self.assertIn('"id": "rec_1"', json_str)

    def test_application_record(self):
        app = ApplicationRecord(
            id="app_1",
            source_connector="inventory",
            package_name="com.whatsapp",
            app_name="WhatsApp",
            category=AppCategory.COMMUNICATION,
            is_system_app=False,
            is_enabled=True,
            supported_capabilities=["NOTIFICATION", "SHARE_IMPORT"]
        )
        self.assertEqual(app.category, AppCategory.COMMUNICATION)
        self.assertTrue(app.is_enabled)
        self.assertIn("NOTIFICATION", app.supported_capabilities)

    def test_notification_record(self):
        notif = NotificationRecord(
            id="notif_1",
            source_connector="listener",
            package_name="com.google.android.gm",
            app_name="Gmail",
            title="Meeting update",
            text="Sync at 4 PM",
            timestamp_ms=1700000000000,
            deduplication_hash="abc123hash"
        )
        self.assertEqual(notif.package_name, "com.google.android.gm")
        self.assertEqual(notif.deduplication_hash, "abc123hash")

    def test_email_record(self):
        email = EmailRecord(
            id="email_1",
            source_connector="gmail",
            message_id="msg_123",
            thread_id="th_123",
            sender="prof@university.edu",
            recipients=["me@cortex.ai"],
            subject="Assignment 2 Due Date",
            snippet="Please submit by Friday",
            timestamp_ms=1700000000000,
            is_academic=True
        )
        self.assertTrue(email.is_academic)
        self.assertEqual(email.sender, "prof@university.edu")

    def test_calendar_and_contact_record(self):
        event = CalendarEventRecord(
            id="cal_1",
            source_connector="cal_provider",
            event_id="e1",
            calendar_id="c1",
            calendar_name="Personal",
            title="Design Review",
            start_time_ms=1700000000000,
            end_time_ms=1700003600000
        )
        self.assertEqual(event.title, "Design Review")

        contact = ContactRecord(
            id="cnt_1",
            source_connector="contacts_provider",
            contact_id="c1",
            lookup_key="lk_1",
            display_name="John Doe",
            phone_numbers=["+1234567890"],
            email_addresses=["john@example.com"]
        )
        self.assertEqual(contact.display_name, "John Doe")


if __name__ == "__main__":
    unittest.main()
