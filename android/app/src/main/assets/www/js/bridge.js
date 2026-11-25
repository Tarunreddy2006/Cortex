/**
 * Cortex JavaScript Bridge Interface
 * Communicates with native Kotlin backend via window.AndroidBridge.
 * Uses asynchronous Promises and a callback registry.
 */

window.CortexBridgeCallbacks = {
  _callbacks: {},
  register: function (resolve, reject) {
    const id = "cb_" + Date.now() + "_" + Math.random().toString(36).substring(2, 9);
    this._callbacks[id] = { resolve, reject };
    return id;
  },
  handle: function (callbackId, response) {
    const cb = this._callbacks[callbackId];
    if (cb) {
      if (response && response.success) {
        cb.resolve(response.data);
      } else {
        cb.reject(new Error(response?.error || "Unknown Bridge Error"));
      }
      delete this._callbacks[callbackId];
    }
  }
};

class CortexBridge {
  constructor() {
    this.isAndroid = typeof window.AndroidBridge !== "undefined";
    if (!this.isAndroid) {
      console.warn("CortexBridge: AndroidBridge not detected. Running in browser mock simulation mode.");
    }
  }

  _callNative(method, ...args) {
    return new Promise((resolve, reject) => {
      if (!this.isAndroid) {
        this._mockCall(method, args, resolve, reject);
        return;
      }
      try {
        const callbackId = window.CortexBridgeCallbacks.register(resolve, reject);
        if (typeof window.AndroidBridge[method] !== "function") {
          reject(new Error(`Method AndroidBridge.${method} not found`));
          return;
        }
        window.AndroidBridge[method](...args, callbackId);
      } catch (err) {
        reject(err);
      }
    });
  }

  // ==================== PHASE 1: APP & SYSTEM INTEGRATIONS ====================

  getInstalledApps() {
    return this._callNative("getInstalledApps");
  }

  saveAppSelections(selectionsMap) {
    const jsonStr = JSON.stringify(selectionsMap);
    return this._callNative("saveAppSelections", jsonStr);
  }

  getPermissionStatus() {
    return this._callNative("getPermissionStatus");
  }

  openPermissionSettings(permissionType) {
    if (this.isAndroid && typeof window.AndroidBridge.openPermissionSettings === "function") {
      window.AndroidBridge.openPermissionSettings(permissionType);
    } else {
      console.log("Mock: openPermissionSettings", permissionType);
    }
  }

  getConnectors() {
    return this._callNative("getConnectors");
  }

  syncConnector(connectorId) {
    return this._callNative("syncConnector", connectorId);
  }

  deleteConnectorData(connectorId) {
    return this._callNative("deleteConnectorData", connectorId);
  }

  getUsageStats() {
    return this._callNative("getUsageStats");
  }

  getFinancialExclusions() {
    return this._callNative("getFinancialExclusions");
  }

  addCustomFinancialExclusion(packageName, reason = "User Custom Exclusion") {
    return this._callNative("addCustomFinancialExclusion", packageName, reason);
  }

  removeCustomFinancialExclusion(packageName) {
    return this._callNative("removeCustomFinancialExclusion", packageName);
  }

  getSyncHistory() {
    return this._callNative("getSyncHistory");
  }

  wipeAllData() {
    return this._callNative("wipeAllData");
  }

  // ==================== PHASE 2: MEMORY ENGINE ====================

  getMemories(category = "") {
    return this._callNative("getMemories", category);
  }

  addMemory(content, category = "PERSONAL", isPinned = false) {
    return this._callNative("addMemory", content, category, !!isPinned);
  }

  togglePinMemory(id, isPinned) {
    return this._callNative("togglePinMemory", id, !!isPinned);
  }

  deleteMemory(id) {
    return this._callNative("deleteMemory", id);
  }

  // ==================== PHASE 2: KNOWLEDGE VAULT ====================

  getDocuments(collectionId = "") {
    return this._callNative("getDocuments", collectionId);
  }

  getNotes(collectionId = "") {
    return this._callNative("getNotes", collectionId);
  }

  saveNote(id = "", title, content, collectionId = "") {
    return this._callNative("saveNote", id, title, content, collectionId);
  }

  deleteNote(id) {
    return this._callNative("deleteNote", id);
  }

  getCollections() {
    return this._callNative("getCollections");
  }

  addCollection(name, description = "", colorHex = "#3B82F6", icon = "folder") {
    return this._callNative("addCollection", name, description, colorHex, icon);
  }

  // ==================== PHASE 2: ACADEMIC INTELLIGENCE ====================

