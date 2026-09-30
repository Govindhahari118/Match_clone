import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import { db, requireAppCheck, requireOpsRole } from "./shared";
import { computeTrustPolicy, normalizeRiskLevel, TrustRiskLevel } from "./trustPolicy";
import {
  PROFILE_MUTATION_REVIEW_THRESHOLD_PER_DAY,
  profileMutationFieldsChanged,
} from "./riskSignalPolicy";

function millis(value: unknown): number {
  if (value instanceof admin.firestore.Timestamp) return value.toMillis();
  const parsed = Number(value || 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

function safeTargetUid(value: unknown, fallback: string): string {
  const uid = typeof value === "string" ? value.trim() : fallback;
  if (!uid || uid.length > 128 || uid.includes("/")) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid profile");
  }
  return uid;
}

async function peerCanReadProfile(viewerUid: string, targetUid: string, target: FirebaseFirestore.DocumentData): Promise<boolean> {
  if (viewerUid === targetUid) return true;
  const [outgoingBlock, incomingBlock, relation, explicitInterest] = await Promise.all([
    db.collection("blocks").doc(viewerUid).collection("blocked").doc(targetUid).get(),
    db.collection("blocks").doc(targetUid).collection("blocked").doc(viewerUid).get(),
    db.collection("privacyRelations").doc(targetUid).collection("members").doc(viewerUid).get(),
    db.collection("interests").doc(`${targetUid}_${viewerUid}`).get(),
  ]);
  if (outgoingBlock.exists || incomingBlock.exists) return false;
  if (relation.data()?.profileHidden === true) return false;

  const accountStatus = String(target.accountStatus || "ACTIVE").toUpperCase();
  if (accountStatus !== "ACTIVE") return false;
  if (target.matrimonyPaused === true || target.deletionPending === true) return false;
  if (target.stealthMode === true && !explicitInterest.exists) return false;
  return true;
}

async function buildTrustSummary(targetUid: string) {
  const [profileSnap, riskSnap] = await Promise.all([
    db.collection("users").doc(targetUid).get(),
    db.collection("riskAssessments").doc(targetUid).get(),
  ]);
  if (!profileSnap.exists) {
    throw new functions.https.HttpsError("not-found", "Profile not found");
  }
  const profile = profileSnap.data() || {};
  const risk = riskSnap.data() || {};

  let authUser: admin.auth.UserRecord;
  try {
    authUser = await admin.auth().getUser(targetUid);
  } catch {
    throw new functions.https.HttpsError("not-found", "Account not found");
  }

  const createdAt = millis(profile.createdAt) || Date.parse(authUser.metadata.creationTime || "");
  const accountAgeDays = Number.isFinite(createdAt) && createdAt > 0
    ? Math.max(0, Math.floor((Date.now() - createdAt) / 86_400_000))
    : 0;
  const riskLevel = normalizeRiskLevel(risk.level);
  const result = computeTrustPolicy({
    phoneVerified: Boolean(authUser.phoneNumber),
    emailVerified: authUser.emailVerified === true,
    identityLevel: Number(profile.verificationLevel || 0),
    profileCompleteness: Number(profile.profileCompleteness || 0),
    hasPhoto: typeof profile.photoUrl === "string" && profile.photoUrl.trim().length > 0,
    accountAgeDays,
    reviewedSafetyStanding: risk.standing === "CLEAR",
    riskLevel,
  });

  return { profile, result };
}

/**
 * Server-authoritative public trust summary. The member app never supplies score inputs and the
 * response exposes positive evidence only; moderation/fraud signals remain private.
 */
export const getTrustSummary = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const viewerUid = context.auth?.uid;
  if (!viewerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = safeTargetUid(data?.targetUid, viewerUid);
  const { profile, result } = await buildTrustSummary(targetUid);
  if (!(await peerCanReadProfile(viewerUid, targetUid, profile))) {
    throw new functions.https.HttpsError("permission-denied", "Profile is not available");
  }

  return {
    targetUid,
    score: result.score,
    tier: result.tier,
    factors: result.positiveFactors,
    formulaVersion: "TRUST_2026_09_29_V1",
  };
});

const RISK_LEVELS = new Set<TrustRiskLevel>(["LOW", "MEDIUM", "HIGH", "CRITICAL"]);

function safeRiskLevel(value: unknown): TrustRiskLevel {
  const level = typeof value === "string" ? value.trim().toUpperCase() : "";
  if (!RISK_LEVELS.has(level as TrustRiskLevel)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid risk level");
  }
  return level as TrustRiskLevel;
}

