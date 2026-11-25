"""
Project Reasoner for Cortex
Analyzes project health, milestone progress, blocker identification,
and recommends next actionable tasks for software projects like SeleneOS or Cortex.
"""
from typing import Dict, Any, List, Optional


class ProjectReasoner:

    @classmethod
    def analyze_project(
        cls,
        project_name_or_id: str,
        projects: List[Dict[str, Any]],
        milestones: List[Dict[str, Any]] = None,
        decisions: List[Dict[str, Any]] = None,
        tasks: List[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Inspects actual project status and returns a structured health and progress report.
        Never invents milestones or technical decisions.
        """
        target_term = project_name_or_id.lower().strip()
        matched_proj = None

        # Exact or partial match
        for p in projects:
            if p.get("id", "").lower() == target_term or target_term in p.get("name", "").lower():
                matched_proj = p
                break

        if not matched_proj:
            # Fallback to first active project if general query
            matched_proj = projects[0] if projects else None

        if not matched_proj:
            return {
                "found": False,
                "project_name": project_name_or_id,
                "message": f"No project matching '{project_name_or_id}' found in your local project directory."
            }

        proj_id = matched_proj.get("id")
        proj_name = matched_proj.get("name")

        # Filter related items
        proj_milestones = [m for m in (milestones or []) if m.get("project_id") == proj_id]
        completed_milestones = [m for m in proj_milestones if m.get("is_completed")]
        pending_milestones = [m for m in proj_milestones if not m.get("is_completed")]

        proj_decisions = [d for d in (decisions or []) if d.get("project_id") == proj_id]

        proj_tasks = [t for t in (tasks or []) if t.get("project_id") == proj_id or proj_name.lower() in (t.get("title", "") + " " + t.get("description", "")).lower()]
        completed_tasks = [t for t in proj_tasks if t.get("status") == "COMPLETED"]
        pending_tasks = [t for t in proj_tasks if t.get("status") != "COMPLETED"]

        # Calculate milestone completion percentage
        total_m = len(proj_milestones)
        milestone_pct = int((len(completed_milestones) / total_m) * 100) if total_m > 0 else matched_proj.get("progress_percent", 0)

        # Identify next task recommendation
        next_task_rec = None
        if pending_tasks:
            # Prioritize URGENT > HIGH > MEDIUM > LOW
            priority_rank = {"URGENT": 4, "HIGH": 3, "MEDIUM": 2, "LOW": 1}
            sorted_tasks = sorted(pending_tasks, key=lambda t: priority_rank.get(t.get("priority", "MEDIUM"), 1), reverse=True)
            top_task = sorted_tasks[0]
            next_task_rec = {
                "title": top_task.get("title"),
                "description": top_task.get("description"),
                "priority": top_task.get("priority", "MEDIUM"),
                "reason": f"Highest priority incomplete task registered for {proj_name}."
            }
        elif pending_milestones:
            top_milestone = pending_milestones[0]
            next_task_rec = {
                "title": f"Work towards milestone: {top_milestone.get('title')}",
                "description": "No granular tasks currently open. Create tasks to advance this milestone.",
                "priority": "HIGH",
                "reason": f"Next pending milestone on the {proj_name} roadmap."
            }
        else:
            next_task_rec = {
                "title": "Define next development sprint",
                "description": f"All current registered tasks and milestones for {proj_name} are completed.",
                "priority": "MEDIUM",
                "reason": "Roadmap is clear. Ready for next architectural feature."
            }

        return {
            "found": True,
            "id": proj_id,
            "name": proj_name,
            "objective": matched_proj.get("objective") or matched_proj.get("description"),
            "status": matched_proj.get("status", "ACTIVE"),
            "tech_stack": matched_proj.get("tech_stack", []),
            "progress_percent": milestone_pct,
            "milestones_total": total_m,
            "milestones_completed": len(completed_milestones),
            "pending_milestones": [{"id": m.get("id"), "title": m.get("title")} for m in pending_milestones],
            "pending_tasks": [{"id": t.get("id"), "title": t.get("title"), "priority": t.get("priority", "MEDIUM")} for t in pending_tasks],
            "recent_decisions": [{"title": d.get("title"), "status": d.get("status")} for d in proj_decisions[:5]],
            "next_recommended_task": next_task_rec
        }
