"""
Briefing Engine for Cortex
Prepares Morning, Evening, and Weekly personal briefings with verified source attribution.
"""
from typing import Dict, Any, List, Optional
import datetime
import time


class BriefingEngine:

    @classmethod
    def generate_morning_briefing(
        cls,
        timetable: List[Dict[str, Any]] = None,
        examinations: List[Dict[str, Any]] = None,
        assignments: List[Dict[str, Any]] = None,
        tasks: List[Dict[str, Any]] = None,
        projects: List[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Synthesizes Morning Briefing: classes today, exams, deadlines, priorities, focus areas.
        """
        today = datetime.date.today()
        dow = today.weekday() + 1
        now_ms = int(time.time() * 1000)

        # 1. Classes Today
        classes_today = [c for c in (timetable or []) if int(c.get("day_of_week", 0)) == dow]
        classes_today.sort(key=lambda x: x.get("start_time", "00:00"))

        # 2. Upcoming Exams (next 14 days)
        upcoming_exams = []
        for e in (examinations or []):
            diff_days = (e.get("exam_date_ms", 0) - now_ms) / (86400 * 1000)
            if 0 <= diff_days <= 14:
                upcoming_exams.append({
                    "title": e.get("title"),
                    "days_remaining": max(0, int(diff_days)),
                    "room": e.get("room"),
                    "syllabus": e.get("syllabus")
                })

        # 3. Pending Assignments due soon
        pending_assignments = []
        for a in (assignments or []):
            if a.get("status") != "COMPLETED":
                due_ms = a.get("dueDateMs") or a.get("due_date_ms") or 0
                diff_days = (due_ms - now_ms) / (86400 * 1000)
                pending_assignments.append({
                    "title": a.get("title"),
                    "priority": a.get("priority", "MEDIUM"),
                    "days_remaining": int(diff_days) if due_ms > 0 else None
                })

        # 4. Urgent / High Priority Tasks
        active_tasks = [t for t in (tasks or []) if t.get("status") != "COMPLETED"]
        priority_rank = {"URGENT": 4, "HIGH": 3, "MEDIUM": 2, "LOW": 1}
        active_tasks.sort(key=lambda t: priority_rank.get(t.get("priority", "MEDIUM"), 1), reverse=True)

        top_tasks = active_tasks[:4]

        # 5. Top 3 Focus Areas
        focus_areas = []
        if upcoming_exams:
            focus_areas.append(f"Review syllabus for {upcoming_exams[0]['title']} ({upcoming_exams[0]['days_remaining']} days left).")
        if top_tasks:
            focus_areas.append(f"Complete high-priority task: '{top_tasks[0].get('title')}'.")
        if classes_today:
            focus_areas.append(f"Attend {len(classes_today)} scheduled classes starting at {classes_today[0].get('start_time')}.")
        elif not focus_areas:
            focus_areas = ["Deep work sprint on active projects.", "Catch up on reading and note consolidation.", "Review pending milestones."]

        return {
            "type": "MORNING_BRIEFING",
            "date": today.strftime("%A, %B %d, %Y"),
            "academic": {
                "classes_count": len(classes_today),
                "classes": [{"subject": c.get("subject_name") or c.get("subject_code"), "time": f"{c.get('start_time')} - {c.get('end_time')}", "room": c.get("room")} for c in classes_today],
                "upcoming_exams": upcoming_exams,
                "pending_assignments": pending_assignments[:3]
            },
            "projects": {
                "active_count": len(projects or []),
                "active_names": [p.get("name") for p in (projects or [])[:3]]
            },
            "priorities": {
                "top_tasks": [{"id": t.get("id"), "title": t.get("title"), "priority": t.get("priority", "MEDIUM")} for t in top_tasks],
                "suggested_focus": focus_areas[:3]
            },
            "source_attribution": "Generated from local Timetable, Examinations, Assignments, and Tasks databases."
        }

    @classmethod
    def generate_evening_briefing(
        cls,
        tasks: List[Dict[str, Any]] = None,
        projects: List[Dict[str, Any]] = None,
        timetable: List[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Synthesizes Evening Briefing: completed tasks, unfinished items, and tomorrow's outlook.
        """
        today = datetime.date.today()
        tomorrow = today + datetime.timedelta(days=1)
        dow_tomorrow = tomorrow.weekday() + 1

        all_tasks = tasks or []
        completed = [t for t in all_tasks if t.get("status") == "COMPLETED"]
        pending = [t for t in all_tasks if t.get("status") != "COMPLETED"]

        classes_tomorrow = [c for c in (timetable or []) if int(c.get("day_of_week", 0)) == dow_tomorrow]

        return {
            "type": "EVENING_BRIEFING",
            "date": today.strftime("%A, %B %d, %Y"),
            "completed_tasks_count": len(completed),
            "pending_tasks_count": len(pending),
            "tomorrow_outlook": {
                "day_name": tomorrow.strftime("%A"),
                "classes_count": len(classes_tomorrow),
                "classes_preview": [c.get("subject_name") or c.get("subject_code") for c in classes_tomorrow]
            },
            "suggested_priorities_tomorrow": [t.get("title") for t in pending[:3]],
            "source_attribution": "Synthesized from your local task completions and timetable registry."
        }

    @classmethod
    def generate_weekly_briefing(
        cls,
        tasks: List[Dict[str, Any]] = None,
        projects: List[Dict[str, Any]] = None,
        memories: List[Dict[str, Any]] = None,
        goals: List[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Weekly progress summary: accomplishments, goal progress, and strategic improvements.
        """
        completed_tasks = [t for t in (tasks or []) if t.get("status") == "COMPLETED"]
        pending_tasks = [t for t in (tasks or []) if t.get("status") != "COMPLETED"]

        active_goals = goals or []

        return {
            "type": "WEEKLY_BRIEFING",
            "week_label": f"Week of {datetime.date.today().strftime('%B %d, %Y')}",
            "completed_tasks": len(completed_tasks),
            "open_responsibilities": len(pending_tasks),
            "active_projects_count": len(projects or []),
            "new_memories_captured": len(memories or []),
            "goal_progress_summary": [{"title": g.get("title"), "progress": f"{g.get('progressPercent', g.get('progress_percent', 0))}%"} for g in active_goals[:4]],
            "suggested_improvements": [
                "Review stale and unverified episodic memories.",
                "Ensure approaching assignment drafts are started 48h prior to deadline.",
                "Consolidate weekly notes into structured Knowledge Vault collections."
            ],
            "source_attribution": "Aggregated from weekly Cortex SQLite local records."
        }
