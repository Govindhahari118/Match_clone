import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

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

export function generateMatrimonyId(): string {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  let id = "TLG-";
  for (let i = 0; i < 5; i++) {
    id += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return id;
}
