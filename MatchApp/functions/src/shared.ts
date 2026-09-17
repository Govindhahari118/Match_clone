import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import * as crypto from "crypto";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

export const db = admin.firestore();
export const messaging = admin.messaging();

/**
 * Stage App Check safely: set `security.enforce_app_check=true` in the production
 * Functions config after Play Integrity metrics show legitimate release traffic.
 */
export function requireAppCheck(context: functions.https.CallableContext): void {
  const enforce = functions.config().security?.enforce_app_check === "true";
  if (enforce && !context.app) {
    throw new functions.https.HttpsError("failed-precondition", "Valid App Check token required");
  }
}

/** Read an FCM token from the private document and migrate any legacy public token. */
export async function getFcmToken(uid: string): Promise<string | undefined> {
  const privateRef = db.collection("userPrivate").doc(uid);
  const privateDoc = await privateRef.get();
  const token = privateDoc.data()?.fcmToken as string | undefined;
  if (token) return token;

  const publicRef = db.collection("users").doc(uid);
  const publicDoc = await publicRef.get();
  const legacy = publicDoc.data()?.fcmToken as string | undefined;
  if (!legacy) return undefined;

  const batch = db.batch();
  batch.set(privateRef, {
    fcmToken: legacy,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
  batch.update(publicRef, { fcmToken: admin.firestore.FieldValue.delete() });
  await batch.commit();
  return legacy;
}

const MATRIMONY_ID_ATTEMPTS = 12;

function newMatrimonyIdCandidate(): string {
  // 64 random bits yields a compact nationwide-neutral public identifier while Firestore
  // reservation below provides authoritative uniqueness even if a random collision occurs.
  return `MAT-${crypto.randomBytes(8).toString("hex").toUpperCase()}`;
}

/**
 * Reserve a stable public Matrimony ID for one Firebase UID.
 *
 * `matrimonyIdAssignments/{uid}` makes trigger retries idempotent; `matrimonyIds/{id}` is the
 * global uniqueness registry. Transactions read both documents before writing, so concurrent
 * account creation cannot assign the same public ID or create two assignments for one UID.
 */
export async function reserveMatrimonyId(uid: string): Promise<string> {
  if (!uid || uid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid account identity");
  }

  const assignmentRef = db.collection("matrimonyIdAssignments").doc(uid);
  for (let attempt = 0; attempt < MATRIMONY_ID_ATTEMPTS; attempt += 1) {
    const candidate = newMatrimonyIdCandidate();
    const registryRef = db.collection("matrimonyIds").doc(candidate);

    const reserved = await db.runTransaction(async (tx) => {
      const [assignment, registry] = await Promise.all([
        tx.get(assignmentRef),
        tx.get(registryRef),
      ]);

      const existing = assignment.data()?.matrimonyId;
      if (assignment.exists && typeof existing === "string" && existing.startsWith("MAT-")) {
        return existing;
      }
      if (registry.exists) return null;

      const createdAt = admin.firestore.FieldValue.serverTimestamp();
      tx.set(registryRef, { uid, createdAt }, { merge: false });
      tx.set(assignmentRef, { uid, matrimonyId: candidate, createdAt }, { merge: false });
      return candidate;
    });

    if (reserved) return reserved;
  }

  functions.logger.error("Unable to reserve Matrimony ID after collision retries", { uid });
  throw new functions.https.HttpsError("resource-exhausted", "Unable to allocate profile ID");
}

/** Release the server-only uniqueness reservation as part of restartable account deletion. */
export async function releaseMatrimonyId(uid: string): Promise<void> {
  const assignmentRef = db.collection("matrimonyIdAssignments").doc(uid);
  await db.runTransaction(async (tx) => {
    const assignment = await tx.get(assignmentRef);
    if (!assignment.exists) return;

    const matrimonyId = assignment.data()?.matrimonyId;
    if (typeof matrimonyId === "string" && matrimonyId.startsWith("MAT-")) {
      const registryRef = db.collection("matrimonyIds").doc(matrimonyId);
      const registry = await tx.get(registryRef);
      if (registry.exists && registry.data()?.uid === uid) {
        tx.delete(registryRef);
      }
    }
    tx.delete(assignmentRef);
  });
}
