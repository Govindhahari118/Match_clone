import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import { db, requireOpsRole } from "./shared";

const SUPPORT_STATUSES = new Set(["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"]);
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
        operatorNote: typeof value.operatorNote === "string" ? value.operatorNote : null,
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
      lastHandledBy: actor.uid,
      lastHandledRole: actor.role,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      ...(nextStatus === "RESOLVED" || nextStatus === "CLOSED" ? {
        resolvedAt: admin.firestore.FieldValue.serverTimestamp(),
      } : {
        resolvedAt: admin.firestore.FieldValue.delete(),
      }),
    });

    tx.create(auditRef, {
      actorUid: actor.uid,
      actorRole: actor.role,
      action: "SUPPORT_TICKET_STATUS_UPDATED",
      targetCollection: "supportTickets",
      targetId: ticketId,
      before: { status: previousStatus },
      after: { status: nextStatus },
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
