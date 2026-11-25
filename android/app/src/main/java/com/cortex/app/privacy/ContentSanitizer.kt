package com.cortex.app.privacy

import java.util.regex.Pattern

object ContentSanitizer {

    private val FINANCIAL_CONTENT_PATTERNS = listOf(
        Pattern.compile("\\b(OTP|one[-\\s]time\\s+password|security\\s+code)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(UPI\\s+PIN|ATM\\s+PIN|CVV|CVC)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(debited|credited|transferred|withdrawn|deposited)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(INR|Rs\\.?|₹|\\$|USD|EUR)\\s*[\\d,]+(\\.\\d{2})?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(account\\s+balance|avail(able)?\\s+bal(ance)?)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(a/c\\s+(no\\.?|ending)|acct\\s+ending|card\\s+ending\\s+in)\\s*(\\d{4}|x+\\d{4})\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(credit\\s+card\\s+statement|minimum\\s+amount\\s+due)\\b", Pattern.CASE_INSENSITIVE)
    )

    private val FINANCIAL_SENDER_PATTERNS = listOf(
        Pattern.compile("alerts?@", Pattern.CASE_INSENSITIVE),
        Pattern.compile("noreply@.*bank.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*@.*(hdfcbank|icicibank|sbi|axisbank|kotak|paytm|phonepe|cred|zerodha|groww)\\.com", Pattern.CASE_INSENSITIVE)
    )

    fun containsFinancialContent(text: String?, sender: String? = null): Boolean {
        if (!sender.isNullOrBlank()) {
            val sLower = sender.trim().lowercase()
            if (FINANCIAL_SENDER_PATTERNS.any { it.matcher(sLower).find() }) {
                return true
            }
        }

        if (text.isNullOrBlank()) return false

        return FINANCIAL_CONTENT_PATTERNS.any { it.matcher(text).find() }
    }
}
