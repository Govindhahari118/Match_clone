import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import { db, persistAndSendNotification, requireOpsRole } from "./shared";
import {
  enforcementSuppressesInteractions,
  normalizeEnforcementStatus,
} from "./accountEnforcementPolicy";
import { resolveMembershipState } from "./membershipAuthority";

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

  const transition = await db.runTransaction(async (tx) => {
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
      requestId: actor.requestId,
      action: "SUPPORT_TICKET_STATUS_UPDATED",
      targetCollection: "supportTickets",
      targetId: ticketId,
      before: { status: previousStatus },
      after: { status: nextStatus, assignedTo, assignedTeam },
      reason,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return {
      previousStatus,
      ownerUid: String(current.uid || ""),
    };
  });

  if (transition.ownerUid && transition.previousStatus !== nextStatus) {
    try {
      await persistAndSendNotification({
        notificationId: `support_${ticketId}_${nextStatus}`,
        userId: transition.ownerUid,
        type: "SYSTEM",
        title: "Support request updated",
        body: nextStatus === "RESOLVED" || nextStatus === "CLOSED"
          ? "Your support request has been resolved. Open Help & Support to review its status."
          : "Your support request status changed. Open Help & Support to review it.",
        entityType: "support",
        entityId: ticketId,
        deepLink: "matrimonyconnect://notifications",
        pushType: "support_status",
        preferenceKey: "critical",
        priority: "high",
      });
    } catch (error) {
      functions.logger.warn("Support status notification delivery failed", {
        ticketId,
        nextStatus,
        error: error instanceof Error ? error.message : String(error),
      });
    }
  }

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
      requestId: actor.requestId,
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
  requireOpsRole(context, ["support", "moderator", "payment_ops", "kyc_reviewer", "ops_admin"]);
  const now = Date.now();

  const [tickets, assisted, reports, verifications, photoModeration, videoModeration] = await Promise.all([
    db.collection("supportTickets")
      .where("status", "in", ["OPEN", "ASSIGNED", "IN_PROGRESS", "WAITING_USER", "ESCALATED"])
      .limit(1000)
      .get(),
    db.collection("rmRequests")
      .where("status", "in", ["OPEN", "ASSIGNED", "IN_PROGRESS", "WAITING_ON_MEMBER"])
      .limit(1000)
      .get(),
    db.collection("profileReports")
      .where("status", "in", ["OPEN", "REVIEWING"])
      .limit(1000)
      .get(),
    db.collection("verificationRequests")
      .where("status", "==", "pending")
      .limit(1000)
      .get(),
    db.collection("photoModeration")
      .where("status", "==", "PENDING")
      .limit(1000)
      .get(),
    db.collection("videoModeration")
      .where("status", "==", "PENDING")
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
    assisted: summarize(assisted.docs),
    moderation: summarize(reports.docs),
    verification: summarize(verifications.docs),
    photoModeration: summarize(photoModeration.docs),
    videoModeration: summarize(videoModeration.docs),
    truncated:
      tickets.size >= 1000 ||
      assisted.size >= 1000 ||
      reports.size >= 1000 ||
      verifications.size >= 1000 ||
      photoModeration.size >= 1000 ||
      videoModeration.size >= 1000,
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
  const membership = resolveMembershipState(subscriptionData, userData);

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
      isPremium: membership.active,
      subscriptionPlan: membership.planId,
      subscriptionExpiry: membership.expiresAtMillis,
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


/**
 * Explicit account enforcement for moderation. The public user document receives only the
 * interaction-relevant status; the operator reason remains in restricted operational records.
 * Non-active states immediately fail the same lifecycle checks used by discovery, interests,
 * Nearby, shared horoscope and other trusted backend paths.
 */
export const setAccountEnforcement = functions.https.onCall(async (data, context) => {
  const actor = requireOpsRole(context, ["moderator", "ops_admin"]);
  const targetUid = safeId(data?.targetUid, "account");
  if (targetUid === actor.uid) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Operators cannot enforce their own account"
    );
  }

  const nextStatus = normalizeEnforcementStatus(data?.status);
  if (!nextStatus) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid enforcement status");
  }
  const reason = safeReason(data?.reason);
  const reportId = data?.reportId == null ? null : safeId(data.reportId, "profile report");

  const userRef = db.collection("users").doc(targetUid);
  const enforcementRef = db.collection("accountEnforcements").doc(targetUid);
  const auditRef = db.collection("opsAuditLog").doc();

  const previousStatus = await db.runTransaction(async (tx) => {
    const user = await tx.get(userRef);
    if (!user.exists) {
      throw new functions.https.HttpsError("not-found", "Account not found");
    }
    const before = String(user.data()?.accountStatus || "ACTIVE").toUpperCase();

    tx.set(userRef, {
      accountStatus: nextStatus,
      matrimonyPaused: enforcementSuppressesInteractions(nextStatus),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });

    tx.set(enforcementRef, {
      targetUid,
      status: nextStatus,
      reason,
      reportId,
      lastActorUid: actor.uid,
      lastActorRole: actor.role,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      ...(nextStatus === "ACTIVE" ? {
        clearedAt: admin.firestore.FieldValue.serverTimestamp(),
      } : {
        clearedAt: admin.firestore.FieldValue.delete(),
      }),
    }, { merge: true });

    tx.create(auditRef, {
      actorUid: actor.uid,
      actorRole: actor.role,
      requestId: actor.requestId,
      action: "ACCOUNT_ENFORCEMENT_UPDATED",
      targetCollection: "users",
      targetId: targetUid,
      before: { accountStatus: before },
      after: { accountStatus: nextStatus },
      reason,
      reportId,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return before;
  });

  if (nextStatus === "SUSPENDED") {
    await admin.auth().revokeRefreshTokens(targetUid);
  }

  functions.logger.info("Account enforcement updated", {
    actorUid: actor.uid,
    actorRole: actor.role,
    targetUid,
    previousStatus,
    nextStatus,
    reportId,
  });

  return {
    success: true,
    targetUid,
    previousStatus,
    status: nextStatus,
  };
});


const ALL_OPS_ROLES = ["support", "moderator", "kyc_reviewer", "payment_ops", "ops_admin"] as const;

/** Minimal bootstrap contract for the separate operator console. */
export const getMyOpsAccess = functions.https.onCall(async (_data, context) => {
  const actor = requireOpsRole(context, [...ALL_OPS_ROLES]);
  const raw = Array.isArray(context.auth?.token?.roles)
    ? context.auth?.token?.roles.filter((value): value is string => typeof value === "string")
    : [];
  const roles = ALL_OPS_ROLES.filter((role) => raw.includes(role));
  return { uid: actor.uid, roles };
});

/**
 * Exact operational account lookup. Search is deliberately not fuzzy and never returns raw KYC,
 * contact numbers, messages, private astrology or payment tokens.
 */
export const lookupOpsAccount = functions.https.onCall(async (data, context) => {
  requireOpsRole(context, [...ALL_OPS_ROLES]);
  const query = typeof data?.query === "string" ? data.query.trim() : "";
  if (!query || query.length > 254) {
    throw new functions.https.HttpsError("invalid-argument", "Enter an account identifier");
  }

  let uid = "";
  if (/^@[a-z0-9][a-z0-9._]{2,29}$/i.test(query)) {
    const normalized = query.slice(1).toLowerCase();
    const registry = await db.collection("usernames").doc(normalized).get();
    uid = String(registry.data()?.uid || "");
  } else if (/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(query)) {
    try {
      uid = (await admin.auth().getUserByEmail(query.toLowerCase())).uid;
    } catch {
      throw new functions.https.HttpsError("not-found", "Account not found");
    }
  } else if (/^MAT-[A-F0-9]+$/i.test(query)) {
    const registry = await db.collection("matrimonyIds").doc(query.toUpperCase()).get();
    uid = String(registry.data()?.uid || "");
  } else if (/^[a-zA-Z0-9._-]{3,128}$/.test(query)) {
    const normalized = query.toLowerCase();
    if (/^[a-z0-9][a-z0-9._]{2,29}$/.test(normalized)) {
      const registry = await db.collection("usernames").doc(normalized).get();
      uid = String(registry.data()?.uid || "");
    }
    if (!uid) {
      const direct = await db.collection("users").doc(query).get();
      if (direct.exists) uid = direct.id;
    }
  }
  if (!uid) throw new functions.https.HttpsError("not-found", "Account not found");

  const [profile, verification, risk, enforcement, subscription] = await Promise.all([
    db.collection("users").doc(uid).get(),
    db.collection("verifications").doc(uid).get(),
    db.collection("riskAssessments").doc(uid).get(),
    db.collection("accountEnforcements").doc(uid).get(),
    db.collection("subscriptions").doc(uid).get(),
  ]);
  if (!profile.exists) throw new functions.https.HttpsError("not-found", "Account not found");

  const p = profile.data() || {};
  const v = verification.data() || {};
  const r = risk.data() || {};
  const e = enforcement.data() || {};
  const membershipState = resolveMembershipState(subscription.data(), p);
  return {
    account: {
      uid,
      displayName: String(p.displayName || ""),
      username: String(p.username || ""),
      matrimonyId: String(p.matrimonyId || ""),
      accountStatus: String(p.accountStatus || "ACTIVE"),
      matrimonyPaused: p.matrimonyPaused === true,
      isVerified: p.isVerified === true,
      verificationLevel: Number(p.verificationLevel || 0),
      profileCompleteness: Number(p.profileCompleteness || 0),
      subscriptionPlan: membershipState.planId,
      subscriptionExpiry: membershipState.expiresAtMillis,
      createdAtMillis: timestampMillis(p.createdAt) || Number(p.createdAt || 0),
    },
    verification: {
      phoneStatus: String(v.phoneStatus || ""),
      status: String(v.status || ""),
      updatedAtMillis: timestampMillis(v.updatedAt),
    },
    risk: {
      level: String(r.level || "LOW"),
      standing: String(r.standing || "UNREVIEWED"),
      updatedAtMillis: timestampMillis(r.updatedAt),
    },
    enforcement: enforcement.exists ? {
      status: String(e.status || ""),
      updatedAtMillis: timestampMillis(e.updatedAt),
      clearedAtMillis: timestampMillis(e.clearedAt),
    } : null,
  };
});

/** KYC queue metadata; raw document bytes are never included in list responses. */
export const listPendingVerificationRequests = functions.https.onCall(async (data, context) => {
  requireOpsRole(context, ["kyc_reviewer", "ops_admin"]);
  const limit = pageSize(data?.limit);
  const snapshot = await db.collection("verificationRequests")
    .where("status", "==", "pending")
    .limit(limit)
    .get();
  return {
    requests: snapshot.docs.map((doc) => {
      const value = doc.data();
      return {
        uid: doc.id,
        docType: String(value.docType || ""),
        verificationMethod: String(value.verificationMethod || ""),
        submittedAtMillis: timestampMillis(value.submittedAt),
        updatedAtMillis: timestampMillis(value.updatedAt),
      };
    }),
  };
});

/**
 * Short-lived, audited raw-document access for an assigned KYC reviewer. This avoids ever making
 * verification Storage objects member-readable or exposing permanent download tokens.
 */
export const getVerificationReviewCase = functions.https.onCall(async (data, context) => {
  const actor = requireOpsRole(context, ["kyc_reviewer", "ops_admin"]);
  const targetUid = safeId(data?.targetUid, "verification account");
  const request = await db.collection("verificationRequests").doc(targetUid).get();
  if (!request.exists || String(request.data()?.status || "").toLowerCase() !== "pending") {
    throw new functions.https.HttpsError("not-found", "Pending verification request not found");
  }

  const value = request.data() || {};
  const documentPath = String(value.documentPath || "");
  if (!documentPath.startsWith(`verifications/${targetUid}/`) || documentPath.includes("..")) {
    throw new functions.https.HttpsError("failed-precondition", "Verification document path is invalid");
  }

  const [signedUrl] = await admin.storage().bucket().file(documentPath).getSignedUrl({
    action: "read",
    expires: Date.now() + 5 * 60 * 1000,
  });
  await db.collection("opsAuditLog").add({
    actorUid: actor.uid,
    actorRole: actor.role,
    requestId: actor.requestId,
    action: "VERIFICATION_DOCUMENT_ACCESSED",
    targetCollection: "verificationRequests",
    targetId: targetUid,
    reason: "KYC_REVIEW",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return {
    uid: targetUid,
    docType: String(value.docType || ""),
    verificationMethod: String(value.verificationMethod || ""),
    submittedAtMillis: timestampMillis(value.submittedAt),
    documentUrl: signedUrl,
    expiresAtMillis: Date.now() + 5 * 60 * 1000,
  };
});

/** Private fraud-review queue. Signals are evidence for human review, never automatic punishment. */
export const listRiskReviewQueue = functions.https.onCall(async (data, context) => {
  requireOpsRole(context, ["moderator", "ops_admin"]);
  const limit = pageSize(data?.limit);
  const snapshot = await db.collection("riskSignals")
    .orderBy("updatedAt", "desc")
    .limit(limit)
    .get();

  const assessments = snapshot.docs.length
    ? await db.getAll(...snapshot.docs.map((doc) => db.collection("riskAssessments").doc(doc.id)))
    : [];
  return {
    cases: snapshot.docs.map((doc, index) => {
      const value = doc.data();
      const assessment = assessments[index]?.data() || {};
      return {
        uid: doc.id,
        reportSignalCount: Number(value.reportSignalCount || 0),
        duplicatePhotoSignalCount: Number(value.duplicatePhotoSignalCount || 0),
        duplicateBioSignalCount: Number(value.duplicateBioSignalCount || 0),
        highVolumeInterestDayCount: Number(value.highVolumeInterestDayCount || 0),
        highVolumeMessageDayCount: Number(value.highVolumeMessageDayCount || 0),
        externalLinkMessageCount: Number(value.externalLinkMessageCount || 0),
        moneyRequestSignalCount: Number(value.moneyRequestSignalCount || 0),
        sharedDeviceAccountSwitchCount: Number(value.sharedDeviceAccountSwitchCount || 0),
        rapidProfileMutationDayCount: Number(value.rapidProfileMutationDayCount || 0),
        paymentAccountMismatchCount: Number(value.paymentAccountMismatchCount || 0),
        updatedAtMillis: timestampMillis(value.updatedAt),
        reviewedLevel: String(assessment.level || "LOW"),
        reviewedStanding: String(assessment.standing || "UNREVIEWED"),
        reviewedAtMillis: timestampMillis(assessment.updatedAt),
      };
    }),
  };
});

/** Role-scoped payment history for an account; provider purchase tokens are never returned. */
export const listAccountPayments = functions.https.onCall(async (data, context) => {
  requireOpsRole(context, ["payment_ops", "ops_admin"]);
  const uid = safeId(data?.uid, "account");
  const limit = pageSize(data?.limit);
  const snapshot = await db.collection("payments")
    .where("uid", "==", uid)
    .limit(limit)
    .get();

  return {
    payments: snapshot.docs.map((doc) => {
      const value = doc.data();
      return {
        id: doc.id,
        status: String(value.status || ""),
        provider: String(value.provider || ""),
        productId: String(value.productId || ""),
        entitlementType: String(value.entitlementType || ""),
        entitlementId: String(value.entitlementId || value.planId || value.boostId || ""),
        orderId: typeof value.orderId === "string" ? value.orderId : null,
        grantedAtMillis: Number(value.grantedAtMillis || 0),
        expiresAtMillis: Number(value.expiresAtMillis || 0),
        verifiedAtMillis: timestampMillis(value.verifiedAt),
        voidedAtMillis: Number(value.voidedAtMillis || 0) || timestampMillis(value.voidedAt),
      };
    }),
  };
});
