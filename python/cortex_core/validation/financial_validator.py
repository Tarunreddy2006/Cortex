"""
Cortex Core - Financial Exclusion Validator
Strict, non-negotiable security layer that protects financial data.
Prevents any financial applications or financial content from entering Cortex persistent storage.
"""

import re
from typing import Set, List, Optional, Tuple
from cortex_core.models.base import BaseRecord
from cortex_core.models.notification import NotificationRecord
from cortex_core.models.email import EmailRecord


# Permanent, non-bypassable denylist of known financial package identifiers.
# This list takes precedence over any user allowlist or 'Select All' action.
PERMANENT_FINANCIAL_PACKAGES: Set[str] = {
    # UPI & Digital Wallets
    "com.google.android.apps.nbu.paisa.user",       # Google Pay (Tez)
    "com.phonepe.app",                             # PhonePe
    "net.one97.paytm",                             # Paytm
    "in.org.npci.upiapp",                          # BHIM UPI
    "com.dreamplug.androidapp",                    # CRED
    "com.mobikwik_new",                            # MobiKwik
    "com.freecharge.android",                      # Freecharge
    "com.amazon.mShop.android.shopping.pay",       # Amazon Pay component
    "com.samsung.android.spay",                    # Samsung Pay
    "com.samsung.android.spaymini",                # Samsung Pay Mini
    "com.google.android.apps.walletnfcrel",        # Google Wallet

    # Indian Major Banks
    "com.sbi.lotusintouch",                        # YONO SBI
    "com.sbi.SBIFreedomPlus",                      # SBI Anywhere
    "com.snapwork.hdfc",                           # HDFC MobileBanking
    "com.hdfcbank.payzapp",                        # PayZapp
    "com.csam.icici.bank.imobile",                 # iMobile ICICI
    "com.axis.mobile",                             # Axis Mobile
    "com.kotak.kotakbank",                         # Kotak 811
    "com.canarabank.mobility",                     # Canara ai1
    "com.bankofbaroda.mconnect",                   # bob World
    "com.punjabbank.mb",                           # PNB ONE
    "com.unionbank.ecommerce.mobile.android",      # Union Bank Vyom
    "com.indusind.mpassbook",                      # IndusMobile
    "com.msf.kbl.mobilebanking",                   # Karnataka Bank
    "com.fss.idbi",                                # IDBI Bank GO Mobile+
    "com.rblbank.mobank",                          # RBL MoBank
    "com.aubank.mobilebanking",                    # AU 0101
    "com.federalbank.fedmobile",                   # FedMobile
    "com.idfcfirstbank.optimus",                   # IDFC FIRST Bank

    # Global Major Banks
    "com.chase.sig.android",                       # Chase Mobile
    "com.infonow.bofa",                            # Bank of America
    "com.wf.wellsfargomobile",                     # Wells Fargo
    "com.citibank.mobile.citiandroid",             # Citi Mobile
    "com.barclays.android.barclaysmobilebanking",  # Barclays
    "com.hsbc.hsbcukmobilebanking",                # HSBC Mobile
    "com.grppl.android.shell.CMBlloydsTSB73",      # Lloyds Bank
    "com.revolut.revolut",                         # Revolut
    "com.monzo.android",                           # Monzo
    "com.wise.android",                            # Wise

    # Investment, Stock Trading & Wealth
    "com.zerodha.kite3",                           # Zerodha Kite
    "com.zerodha.coin",                            # Zerodha Coin
    "com.nextbillion.groww",                       # Groww
    "com.msf.angelmobile",                         # Angel One
    "com.upstox.pro",                              # Upstox
    "com.dhan.live",                               # Dhan
    "com.fivepaisa.invest",                        # 5paisa
    "com.invest.etmoney",                          # ET Money
    "com.kuvera.app",                              # Kuvera
    "in.indmoney",                                 # INDmoney
    "com.tastytrade.tastytrade",                   # Tastytrade
    "com.robinhood.android",                       # Robinhood
    "com.tdameritrade.mobile3",                    # TD Ameritrade
    "com.fidelity.android",                        # Fidelity Investments
    "com.webull.mn",                               # Webull

    # Crypto & Digital Assets
    "com.binance.dev",                             # Binance
    "com.coindcx.btc",                             # CoinDCX
    "com.wazirx.android",                          # WazirX
    "com.coinswitch.kuber",                        # CoinSwitch
    "com.wallet.crypto.trustapp",                  # Trust Wallet
    "io.metamask",                                 # MetaMask
    "com.coinbase.android",                        # Coinbase
    "com.okx.mobile",                              # OKX
    "com.bybit.app",                               # Bybit

    # Lending, BNPL & Microfinance
    "com.paymentech.lazypay",                      # LazyPay
    "com.simpl.simplpay",                          # Simpl
    "com.bajajfinserv.market",                     # Bajaj Finserv
    "com.navi.navi",                               # Navi Finserv
    "com.kreditbee.android",                       # KreditBee
    "com.whizdm.moneyview",                        # Money View
    "com.slicepay",                                # slice
    "com.onecard.app",                             # OneCard
}

