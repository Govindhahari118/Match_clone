import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { requireActiveConsent } from "./consent";
import {
  ASSISTED_STATUSES,
  assistedStatusTransitionAllowed,
  normalizeAssistedPlan,
  normalizeAssistedStatus,
} from "./assistedPolicy";
import { db, requireAppCheck, requireOpsRole } from "./shared";

function safeText(value: unknown, max: number): string {
  return typeof value === "string" ? value.trim().slice(0, max) : "";
}

function normalizePhone(value: unknown): string {
  const digits = typeof value === "string" ? value.replace(/\D/g, "") : "";
  if (!/^\d{10,15}$/.test(digits)) {
    throw new functions.https.HttpsError("invalid-argument", "Enter a valid callback number");
  }
  return digits;
}

function timestampMillis(value: unknown): number | null {
  return value instanceof admin.firestore.Timestamp ? value.toMillis() : null;
}

/**
 * Records interest in a human-assisted service. This does not purchase a plan, assign an operator,
 * or create a paid entitlement. One canonical request per account prevents duplicate callback spam.
 */
export const requestRelationshipManager = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const plan = normalizeAssistedPlan(data?.plan);
  if (!plan) throw new functions.https.HttpsError("invalid-argument", "Unsupported assisted plan");
  const name = safeText(data?.name, 100);
  if (name.length < 2) throw new functions.https.HttpsError("invalid-argument", "Enter your name");
  const phone = normalizePhone(data?.phone);
  const preferences = safeText(data?.preferences, 2000);
  if (preferences) await requireActiveConsent(uid, "sensitive_preferences");

  const user = await db.collection("users").doc(uid).get();
  if (!user.exists || String(user.data()?.accountStatus || "ACTIVE").toUpperCase() !== "ACTIVE") {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Assisted matchmaking is unavailable while the account is not active"
    );
  }

  const ref = db.collection("rmRequests").doc(uid);
  await db.runTransaction(async (tx) => {
    const existing = await tx.get(ref);
    const current = existing.data() || {};
    const currentStatus = normalizeAssistedStatus(current.status);
    if (currentStatus && !["RESOLVED", "CANCELLED"].includes(currentStatus)) {
      tx.set(ref, {
        plan,
        preferences,
        name,
        phone,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
      return;
    }

    tx.set(ref, {
      uid,
      plan,
      preferences,
      name,
      phone,
      status: "OPEN",
      source: "ANDROID",
      assignedTo: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: false });
  });

  return { requestId: uid, status: "OPEN_OR_ACTIVE" };
});

export const getMyRelationshipManagerRequest = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const snap = await db.collection("rmRequests").doc(uid).get();
  if (!snap.exists) return { request: null };
  const value = snap.data() || {};
  return {
    request: {
      id: snap.id,
      plan: String(value.plan || ""),
      preferences: String(value.preferences || ""),
      name: String(value.name || ""),
      phone: String(value.phone || ""),
      status: String(value.status || "OPEN"),
      createdAtMillis: timestampMillis(value.createdAt),
      updatedAtMillis: timestampMillis(value.updatedAt),
    },
  };
});

export const cancelMyRelationshipManagerRequest = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const ref = db.collection("rmRequests").doc(uid);
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) return;
    const current = normalizeAssistedStatus(snap.data()?.status) || "OPEN";
    if (current === "RESOLVED" || current === "CANCELLED") return;
    tx.update(ref, {
      status: "CANCELLED",
      preferences: "",
      phone: "",
      cancelledAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });
  return { success: true };
});

export const listAssistedRequests = functions.https.onCall(async (data, context) => {
  requireOpsRole(context, ["support", "ops_admin"]);
  const status = normalizeAssistedStatus(data?.status ?? "OPEN");
  if (!status) throw new functions.https.HttpsError("invalid-argument", "Invalid request status");
  const rawLimit = Number(data?.limit ?? 50);
  const limit = Number.isFinite(rawLimit) ? Math.max(1, Math.min(100, Math.floor(rawLimit))) : 50;
  const snapshot = await db.collection("rmRequests").where("status", "==", status).limit(limit).get();
  return {
    requests: snapshot.docs.map((doc) => {
      const value = doc.data();
      return {
        id: doc.id,
        uid: String(value.uid || doc.id),
        plan: String(value.plan || ""),
        name: String(value.name || ""),
        phone: String(value.phone || ""),
        preferences: String(value.preferences || ""),
        status: String(value.status || "OPEN"),
        assignedTo: typeof value.assignedTo === "string" ? value.assignedTo : null,
        createdAtMillis: timestampMillis(value.createdAt),
        updatedAtMillis: timestampMillis(value.updatedAt),
      };
    }),
  };
});

export const updateAssistedRequest = functions.https.onCall(async (data, context) => {
  const actor = requireOpsRole(context, ["support", "ops_admin"]);
  const uid = safeText(data?.uid, 128);
  if (!uid) throw new functions.https.HttpsError("invalid-argument", "Account is required");
  const next = normalizeAssistedStatus(data?.status);
  if (!next) throw new functions.https.HttpsError("invalid-argument", "Invalid request status");
  const reason = safeText(data?.reason, 500);
  if (reason.length < 3) {
    throw new functions.https.HttpsError("invalid-argument", "A 3-500 character reason is required");
  }
  const assignedTo = safeText(data?.assignedTo, 128) || null;
  const ref = db.collection("rmRequests").doc(uid);
  const audit = db.collection("opsAuditLog").doc();

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) throw new functions.https.HttpsError("not-found", "Request not found");
    const current = normalizeAssistedStatus(snap.data()?.status);
    if (!current) throw new functions.https.HttpsError("failed-precondition", "Request state is invalid");
    if (!assistedStatusTransitionAllowed(current, next)) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Cannot move assisted request from ${current} to ${next}`
      );
    }
    if (next === "ASSIGNED" && !assignedTo) {
      throw new functions.https.HttpsError("invalid-argument", "Assigned requests require an owner");
    }

    tx.update(ref, {
      status: next,
      assignedTo,
      lastHandledBy: actor.uid,
      lastHandledRole: actor.role,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      ...(next === "RESOLVED" ? {
        resolvedAt: admin.firestore.FieldValue.serverTimestamp(),
      } : {}),
    });
    tx.create(audit, {
      actorUid: actor.uid,
      actorRole: actor.role,
      action: "ASSISTED_REQUEST_UPDATED",
      targetCollection: "rmRequests",
      targetId: uid,
      before: { status: current },
      after: { status: next, assignedTo },
      reason,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  return { success: true, status: next };
});

export const ASSISTED_WORKFLOW_STATUSES = ASSISTED_STATUSES;
