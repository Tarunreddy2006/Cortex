"""
Unit tests for Cortex Phase 2 Core
- Personal Memory Engine
- Knowledge Vault Pipeline
- Academic & Life Data Models
"""

import os
import tempfile
import unittest
from cortex_core.models.memory import MemoryRecord, MemoryCategory, ConfidenceStatus, ApprovalStatus
from cortex_core.models.academic import AcademicProfileRecord, SubjectRecord, TimetableRecord, AssignmentRecord, ExaminationRecord
from cortex_core.models.knowledge import DocumentItemRecord, CollectionRecord, NoteRecord
from cortex_core.models.life import TaskRecord, GoalRecord, ProjectRecord
from cortex_core.processing.memory_engine import MemoryEngine
from cortex_core.processing.knowledge_pipeline import KnowledgePipeline


class TestPhase2Core(unittest.TestCase):

    def test_memory_engine_classification(self):
        # Academic
        academic_text = "The advanced machine learning examination and assignment coursework deadline is Dec 12."
        cat = MemoryEngine.classify_category(academic_text)
        self.assertEqual(cat, MemoryCategory.ACADEMIC)

        # Project
        project_text = "Completed milestone 2 refactor for backend api and deployment architecture."
        cat = MemoryEngine.classify_category(project_text)
        self.assertEqual(cat, MemoryCategory.PROJECT)

        # Episodic
        episodic_text = "Scheduled meeting with advisor Dr. Rao for dissertation discussion tomorrow."
        cat = MemoryEngine.classify_category(episodic_text)
        self.assertEqual(cat, MemoryCategory.EPISODIC)

        # Knowledge
        knowledge_text = "Transformer architecture attention mechanism formula and research paper review."
        cat = MemoryEngine.classify_category(knowledge_text)
        self.assertEqual(cat, MemoryCategory.KNOWLEDGE)

        # Personal
        personal_text = "Remember to buy extra coffee beans and pack running shoes for weekend trip."
        cat = MemoryEngine.classify_category(personal_text)
        self.assertEqual(cat, MemoryCategory.PERSONAL)

    def test_memory_engine_transient_rejection(self):
        self.assertTrue(MemoryEngine.is_transient_noise("ok"))
        self.assertTrue(MemoryEngine.is_transient_noise("k."))
        self.assertTrue(MemoryEngine.is_transient_noise("thanks!"))
        self.assertTrue(MemoryEngine.is_transient_noise("Your OTP is 492010"))
        self.assertFalse(MemoryEngine.is_transient_noise("Meeting with Dr. Smith scheduled at 4 PM tomorrow."))

    def test_memory_engine_financial_exclusion(self):
        fin_text = "Account 5489 debited INR 5,000 via UPI transaction to vendor."
        record, accepted, reason = MemoryEngine.process_incoming_content(
            content=fin_text,
            source_connector="test_source",
            is_user_explicit=False
        )
        self.assertFalse(accepted)
        self.assertIsNone(record)
        self.assertIn("financial", reason.lower())

    def test_memory_engine_valid_creation(self):
        content = "Prof. Sharma announced that the final robotics project submission deadline is next Friday."
        record, accepted, reason = MemoryEngine.process_incoming_content(
            content=content,
            source_connector="classroom",
            is_user_explicit=False
        )
        self.assertTrue(accepted)
        self.assertIsNotNone(record)
        self.assertEqual(record.category, MemoryCategory.ACADEMIC)
        self.assertEqual(record.confidence_status, ConfidenceStatus.EXTRACTED)
        self.assertTrue(len(record.tags) > 0)

    def test_knowledge_pipeline_file_type_and_text(self):
        self.assertEqual(KnowledgePipeline.get_document_type("thesis.pdf"), "PDF")
        self.assertEqual(KnowledgePipeline.get_document_type("notes.md"), "MD")
        self.assertEqual(KnowledgePipeline.get_document_type("data.xlsx"), "XLSX")

        # Test text extraction from temporary file
        with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".md", encoding="utf-8") as f:
            f.write("# Autonomous Navigation\nResearch notes regarding LiDAR SLAM algorithms in agricultural fields.")
            temp_path = f.name

        try:
            record, success, reason = KnowledgePipeline.ingest_document(
                file_path=temp_path,
                file_name="navigation.md",
                title="LiDAR SLAM Notes"
            )
            self.assertTrue(success)
            self.assertIsNotNone(record)
            self.assertIn("LiDAR", record.extracted_text)
            self.assertEqual(record.category, "RESEARCH")
        finally:
            if os.path.exists(temp_path):
                os.remove(temp_path)

    def test_phase2_models_serialization(self):
        task = TaskRecord(
            id="t1",
            source_connector="planner",
            title="Submit robotics paper draft",
            priority="HIGH",
            status="PENDING",
            category="ACADEMIC"
        )
        d = task.to_dict()
        self.assertEqual(d["title"], "Submit robotics paper draft")
        self.assertEqual(d["priority"], "HIGH")

        goal = GoalRecord(
            id="g1",
            source_connector="life",
            title="Publish first conference paper",
            goal_type="ACADEMIC",
            progress_percent=75
        )
        self.assertEqual(goal.progress_percent, 75)

        proj = ProjectRecord(
            id="p1",
            source_connector="projects",
            name="SeleneOS",
            status="ACTIVE",
            tech_stack=["Kotlin", "Python", "SQLite"]
        )
        self.assertIn("Kotlin", proj.tech_stack)


if __name__ == "__main__":
    unittest.main()
