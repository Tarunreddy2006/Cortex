"""
Cortex Entrypoint for Chaquopy & Android Bridge
Exposes Python processing, normalization, and validation functions directly to Kotlin.
All inputs and outputs are structured JSON strings for maximum safety and compatibility.
"""

import json
import os
from typing import Dict, Any, List
from cortex_core.validation.financial_validator import FinancialExclusionValidator
from cortex_core.processing.notification_processor import NotificationProcessor
from cortex_core.processing.email_processor import EmailProcessor
from cortex_core.processing.calendar_processor import CalendarProcessor
from cortex_core.processing.contact_processor import ContactProcessor
from cortex_core.processing.whatsapp_export_processor import WhatsAppExportProcessor
from cortex_core.processing.shared_content_processor import SharedContentProcessor
from cortex_core.processing.memory_engine import MemoryEngine
from cortex_core.processing.knowledge_pipeline import KnowledgePipeline
from cortex_core.connector_interfaces.registry import ConnectorRegistry


_registry = ConnectorRegistry()


def is_financial_package(package_name: str, custom_denylist_json: str = "[]") -> bool:
    try:
        custom_list = set(json.loads(custom_denylist_json)) if custom_denylist_json else set()
    except Exception:
        custom_list = set()
    return FinancialExclusionValidator.is_financial_package(package_name, custom_list)


def check_financial_content(text: str, sender: str = "") -> str:
    is_fin, reason = FinancialExclusionValidator.contains_financial_content(text, sender or None)
    return json.dumps({"is_financial": is_fin, "reason": reason})


def process_notification(input_json: str) -> str:
    try:
        data = json.loads(input_json)
        custom_denylist = set(data.get("custom_denylist", []))
        record, accepted, reason = NotificationProcessor.process(
            package_name=data.get("package_name", ""),
            app_name=data.get("app_name", ""),
            title=data.get("title", ""),
            text=data.get("text", ""),
            timestamp_ms=int(data.get("timestamp_ms", 0)),
            is_grouped=bool(data.get("is_grouped", False)),
            group_key=data.get("group_key"),
            custom_denylist=custom_denylist
        )
        return json.dumps({
            "success": accepted,
            "record": record.to_dict() if record else None,
            "reason": reason
        })
    except Exception as e:
        return json.dumps({"success": False, "record": None, "reason": str(e)})


def process_email(input_json: str) -> str:
    try:
        data = json.loads(input_json)
        record, accepted, reason = EmailProcessor.process(
            message_id=data.get("message_id", ""),
            thread_id=data.get("thread_id", ""),
            sender=data.get("sender", ""),
            recipients=data.get("recipients", []),
            subject=data.get("subject", ""),
            snippet=data.get("snippet", ""),
            body_text=data.get("body_text"),
            timestamp_ms=int(data.get("timestamp_ms", 0)),
            labels=data.get("labels", []),
            has_attachments=bool(data.get("has_attachments", False))
        )
        return json.dumps({
            "success": accepted,
            "record": record.to_dict() if record else None,
            "reason": reason
        })
    except Exception as e:
        return json.dumps({"success": False, "record": None, "reason": str(e)})


def process_calendar_event(input_json: str) -> str:
    try:
        data = json.loads(input_json)
        record, accepted, reason = CalendarProcessor.process(
            event_id=data.get("event_id", ""),
            calendar_id=data.get("calendar_id", ""),
            calendar_name=data.get("calendar_name", ""),
            title=data.get("title", ""),
            start_time_ms=int(data.get("start_time_ms", 0)),
            end_time_ms=int(data.get("end_time_ms", 0)),
            description=data.get("description"),
            location=data.get("location"),
            is_all_day=bool(data.get("is_all_day", False)),
            rrule=data.get("rrule"),
            organizer=data.get("organizer"),
            source_connector=data.get("source_connector", "calendar_provider")
        )
        return json.dumps({
            "success": accepted,
            "record": record.to_dict() if record else None,
            "reason": reason
        })
    except Exception as e:
        return json.dumps({"success": False, "record": None, "reason": str(e)})


def process_contact(input_json: str) -> str:
    try:
        data = json.loads(input_json)
        record, accepted, reason = ContactProcessor.process(
            contact_id=data.get("contact_id", ""),
            lookup_key=data.get("lookup_key", ""),
            display_name=data.get("display_name", ""),
            phone_numbers=data.get("phone_numbers", []),
            email_addresses=data.get("email_addresses", []),
            organization=data.get("organization"),
            title=data.get("title"),
            is_starred=bool(data.get("is_starred", False))
        )
        return json.dumps({
            "success": accepted,
            "record": record.to_dict() if record else None,
            "reason": reason
        })
    except Exception as e:
        return json.dumps({"success": False, "record": None, "reason": str(e)})


