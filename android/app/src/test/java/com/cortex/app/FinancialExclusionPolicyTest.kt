package com.cortex.app

import com.cortex.app.privacy.FinancialExclusionPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialExclusionPolicyTest {

    @Test
    fun testPermanentDenylist_ContainsMajorIndianUpiApps() {
        assertTrue(FinancialExclusionPolicy.isPermanentlyExcluded("com.google.android.apps.nbu.paisa.user"))
        assertTrue(FinancialExclusionPolicy.isPermanentlyExcluded("com.phonepe.app"))
        assertTrue(FinancialExclusionPolicy.isPermanentlyExcluded("net.one97.paytm"))
        assertTrue(FinancialExclusionPolicy.isPermanentlyExcluded("in.org.npci.upiapp"))
        assertTrue(FinancialExclusionPolicy.isPermanentlyExcluded("com.dreamplug.androidapp"))
    }

    @Test
    fun testPermanentDenylist_ContainsMajorBanks() {
        assertTrue(FinancialExclusionPolicy.isPermanentlyExcluded("com.snapwork.hdfc"))
        assertTrue(FinancialExclusionPolicy.isPermanentlyExcluded("com.sbi.lotusintouch"))
        assertTrue(FinancialExclusionPolicy.isPermanentlyExcluded("com.csam.icici.bank.imobile"))
        assertTrue(FinancialExclusionPolicy.isPermanentlyExcluded("com.chase.sig.android"))
    }

    @Test
    fun testFinancialPackage_HeuristicAndCustomDenylist() {
        val customSet = setOf("com.secret.crypto.wallet")
        assertTrue(FinancialExclusionPolicy.isFinancialPackage("com.secret.crypto.wallet", customSet))
        assertTrue(FinancialExclusionPolicy.isFinancialPackage("com.mybank.netbanking"))
        assertFalse(FinancialExclusionPolicy.isFinancialPackage("com.whatsapp", customSet))
        assertFalse(FinancialExclusionPolicy.isFinancialPackage("com.google.android.gm", customSet))
    }
}
