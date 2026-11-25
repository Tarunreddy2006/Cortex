"""
Cortex Core - Application Record Model
"""

from dataclasses import dataclass, field
from enum import Enum
from typing import List, Optional
from cortex_core.models.base import BaseRecord


class AppCategory(str, Enum):
    GOOGLE = "GOOGLE"
    COMMUNICATION = "COMMUNICATION"
    EDUCATION = "EDUCATION"
    PRODUCTIVITY = "PRODUCTIVITY"
    SOCIAL = "SOCIAL"
    BROWSERS = "BROWSERS"
    OTHER = "OTHER"
    FINANCIAL = "FINANCIAL"


@dataclass
class ApplicationRecord(BaseRecord):
    """Represents an installed application discovered on the device."""
    package_name: str = ""
    app_name: str = ""
    category: AppCategory = AppCategory.OTHER
    is_system_app: bool = False
    is_enabled: bool = False
    supported_capabilities: List[str] = field(default_factory=list)
    version_name: Optional[str] = None
    version_code: Optional[int] = None
    installed_at: Optional[str] = None
    updated_at: Optional[str] = None
    icon_uri: Optional[str] = None
