"""
Memory Reasoner for Cortex
Discovers related memories, detects conflicting facts, detects duplicates,
identifies stale memories, and facilitates memory consolidation.
"""
from typing import Dict, Any, List, Set, Tuple
import time


class MemoryReasoner:

    @staticmethod
    def _tokenize(text: str) -> Set[str]:
        words = set((text or "").lower().split())
        return {w.strip(".,!?:;\"'()[]{}") for w in words if len(w) > 2}

    @classmethod
    def jaccard_similarity(cls, text_a: str, text_b: str) -> float:
        set_a = cls._tokenize(text_a)
        set_b = cls._tokenize(text_b)
        if not set_a or not set_b:
            return 0.0
        intersection = len(set_a.intersection(set_b))
        union = len(set_a.union(set_b))
        return intersection / union if union > 0 else 0.0

    @classmethod
    def find_related_memories(cls, target_memory: Dict[str, Any], all_memories: List[Dict[str, Any]], threshold: float = 0.25) -> List[Dict[str, Any]]:
        """
        Finds memories related by content similarity, shared tags, or category.
        """
        target_id = target_memory.get("id")
        target_content = target_memory.get("content", "")
        target_tags = set(target_memory.get("tags", []))
        target_category = target_memory.get("category", "")

        related: List[Dict[str, Any]] = []

        for m in all_memories:
            if m.get("id") == target_id:
                continue

            sim = cls.jaccard_similarity(target_content, m.get("content", ""))
            tag_overlap = len(target_tags.intersection(set(m.get("tags", [])))) if target_tags else 0
            cat_match = 1 if m.get("category") == target_category and target_category else 0

            combined_score = sim * 0.6 + (0.2 if tag_overlap > 0 else 0.0) + (0.2 if cat_match else 0.0)

            if combined_score >= threshold:
                related.append({
                    "id": m.get("id"),
                    "title": m.get("title") or m.get("category"),
                    "snippet": m.get("content", "")[:120],
                    "category": m.get("category"),
                    "similarity": round(combined_score, 2)
                })

        related.sort(key=lambda x: x["similarity"], reverse=True)
        return related[:6]

    @classmethod
    def detect_duplicates(cls, all_memories: List[Dict[str, Any]], threshold: float = 0.65) -> List[Dict[str, Any]]:
        """
        Identifies duplicate or near-duplicate memories for user cleanup.
        """
        duplicates: List[Dict[str, Any]] = []
        n = len(all_memories)

        for i in range(n):
            for j in range(i + 1, n):
                mem_a = all_memories[i]
                mem_b = all_memories[j]
                sim = cls.jaccard_similarity(mem_a.get("content", ""), mem_b.get("content", ""))
                if sim >= threshold:
                    duplicates.append({
                        "memory_a": {"id": mem_a.get("id"), "content": mem_a.get("content")[:100]},
                        "memory_b": {"id": mem_b.get("id"), "content": mem_b.get("content")[:100]},
                        "similarity": round(sim, 2),
                        "recommendation": "Merge or remove older duplicate entry"
                    })
        return duplicates

    @classmethod
    def detect_conflicts(cls, all_memories: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Detects conflicting personal statements or dates.
        Example: 'Exam is on Monday' vs 'Exam is on Wednesday'.
        """
        conflicts: List[Dict[str, Any]] = []
        contradiction_pairs = [
            (r"\bmonday\b", r"\bwednesday\b"),
            (r"\bmonday\b", r"\bfriday\b"),
            (r"\btuesday\b", r"\bthursday\b"),
            (r"\bcancelled\b", r"\bconfirmed\b"),
            (r"\bpostponed\b", r"\bon\s*time\b"),
            (r"\bfail(ed)?\b", r"\bpass(ed)?\b"),
        ]

        import re
        n = len(all_memories)
        for i in range(n):
            for j in range(i + 1, n):
                mem_a = all_memories[i]
                mem_b = all_memories[j]

                # Only check if they share a common subject or context
                sim = cls.jaccard_similarity(mem_a.get("content", ""), mem_b.get("content", ""))
                if sim > 0.2:
                    text_a = mem_a.get("content", "").lower()
                    text_b = mem_b.get("content", "").lower()

                    for pat1, pat2 in contradiction_pairs:
                        if (re.search(pat1, text_a) and re.search(pat2, text_b)) or (re.search(pat2, text_a) and re.search(pat1, text_b)):
                            conflicts.append({
                                "conflict_type": "CONTRADICTION",
                                "item_a": {"id": mem_a.get("id"), "content": mem_a.get("content")},
                                "item_b": {"id": mem_b.get("id"), "content": mem_b.get("content")},
                                "explanation": f"Conflict detected between opposing terms ('{pat1.strip(r'\\b')}' vs '{pat2.strip(r'\\b')}') in related context.",
                                "requires_user_confirmation": True
                            })
        return conflicts

    @classmethod
    def identify_stale_memories(cls, all_memories: List[Dict[str, Any]], days_threshold: int = 90) -> List[Dict[str, Any]]:
        """
        Finds memories unreviewed or unmodified for over days_threshold.
        """
        stale: List[Dict[str, Any]] = []
        now_ms = int(time.time() * 1000)
        threshold_ms = days_threshold * 86400 * 1000

        for m in all_memories:
            updated_at = m.get("updated_at") or m.get("created_at") or 0
            if (now_ms - updated_at) > threshold_ms and not m.get("is_pinned"):
                days_old = int((now_ms - updated_at) / (86400 * 1000))
                stale.append({
                    "id": m.get("id"),
                    "title": m.get("title") or m.get("category"),
                    "snippet": m.get("content", "")[:100],
                    "days_inactive": days_old,
                    "category": m.get("category")
                })
        return stale
