/**
 * Cortex Application Logic & Controller
 * Phase 3 — Personal Intelligence, Reasoning & Automation
 */

const App = {
  state: {
    // Phase 1 & 2 State
    installedApps: [],
    selections: {},
    connectors: [],
    permissions: null,
    financialExclusions: [],
    memories: [],
    notes: [],
    documents: [],
    collections: [],
    academicProfile: null,
    subjects: [],
    timetable: [],
    assignments: [],
    examinations: [],
    tasks: [],
    goals: [],
    projects: [],
    timeline: [],

    // Phase 3 Intelligence State
    conversations: [],
    currentConversationId: "",
    chatMessages: [],
    proposedActions: [],
    decisions: [],
    proactiveAlerts: [],
    localAiStatus: null,

    // View State
    activeScreen: "screen-home",
    selectedTimetableDay: new Date().getDay() === 0 ? 1 : new Date().getDay(),
    selectedMemoryCategory: "",
    selectedTaskPriority: "",
    selectedTimelineType: ""
  },

  async init() {
    this.setupEventListeners();
    this.updateGreeting();
    await this.loadAllData();
    await this.checkProactiveAlerts();
  },

  updateGreeting() {
    const greetingEl = document.getElementById("home-greeting");
    if (!greetingEl) return;
    const hour = new Date().getHours();
    let text = "Good Evening, Tharun";
    if (hour < 12) text = "Good Morning, Tharun";
    else if (hour < 17) text = "Good Afternoon, Tharun";
    greetingEl.textContent = text;

    const dayLabel = document.getElementById("today-day-label");
    if (dayLabel) {
      const days = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
      dayLabel.textContent = days[new Date().getDay()];
    }
  },

  setupEventListeners() {
    // Bottom navigation items (Scrollable side-wise)
    document.querySelectorAll(".bottom-nav .nav-item").forEach(btn => {
      btn.addEventListener("click", () => {
        const target = btn.dataset.target;
        if (target) {
          this.navigateTo(target);
          // Scroll active button into view inside bottom nav
          btn.scrollIntoView({ behavior: "smooth", inline: "center", block: "nearest" });
        }
      });
    });

    // Refresh button
    const btnRefresh = document.getElementById("btn-refresh");
    if (btnRefresh) {
      btnRefresh.addEventListener("click", async () => {
        UI.showToast("Synchronizing Cortex...");
        await this.loadAllData();
        await this.checkProactiveAlerts();
        UI.showToast("Cortex is synchronized");
      });
    }

    // Universal Search
    const btnSearch = document.getElementById("btn-open-search");
    if (btnSearch) btnSearch.addEventListener("click", () => this.openSearchModal());
    const btnCloseSearch = document.getElementById("btn-close-search");
    if (btnCloseSearch) btnCloseSearch.addEventListener("click", () => this.closeSearchModal());
    const searchInput = document.getElementById("universal-search-input");
    if (searchInput) {
      let timeout = null;
      searchInput.addEventListener("input", (e) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => this.executeSearch(e.target.value.trim()), 200);
      });
    }

    // Google Diagnostics
    const btnGoogleDiag = document.getElementById("btn-open-google-diag");
    if (btnGoogleDiag) btnGoogleDiag.addEventListener("click", () => this.openGoogleDiagnosticsModal());
    const btnCloseGoogleDiag = document.getElementById("btn-close-google-diag");
    if (btnCloseGoogleDiag) btnCloseGoogleDiag.addEventListener("click", () => this.closeGoogleDiagnosticsModal());
    const btnConfirmGoogleDiag = document.getElementById("btn-confirm-google-diag");
    if (btnConfirmGoogleDiag) btnConfirmGoogleDiag.addEventListener("click", () => this.closeGoogleDiagnosticsModal());

    // Quick Capture
    const btnQuickMemory = document.getElementById("btn-quick-memory");
    if (btnQuickMemory) btnQuickMemory.addEventListener("click", () => this.handleQuickCapture("MEMORY"));
    const btnQuickNote = document.getElementById("btn-quick-note");
    if (btnQuickNote) btnQuickNote.addEventListener("click", () => this.handleQuickCapture("NOTE"));
    const btnQuickTask = document.getElementById("btn-quick-task");
    if (btnQuickTask) btnQuickTask.addEventListener("click", () => this.handleQuickCapture("TASK"));

    // Subtabs switcher
    document.querySelectorAll(".subtabs").forEach(container => {
      container.querySelectorAll(".subtab-btn").forEach(btn => {
        btn.addEventListener("click", () => {
          container.querySelectorAll(".subtab-btn").forEach(b => b.classList.remove("active"));
          btn.classList.add("active");
          const subtab = btn.dataset.subtab;
          const parentScreen = container.closest(".screen");
          if (parentScreen) {
            parentScreen.querySelectorAll("[id^='pane-']").forEach(p => p.style.display = "none");
            const targetPane = parentScreen.querySelector(`[id$='-${subtab}']`);
            if (targetPane) targetPane.style.display = "block";
          }
        });
      });
    });

    // Chat: Send Message
    const btnSendMsg = document.getElementById("btn-send-message");
    const assistantInput = document.getElementById("assistant-input");
    if (btnSendMsg && assistantInput) {
      const sendHandler = () => {
        const text = assistantInput.value.trim();
        if (text) {
          assistantInput.value = "";
          this.sendChatMessage(text);
        }
      };
      btnSendMsg.addEventListener("click", sendHandler);
      assistantInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter") sendHandler();
      });
    }

    // Chat: Quick Prompts
    document.querySelectorAll(".prompt-chip").forEach(chip => {
      chip.addEventListener("click", () => {
        const promptText = chip.dataset.prompt;
        if (promptText) {
          this.navigateTo("screen-assistant");
          this.sendChatMessage(promptText);
        }
      });
    });

    // Chat: New Chat Button
    const btnNewChat = document.getElementById("btn-new-chat");
    if (btnNewChat) {
      btnNewChat.addEventListener("click", () => {
        this.state.currentConversationId = "";
        const stream = document.getElementById("assistant-chat-stream");
        if (stream) {
          stream.innerHTML = `
            <div class="chat-bubble cortex">
              New session started. How can I help you with your classes, projects, plans, or notes?
              <div class="chat-meta">
                <span class="badge badge-primary" style="font-size: 9px;">SESSION_RESET</span>
                <span>Just now</span>
              </div>
            </div>
          `;
        }
        UI.showToast("New chat session started");
      });
    }

    // Briefing buttons on Dashboard
    const btnMorning = document.getElementById("btn-home-morning-brief");
    if (btnMorning) btnMorning.addEventListener("click", () => this.triggerBriefing("MORNING"));
    const btnEvening = document.getElementById("btn-home-evening-brief");
    if (btnEvening) btnEvening.addEventListener("click", () => this.triggerBriefing("EVENING"));
    const btnWeekly = document.getElementById("btn-home-weekly-brief");
    if (btnWeekly) btnWeekly.addEventListener("click", () => this.triggerBriefing("WEEKLY"));

    // Planner actions
    const btnGenPlanner = document.getElementById("btn-generate-planner");
    if (btnGenPlanner) btnGenPlanner.addEventListener("click", () => this.loadDailyPlan());

    const btnTriggerStudy = document.getElementById("btn-trigger-study-plan");
    if (btnTriggerStudy) {
      btnTriggerStudy.addEventListener("click", async () => {
        const days = parseInt(document.getElementById("input-study-days")?.value, 10) || 5;
        const hours = parseFloat(document.getElementById("input-study-hours")?.value) || 3.0;
        await this.generateStudyPlan(days, hours);
      });
    }

    // Decision action
    const btnEvalDec = document.getElementById("btn-evaluate-decision");
    if (btnEvalDec) btnEvalDec.addEventListener("click", () => this.showEvaluateDecisionModal());

    // Memory: Check conflicts
    const btnConflicts = document.getElementById("btn-check-conflicts");
    if (btnConflicts) btnConflicts.addEventListener("click", () => this.runMemoryConflictsCheck());

    // Action button triggers
    const btnAddMemory = document.getElementById("btn-add-memory");
    if (btnAddMemory) btnAddMemory.addEventListener("click", () => this.showAddMemoryModal());

    const btnAddNote = document.getElementById("btn-add-note");
    if (btnAddNote) btnAddNote.addEventListener("click", () => this.showAddNoteModal());
    const btnAddCollection = document.getElementById("btn-add-collection");
    if (btnAddCollection) btnAddCollection.addEventListener("click", () => this.showAddCollectionModal());

    const btnAcademicAction = document.getElementById("btn-academic-action");
    if (btnAcademicAction) btnAcademicAction.addEventListener("click", () => this.showAcademicActionModal());
    const btnEditProfile = document.getElementById("btn-edit-academic-profile");
    if (btnEditProfile) btnEditProfile.addEventListener("click", () => this.showEditProfileModal());

    const btnAddTask = document.getElementById("btn-add-task");
    if (btnAddTask) btnAddTask.addEventListener("click", () => this.showAddTaskModal());
    const btnAddGoal = document.getElementById("btn-add-goal");
    if (btnAddGoal) btnAddGoal.addEventListener("click", () => this.showAddGoalModal());

    const btnAddProject = document.getElementById("btn-add-project");
    if (btnAddProject) btnAddProject.addEventListener("click", () => this.showAddProjectModal());

    // Connectors buttons
    const btnNavAppSelection = document.getElementById("btn-nav-app-selection");
    if (btnNavAppSelection) btnNavAppSelection.addEventListener("click", () => this.navigateTo("screen-selection"));
    const btnNavPerms = document.getElementById("btn-nav-permissions");
    if (btnNavPerms) btnNavPerms.addEventListener("click", () => this.navigateTo("screen-permissions"));
    const btnNavGoogle = document.getElementById("btn-nav-google");
    if (btnNavGoogle) btnNavGoogle.addEventListener("click", () => this.openGoogleDiagnosticsModal());
    const btnNavSync = document.getElementById("btn-nav-sync");
    if (btnNavSync) btnNavSync.addEventListener("click", () => this.navigateTo("screen-sync"));
    const btnNavPrivacy = document.getElementById("btn-nav-privacy");
    if (btnNavPrivacy) btnNavPrivacy.addEventListener("click", () => this.navigateTo("screen-privacy"));

    // Settings Wipe All Data
    const btnWipeAll = document.getElementById("btn-wipe-all");
    if (btnWipeAll) {
      btnWipeAll.addEventListener("click", () => {
        UI.showModal(
          "Nuclear Wipe Confirmation",
          `<p style="color: var(--accent-danger); font-weight: 600;">Are you sure you want to permanently erase ALL data?</p>
           <p>This will erase all memories, notes, conversations, actions, timetable entries, tasks, goals, and sync history.</p>`,
          "WIPE ALL DATA",
          async () => {
            try {
              await bridge.wipeAllData();
              UI.showToast("All data wiped successfully");
              await this.loadAllData();
            } catch (err) {
              UI.showToast("Error wiping data: " + err.message);
            }
          }
        );
      });
    }

    // Dynamic delegates
    this.setupDynamicDelegates();
  },

  setupDynamicDelegates() {
    document.addEventListener("click", async (e) => {
      // Approve Action
      const btnApprove = e.target.closest(".btn-approve-action");
      if (btnApprove) {
        const id = btnApprove.dataset.id;
        try {
          UI.showToast("Executing action...");
          await bridge.approveAction(id);
          UI.showToast("Action approved & executed");
          await this.loadProposedActions();
          await this.loadTasks();
        } catch (err) {
          UI.showToast("Failed to approve action: " + err.message);
        }
        return;
      }

      // Reject Action
      const btnReject = e.target.closest(".btn-reject-action");
      if (btnReject) {
        const id = btnReject.dataset.id;
        try {
          await bridge.rejectAction(id);
          UI.showToast("Action rejected");
          await this.loadProposedActions();
        } catch (err) {
          UI.showToast("Failed to reject action: " + err.message);
        }
        return;
      }

      // Dismiss Proactive Alert
      const btnDismissAlert = e.target.closest(".btn-dismiss-alert");
      if (btnDismissAlert) {
        const id = btnDismissAlert.dataset.id;
        try {
          await bridge.dismissProactiveAlert(id);
          btnDismissAlert.closest(".proactive-banner")?.remove();
        } catch (err) {
          console.error("dismiss alert error:", err);
        }
        return;
      }

      // Delete Decision
      const btnDelDec = e.target.closest(".btn-delete-decision");
      if (btnDelDec) {
        const id = btnDelDec.dataset.id;
        try {
          await bridge.deleteDecision(id);
          UI.showToast("Decision record removed");
          await this.loadDecisions();
        } catch (err) {
          UI.showToast("Failed to delete decision: " + err.message);
        }
        return;
      }

      // Dynamic Reschedule Task Trigger from Daily Plan
      const btnReschedTask = e.target.closest(".btn-reschedule-task-quick");
      if (btnReschedTask) {
        const taskId = btnReschedTask.dataset.id;
        const taskTitle = btnReschedTask.dataset.title;
        UI.showModal(
          "Reschedule Task",
          `<p>Defer <strong>'${taskTitle}'</strong> to another day?</p>
           <p style="font-size: 12px; color: var(--text-muted);">Existing calendar events and classes will remain untouched.</p>
           <div style="margin-top: 10px;">
             <label style="font-size: 11px; font-weight: 600;">Days to Defer:</label>
             <input type="number" id="modal-defer-days" class="search-box" value="1" min="1" max="14" style="margin-bottom:0;" />
           </div>`,
          "Propose Reschedule",
          async (overlay) => {
            const deferDays = parseInt(overlay.querySelector("#modal-defer-days").value, 10) || 1;
            try {
              const res = await bridge.rescheduleTask(taskId, deferDays);
              UI.showToast("Reschedule proposed in Action Approvals");
              App.navigateTo("screen-actions");
              await App.loadProposedActions();
            } catch (err) {
              UI.showToast("Reschedule error: " + err.message);
            }
          }
        );
        return;
      }
    });
  },

  navigateTo(screenId) {
    document.querySelectorAll(".screen").forEach(s => s.classList.remove("active"));
    const target = document.getElementById(screenId);
    if (target) {
      target.classList.add("active");
      this.state.activeScreen = screenId;
    }

    document.querySelectorAll(".bottom-nav .nav-item").forEach(btn => {
      btn.classList.toggle("active", btn.dataset.target === screenId);
    });

    window.scrollTo({ top: 0, behavior: "smooth" });

    // Load screen-specific data on navigation
    if (screenId === "screen-actions") this.loadProposedActions();
    else if (screenId === "screen-planner") this.loadDailyPlan();
    else if (screenId === "screen-decisions") this.loadDecisions();
    else if (screenId === "screen-models") this.loadLocalAiStatus();
  },

  async loadAllData() {
    try {
      await Promise.allSettled([
        this.loadMemories(),
        this.loadNotes(),
        this.loadCollections(),
        this.loadAcademicProfile(),
        this.loadSubjects(),
        this.loadTimetableForDay(this.state.selectedTimetableDay),
        this.loadAssignments(),
        this.loadExaminations(),
        this.loadTasks(),
        this.loadGoals(),
        this.loadProjects(),
        this.loadTimeline(),
        this.loadProposedActions(),
        this.loadDecisions()
      ]);
      this.updateHomeDashboard();
    } catch (err) {
      console.error("loadAllData error:", err);
    }
  },

  // ==================== ASK CORTEX CHAT ====================
  async sendChatMessage(text) {
    const stream = document.getElementById("assistant-chat-stream");
    if (!stream) return;

    // 1. Render User Message
    const userMsgObj = { sender: "USER", text: text, createdAt: Date.now() };
    stream.innerHTML += UI.chatBubble(userMsgObj);
    stream.scrollTop = stream.scrollHeight;

    // 2. Typing Placeholder
    const typingId = "typing_" + Date.now();
    stream.innerHTML += `
      <div class="chat-bubble cortex" id="${typingId}">
        <span style="display: inline-flex; gap: 4px; align-items: center; color: var(--text-muted);">
          <span>Thinking locally...</span>
        </span>
      </div>
    `;
    stream.scrollTop = stream.scrollHeight;

    try {
      const resp = await bridge.askCortex(this.state.currentConversationId, text);
      const typingEl = document.getElementById(typingId);
      if (typingEl) typingEl.remove();

      this.state.currentConversationId = resp.conversationId || this.state.currentConversationId;

      const cortexMsgObj = {
        sender: "CORTEX",
        text: resp.answer,
        intent: resp.intent,
        sourcesJson: resp.citations,
        createdAt: Date.now()
      };
      stream.innerHTML += UI.chatBubble(cortexMsgObj);

      // Render proposed action cards inside chat if any
      const actions = resp.proposed_actions || [];
      for (const act of actions) {
        stream.innerHTML += UI.actionProposalCard(act);
      }

      stream.scrollTop = stream.scrollHeight;
    } catch (err) {
      const typingEl = document.getElementById(typingId);
      if (typingEl) {
        typingEl.innerHTML = `<span style="color: var(--accent-danger);">Error: ${err.message}</span>`;
      }
    }
  },

  // ==================== PROACTIVE MONITORING ====================
  async checkProactiveAlerts() {
    try {
      const alerts = await bridge.getProactiveAlerts() || [];
      this.state.proactiveAlerts = alerts;
      const container = document.getElementById("home-proactive-alerts-container");
      if (!container) return;
      if (alerts.length === 0) {
        container.innerHTML = "";
        return;
      }
      container.innerHTML = alerts.slice(0, 2).map(a => UI.proactiveBanner(a)).join("");
    } catch (err) {
      console.error("proactive alert error:", err);
    }
  },

  // ==================== ACTION APPROVALS ====================
  async loadProposedActions() {
    try {
      const actions = await bridge.getProposedActions() || [];
      this.state.proposedActions = actions;

      const pendingContainer = document.getElementById("pending-actions-list-container");
      const historyContainer = document.getElementById("action-history-list-container");

      const pending = actions.filter(a => a.status === "PROPOSED");
      const history = actions.filter(a => a.status !== "PROPOSED");

      const statActions = document.getElementById("home-stat-actions");
      if (statActions) statActions.textContent = pending.length;

      if (pendingContainer) {
        if (pending.length === 0) {
          pendingContainer.innerHTML = `<div style="text-align: center; padding: 30px; color: var(--text-muted);">No pending actions requiring your approval.</div>`;
        } else {
          pendingContainer.innerHTML = pending.map(a => UI.actionProposalCard(a)).join("");
        }
      }

      if (historyContainer) {
        if (history.length === 0) {
          historyContainer.innerHTML = `<div style="text-align: center; padding: 30px; color: var(--text-muted);">No action history recorded.</div>`;
        } else {
          historyContainer.innerHTML = history.map(a => UI.actionProposalCard(a)).join("");
        }
      }
    } catch (err) {
      console.error("loadProposedActions error:", err);
    }
  },

  // ==================== DAILY & ADAPTIVE PLANNER ====================
  async loadDailyPlan() {
    const container = document.getElementById("daily-plan-container");
    if (!container) return;
    container.innerHTML = `<div style="text-align: center; padding: 30px; color: var(--text-muted);">Calculating optimized daily agenda...</div>`;

    try {
      const plan = await bridge.generateDailyPlan();
      const topFocus = (plan.top_3_focus || []).map(f => `<li style="margin-left: 18px; margin-bottom: 4px;">${f}</li>`).join("");

      const tasksHtml = (plan.prioritized_tasks || []).map(t => `
        <div class="card" style="margin-bottom: 8px;">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <strong style="color: var(--text-main);">${t.title}</strong>
            <span class="priority-${t.priority}">${t.priority}</span>
          </div>
          <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">
            ${t.priority_explanation}
          </div>
          <div style="display: flex; justify-content: flex-end; margin-top: 6px;">
            <button class="btn btn-secondary btn-sm btn-reschedule-task-quick" data-id="${t.id}" data-title="${t.title}" style="font-size: 11px; padding: 2px 6px;">
              Cannot Complete Today
            </button>
          </div>
        </div>
      `).join("");

      container.innerHTML = `
        <div class="card" style="border-left: 4px solid var(--accent-cyan); margin-bottom: 12px;">
          <div class="card-title">Top 3 Recommended Focus Areas</div>
          <ul style="font-size: 13px; line-height: 1.6; color: var(--text-main);">${topFocus}</ul>
        </div>
        <div class="card-title" style="margin-bottom: 8px;">Prioritized Daily Agenda</div>
        ${tasksHtml || '<div style="color: var(--text-muted); padding: 10px;">No pending tasks to schedule.</div>'}
      `;
    } catch (err) {
      container.innerHTML = `<div style="color: var(--accent-danger);">Planner error: ${err.message}</div>`;
    }
  },

  async generateStudyPlan(days, hours) {
    const container = document.getElementById("study-plan-results-container");
    if (!container) return;
    container.innerHTML = `<div style="text-align: center; padding: 20px; color: var(--text-muted);">Generating ${days}-day adaptive study schedule...</div>`;

    try {
      const plan = await bridge.generateStudyPlan(days, hours);
      const schedule = plan.schedule || [];

      const blocksHtml = schedule.map(day => `
        <div class="card" style="margin-bottom: 10px;">
          <div class="card-title" style="font-size: 14px;">${day.day_label} &bull; ${day.total_study_hours} hrs</div>
          <div style="display: flex; flex-direction: column; gap: 6px; margin-top: 8px;">
            ${(day.sessions || []).map(s => `
              <div style="background: rgba(255,255,255,0.04); padding: 8px 10px; border-radius: var(--radius-sm); font-size: 12px;">
                <div style="font-weight: 700; color: #60A5FA;">${s.slot}: ${s.subject} (${s.duration_hours}h)</div>
                <div style="color: var(--text-muted);">${s.activity}</div>
                <div style="color: var(--text-dim); font-size: 11px; margin-top: 2px;"><em>${s.reason}</em></div>
              </div>
            `).join("")}
          </div>
        </div>
      `).join("");

      container.innerHTML = `
        <div style="margin-top: 12px;">
          <div class="card-title" style="margin-bottom: 8px;">Adaptive Study Schedule</div>
          ${blocksHtml}
        </div>
      `;
    } catch (err) {
      container.innerHTML = `<div style="color: var(--accent-danger);">Error: ${err.message}</div>`;
    }
  },

  // ==================== BRIEFINGS ====================
  async triggerBriefing(type) {
    const preview = document.getElementById("home-briefing-preview");
    if (!preview) return;
    preview.innerHTML = `Synthesizing ${type.toLowerCase()} briefing from your records...`;

    try {
      const res = await bridge.generateBriefing(type);
      if (type === "MORNING") {
        const acad = res.academic || {};
        const focus = (res.priorities?.suggested_focus || []).map(f => `&bull; ${f}`).join("<br>");
        preview.innerHTML = `
          <strong>Academic:</strong> ${acad.classes_count || 0} classes today.<br>
          ${focus ? `<strong>Focus Today:</strong><br>${focus}` : ''}
        `;
      } else if (type === "EVENING") {
        preview.innerHTML = `
          <strong>Accomplishments:</strong> Completed ${res.completed_tasks_count || 0} tasks.<br>
          <strong>Tomorrow's Classes:</strong> ${res.tomorrow_outlook?.classes_count || 0} scheduled classes.
        `;
      } else {
        preview.innerHTML = `
          <strong>Weekly Output:</strong> Completed ${res.completed_tasks || 0} tasks &bull; ${res.active_projects_count || 0} active projects.
        `;
      }
    } catch (err) {
      preview.innerHTML = `<span style="color: var(--accent-danger);">Briefing error: ${err.message}</span>`;
    }
  },

  // ==================== DECISIONS ====================
  async loadDecisions() {
    try {
      const list = await bridge.getDecisions() || [];
      this.state.decisions = list;
      const container = document.getElementById("decisions-list-container");
      if (!container) return;
      if (list.length === 0) {
        container.innerHTML = `<div style="text-align: center; padding: 30px; color: var(--text-muted);">No evaluated decisions yet. Click "+ Evaluate" to analyze a decision.</div>`;
        return;
      }
      container.innerHTML = list.map(d => UI.decisionRecordCard(d)).join("");
    } catch (err) {
      console.error("loadDecisions error:", err);
    }
  },

  showEvaluateDecisionModal() {
    UI.showModal(
      "10-Point Decision Analysis",
      `
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Decision Statement</label>
          <input type="text" id="modal-dec-stmt" class="search-box" style="margin-bottom: 0;" placeholder="e.g. Should I adopt Rust or Go for the network layer?" />
        </div>
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Option A</label>
          <input type="text" id="modal-dec-opt1" class="search-box" style="margin-bottom: 0;" placeholder="Option A" />
        </div>
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Option B</label>
          <input type="text" id="modal-dec-opt2" class="search-box" style="margin-bottom: 0;" placeholder="Option B" />
        </div>
      `,
      "Evaluate Decision",
      async (overlay) => {
        const stmt = overlay.querySelector("#modal-dec-stmt").value.trim();
        const opt1 = overlay.querySelector("#modal-dec-opt1").value.trim() || "Option A";
        const opt2 = overlay.querySelector("#modal-dec-opt2").value.trim() || "Option B";
        if (!stmt) {
          UI.showToast("Decision statement required");
          return false;
        }
        try {
          UI.showToast("Evaluating 10-point analysis...");
          await bridge.evaluateDecision(stmt, [opt1, opt2]);
          UI.showToast("Decision evaluated and saved");
          await App.loadDecisions();
        } catch (err) {
          UI.showToast("Error: " + err.message);
        }
      }
    );
  },

  // ==================== LOCAL AI MODEL STATUS ====================
  async loadLocalAiStatus() {
    try {
      const status = await bridge.getLocalAiStatus();
      this.state.localAiStatus = status;

      const badge = document.getElementById("ai-active-provider-badge");
      if (badge) badge.textContent = status.active_provider || "DETERMINISTIC_FALLBACK";

      const explanation = document.getElementById("ai-status-explanation");
      if (explanation && status.explanation) explanation.textContent = status.explanation;

      const container = document.getElementById("models-list-container");
      if (container && status.available_models) {
        container.innerHTML = status.available_models.map(m => UI.localModelCard(m, status.active_provider)).join("");
      }
    } catch (err) {
      console.error("loadLocalAiStatus error:", err);
    }
  },

  // ==================== MEMORY CONFLICTS ====================
  async runMemoryConflictsCheck() {
    UI.showToast("Analyzing memory graph for contradictions...");
    try {
      const conflicts = await bridge.reasonMemories("conflicts", {});
      if (!conflicts || conflicts.length === 0) {
        UI.showToast("No conflicting facts found in your memory records.");
        return;
      }
      UI.showModal(
        "Memory Conflict Resolution",
        conflicts.map(c => `
          <div class="card" style="margin-bottom: 8px;">
            <div style="font-weight:700; color: var(--accent-warning); font-size:13px; margin-bottom:4px;">Contradiction Identified:</div>
            <div style="font-size:12px; color: var(--text-muted);">${c.explanation}</div>
            <div style="margin-top:6px; font-size:12px; background: rgba(255,255,255,0.04); padding:6px; border-radius:4px;">
              <strong>Record A:</strong> ${c.item_a.content}<br>
              <strong>Record B:</strong> ${c.item_b.content}
            </div>
          </div>
        `).join(""),
        "Close"
      );
    } catch (err) {
      UI.showToast("Conflict check error: " + err.message);
    }
  },

  // ==================== EXISTING CORE METHODS (PRESERVED) ====================
  async loadMemories() {
    try {
      this.state.memories = await bridge.getMemories(this.state.selectedMemoryCategory) || [];
      this.renderMemories();
      this.updateHomeDashboard();
    } catch (err) { console.error(err); }
  },

  renderMemories() {
    const container = document.getElementById("memories-list-container");
    if (!container) return;
    let list = this.state.memories;
    if (this.state.selectedMemoryCategory) {
      list = list.filter(m => (m.category || "").toUpperCase() === this.state.selectedMemoryCategory);
    }
    if (list.length === 0) {
      container.innerHTML = `<div style="text-align: center; padding: 30px; color: var(--text-muted);">No memories found.</div>`;
      return;
    }
    container.innerHTML = list.map(m => UI.memoryCard(m)).join("");
  },

  async loadNotes() {
    try {
      this.state.notes = await bridge.getNotes() || [];
      const container = document.getElementById("notes-list-container");
      if (container) {
        container.innerHTML = this.state.notes.length === 0
          ? `<div style="text-align: center; padding: 30px; color: var(--text-muted);">No notes yet. Click "+ Note" to capture your thoughts.</div>`
          : this.state.notes.map(n => UI.noteCard(n)).join("");
      }
    } catch (err) { console.error(err); }
  },

  async loadCollections() {
    try {
      this.state.collections = await bridge.getCollections() || [];
      const container = document.getElementById("collections-list-container");
      if (container) {
        container.innerHTML = this.state.collections.length === 0
          ? `<div style="text-align: center; padding: 30px; color: var(--text-muted);">No collections created.</div>`
          : this.state.collections.map(c => `
              <div class="card" style="border-left: 4px solid ${c.colorHex || 'var(--primary)'}; margin-bottom: 10px;">
                <div class="card-title">
                  <span>${c.name}</span>
                  <span class="badge badge-primary">${c.documentCount || 0} Docs &bull; ${c.noteCount || 0} Notes</span>
                </div>
                ${c.description ? `<div style="font-size: 13px; color: var(--text-muted);">${c.description}</div>` : ''}
              </div>
            `).join("");
      }
    } catch (err) { console.error(err); }
  },

  async loadAcademicProfile() {
    try {
      this.state.academicProfile = await bridge.getAcademicProfile();
      const detailsEl = document.getElementById("academic-profile-details");
      if (detailsEl && this.state.academicProfile) {
        const p = this.state.academicProfile;
        detailsEl.innerHTML = `
          <strong>Institution:</strong> ${p.institution || 'Not set'}<br>
          <strong>Degree & Department:</strong> ${p.degree || ''} ${p.department ? `(${p.department})` : ''}<br>
          <strong>Current Semester:</strong> Semester ${p.currentSemester || 1}<br>
          <strong>Academic Year:</strong> ${p.academicYear || 'Current'}
        `;
      }
    } catch (err) { console.error(err); }
  },

  async loadSubjects() {
    try {
      this.state.subjects = await bridge.getSubjects() || [];
      const container = document.getElementById("subjects-list-container");
      if (container) {
        container.innerHTML = this.state.subjects.length === 0
          ? `<div style="text-align: center; padding: 30px; color: var(--text-muted);">No academic subjects registered.</div>`
          : this.state.subjects.map(s => UI.subjectCard(s)).join("");
      }
    } catch (err) { console.error(err); }
  },

  async loadTimetableForDay(dayOfWeek) {
    try {
      this.state.timetable = await bridge.getTimetable(dayOfWeek) || [];
      const container = document.getElementById("timetable-list-container");
      if (container) {
        container.innerHTML = this.state.timetable.length === 0
          ? `<div style="text-align: center; padding: 30px; color: var(--text-muted);">No classes scheduled for this day.</div>`
          : this.state.timetable.map(tt => UI.timetableRow(tt)).join("");
      }
      this.updateHomeDashboard();
    } catch (err) { console.error(err); }
  },

  async loadAssignments() {
    try {
      this.state.assignments = await bridge.getAssignments() || [];
      const container = document.getElementById("assignments-list-container");
      if (container) {
        container.innerHTML = this.state.assignments.length === 0
          ? `<div style="text-align: center; padding: 30px; color: var(--text-muted);">No coursework or assignments pending.</div>`
          : this.state.assignments.map(a => UI.assignmentCard(a)).join("");
      }
      this.updateHomeDashboard();
    } catch (err) { console.error(err); }
  },

  async loadExaminations() {
    try {
      this.state.examinations = await bridge.getExaminations() || [];
      const container = document.getElementById("exams-list-container");
      if (container) {
        container.innerHTML = this.state.examinations.length === 0
          ? `<div style="text-align: center; padding: 30px; color: var(--text-muted);">No examinations scheduled.</div>`
          : this.state.examinations.map(e => UI.examCard(e)).join("");
      }
      this.updateHomeDashboard();
    } catch (err) { console.error(err); }
  },

  async loadTasks() {
    try {
      this.state.tasks = await bridge.getTasks() || [];
      const container = document.getElementById("tasks-list-container");
      if (container) {
        container.innerHTML = this.state.tasks.length === 0
          ? `<div style="text-align: center; padding: 30px; color: var(--text-muted);">No tasks found.</div>`
          : this.state.tasks.map(t => UI.taskItem(t)).join("");
      }
      this.updateHomeDashboard();
    } catch (err) { console.error(err); }
  },

  async loadGoals() {
    try {
      this.state.goals = await bridge.getGoals() || [];
      const container = document.getElementById("goals-list-container");
      if (container) {
        container.innerHTML = this.state.goals.length === 0
          ? `<div style="text-align: center; padding: 30px; color: var(--text-muted);">No goals registered.</div>`
          : this.state.goals.map(g => UI.goalCard(g)).join("");
      }
    } catch (err) { console.error(err); }
  },

  async loadProjects() {
    try {
      this.state.projects = await bridge.getProjects() || [];
      const container = document.getElementById("projects-list-container");
      if (container) {
        container.innerHTML = this.state.projects.length === 0
          ? `<div style="text-align: center; padding: 30px; color: var(--text-muted);">No software projects tracked.</div>`
          : this.state.projects.map(p => UI.projectCard(p)).join("");
      }
      this.updateHomeDashboard();
    } catch (err) { console.error(err); }
  },

  async loadTimeline() {
    try {
      this.state.timeline = await bridge.getTimeline(50, 0, this.state.selectedTimelineType) || [];
      const container = document.getElementById("timeline-stream-container");
      if (container) {
        container.innerHTML = this.state.timeline.length === 0
          ? `<div style="padding: 20px; color: var(--text-muted);">No timeline events recorded yet.</div>`
          : this.state.timeline.map(e => UI.timelineItem(e)).join("");
      }
    } catch (err) { console.error(err); }
  },

  updateHomeDashboard() {
    const statMem = document.getElementById("home-stat-memories");
    if (statMem) statMem.textContent = this.state.memories.length;

    const activeTasks = this.state.tasks.filter(t => t.status !== "COMPLETED");
    const statTask = document.getElementById("home-stat-tasks");
    if (statTask) statTask.textContent = activeTasks.length;

    const pendingAsgn = this.state.assignments.filter(a => a.status !== "COMPLETED");
    const statAsgn = document.getElementById("home-stat-assignments");
    if (statAsgn) statAsgn.textContent = pendingAsgn.length;

    const ttContainer = document.getElementById("home-timetable-container");
    if (ttContainer) {
      ttContainer.innerHTML = this.state.timetable.length === 0
        ? `<div style="font-size: 13px; color: var(--text-muted); padding: 8px 0;">No classes scheduled for today.</div>`
        : this.state.timetable.slice(0, 3).map(tt => UI.timetableRow(tt)).join("");
    }

    const taskContainer = document.getElementById("home-tasks-container");
    if (taskContainer) {
      taskContainer.innerHTML = activeTasks.length === 0
        ? `<div style="font-size: 13px; color: var(--text-muted); padding: 8px 0;">No pending tasks. You are all caught up!</div>`
        : activeTasks.slice(0, 3).map(t => UI.taskItem(t)).join("");
    }

    const memContainer = document.getElementById("home-memories-container");
    if (memContainer) {
      memContainer.innerHTML = this.state.memories.length === 0
        ? `<div style="font-size: 13px; color: var(--text-muted); padding: 8px 0;">No memories recorded yet. Use Quick Capture above.</div>`
        : this.state.memories.slice(0, 2).map(m => UI.memoryCard(m)).join("");
    }
  },

  async handleQuickCapture(type) {
    const input = document.getElementById("quick-capture-input");
    const text = input ? input.value.trim() : "";
    if (!text) {
      UI.showToast("Please enter text to capture");
      return;
    }
    try {
      if (type === "MEMORY") {
        await bridge.addMemory(text, "PERSONAL", false);
        UI.showToast("Personal memory captured");
        await this.loadMemories();
      } else if (type === "NOTE") {
        const title = text.length > 30 ? text.substring(0, 30) + "..." : text;
        await bridge.saveNote("", title, text, "");
        UI.showToast("Note saved to vault");
        await this.loadNotes();
      } else if (type === "TASK") {
        await bridge.saveTask("", text, "", "MEDIUM", "PERSONAL", Date.now() + 86400000);
        UI.showToast("Task added for today");
        await this.loadTasks();
      }
      if (input) input.value = "";
    } catch (err) {
      UI.showToast("Capture failed: " + err.message);
    }
  },

  // ==================== UNIVERSAL SEARCH OVERLAY ====================
  openSearchModal() {
    const modal = document.getElementById("search-overlay-modal");
    if (modal) modal.classList.add("active");
    const input = document.getElementById("universal-search-input");
    if (input) { input.value = ""; input.focus(); }
    const results = document.getElementById("universal-search-results");
    if (results) results.innerHTML = "";
  },

  closeSearchModal() {
    const modal = document.getElementById("search-overlay-modal");
    if (modal) modal.classList.remove("active");
  },

  async executeSearch(query) {
    const statusText = document.getElementById("search-status-text");
    const resultsContainer = document.getElementById("universal-search-results");
    if (!resultsContainer) return;

    if (!query || query.length < 2) {
      if (statusText) statusText.textContent = "Type at least 2 characters to search across your second brain.";
      resultsContainer.innerHTML = "";
      return;
    }
    if (statusText) statusText.textContent = `Searching for "${query}"...`;
    try {
      const resp = await bridge.searchUniversal(query);
      const matches = resp.results || [];
      if (statusText) statusText.textContent = `Found ${matches.length} matches:`;
      resultsContainer.innerHTML = matches.length === 0
        ? `<div style="text-align: center; padding: 20px; color: var(--text-muted);">No results found matching "${query}".</div>`
        : matches.map(item => UI.searchResultItem(item)).join("");
    } catch (err) {
      if (statusText) statusText.textContent = "Search error: " + err.message;
    }
  },

  // ==================== GOOGLE DIAGNOSTICS ====================
  async openGoogleDiagnosticsModal() {
    const modal = document.getElementById("google-diag-modal");
    if (!modal) return;
    modal.classList.add("active");
    const container = document.getElementById("google-diag-services-container");
    if (container) {
      try {
        const diag = await bridge.getGoogleDiagnostics();
        const s = diag.services || {};
        container.innerHTML = `
          <div class="card" style="margin-bottom: 8px;">
            <div style="display:flex; justify-content:space-between; align-items:center;">
              <strong>Gmail Connector</strong>
              <span class="badge ${s.gmail?.authorized ? 'badge-success' : 'badge-warning'}">${s.gmail?.status || 'AWAITING_GOOGLE_CLOUD_CONFIG'}</span>
            </div>
            <div style="font-size:12px; color:var(--text-muted); margin-top:4px;">${s.gmail?.reason || 'Create Android OAuth Client ID in Google Cloud Console.'}</div>
          </div>
          <div class="card" style="margin-bottom: 8px;">
            <div style="display:flex; justify-content:space-between; align-items:center;">
              <strong>Google Drive Connector</strong>
              <span class="badge ${s.drive?.authorized ? 'badge-success' : 'badge-warning'}">${s.drive?.status || 'AWAITING_GOOGLE_CLOUD_CONFIG'}</span>
            </div>
            <div style="font-size:12px; color:var(--text-muted); margin-top:4px;">${s.drive?.reason || 'Access via Storage Access Framework / Google Picker.'}</div>
          </div>
          <div class="card" style="margin-bottom: 8px;">
            <div style="display:flex; justify-content:space-between; align-items:center;">
              <strong>Google Classroom Connector</strong>
              <span class="badge ${s.classroom?.authorized ? 'badge-success' : 'badge-warning'}">${s.classroom?.status || 'AWAITING_GOOGLE_CLOUD_CONFIG'}</span>
            </div>
            <div style="font-size:12px; color:var(--text-muted); margin-top:4px;">${s.classroom?.reason || 'Awaiting Classroom API enablement and test user registration.'}</div>
          </div>
          <div class="card">
            <div style="display:flex; justify-content:space-between; align-items:center;">
              <strong>Google Calendar (Android Provider)</strong>
              <span class="badge badge-success">OPERATIONAL</span>
            </div>
            <div style="font-size:12px; color:var(--text-muted); margin-top:4px;">Operating via native Android Calendar Provider (CalendarContract.Events).</div>
          </div>
        `;
      } catch (err) {
        container.innerHTML = `<div style="color:var(--accent-danger);">Failed to retrieve diagnostics: ${err.message}</div>`;
      }
    }
  },

  closeGoogleDiagnosticsModal() {
    const modal = document.getElementById("google-diag-modal");
    if (modal) modal.classList.remove("active");
  },

  // Modals for creation
  showAddMemoryModal() {
    UI.showModal(
      "Record Personal Memory",
      `
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Category</label>
          <select id="modal-memory-cat" class="search-box" style="margin-bottom: 0;">
            <option value="PERSONAL">Personal</option>
            <option value="EPISODIC">Episodic (Event/Experience)</option>
            <option value="ACADEMIC">Academic</option>
            <option value="PROJECT">Project Intelligence</option>
            <option value="KNOWLEDGE">General Knowledge</option>
          </select>
        </div>
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Content</label>
          <textarea id="modal-memory-content" class="search-box" style="height: 100px; margin-bottom: 0; resize: vertical;" placeholder="Type the fact, decision, or memory..."></textarea>
        </div>
        <div style="display: flex; align-items: center; gap: 8px;">
          <input type="checkbox" id="modal-memory-pin" style="width: 16px; height: 16px;" />
          <label for="modal-memory-pin" style="font-size: 13px;">Pin to top of memories</label>
        </div>
      `,
      "Record Memory",
      async (overlay) => {
        const cat = overlay.querySelector("#modal-memory-cat").value;
        const content = overlay.querySelector("#modal-memory-content").value.trim();
        const pin = overlay.querySelector("#modal-memory-pin").checked;
        if (!content) { UI.showToast("Content cannot be empty"); return false; }
        try {
          await bridge.addMemory(content, cat, pin);
          UI.showToast("Memory stored in Cortex");
          await App.loadMemories();
        } catch (err) { UI.showToast("Error: " + err.message); }
      }
    );
  },

  showAddNoteModal() {
    UI.showModal(
      "New Knowledge Note",
      `
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Title</label>
          <input type="text" id="modal-note-title" class="search-box" style="margin-bottom: 0;" placeholder="Note Title" />
        </div>
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Content</label>
          <textarea id="modal-note-content" class="search-box" style="height: 120px; margin-bottom: 0; resize: vertical;" placeholder="Write note content..."></textarea>
        </div>
      `,
      "Save Note",
      async (overlay) => {
        const title = overlay.querySelector("#modal-note-title").value.trim() || "Untitled Note";
        const content = overlay.querySelector("#modal-note-content").value.trim();
        if (!content) { UI.showToast("Note content cannot be empty"); return false; }
        try {
          await bridge.saveNote("", title, content, "");
          UI.showToast("Note saved to vault");
          await App.loadNotes();
        } catch (err) { UI.showToast("Error saving note: " + err.message); }
      }
    );
  },

  showAddCollectionModal() {
    UI.showModal(
      "Create Collection",
      `
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Name</label>
          <input type="text" id="modal-col-name" class="search-box" style="margin-bottom: 0;" placeholder="e.g. Distributed Systems" />
        </div>
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Description</label>
          <input type="text" id="modal-col-desc" class="search-box" style="margin-bottom: 0;" placeholder="Brief description..." />
        </div>
      `,
      "Create",
      async (overlay) => {
        const name = overlay.querySelector("#modal-col-name").value.trim();
        const desc = overlay.querySelector("#modal-col-desc").value.trim();
        if (!name) { UI.showToast("Name is required"); return false; }
        try {
          await bridge.addCollection(name, desc);
          UI.showToast("Collection created");
          await App.loadCollections();
        } catch (err) { UI.showToast("Error: " + err.message); }
      }
    );
  },

  showAddTaskModal() {
    UI.showModal(
      "Add New Task",
      `
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Task Title</label>
          <input type="text" id="modal-task-title" class="search-box" style="margin-bottom: 0;" placeholder="What needs to be done?" />
        </div>
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Priority</label>
          <select id="modal-task-priority" class="search-box" style="margin-bottom: 0;">
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="URGENT">Urgent</option>
            <option value="LOW">Low</option>
          </select>
        </div>
      `,
      "Add Task",
      async (overlay) => {
        const title = overlay.querySelector("#modal-task-title").value.trim();
        const priority = overlay.querySelector("#modal-task-priority").value;
        if (!title) { UI.showToast("Task title is required"); return false; }
        try {
          await bridge.saveTask("", title, "", priority, "PERSONAL", Date.now() + 86400000);
          UI.showToast("Task added");
          await App.loadTasks();
        } catch (err) { UI.showToast("Error adding task: " + err.message); }
      }
    );
  },

  showAddGoalModal() {
    UI.showModal(
      "Track Goal",
      `
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Goal Title</label>
          <input type="text" id="modal-goal-title" class="search-box" style="margin-bottom: 0;" placeholder="e.g. Master Machine Learning" />
        </div>
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Category</label>
          <select id="modal-goal-type" class="search-box" style="margin-bottom: 0;">
            <option value="ACADEMIC">Academic</option>
            <option value="PROJECT">Project</option>
            <option value="PERSONAL">Personal</option>
          </select>
        </div>
      `,
      "Save Goal",
      async (overlay) => {
        const title = overlay.querySelector("#modal-goal-title").value.trim();
        const type = overlay.querySelector("#modal-goal-type").value;
        if (!title) { UI.showToast("Title is required"); return false; }
        try {
          await bridge.saveGoal("", title, "", type, Date.now() + 5184000000, 0);
          UI.showToast("Goal tracked in Cortex");
          await App.loadGoals();
        } catch (err) { UI.showToast("Error: " + err.message); }
      }
    );
  },

  showAddProjectModal() {
    UI.showModal(
      "Register Software Project",
      `
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Project Name</label>
          <input type="text" id="modal-proj-name" class="search-box" style="margin-bottom: 0;" placeholder="e.g. SeleneOS" />
        </div>
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Objective</label>
          <input type="text" id="modal-proj-desc" class="search-box" style="margin-bottom: 0;" placeholder="What is this project?" />
        </div>
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Tech Stack</label>
          <input type="text" id="modal-proj-stack" class="search-box" style="margin-bottom: 0;" placeholder="Rust, C, x86_64" />
        </div>
      `,
      "Add Project",
      async (overlay) => {
        const name = overlay.querySelector("#modal-proj-name").value.trim();
        const desc = overlay.querySelector("#modal-proj-desc").value.trim();
        const stackStr = overlay.querySelector("#modal-proj-stack").value.trim();
        const stackArr = stackStr ? stackStr.split(",").map(s => s.trim()).filter(Boolean) : [];
        if (!name) { UI.showToast("Project name required"); return false; }
        try {
          await bridge.saveProject("", name, desc, desc, "ACTIVE", JSON.stringify(stackArr), Date.now() + 5184000000);
          UI.showToast("Project registered");
          await App.loadProjects();
        } catch (err) { UI.showToast("Error: " + err.message); }
      }
    );
  },

  showAcademicActionModal() {
    UI.showModal(
      "Academic Actions",
      `
        <div style="display: flex; flex-direction: column; gap: 8px;">
          <button class="btn btn-secondary btn-block" id="modal-btn-add-subject">Add Subject / Course</button>
          <button class="btn btn-secondary btn-block" id="modal-btn-add-timetable">Add Timetable Class</button>
          <button class="btn btn-secondary btn-block" id="modal-btn-add-assignment">Add Assignment</button>
          <button class="btn btn-secondary btn-block" id="modal-btn-add-exam">Add Examination</button>
        </div>
      `,
      "Close"
    );
    setTimeout(() => {
      document.getElementById("modal-btn-add-subject")?.addEventListener("click", () => App.showAddSubjectModal());
      document.getElementById("modal-btn-add-timetable")?.addEventListener("click", () => App.showAddTimetableModal());
      document.getElementById("modal-btn-add-assignment")?.addEventListener("click", () => App.showAddAssignmentModal());
      document.getElementById("modal-btn-add-exam")?.addEventListener("click", () => App.showAddExamModal());
    }, 50);
  },

  showAddSubjectModal() {
    UI.showModal(
      "Register Subject / Course",
      `
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Course Code</label>
          <input type="text" id="modal-sub-code" class="search-box" style="margin-bottom: 0;" placeholder="e.g. CS601" />
        </div>
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Course Name</label>
          <input type="text" id="modal-sub-name" class="search-box" style="margin-bottom: 0;" placeholder="e.g. Distributed Systems" />
        </div>
      `,
      "Save Subject",
      async (overlay) => {
        const code = overlay.querySelector("#modal-sub-code").value.trim();
        const name = overlay.querySelector("#modal-sub-name").value.trim();
        if (!code || !name) { UI.showToast("Course code and name required"); return false; }
        try {
          await bridge.saveSubject("", code, name, "", 4, "#3B82F6");
          UI.showToast("Subject saved");
          await App.loadSubjects();
        } catch (err) { UI.showToast("Error: " + err.message); }
      }
    );
  },

  showAddTimetableModal() {
    const subjects = this.state.subjects;
    if (subjects.length === 0) {
      UI.showToast("Please register at least one Subject first!");
      this.showAddSubjectModal();
      return;
    }
    const optionsHtml = subjects.map(s => `<option value="${s.id}">${s.code}: ${s.name}</option>`).join("");
    UI.showModal(
      "Add Timetable Class",
      `
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Day of Week</label>
          <select id="modal-tt-day" class="search-box" style="margin-bottom: 0;">
            <option value="1">Monday</option>
            <option value="2">Tuesday</option>
            <option value="3">Wednesday</option>
            <option value="4">Thursday</option>
            <option value="5">Friday</option>
            <option value="6">Saturday</option>
          </select>
        </div>
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Subject</label>
          <select id="modal-tt-subject" class="search-box" style="margin-bottom: 0;">${optionsHtml}</select>
        </div>
        <div style="display: flex; gap: 8px; margin-bottom: 12px;">
          <div style="flex: 1;">
            <label style="font-size: 12px; font-weight: 600;">Start Time</label>
            <input type="text" id="modal-tt-start" class="search-box" style="margin-bottom: 0;" value="09:00" />
          </div>
          <div style="flex: 1;">
            <label style="font-size: 12px; font-weight: 600;">End Time</label>
            <input type="text" id="modal-tt-end" class="search-box" style="margin-bottom: 0;" value="10:30" />
          </div>
        </div>
        <div style="margin-bottom: 12px;">
          <label style="font-size: 12px; font-weight: 600;">Room / Hall</label>
          <input type="text" id="modal-tt-room" class="search-box" style="margin-bottom: 0;" placeholder="Hall 302" />
        </div>
      `,
      "Add Class",
      async (overlay) => {
        const day = overlay.querySelector("#modal-tt-day").value;
        const subId = overlay.querySelector("#modal-tt-subject").value;
        const start = overlay.querySelector("#modal-tt-start").value.trim();
        const end = overlay.querySelector("#modal-tt-end").value.trim();
        const room = overlay.querySelector("#modal-tt-room").value.trim();
        try {
          await bridge.saveTimetableEntry("", day, subId, start, end, room);
          UI.showToast("Class added to timetable");
          await App.loadTimetableForDay(App.state.selectedTimetableDay);
        } catch (err) { UI.showToast("Error adding class: " + err.message); }
      }
    );
  },

  showAddAssignmentModal() {
    const subjects = this.state.subjects;
    const optionsHtml = subjects.map(s => `<option value="${s.id}">${s.code}: ${s.name}</option>`).join("");
    UI.showModal(
      "Add Academic Assignment",
      `
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Assignment Title</label>
          <input type="text" id="modal-asgn-title" class="search-box" style="margin-bottom: 0;" placeholder="e.g. Implement Raft Consensus" />
        </div>
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Subject</label>
          <select id="modal-asgn-sub" class="search-box" style="margin-bottom: 0;">${optionsHtml}</select>
        </div>
      `,
      "Save Assignment",
      async (overlay) => {
        const title = overlay.querySelector("#modal-asgn-title").value.trim();
        const subId = overlay.querySelector("#modal-asgn-sub").value;
        if (!title) { UI.showToast("Title is required"); return false; }
        try {
          await bridge.saveAssignment("", subId, title, "", Date.now() + 172800000, "HIGH");
          UI.showToast("Assignment recorded");
          await App.loadAssignments();
        } catch (err) { UI.showToast("Error: " + err.message); }
      }
    );
  },

  showAddExamModal() {
    const subjects = this.state.subjects;
    const optionsHtml = subjects.map(s => `<option value="${s.id}">${s.code}: ${s.name}</option>`).join("");
    UI.showModal(
      "Schedule Examination",
      `
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Exam Title</label>
          <input type="text" id="modal-ex-title" class="search-box" style="margin-bottom: 0;" placeholder="e.g. Mid-Term Examination" />
        </div>
        <div style="margin-bottom: 12px;">
          <label style="display: block; font-size: 12px; font-weight: 600; margin-bottom: 4px;">Subject</label>
          <select id="modal-ex-sub" class="search-box" style="margin-bottom: 0;">${optionsHtml}</select>
        </div>
      `,
      "Schedule Exam",
      async (overlay) => {
        const title = overlay.querySelector("#modal-ex-title").value.trim();
        const subId = overlay.querySelector("#modal-ex-sub").value;
        if (!title) { UI.showToast("Title is required"); return false; }
        try {
          await bridge.saveExamination("", subId, title, Date.now() + 604800000, 180, "Exam Hall A", "Modules 1-3");
          UI.showToast("Exam scheduled");
          await App.loadExaminations();
        } catch (err) { UI.showToast("Error: " + err.message); }
      }
    );
  },

  showEditProfileModal() {
    const p = this.state.academicProfile || {};
    UI.showModal(
      "Edit Academic Profile",
      `
        <div style="margin-bottom: 10px;">
          <label style="font-size: 12px; font-weight: 600;">Institution</label>
          <input type="text" id="modal-prof-inst" class="search-box" value="${p.institution || ''}" placeholder="University name" />
        </div>
        <div style="margin-bottom: 10px;">
          <label style="font-size: 12px; font-weight: 600;">Degree</label>
          <input type="text" id="modal-prof-deg" class="search-box" value="${p.degree || ''}" placeholder="e.g. B.Tech" />
        </div>
        <div style="margin-bottom: 10px;">
          <label style="font-size: 12px; font-weight: 600;">Department</label>
          <input type="text" id="modal-prof-dept" class="search-box" value="${p.department || ''}" placeholder="Computer Science" />
        </div>
      `,
      "Save Profile",
      async (overlay) => {
        const inst = overlay.querySelector("#modal-prof-inst").value.trim();
        const deg = overlay.querySelector("#modal-prof-deg").value.trim();
        const dept = overlay.querySelector("#modal-prof-dept").value.trim();
        try {
          await bridge.saveAcademicProfile(inst, deg, dept, 6, "2025-2026", 8.8);
          UI.showToast("Profile updated");
          await App.loadAcademicProfile();
        } catch (err) { UI.showToast("Error updating profile: " + err.message); }
      }
    );
  }
};

document.addEventListener("DOMContentLoaded", () => {
  App.init();
});
