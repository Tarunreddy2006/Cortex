"""
Action Preparer & Automation Gate for Cortex
Prepares structured action proposals requiring explicit user confirmation before execution.
Never executes destructive or external mutations automatically.
"""
from typing import Dict, Any, List, Optional
import time
import uuid


class ActionType:
    CREATE_TASK = "CREATE_TASK"
    SCHEDULE_STUDY_BLOCK = "SCHEDULE_STUDY_BLOCK"
    CREATE_CALENDAR_EVENT = "CREATE_CALENDAR_EVENT"
    CREATE_REMINDER = "CREATE_REMINDER"
    RESCHEDULE_TASK = "RESCHEDULE_TASK"
    UPDATE_GOAL_PROGRESS = "UPDATE_GOAL_PROGRESS"


class ActionPreparer:

    @classmethod
    def propose_action(
        cls,
        action_type: str,
        title: str,
        description: str,
        payload: Dict[str, Any],
        source_context: str = "Assistant Conversation"
    ) -> Dict[str, Any]:
        """
        Creates a proposed action envelope. Status is strictly 'PROPOSED'.
        Execution must occur only after explicit user approval.
        """
        action_id = f"act_{int(time.time() * 1000)}_{uuid.uuid4().hex[:6]}"

        return {
            "id": action_id,
            "action_type": action_type,
            "title": title,
            "description": description,
            "payload": payload,
            "source_context": source_context,
            "status": "PROPOSED",  # PROPOSED, APPROVED, REJECTED, CANCELLED, EXECUTED
            "created_at": int(time.time() * 1000),
            "executed_at": None,
            "preview_summary": f"[{action_type}] {title}: {description}"
        }

    @classmethod
    def create_task_proposal(cls, title: str, description: str = "", priority: str = "MEDIUM", category: str = "PERSONAL", due_date_ms: Optional[int] = None) -> Dict[str, Any]:
        due = due_date_ms or (int(time.time() * 1000) + 86400 * 1000)
        return cls.propose_action(
            action_type=ActionType.CREATE_TASK,
            title=f"Add Task: '{title}'",
            description=f"Create a new {priority} priority task in category {category}.",
            payload={
                "title": title,
                "description": description,
                "priority": priority,
                "category": category,
                "due_date_ms": due
            }
        )

    @classmethod
    def create_study_block_proposal(cls, subject: str, start_time: str, duration_hours: float, topic: str = "") -> Dict[str, Any]:
        return cls.propose_action(
            action_type=ActionType.SCHEDULE_STUDY_BLOCK,
            title=f"Schedule Study Session: {subject}",
            description=f"Reserve a {duration_hours}h revision block for '{subject}' (Topic: {topic or 'Core Review'}).",
            payload={
                "subject": subject,
                "start_time": start_time,
                "duration_hours": duration_hours,
                "topic": topic
            }
        )
