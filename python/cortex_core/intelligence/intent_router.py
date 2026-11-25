"""
Intent Router for Cortex
Classifies natural-language user queries into domain categories for targeted context retrieval.
"""
import re
from enum import Enum
from typing import Dict, Any, List, Tuple


class IntentType(str, Enum):
    ACADEMIC = "ACADEMIC"
    PROJECT = "PROJECT"
    LIFE_PLAN = "LIFE_PLAN"
    BRIEFING = "BRIEFING"
    DECISION = "DECISION"
    KNOWLEDGE_RAG = "KNOWLEDGE_RAG"
    ACTION = "ACTION"
    GENERAL = "GENERAL"


class IntentRouter:
    """
    Fast, deterministic intent classifier using keyword density, regex patterns,
    and conversational context. Works 100% offline with zero cloud dependency.
    """

    PATTERNS: Dict[IntentType, List[str]] = {
        IntentType.ACADEMIC: [
            r"\bclass(es)?\b", r"\btimetable\b", r"\bexam(s|ination)?\b", r"\bassignment(s)?\b",
            r"\bsubject(s)?\b", r"\bstudy\b", r"\brevis(e|ion)\b", r"\bsyllabus\b",
            r"\bprofessor\b", r"\bfaculty\b", r"\bsemester\b", r"\bgpa\b", r"\bcourse(work)?\b",
            r"\blecture(s)?\b", r"\bcollege\b", r"\buniversity\b"
        ],
        IntentType.PROJECT: [
            r"\bproject(s)?\b", r"\bmilestone(s)?\b", r"\bdecision(s)?\b", r"\bblocker(s)?\b",
            r"\btech\s*stack\b", r"\barchitecture\b", r"\bseleneos\b", r"\bcortex\b",
            r"\bwork\s*on\s*next\b", r"\bcode\b", r"\bgithub\b", r"\bfeature(s)?\b", r"\bdev\b",
            r"\b(complete|finish|do)\s*(first|next)\b", r"\bwhat\s*(should\s*i\s*)?(complete|do|work\s*on)\s*(first|next)\b"
        ],
        IntentType.LIFE_PLAN: [
            r"\bplan\s*(my\s*day|today|tomorrow|week)?\b", r"\bwhat\s*am\s*i\s*forgetting\b",
            r"\btask(s)?\b", r"\bpriorit(y|ies)\b", r"\bto[- ]?do\b", r"\boverdue\b",
            r"\breschedul(e|ing)\b", r"\bcannot\s*complete\b", r"\bdeadline(s)?\b",
            r"\bappointment(s)?\b", r"\bschedule\b"
        ],
        IntentType.BRIEFING: [
            r"\bmorning\s*briefing\b", r"\bevening\s*briefing\b", r"\bweekly\s*(briefing|summary)\b",
            r"\bsummarize\s*(my\s*day|today|my\s*week|the\s*week)\b", r"\bdaily\s*overview\b",
            r"\bbrief\s*me\b"
        ],
        IntentType.DECISION: [
            r"\bshould\s*i\b", r"\bevaluate\s*(this)?\b", r"\bcompare\b", r"\bpros\s*and\s*cons\b",
            r"\badvantages?\b", r"\bdisadvantages?\b", r"\brisk(s)?\b", r"\bwhich\s*(one|skill|option)\b",
            r"\bdecision\b", r"\brecommend(ation)?\b"
        ],
        IntentType.KNOWLEDGE_RAG: [
            r"\bfind\s*(my\s*)?note(s)?\b", r"\bdoc(ument)?(s)?\b", r"\bresearch\s*paper\b",
            r"\bpdf\b", r"\bexplain\b", r"\bwhat\s*did\s*i\s*(learn|save)\b",
            r"\bcollection(s)?\b", r"\bnotes?\s*about\b"
        ],
        IntentType.ACTION: [
            r"\bcreate\s*(a\s*)?(task|reminder|event|goal)\b", r"\bremind\s*me\s*to\b",
            r"\badd\s*(a\s*)?(task|note|memory|exam|assignment)\b", r"\bschedule\s*a\b"
        ]
    }

    @classmethod
    def classify(cls, query: str, conversation_context: List[Dict[str, Any]] = None) -> Tuple[IntentType, float]:
        """
        Classifies user query and returns (IntentType, confidence_score 0.0 - 1.0).
        Considers conversation context for follow-up questions.
        """
        text = query.lower().strip()
        if not text:
            return IntentType.GENERAL, 0.0

        scores: Dict[IntentType, int] = {intent: 0 for intent in IntentType}

        for intent, patterns in cls.PATTERNS.items():
            for pat in patterns:
                matches = len(re.findall(pat, text))
                if matches > 0:
                    scores[intent] += matches * 2

        # Contextual inheritance for follow-up queries
        if conversation_context:
            last_message = conversation_context[-1] if len(conversation_context) > 0 else None
            if last_message:
                prev_intent_name = last_message.get("intent")
                if prev_intent_name and prev_intent_name in IntentType.__members__:
                    prev_intent = IntentType[prev_intent_name]
                    # If follow-up mentions priority/sequence words, boost previous intent
                    if any(w in text for w in ["first", "next", "priorit", "complete", "then", "more", "why"]):
                        scores[prev_intent] += 5
                    elif max(scores.values()) == 0:
                        return prev_intent, 0.75

        best_intent = max(scores, key=lambda k: scores[k])
        max_score = scores[best_intent]

        if max_score == 0:
            return IntentType.GENERAL, 0.5

        confidence = min(1.0, 0.5 + (max_score * 0.15))
        return best_intent, confidence
