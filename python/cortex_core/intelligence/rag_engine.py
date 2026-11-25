"""
Local-First Retrieval-Augmented Generation (RAG) Engine
Indexes, ranks, and retrieves local context from memories, notes, documents,
academic records, tasks, and projects with explicit source attribution.
"""
import math
import re
from typing import Dict, Any, List, Optional
from cortex_core.validation.financial_validator import FinancialExclusionValidator


class LocalRAGEngine:
    """
    Offline BM25-based semantic and lexical retrieval engine.
    Calculates TF-IDF and term-saturation relevance across stored personal records.
    Never returns or processes excluded financial content.
    """

    STOPWORDS = {
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
        "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but",
        "by", "could", "did", "do", "does", "doing", "down", "during", "each", "few", "for", "from",
        "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself", "him",
        "himself", "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself", "just",
        "me", "more", "most", "my", "myself", "no", "nor", "not", "now", "of", "off", "on", "once",
        "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same",
        "she", "should", "so", "some", "such", "than", "that", "the", "their", "theirs", "them",
        "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too",
        "under", "until", "up", "very", "was", "we", "were", "what", "when", "where", "which",
        "while", "who", "whom", "why", "with", "would", "you", "your", "yours", "yourself",
        "yourselves", "tell", "show", "find", "give"
    }

    @classmethod
    def tokenize(cls, text: str) -> List[str]:
        words = re.findall(r"\b[a-zA-Z0-9_\-\.]{2,}\b", (text or "").lower())
        return [w for w in words if w not in cls.STOPWORDS]

    @classmethod
    def score_document(cls, query_tokens: List[str], doc_tokens: List[str], doc_len: int, avg_doc_len: float) -> float:
        """
        BM25-style term-frequency with document length normalization.
        k1 = 1.2, b = 0.75
        """
        if not query_tokens or not doc_tokens:
            return 0.0

        k1 = 1.2
        b = 0.75
        tf_map: Dict[str, int] = {}
        for token in doc_tokens:
            tf_map[token] = tf_map.get(token, 0) + 1

        score = 0.0
        norm_len = (1.0 - b) + b * (doc_len / (avg_doc_len if avg_doc_len > 0 else 1.0))

        for qt in query_tokens:
            if qt in tf_map:
                tf = tf_map[qt]
                # BM25 tf saturation
                term_score = (tf * (k1 + 1.0)) / (tf + k1 * norm_len)
                score += term_score

        return score

    @classmethod
    def retrieve(
        cls,
        query: str,
        memories: List[Dict[str, Any]] = None,
        notes: List[Dict[str, Any]] = None,
        documents: List[Dict[str, Any]] = None,
        tasks: List[Dict[str, Any]] = None,
        projects: List[Dict[str, Any]] = None,
        academics: Dict[str, Any] = None,
        max_results: int = 8
    ) -> Dict[str, Any]:
        """
        Executes unified search and contextual ranking across all personal items.
        Returns ranked chunks with source citations and metadata.
        """
        query_tokens = cls.tokenize(query)
        if not query_tokens:
            return {"results": [], "total_found": 0, "has_context": False}

        candidates: List[Dict[str, Any]] = []

        # 1. Memories
        for m in (memories or []):
            content = m.get("content", "")
            # Privacy gate
            is_fin, _ = FinancialExclusionValidator.contains_financial_content(content)
            if is_fin or FinancialExclusionValidator.is_financial_package(m.get("source_connector", "")):
                continue

            doc_tokens = cls.tokenize(content + " " + m.get("title", "") + " " + str(m.get("tags", [])))
            score = cls.score_document(query_tokens, doc_tokens, len(doc_tokens), 30.0)
            if m.get("is_pinned"):
                score *= 1.3  # Pinned memory boost
            if score > 0.3:
                candidates.append({
                    "id": m.get("id"),
                    "source_type": "MEMORY",
                    "title": m.get("title") or f"{m.get('category', 'PERSONAL')} Memory",
                    "snippet": content[:220] + ("..." if len(content) > 220 else ""),
                    "full_text": content,
                    "category": m.get("category", "PERSONAL"),
                    "source_reference": f"Memory ({m.get('category', 'PERSONAL')}) #{m.get('id', '')[:8]}",
                    "score": round(score, 3)
                })

        # 2. Notes
        for n in (notes or []):
            content = n.get("content", "")
            title = n.get("title", "Untitled Note")
            doc_tokens = cls.tokenize(title + " " + content)
            score = cls.score_document(query_tokens, doc_tokens, len(doc_tokens), 50.0)
            if score > 0.3:
                candidates.append({
                    "id": n.get("id"),
                    "source_type": "NOTE",
                    "title": title,
                    "snippet": content[:220] + ("..." if len(content) > 220 else ""),
                    "full_text": content,
                    "category": "KNOWLEDGE",
                    "source_reference": f"Note: '{title}'",
                    "score": round(score, 3)
                })

        # 3. Documents
        for d in (documents or []):
            text = d.get("extracted_text", "")
            title = d.get("title") or d.get("file_name", "Document")
            doc_tokens = cls.tokenize(title + " " + text)
            score = cls.score_document(query_tokens, doc_tokens, len(doc_tokens), 150.0)
            if score > 0.3:
                page_ref = f", Page {d.get('page_number', 1)}" if d.get("page_number") else ""
                candidates.append({
                    "id": d.get("id"),
                    "source_type": "DOCUMENT",
                    "title": title,
                    "snippet": text[:240] + ("..." if len(text) > 240 else ""),
                    "full_text": text,
                    "category": "KNOWLEDGE",
                    "source_reference": f"Document: '{title}'{page_ref}",
                    "score": round(score, 3)
                })

        # 4. Tasks
        for t in (tasks or []):
            title = t.get("title", "")
            desc = t.get("description", "")
            doc_tokens = cls.tokenize(title + " " + desc)
            score = cls.score_document(query_tokens, doc_tokens, len(doc_tokens), 20.0)
            if score > 0.3:
                status_str = t.get("status", "PENDING")
                candidates.append({
                    "id": t.get("id"),
                    "source_type": "TASK",
                    "title": title,
                    "snippet": f"Status: {status_str} | Priority: {t.get('priority', 'MEDIUM')} | {desc}".strip(),
                    "full_text": f"{title} - {desc}",
                    "category": t.get("category", "PERSONAL"),
                    "source_reference": f"Task: '{title}' ({status_str})",
                    "score": round(score, 3)
                })

        # 5. Projects
        for p in (projects or []):
            name = p.get("name", "")
            desc = p.get("description", "")
            obj = p.get("objective", "")
            stack = str(p.get("tech_stack", []))
            doc_tokens = cls.tokenize(name + " " + desc + " " + obj + " " + stack)
            score = cls.score_document(query_tokens, doc_tokens, len(doc_tokens), 40.0)
            if score > 0.3:
                candidates.append({
                    "id": p.get("id"),
                    "source_type": "PROJECT",
                    "title": name,
                    "snippet": f"Objective: {obj} | Status: {p.get('status', 'ACTIVE')} | Tech: {stack}".strip(),
                    "full_text": f"{name}: {desc} ({obj})",
                    "category": "PROJECT",
                    "source_reference": f"Project: '{name}'",
                    "score": round(score, 3)
                })

        # 6. Academic Items (Subjects, Timetable, Assignments, Exams)
        if academics:
            for s in academics.get("subjects", []):
                s_name = s.get("name", "")
                s_code = s.get("code", "")
                faculty = s.get("faculty", "")
                doc_tokens = cls.tokenize(f"{s_code} {s_name} {faculty}")
                score = cls.score_document(query_tokens, doc_tokens, len(doc_tokens), 20.0)
                if score > 0.3:
                    candidates.append({
                        "id": s.get("id"),
                        "source_type": "SUBJECT",
                        "title": f"{s_code}: {s_name}",
                        "snippet": f"Faculty: {faculty} | Credits: {s.get('credits', 3)}",
                        "full_text": f"{s_code} {s_name} taught by {faculty}",
                        "category": "ACADEMIC",
                        "source_reference": f"Course: {s_code} ({s_name})",
                        "score": round(score, 3)
                    })

            for tt in academics.get("timetable", []):
                s_name = tt.get("subject_name") or tt.get("subject_code", "")
                room = tt.get("room", "")
                time_str = f"{tt.get('start_time')} - {tt.get('end_time')}"
                day_name = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"][min(6, max(0, int(tt.get("day_of_week", 1)) - 1))]
                doc_tokens = cls.tokenize(f"{s_name} {room} {time_str} {day_name} class lecture timetable")
                score = cls.score_document(query_tokens, doc_tokens, len(doc_tokens), 20.0)
                if score > 0.3:
                    candidates.append({
                        "id": tt.get("id"),
                        "source_type": "TIMETABLE",
                        "title": f"{day_name} Class: {s_name}",
                        "snippet": f"Time: {time_str} | Room: {room}",
                        "full_text": f"{day_name} {time_str} in room {room}",
                        "category": "ACADEMIC",
                        "source_reference": f"Timetable ({day_name} {time_str}, Room {room})",
                        "score": round(score, 3)
                    })

            for asgn in academics.get("assignments", []):
                a_title = asgn.get("title", "")
                desc = asgn.get("description", "")
                doc_tokens = cls.tokenize(f"{a_title} {desc} assignment coursework due")
                score = cls.score_document(query_tokens, doc_tokens, len(doc_tokens), 25.0)
                if score > 0.3:
                    candidates.append({
                        "id": asgn.get("id"),
                        "source_type": "ASSIGNMENT",
                        "title": a_title,
                        "snippet": f"Status: {asgn.get('status')} | Priority: {asgn.get('priority')} | {desc}",
                        "full_text": f"{a_title} - {desc}",
                        "category": "ACADEMIC",
                        "source_reference": f"Assignment: '{a_title}'",
                        "score": round(score, 3)
                    })

            for exam in academics.get("examinations", []):
                e_title = exam.get("title", "")
                syl = exam.get("syllabus", "")
                doc_tokens = cls.tokenize(f"{e_title} {syl} exam examination test mid-term")
                score = cls.score_document(query_tokens, doc_tokens, len(doc_tokens), 25.0)
                if score > 0.3:
                    candidates.append({
                        "id": exam.get("id"),
                        "source_type": "EXAMINATION",
                        "title": e_title,
                        "snippet": f"Room: {exam.get('room')} | Duration: {exam.get('duration')} min | Syllabus: {syl}",
                        "full_text": f"{e_title} syllabus: {syl}",
                        "category": "ACADEMIC",
                        "source_reference": f"Exam: '{e_title}'",
                        "score": round(score, 3)
                    })

        # Sort candidates by relevance score descending
        candidates.sort(key=lambda x: x["score"], reverse=True)
        top_results = candidates[:max_results]

        return {
            "results": top_results,
            "total_found": len(candidates),
            "has_context": len(top_results) > 0,
            "citations": [item["source_reference"] for item in top_results]
        }
