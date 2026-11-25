"""
Cortex Core - Personal Memory Engine
Phase 2: Memory classification, filtering, and tag extraction.
"""

import hashlib
import re
from typing import List, Optional, Tuple, Dict, Any
from cortex_core.models.memory import MemoryRecord, MemoryCategory, ConfidenceStatus, ApprovalStatus
from cortex_core.validation.financial_validator import FinancialExclusionValidator


ACADEMIC_KEYWORDS = {
    "exam", "examination", "assignment", "homework", "syllabus", "coursework",
    "lecture", "professor", "semester", "grade", "gpa", "classroom", "curriculum",
    "laboratory", "quiz", "deadline", "university", "college", "faculty"
}

PROJECT_KEYWORDS = {
    "project", "milestone", "repository", "commit", "sprint", "feature",
    "backend", "frontend", "api", "deployment", "bug", "refactor",
    "pull request", "framework", "release", "technical decision"
}

EPISODIC_KEYWORDS = {
    "meeting", "appointment", "discussion", "visited", "call with", "met with",
    "interview", "conference", "presentation", "session", "scheduled for"
}

KNOWLEDGE_KEYWORDS = {
    "research", "paper", "algorithm", "definition", "concept", "theory",
    "formula", "analysis", "summary", "article", "book", "documentation"
}

TRANSIENT_WORDS = {
    "ok", "k", "okay", "yes", "no", "thanks", "thank you", "cool",
    "great", "nice", "done", "lol", "haha", "fine", "yep", "nope"
}

TRANSIENT_PATTERNS = [
    re.compile(r"\b(otp|verification code|security code)\b", re.IGNORECASE),
    re.compile(r"\b(download now|click here|limited time offer|sale|discount)\b", re.IGNORECASE)
]


class MemoryEngine:
    """Intelligent memory classifier and extraction engine."""

    @staticmethod
    def is_transient_noise(text: str) -> bool:
        """Determines if text is ephemeral chatter or promotional noise."""
        clean = text.strip()
        stripped = re.sub(r"^[^\w]+|[^\w]+$", "", clean).lower()
        if stripped in TRANSIENT_WORDS:
            return True
        if len(clean) < 4 and not any(c.isalnum() for c in clean):
            return True
        for pattern in TRANSIENT_PATTERNS:
            if pattern.search(clean):
                return True
        return False

    @classmethod
    def classify_category(cls, text: str) -> MemoryCategory:
        """
        Classify memory content into one of 5 canonical categories
        using weighted keyword density.
        """
        lower = text.lower()
        tokens = set(re.findall(r"\b[a-z]{3,}\b", lower))

        scores = {
            MemoryCategory.ACADEMIC: len(tokens & ACADEMIC_KEYWORDS),
            MemoryCategory.PROJECT: len(tokens & PROJECT_KEYWORDS),
            MemoryCategory.EPISODIC: len(tokens & EPISODIC_KEYWORDS),
            MemoryCategory.KNOWLEDGE: len(tokens & KNOWLEDGE_KEYWORDS),
        }

        best_category = max(scores, key=lambda k: scores[k])
        if scores[best_category] > 0:
            return best_category
        return MemoryCategory.PERSONAL

    @classmethod
    def extract_tags(cls, text: str, max_tags: int = 5) -> List[str]:
        """Extract meaningful keyword tags from content."""
        words = re.findall(r"\b[a-zA-Z]{4,}\b", text)
        stop_words = {
            "this", "that", "with", "from", "have", "were", "what", "when",
            "where", "your", "them", "then", "there", "their", "some", "more",
            "will", "about", "would", "should", "could"
        }
        filtered = [w.lower() for w in words if w.lower() not in stop_words]
        counts: Dict[str, int] = {}
        for w in filtered:
            counts[w] = counts.get(w, 0) + 1

        sorted_tags = sorted(counts.items(), key=lambda x: x[1], reverse=True)
        return [tag for tag, _ in sorted_tags[:max_tags]]

    @classmethod
    def process_incoming_content(
        cls,
        content: str,
        source_connector: str,
        source_record_id: Optional[str] = None,
        is_user_explicit: bool = False
    ) -> Tuple[Optional[MemoryRecord], bool, str]:
        """
        Processes content into a memory record.
        Strictly enforces financial exclusions and filters out transient chatter.
        """
        # Step 1: Strict financial exclusion check
        is_fin, reason = FinancialExclusionValidator.contains_financial_content(content)
        if is_fin:
            return None, False, f"Memory rejected: Financial policy triggered ({reason})"

        # Step 2: Transient noise filter (for automated sources)
        if not is_user_explicit and cls.is_transient_noise(content):
            return None, False, "Memory rejected: Transient noise / non-informative content"

        # Step 3: Classify category & extract tags
        category = cls.classify_category(content)
        tags = cls.extract_tags(content)

        confidence = (
            ConfidenceStatus.USER_EXPLICIT if is_user_explicit
            else ConfidenceStatus.EXTRACTED
        )

        content_hash = hashlib.sha256(content.strip().encode("utf-8")).hexdigest()
        mem_id = f"mem_{content_hash[:16]}"

        record = MemoryRecord(
            id=mem_id,
            source_connector=source_connector,
            content=content.strip(),
            category=category,
            confidence_status=confidence,
            approval_status=ApprovalStatus.APPROVED,
            source_record_id=source_record_id,
            tags=tags
        )

        return record, True, "Memory created and categorized"
