import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";

const PACKAGES = new Set(["Basic Verify", "Professional", "Premium 360°"]);
const PROFILE_ID = /^TLG-[A-Z0-9]{5}$/;

function pairId(uidA: string, uidB: string): string {
  return [uidA, uidB].sort().join("_");
}

function packageName(value: unknown): string {
  const name = typeof value === "string" ? value.trim() : "";
  if (!PACKAGES.has(name)) {
    throw new functions.https.HttpsError("invalid-argument", "Unsupported background check package");
  }
  return name;
}

function matrimonyId(value: unknown): string {
  const id = typeof value === "string" ? value.trim().toUpperCase() : "";
  if (!PROFILE_ID.test(id)) {
    throw new functions.https.HttpsError("invalid-argument", "Enter a valid Matrimony ID");
  }
  return id;
}

/**
 * Records a background-check service request against a real mutual match. This endpoint does not
 * claim that any check has been paid for, started or completed; an operations/provider workflow
 * must update the server-owned status and report fields later.
 */
export const requestBackgroundCheckByProfileId = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetProfileId = matrimonyId(data?.profileId);
  const selectedPackage = packageName(data?.plan);
  const result = await db.collection("users")
    .where("matrimonyId", "==", targetProfileId)
    .limit(1)
    .get();
  if (result.empty) throw new functions.https.HttpsError("not-found", "Profile not found");

  const targetUid = result.docs[0].id;
  if (targetUid === uid) {
    throw new functions.https.HttpsError("invalid-argument", "Choose another member's profile");
  }

  const [match, fromBlock, toBlock, fromPrivacy, toPrivacy] = await Promise.all([
    db.collection("matches").doc(pairId(uid, targetUid)).get(),
    db.collection("blocks").doc(uid).collection("blocked").doc(targetUid).get(),
    db.collection("blocks").doc(targetUid).collection("blocked").doc(uid).get(),
    db.collection("privacyRelations").doc(uid).collection("members").doc(targetUid).get(),
    db.collection("privacyRelations").doc(targetUid).collection("members").doc(uid).get(),
  ]);
  if (!match.exists) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Background-check requests are available only for mutual matches"
    );
  }
  if (fromBlock.exists || toBlock.exists ||
      fromPrivacy.data()?.profileHidden === true || toPrivacy.data()?.profileHidden === true) {
    throw new functions.https.HttpsError("permission-denied", "Request unavailable for this relationship");
  }

  const requestRef = db.collection("backgroundChecks").doc(`${uid}_${targetUid}`);
  const response = await db.runTransaction(async (tx) => {
    const existing = await tx.get(requestRef);
    if (existing.exists) {
      const status = String(existing.data()?.status || "submitted");
      if (status !== "cancelled") {
        return { requestId: requestRef.id, alreadyRequested: true, status };
      }
    }
    tx.set(requestRef, {
      requestedBy: uid,
      targetUid,
      targetProfileId,
      plan: selectedPackage,
      status: "submitted",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: false });
    return { requestId: requestRef.id, alreadyRequested: false, status: "submitted" };
  });

  return { success: true, ...response };
});