def parse_whatsapp_export(chat_text: str, file_name: str = "chat.txt") -> str:
    try:
        result = WhatsAppExportProcessor.parse_chat_text(chat_text, file_name)
        return json.dumps({"success": True, "data": result})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})


def process_shared_link(url: str, title: str = "", source_app: str = "") -> str:
    try:
        record, accepted, reason = SharedContentProcessor.process_shared_link(url, title, source_app)
        return json.dumps({
            "success": accepted,
            "record": record.to_dict() if record else None,
            "reason": reason
        })
    except Exception as e:
        return json.dumps({"success": False, "record": None, "reason": str(e)})


# Phase 2 Entrypoints
def process_memory(input_json: str) -> str:
    """
    {content, source_connector, source_record_id, is_user_explicit}
    """
    try:
        data = json.loads(input_json)
        content = data.get("content", "")
        source_connector = data.get("source_connector", "manual_entry")
        source_record_id = data.get("source_record_id")
        is_user_explicit = bool(data.get("is_user_explicit", True))

        record, success, reason = MemoryEngine.process_incoming_content(
            content=content,
            source_connector=source_connector,
            source_record_id=source_record_id,
            is_user_explicit=is_user_explicit
        )
        return json.dumps({
            "success": success,
            "record": record.to_dict() if record else None,
            "reason": reason
        })
    except Exception as e:
        return json.dumps({"success": False, "record": None, "reason": str(e)})


def ingest_document(input_json: str) -> str:
    """
    {file_path, file_name, title, collection_id, source_connector}
    """
    try:
        data = json.loads(input_json)
        file_path = data.get("file_path", "")
        file_name = data.get("file_name", os.path.basename(file_path))
        title = data.get("title")
        collection_id = data.get("collection_id")
        source_connector = data.get("source_connector", "knowledge_vault")

        record, success, reason = KnowledgePipeline.ingest_document(
            file_path=file_path,
            file_name=file_name,
            title=title,
            collection_id=collection_id,
            source_connector=source_connector
        )
        return json.dumps({
            "success": success,
            "record": record.to_dict() if record else None,
            "reason": reason
        })
    except Exception as e:
        return json.dumps({"success": False, "record": None, "reason": str(e)})


def get_connector_registry() -> str:
    connectors = [c.to_dict() for c in _registry.list_connectors()]
    return json.dumps(connectors)


# ==================== PHASE 3 ENTRYPOINTS: INTELLIGENCE & REASONING ====================

from cortex_core.intelligence.assistant import CortexAssistant
from cortex_core.intelligence.briefing_engine import BriefingEngine
from cortex_core.intelligence.academic_planner import AcademicPlanner
from cortex_core.intelligence.life_planner import LifePlanner
from cortex_core.intelligence.project_reasoner import ProjectReasoner
from cortex_core.intelligence.decision_engine import DecisionEngine
from cortex_core.intelligence.proactive_monitor import ProactiveMonitor
from cortex_core.intelligence.local_ai_provider import LocalAIProvider
from cortex_core.intelligence.memory_reasoner import MemoryReasoner


def ask_cortex(input_json: str) -> str:
    """
    Central AI Assistant entrypoint.
    Input: { "query": str, "conversation_history": list, "database_snapshot": dict }
    """
    try:
        data = json.loads(input_json)
        query = data.get("query", "")
        history = data.get("conversation_history", [])
        snapshot = data.get("database_snapshot", {})

        result = CortexAssistant.answer_query(
            query=query,
            conversation_history=history,
            database_snapshot=snapshot
        )
        return json.dumps({"success": True, "data": result})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})


