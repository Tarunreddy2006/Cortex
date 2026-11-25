package com.cortex.app

import com.cortex.app.database.entities.ConversationEntity
import com.cortex.app.database.entities.ConversationMessageEntity
import com.cortex.app.database.entities.DecisionRecordEntity
import com.cortex.app.database.entities.ProactiveAlertEntity
import com.cortex.app.database.entities.ProposedActionEntity
import com.cortex.app.privacy.FinancialExclusionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3EntitiesAndIntelligenceTest {

    @Test
    fun testConversationEntities() {
        val conv = ConversationEntity(
            id = "conv_101",
            title = "Academic Planning & SeleneOS"
        )
        assertEquals("conv_101", conv.id)
        assertEquals("Academic Planning & SeleneOS", conv.title)

        val userMsg = ConversationMessageEntity(
            id = "msg_01",
            conversationId = "conv_101",
            sender = "USER",
            text = "What is pending in SeleneOS?"
        )
        assertEquals("USER", userMsg.sender)
        assertEquals("conv_101", userMsg.conversationId)

        val cortexMsg = ConversationMessageEntity(
            id = "msg_02",
            conversationId = "conv_101",
            sender = "CORTEX",
            text = "SeleneOS has 1 pending milestone: Memory paging system.",
            sourcesJson = "[\"Project: 'SeleneOS'\"]",
            intent = "PROJECT",
            referencedProject = "SeleneOS"
        )
        assertEquals("CORTEX", cortexMsg.sender)
        assertEquals("PROJECT", cortexMsg.intent)
        assertEquals("SeleneOS", cortexMsg.referencedProject)
    }

    @Test
    fun testProposedActionEntity() {
        val action = ProposedActionEntity(
            id = "act_001",
            actionType = "CREATE_TASK",
            title = "Add Task: 'Implement page fault handler'",
            description = "Creates high priority task for SeleneOS",
            payloadJson = "{\"title\":\"Implement page fault handler\",\"priority\":\"HIGH\"}",
            status = "PROPOSED"
        )
        assertEquals("PROPOSED", action.status)
        assertEquals("CREATE_TASK", action.actionType)
        assertTrue(action.payloadJson.contains("page fault"))
    }

    @Test
    fun testDecisionRecordEntity() {
        val dec = DecisionRecordEntity(
            id = "dec_001",
            statement = "Should I adopt Raft or Paxos for cluster consensus?",
            recommendation = "Adopt Raft for decomposed leader election and comprehensibility.",
            status = "EVALUATED"
        )
        assertEquals("dec_001", dec.id)
        assertTrue(dec.recommendation.contains("Raft"))
        assertEquals("EVALUATED", dec.status)
    }

    @Test
    fun testProactiveAlertEntity() {
        val alert = ProactiveAlertEntity(
            id = "al_01",
            alertKey = "exam_countdown_cs601",
            alertType = "UPCOMING_EXAM",
            title = "Upcoming Exam: CS601 Distributed Systems",
            description = "Mid-term exam in 3 days. Review syllabus modules.",
            priority = "URGENT",
            isDismissed = false
        )
        assertEquals("URGENT", alert.priority)
        assertFalse(alert.isDismissed)
        assertEquals("UPCOMING_EXAM", alert.alertType)
    }

    @Test
    fun testFinancialPrivacyGate_StrictIntelligenceExclusion() {
        // Excluded packages must never pass financial check
        val finPackages = listOf(
            "com.google.android.apps.nbu.paisa.user",
            "com.phonepe.app",
            "net.one97.paytm",
            "com.snapwork.hdfc",
            "com.sbi.lotusintouch"
        )
        for (pkg in finPackages) {
            assertTrue(FinancialExclusionPolicy.isFinancialPackage(pkg))
        }

        // Permitted apps
        assertFalse(FinancialExclusionPolicy.isFinancialPackage("com.whatsapp"))
        assertFalse(FinancialExclusionPolicy.isFinancialPackage("com.google.android.gm"))
    }
}
