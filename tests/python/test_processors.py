"""
Unit tests for Cortex Processors: Notification, Email, Calendar, WhatsApp Export
"""

import unittest
from cortex_core.processing.notification_processor import NotificationProcessor
from cortex_core.processing.email_processor import EmailProcessor
from cortex_core.processing.whatsapp_export_processor import WhatsAppExportProcessor
from cortex_core.processing.shared_content_processor import SharedContentProcessor


class TestProcessors(unittest.TestCase):

    def test_notification_processor_allowed(self):
        record, accepted, reason = NotificationProcessor.process(
            package_name="com.whatsapp",
            app_name="WhatsApp",
            title="Project Team",
            text="Team meeting tomorrow at 3 PM to review architecture",
            timestamp_ms=1700000000000
        )
        self.assertTrue(accepted)
        self.assertIsNotNone(record)
        self.assertIsNotNone(record.extracted_action_or_event)
        self.assertTrue(len(record.deduplication_hash) > 10)

    def test_notification_processor_financial_rejected(self):
        record, accepted, reason = NotificationProcessor.process(
            package_name="com.phonepe.app",
            app_name="PhonePe",
            title="Money Sent",
            text="Rs 500 debited from your account",
            timestamp_ms=1700000000000
        )
        self.assertFalse(accepted)
        self.assertIsNone(record)
        self.assertIn("financial", reason.lower())

    def test_email_processor_academic(self):
        record, accepted, reason = EmailProcessor.process(
            message_id="msg_99",
            thread_id="th_99",
            sender="course_admin@stanford.edu",
            recipients=["student@cortex.ai"],
            subject="CS229 Assignment 3 Due Date Extended",
            snippet="The coursework submission deadline has been moved to Monday.",
            body_text="Details regarding homework submission on Canvas...",
            timestamp_ms=1700000000000
        )
        self.assertTrue(accepted)
        self.assertIsNotNone(record)
        self.assertTrue(record.is_academic)

    def test_email_processor_financial_rejected(self):
        record, accepted, reason = EmailProcessor.process(
            message_id="msg_fin",
            thread_id="th_fin",
            sender="alerts@hdfcbank.com",
            recipients=["user@cortex.ai"],
            subject="Account Statement Alert",
            snippet="Your account balance is INR 45,210. Available balance updated.",
            body_text=None,
            timestamp_ms=1700000000000
        )
        self.assertFalse(accepted)
        self.assertIsNone(record)

    def test_whatsapp_export_processor(self):
        sample_chat = (
            "[12/05/24, 14:30:15] Messages and calls are end-to-end encrypted.\n"
            "[12/05/24, 14:31:00] Alice: Hey Bob, do we have meeting tomorrow?\n"
            "[12/05/24, 14:31:45] Bob: Yes, submit the assignment before 5 PM.\n"
            "[12/05/24, 14:32:10] Alice: Got it, thanks!\n"
            "[12/05/24, 14:35:00] Bob: Your OTP for payment is 482910\n"  # financial trace
        )
        result = WhatsAppExportProcessor.parse_chat_text(sample_chat, "test_chat.txt")
        self.assertEqual(result["total_messages"], 3)  # OTP message skipped
        self.assertEqual(result["skipped_financial_messages"], 1)
        self.assertIn("Alice", result["participants"])
        self.assertIn("Bob", result["participants"])
        self.assertTrue(len(result["extracted_tasks"]) >= 1)

    def test_shared_link_processor(self):
        record, accepted, reason = SharedContentProcessor.process_shared_link(
            url="https://arxiv.org/abs/2301.00001",
            title="Interesting Paper",
            source_app="com.android.chrome"
        )
        self.assertTrue(accepted)
        self.assertIsNotNone(record)
        self.assertEqual(record.url, "https://arxiv.org/abs/2301.00001")


if __name__ == "__main__":
    unittest.main()
