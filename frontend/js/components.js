/**
 * Cortex Reusable UI Components
 */

const UI = {
  showToast(message, durationMs = 2600) {
    let toast = document.getElementById("toast");
    if (!toast) {
      toast = document.createElement("div");
      toast.id = "toast";
      toast.className = "toast";
      document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.classList.add("show");
    setTimeout(() => {
      toast.classList.remove("show");
    }, durationMs);
  },

  capabilityBadge(cap) {
    switch (cap) {
      case "OFFICIAL_API":
        return `<span class="badge badge-primary">Official API</span>`;
      case "SYSTEM_PROVIDER":
        return `<span class="badge badge-success">System API</span>`;
      case "NOTIFICATION":
        return `<span class="badge" style="background: rgba(6,182,212,0.15); color: #22D3EE; border: 1px solid rgba(6,182,212,0.3)">Notifications</span>`;
      case "SHARE_IMPORT":
        return `<span class="badge badge-warning">Share/Import</span>`;
      case "EXCLUDED":
        return `<span class="badge badge-danger">Financial Excluded</span>`;
      default:
        return `<span class="badge badge-gray">Unsupported</span>`;
    }
  },

  appItemRow(app) {
    const isFinancial = (app.category === "FINANCIAL");
    const iconSrc = app.iconBase64 || "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='40' height='40' viewBox='0 0 24 24' fill='none' stroke='%233B82F6' stroke-width='2'><rect x='3' y='3' width='18' height='18' rx='4'/><path d='M9 9h6v6H9z'/></svg>";
    const capsHtml = (app.supportedCapabilities || []).map(c => UI.capabilityBadge(c)).join(" ");

    return `
      <div class="app-item ${isFinancial ? 'financial-excluded-item' : ''}" data-pkg="${app.packageName}">
        <div class="app-info-row">
          <img class="app-icon" src="${iconSrc}" alt="${app.appName}" onerror="this.src='data:image/svg+xml;utf8,<svg xmlns=\'http://www.w3.org/2000/svg\' width=\'40\' height=\'40\' viewBox=\'0 0 24 24\' fill=\'none\' stroke=\'%2364748B\' stroke-width=\'2\'><rect x=\'3\' y=\'3\' width=\'18\' height=\'18\' rx=\'4\'/></svg>'" />
          <div class="app-text">
            <div class="app-name">${app.appName} ${app.isSystemApp ? '<span class="badge badge-gray" style="font-size:9px">SYSTEM</span>' : ''}</div>
            <div class="app-pkg">${app.packageName}</div>
            <div style="margin-top: 4px; display: flex; gap: 4px; flex-wrap: wrap;">${capsHtml}</div>
          </div>
        </div>
        <div class="app-action">
          ${isFinancial ? `
            <span class="badge badge-danger" title="Permanently excluded for your financial protection">🔒 Excluded</span>
          ` : `
            <label class="switch">
              <input type="checkbox" class="app-toggle" data-pkg="${app.packageName}" ${app.isEnabled ? 'checked' : ''}>
              <span class="slider"></span>
            </label>
          `}
        </div>
      </div>
    `;
  },

  showModal(title, bodyHtml, confirmText = "Save", onConfirm = null) {
    let overlay = document.getElementById("modal-overlay");
    if (!overlay) {
      overlay = document.createElement("div");
      overlay.id = "modal-overlay";
      overlay.className = "modal-overlay";
      document.body.appendChild(overlay);
    }
    overlay.innerHTML = `
      <div class="modal-card">
        <div class="card-title">
          <span>${title}</span>
          <button class="btn btn-secondary btn-sm" id="modal-close-btn">&times;</button>
        </div>
        <div style="margin: 14px 0; font-size: 14px; color: var(--text-muted); line-height: 1.6;">
          ${bodyHtml}
        </div>
        <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px;">
          <button class="btn btn-secondary btn-sm" id="modal-cancel-btn">Cancel</button>
          <button class="btn btn-primary btn-sm" id="modal-confirm-btn">${confirmText}</button>
        </div>
      </div>
    `;
    overlay.classList.add("active");

    const close = () => overlay.classList.remove("active");
    overlay.querySelector("#modal-close-btn").onclick = close;
    overlay.querySelector("#modal-cancel-btn").onclick = close;
    overlay.querySelector("#modal-confirm-btn").onclick = () => {
      if (onConfirm) {
        const proceed = onConfirm(overlay);
        if (proceed !== false) close();
      } else {
        close();
      }
    };
  },

  memoryCard(m) {
    const isPinned = !!m.isPinned;
    const tags = Array.isArray(m.tags) ? m.tags : (m.tags ? JSON.parse(m.tags) : []);
    const tagsHtml = tags.map(t => `<span class="tag-pill">#${t}</span>`).join(" ");
    const dateStr = new Date(m.createdAt || Date.now()).toLocaleDateString([], { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });

    return `
      <div class="memory-card ${isPinned ? 'pinned' : ''}" data-id="${m.id}">
        <div class="memory-header">
          <div style="display: flex; align-items: center; gap: 6px;">
            <span class="badge badge-primary" style="font-size: 10px;">${m.category || 'PERSONAL'}</span>
            ${isPinned ? '<span style="color: var(--accent-cyan); font-size: 12px;">📌 Pinned</span>' : ''}
          </div>
          <div style="display: flex; gap: 4px;">
            <button class="btn btn-secondary btn-sm btn-pin-memory" data-id="${m.id}" data-pinned="${isPinned}" style="padding: 2px 6px; font-size: 11px;">
              ${isPinned ? 'Unpin' : 'Pin'}
            </button>
            <button class="btn btn-secondary btn-sm btn-delete-memory" data-id="${m.id}" style="padding: 2px 6px; font-size: 11px; color: var(--accent-danger);">
              &times;
            </button>
          </div>
        </div>
        <div class="memory-content">${m.content}</div>
        <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 8px;">
          <div class="tag-list">${tagsHtml}</div>
          <span style="font-size: 11px; color: var(--text-dim);">${dateStr}</span>
        </div>
      </div>
    `;
  },

  noteCard(n) {
    const dateStr = new Date(n.updatedAt || Date.now()).toLocaleDateString([], { month: "short", day: "numeric" });
    return `
      <div class="card" data-id="${n.id}" style="margin-bottom: 10px;">
        <div class="card-title" style="margin-bottom: 6px;">
          <span>${n.title || 'Untitled Note'}</span>
          <button class="btn btn-secondary btn-sm btn-delete-note" data-id="${n.id}" style="padding: 2px 6px; font-size: 11px; color: var(--accent-danger);">&times;</button>
        </div>
        <div style="font-size: 13px; color: var(--text-muted); line-height: 1.5; max-height: 80px; overflow: hidden; text-overflow: ellipsis;">
          ${n.content}
        </div>
        <div style="margin-top: 8px; font-size: 11px; color: var(--text-dim);">Updated ${dateStr}</div>
      </div>
    `;
  },

  taskItem(t) {
    const isDone = t.status === "COMPLETED";
    const priority = t.priority || "MEDIUM";
    const dueStr = t.dueDateMs ? new Date(t.dueDateMs).toLocaleDateString([], { month: "short", day: "numeric" }) : "";

    return `
      <div class="task-item ${isDone ? 'completed' : ''}" data-id="${t.id}">
        <input type="checkbox" class="task-checkbox btn-toggle-task" data-id="${t.id}" ${isDone ? 'checked' : ''} />
        <div class="task-body">
          <div class="task-title">${t.title}</div>
          ${t.description ? `<div class="task-desc">${t.description}</div>` : ''}
          <div class="task-footer">
            <span class="priority-${priority}">${priority}</span>
            <span class="badge badge-gray" style="font-size: 10px;">${t.category || 'PERSONAL'}</span>
            ${dueStr ? `<span>Due ${dueStr}</span>` : ''}
          </div>
        </div>
        <button class="btn btn-secondary btn-sm btn-delete-task" data-id="${t.id}" style="padding: 2px 6px; font-size: 11px; color: var(--accent-danger);">&times;</button>
      </div>
    `;
  },

  goalCard(g) {
    const percent = Math.min(100, Math.max(0, g.progressPercent || 0));
    return `
      <div class="goal-card" data-id="${g.id}">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
          <div style="font-weight: 700; font-size: 15px;">${g.title}</div>
          <span class="badge badge-primary">${g.goalType || 'PERSONAL'}</span>
        </div>
        ${g.description ? `<div style="font-size: 13px; color: var(--text-muted); margin-bottom: 8px;">${g.description}</div>` : ''}
        <div class="progress-track">
          <div class="progress-fill" style="width: ${percent}%;"></div>
        </div>
        <div style="display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: var(--text-dim);">
          <span>${percent}% Complete</span>
          <button class="btn btn-secondary btn-sm btn-update-goal-prog" data-id="${g.id}" data-percent="${percent}" style="padding: 2px 8px; font-size: 11px;">Update Progress</button>
        </div>
      </div>
    `;
  },

  timetableRow(tt) {
    return `
      <div class="timetable-entry" data-id="${tt.id}" style="border-left-color: ${tt.colorHex || 'var(--primary)'};">
        <div class="timetable-time">${tt.startTime} - ${tt.endTime}</div>
        <div style="flex: 1;">
          <div style="font-weight: 700; font-size: 14px; color: var(--text-main);">${tt.subjectCode ? tt.subjectCode + ': ' : ''}${tt.subjectName || 'Course'}</div>
          ${tt.room ? `<div style="font-size: 12px; color: var(--text-muted);">Room: ${tt.room}</div>` : ''}
        </div>
        <button class="btn btn-secondary btn-sm btn-delete-timetable" data-id="${tt.id}" style="padding: 2px 6px; font-size: 11px; color: var(--accent-danger);">&times;</button>
      </div>
    `;
  },

  subjectCard(s) {
    return `
      <div class="card" data-id="${s.id}" style="border-left: 4px solid ${s.colorHex || '#3B82F6'}; margin-bottom: 10px;">
        <div class="card-title">
          <span>${s.code}: ${s.name}</span>
          <button class="btn btn-secondary btn-sm btn-delete-subject" data-id="${s.id}" style="padding: 2px 6px; font-size: 11px; color: var(--accent-danger);">&times;</button>
        </div>
        <div style="font-size: 12px; color: var(--text-muted); line-height: 1.6;">
          ${s.faculty ? `<strong>Faculty:</strong> ${s.faculty}<br>` : ''}
          <strong>Credits:</strong> ${s.credits || 3}
        </div>
      </div>
    `;
  },

  assignmentCard(a) {
    const isDone = a.status === "COMPLETED";
    const dueStr = a.dueDateMs ? new Date(a.dueDateMs).toLocaleDateString([], { month: "short", day: "numeric" }) : "No due date";
    return `
      <div class="card" data-id="${a.id}" style="margin-bottom: 10px; opacity: ${isDone ? '0.6' : '1'};">
        <div class="card-title">
          <span style="${isDone ? 'text-decoration: line-through;' : ''}">${a.title}</span>
          <span class="priority-${a.priority || 'MEDIUM'}">${a.priority || 'MEDIUM'}</span>
        </div>
        ${a.description ? `<div style="font-size: 13px; color: var(--text-muted); margin-bottom: 8px;">${a.description}</div>` : ''}
        <div style="display: flex; justify-content: space-between; align-items: center; font-size: 11px; color: var(--text-dim); margin-top: 6px;">
          <span>${a.subjectCode || 'Course'} &bull; Due: ${dueStr}</span>
          <button class="btn btn-secondary btn-sm btn-toggle-assignment" data-id="${a.id}" data-status="${isDone ? 'PENDING' : 'COMPLETED'}" style="padding: 2px 8px; font-size: 11px;">
            ${isDone ? 'Mark Pending' : 'Mark Done'}
          </button>
        </div>
      </div>
    `;
  },

  examCard(e) {
    const examDate = e.examDateMs ? new Date(e.examDateMs).toLocaleDateString([], { weekday: "short", month: "short", day: "numeric" }) : "";
    return `
      <div class="card" style="border-left: 4px solid var(--accent-warning); margin-bottom: 10px;">
        <div class="card-title">
          <span>${e.title}</span>
          <span class="badge badge-warning">${e.duration || 180} min</span>
        </div>
        <div style="font-size: 13px; color: var(--text-muted); line-height: 1.6;">
          <strong>Date:</strong> ${examDate}<br>
          ${e.room ? `<strong>Room:</strong> ${e.room}<br>` : ''}
          ${e.syllabus ? `<strong>Syllabus:</strong> ${e.syllabus}` : ''}
        </div>
      </div>
    `;
  },

  projectCard(p) {
    const techStack = Array.isArray(p.techStack) ? p.techStack : (p.techStack ? JSON.parse(p.techStack) : []);
    const stackHtml = techStack.map(s => `<span class="badge badge-gray" style="font-size: 10px;">${s}</span>`).join(" ");

    return `
      <div class="card" data-id="${p.id}" style="margin-bottom: 14px;">
        <div class="card-title">
          <span>${p.name}</span>
          <span class="badge badge-success">${p.status || 'ACTIVE'}</span>
        </div>
        ${p.description ? `<div style="font-size: 13px; color: var(--text-muted); margin-bottom: 8px;">${p.description}</div>` : ''}
        ${p.objective ? `<div style="font-size: 12px; color: var(--text-dim); margin-bottom: 8px;"><strong>Goal:</strong> ${p.objective}</div>` : ''}
        <div style="display: flex; gap: 4px; flex-wrap: wrap; margin-bottom: 10px;">${stackHtml}</div>
        <div style="display: flex; gap: 6px; justify-content: flex-end;">
          <button class="btn btn-secondary btn-sm btn-delete-project" data-id="${p.id}" style="color: var(--accent-danger);">&times; Delete</button>
        </div>
      </div>
    `;
  },

  timelineItem(evt) {
    const timeStr = new Date(evt.timestamp || Date.now()).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    const dateStr = new Date(evt.timestamp || Date.now()).toLocaleDateString([], { month: "short", day: "numeric" });

    return `
      <div class="timeline-node">
        <div class="timeline-dot"></div>
        <div class="timeline-card">
          <div class="timeline-meta">
            <span class="badge badge-primary" style="font-size: 10px;">${evt.category || 'SYSTEM'}</span>
            <span>${dateStr} &bull; ${timeStr}</span>
          </div>
          <div style="font-weight: 700; font-size: 14px; margin-bottom: 4px;">${evt.title}</div>
          <div style="font-size: 12px; color: var(--text-muted);">${evt.description || ''}</div>
        </div>
      </div>
    `;
  },

  searchResultItem(item) {
    return `
      <div class="search-result-item" data-entity="${item.entityType}" data-id="${item.id}">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
          <span class="badge badge-primary" style="font-size: 10px;">${item.entityType}</span>
          <span class="badge badge-gray" style="font-size: 10px;">${item.category || ''}</span>
        </div>
        <div style="font-weight: 700; font-size: 14px; color: var(--text-main); margin-bottom: 4px;">${item.title}</div>
        <div style="font-size: 12px; color: var(--text-muted); line-height: 1.4;">${item.snippet || ''}</div>
      </div>
    `;
  },

  // ==================== PHASE 3 UI COMPONENTS ====================

  formatMarkdown(text) {
    if (!text) return "";
    let s = text
      .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
      .replace(/### (.*?)\n/g, '<h3 style="font-size:15px; font-weight:800; color:#60A5FA; margin:8px 0 4px;">$1</h3>')
      .replace(/## (.*?)\n/g, '<h2 style="font-size:16px; font-weight:800; color:#F8FAFC; margin:10px 0 4px;">$1</h2>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/_(.*?)_/g, '<em>$1</em>')
      .replace(/`([^`]+)`/g, '<code style="background:rgba(255,255,255,0.08); padding:1px 4px; border-radius:3px; font-family:monospace; font-size:12px;">$1</code>')
      .replace(/\n&bull; (.*?)(?=\n|$)/g, '<li style="margin-left:16px;">$1</li>')
      .replace(/\n\n/g, '<div style="height:8px;"></div>')
      .replace(/\n/g, '<br>');
    return s;
  },

  chatBubble(msg) {
    const isUser = msg.sender === "USER";
    const timeStr = new Date(msg.createdAt || Date.now()).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    const formatted = UI.formatMarkdown(msg.text);

    let citations = [];
    if (msg.sourcesJson) {
      try {
        citations = typeof msg.sourcesJson === "string" ? JSON.parse(msg.sourcesJson) : msg.sourcesJson;
      } catch (e) { citations = []; }
    }

    const citationHtml = citations.map(c => `<span class="citation-pill" title="Verified Source Citation">📌 ${c}</span>`).join(" ");

    return `
      <div class="chat-bubble ${isUser ? 'user' : 'cortex'}">
        <div>${formatted}</div>
        ${citationHtml ? `<div class="citation-pill-container">${citationHtml}</div>` : ''}
        <div class="chat-meta">
          ${!isUser && msg.intent ? `<span class="badge badge-primary" style="font-size:9px;">${msg.intent}</span>` : '<span></span>'}
          <span>${timeStr}</span>
        </div>
      </div>
    `;
  },

  actionProposalCard(act) {
    const isProposed = act.status === "PROPOSED";
    return `
      <div class="action-proposal-card" data-id="${act.id}">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
          <span style="font-weight: 700; font-size: 14px; color: var(--text-main);">${act.title}</span>
          <span class="action-status-badge action-status-${act.status}">${act.status}</span>
        </div>
        <div style="font-size: 13px; color: var(--text-muted); margin-bottom: 8px;">${act.description}</div>
        <div style="font-size: 11px; color: var(--text-dim); margin-bottom: 10px;">Context: ${act.sourceContext || 'Assistant'}</div>
        ${isProposed ? `
          <div style="display: flex; gap: 8px; justify-content: flex-end;">
            <button class="btn btn-secondary btn-sm btn-reject-action" data-id="${act.id}">Reject</button>
            <button class="btn btn-primary btn-sm btn-approve-action" data-id="${act.id}">Approve Action</button>
          </div>
        ` : `
          <div style="font-size: 11px; color: var(--text-dim); text-align: right;">Executed: ${new Date(act.executedAt || act.createdAt).toLocaleDateString()}</div>
        `}
      </div>
    `;
  },

  proactiveBanner(alert) {
    const isUrgent = alert.priority === "URGENT";
    return `
      <div class="proactive-banner ${isUrgent ? 'urgent' : ''}" data-id="${alert.id}">
        <div style="flex: 1;">
          <div style="display: flex; align-items: center; gap: 6px; margin-bottom: 3px;">
            <span class="badge ${isUrgent ? 'badge-danger' : 'badge-warning'}" style="font-size: 9px;">${alert.priority}</span>
            <strong style="font-size: 13px; color: var(--text-main);">${alert.title}</strong>
          </div>
          <div style="font-size: 12px; color: var(--text-muted);">${alert.description}</div>
        </div>
        <button class="btn btn-secondary btn-sm btn-dismiss-alert" data-id="${alert.id}" style="padding: 2px 8px; font-size: 11px;">&times;</button>
      </div>
    `;
  },

  decisionRecordCard(dec) {
    let alts = [];
    try { alts = JSON.parse(dec.alternativesJson || "[]"); } catch (e) { alts = []; }
    return `
      <div class="decision-card" data-id="${dec.id}">
        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px;">
          <strong style="font-size: 14px; color: #60A5FA;">${dec.statement}</strong>
          <button class="btn btn-secondary btn-sm btn-delete-decision" data-id="${dec.id}" style="padding: 2px 6px; font-size: 11px; color: var(--accent-danger);">&times;</button>
        </div>
        <div class="decision-section">
          <div class="decision-section-title">Recommendation:</div>
          <div style="color: var(--text-main); font-weight: 600;">${dec.recommendation}</div>
        </div>
        ${alts.length > 0 ? `
          <div class="decision-section">
            <div class="decision-section-title">Evaluated Alternatives:</div>
            <div style="font-size: 12px; color: var(--text-muted);">${alts.join(" &bull; ")}</div>
          </div>
        ` : ''}
        <div style="margin-top: 8px; font-size: 11px; color: var(--text-dim);">Evaluated ${new Date(dec.createdAt).toLocaleDateString()}</div>
      </div>
    `;
  },

  localModelCard(m, activeProvider) {
    const isThisActive = m.is_installed || (activeProvider === "OFFLINE_DETERMINISTIC_FALLBACK" && m.id === "qwen2.5-1.5b-q4");
    return `
      <div class="model-card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
          <strong>${m.name}</strong>
          <span class="badge ${isThisActive ? 'badge-success' : 'badge-gray'}">${isThisActive ? 'ACTIVE' : 'READY'}</span>
        </div>
        <div style="font-size: 12px; color: var(--text-muted); line-height: 1.5;">${m.architecture} &bull; ${m.file_size_mb} MB &bull; ${m.context_window} tokens</div>
        <div class="model-spec-grid">
          <div class="model-spec-item">RAM: ~${m.recommended_ram_gb} GB</div>
          <div class="model-spec-item">Quantization: Q4_K_M</div>
          <div class="model-spec-item">Inference: ARM64 CPU / NPU</div>
          <div class="model-spec-item">Offline: 100% On-Device</div>
        </div>
      </div>
    `;
  }
};
