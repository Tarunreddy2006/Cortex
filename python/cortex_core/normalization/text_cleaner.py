"""
Cortex Core - Text Cleaning & Redaction
"""

import re
from typing import Optional


# Regex patterns for removing or redacting sensitive tokens if encountered
CREDIT_CARD_PATTERN = re.compile(r"\b(?:\d{4}[ -]?){3}\d{4}\b")
PHONE_TOKEN_PATTERN = re.compile(r"\b(?:\+?\d{1,3}[ -]?)?\(?\d{3}\)?[ -]?\d{3}[ -]?\d{4}\b")
WHITESPACE_PATTERN = re.compile(r"\s+")


class TextCleaner:
    """Sanitizes text, removes redundant whitespace, and strips formatting artifacts."""

    @staticmethod
    def clean(text: Optional[str]) -> str:
        """Strip excess whitespace and non-printable control characters."""
        if not text:
            return ""
        # Remove null characters and non-printable control chars except \n and \t
        sanitized = "".join(ch for ch in text if ch in ("\n", "\t") or (ch.isprintable() and ord(ch) >= 32))
        # Collapse multi-spaces while keeping single spaces
        lines = [WHITESPACE_PATTERN.sub(" ", line).strip() for line in sanitized.splitlines()]
        return "\n".join(line for line in lines if line)

    @staticmethod
    def redact_credit_cards(text: str) -> str:
        """Redact possible card numbers if passed in non-financial channels."""
        return CREDIT_CARD_PATTERN.sub("[CARD_REDACTED]", text)

    @staticmethod
    def truncate(text: str, max_chars: int = 2000) -> str:
        """Ensure bounded text size for storage efficiency."""
        if len(text) <= max_chars:
            return text
        return text[:max_chars] + "..."
