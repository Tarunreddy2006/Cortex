"""
Proactive Monitoring Engine for Cortex
Scans stored database state for approaching deadlines, overdue tasks,
and upcoming examinations with quiet-hours suppression and importance scoring.
"""
from typing import Dict, Any, List, Optional
import time
import datetime


class ProactiveMonitor:

    @classmethod
    def evaluate_proactive_alerts(
        cls,
        tasks: List[Dict[str, Any]] = None,
        examinations: List[Dict[str, Any]] = None,
        assignments: List[Dict[str, Any]] = None,
        milestones: List[Dict[str, Any]] = None,
        suppression_history: List[str] = None,
        current_hour: Optional[int] = None,
        quiet_hours_start: int = 22,
        quiet_hours_end: int = 7
    ) -> List[Dict[str, Any]]:
        """
        Evaluates conditions and generates actionable proactive alerts.
        Suppresses duplicates and respects quiet hours unless critical.
        """
        now_ms = int(time.time() * 1000)
        hour = current_hour if current_hour is not None else datetime.datetime.now().hour
        is_quiet_hours = (hour >= quiet_hours_start or hour < quiet_hours_end)
        suppressed_set = set(suppression_history or [])

        alerts: List[Dict[str, Any]] = []

        # 1. Overdue Tasks
        for t in (tasks or []):
            if t.get("status") != "COMPLETED":
                due_ms = t.get("dueDateMs") or t.get("due_date_ms")
                if due_ms and due_ms < now_ms:
                    alert_key = f"overdue_task_{t.get('id')}"
                    if alert_key not in suppressed_set:
                        hours_overdue = int((now_ms - due_ms) / (3600 * 1000))
                        alerts.append({
                            "key": alert_key,
                            "type": "OVERDUE_TASK",
                            "priority": "HIGH",
                            "title": f"Overdue Task: {t.get('title')}",
                            "description": f"This task was due {hours_overdue} hours ago. Would you like to reschedule or complete it?",
                            "reference_id": t.get("id"),
                            "action_suggestion": "RESCHEDULE",
                            "is_critical": False
                        })

        # 2. Upcoming Examinations (within 7 days)
        for e in (examinations or []):
            exam_ms = e.get("exam_date_ms") or 0
            diff_ms = exam_ms - now_ms
            diff_days = diff_ms / (86400 * 1000)
            if 0 < diff_days <= 7:
                alert_key = f"upcoming_exam_{e.get('id')}_{int(diff_days)}"
                if alert_key not in suppressed_set:
                    alerts.append({
                        "key": alert_key,
                        "type": "UPCOMING_EXAM",
                        "priority": "URGENT",
                        "title": f"Upcoming Exam: {e.get('title')}",
                        "description": f"Exam scheduled in {int(diff_days)} days (Room: {e.get('room', 'TBD')}). Review syllabus modules.",
                        "reference_id": e.get("id"),
                        "action_suggestion": "SCHEDULE_STUDY",
                        "is_critical": True
                    })

        # 3. Assignment Deadlines (within 48 hours)
        for a in (assignments or []):
            if a.get("status") != "COMPLETED":
                due_ms = a.get("dueDateMs") or a.get("due_date_ms") or 0
                diff_ms = due_ms - now_ms
                diff_hours = diff_ms / (3600 * 1000)
                if 0 < diff_hours <= 48:
                    alert_key = f"due_assignment_{a.get('id')}"
                    if alert_key not in suppressed_set:
                        alerts.append({
                            "key": alert_key,
                            "type": "ASSIGNMENT_DUE",
                            "priority": "HIGH",
                            "title": f"Assignment Due: {a.get('title')}",
                            "description": f"Deadline approaching in {int(diff_hours)} hours. Status: PENDING.",
                            "reference_id": a.get("id"),
                            "action_suggestion": "VIEW_ASSIGNMENT",
                            "is_critical": False
                        })

        # Filter during quiet hours: only allow critical alerts
        if is_quiet_hours:
            alerts = [a for a in alerts if a.get("is_critical", False)]

        # Sort by urgency
        p_rank = {"URGENT": 3, "HIGH": 2, "MEDIUM": 1, "LOW": 0}
        alerts.sort(key=lambda x: p_rank.get(x.get("priority"), 0), reverse=True)

        return alerts
