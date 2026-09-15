import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";

const MIN_HEARTBEAT_INTERVAL_MS = 60_000;

/**
 * Records an authenticated member heartbeat used for online/recent activity indicators.
 * The timestamp is server-owned so clients cannot forge another member's activity state.
 */
export const touchPresence = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  }

  const userRef = db.collection("users").doc(uid);
  const snap = await userRef.get();
  if (!snap.exists) {
    throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");
  }

  const now = Date.now();
  const previous = Number(snap.data()?.lastActiveAt || 0);
  if (Number.isFinite(previous) && now - previous < MIN_HEARTBEAT_INTERVAL_MS) {
    return { success: true, lastActiveAt: previous, throttled: true };
  }

  await userRef.update({ lastActiveAt: now });
  return { success: true, lastActiveAt: now, throttled: false };
});
