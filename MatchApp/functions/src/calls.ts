import { localizedNotificationCopy } from "./notificationCopyPolicy";
import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db, persistAndSendNotification, requireAppCheck } from "./shared";
import { productionFeatureEnabled } from "./featureFlagPolicy";
import { accountIsActive } from "./accountStatusPolicy";
import { evaluateCallEligibility } from "./callPolicy";
import {
  callInitiationAllowed,
  normalizeCallKind,
} from "./callSessionPolicy";
import { communicationProvider } from "./communicationProvider";
import { notificationDeepLink } from "./notificationLinkPolicy";
import {
  canCancelCallRequest,
  canReplaceCallRequest,
  canRespondToCallRequest,
  normalizeCallRequestStatus,
  validProposedCallTime,
} from "./callRequestPolicy";

function cleanUid(value: unknown): string {
  const uid = typeof value === "string" ? value.trim() : "";
  if (!uid || uid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "A valid target member is required");
  }
  return uid;
}

function pairId(uidA: string, uidB: string): string {
  return [uidA, uidB].sort().join("_");
}

function positiveLimit(value: unknown, fallback: number, max: number): number {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.max(1, Math.min(max, Math.trunc(parsed)));
}

async function relationshipEligibility(
  callerUid: string, targetUid: string, tx?: FirebaseFirestore.Transaction
) {
  const read = (ref: FirebaseFirestore.DocumentReference) => tx ? tx.get(ref) : ref.get();
  const [
    caller,
    target,
    match,
    callerBlock,
    targetBlock,
    callerPrivacy,
    targetPrivacy,
  ] = await Promise.all([
    read(db.collection("users").doc(callerUid)),
    read(db.collection("users").doc(targetUid)),
    read(db.collection("matches").doc(pairId(callerUid, targetUid))),
    read(db.collection("blocks").doc(callerUid).collection("blocked").doc(targetUid)),
    read(db.collection("blocks").doc(targetUid).collection("blocked").doc(callerUid)),
    read(db.collection("privacyRelations").doc(callerUid).collection("members").doc(targetUid)),
    read(db.collection("privacyRelations").doc(targetUid).collection("members").doc(callerUid)),
  ]);

  return evaluateCallEligibility({
    callerUid,
    targetUid,
    callerActive: caller.exists && accountIsActive(caller.data()?.accountStatus),
    targetActive: target.exists && accountIsActive(target.data()?.accountStatus),
    mutualMatch: match.exists &&
      Array.isArray(match.data()?.users) &&
      match.data()?.users.length === 2 &&
      match.data()?.users.includes(callerUid) &&
      match.data()?.users.includes(targetUid),
    blockedEitherWay: callerBlock.exists || targetBlock.exists,
    profileHiddenEitherWay:
      callerPrivacy.data()?.profileHidden === true ||
      targetPrivacy.data()?.profileHidden === true,
    contactHiddenEitherWay:
      callerPrivacy.data()?.contactHidden === true ||
      targetPrivacy.data()?.contactHidden === true,
  });
}

function providerRolloutReady(): boolean {
  return communicationProvider.ready &&
    productionFeatureEnabled(functions.config().features?.secure_calls);
}

/**
 * Returns current relationship eligibility separately from provider availability.
 * Provider capability never overrides mutual-match, block, privacy or account-state authority.
 */
export const getSecureCallCapability = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = context.auth?.uid;
  if (!callerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = cleanUid(data?.targetUid);
  const result = await relationshipEligibility(callerUid, targetUid);
  const rolloutEnabled = productionFeatureEnabled(functions.config().features?.secure_calls);
  const providerReady = communicationProvider.ready && rolloutEnabled;

  return {
    eligible: result.eligible,
    reason: result.reason,
    providerReady,
    rolloutEnabled,
    voiceAvailable: result.eligible && providerReady && communicationProvider.voiceAvailable,
    videoAvailable: result.eligible && providerReady && communicationProvider.videoAvailable,
    numberMaskingAvailable:
      result.eligible && providerReady && communicationProvider.numberMaskingAvailable,
  };
});

/**
 * Server-only call-session allocation boundary.
 *
 * The default provider adapter is disabled, so this endpoint fails closed until a reviewed
 * production adapter is installed and the secure-calls rollout flag is enabled.
 */