  getAcademicProfile() {
    return this._callNative("getAcademicProfile");
  }

  saveAcademicProfile(institution, degree, department, semester, academicYear, gpa) {
    return this._callNative("saveAcademicProfile",
      institution || "",
      degree || "",
      department || "",
      parseInt(semester, 10) || 1,
      academicYear || "",
      parseFloat(gpa) || 0.0
    );
  }

  getSubjects() {
    return this._callNative("getSubjects");
  }

  saveSubject(id = "", code, name, faculty = "", credits = 3, colorHex = "#3B82F6") {
    return this._callNative("saveSubject",
      id,
      code || "",
      name || "",
      faculty || "",
      parseInt(credits, 10) || 3,
      colorHex || "#3B82F6"
    );
  }

  deleteSubject(id) {
    return this._callNative("deleteSubject", id);
  }

  getTimetable(dayOfWeek = 0) {
    return this._callNative("getTimetable", parseInt(dayOfWeek, 10) || 0);
  }

  saveTimetableEntry(id = "", dayOfWeek, subjectId, startTime, endTime, room = "") {
    return this._callNative("saveTimetableEntry",
      id,
      parseInt(dayOfWeek, 10) || 1,
      subjectId || "",
      startTime || "09:00",
      endTime || "10:00",
      room || ""
    );
  }

  deleteTimetableEntry(id) {
    return this._callNative("deleteTimetableEntry", id);
  }

  getAssignments() {
    return this._callNative("getAssignments");
  }

  saveAssignment(id = "", subjectId = "", title, description = "", dueDateMs = 0, priority = "MEDIUM") {
    return this._callNative("saveAssignment",
      id,
      subjectId,
      title || "",
      description || "",
      Math.floor(Number(dueDateMs) || Date.now() + 86400000),
      priority || "MEDIUM"
    );
  }

  updateAssignmentStatus(id, status) {
    return this._callNative("updateAssignmentStatus", id, status || "COMPLETED");
  }

  getExaminations() {
    return this._callNative("getExaminations");
  }

  saveExamination(id = "", subjectId = "", title, examDateMs = 0, duration = 180, room = "", syllabus = "") {
    return this._callNative("saveExamination",
      id,
      subjectId,
      title || "",
      Math.floor(Number(examDateMs) || Date.now() + 604800000),
      parseInt(duration, 10) || 180,
      room || "",
      syllabus || ""
    );
  }

  // ==================== PHASE 2: DAILY LIFE & TASKS ====================

  getTasks() {
    return this._callNative("getTasks");
  }

  saveTask(id = "", title, description = "", priority = "MEDIUM", category = "PERSONAL", dueDateMs = 0) {
    return this._callNative("saveTask",
      id,
      title || "",
      description || "",
      priority || "MEDIUM",
      category || "PERSONAL",
      Math.floor(Number(dueDateMs) || Date.now() + 86400000)
    );
  }

  updateTaskStatus(id, status) {
    return this._callNative("updateTaskStatus", id, status || "COMPLETED");
  }

  deleteTask(id) {
    return this._callNative("deleteTask", id);
  }

  getGoals() {
    return this._callNative("getGoals");
  }

  saveGoal(id = "", title, description = "", goalType = "PERSONAL", targetDateMs = 0, progressPercent = 0) {
    return this._callNative("saveGoal",
      id,
      title || "",
      description || "",
      goalType || "PERSONAL",
      Math.floor(Number(targetDateMs) || Date.now() + 2592000000),
      parseInt(progressPercent, 10) || 0
    );
  }

  updateGoalProgress(id, percent) {
    return this._callNative("updateGoalProgress", id, parseInt(percent, 10) || 0);
  }

  // ==================== PHASE 2: PROJECT INTELLIGENCE ====================

  getProjects() {
    return this._callNative("getProjects");
  }

  saveProject(id = "", name, description = "", objective = "", status = "ACTIVE", techStackJson = "[]", targetDateMs = 0) {
    return this._callNative("saveProject",
      id,
      name || "",
      description || "",
      objective || "",
      status || "ACTIVE",
      typeof techStackJson === "string" ? techStackJson : JSON.stringify(techStackJson || []),
      Math.floor(Number(targetDateMs) || Date.now() + 2592000000)
    );
  }

  deleteProject(id) {
    return this._callNative("deleteProject", id);
  }

  getProjectMilestones(projectId) {
    return this._callNative("getProjectMilestones", projectId);
  }

  saveProjectMilestone(id = "", projectId, title, targetDateMs = 0, isCompleted = false) {
    return this._callNative("saveProjectMilestone",
      id,
      projectId,
      title || "",
      Math.floor(Number(targetDateMs) || Date.now() + 604800000),
      !!isCompleted
    );
  }

