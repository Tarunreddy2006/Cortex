"""
Unit tests for Cortex Text Cleaner, Date Parser, and Normalizer
"""

import unittest
from cortex_core.normalization.text_cleaner import TextCleaner
from cortex_core.normalization.date_parser import DateParser
from cortex_core.normalization.normalizer import Normalizer
from cortex_core.models.notification import NotificationRecord
from cortex_core.models.contact import ContactRecord


class TestNormalization(unittest.TestCase):

    def test_text_cleaner(self):
        raw = "  Hello \t  world \n\n  This is   a test.   "
        cleaned = TextCleaner.clean(raw)
        self.assertEqual(cleaned, "Hello world\nThis is a test.")

    def test_credit_card_redaction(self):
        text = "My number is 4111 2222 3333 4444 please call"
        redacted = TextCleaner.redact_credit_cards(text)
        self.assertIn("[CARD_REDACTED]", redacted)
        self.assertNotIn("4111 2222 3333 4444", redacted)

    def test_date_parser(self):
        epoch = 1700000000000
        iso = DateParser.epoch_ms_to_iso(epoch)
        self.assertIn("2023", iso)

        parsed = DateParser.parse_iso(iso)
        self.assertEqual(parsed, epoch)

    def test_normalizer_notification(self):
        notif = NotificationRecord(
            id="n1",
            source_connector="listener",
            package_name="com.whatsapp",
            title="   Test Title   ",
            text="   Some   message with 4111-2222-3333-4444   ",
            timestamp_ms=1700000000000
        )
        normalized = Normalizer.normalize(notif)
        self.assertEqual(normalized.title, "Test Title")
        self.assertIn("[CARD_REDACTED]", normalized.text)
        self.assertIsNotNone(normalized.created_at)

    def test_normalizer_contact(self):
        contact = ContactRecord(
            id="c1",
            source_connector="contacts",
            display_name="  Alice Smith  ",
            phone_numbers=["+1 (555) 019-2834", "987-654-3210"],
            email_addresses=[" ALICE@EXAMPLE.COM "]
        )
        normalized = Normalizer.normalize(contact)
        self.assertEqual(normalized.display_name, "Alice Smith")
        self.assertEqual(normalized.phone_numbers, ["+15550192834", "9876543210"])
        self.assertEqual(normalized.email_addresses, ["alice@example.com"])


if __name__ == "__main__":
    unittest.main()
