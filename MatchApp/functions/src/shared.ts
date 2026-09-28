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

export type OpsRole =
  | "support"
  | "moderator"
  | "kyc_reviewer"
  | "payment_ops"
  | "ops_admin";

export function resolveOpsRole(raw: unknown, allowed: OpsRole[]): OpsRole | undefined {
  const roles = Array.isArray(raw) ?
    raw.filter((value): value is string => typeof value === "string") :
    [];
  return allowed.find((role) => roles.includes(role));
}

export function requireOpsRole(
  context: functions.https.CallableContext,
  allowed: OpsRole[]
): { uid: string; role: OpsRole } {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const matched = resolveOpsRole(context.auth?.token?.roles, allowed);
  if (!matched) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Your operations role does not permit this action"
    );
  }
  return { uid, role: matched };
}

export type NotificationPreferenceKey = "interests" | "matches" | "messages" | "system";

type FcmDeviceToken = {
  deviceId: string;
  token: string;
  ref: admin.firestore.DocumentReference;
};

function validFcmToken(value: unknown): value is string {
  return typeof value === "string" && value.trim().length >= 20 && value.trim().length <= 4096;
}

async function migrateLegacyFcmToken(uid: string): Promise<FcmDeviceToken | undefined> {
  const privateRef = db.collection("userPrivate").doc(uid);
  const publicRef = db.collection("users").doc(uid);
  const [privateDoc, publicDoc] = await Promise.all([privateRef.get(), publicRef.get()]);
  const privateToken = privateDoc.data()?.fcmToken;
  const publicToken = publicDoc.data()?.fcmToken;
  const token = validFcmToken(privateToken) ? privateToken.trim() :
    validFcmToken(publicToken) ? publicToken.trim() : undefined;
  if (!token) return undefined;

  const deviceId = "legacy_" + crypto.createHash("sha256").update(token).digest("hex").slice(0, 32);
  const tokenRef = db.collection("fcmTokens").doc(uid).collection("devices").doc(deviceId);
  const ownerRef = db.collection("fcmDeviceOwners").doc(deviceId);

  await db.runTransaction(async (tx) => {
    const owner = await tx.get(ownerRef);
    const priorUid = owner.data()?.uid;
    if (typeof priorUid === "string" && priorUid && priorUid !== uid) {
      tx.delete(db.collection("fcmTokens").doc(priorUid).collection("devices").doc(deviceId));
    }
    tx.set(tokenRef, {
      uid,
      deviceId,
      token,
      platform: "android",
      migratedLegacy: true,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    tx.set(ownerRef, {
      uid,
      deviceId,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
  });

  const cleanup = db.batch();
  if (privateDoc.exists && privateDoc.data()?.fcmToken !== undefined) {
    cleanup.update(privateRef, {
      fcmToken: admin.firestore.FieldValue.delete(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  }
  if (publicDoc.exists && publicDoc.data()?.fcmToken !== undefined) {
    cleanup.update(publicRef, { fcmToken: admin.firestore.FieldValue.delete() });
  }
  await cleanup.commit();

  return { deviceId, token, ref: tokenRef };
}

/**
 * Private per-installation FCM registry. Existing single-token accounts are migrated lazily on the
 * first send, while every current Android install registers through the authenticated callable.
 */
export async function getFcmDeviceTokens(uid: string): Promise<FcmDeviceToken[]> {
  const devices = db.collection("fcmTokens").doc(uid).collection("devices");
  const snapshot = await devices.limit(500).get();
  const records = snapshot.docs.flatMap((doc) => {
    const token = doc.data()?.token;
    return validFcmToken(token) ? [{
      deviceId: doc.id,
      token: token.trim(),
      ref: doc.ref,
    }] : [];
  });
  if (records.length > 0) return records;

  const migrated = await migrateLegacyFcmToken(uid);
  return migrated ? [migrated] : [];
}

export async function notificationPreferenceEnabled(
  uid: string,
  key: NotificationPreferenceKey
): Promise<boolean> {
  const prefs = await db.collection("notificationPrefs").doc(uid).get();
  return prefs.data()?.[key] !== false;
}

/**
 * Deliver one minimal data notification to all currently registered installations for the account.
 * Invalid/expired tokens are pruned from the private registry without exposing tokens to clients.
 */
export async function sendDataToUserDevices(
  uid: string,
  data: Record<string, string>,
  android: admin.messaging.AndroidConfig,
  preferenceKey: NotificationPreferenceKey
): Promise<void> {
  if (!(await notificationPreferenceEnabled(uid, preferenceKey))) return;

  const records = await getFcmDeviceTokens(uid);
  if (records.length === 0) return;

  const unique = Array.from(new Map(records.map((record) => [record.token, record])).values());
  for (let offset = 0; offset < unique.length; offset += 500) {
    const chunk = unique.slice(offset, offset + 500);
    const response = await messaging.sendEachForMulticast({
      tokens: chunk.map((record) => record.token),
      data,
      android,
    });

    const cleanup = db.batch();
    let cleanupCount = 0;
    response.responses.forEach((result, index) => {
      const code = result.error?.code;
      if (code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-registration-token") {
        cleanup.delete(chunk[index].ref);
        cleanupCount += 1;
      }
    });
    if (cleanupCount > 0) await cleanup.commit();
  }
}


export type PersistedNotificationInput = {
  notificationId: string;
  userId: string;
  type: string;
  title: string;
  body: string;
  entityType: string;
  entityId: string;
  deepLink: string;
  pushType: string;
  preferenceKey: NotificationPreferenceKey;
  priority?: "high" | "normal";
  fromFirebaseUid?: string;
};

/**
 * Durable notification contract: commit the user-visible event first, then attempt data-only push
 * delivery. Trigger retries reuse the same event id; a repeated push still carries that id so the
 * client cache is idempotent.
 */
export async function persistAndSendNotification(
  input: PersistedNotificationInput
): Promise<void> {
  const ref = db.collection("notifications").doc(input.notificationId);
  await db.runTransaction(async (tx) => {
    const existing = await tx.get(ref);
    if (!existing.exists) {
      tx.create(ref, {
        userId: input.userId,
        type: input.type,
        title: input.title,
        body: input.body,
        entityType: input.entityType,
        entityId: input.entityId,
        deepLink: input.deepLink,
        ...(input.fromFirebaseUid ? { fromFirebaseUid: input.fromFirebaseUid } : {}),
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        readAt: null,
      });
    }
  });

  await sendDataToUserDevices(
    input.userId,
    {
      type: input.pushType,
      title: input.title,
      body: input.body,
      recipient_uid: input.userId,
      notification_id: input.notificationId,
      entity_type: input.entityType,
      entity_id: input.entityId,
      deep_link: input.deepLink,
      ...(input.fromFirebaseUid ? {
        user_id: input.fromFirebaseUid,
        peer_uid: input.fromFirebaseUid,
      } : {}),
    },
    { priority: input.priority ?? "high" },
    input.preferenceKey
  );

  await ref.set({
    pushLastAttemptAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
}

const MATRIMONY_ID_ATTEMPTS = 12;

function newMatrimonyIdCandidate(): string {
  return `MAT-${crypto.randomBytes(8).toString("hex").toUpperCase()}`;
}

/**
 * Reserve one nationwide-neutral public Matrimony ID for a Firebase UID.
 * The UID assignment makes trigger retries idempotent; the global registry makes
 * concurrent allocation collision-safe.
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

/** Release the server-only uniqueness reservation during account deletion. */
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