export const startSecureCallSession = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = context.auth?.uid;
  if (!callerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = cleanUid(data?.targetUid);
  const kind = normalizeCallKind(data?.kind);
  const eligibility = await relationshipEligibility(callerUid, targetUid);
  if (!eligibility.eligible) {
    throw new functions.https.HttpsError("failed-precondition", eligibility.reason);
  }
  if (!providerRolloutReady()) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Secure calling is not enabled in production"
    );
  }
  if (kind === "VOICE" && !communicationProvider.voiceAvailable) {
    throw new functions.https.HttpsError("failed-precondition", "Voice calling is unavailable");
  }
  if (kind === "VIDEO" && !communicationProvider.videoAvailable) {
    throw new functions.https.HttpsError("failed-precondition", "Video calling is unavailable");
  }

  const now = Date.now();
  const hourBucket = Math.floor(now / 3_600_000);
  const pair = pairId(callerUid, targetUid);
  const accountLimit = positiveLimit(functions.config().call_limits?.account_hourly, 10, 100);
  const pairLimit = positiveLimit(functions.config().call_limits?.pair_hourly, 3, 30);
  const accountUsageRef = db.collection("callRateUsage").doc(`${callerUid}_${hourBucket}`);
  const pairUsageRef = db.collection("callPairRateUsage").doc(`${pair}_${hourBucket}`);
  const sessionRef = db.collection("callSessions").doc();

  await db.runTransaction(async (tx) => {
    const [accountUsage, pairUsage] = await Promise.all([
      tx.get(accountUsageRef),
      tx.get(pairUsageRef),
    ]);
    const accountStarts = Number(accountUsage.data()?.count || 0);
    const pairStarts = Number(pairUsage.data()?.count || 0);
    if (!callInitiationAllowed(accountStarts, pairStarts, accountLimit, pairLimit)) {
      throw new functions.https.HttpsError(
        "resource-exhausted",
        "Secure call initiation limit reached. Try again later."
      );
    }

    const stamp = admin.firestore.FieldValue.serverTimestamp();
    tx.set(accountUsageRef, {
      uid: callerUid,
      bucket: hourBucket,
      count: accountStarts + 1,
      updatedAt: stamp,
    }, { merge: false });
    tx.set(pairUsageRef, {
      pairId: pair,
      bucket: hourBucket,
      count: pairStarts + 1,
      updatedAt: stamp,
    }, { merge: false });
    tx.create(sessionRef, {
      callerUid,
      targetUid,
      users: [callerUid, targetUid].sort(),
      kind,
      state: "REQUESTED",
      provider: "pending-allocation",
      createdAt: stamp,
      updatedAt: stamp,
    });
  });

  try {
    const allocation = await communicationProvider.allocateSession({
      sessionId: sessionRef.id,
      callerUid,
      targetUid,
      kind,
    });
    const ttlMs = Math.max(1, Math.min(15 * 60_000, allocation.expiresAtMs - now));
    await sessionRef.set({
      providerSessionId: allocation.providerSessionId,
      provider: "configured-adapter",
      state: "REQUESTED",
      numberMasking: allocation.numberMasking,
      tokenExpiresAtMs: now + ttlMs,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });

    return {
      sessionId: sessionRef.id,
      kind,
      clientToken: allocation.clientToken,
      expiresAtMs: now + ttlMs,
      numberMasking: allocation.numberMasking,
    };
  } catch (error) {
    await sessionRef.set({
      state: "FAILED",
      provider: "allocation-failed",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    functions.logger.error("Secure call provider allocation failed", {
      sessionId: sessionRef.id,
      callerUid,
      targetUid,
      kind,
    });
    throw new functions.https.HttpsError(
      "unavailable",
      "Secure calling is temporarily unavailable"
    );
  }
});

function callRequestRef(uidA: string, uidB: string) {
  return db.collection("callRequests").doc(pairId(uidA, uidB));
}

function callRequestUpdatedAtMs(data: FirebaseFirestore.DocumentData | undefined): number {
  const raw = data?.updatedAt;
  if (raw && typeof raw.toMillis === "function") return raw.toMillis();
  return Number(data?.updatedAtMs || 0);
}

async function notifyCallRequest(
  recipientUid: string,
  actorUid: string,
  pair: string,
  type: "CALL_REQUEST" | "CALL_ACCEPTED" | "CALL_DECLINED" | "CALL_CANCELLED",
  title: string,
  body: string,
  revision: number
): Promise<void> {
  await persistAndSendNotification({
    notificationId: `call_${type.toLowerCase()}_${pair}_${recipientUid}_${revision}`,
    userId: recipientUid,
    type,
    title,
    body,
    entityType: "call_request",
    entityId: pair,
    deepLink: notificationDeepLink("messages", functions.config().app_links?.host),
    action: "CHAT",
    pushType: type.toLowerCase(),
    preferenceKey: "messages",
    priority: "high",
    fromFirebaseUid: actorUid,
    localizedCopy: localizedNotificationCopy(type),
  });
}

