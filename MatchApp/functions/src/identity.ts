import * as functions from "firebase-functions/v1";
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
