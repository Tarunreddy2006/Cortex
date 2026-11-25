# 🧠 Cortex — Your Another Brain

> **A personal second brain that runs 100% privately on your Android phone. No cloud servers. No subscriptions. Zero trackers.**

[![Download APK](https://img.shields.io/badge/Download-cortex--debug.apk-blue?style=for-the-badge&logo=android)](cortex-debug.apk)
[![Android](https://img.shields.io/badge/Android-14%20(API%2034)-3DDC84?style=for-the-badge&logo=android)](https://www.android.com/)
[![Architecture](https://img.shields.io/badge/Architecture-arm64--v8a-orange?style=for-the-badge)](https://developer.android.com)
[![Offline First](https://img.shields.io/badge/Privacy-100%25%20On--Device-success?style=for-the-badge)](https://github.com/Tarunreddy2006/Cortex)

---

## 💡 Why Cortex?

Our digital lives are scattered everywhere. 

Your university timetable is in one portal, assignment deadlines are in your email, project notes are buried in files, and daily tasks live in half a dozen different apps. Most "AI assistants" offer to help, but with major drawbacks: they send your private notes and data to remote cloud servers, charge hefty monthly fees, and don't actually understand your everyday schedule.

**Cortex was built to fix this.**

Cortex is an offline-first personal intelligence engine that lives right inside your pocket. It connects your authorized digital environment, understands your classes, tracks your ongoing projects, helps you study for upcoming exams, and answers your natural questions using your real life data.

Everything is processed and stored locally on your device. When your phone has no internet connection, Cortex works just as fast.

---

## 📦 Download and Install

You can install Cortex directly on your Android phone in two minutes without setting up any development environment.

### 📥 Direct APK Download
1. Download the pre-built APK file directly from this repository:
   👉 **[Download cortex-debug.apk](cortex-debug.apk)** *(~41 MB, built for Android `arm64-v8a`)*
2. Transfer or open the `.apk` file on your Android phone.
3. If prompted, allow your browser or file manager to **"Install unknown apps"**.
4. Tap **Install** and open **Cortex**!

### 💻 Or Install via ADB (from your PC)
```bash
adb devices
adb install -r cortex-debug.apk
```

---

## ✨ What Can Cortex Do?

### 💬 1. Ask Cortex (Conversational Assistant)
Talk to your second brain in everyday language. Cortex looks through your actual timetable, tasks, assignments, and notes to give you relevant answers instead of generic web search results:
- *"Do I have any classes today?"*
- *"What assignments are due this week?"*
- *"How is my SeleneOS project going, and what's blocking it?"*
- *"What should I study tonight?"*

Cortex understands conversational context and pronouns: if you ask about a project and then say *"What should I complete first?"*, Cortex immediately knows you're talking about that project's pending tasks.

---

### 📅 2. Adaptive Daily & Study Planner
- **Real Timetable Awareness**: Knows your daily class schedule and exam dates. Cortex enforces schedule truth—it never invents fake cancellations.
- **5-Day Exam Revision**: Approaching an exam? Cortex automatically balances your study blocks across the week based on syllabus coverage and free hours.
- **Guilt-Free Rescheduling**: Having an overwhelming day? Just say *"I can't finish this today"*, and Cortex evaluates deadline safety margins and suggests realistic deferrals.

---

### 🛡️ 3. Action Approvals (You Stay in Charge)
Cortex is helpful, never presumptuous. It **never modifies or deletes your data silently**.
Whenever it suggests scheduling a study block, creating a new task, or postponing a deadline, it generates a clear **Action Proposal Card**. You can review the details and confirm with a single tap, or reject it entirely.

---

### 🚀 4. Project Intelligence
Keep high-ambition technical projects (like operating systems, machine learning models, or web apps) on track:
- Visualizes milestone health and completion percentages.
- Surfaces dependencies and blockers early.
- Pinpoints the single highest-impact task you should work on next.

---

### ⚖️ 5. 10-Point Decision Workspace
Stuck between two hard choices? Cortex runs a structured, objective 10-point analysis:
1. Clarifies the core decision
2. Lists viable alternatives
3. Brings up relevant past decisions and memories
4. Evaluates concrete advantages
5. Highlights potential disadvantages
6. Identifies real-world risks
7. Flags unknown information
8. Surfaces hidden assumptions
9. Recommends a clear path forward
10. Explains what conditions would change that recommendation

---

### 🔒 6. Strict Financial Exclusion (Privacy First)
Your financial life is sacred. Cortex has an **ironclad, non-bypassable privacy firewall**:
- Permanently denylists all banking applications, UPI payment apps (**Google Pay, PhonePe, Paytm, BHIM, CRED**), and crypto/trading platforms.
- Automatically discards any notification or snippet containing sensitive monetary or transaction keywords (`debited`, `credited`, `OTP`, `UPI PIN`, `CVV`, `account balance`).
- Your money, accounts, and passwords remain completely invisible to Cortex.

---

### 📱 7. Modern Dark-Mode Experience
- **Side-Scrollable Bottom Dock**: Swipe smoothly with one thumb across all 14 workspaces:
  - 🏠 **Dashboard**: Quick metrics, daily schedule overview, and health glance.
  - 💬 **Ask Cortex**: Full-featured conversational assistant with citation pills.
  - ⚡ **Action Center**: Pending action proposals awaiting your green light.
  - 🗓️ **Daily Planner**: Adaptive agenda, timetable review, and revision blocks.
  - ⚖️ **Decision Studio**: Step-by-step trade-off analysis.
  - 🧠 **Memory Engine**: Episodic personal knowledge and fact reasoner.
  - 📚 **Knowledge Vault**: Clean document viewer and full-text search.
  - 🎓 **Academic Hub**: Timetable, subject list, assignments, and exams.
  - ✅ **Daily Life**: Prioritized tasks, deadlines, and personal goals.
  - 💻 **Projects**: Deep dive into active project milestones and blockers.
  - ⏳ **Timeline**: Chronological feed of your digital life events.
  - 🤖 **Local AI**: On-device model status and specs (Qwen 2.5 / SmolLM2).
  - 🔌 **Connectors**: Control app permissions, notifications, and Google links.
  - ⚙️ **Settings**: Privacy policies, custom denylists, and full data wipe.

---

## 🏗️ Architecture: How It Works

Cortex uses a thoughtful hybrid architecture designed to squeeze maximum performance out of Android hardware:

```
┌─────────────────────────────────────────────────────────────┐
│                 FRONTEND LAYER (WebView)                    │
│   HTML5 • Modern Dark CSS3 • Vanilla JS • 14 Workspaces     │
│   Side-scrollable bottom dock • Zero external web assets    │
└──────────────────────────────┬──────────────────────────────┘
                               │ Asynchronous JSON Bridge
┌──────────────────────────────▼──────────────────────────────┐
│                  KOTLIN ANDROID LAYER                       │
│   Application Lifecycle • Room SQLite Database (v3)         │
│   NotificationListenerService • UsageStatsManager           │
│   Calendar & Contacts Content Providers • WorkManager       │
└──────────────────────────────┬──────────────────────────────┘
                               │ JNI Binder Bridge (Chaquopy)
┌──────────────────────────────▼──────────────────────────────┐
│               PYTHON INTELLIGENCE ENGINE                    │
│   Embedded Python 3.11 Runtime (ARM64)                      │
│   Intent Router • BM25 RAG Engine • Ask Cortex Assistant    │
│   Academic & Life Planner • Decision Evaluator              │
│   Proactive Alert Monitor • Privacy Sanitizer               │
└─────────────────────────────────────────────────────────────┘
```

- **Kotlin + Room (SQLite)**: Ensures butter-smooth UI rendering, rock-solid multithreading on Android binder services, and permanent data persistence on device.
- **Embedded Python via Chaquopy**: Brings the unmatched data-processing and natural language ecosystem of Python directly inside the APK—no Termux, no root, and no command line required on the phone.
- **Pure Local Web UI**: Lightning-fast, customizable, and completely self-contained.

---

## 🎯 Target Device Specifications

Tested and verified on:
- **Device**: Infinix Series Smartphone
- **Android Version**: Android 14 (API Level 34)
- **RAM**: 12 GB
- **Internal Storage**: 256 GB
- **CPU ABI**: `arm64-v8a` (64-bit ARM)

---

## 🛠️ Building from Source

If you want to compile Cortex yourself using Android Studio or command-line tools:

### Prerequisites
- **Operating System**: Windows 10/11, macOS, or Linux
- **JDK**: Java Development Kit 17 or 21 (e.g. Android Studio JBR)
- **Android SDK**: Android API 34 platform & build-tools 34.0.0
- **Python**: Python 3.11+ installed on your host machine

### Clone & Build
```bash
# 1. Clone the repository
git clone https://github.com/Tarunreddy2006/Cortex.git
cd Cortex

# 2. Run the test suites
# Python Intelligence Engine tests (41 tests)
python -m unittest discover -s tests/python -p "test_*.py"

# Android Kotlin & Room tests (16 tests)
cd android
./gradlew testDebugUnitTest

# 3. Assemble the debug APK
./gradlew assembleDebug

# Output APK location:
# android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License

Personal project developed by **Tarun Reddy**. All rights reserved. Free for personal and educational use.
