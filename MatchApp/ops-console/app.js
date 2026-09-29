/* global firebase */
(() => {
  "use strict";

  const auth = firebase.auth();
  const functions = firebase.functions();
  const el = (id) => document.getElementById(id);
  const state = { roles: [] };

  const roleTabs = {
    dashboard: ["support", "moderator", "kyc_reviewer", "payment_ops", "ops_admin"],
    account: ["support", "moderator", "kyc_reviewer", "payment_ops", "ops_admin"],
    support: ["support", "ops_admin"],
    reports: ["moderator", "ops_admin"],
    risk: ["moderator", "ops_admin"],
    verification: ["kyc_reviewer", "ops_admin"],
    photos: ["moderator", "ops_admin"],
    payments: ["payment_ops", "ops_admin"],
  };

  function hasRole(tab) {
    return roleTabs[tab]?.some((role) => state.roles.includes(role)) === true;
  }

  function setStatus(message, isError = false) {
    const node = el("status");
    node.textContent = message || "";
    node.classList.toggle("error", isError);
  }

  function errorMessage(error) {
    const raw = error?.message || String(error || "Unknown error");
    return raw.replace(/^FirebaseError:\s*/i, "").slice(0, 500);
  }

  async function call(name, data = {}) {
    const fn = functions.httpsCallable(name);
    const result = await fn(data);
    return result.data || {};
  }

  function clear(node) {
    while (node.firstChild) node.removeChild(node.firstChild);
  }

  function text(tag, value, className) {
    const node = document.createElement(tag);
    node.textContent = value ?? "";
    if (className) node.className = className;
    return node;
  }

  function button(label, onClick, className = "") {
    const node = document.createElement("button");
    node.type = "button";
    node.textContent = label;
    if (className) node.className = className;
    node.addEventListener("click", async () => {
      node.disabled = true;
      try {
        await onClick();
      } catch (error) {
        setStatus(errorMessage(error), true);
      } finally {
        node.disabled = false;
      }
    });
    return node;
  }

  function input(placeholder, value = "") {
    const node = document.createElement("input");
    node.placeholder = placeholder;
    node.value = value;
    return node;
  }

  function select(values, selected) {
    const node = document.createElement("select");
    values.forEach((value) => {
      const option = document.createElement("option");
      option.value = value;
      option.textContent = value;
      option.selected = value === selected;
      node.appendChild(option);
    });
    return node;
  }

  function item(title, meta = "") {
    const root = document.createElement("article");
    root.className = "item";
    root.appendChild(text("h3", title));
    if (meta) root.appendChild(text("div", meta, "muted"));
    return root;
  }

  function formatTime(value) {
    if (!value) return "—";
    const date = new Date(Number(value));
    return Number.isNaN(date.getTime()) ? "—" : date.toLocaleString();
  }

  function showTab(tab) {
    document.querySelectorAll(".panel").forEach((panel) => {
      panel.hidden = panel.id !== tab;
    });
    document.querySelectorAll("#tabs button").forEach((node) => {
      node.classList.toggle("active", node.dataset.tab === tab);
    });
  }

  function configureTabs() {
    document.querySelectorAll("#tabs button").forEach((node) => {
      const tab = node.dataset.tab;
      node.hidden = !hasRole(tab);
      node.onclick = () => showTab(tab);
    });
    const first = Object.keys(roleTabs).find(hasRole) || "dashboard";
    showTab(first);
  }

  async function loadAccess(user) {
    const access = await call("getMyOpsAccess");
    state.roles = Array.isArray(access.roles) ? access.roles : [];
    el("identity").textContent = `${user.email || user.uid} • ${state.roles.join(", ") || "no role"}`;
    configureTabs();
    el("loginCard").hidden = true;
    el("app").hidden = false;
    el("signOut").hidden = false;
    setStatus("Operator access verified.");
    if (hasRole("dashboard")) await loadMetrics();
  }

  async function loadMetrics() {
    const data = await call("getOpsQueueMetrics");
    el("metricsOutput").textContent = JSON.stringify(data, null, 2);
  }

  async function lookupAccount(query) {
    const data = await call("lookupOpsAccount", { query });
    el("accountOutput").textContent = JSON.stringify(data, null, 2);
  }

  async function loadSupport() {
    const data = await call("listSupportTickets", { status: "OPEN", limit: 100 });
    const root = el("supportList");
    clear(root);
    (data.tickets || []).forEach((ticket) => {
      const row = item(
        `${ticket.category || "Support"} • ${ticket.uid}`,
        `${ticket.status} • created ${formatTime(ticket.createdAtMillis)}`
      );
      row.appendChild(text("p", ticket.message || ""));
      const status = select(
        ["OPEN", "ASSIGNED", "IN_PROGRESS", "WAITING_USER", "RESOLVED", "CLOSED", "ESCALATED"],
        ticket.status || "OPEN"
      );
      const reason = input("Operator reason (required)");
      const note = input("Internal note (optional)");
      const team = input("Assigned team (optional)");
      const actions = document.createElement("div");
      actions.className = "row-actions";
      actions.append(status, reason, note, team);
      actions.appendChild(button("Save", async () => {
        await call("updateSupportTicketStatus", {
          ticketId: ticket.id,
          status: status.value,
          reason: reason.value,
          note: note.value,
          assignedTeam: team.value,
        });
        setStatus("Support ticket updated.");
        await loadSupport();
      }));
      row.appendChild(actions);
      root.appendChild(row);
    });
    if (!(data.tickets || []).length) root.appendChild(text("p", "No open support tickets.", "muted"));
  }

  async function loadReports() {
    const data = await call("listProfileReportsForModeration", { status: "OPEN", limit: 100 });
    const root = el("reportList");
    clear(root);
    (data.reports || []).forEach((report) => {
      const row = item(
        `${report.reason || "Report"} • target ${report.targetUid}`,
        `Reporter ${report.reporterUid} • ${formatTime(report.createdAtMillis)}`
      );
      if (report.details) row.appendChild(text("p", report.details));
      const reason = input("Review reason (required)");
      const status = select(["REVIEWING", "ACTIONED", "DISMISSED"], "REVIEWING");
      const enforcement = select(["ACTIVE", "UNDER_REVIEW", "RESTRICTED", "SUSPENDED"], "UNDER_REVIEW");
      const actions = document.createElement("div");
      actions.className = "row-actions";
      actions.append(status, reason);
      actions.appendChild(button("Update report", async () => {
        await call("updateProfileReportStatus", {
          reportId: report.id,
          status: status.value,
          reason: reason.value,
        });
        setStatus("Moderation report updated.");
        await loadReports();
      }));
      actions.append(enforcement);
      actions.appendChild(button("Apply account state", async () => {
        await call("setAccountEnforcement", {
          targetUid: report.targetUid,
          status: enforcement.value,
          reason: reason.value,
          reportId: report.id,
        });
        setStatus(`Account state set to ${enforcement.value}.`);
      }, enforcement.value === "SUSPENDED" ? "danger" : "warn"));
      row.appendChild(actions);
      root.appendChild(row);
    });
    if (!(data.reports || []).length) root.appendChild(text("p", "No open moderation reports.", "muted"));
  }

  async function loadRisk() {
    const data = await call("listRiskReviewQueue", { limit: 100 });
    const root = el("riskList");
    clear(root);
    (data.cases || []).forEach((risk) => {
      const row = item(
        `Risk signals • ${risk.uid}`,
        `Last signal ${formatTime(risk.updatedAtMillis)} • reviewed ${risk.reviewedLevel || "LOW"}`
      );
      const facts = [
        ["Reports", risk.reportSignalCount],
        ["Duplicate photos", risk.duplicatePhotoSignalCount],
        ["High-volume interest days", risk.highVolumeInterestDayCount],
        ["High-volume message days", risk.highVolumeMessageDayCount],
        ["External-link messages", risk.externalLinkMessageCount],
        ["Money-request signals", risk.moneyRequestSignalCount],
      ];
      facts.forEach(([label, value]) => {
        const pill = text("span", `${label}: ${Number(value || 0)}`, "pill");
        row.appendChild(pill);
      });
      const level = select(["LOW", "MEDIUM", "HIGH", "CRITICAL"], risk.reviewedLevel || "LOW");
      const standing = select(["UNREVIEWED", "CLEAR"], risk.reviewedStanding || "UNREVIEWED");
      const reason = input("Human review reason (required)");
      const actions = document.createElement("div");
      actions.className = "row-actions";
      actions.append(level, standing, reason);
      actions.appendChild(button("Save assessment", async () => {
        const requestedStanding = level.value === "LOW" ? standing.value : "UNREVIEWED";
        await call("updateRiskAssessment", {
          targetUid: risk.uid,
          level: level.value,
          standing: requestedStanding,
          reason: reason.value,
        });
        setStatus("Risk assessment saved.");
        await loadRisk();
      }));
      row.appendChild(actions);
      root.appendChild(row);
    });
    if (!(data.cases || []).length) root.appendChild(text("p", "No risk signals queued.", "muted"));
  }

  async function loadVerification() {
    const data = await call("listPendingVerificationRequests", { limit: 100 });
    const root = el("verificationList");
    clear(root);
    (data.requests || []).forEach((request) => {
      const row = item(
        `${request.docType || "Government ID"} • ${request.uid}`,
        `${request.verificationMethod || ""} • ${formatTime(request.submittedAtMillis)}`
      );
      const reason = input("Rejection reason");
      const actions = document.createElement("div");
      actions.className = "row-actions";
      actions.appendChild(button("Open document", async () => {
        const review = await call("getVerificationReviewCase", { targetUid: request.uid });
        window.open(review.documentUrl, "_blank", "noopener,noreferrer");
        setStatus("Short-lived KYC review link opened and audited.");
      }));
      actions.appendChild(button("Approve", async () => {
        await call("approveVerification", {
          targetUid: request.uid,
          approved: true,
          newLevel: 2,
        });
        setStatus("Verification approved.");
        await loadVerification();
      }));
      actions.append(reason);
      actions.appendChild(button("Reject", async () => {
        await call("approveVerification", {
          targetUid: request.uid,
          approved: false,
          rejectionReason: reason.value,
        });
        setStatus("Verification rejected.");
        await loadVerification();
      }, "danger"));
      row.appendChild(actions);
      root.appendChild(row);
    });
    if (!(data.requests || []).length) root.appendChild(text("p", "No pending verification requests.", "muted"));
  }

  async function loadPhotos() {
    const data = await call("listPendingPhotoModeration", { limit: 100 });
    const root = el("photoList");
    clear(root);
    (data.items || []).forEach((photo) => {
      const row = item(
        `Profile photo • ${photo.uid}`,
        `${photo.contentType || ""} • ${Number(photo.size || 0)} bytes • ${formatTime(photo.createdAtMillis)}`
      );
      const reason = input("Moderation reason (required)");
      const actions = document.createElement("div");
      actions.className = "row-actions";
      actions.appendChild(button("Open photo", async () => {
        const review = await call("getPhotoModerationReviewCase", { moderationId: photo.id });
        window.open(review.documentUrl, "_blank", "noopener,noreferrer");
        setStatus("Short-lived photo review link opened and audited.");
      }));
      actions.append(reason);
      actions.appendChild(button("Approve", async () => {
        await call("reviewProfilePhoto", {
          moderationId: photo.id,
          decision: "APPROVED",
          reason: reason.value,
        });
        setStatus("Photo approved.");
        await loadPhotos();
      }));
      actions.appendChild(button("Reject", async () => {
        await call("reviewProfilePhoto", {
          moderationId: photo.id,
          decision: "REJECTED",
          reason: reason.value,
        });
        setStatus("Photo rejected.");
        await loadPhotos();
      }, "danger"));
      row.appendChild(actions);
      root.appendChild(row);
    });
    if (!(data.items || []).length) root.appendChild(text("p", "No pending photos.", "muted"));
  }

  async function loadVideos() {
    const data = await call("listPendingVideoModeration", { limit: 100 });
    const root = el("videoList");
    clear(root);
    (data.items || []).forEach((video) => {
      const row = item(
        `Profile video • ${video.uid}`,
        `${video.contentType || ""} • ${Number(video.size || 0)} bytes • ${formatTime(video.createdAtMillis)}`
      );
      const reason = input("Moderation reason (required)");
      const actions = document.createElement("div");
      actions.className = "row-actions";
      actions.appendChild(button("Open video", async () => {
        const review = await call("getVideoModerationReviewCase", { moderationId: video.id });
        window.open(review.documentUrl, "_blank", "noopener,noreferrer");
        setStatus("Short-lived video review link opened and audited.");
      }));
      actions.append(reason);
      actions.appendChild(button("Approve", async () => {
        await call("reviewProfileVideo", {
          moderationId: video.id,
          decision: "APPROVED",
          reason: reason.value,
        });
        setStatus("Video approved.");
        await loadVideos();
      }));
      actions.appendChild(button("Reject", async () => {
        await call("reviewProfileVideo", {
          moderationId: video.id,
          decision: "REJECTED",
          reason: reason.value,
        });
        setStatus("Video rejected.");
        await loadVideos();
      }, "danger"));
      row.appendChild(actions);
      root.appendChild(row);
    });
    if (!(data.items || []).length) root.appendChild(text("p", "No pending videos.", "muted"));
  }

  async function lookupPayment(paymentId) {
    const data = await call("getPaymentReconciliationCase", { paymentId });
    el("paymentOutput").textContent = JSON.stringify(data, null, 2);
  }

  el("loginForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    setStatus("");
    try {
      await auth.signInWithEmailAndPassword(el("email").value.trim(), el("password").value);
    } catch (error) {
      setStatus(errorMessage(error), true);
    }
  });

  el("signOut").addEventListener("click", () => auth.signOut());
  el("accountForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await lookupAccount(el("accountQuery").value.trim());
    } catch (error) {
      setStatus(errorMessage(error), true);
    }
  });
  el("paymentForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await lookupPayment(el("paymentId").value.trim());
    } catch (error) {
      setStatus(errorMessage(error), true);
    }
  });

  document.querySelectorAll("[data-action]").forEach((node) => {
    node.addEventListener("click", async () => {
      const action = node.dataset.action;
      node.disabled = true;
      try {
        if (action === "metrics") await loadMetrics();
        if (action === "support") await loadSupport();
        if (action === "reports") await loadReports();
        if (action === "risk") await loadRisk();
        if (action === "verification") await loadVerification();
        if (action === "photos") await loadPhotos();
        if (action === "videos") await loadVideos();
      } catch (error) {
        setStatus(errorMessage(error), true);
      } finally {
        node.disabled = false;
      }
    });
  });

  auth.onAuthStateChanged(async (user) => {
    if (!user) {
      state.roles = [];
      el("identity").textContent = "Signed out";
      el("loginCard").hidden = false;
      el("app").hidden = true;
      el("signOut").hidden = true;
      return;
    }
    try {
      await user.getIdToken(true);
      await loadAccess(user);
    } catch (error) {
      setStatus(errorMessage(error), true);
      await auth.signOut();
    }
  });
})();