function safeReason(value: unknown): string {
  const reason = typeof value === "string" ? value.trim() : "";
  if (reason.length < 3 || reason.length > 500) {
    throw new functions.https.HttpsError("invalid-argument", "A 3-500 character review reason is required");
  }
  return reason;
}

/**
 * Human-reviewed risk state. Reports/signals are evidence for moderation, never automatic proof of
 * wrongdoing. The public trust summary receives only the resulting level, not the underlying signal
 * formula or reason.
 */
export const updateRiskAssessment = functions.https.onCall(async (data, context) => {
  const actor = requireOpsRole(context, ["moderator", "ops_admin"]);
  const targetUid = safeTargetUid(data?.targetUid, "");
  const level = safeRiskLevel(data?.level);
  const reason = safeReason(data?.reason);
  const standing = data?.standing === "CLEAR" && level === "LOW" ? "CLEAR" : "UNREVIEWED";

  const profileRef = db.collection("users").doc(targetUid);
  const riskRef = db.collection("riskAssessments").doc(targetUid);
  const auditRef = db.collection("opsAuditLog").doc();

  await db.runTransaction(async (tx) => {
    const [profile, previous] = await Promise.all([tx.get(profileRef), tx.get(riskRef)]);
    if (!profile.exists) throw new functions.https.HttpsError("not-found", "Profile not found");

    tx.set(riskRef, {
      uid: targetUid,
      level,
      standing,
      reviewedBy: actor.uid,
      reviewedRole: actor.role,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    tx.create(auditRef, {
      actorUid: actor.uid,
      actorRole: actor.role,
      action: "RISK_ASSESSMENT_UPDATED",
      targetCollection: "riskAssessments",
      targetId: targetUid,
      before: {
        level: String(previous.data()?.level || "LOW"),
        standing: String(previous.data()?.standing || "UNREVIEWED"),
      },
      after: { level, standing },
      reason,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  return { success: true, targetUid, level, standing };
});

/**
 * A report contributes a private moderation signal only. It does not itself change Trust Score or
 * suspend the member, preventing malicious mass-reporting from becoming an automatic penalty.
 */
export const onProfileReportRiskSignal = functions.firestore
  .document("profileReports/{reportId}")
  .onCreate(async (snap) => {
    const targetUid = typeof snap.data()?.targetUid === "string" ? snap.data().targetUid.trim() : "";
    if (!targetUid) return;

    await db.collection("riskSignals").doc(targetUid).set({
      reportSignalCount: admin.firestore.FieldValue.increment(1),
      lastReportSignalAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
  });


// Interest-volume and message-safety signals are owned by interests.ts and messageSafety.ts.
// Keeping a second Firestore-triggered counter here would double-count the same authoritative
// action. riskDayKey remains for independent profile-mutation review signals.

/**
 * Counts rapid edits to member-controlled profile fields as a private review signal. Server-owned
 * entitlement/moderation/completeness writes are excluded by the field policy and no enforcement is
 * automatic.
 */
export const onRapidProfileMutationRiskSignal = functions.firestore
  .document("users/{uid}")
  .onUpdate(async (change, context) => {
    const before = change.before.data() || {};
    const after = change.after.data() || {};
    if (!profileMutationFieldsChanged(before, after)) return;

    const uid = context.params.uid;
    const day = riskDayKey();
    const activityRef = db.collection("riskActivity").doc(uid).collection("days").doc(day);
    const signalRef = db.collection("riskSignals").doc(uid);

    await db.runTransaction(async (tx) => {
      const activity = await tx.get(activityRef);
      const current = Number(activity.data()?.profileMutationCount || 0);
      const next = current + 1;
      const alreadyFlagged = activity.data()?.profileMutationVolumeFlagged === true;

      tx.set(activityRef, {
        day,
        profileMutationCount: next,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        ...(next >= PROFILE_MUTATION_REVIEW_THRESHOLD_PER_DAY
          ? { profileMutationVolumeFlagged: true }
          : {}),
      }, { merge: true });

      if (next >= PROFILE_MUTATION_REVIEW_THRESHOLD_PER_DAY && !alreadyFlagged) {
        tx.set(signalRef, {
          rapidProfileMutationDayCount: admin.firestore.FieldValue.increment(1),
          lastRapidProfileMutationSignalAt: admin.firestore.FieldValue.serverTimestamp(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, { merge: true });
      }
    });
  });