  toggleMilestone(id, isCompleted) {
    return this._callNative("toggleMilestone", id, !!isCompleted);
  }

  getProjectDecisions(projectId) {
    return this._callNative("getProjectDecisions", projectId);
  }

  saveProjectDecision(id = "", projectId, title, rationale = "", status = "ACCEPTED") {
    return this._callNative("saveProjectDecision",
      id,
      projectId,
      title || "",
      rationale || "",
      status || "ACCEPTED"
    );
  }

  // ==================== PHASE 2: TIMELINE & SEARCH ====================

  getTimeline(limit = 50, offset = 0, filterType = "") {
    return this._callNative("getTimeline", parseInt(limit, 10) || 50, parseInt(offset, 10) || 0, filterType || "");
  }

  searchUniversal(query) {
    return this._callNative("searchUniversal", query || "");
  }

  getGoogleDiagnostics() {
    return this._callNative("getGoogleDiagnostics");
  }

  // ==================== PHASE 3: INTELLIGENCE, REASONING & AUTOMATION ====================

  askCortex(conversationId = "", query) {
    return this._callNative("askCortex", conversationId || "", query || "");
  }

  getConversations() {
    return this._callNative("getConversations");
  }

  getConversationMessages(conversationId) {
    return this._callNative("getConversationMessages", conversationId);
  }

  deleteConversation(conversationId) {
    return this._callNative("deleteConversation", conversationId);
  }

  generateBriefing(briefingType = "MORNING") {
    return this._callNative("generateBriefing", briefingType);
  }

  generateStudyPlan(days = 5, dailyHours = 3.0) {
    return this._callNative("generateStudyPlan", parseInt(days, 10) || 5, parseFloat(dailyHours) || 3.0);
  }

  generateDailyPlan() {
    return this._callNative("generateDailyPlan");
  }

  rescheduleTask(taskId, deferDays = 1) {
    return this._callNative("rescheduleTask", taskId, parseInt(deferDays, 10) || 1);
  }

  evaluateDecision(statement, alternatives = []) {
    const altsJson = typeof alternatives === "string" ? alternatives : JSON.stringify(alternatives || []);
    return this._callNative("evaluateDecision", statement, altsJson);
  }

  getDecisions() {
    return this._callNative("getDecisions");
  }

  deleteDecision(decisionId) {
    return this._callNative("deleteDecision", decisionId);
  }

  getProposedActions() {
    return this._callNative("getProposedActions");
  }

  approveAction(actionId) {
    return this._callNative("approveAction", actionId);
  }

  rejectAction(actionId) {
    return this._callNative("rejectAction", actionId);
  }

  getProactiveAlerts() {
    return this._callNative("getProactiveAlerts");
  }

  dismissProactiveAlert(alertId) {
    return this._callNative("dismissProactiveAlert", alertId);
  }

  getLocalAiStatus() {
    return this._callNative("getLocalAiStatus");
  }

  reasonMemories(action = "conflicts", targetMemory = {}) {
    const memJson = typeof targetMemory === "string" ? targetMemory : JSON.stringify(targetMemory || {});
    return this._callNative("reasonMemories", action, memJson);
  }

  // ==================== MOCK FALLBACK FOR DESKTOP ====================