def generate_briefing(input_json: str) -> str:
    """
    Generates morning, evening, or weekly briefings.
    Input: { "type": "MORNING" | "EVENING" | "WEEKLY", ...data }
    """
    try:
        data = json.loads(input_json)
        b_type = (data.get("type") or "MORNING").upper()

        if "EVENING" in b_type:
            res = BriefingEngine.generate_evening_briefing(
                tasks=data.get("tasks", []),
                projects=data.get("projects", []),
                timetable=data.get("timetable", [])
            )
        elif "WEEK" in b_type:
            res = BriefingEngine.generate_weekly_briefing(
                tasks=data.get("tasks", []),
                projects=data.get("projects", []),
                memories=data.get("memories", []),
                goals=data.get("goals", [])
            )
        else:
            res = BriefingEngine.generate_morning_briefing(
                timetable=data.get("timetable", []),
                examinations=data.get("examinations", []),
                assignments=data.get("assignments", []),
                tasks=data.get("tasks", []),
                projects=data.get("projects", [])
            )
        return json.dumps({"success": True, "data": res})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})


def generate_study_plan(input_json: str) -> str:
    """
    Generates adaptive multi-day study schedule.
    """
    try:
        data = json.loads(input_json)
        days = int(data.get("days", 5))
        daily_hours = float(data.get("daily_hours", 3.0))
        plan = AcademicPlanner.generate_adaptive_study_plan(
            days=days,
            examinations=data.get("examinations", []),
            assignments=data.get("assignments", []),
            subjects=data.get("subjects", []),
            daily_study_hours=daily_hours
        )
        return json.dumps({"success": True, "data": plan})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})


def generate_daily_plan(input_json: str) -> str:
    """
    Plans daily activities and task priorities.
    """
    try:
        data = json.loads(input_json)
        plan = LifePlanner.generate_daily_plan(
            tasks=data.get("tasks", []),
            timetable=data.get("timetable", []),
            calendar_events=data.get("calendar_events", [])
        )
        return json.dumps({"success": True, "data": plan})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})


def reschedule_task(input_json: str) -> str:
    """
    Dynamically reschedules an incomplete task without deleting it.
    """
    try:
        data = json.loads(input_json)
        task_id = data.get("task_id", "")
        tasks = data.get("tasks", [])
        defer_days = int(data.get("defer_days", 1))
        res = LifePlanner.reschedule_incomplete_task(task_id, tasks, defer_days)
        return json.dumps({"success": True, "data": res})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})


def evaluate_decision(input_json: str) -> str:
    """
    Produces 10-point structured decision evaluation.
    """
    try:
        data = json.loads(input_json)
        statement = data.get("statement", "")
        alts = data.get("alternatives", [])
        mems = data.get("memories", [])
        notes = data.get("notes", [])
        res = DecisionEngine.evaluate_decision(statement, alts, mems, notes)
        return json.dumps({"success": True, "data": res})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})


def analyze_project(input_json: str) -> str:
    """
    Analyzes project health and recommends next task (e.g. for SeleneOS).
    """
    try:
        data = json.loads(input_json)
        proj_name = data.get("project_name", "SeleneOS")
        projs = data.get("projects", [])
        milestones = data.get("milestones", [])
        decisions = data.get("decisions", [])
        tasks = data.get("tasks", [])
        res = ProjectReasoner.analyze_project(proj_name, projs, milestones, decisions, tasks)
        return json.dumps({"success": True, "data": res})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})


def get_proactive_alerts(input_json: str) -> str:
    """
    Scans for approaching deadlines and exams.
    """
    try:
        data = json.loads(input_json)
        alerts = ProactiveMonitor.evaluate_proactive_alerts(
            tasks=data.get("tasks", []),
            examinations=data.get("examinations", []),
            assignments=data.get("assignments", []),
            milestones=data.get("milestones", []),
            suppression_history=data.get("suppression_history", [])
        )
        return json.dumps({"success": True, "data": alerts})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})


def get_local_ai_status(input_json: str = "{}") -> str:
    """
    Returns available quantized edge models and active runtime status.
    """
    try:
        data = json.loads(input_json) if input_json else {}
        models_dir = data.get("models_dir", "")
        status = LocalAIProvider.get_model_status(models_dir)
        return json.dumps({"success": True, "data": status})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})


def reason_memories(input_json: str) -> str:
    """
    Runs memory conflict, duplicate, or stale analysis.
    """
    try:
        data = json.loads(input_json)
        action = data.get("action", "conflicts")
        all_mems = data.get("memories", [])
        target_mem = data.get("target_memory", {})

        if action == "duplicates":
            res = MemoryReasoner.detect_duplicates(all_mems)
        elif action == "stale":
            res = MemoryReasoner.identify_stale_memories(all_mems)
        elif action == "related":
            res = MemoryReasoner.find_related_memories(target_mem, all_mems)
        else:
            res = MemoryReasoner.detect_conflicts(all_mems)

        return json.dumps({"success": True, "data": res})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})