/**
 * Creates/replaces one active call-coordination request for a mutual match.
 * This is provider-independent scheduling only; it never allocates a live call session.
 */
export const requestSecureCall = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const requesterUid = context.auth?.uid;
  if (!requesterUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = cleanUid(data?.targetUid);
  const kind = normalizeCallKind(data?.kind);
  const proposedAtMs = Number(data?.proposedAtMs);
  const now = Date.now();

  const eligibility = await relationshipEligibility(requesterUid, targetUid);
  if (!eligibility.eligible) {
    throw new functions.https.HttpsError("failed-precondition", eligibility.reason);
  }
  if (!validProposedCallTime(now, proposedAtMs)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Choose a call time between 15 minutes and 14 days from now"
    );
  }

  const pair = pairId(requesterUid, targetUid);
  const ref = callRequestRef(requesterUid, targetUid);
  await db.runTransaction(async (tx) => {
    const fresh = await relationshipEligibility(requesterUid, targetUid, tx);
    if (!fresh.eligible) throw new functions.https.HttpsError("failed-precondition", fresh.reason);
    const existing = await tx.get(ref);
    const current = normalizeCallRequestStatus(existing.data()?.status);
    const updatedAt = callRequestUpdatedAtMs(existing.data());
    if (existing.exists && !canReplaceCallRequest(current, updatedAt, now)) {
      throw new functions.https.HttpsError(
        "already-exists",
        "An active call request already exists for this match"
      );
    }

    const nextRevision = Number(existing.data()?.revision || 0) + 1;
    tx.set(ref, {
      revision: nextRevision,
      pairId: pair,
      requesterUid,
      targetUid,
      users: [requesterUid, targetUid].sort(),
      kind,
      proposedAtMs: Math.trunc(proposedAtMs),
      status: "PENDING",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAtMs: now,
    }, { merge: false });
    return nextRevision;
  });


  return {
    success: true,
    pairId: pair,
    status: "PENDING",
    kind,
    proposedAtMs: Math.trunc(proposedAtMs),
  };
});

/** Returns the current participant-visible call coordination state for this matched pair. */
export const getSecureCallRequest = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const viewerUid = context.auth?.uid;
  if (!viewerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = cleanUid(data?.targetUid);
  const eligibility = await relationshipEligibility(viewerUid, targetUid);
  if (!eligibility.eligible) {
    return { exists: false, reason: eligibility.reason };
  }

  const snap = await callRequestRef(viewerUid, targetUid).get();
  if (!snap.exists) return { exists: false, reason: "none" };
  const value = snap.data() || {};
  const users = Array.isArray(value.users) ? value.users : [];
  if (!users.includes(viewerUid) || !users.includes(targetUid) || users.length !== 2) {
    throw new functions.https.HttpsError("permission-denied", "Call request is unavailable");
  }
  return {
    exists: true,
    pairId: snap.id,
    requesterUid: String(value.requesterUid || ""),
    targetUid: String(value.targetUid || ""),
    kind: normalizeCallKind(value.kind),
    proposedAtMs: Number(value.proposedAtMs || 0),
    status: normalizeCallRequestStatus(value.status) || "CANCELLED",
  };
});

