import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import { db, requireAppCheck } from "./shared";

const USERNAME_PATTERN = /^[a-z0-9][a-z0-9._]{2,29}$/;
const RESERVED = new Set([
  "admin", "administrator", "support", "help", "match", "matrimony", "official",
  "security", "moderator", "system", "null", "undefined",
]);

function normalizeUsername(value: unknown): string {
  if (typeof value !== "string") return "";
  return value.trim().toLowerCase().replace(/^@+/, "");
}

export const setUsername = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const username = normalizeUsername(data?.username);
  if (!USERNAME_PATTERN.test(username) || username.includes("..") || RESERVED.has(username)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Username must be 3-30 characters and use only letters, numbers, dot or underscore."
    );
  }

  const userRef = db.collection("users").doc(uid);
  const desiredRef = db.collection("usernames").doc(username);

  await db.runTransaction(async (tx) => {
    const [userSnap, desiredSnap] = await Promise.all([tx.get(userRef), tx.get(desiredRef)]);
    if (!userSnap.exists) {
      throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");
    }
    if (desiredSnap.exists && desiredSnap.data()?.uid !== uid) {
      throw new functions.https.HttpsError("already-exists", "That username is already taken");
    }

    const previous = normalizeUsername(userSnap.data()?.usernameNormalized || userSnap.data()?.username);
    if (previous && previous !== username) {
      const previousRef = db.collection("usernames").doc(previous);
      const previousSnap = await tx.get(previousRef);
      if (previousSnap.exists && previousSnap.data()?.uid === uid) tx.delete(previousRef);
    }

    tx.set(desiredRef, { uid, username, updatedAt: Date.now() });
    tx.update(userRef, { username, usernameNormalized: username, updatedAt: Date.now() });
  });

  return { success: true, username };
});


function timestampMillis(value: unknown): number | null {
  return value instanceof admin.firestore.Timestamp ? value.toMillis() : null;
}

/** Returns privacy-safe installation records for the signed-in account; FCM tokens never leave the backend. */
export const listMyDevices = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const snapshot = await db.collection("fcmTokens").doc(uid).collection("devices")
    .limit(100)
    .get();

  return {
    devices: snapshot.docs.map((doc) => {
      const value = doc.data();
      return {
        deviceId: doc.id,
        platform: String(value.platform || "unknown"),
        appVersion: String(value.appVersion || ""),
        updatedAtMillis: timestampMillis(value.updatedAt),
      };
    }),
  };
});

/**
 * Invalidates Firebase refresh tokens and removes every registered push installation for this
 * account. The calling app must sign out locally after this callable succeeds.
 */
export const revokeAllSessions = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const deviceSnapshot = await db.collection("fcmTokens").doc(uid).collection("devices")
    .limit(500)
    .get();
  const ownerSnapshots = await Promise.all(deviceSnapshot.docs.map((doc) =>
    db.collection("fcmDeviceOwners").doc(doc.id).get()
  ));

  const batch = db.batch();
  deviceSnapshot.docs.forEach((doc, index) => {
    batch.delete(doc.ref);
    const owner = ownerSnapshots[index];
    if (owner.exists && owner.data()?.uid === uid) batch.delete(owner.ref);
  });

  const eventRef = db.collection("securityEvents").doc();
  batch.create(eventRef, {
    uid,
    type: "ALL_SESSIONS_REVOKED",
    revokedDeviceCount: deviceSnapshot.size,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  if (deviceSnapshot.size > 0 || eventRef) await batch.commit();
  await admin.auth().revokeRefreshTokens(uid);

  return { success: true, revokedDeviceCount: deviceSnapshot.size };
});