# Regex patterns matching package names that strongly suggest financial activity
FINANCIAL_PACKAGE_PATTERNS = [
    re.compile(r"\bbanking\b", re.IGNORECASE),
    re.compile(r"\bfinance\b", re.IGNORECASE),
    re.compile(r"\bwallet\b", re.IGNORECASE),
    re.compile(r"\bpayment\b", re.IGNORECASE),
    re.compile(r"\bupiapp\b", re.IGNORECASE),
    re.compile(r"\btrading\b", re.IGNORECASE),
    re.compile(r"\bnetbanking\b", re.IGNORECASE),
    re.compile(r"\bpassbook\b", re.IGNORECASE),
]

# Sensitive financial content triggers (transaction alerts, OTPs, balance updates, card info)
FINANCIAL_CONTENT_REGEXES = [
    # OTP / PIN / Verification Code
    re.compile(r"\b(OTP|one[-\s]time\s+password|security\s+code|verification\s+code)\b", re.IGNORECASE),
    re.compile(r"\b(UPI\s+PIN|ATM\s+PIN|CVV|CVC)\b", re.IGNORECASE),

    # Transaction alerts (Debit/Credit/Transferred/Refunded)
    re.compile(r"\b(debited|credited|transferred|withdrawn|deposited|remitted)\b", re.IGNORECASE),
    re.compile(r"\b(INR|Rs\.?|₹|\$|USD|EUR|GBP)\s*[\d,]+(\.\d{2})?\b", re.IGNORECASE),
    re.compile(r"\b(account\s+balance|avail(able)?\s+bal(ance)?|a/c\s+bal)\b", re.IGNORECASE),
    re.compile(r"\b(a/c\s+(no\.?|ending)|acct\s+ending|card\s+ending\s+in)\s*(\d{4}|x+\d{4})\b", re.IGNORECASE),
    re.compile(r"\b(NEFT|RTGS|IMPS|UPI\s+Ref|UTR\s+no\.?)\b", re.IGNORECASE),

    # Sensitive banking/credit lines
    re.compile(r"\b(credit\s+card\s+statement|loan\s+account|minimum\s+amount\s+due|payment\s+due\s+date)\b", re.IGNORECASE),
]

# Known financial email domains and sender patterns
FINANCIAL_SENDER_PATTERNS = [
    re.compile(r"alerts?@", re.IGNORECASE),
    re.compile(r"noreply@.*bank.*", re.IGNORECASE),
    re.compile(r"statement.*@", re.IGNORECASE),
    re.compile(r".*@.*(hdfcbank|icicibank|sbi|axisbank|kotak|paytm|phonepe|cred|zerodha|groww)\.com", re.IGNORECASE),
]


class FinancialExclusionValidator:
    """Non-negotiable gatekeeper preventing financial data ingestion."""

    @staticmethod
    def is_financial_package(package_name: str, custom_denylist: Optional[Set[str]] = None) -> bool:
        """
        Check if an application package is classified as financial.
        Returns True if the package is permanently blacklisted, matches known financial
        patterns, or is present in the user-specified custom denylist.
        """
        if not package_name:
            return False

        pkg_lower = package_name.strip().lower()

        # 1. Check permanent hardcoded financial list
        if pkg_lower in {p.lower() for p in PERMANENT_FINANCIAL_PACKAGES}:
            return True

        # 2. Check custom user-defined denylist
        if custom_denylist:
            if pkg_lower in {p.lower() for p in custom_denylist}:
                return True

        # 3. Check package name heuristic patterns
        for pattern in FINANCIAL_PACKAGE_PATTERNS:
            if pattern.search(pkg_lower):
                return True

        return False

    @staticmethod
    def contains_financial_content(text: Optional[str], sender: Optional[str] = None) -> Tuple[bool, str]:
        """
        Examine content (notification text, email body/snippet) for financial traces.
        Returns (is_financial, reason).
        Principle: If not confident that content is non-financial, flag it to prevent storage.
        """
        if sender:
            sender_lower = sender.strip().lower()
            for pattern in FINANCIAL_SENDER_PATTERNS:
                if pattern.search(sender_lower):
                    return True, f"Sender matches financial pattern: {pattern.pattern}"

        if not text:
            return False, "Empty content"

        clean_text = text.strip()
        for regex in FINANCIAL_CONTENT_REGEXES:
            match = regex.search(clean_text)
            if match:
                return True, f"Content triggered financial rule: {match.group(0)}"

        return False, "Clean"

    @classmethod
    def validate_for_ingestion(cls, record: BaseRecord, custom_denylist: Optional[Set[str]] = None) -> Tuple[bool, str]:
        """
        Final safety gate: Verifies whether a record is safe to persist.
        Returns (is_allowed, reason).
        """
        if isinstance(record, NotificationRecord):
            # Check source package
            if cls.is_financial_package(record.package_name, custom_denylist):
                return False, f"Package {record.package_name} is on financial exclusion denylist"

            # Check notification text and title
            combined_text = f"{record.title} {record.text}".strip()
            is_fin, reason = cls.contains_financial_content(combined_text)
            if is_fin:
                return False, f"Notification content flagged: {reason}"

        elif isinstance(record, EmailRecord):
            # Check sender
            is_fin, reason = cls.contains_financial_content(record.snippet, record.sender)
            if is_fin:
                return False, f"Email flagged as financial: {reason}"

            # Check subject
            is_fin_subj, reason_subj = cls.contains_financial_content(record.subject)
            if is_fin_subj:
                return False, f"Email subject flagged as financial: {reason_subj}"

        return True, "Safe"
