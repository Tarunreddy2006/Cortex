"""
Unit Tests for Cortex Phase 3 — Personal Intelligence, Reasoning & Automation
"""
import unittest
import json
import datetime
import time

from cortex_core.intelligence.intent_router import IntentRouter, IntentType
from cortex_core.intelligence.rag_engine import LocalRAGEngine
from cortex_core.intelligence.assistant import CortexAssistant
from cortex_core.intelligence.academic_planner import AcademicPlanner
from cortex_core.intelligence.project_reasoner import ProjectReasoner
from cortex_core.intelligence.life_planner import LifePlanner
from cortex_core.intelligence.decision_engine import DecisionEngine
from cortex_core.intelligence.briefing_engine import BriefingEngine
from cortex_core.intelligence.proactive_monitor import ProactiveMonitor
from cortex_core.intelligence.action_preparer import ActionPreparer, ActionType
from cortex_core.intelligence.local_ai_provider import LocalAIProvider
from cortex_core.intelligence.memory_reasoner import MemoryReasoner


class TestPhase3Intelligence(unittest.TestCase):

    def test_intent_classification(self):
        # Academic
        intent, conf = IntentRouter.classify("Do I have classes tomorrow?")
        self.assertEqual(intent, IntentType.ACADEMIC)
        self.assertGreater(conf, 0.5)

        # Project
        intent, conf = IntentRouter.classify("What is pending in SeleneOS project?")
        self.assertEqual(intent, IntentType.PROJECT)

        # Life plan
        intent, conf = IntentRouter.classify("Plan my day and tell me what tasks are overdue")
        self.assertEqual(intent, IntentType.LIFE_PLAN)

        # Decision
        intent, conf = IntentRouter.classify("Should I learn Rust or Go next? Compare them.")
        self.assertEqual(intent, IntentType.DECISION)

        # Knowledge RAG
        intent, conf = IntentRouter.classify("Find my notes about artificial intelligence and research paper")
        self.assertEqual(intent, IntentType.KNOWLEDGE_RAG)

        # Briefing
        intent, conf = IntentRouter.classify("Give me the morning briefing")
        self.assertEqual(intent, IntentType.BRIEFING)

    def test_contextual_follow_up_resolution(self):
        # Follow-up without repeating project name
        history = [
            {"query": "What is pending in SeleneOS?", "intent": "PROJECT", "referenced_project": "SeleneOS", "text": "SeleneOS status overview"}
        ]
        intent, _ = IntentRouter.classify("What should I complete first?", conversation_context=history)
        self.assertEqual(intent, IntentType.PROJECT)

        entities = CortexAssistant.resolve_contextual_entities("What should I complete first?", history)
        self.assertEqual(entities.get("referenced_project"), "SeleneOS")

    def test_local_rag_retrieval_and_source_attribution(self):
        memories = [
            {"id": "m1", "content": "Raft decomposes consensus into leader election, log replication, and safety.", "category": "ACADEMIC", "tags": ["consensus", "raft"]}
        ]
        notes = [
            {"id": "n1", "title": "Distributed Algorithms", "content": "Paxos ensures safety under asynchronous network conditions."}
        ]
        tasks = [
            {"id": "t1", "title": "Implement Raft in Python", "description": "Write leader heartbeat RPC", "status": "PENDING", "priority": "HIGH"}
        ]

        # Query matches memory and task
        rag_res = LocalRAGEngine.retrieve(
            query="Tell me about Raft leader consensus",
            memories=memories,
            notes=notes,
            tasks=tasks
        )
        self.assertTrue(rag_res["has_context"])
        self.assertGreater(rag_res["total_found"], 0)
        citations = rag_res["citations"]
        self.assertTrue(any("Raft" in c or "Memory" in c for c in citations))

    def test_rag_financial_privacy_gate(self):
        # Financial memory must be strictly filtered out from RAG results
        memories = [
            {"id": "fin1", "content": "SBI Bank Account 9876 debited Rs 5000 via UPI PhonePe Ref 12345", "category": "PERSONAL"},
            {"id": "safe1", "content": "Studied distributed hash tables and consistent hashing", "category": "ACADEMIC"}
        ]
        rag_res = LocalRAGEngine.retrieve(query="bank account UPI", memories=memories)
        # Financial item must NOT be retrieved
        for item in rag_res["results"]:
            self.assertNotEqual(item["id"], "fin1")

    def test_academic_schedule_verification_no_hallucination(self):
        timetable = [
            {"day_of_week": 1, "subject_name": "Distributed Systems", "start_time": "09:00", "end_time": "10:30", "room": "Hall 302"}
        ]
        # Target a Monday date: e.g. 2026-11-30 (which is a Monday)
        monday_date = datetime.date(2026, 11, 30)
        res = AcademicPlanner.verify_schedule_for_date(monday_date, timetable)
        self.assertTrue(res["has_classes"])
        self.assertEqual(res["classes"][0]["status"], "SCHEDULED")
        self.assertFalse("CANCELLED" in res["classes"][0]["status"])

    def test_adaptive_study_plan_generation(self):
        exams = [
            {"title": "Distributed Systems Mid-Term", "exam_date_ms": int(time.time() * 1000) + 259200000, "syllabus": "Modules 1-3"}
        ]
        assignments = [
            {"title": "Implement Raft", "status": "PENDING", "priority": "HIGH"}
        ]
        plan = AcademicPlanner.generate_adaptive_study_plan(days=5, examinations=exams, assignments=assignments)
        self.assertEqual(plan["plan_days"], 5)
        self.assertEqual(len(plan["schedule"]), 5)
        self.assertTrue(plan["requires_user_approval"])

    def test_life_planner_prioritization_and_reschedule(self):
        tasks = [
            {"id": "t1", "title": "Urgent Assignment", "priority": "URGENT", "status": "PENDING", "dueDateMs": int(time.time() * 1000) - 3600000},
            {"id": "t2", "title": "Casual Reading", "priority": "LOW", "status": "PENDING", "dueDateMs": int(time.time() * 1000) + 864000000}
        ]
        daily_plan = LifePlanner.generate_daily_plan(tasks=tasks)
        # Urgent overdue task should be ranked first
        self.assertEqual(daily_plan["prioritized_tasks"][0]["id"], "t1")
        self.assertTrue("OVERDUE" in daily_plan["prioritized_tasks"][0]["priority_explanation"])

        # Test rescheduling
        resched = LifePlanner.reschedule_incomplete_task("t1", tasks, defer_days=1)
        self.assertTrue(resched["success"])
        self.assertGreater(resched["proposed_due_ms"], resched["old_due_ms"])
        self.assertTrue(resched["requires_user_confirmation"])

    def test_project_reasoner(self):
        projects = [
            {"id": "p_selene", "name": "SeleneOS", "objective": "Microkernel operating system", "status": "ACTIVE"}
        ]
        milestones = [
            {"id": "m1", "project_id": "p_selene", "title": "Memory paging system", "is_completed": False}
        ]
        tasks = [
            {"id": "t_sel1", "project_id": "p_selene", "title": "Implement page fault handler", "priority": "HIGH", "status": "PENDING"}
        ]
        analysis = ProjectReasoner.analyze_project("SeleneOS", projects, milestones, [], tasks)
        self.assertTrue(analysis["found"])
        self.assertEqual(analysis["name"], "SeleneOS")
        self.assertIsNotNone(analysis["next_recommended_task"])
        self.assertEqual(analysis["next_recommended_task"]["title"], "Implement page fault handler")

    def test_decision_engine(self):
        res = DecisionEngine.evaluate_decision("Should I use SQLite Room or Realm DB?")
        self.assertIn("1_decision_statement", res)
        self.assertIn("9_recommendation", res)
        self.assertIn("6_risks", res)

        # Financial decision rejection
        fin_res = DecisionEngine.evaluate_decision("Should I invest Rs 50000 in crypto or mutual funds with my bank?")
        self.assertTrue(fin_res.get("error", False))

    def test_proactive_monitor_alerts(self):
        now_ms = int(time.time() * 1000)
        tasks = [{"id": "t1", "title": "Overdue task", "status": "PENDING", "dueDateMs": now_ms - 7200000}]
        exams = [{"id": "e1", "title": "Mid-Term", "exam_date_ms": now_ms + 172800000}]
        alerts = ProactiveMonitor.evaluate_proactive_alerts(tasks=tasks, examinations=exams, current_hour=14)
        self.assertGreaterEqual(len(alerts), 2)

    def test_action_preparer(self):
        prop = ActionPreparer.create_task_proposal("Complete Compiler Lab", priority="HIGH")
        self.assertEqual(prop["status"], "PROPOSED")
        self.assertEqual(prop["action_type"], ActionType.CREATE_TASK)
        self.assertIn("Complete Compiler Lab", prop["title"])

    def test_local_ai_status(self):
        status = LocalAIProvider.get_model_status()
        self.assertTrue(status["is_offline_operational"])
        self.assertGreater(len(status["available_models"]), 0)
        self.assertIn("Qwen", status["available_models"][0]["name"])

    def test_memory_reasoner(self):
        mem1 = {"id": "1", "content": "Distributed algorithms exam is on Monday at 9 AM"}
        mem2 = {"id": "2", "content": "Distributed algorithms exam is on Wednesday at 9 AM"}
        conflicts = MemoryReasoner.detect_conflicts([mem1, mem2])
        self.assertGreater(len(conflicts), 0)

        # Duplicates
        mem3 = {"id": "3", "content": "Distributed algorithms exam is on Monday at 9 AM"}
        duplicates = MemoryReasoner.detect_duplicates([mem1, mem3])
        self.assertGreater(len(duplicates), 0)


if __name__ == "__main__":
    unittest.main()
