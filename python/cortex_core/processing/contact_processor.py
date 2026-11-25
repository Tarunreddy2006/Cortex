"""
Cortex Core - Contact Processing Pipeline
"""

from typing import List, Optional, Tuple
from cortex_core.models.contact import ContactRecord
from cortex_core.normalization.normalizer import Normalizer


class ContactProcessor:
    """Processes contact records retrieved from Android Contacts Provider."""

    @classmethod
    def process(
        cls,
        contact_id: str,
        lookup_key: str,
        display_name: str,
        phone_numbers: List[str],
        email_addresses: List[str],
        organization: Optional[str] = None,
        title: Optional[str] = None,
        is_starred: bool = False
    ) -> Tuple[Optional[ContactRecord], bool, str]:
        if not display_name and not phone_numbers and not email_addresses:
            return None, False, "Contact has no identifying information"

        record = ContactRecord(
            id=f"contact_{contact_id}",
            source_connector="contacts_provider",
            contact_id=contact_id,
            lookup_key=lookup_key,
            display_name=display_name or "Unnamed Contact",
            phone_numbers=phone_numbers or [],
            email_addresses=email_addresses or [],
            organization=organization,
            title=title,
            is_starred=is_starred
        )

        normalized = Normalizer.normalize(record)
        return normalized, True, "Accepted and normalized"
