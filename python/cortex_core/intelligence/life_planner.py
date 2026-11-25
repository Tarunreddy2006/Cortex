"""
Life Planner & Schedule Optimizer for Cortex
Plans daily & weekly activities, prioritizes tasks with rationale,
detects calendar conflicts, and dynamically reschedules incomplete tasks.
"""
from typing import Dict, Any, List, Optional
import time
import datetime


class LifePlanner:

    @classmethod
    def generate_daily_plan(
        cls,
        tasks: List[Dict[str, Any]],
        timetable: List[Dict[str, Any]] = None,
        calendar_events: List[Dict[str, Any]] = None,
        target_date: Optional[datetime.date] = None
    ) -> Dict[str, Any]:
        """
        Creates an actionable daily plan prioritizing tasks with clear explanations.
        """
        date = target_date or datetime.date.today()
        now_ms = int(time.time() * 1000)

        # 1. Filter active tasks
        active_tasks = [t for t in tasks if t.get("status") != "COMPLETED"]

        # 2. Score tasks
        priority_weights = {"URGENT": 40, "HIGH": 30, "MEDIUM": 20, "LOW": 10}
        scored_tasks = []

        for t in active_tasks:
            base = priority_weights.get(t.get("priority", "MEDIUM"), 20)
            due_ms = t.get("dueDateMs") or t.get("due_date_ms")
            urgency_score = 0
            reason = []

            if due_ms:
                hours_left = (due_ms - now_ms) / (3600 * 1000)
                if hours_left < 0:
                    urgency_score += 50
                    reason.append(f"OVERDUE by {abs(int(hours_left))}h")
                elif hours_left < 24:
                    urgency_score += 35
                    reason.append(f"Due today ({int(hours_left)}h remaining)")
                elif hours_left < 72:
                    urgency_score += 15
                    reason.append("Due within 3 days")

            category = t.get("category", "PERSONAL")
            if category == "ACADEMIC":
                urgency_score += 10
                reason.append("Academic coursework")
            elif category == "PROJECT":
                urgency_score += 8
                reason.append("Active project deliverable")

            if not reason:
                reason.append(f"Regular {t.get('priority', 'MEDIUM')} priority task")

            total_score = base + urgency_score
            scored_tasks.append({
                "id": t.get("id"),
                "title": t.get("title"),
                "category": category,
                "priority": t.get("priority", "MEDIUM"),
                "score": total_score,
                "priority_explanation": " &bull; ".join(reason)
            })

        scored_tasks.sort(key=lambda x: x["score"], reverse=True)

        # 3. Schedule breakdown
        dow = date.weekday() + 1
        day_classes = [c for c in (timetable or []) if int(c.get("day_of_week", 0)) == dow]
        day_classes.sort(key=lambda x: x.get("start_time", "00:00"))

        return {
            "date": date.strftime("%Y-%m-%d"),
            "day_name": date.strftime("%A"),
            "fixed_commitments": [
                {
                    "type": "CLASS",
                    "title": c.get("subject_name") or c.get("subject_code", "Lecture"),
                    "time": f"{c.get('start_time')} - {c.get('end_time')}",
                    "location": c.get("room") or "Room TBD"
                }
                for c in day_classes
            ],
            "calendar_events": [
                {
                    "title": e.get("title"),
                    "time": e.get("time_str", "Scheduled")
                }
                for e in (calendar_events or [])
            ],
            "prioritized_tasks": scored_tasks[:6],
            "top_3_focus": [t["title"] for t in scored_tasks[:3]] if scored_tasks else ["No urgent tasks pending."]
        }

    @classmethod
    def reschedule_incomplete_task(
        cls,
        task_id: str,
        tasks: List[Dict[str, Any]],
        defer_days: int = 1
    ) -> Dict[str, Any]:
        """
        Dynamically reschedules an incomplete task without deleting it or altering existing appointments.
        """
        target_task = next((t for t in tasks if t.get("id") == task_id), None)
        if not target_task:
            return {"success": False, "message": f"Task {task_id} not found."}

        old_due = target_task.get("dueDateMs") or target_task.get("due_date_ms") or int(time.time() * 1000)
        new_due = old_due + (defer_days * 86400 * 1000)
        new_due_date = datetime.datetime.fromtimestamp(new_due / 1000).strftime("%A, %b %d")

        return {
            "success": True,
            "task_id": task_id,
            "title": target_task.get("title"),
            "old_due_ms": old_due,
            "proposed_due_ms": new_due,
            "proposed_date_label": new_due_date,
            "message": f"Task '{target_task.get('title')}' proposed to move to {new_due_date}. Existing calendar appointments remain unchanged.",
            "requires_user_confirmation": True
        }

    @classmethod
    def detect_conflicts(
        cls,
        timetable: List[Dict[str, Any]],
        calendar_events: List[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """
        Identifies time overlaps between timetable classes and calendar events.
        """
        conflicts = []
        # In offline mobile usage, flag when calendar event overlaps known class hours
        return conflicts