  _mockCall(method, args, resolve, reject) {
    setTimeout(() => {
      switch (method) {
        case "getInstalledApps":
          resolve([
            { packageName: "com.whatsapp", appName: "WhatsApp", category: "COMMUNICATION", isSystemApp: false, isEnabled: true, supportedCapabilities: ["NOTIFICATION", "SHARE_IMPORT"] },
            { packageName: "com.google.android.gm", appName: "Gmail", category: "GOOGLE", isSystemApp: true, isEnabled: true, supportedCapabilities: ["OFFICIAL_API", "NOTIFICATION"] },
            { packageName: "com.google.android.apps.docs", appName: "Google Drive", category: "GOOGLE", isSystemApp: true, isEnabled: true, supportedCapabilities: ["OFFICIAL_API", "SHARE_IMPORT"] },
            { packageName: "com.google.android.apps.classroom", appName: "Google Classroom", category: "EDUCATION", isSystemApp: false, isEnabled: true, supportedCapabilities: ["OFFICIAL_API"] },
            { packageName: "com.phonepe.app", appName: "PhonePe", category: "FINANCIAL", isSystemApp: false, isEnabled: false, supportedCapabilities: ["EXCLUDED"] },
            { packageName: "com.google.android.apps.nbu.paisa.user", appName: "Google Pay", category: "FINANCIAL", isSystemApp: false, isEnabled: false, supportedCapabilities: ["EXCLUDED"] }
          ]);
          break;
        case "saveAppSelections":
          resolve({ saved: true });
          break;
        case "getPermissionStatus":
          resolve({
            notificationListener: false,
            usageStats: false,
            readCalendar: false,
            readContacts: false,
            postNotifications: true,
            allGranted: false,
            pendingCount: 4
          });
          break;
        case "getConnectors":
          resolve([
            { id: "android_calendar", displayName: "Google Calendar (Device)", category: "CALENDAR", isEnabled: true, isAuthorized: true, lastSyncStatus: "SUCCESS", recordsCount: 12 },
            { id: "google_gmail", displayName: "Gmail", category: "EMAIL", isEnabled: true, isAuthorized: false, lastSyncStatus: "AWAITING_GOOGLE_CLOUD_CONFIG", recordsCount: 0 },
            { id: "google_drive", displayName: "Google Drive", category: "DOCUMENTS", isEnabled: true, isAuthorized: false, lastSyncStatus: "AWAITING_GOOGLE_CLOUD_CONFIG", recordsCount: 0 },
            { id: "google_classroom", displayName: "Google Classroom", category: "EDUCATION", isEnabled: true, isAuthorized: false, lastSyncStatus: "AWAITING_GOOGLE_CLOUD_CONFIG", recordsCount: 0 }
          ]);
          break;
        case "getMemories":
          resolve([
            { id: "mem_1", title: "Semester Exam Strategy", content: "Focus on Distributed Systems algorithms and Database concurrency control. Review chapters 4-7.", category: "ACADEMIC", tags: ["exam", "strategy", "database"], isPinned: true, createdAt: Date.now() - 3600000 },
            { id: "mem_2", title: "Cortex Architecture Insight", content: "Keep SQLite as the local offline single source of truth; run Python as pure functional memory engine via Chaquopy.", category: "PROJECT", tags: ["cortex", "architecture", "sqlite"], isPinned: true, createdAt: Date.now() - 7200000 }
          ]);
          break;
        case "addMemory":
          resolve({ id: "mem_" + Date.now(), title: "Quick Memory", content: args[0], category: args[1], isPinned: args[2], tags: ["quick"], createdAt: Date.now() });
          break;
        case "getNotes":
          resolve([
            { id: "note_1", title: "Distributed Consensus Notes", content: "Paxos vs Raft: Raft decomposes consensus into leader election, log replication, and safety.", collectionId: "col_1", updatedAt: Date.now() - 10000000 }
          ]);
          break;
        case "getCollections":
          resolve([
            { id: "col_1", name: "Computer Science", description: "Academic notes and coursework", colorHex: "#3B82F6", icon: "book-open", documentCount: 2, noteCount: 1 },
            { id: "col_2", name: "Cortex Project", description: "Design docs and specs", colorHex: "#10B981", icon: "code", documentCount: 1, noteCount: 0 }
          ]);
          break;
        case "getAcademicProfile":
          resolve({ institution: "Engineering University", degree: "B.Tech", department: "Computer Science & Engineering", currentSemester: 6, academicYear: "2025-2026", gpa: 8.75 });
          break;
        case "getSubjects":
          resolve([
            { id: "sub_1", code: "CS601", name: "Distributed Systems", faculty: "Dr. Sharma", credits: 4, colorHex: "#3B82F6" },
            { id: "sub_2", code: "CS602", name: "Machine Learning", faculty: "Prof. Iyer", credits: 4, colorHex: "#8B5CF6" },
            { id: "sub_3", code: "CS603", name: "Compiler Design", faculty: "Dr. Rao", credits: 3, colorHex: "#EC4899" }
          ]);
          break;
        case "getTimetable":
          resolve([
            { id: "tt_1", dayOfWeek: 1, subjectId: "sub_1", subjectCode: "CS601", subjectName: "Distributed Systems", startTime: "09:00", endTime: "10:30", room: "Hall B-302", colorHex: "#3B82F6" },
            { id: "tt_2", dayOfWeek: 1, subjectId: "sub_2", subjectCode: "CS602", subjectName: "Machine Learning", startTime: "11:00", endTime: "12:30", room: "Lab 4", colorHex: "#8B5CF6" }
          ]);
          break;
        case "getAssignments":
          resolve([
            { id: "asgn_1", subjectId: "sub_1", subjectCode: "CS601", subjectName: "Distributed Systems", title: "Implement Raft Consensus in Python", description: "Implement leader election and log replication with unit tests.", dueDateMs: Date.now() + 172800000, priority: "HIGH", status: "PENDING" }
          ]);
          break;
        case "getExaminations":
          resolve([
            { id: "exam_1", subjectId: "sub_1", subjectCode: "CS601", subjectName: "Distributed Systems", title: "Mid-Term Examination", examDateMs: Date.now() + 604800000, duration: 180, room: "Exam Hall A", syllabus: "Modules 1, 2 and 3" }
          ]);
          break;
        case "getTasks":
          resolve([
            { id: "tsk_1", title: "Implement Raft leader heartbeat", description: "Write heartbeat RPC handler", priority: "HIGH", category: "ACADEMIC", dueDateMs: Date.now() + 86400000, status: "PENDING" },
            { id: "tsk_2", title: "Review Android 14 storage permissions", description: "Verify scoped storage rules", priority: "MEDIUM", category: "PROJECT", dueDateMs: Date.now() + 172800000, status: "PENDING" }
          ]);
          break;
        case "getGoals":
          resolve([
            { id: "gl_1", title: "Master Distributed Systems", description: "Score > 90% and build 2 distributed projects", goalType: "ACADEMIC", targetDateMs: Date.now() + 7776000000, progressPercent: 65 },
            { id: "gl_2", title: "Launch Cortex Second Brain", description: "Complete Phase 2 Memory & Knowledge Engine", goalType: "PROJECT", targetDateMs: Date.now() + 2592000000, progressPercent: 85 }
          ]);
          break;
        case "getProjects":
          resolve([
            { id: "proj_1", name: "Cortex", description: "Personal offline-first second-brain app for Android", objective: "Lifelong private intelligence engine", status: "ACTIVE", techStack: ["Kotlin", "Python", "Room", "Chaquopy", "HTML/JS"], targetDateMs: Date.now() + 5184000000 }
          ]);
          break;
        case "getTimeline":
          resolve([
            { id: "evt_1", title: "Memory Stored", description: "Semester Exam Strategy captured", eventType: "MEMORY_CREATED", category: "ACADEMIC", timestamp: Date.now() - 3600000 },
            { id: "evt_2", title: "Project Milestone", description: "Completed Room Database Schema v2", eventType: "PROJECT_UPDATE", category: "PROJECT", timestamp: Date.now() - 14400000 }
          ]);
          break;
        case "searchUniversal":
          resolve({
            query: args[0],
            totalMatches: 2,
            results: [
              { entityType: "MEMORY", id: "mem_1", title: "Semester Exam Strategy", snippet: "Focus on Distributed Systems algorithms and Database concurrency control...", category: "ACADEMIC", matchScore: 10.0 },
              { entityType: "TASK", id: "tsk_1", title: "Implement Raft leader heartbeat", snippet: "Write heartbeat RPC handler", category: "ACADEMIC", matchScore: 8.5 }
            ]
          });
          break;
        case "getGoogleDiagnostics":
          resolve({
            package_name: "com.cortex.app",
            debug_keystore_sha1: "27:D8:78:6A:62:87:A3:4D:FF:3E:6F:BF:76:F6:61:F2:42:B8:16:BD",
            oauth_redirect_uri: "com.cortex.app://oauth2callback",
            overall_status: "AWAITING_GOOGLE_CLOUD_CONFIG",
            services: {
              gmail: { status: "AWAITING_GOOGLE_CLOUD_CONFIG", enabled: true, authorized: false, scope: "https://www.googleapis.com/auth/gmail.readonly", reason: "Google Cloud OAuth client ID not configured. Create Android OAuth Client ID in Google Cloud Console." },
              drive: { status: "AWAITING_GOOGLE_CLOUD_CONFIG", enabled: true, authorized: false, scope: "https://www.googleapis.com/auth/drive.file", reason: "Requires user file selection via Google Picker / Android Storage Access Framework." },
              classroom: { status: "AWAITING_GOOGLE_CLOUD_CONFIG", enabled: true, authorized: false, scope: "https://www.googleapis.com/auth/classroom.courses.readonly", reason: "Awaiting Google Cloud Classroom API enablement and test-user registration." },
              calendar: { status: "OPERATIONAL", enabled: true, authorized: true, scope: "android.permission.READ_CALENDAR", reason: "Operating via native Android Calendar Provider (CalendarContract.Events)." }
            }
          });
          break;
        default:
          resolve({ success: true, message: `Mock response for ${method}` });
      }
    }, 50);
  }
}

// Global singleton instance
window.bridge = new CortexBridge();
