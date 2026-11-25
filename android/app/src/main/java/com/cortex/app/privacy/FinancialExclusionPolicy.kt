package com.cortex.app.privacy

import java.util.Locale
import java.util.regex.Pattern

object FinancialExclusionPolicy {

    val PERMANENT_FINANCIAL_PACKAGES: Set<String> = setOf(
        // UPI & Digital Wallets
        "com.google.android.apps.nbu.paisa.user",       // Google Pay
        "com.phonepe.app",                             // PhonePe
        "net.one97.paytm",                             // Paytm
        "in.org.npci.upiapp",                          // BHIM UPI
        "com.dreamplug.androidapp",                    // CRED
        "com.mobikwik_new",                            // MobiKwik
        "com.freecharge.android",                      // Freecharge
        "com.samsung.android.spay",                    // Samsung Pay
        "com.google.android.apps.walletnfcrel",        // Google Wallet

        // Indian Banking
        "com.sbi.lotusintouch",                        // YONO SBI
        "com.sbi.SBIFreedomPlus",                      // SBI Anywhere
        "com.snapwork.hdfc",                           // HDFC Bank Mobile
        "com.hdfcbank.payzapp",                        // PayZapp
        "com.csam.icici.bank.imobile",                 // iMobile ICICI
        "com.axis.mobile",                             // Axis Mobile
        "com.kotak.kotakbank",                         // Kotak 811
        "com.canarabank.mobility",                     // Canara ai1
        "com.bankofbaroda.mconnect",                   // bob World
        "com.punjabbank.mb",                           // PNB ONE
        "com.unionbank.ecommerce.mobile.android",      // Union Bank Vyom
        "com.indusind.mpassbook",                      // IndusMobile
        "com.federalbank.fedmobile",                   // FedMobile
        "com.idfcfirstbank.optimus",                   // IDFC FIRST Bank
        "com.rblbank.mobank",                          // RBL MoBank
        "com.aubank.mobilebanking",                    // AU 0101

        // Global Banking
        "com.chase.sig.android",                       // Chase
        "com.infonow.bofa",                            // Bank of America
        "com.wf.wellsfargomobile",                     // Wells Fargo
        "com.citibank.mobile.citiandroid",             // Citi Mobile
        "com.barclays.android.barclaysmobilebanking",  // Barclays
        "com.hsbc.hsbcukmobilebanking",                // HSBC Mobile
        "com.revolut.revolut",                         // Revolut
        "com.monzo.android",                           // Monzo
        "com.wise.android",                            // Wise

        // Investment, Trading & Wealth
        "com.zerodha.kite3",                           // Zerodha Kite
        "com.zerodha.coin",                            // Zerodha Coin
        "com.nextbillion.groww",                       // Groww
        "com.msf.angelmobile",                         // Angel One
        "com.upstox.pro",                              // Upstox
        "com.dhan.live",                               // Dhan
        "com.fivepaisa.invest",                        // 5paisa
        "com.invest.etmoney",                          // ET Money
        "in.indmoney",                                 // INDmoney
        "com.robinhood.android",                       // Robinhood
        "com.fidelity.android",                        // Fidelity

        // Crypto & Digital Assets
        "com.binance.dev",                             // Binance
        "com.coindcx.btc",                             // CoinDCX
        "com.wazirx.android",                          // WazirX
        "com.coinswitch.kuber",                        // CoinSwitch
        "com.wallet.crypto.trustapp",                  // Trust Wallet
        "io.metamask",                                 // MetaMask
        "com.coinbase.android",                        // Coinbase

        // Credit, BNPL & Microfinance
        "com.paymentech.lazypay",                      // LazyPay
        "com.simpl.simplpay",                          // Simpl
        "com.bajajfinserv.market",                     // Bajaj Finserv
        "com.navi.navi",                               // Navi
        "com.kreditbee.android",                       // KreditBee
        "com.whizdm.moneyview",                        // Money View
        "com.slicepay",                                // slice
        "com.onecard.app"                              // OneCard
    )

    private val FINANCIAL_KEYWORDS = listOf(
        "banking", "finance", "wallet", "payment", "upiapp", "trading", "netbanking", "passbook"
    )

    fun isPermanentlyExcluded(packageName: String): Boolean {
        return PERMANENT_FINANCIAL_PACKAGES.contains(packageName.trim().lowercase(Locale.ROOT))
    }

    fun isFinancialPackage(packageName: String, userExclusions: Set<String> = emptySet()): Boolean {
        val pkgLower = packageName.trim().lowercase(Locale.ROOT)
        if (isPermanentlyExcluded(pkgLower)) return true
        if (userExclusions.contains(pkgLower)) return true

        return FINANCIAL_KEYWORDS.any { keyword ->
            pkgLower.contains(keyword)
        }
    }

    fun getKnownAppName(packageName: String): String {
        return when (packageName) {
            "com.google.android.apps.nbu.paisa.user" -> "Google Pay"
            "com.phonepe.app" -> "PhonePe"
            "net.one97.paytm" -> "Paytm"
            "in.org.npci.upiapp" -> "BHIM UPI"
            "com.dreamplug.androidapp" -> "CRED"
            "com.snapwork.hdfc" -> "HDFC MobileBanking"
            "com.sbi.lotusintouch" -> "YONO SBI"
            "com.csam.icici.bank.imobile" -> "iMobile ICICI"
            "com.axis.mobile" -> "Axis Mobile"
            "com.kotak.kotakbank" -> "Kotak 811"
            "com.zerodha.kite3" -> "Zerodha Kite"
            "com.nextbillion.groww" -> "Groww"
            "com.binance.dev" -> "Binance"
            else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }
}
