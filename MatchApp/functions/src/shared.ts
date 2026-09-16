import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";

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

/** Generate a nationwide-neutral candidate. Uniqueness is guaranteed by registry reservation. */
export function generateMatrimonyId(): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Avoid ambiguous 0/O and 1/I.
  let id = "MAT-";
  for (let i = 0; i < 12; i++) {
    id += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return id;
}

/**
 * Reserve a Matrimony ID and attach it to the profile in one transaction.
 * The helper is idempotent for retried Firestore triggers and retries candidate collisions.
 */
export async function reserveMatrimonyId(uid: string): Promise<string> {
  const userRef = db.collection("users").doc(uid);

  for (let attempt = 0; attempt < 10; attempt += 1) {
    const candidate = generateMatrimonyId();
    const registryRef = db.collection("matrimonyIds").doc(candidate);
    try {
      return await db.runTransaction(async (tx) => {
        const [user, registry] = await Promise.all([tx.get(userRef), tx.get(registryRef)]);
        if (!user.exists) throw new Error("PROFILE_NOT_FOUND");

        const existing = typeof user.data()?.matrimonyId === "string"
          ? String(user.data()?.matrimonyId).trim()
          : "";
        if (existing) return existing;
        if (registry.exists) throw new Error("MATRIMONY_ID_COLLISION");

        tx.create(registryRef, {
          uid,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        tx.update(userRef, { matrimonyId: candidate });
        return candidate;
      });
    } catch (err) {
      if (err instanceof Error && err.message === "MATRIMONY_ID_COLLISION") continue;
      throw err;
    }
  }

  throw new Error("MATRIMONY_ID_EXHAUSTED");
}
