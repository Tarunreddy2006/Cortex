"""
Unit tests for Financial Exclusion Policy and Validator
Non-negotiable requirement: Financial apps and content must NEVER enter storage.
"""

import unittest
from cortex_core.validation.financial_validator import (
    FinancialExclusionValidator,
    PERMANENT_FINANCIAL_PACKAGES,
)
from cortex_core.models.notification import NotificationRecord
from cortex_core.models.email import EmailRecord


class TestFinancialExclusion(unittest.TestCase):

    def test_permanent_denylist_contains_core_apps(self):
        # UPI apps
        self.assertTrue(FinancialExclusionValidator.is_financial_package("com.google.android.apps.nbu.paisa.user"))
        self.assertTrue(FinancialExclusionValidator.is_financial_package("com.phonepe.app"))
        self.assertTrue(FinancialExclusionValidator.is_financial_package("net.one97.paytm"))
        self.assertTrue(FinancialExclusionValidator.is_financial_package("in.org.npci.upiapp"))
        self.assertTrue(FinancialExclusionValidator.is_financial_package("com.dreamplug.androidapp"))

        # Banking
        self.assertTrue(FinancialExclusionValidator.is_financial_package("com.snapwork.hdfc"))
        self.assertTrue(FinancialExclusionValidator.is_financial_package("com.sbi.lotusintouch"))
        self.assertTrue(FinancialExclusionValidator.is_financial_package("com.csam.icici.bank.imobile"))
        self.assertTrue(FinancialExclusionValidator.is_financial_package("com.chase.sig.android"))

        # Investments & Crypto
        self.assertTrue(FinancialExclusionValidator.is_financial_package("com.zerodha.kite3"))
        self.assertTrue(FinancialExclusionValidator.is_financial_package("com.nextbillion.groww"))
        self.assertTrue(FinancialExclusionValidator.is_financial_package("com.binance.dev"))

    def test_custom_user_denylist(self):
        custom_set = {"com.custom.secretcrypto", "org.personal.wallet"}
        self.assertTrue(FinancialExclusionValidator.is_financial_package("com.custom.secretcrypto", custom_set))
        self.assertTrue(FinancialExclusionValidator.is_financial_package("org.personal.wallet", custom_set))
        self.assertFalse(FinancialExclusionValidator.is_financial_package("com.whatsapp", custom_set))

    def test_package_heuristic_pattern(self):
        self.assertTrue(FinancialExclusionValidator.is_financial_package("com.myregional.banking.app"))
        self.assertTrue(FinancialExclusionValidator.is_financial_package("org.local.netbanking"))

    def test_financial_content_rejection(self):
        # Transaction alerts
        fin_text_1 = "Your A/C ending 4589 debited by INR 2,500.00 on 24-Nov-26 via UPI"
        is_fin, reason = FinancialExclusionValidator.contains_financial_content(fin_text_1)
        self.assertTrue(is_fin)

        # OTP alerts
        fin_text_2 = "Your OTP is 492011 for transaction of Rs. 400 at Swiggy. Do not share."
        is_fin, reason = FinancialExclusionValidator.contains_financial_content(fin_text_2)
        self.assertTrue(is_fin)

        # ATM PIN / CVV
        fin_text_3 = "Never disclose your ATM PIN or CVV to anyone."
        is_fin, reason = FinancialExclusionValidator.contains_financial_content(fin_text_3)
        self.assertTrue(is_fin)

        # Non-financial text must pass cleanly
        clean_text = "Hey, let's meet at the library tomorrow at 3 PM to work on our presentation."
        is_fin, reason = FinancialExclusionValidator.contains_financial_content(clean_text)
        self.assertFalse(is_fin)

    def test_sender_financial_rejection(self):
        is_fin, _ = FinancialExclusionValidator.contains_financial_content(
            "Important statement update",
            sender="alerts@hdfcbank.com"
        )
        self.assertTrue(is_fin)

    def test_validate_for_ingestion(self):
        # Financial notification rejected
        bad_notif = NotificationRecord(
            id="n1",
            source_connector="listener",
            package_name="com.phonepe.app",
            app_name="PhonePe",
            title="Payment Successful",
            text="Paid Rs 150 to Coffee Shop",
            timestamp_ms=1700000000000
        )
        allowed, reason = FinancialExclusionValidator.validate_for_ingestion(bad_notif)
        self.assertFalse(allowed)

        # Clean notification accepted
        good_notif = NotificationRecord(
            id="n2",
            source_connector="listener",
            package_name="com.whatsapp",
            app_name="WhatsApp",
            title="Study Group",
            text="Don't forget the assignment due tomorrow",
            timestamp_ms=1700000000000
        )
        allowed, reason = FinancialExclusionValidator.validate_for_ingestion(good_notif)
        self.assertTrue(allowed)


if __name__ == "__main__":
    unittest.main()
