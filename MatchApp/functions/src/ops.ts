import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import { db, requireOpsRole } from "./shared";

const SUPPORT_STATUSES = new Set(["OPEN", "ASSIGNED", "IN_PROGRESS", "WAITING_USER", "RESOLVED", "CLOSED", "ESCALATED"]);
const REPORT_STATUSES = new Set(["OPEN", "REVIEWING", "ACTIONED", "DISMISSED"]);
const MAX_PAGE_SIZE = 100;

function safeId(value: unknown, label: string): string {
  const id = typeof value === "string" ? value.trim() : "";
  if (!id || id.length > 256 || id.includes("/")) {
    throw new functions.https.HttpsError("invalid-argument", `Invalid ${label}`);
  }
  return id;
}

function safeStatus(value: unknown, allowed: Set<string>, label: string): string {
  const status = typeof value === "string" ? value.trim().toUpperCase() : "";
  if (!allowed.has(status)) {
    throw new functions.https.HttpsError("invalid-argument", `Invalid ${label} status`);
  }
  return status;
}

function safeReason(value: unknown): string {
  const reason = typeof value === "string" ? value.trim() : "";
  if (reason.length < 3 || reason.length > 500) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "A 3-500 character operator reason is required"
    );
  }
  return reason;
}

function safeAssignment(value: unknown, label: string): string | null {
  const assignment = typeof value === "string" ? value.trim() : "";
  if (!assignment) return null;
  if (assignment.length > 128 || /[\r\n]/.test(assignment)) {
    throw new functions.https.HttpsError("invalid-argument", `Invalid ${label}`);
  }
  return assignment;
}

function safeNote(value: unknown): string | null {
  const note = typeof value === "string" ? value.trim() : "";
  if (!note) return null;
  if (note.length > 1000) {
    throw new functions.https.HttpsError("invalid-argument", "Operator note is too long");
  }
  return note;
}

function pageSize(value: unknown): number {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return 50;
  return Math.max(1, Math.min(MAX_PAGE_SIZE, Math.floor(parsed)));
}

function timestampMillis(value: unknown): number | null {
  return value instanceof admin.firestore.Timestamp ? value.toMillis() : null;
}

/**
 * Support queue for authorized operations users.
 * The Android member app cannot call this unless its Firebase token carries an explicit ops role.
 */
export const listSupportTickets = functions.https.onCall(async (data, context) => {
  requireOpsRole(context, ["support", "ops_admin"]);
  const status = safeStatus(data?.status ?? "OPEN", SUPPORT_STATUSES, "support ticket");
  const limit = pageSize(data?.limit);

  const snapshot = await db.collection("supportTickets")
    .where("status", "==", status)
    .limit(limit)
    .get();

  return {
    tickets: snapshot.docs.map((doc) => {
      const value = doc.data();
      return {
        id: doc.id,
        uid: String(value.uid || ""),
        category: String(value.category || ""),
        message: String(value.message || ""),
        status: String(value.status || "OPEN"),
        source: String(value.source || ""),
        createdAtMillis: timestampMillis(value.createdAt),
        updatedAtMillis: timestampMillis(value.updatedAt),
        assignedTo: typeof value.assignedTo === "string" ? value.assignedTo : null,
        assignedTeam: typeof value.assignedTeam === "string" ? value.assignedTeam : null,
        operatorNote: typeof value.operatorNote === "string" ? value.operatorNote : null,
        resolvedAtMillis: timestampMillis(value.resolvedAt),
        escalatedAtMillis: timestampMillis(value.escalatedAt),
      };
    }),
  };
});

/**
 * Updates only workflow state for a support ticket and writes a separate immutable audit record.
 * Audit records contain the status delta/reason, not the member's free-form support message.
 */
