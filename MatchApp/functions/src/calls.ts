import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";
import { productionFeatureEnabled } from "./featureFlagPolicy";
import { accountIsActive } from "./accountStatusPolicy";
import { evaluateCallEligibility } from "./callPolicy";
import {
  callInitiationAllowed,
  normalizeCallKind,
} from "./callSessionPolicy";
import { communicationProvider } from "./communicationProvider";

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

async function relationshipEligibility(callerUid: string, targetUid: string) {
  const [
    caller,
    target,
    match,
    callerBlock,
    targetBlock,
    callerPrivacy,
    targetPrivacy,
  ] = await Promise.all([
    db.collection("users").doc(callerUid).get(),
    db.collection("users").doc(targetUid).get(),
    db.collection("matches").doc(pairId(callerUid, targetUid)).get(),
    db.collection("blocks").doc(callerUid).collection("blocked").doc(targetUid).get(),
    db.collection("blocks").doc(targetUid).collection("blocked").doc(callerUid).get(),
    db.collection("privacyRelations").doc(callerUid).collection("members").doc(targetUid).get(),
    db.collection("privacyRelations").doc(targetUid).collection("members").doc(callerUid).get(),
  ]);

  return evaluateCallEligibility({
    callerUid,
    targetUid,
    callerActive: caller.exists && accountIsActive(caller.data()?.accountStatus),
    targetActive: target.exists && accountIsActive(target.data()?.accountStatus),
    mutualMatch: match.exists &&
      Array.isArray(match.data()?.users) &&
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
