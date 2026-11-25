package com.cortex.app

import com.cortex.app.database.entities.AcademicProfileEntity
import com.cortex.app.database.entities.GoalEntity
import com.cortex.app.database.entities.MemoryEntity
import com.cortex.app.database.entities.ProjectEntity
import com.cortex.app.database.entities.TaskEntity
import com.cortex.app.privacy.FinancialExclusionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2EntitiesAndIngestionTest {

    @Test
    fun testMemoryEntity_CreationAndDefaults() {
        val mem = MemoryEntity(
            id = "mem_123",
            content = "Two-phase commit ensures atomic commitment across distributed nodes.",
            category = "ACADEMIC",
            sourceConnector = "user_explicit",
            isPinned = true,
            tagsJson = "[\"distributed\",\"database\",\"concurrency\"]"
        )

        assertEquals("mem_123", mem.id)
        assertEquals("ACADEMIC", mem.category)
        assertTrue(mem.isPinned)
        assertTrue(mem.tagsJson.contains("distributed"))
        assertEquals("user_explicit", mem.sourceConnector)
    }

    @Test
    fun testMemoryCategories_SupportedList() {
        val categories = listOf("PERSONAL", "EPISODIC", "ACADEMIC", "PROJECT", "KNOWLEDGE")
        assertTrue(categories.contains("PERSONAL"))
        assertTrue(categories.contains("EPISODIC"))
        assertTrue(categories.contains("ACADEMIC"))
        assertTrue(categories.contains("PROJECT"))
        assertTrue(categories.contains("KNOWLEDGE"))
    }

    @Test
    fun testTaskAndGoalEntities() {
        val task = TaskEntity(
            id = "task_001",
            title = "Prepare Mid-Term Presentation",
            description = "Slides on Paxos vs Raft consensus",
            priority = "HIGH",
            status = "PENDING",
            category = "ACADEMIC",
            dueDateMs = 1735000000000L
        )

        assertEquals("HIGH", task.priority)
        assertEquals("PENDING", task.status)
        assertEquals("task_001", task.id)
        assertEquals("ACADEMIC", task.category)

        val goal = GoalEntity(
            id = "goal_001",
            title = "Master Cloud Architecture",
            description = "Build 3 scalable distributed systems",
            goalType = "ACADEMIC",
            targetDateMs = 1740000000000L,
            progressPercent = 45,
            status = "ACTIVE"
        )

        assertEquals(45, goal.progressPercent)
        assertEquals("ACADEMIC", goal.goalType)
        assertEquals("ACTIVE", goal.status)
    }

    @Test
    fun testProjectEntity() {
        val proj = ProjectEntity(
            id = "proj_cortex",
            name = "Cortex",
            description = "Personal offline-first second-brain app",
            objective = "Lifelong private intelligence",
            status = "ACTIVE",
            techStackJson = "[\"Kotlin\",\"Python\",\"Room\",\"Chaquopy\"]",
            progressPercent = 80
        )

        assertEquals("Cortex", proj.name)
        assertEquals("ACTIVE", proj.status)
        assertTrue(proj.techStackJson.contains("Chaquopy"))
        assertEquals(80, proj.progressPercent)
    }

    @Test
    fun testAcademicProfileEntity() {
        val profile = AcademicProfileEntity(
            id = "default_profile",
            institution = "Engineering Institute",
            degree = "B.Tech",
            department = "Computer Science",
            semester = 6,
            academicYear = "2025-2026"
        )

        assertEquals("B.Tech", profile.degree)
        assertEquals(6, profile.semester)
        assertEquals("Engineering Institute", profile.institution)
    }

    @Test
    fun testFinancialExclusionGating_ProtectsMemoryIngestion() {
        // Any package identified as financial must be rejected before memory ingestion
        val bankingPackages = listOf(
            "com.google.android.apps.nbu.paisa.user",
            "com.phonepe.app",
            "net.one97.paytm",
            "in.org.npci.upiapp",
            "com.snapwork.hdfc",
            "com.sbi.lotusintouch",
            "com.binance.dev",
            "com.coinbase.android"
        )

        for (pkg in bankingPackages) {
            val isExcluded = FinancialExclusionPolicy.isFinancialPackage(pkg)
            assertTrue("Expected $pkg to be excluded by financial policy", isExcluded)
        }

        // Whitelisted safe application packages must pass gating
        val safePackages = listOf(
            "com.google.android.gm",
            "com.google.android.calendar",
            "com.whatsapp",
            "org.telegram.messenger",
            "com.google.android.apps.classroom",
            "com.google.android.apps.docs"
        )

        for (pkg in safePackages) {
            val isExcluded = FinancialExclusionPolicy.isFinancialPackage(pkg)
            assertFalse("Expected $pkg to be allowed through privacy gate", isExcluded)
        }
    }
}
