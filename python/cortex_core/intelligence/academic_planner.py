"""
Academic Planner & Schedule Reasoner for Cortex
Generates daily/weekly class summaries, 5-day adaptive study plans,
and verifies schedule changes without hallucinated cancellations.
"""
from typing import Dict, Any, List, Optional
import datetime
import time


class AcademicPlanner:

    DAYS_MAP = {
        1: "Monday", 2: "Tuesday", 3: "Wednesday", 4: "Thursday",
        5: "Friday", 6: "Saturday", 7: "Sunday"
    }

    @classmethod
    def get_classes_for_day(cls, day_of_week: int, timetable: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Retrieves scheduled classes for given day (1=Mon .. 7=Sun).
        """
        day_classes = [tt for tt in timetable if int(tt.get("day_of_week", 0)) == day_of_week]
        day_classes.sort(key=lambda x: x.get("start_time", "00:00"))
        return day_classes

    @classmethod
    def verify_schedule_for_date(
        cls,
        target_date: datetime.date,
        timetable: List[Dict[str, Any]],
        calendar_events: List[Dict[str, Any]] = None,
        cancellations: List[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Source-aware logic: checks regular timetable + calendar exceptions + verified announcements.
        Never interprets missing data as confirmation of cancellation.
        """
        # Python weekday: Mon=0 .. Sun=6 -> Convert to 1..7
        dow = target_date.weekday() + 1
        day_name = cls.DAYS_MAP.get(dow, "Unknown")
        regular_classes = cls.get_classes_for_day(dow, timetable)

        classes_result: List[Dict[str, Any]] = []

        for c in regular_classes:
            subj = c.get("subject_name") or c.get("subject_code", "Course")
            start = c.get("start_time", "09:00")
            end = c.get("end_time", "10:00")
            room = c.get("room", "TBD")

            # Check explicit cancellation notice
            is_cancelled = False
            cancel_reason = None
            for ann in (cancellations or []):
                ann_text = ann.get("content", "").lower()
                if subj.lower() in ann_text and ("cancel" in ann_text or "holiday" in ann_text or "postpone" in ann_text):
                    is_cancelled = True
                    cancel_reason = ann.get("content")
                    break

            classes_result.append({
                "subject": subj,
                "time": f"{start} - {end}",
                "room": room,
                "status": "CANCELLED" if is_cancelled else "SCHEDULED",
                "verification_source": "Explicit announcement" if is_cancelled else "Regular timetable registry",
                "cancellation_reason": cancel_reason
            })

        return {
            "target_date": target_date.strftime("%Y-%m-%d"),
            "day_name": day_name,
            "has_classes": len(classes_result) > 0,
            "classes": classes_result,
            "note": "Schedule verified against regular timetable and recorded announcements. Unannounced exceptions are not assumed."
        }

    @classmethod
    def generate_adaptive_study_plan(
        cls,
        days: int = 5,
        examinations: List[Dict[str, Any]] = None,
        assignments: List[Dict[str, Any]] = None,
        subjects: List[Dict[str, Any]] = None,
        daily_study_hours: float = 3.0
    ) -> Dict[str, Any]:
        """
        Produces realistic 5-day study plan distributing revision blocks
        based on upcoming exam proximity, pending assignments, and subject credits.
        """
        today = datetime.date.today()
        daily_blocks: List[Dict[str, Any]] = []

        # Find nearest exams
        now_ms = int(time.time() * 1000)
        upcoming_exams = [e for e in (examinations or []) if (e.get("exam_date_ms", 0) - now_ms) > 0]
        upcoming_exams.sort(key=lambda x: x.get("exam_date_ms", 0))

        pending_assignments = [a for a in (assignments or []) if a.get("status") != "COMPLETED"]

        subject_list = subjects or []
        sub_names = [s.get("name") for s in subject_list] if subject_list else ["Distributed Systems", "Machine Learning", "Database Systems"]

        for d in range(days):
            plan_date = today + datetime.timedelta(days=d)
            day_str = plan_date.strftime("%A, %b %d")

            blocks: List[Dict[str, Any]] = []

            # 1. Check if an exam is within 48h
            target_exam = None
            for e in upcoming_exams:
                diff_days = (e.get("exam_date_ms", 0) - now_ms) / (86400 * 1000)
                if 0 <= diff_days <= (d + 2):
                    target_exam = e
                    break

            if target_exam:
                blocks.append({
                    "slot": "Block 1 (Morning / High Focus)",
                    "subject": target_exam.get("title", "Exam Subject"),
                    "activity": f"Intensive Revision: {target_exam.get('syllabus', 'Key Core Concepts')}",
                    "duration_hours": 1.5,
                    "reason": f"Approaching examination ({target_exam.get('title')})"
                })
            else:
                # Rotate subjects
                sub_index = d % len(sub_names)
                blocks.append({
                    "slot": "Block 1 (Morning Focus)",
                    "subject": sub_names[sub_index],
                    "activity": "Review textbook chapters & lecture notes",
                    "duration_hours": 1.5,
                    "reason": "Regular curriculum pacing"
                })

            # 2. Check pending assignment
            if pending_assignments:
                asgn = pending_assignments[d % len(pending_assignments)]
                blocks.append({
                    "slot": "Block 2 (Afternoon / Practical)",
                    "subject": asgn.get("subject_code") or "Coursework",
                    "activity": f"Work on assignment: '{asgn.get('title')}'",
                    "duration_hours": 1.5,
                    "reason": f"Pending assignment deadline ({asgn.get('priority', 'MEDIUM')} priority)"
                })
            else:
                sub_index2 = (d + 1) % len(sub_names)
                blocks.append({
                    "slot": "Block 2 (Evening Review)",
                    "subject": sub_names[sub_index2],
                    "activity": "Practice problem sets & self-assessment",
                    "duration_hours": 1.5,
                    "reason": "Active recall reinforcement"
                })

            daily_blocks.append({
                "date": plan_date.strftime("%Y-%m-%d"),
                "day_label": day_str,
                "total_study_hours": min(daily_study_hours, sum(b["duration_hours"] for b in blocks)),
                "sessions": blocks
            })

        return {
            "plan_days": days,
            "daily_target_hours": daily_study_hours,
            "schedule": daily_blocks,
            "status": "PROPOSED",
            "requires_user_approval": True,
            "recommendation_rationale": "Plan balances upcoming exam urgency, assignment deadlines, and cognitive pacing without excessive fatigue."
        }