/** Target member accepts or declines a pending call request. */
export const respondSecureCallRequest = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const actorUid = context.auth?.uid;
  if (!actorUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const otherUid = cleanUid(data?.targetUid);
  const response = String(data?.response || "").trim().toUpperCase();
  if (response !== "ACCEPTED" && response !== "DECLINED") {
    throw new functions.https.HttpsError("invalid-argument", "Response must be ACCEPTED or DECLINED");
  }
  const eligibility = await relationshipEligibility(actorUid, otherUid);
  if (!eligibility.eligible) {
    throw new functions.https.HttpsError("failed-precondition", eligibility.reason);
  }

  const ref = callRequestRef(actorUid, otherUid);
  let requesterUid = "";
  await db.runTransaction(async (tx) => {
    const fresh = await relationshipEligibility(actorUid, otherUid, tx);
    if (!fresh.eligible) throw new functions.https.HttpsError("failed-precondition", fresh.reason);
    const snap = await tx.get(ref);
    if (!snap.exists) throw new functions.https.HttpsError("not-found", "Call request not found");
    const value = snap.data() || {};
    requesterUid = String(value.requesterUid || "");
    const targetUid = String(value.targetUid || "");
    const status = normalizeCallRequestStatus(value.status);
    if (requesterUid !== otherUid || targetUid !== actorUid || !Array.isArray(value.users) ||
      value.users.length !== 2 || !value.users.includes(actorUid) || !value.users.includes(otherUid)) {
      throw new functions.https.HttpsError("permission-denied", "Call request participants mismatch");
    }
    // Do not accept a proposed time that has already passed.
    if (response === "ACCEPTED" && (
      !Number.isFinite(Number(value.proposedAtMs)) ||
      Number(value.proposedAtMs) <= Date.now()
    )) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "The proposed call time has expired; request a new time"
      );
    }
    if (!canRespondToCallRequest(status, actorUid, targetUid)) {
      throw new functions.https.HttpsError("permission-denied", "Call request cannot be changed");
    }
    const nextRevision = Number(value.revision || 0) + 1;
    tx.update(ref, {
      revision: nextRevision,
      status: response,
      respondedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAtMs: Date.now(),
    });
    return nextRevision;
  });


  return { success: true, status: response };
});

/** Requesting member can withdraw a pending or accepted coordination request. */
export const cancelSecureCallRequest = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const actorUid = context.auth?.uid;
  if (!actorUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const otherUid = cleanUid(data?.targetUid);
  const eligibility = await relationshipEligibility(actorUid, otherUid);
  if (!eligibility.eligible) {
    throw new functions.https.HttpsError("failed-precondition", eligibility.reason);
  }

  const ref = callRequestRef(actorUid, otherUid);
  let targetUid = "";
  await db.runTransaction(async (tx) => {
    const fresh = await relationshipEligibility(actorUid, otherUid, tx);
    if (!fresh.eligible) throw new functions.https.HttpsError("failed-precondition", fresh.reason);
    const snap = await tx.get(ref);
    if (!snap.exists) throw new functions.https.HttpsError("not-found", "Call request not found");
    const value = snap.data() || {};
    targetUid = String(value.targetUid || "");
    const requesterUid = String(value.requesterUid || "");
    const status = normalizeCallRequestStatus(value.status);
    if (targetUid !== otherUid || requesterUid !== actorUid || !Array.isArray(value.users) ||
      value.users.length !== 2 || !value.users.includes(actorUid) || !value.users.includes(otherUid)) {
      throw new functions.https.HttpsError("permission-denied", "Call request participants mismatch");
    }
    if (!canCancelCallRequest(status, actorUid, requesterUid)) {
      throw new functions.https.HttpsError("permission-denied", "Call request cannot be cancelled");
    }
    const nextRevision = Number(value.revision || 0) + 1;
    tx.update(ref, {
      revision: nextRevision,
      status: "CANCELLED",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAtMs: Date.now(),
    });
    return nextRevision;
  });


  return { success: true, status: "CANCELLED" };
});

/** Retryable delivery; suppress stale events and re-check current relationship eligibility. */
export const onSecureCallRequestChanged = functions.runWith({ failurePolicy: true }).firestore
  .document("callRequests/{pairId}")
  .onWrite(async (change, context) => {
    if (!change.after.exists) return;
    const value = change.after.data() || {};
    const revision = Number(value.revision || 0);
    if (!Number.isSafeInteger(revision) || revision < 1 ||
        revision === Number(change.before.data()?.revision || 0)) return;
    const current = await change.after.ref.get();
    if (!current.exists || Number(current.data()?.revision) !== revision) return;
    const requester = String(value.requesterUid || "");
    const target = String(value.targetUid || "");
    if (!requester || !target || requester === target) return;
    const eligibility = await relationshipEligibility(requester, target);
    if (!eligibility.eligible) return;
    const status = normalizeCallRequestStatus(value.status);
    const types = {
      PENDING: "CALL_REQUEST", ACCEPTED: "CALL_ACCEPTED",
      DECLINED: "CALL_DECLINED", CANCELLED: "CALL_CANCELLED",
    } as const;
    if (!status) return;
    const requesterEvent = status === "PENDING" || status === "CANCELLED";
    await notifyCallRequest(
      requesterEvent ? target : requester,
      requesterEvent ? requester : target,
      context.params.pairId,
      types[status],
      status === "PENDING" ? "Call request" : `Call request ${status.toLowerCase()}`,
      "Open Matree to review the secure-call request.",
      revision
    );
  });
