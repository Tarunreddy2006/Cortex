"""
Cortex Core - Base Record Model
"""

from dataclasses import dataclass, asdict, field
from datetime import datetime, timezone
from typing import Dict, Any, Optional
import json


@dataclass
class BaseRecord:
    """Base class for all Cortex normalized records."""
    id: str
    source_connector: str
    created_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    metadata: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        """Convert record to dictionary."""
        return asdict(self)

    def to_json(self) -> str:
        """Serialize record to JSON."""
        return json.dumps(self.to_dict(), ensure_ascii=False)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]):
        """Construct record from dictionary."""
        return cls(**data)
