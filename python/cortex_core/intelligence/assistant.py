"""
Conversational Assistant ("Ask Cortex") Engine
Orchestrates multi-turn conversation context, entity resolution, domain routing,
local RAG retrieval, and factual source-attributed answer generation.
"""
import re
import datetime
from typing import Dict, Any, List, Optional
from cortex_core.intelligence.intent_router import IntentRouter, IntentType
from cortex_core.intelligence.rag_engine import LocalRAGEngine
from cortex_core.intelligence.local_ai_provider import LocalAIProvider
from cortex_core.intelligence.academic_planner import AcademicPlanner
from cortex_core.intelligence.project_reasoner import ProjectReasoner
from cortex_core.intelligence.life_planner import LifePlanner
from cortex_core.intelligence.decision_engine import DecisionEngine
from cortex_core.intelligence.briefing_engine import BriefingEngine
from cortex_core.intelligence.action_preparer import ActionPreparer
from cortex_core.validation.financial_validator import FinancialExclusionValidator


class CortexAssistant:

    @classmethod
    def resolve_contextual_entities(cls, query: str, history: List[Dict[str, Any]]) -> Dict[str, Any]:
        """
        Extracts pronouns or implicit entity references from previous turns.
        Example: 'What should I complete first?' after discussing 'SeleneOS' -> entity: 'SeleneOS'
        """
        resolved_entity = None
        q_lower = query.lower()

        # Check if project was mentioned in recent messages
        for msg in reversed(history[-4:] if history else []):
            text = (msg.get("text") or msg.get("query") or "").lower()
            if "seleneos" in text or "selene" in text:
                resolved_entity = "SeleneOS"
                break
            elif "cortex" in text:
                resolved_entity = "Cortex"
                break
            elif msg.get("referenced_project"):
                resolved_entity = msg["referenced_project"]
                break

        return {"referenced_project": resolved_entity}

    @classmethod
    def answer_query(
        cls,
        query: str,
        conversation_history: List[Dict[str, Any]] = None,
        database_snapshot: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """
        Central conversational entrypoint for Ask Cortex.
        """
        clean_q = (query or "").strip()
        history = conversation_history or []
        db = database_snapshot or {}

        # 1. Privacy Gate: Absolute Financial Exclusion
        is_fin, fin_reason = FinancialExclusionValidator.contains_financial_content(clean_q)
        if is_fin:
            return {
                "answer": f"🔒 **Financial Exclusion Notice:** This request involves financial, banking, or transactional data ({fin_reason}).\n\nCortex enforces a permanent privacy exclusion policy to protect your digital security. Banking, UPI, and financial records are never accessed, indexed, or processed by the intelligence engine.",
                "intent": "EXCLUDED",
                "citations": ["Cortex Non-Negotiable Financial Privacy Policy"],
                "confidence": 1.0,
                "has_personal_records": False,
                "proposed_actions": []
            }

        # 2. Contextual Entity Resolution
        context_meta = cls.resolve_contextual_entities(clean_q, history)

        # 3. Intent Classification
        intent, confidence = IntentRouter.classify(clean_q, history)

        domain_result = None
        proposed_actions = []

        # 4. Domain-Specific Execution
        q_lower = clean_q.lower()

        # A. ACADEMIC DOMAIN
        if intent == IntentType.ACADEMIC:
            if "tomorrow" in q_lower:
                target_d = datetime.date.today() + datetime.timedelta(days=1)
                domain_result = AcademicPlanner.verify_schedule_for_date(
                    target_date=target_d,
                    timetable=db.get("timetable", [])
                )
            elif "today" in q_lower or "class" in q_lower or "schedule" in q_lower:
                target_d = datetime.date.today()
                domain_result = AcademicPlanner.verify_schedule_for_date(
                    target_date=target_d,
                    timetable=db.get("timetable", [])
                )
            elif "study plan" in q_lower or "revise" in q_lower:
                days = 5
                match = re.search(r"(\d+)\s*days?", q_lower)
                if match:
                    days = int(match.group(1))
                plan = AcademicPlanner.generate_adaptive_study_plan(
                    days=days,
                    examinations=db.get("examinations", []),
                    assignments=db.get("assignments", []),
                    subjects=db.get("subjects", [])
                )
                domain_result = {
                    "answer": f"### 📚 Adaptive {days}-Day Study Plan\n" + "\n".join([
                        f"**{b['day_label']}:**\n" + "\n".join([f"&bull; _{s['slot']}_: **{s['subject']}** — {s['activity']} ({s['duration_hours']}h)" for s in b["sessions"]])
                        for b in plan["schedule"]
                    ]) + "\n\n_Review the proposed plan above. Would you like me to schedule these study blocks in your calendar?_",
                    "status": "PROPOSED",
                    "source": "Adaptive Curriculum Pacing Engine"
                }

        # B. PROJECT DOMAIN
        elif intent == IntentType.PROJECT:
            target_proj_name = context_meta.get("referenced_project") or "Cortex"
            # Extract project name from query if mentioned
            if "seleneos" in q_lower:
                target_proj_name = "SeleneOS"
            elif "cortex" in q_lower:
                target_proj_name = "Cortex"

            domain_result = ProjectReasoner.analyze_project(
                project_name_or_id=target_proj_name,
                projects=db.get("projects", []),
                milestones=db.get("project_milestones", []),
                decisions=db.get("project_decisions", []),
                tasks=db.get("tasks", [])
            )

        # C. LIFE & PLANNING DOMAIN
        elif intent == IntentType.LIFE_PLAN:
            if "plan" in q_lower:
                plan = LifePlanner.generate_daily_plan(
                    tasks=db.get("tasks", []),
                    timetable=db.get("timetable", [])
                )
                focus_lines = "\n".join([f"&bull; {f}" for f in plan.get("top_3_focus", [])])
                task_lines = "\n".join([f"&bull; **{t['title']}** [{t['priority']}] — _{t['priority_explanation']}_" for t in plan.get("prioritized_tasks", [])])

                domain_result = {
                    "answer": f"### 🗓️ Daily Focus Plan for {plan.get('day_name')}\n\n**Top 3 Recommended Focus Areas:**\n{focus_lines}\n\n**Prioritized Agenda:**\n{task_lines}",
                    "citations": ["Personal Tasks & Daily Planner"],
                    "confidence": 0.95
                }
            elif "forgetting" in q_lower or "overdue" in q_lower:
                active_tasks = [t for t in db.get("tasks", []) if t.get("status") != "COMPLETED"]
                overdue = [t for t in active_tasks if (t.get("dueDateMs") or 0) < int(datetime.datetime.now().timestamp() * 1000)]
                if overdue:
                    lines = [f"&bull; **{t.get('title')}** (Priority: {t.get('priority')})" for t in overdue[:5]]
                    domain_result = {
                        "answer": f"You have **{len(overdue)} overdue task(s)** that need your attention:\n\n" + "\n".join(lines),
                        "citations": ["Tasks Registry"],
                        "confidence": 0.95
                    }
                else:
                    domain_result = {
                        "answer": "You have no overdue tasks! All your active responsibilities are currently on track.",
                        "citations": ["Tasks Registry"],
                        "confidence": 0.95
                    }

        # D. BRIEFING DOMAIN
        elif intent == IntentType.BRIEFING:
            if "evening" in q_lower:
                domain_result = BriefingEngine.generate_evening_briefing(
                    tasks=db.get("tasks", []),
                    projects=db.get("projects", []),
                    timetable=db.get("timetable", [])
                )
            elif "week" in q_lower:
                domain_result = BriefingEngine.generate_weekly_briefing(
                    tasks=db.get("tasks", []),
                    projects=db.get("projects", []),
                    memories=db.get("memories", []),
                    goals=db.get("goals", [])
                )
            else:
                domain_result = BriefingEngine.generate_morning_briefing(
                    timetable=db.get("timetable", []),
                    examinations=db.get("examinations", []),
                    assignments=db.get("assignments", []),
                    tasks=db.get("tasks", []),
                    projects=db.get("projects", [])
                )

        # E. DECISION DOMAIN
        elif intent == IntentType.DECISION:
            domain_result = DecisionEngine.evaluate_decision(
                decision_statement=clean_q,
                context_memories=db.get("memories", []),
                notes=db.get("notes", [])
            )

        # F. ACTION DOMAIN
        elif intent == IntentType.ACTION:
            # Check if user wants to add a task
            task_match = re.search(r"(?:create|add|remind\s*me\s*to)\s*(?:a\s*task\s*(?:to|for)?\s*)?(.+)", clean_q, re.IGNORECASE)
            task_title = task_match.group(1).strip() if task_match else clean_q
            prop = ActionPreparer.create_task_proposal(
                title=task_title,
                priority="HIGH" if "urgent" in q_lower or "important" in q_lower else "MEDIUM",
                category="PERSONAL"
            )
            proposed_actions.append(prop)
            domain_result = {
                "answer": f"I have prepared an action proposal to add the task: **'{task_title}'**.\n\nPlease confirm in the Action Approval Centre or approve below before it is saved to your records.",
                "citations": ["Action Preparer Automation Engine"],
                "confidence": 1.0
            }

        # 5. RAG Retrieval across Knowledge & Memories
        rag_output = LocalRAGEngine.retrieve(
            query=clean_q,
            memories=db.get("memories", []),
            notes=db.get("notes", []),
            documents=db.get("documents", []),
            tasks=db.get("tasks", []),
            projects=db.get("projects", []),
            academics={
                "subjects": db.get("subjects", []),
                "timetable": db.get("timetable", []),
                "assignments": db.get("assignments", []),
                "examinations": db.get("examinations", [])
            }
        )

        # 6. Final Answer Synthesis
        synthesis = LocalAIProvider.synthesize_answer(
            query=clean_q,
            intent=intent.value,
            retrieved_context=rag_output.get("results", []),
            conversation_context=history,
            specific_domain_result=domain_result
        )

        return {
            "query": clean_q,
            "intent": intent.value,
            "confidence": synthesis.get("confidence", confidence),
            "answer": synthesis.get("answer", ""),
            "citations": synthesis.get("citations", []),
            "has_personal_records": synthesis.get("has_personal_records", False),
            "provider": synthesis.get("provider", "LOCAL_AI"),
            "referenced_project": context_meta.get("referenced_project"),
            "proposed_actions": proposed_actions,
            "retrieved_count": len(rag_output.get("results", []))
        }