export const updateSupportTicketStatus = functions.https.onCall(async (data, context) => {
  const actor = requireOpsRole(context, ["support", "ops_admin"]);
  const ticketId = safeId(data?.ticketId, "support ticket");
  const nextStatus = safeStatus(data?.status, SUPPORT_STATUSES, "support ticket");
  const reason = safeReason(data?.reason);
  const note = safeNote(data?.note);
  const assignedTo = safeAssignment(data?.assignedTo, "ticket owner");
  const assignedTeam = safeAssignment(data?.assignedTeam, "ticket team");

  if (nextStatus === "ASSIGNED" && !assignedTo && !assignedTeam) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Assigned tickets require an owner or team"
    );
  }

  const ticketRef = db.collection("supportTickets").doc(ticketId);
  const auditRef = db.collection("opsAuditLog").doc();

  await db.runTransaction(async (tx) => {
    const snapshot = await tx.get(ticketRef);
    if (!snapshot.exists) {
      throw new functions.https.HttpsError("not-found", "Support ticket not found");
    }
    const current = snapshot.data() || {};
    const previousStatus = String(current.status || "OPEN").toUpperCase();
    if (!SUPPORT_STATUSES.has(previousStatus)) {
      throw new functions.https.HttpsError("failed-precondition", "Support ticket has invalid state");
    }

    tx.update(ticketRef, {
      status: nextStatus,
      operatorNote: note,
      assignedTo,
      assignedTeam,
      lastHandledBy: actor.uid,
      lastHandledRole: actor.role,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      ...(nextStatus === "RESOLVED" || nextStatus === "CLOSED" ? {
        resolvedAt: admin.firestore.FieldValue.serverTimestamp(),
      } : {
        resolvedAt: admin.firestore.FieldValue.delete(),
      }),
      ...(nextStatus === "ESCALATED" ? {
        escalatedAt: admin.firestore.FieldValue.serverTimestamp(),
      } : {
        escalatedAt: admin.firestore.FieldValue.delete(),
      }),
    });

    tx.create(auditRef, {
      actorUid: actor.uid,
      actorRole: actor.role,
      action: "SUPPORT_TICKET_STATUS_UPDATED",
      targetCollection: "supportTickets",
      targetId: ticketId,
      before: { status: previousStatus },
      after: { status: nextStatus, assignedTo, assignedTeam },
      reason,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  return { success: true, ticketId, status: nextStatus };
});

export const listProfileReportsForModeration = functions.https.onCall(async (data, context) => {
  requireOpsRole(context, ["moderator", "ops_admin"]);
  const status = safeStatus(data?.status ?? "OPEN", REPORT_STATUSES, "profile report");
  const limit = pageSize(data?.limit);

  const snapshot = await db.collection("profileReports")
    .where("status", "==", status)
    .limit(limit)
    .get();

  return {
    reports: snapshot.docs.map((doc) => {
      const value = doc.data();
      return {
        id: doc.id,
        reporterUid: String(value.reporterUid || ""),
        targetUid: String(value.targetUid || ""),
        reason: String(value.reason || ""),
        details: typeof value.details === "string" ? value.details : null,
        status: String(value.status || "OPEN"),
        createdAtMillis: timestampMillis(value.createdAt),
        updatedAtMillis: timestampMillis(value.updatedAt),
      };
    }),
  };
});

/**
 * Moderator workflow transition with actor/reason/status-delta audit evidence.
 * This intentionally does not mutate the reported member's account automatically; any punitive
 * account action must be a separate explicit privileged action with its own policy and audit.
 */
export const updateProfileReportStatus = functions.https.onCall(async (data, context) => {
  const actor = requireOpsRole(context, ["moderator", "ops_admin"]);
  const reportId = safeId(data?.reportId, "profile report");
  const nextStatus = safeStatus(data?.status, REPORT_STATUSES, "profile report");
  const reason = safeReason(data?.reason);

  const reportRef = db.collection("profileReports").doc(reportId);
  const auditRef = db.collection("opsAuditLog").doc();

  await db.runTransaction(async (tx) => {
    const snapshot = await tx.get(reportRef);
    if (!snapshot.exists) {
      throw new functions.https.HttpsError("not-found", "Profile report not found");
    }
    const current = snapshot.data() || {};
    const previousStatus = String(current.status || "OPEN").toUpperCase();
    if (!REPORT_STATUSES.has(previousStatus)) {
      throw new functions.https.HttpsError("failed-precondition", "Profile report has invalid state");
    }

    tx.update(reportRef, {
      status: nextStatus,
      lastReviewedBy: actor.uid,
      lastReviewedRole: actor.role,
      resolutionReason: reason,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      ...(nextStatus === "ACTIONED" || nextStatus === "DISMISSED" ? {
        resolvedAt: admin.firestore.FieldValue.serverTimestamp(),
      } : {
        resolvedAt: admin.firestore.FieldValue.delete(),
      }),
    });

    tx.create(auditRef, {
      actorUid: actor.uid,
      actorRole: actor.role,
      action: "PROFILE_REPORT_STATUS_UPDATED",
      targetCollection: "profileReports",
      targetId: reportId,
      before: { status: previousStatus },
      after: { status: nextStatus },
      reason,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  return { success: true, reportId, status: nextStatus };
});


/**
 * Minimal privacy-safe queue/SLA metrics for operations dashboards.
 * Returns counts and age buckets only; no member messages, report details, contact data or KYC data.
 */
export const getOpsQueueMetrics = functions.https.onCall(async (_data, context) => {
  requireOpsRole(context, ["support", "moderator", "payment_ops", "ops_admin"]);
  const now = Date.now();

  const [tickets, reports] = await Promise.all([
    db.collection("supportTickets")
      .where("status", "in", ["OPEN", "ASSIGNED", "IN_PROGRESS", "WAITING_USER", "ESCALATED"])
      .limit(1000)
      .get(),
    db.collection("profileReports")
      .where("status", "in", ["OPEN", "REVIEWING"])
      .limit(1000)
      .get(),
  ]);

  const summarize = (docs: FirebaseFirestore.QueryDocumentSnapshot[]) => {
    let olderThan24h = 0;
    let olderThan72h = 0;
    for (const doc of docs) {
      const createdAt = doc.data().createdAt;
      const createdMillis = createdAt instanceof admin.firestore.Timestamp
        ? createdAt.toMillis()
        : now;
      const age = Math.max(0, now - createdMillis);
      if (age >= 24 * 60 * 60 * 1000) olderThan24h += 1;
      if (age >= 72 * 60 * 60 * 1000) olderThan72h += 1;
    }
    return { open: docs.length, olderThan24h, olderThan72h };
  };

  return {
    generatedAtMillis: now,
    support: summarize(tickets.docs),
    moderation: summarize(reports.docs),
    truncated: tickets.size >= 1000 || reports.size >= 1000,
  };
});


/**
 * Role-scoped payment support view. Raw purchase tokens are never stored or returned.
 * The response is intentionally limited to reconciliation metadata and current server entitlement.
 */
export const getPaymentReconciliationCase = functions.https.onCall(async (data, context) => {
  requireOpsRole(context, ["payment_ops", "ops_admin"]);
  const paymentId = safeId(data?.paymentId, "payment");

  const payment = await db.collection("payments").doc(paymentId).get();
  if (!payment.exists) {
    throw new functions.https.HttpsError("not-found", "Payment record not found");
  }
  const value = payment.data() || {};
  const uid = String(value.uid || "");
  if (!uid) {
    throw new functions.https.HttpsError("failed-precondition", "Payment record has no account owner");
  }

  const [user, subscription] = await Promise.all([
    db.collection("users").doc(uid).get(),
    db.collection("subscriptions").doc(uid).get(),
  ]);
  const userData = user.data() || {};
  const subscriptionData = subscription.data() || {};

  return {
    payment: {
      id: payment.id,
      uid,
      provider: String(value.provider || ""),
      status: String(value.status || ""),
      productId: String(value.productId || ""),
      entitlementType: String(value.entitlementType || ""),
      entitlementId: String(value.entitlementId || value.planId || value.boostId || ""),
      orderId: typeof value.orderId === "string" ? value.orderId : null,
      regionCode: typeof value.regionCode === "string" ? value.regionCode : null,
      grantedAtMillis: Number(value.grantedAtMillis || 0),
      expiresAtMillis: Number(value.expiresAtMillis || 0),
      consumptionPending: value.consumptionPending === true,
      verifiedAtMillis: timestampMillis(value.verifiedAt),
      voidedAtMillis: Number(value.voidedAtMillis || 0) || timestampMillis(value.voidedAt),
      voidedReason: typeof value.voidedReason === "string" ? value.voidedReason : null,
    },
    currentEntitlement: {
      isPremium: userData.isPremium === true,
      subscriptionPlan: String(userData.subscriptionPlan || "FREE"),
      subscriptionExpiry: Number(userData.subscriptionExpiry || 0),
      boostUntil: Number(subscriptionData.boostUntil || 0),
    },
  };
});

/**
 * Moderator case view: only report evidence needed for review plus a minimal target trust state.
 * It deliberately omits contact details, exact location, messages, payment data and identity docs.
 */
export const getModerationCase = functions.https.onCall(async (data, context) => {
  requireOpsRole(context, ["moderator", "ops_admin"]);
  const reportId = safeId(data?.reportId, "profile report");
  const report = await db.collection("profileReports").doc(reportId).get();
  if (!report.exists) {
    throw new functions.https.HttpsError("not-found", "Profile report not found");
  }
  const value = report.data() || {};
  const targetUid = String(value.targetUid || "");
  if (!targetUid) {
    throw new functions.https.HttpsError("failed-precondition", "Report target is missing");
  }

  const [target, verification, audit] = await Promise.all([
    db.collection("users").doc(targetUid).get(),
    db.collection("verifications").doc(targetUid).get(),
    db.collection("opsAuditLog")
      .where("targetCollection", "==", "profileReports")
      .where("targetId", "==", reportId)
      .limit(50)
      .get(),
  ]);
  const targetData = target.data() || {};
  const verificationData = verification.data() || {};

  return {
    report: {
      id: report.id,
      reporterUid: String(value.reporterUid || ""),
      targetUid,
      reason: String(value.reason || ""),
      details: typeof value.details === "string" ? value.details : null,
      status: String(value.status || "OPEN"),
      createdAtMillis: timestampMillis(value.createdAt),
      updatedAtMillis: timestampMillis(value.updatedAt),
      resolutionReason: typeof value.resolutionReason === "string"
        ? value.resolutionReason
        : null,
    },
    target: {
      exists: target.exists,
      displayName: String(targetData.displayName || ""),
      matrimonyId: String(targetData.matrimonyId || ""),
      isVerified: targetData.isVerified === true,
      verificationLevel: Number(targetData.verificationLevel || 0),
      accountStatus: String(targetData.accountStatus || "ACTIVE"),
      matrimonyPaused: targetData.matrimonyPaused === true,
    },
    verification: {
      status: String(verificationData.status || ""),
      level: Number(verificationData.level || 0),
      updatedAtMillis: timestampMillis(verificationData.updatedAt),
    },
    audit: audit.docs.map((doc) => {
      const entry = doc.data();
      return {
        id: doc.id,
        actorUid: String(entry.actorUid || ""),
        actorRole: String(entry.actorRole || ""),
        action: String(entry.action || ""),
        reason: String(entry.reason || ""),
        createdAtMillis: timestampMillis(entry.createdAt),
      };
    }),
  };
});
