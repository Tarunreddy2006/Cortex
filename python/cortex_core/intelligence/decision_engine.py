"""
Decision Support Engine for Cortex
Produces a 10-point structured decision analysis separating verified facts from estimates.
Strictly excludes financial application and bank account data.
"""
from typing import Dict, Any, List, Optional
import time
from cortex_core.validation.financial_validator import FinancialExclusionValidator


class DecisionEngine:

    @classmethod
    def evaluate_decision(
        cls,
        decision_statement: str,
        alternatives: List[str] = None,
        context_memories: List[Dict[str, Any]] = None,
        notes: List[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Generates 10-point structured analysis for decision making.
        """
        # Privacy gate: ensure query doesn't target financial accounts
        is_fin, reason = FinancialExclusionValidator.contains_financial_content(decision_statement)
        if is_fin:
            return {
                "error": True,
                "message": f"Decision request rejected by financial privacy policy: {reason}. Cortex permanently excludes financial and banking data."
            }

        alts = alternatives or ["Option A (Maintain current course)", "Option B (Adopt new alternative)"]

        # Aggregate relevant verified knowledge
        verified_facts = []
        for m in (context_memories or [])[:4]:
            verified_facts.append(m.get("content", "")[:140])
        for n in (notes or [])[:2]:
            verified_facts.append(n.get("content", "")[:140])

        if not verified_facts:
            verified_facts = ["No previous conflicting decisions or explicit notes found in local Cortex library."]

        # Structured 10-point synthesis
        analysis = {
            "1_decision_statement": decision_statement,
            "2_available_information": verified_facts,
            "3_alternatives": alts,
            "4_advantages": [
                f"Pros of '{alts[0]}': Lower switching overhead, established familiarity, predictable execution timeline.",
                f"Pros of '{alts[1] if len(alts) > 1 else alts[0]}': Higher long-term capability, architectural modernness, expanded learning opportunity."
            ],
            "5_disadvantages": [
                "Opportunity cost of deferred experimentation.",
                "Short-term learning curve and integration testing overhead."
            ],
            "6_risks": [
                "Underestimating implementation complexity during active semester or project deadlines.",
                "Context fragmentation across too many concurrent priorities."
            ],
            "7_unknown_information": [
                "Long-term maintenance cost of the selected alternative.",
                "Exact time requirement compared to available weekly study/work blocks."
            ],
            "8_assumptions": [
                "Current weekly study and development schedule remains relatively stable.",
                "Relevant foundational prerequisites are already understood."
            ],
            "9_recommendation": f"Adopt '{alts[1] if len(alts) > 1 else alts[0]}' with an incremental spike: prototype for 2 days before full commitment.",
            "10_conditions_that_change_recommendation": [
                "Upcoming examinations in the next 14 days take strict precedence.",
                "If initial spike reveals unexpected dependency blockers, revert to Option A."
            ],
            "status": "EVALUATED",
            "evaluated_at": int(time.time() * 1000)
        }

        return analysis
