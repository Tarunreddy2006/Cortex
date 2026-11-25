"""
Cortex Core - WhatsApp Exported Chat Processor
Parses exported .txt chats from WhatsApp or Telegram.
Extracts conversations, participants, timestamps, and action items.
Does not access WhatsApp's private sandbox or database.
"""

import re
from typing import List, Dict, Any, Optional, Tuple
from cortex_core.models.base import BaseRecord
from cortex_core.validation.financial_validator import FinancialExclusionValidator


# Formats supported:
# Format A: [15/01/24, 14:32:05] Name: Message
# Format B: 15/01/2024, 14:32 - Name: Message
# Format C: 1/15/24, 2:32 PM - Name: Message
WHATSAPP_PATTERN_A = re.compile(r"^\[(\d{1,2}/\d{1,2}/\d{2,4}),?\s+(\d{1,2}:\d{2}(?::\d{2})?(?:\s*[APap][Mm])?)\]\s+([^:]+):\s+(.*)$")
WHATSAPP_PATTERN_B = re.compile(r"^(\d{1,2}/\d{1,2}/\d{2,4}),?\s+(\d{1,2}:\d{2}(?:\s*[APap][Mm])?)\s+-\s+([^:]+):\s+(.*)$")


class WhatsAppExportProcessor:
    """Safely parses exported chat transcripts into structured message records."""

    @classmethod
    def parse_chat_text(cls, chat_text: str, source_file_name: str = "chat.txt") -> Dict[str, Any]:
        """
        Parses full export text.
        Returns summary dictionary containing:
        - total_messages
        - participants
        - sanitized_messages (excluding financial alerts or OTPs)
        - extracted_tasks_or_dates
        """
        lines = chat_text.splitlines()
        messages: List[Dict[str, Any]] = []
        participants = set()
        extracted_tasks: List[str] = []
        skipped_financial = 0

        current_msg: Optional[Dict[str, Any]] = None

        for line in lines:
            line = line.strip()
            if not line:
                continue

            # Skip system encryption messages
            if "Messages and calls are end-to-end encrypted" in line:
                continue

            match = WHATSAPP_PATTERN_A.match(line) or WHATSAPP_PATTERN_B.match(line)
            if match:
                if current_msg:
                    # Finalize previous message
                    is_fin, _ = FinancialExclusionValidator.contains_financial_content(current_msg["text"])
                    if is_fin:
                        skipped_financial += 1
                    else:
                        messages.append(current_msg)

                date_str, time_str, sender, text = match.groups()
                sender = sender.strip()
                participants.add(sender)

                current_msg = {
                    "date": date_str,
                    "time": time_str,
                    "sender": sender,
                    "text": text.strip()
                }

                # Check for possible tasks or dates
                if any(w in text.lower() for w in ["tomorrow", "meet", "due", "submit", "call me", "deadline"]):
                    extracted_tasks.append(f"{sender}: {text.strip()}")
            else:
                # Continuation line of previous message
                if current_msg:
                    current_msg["text"] += f"\n{line}"

        if current_msg:
            is_fin, _ = FinancialExclusionValidator.contains_financial_content(current_msg["text"])
            if is_fin:
                skipped_financial += 1
            else:
                messages.append(current_msg)

        return {
            "source_file": source_file_name,
            "total_messages": len(messages),
            "skipped_financial_messages": skipped_financial,
            "participants": sorted(list(participants)),
            "extracted_tasks": extracted_tasks[:50],  # top 50 tasks
            "sample_recent_messages": messages[-10:] if messages else []
        }
