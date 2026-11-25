"""
Cortex Core - Contact Record Model
"""

from dataclasses import dataclass, field
from typing import List, Optional
from cortex_core.models.base import BaseRecord


@dataclass
class ContactRecord(BaseRecord):
    """Represents a contact retrieved through Contacts Provider or People API."""
    contact_id: str = ""
    lookup_key: str = ""
    display_name: str = ""
    phone_numbers: List[str] = field(default_factory=list)
    email_addresses: List[str] = field(default_factory=list)
    organization: Optional[str] = None
    title: Optional[str] = None
    is_starred: bool = False
