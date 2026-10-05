import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {
  notificationActionFor,
  notificationDeepLink,
} from "./notificationLinkPolicy";
import {
  db,
  persistAndSendNotification,
  requireAppCheck,
} from "./shared";
import { deviceAccountSwitchIsReviewSignal } from "./riskSignalPolicy";
import { accountIsActive } from "./accountStatusPolicy";

type NotificationPayload = {
  userId: string;
  type: string;
  title: string;
  body: string;
  entityType: string;
  entityId: string;
  deepLink: string;
  fromFirebaseUid?: string;
};

function requireDeviceId(value: unknown): string {
  const deviceId = typeof value === "string" ? value.trim() : "";
  if (!/^[A-Za-z0-9_-]{16,128}$/.test(deviceId)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid device identity");
  }
  return deviceId;
}

function requireFcmToken(value: unknown): string {
  const token = typeof value === "string" ? value.trim() : "";
  if (token.length < 20 || token.length > 4096) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid notification token");
  }
  return token;
}

/**
 * Register exactly this authenticated app installation. A device identity can belong to only one
 * signed-in account at a time; registering after an account switch atomically removes the prior
 * account's device record.
 */
export const registerFcmDevice = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const deviceId = requireDeviceId(data?.deviceId);
  const token = requireFcmToken(data?.token);
  const appVersion = typeof data?.appVersion === "string" ?
    data.appVersion.trim().slice(0, 64) : "";

  const deviceRef = db.collection("fcmTokens").doc(uid).collection("devices").doc(deviceId);
  const ownerRef = db.collection("fcmDeviceOwners").doc(deviceId);

  await db.runTransaction(async (tx) => {
    const owner = await tx.get(ownerRef);
    const priorUid = owner.data()?.uid;
    if (deviceAccountSwitchIsReviewSignal(priorUid, uid)) {
      tx.delete(db.collection("fcmTokens").doc(String(priorUid)).collection("devices").doc(deviceId));
      tx.set(db.collection("riskSignals").doc(uid), {
        sharedDeviceAccountSwitchCount: admin.firestore.FieldValue.increment(1),
        lastSharedDeviceSignalAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
    }

    tx.set(deviceRef, {
      uid,
      deviceId,
      token,
      platform: "android",
      appVersion,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    tx.set(ownerRef, {
      uid,
      deviceId,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
  });

  return { registered: true };
});

/** Revoke only the current authenticated installation, not the user's other signed-in devices. */
export const revokeFcmDevice = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const deviceId = requireDeviceId(data?.deviceId);
  const deviceRef = db.collection("fcmTokens").doc(uid).collection("devices").doc(deviceId);
  const ownerRef = db.collection("fcmDeviceOwners").doc(deviceId);

  await db.runTransaction(async (tx) => {
    const owner = await tx.get(ownerRef);
    tx.delete(deviceRef);
    if (owner.data()?.uid === uid) tx.delete(ownerRef);
  });

  return { revoked: true };
});

function relationshipMatchId(uidA: string, uidB: string): string {
  return [uidA, uidB].sort().join("_");
}

function preferenceFor(type: string): "interests" | "matches" | "messages" | "system" {
  if (type === "INTEREST") return "interests";
  if (type === "MATCH") return "matches";
  if (type === "MESSAGE") return "messages";
  return "system";
}

async function relationshipEventStillVisible(
  eventRef: FirebaseFirestore.DocumentReference,
  uidA: string,
  uidB: string
): Promise<boolean> {
  const [event, blockAB, blockBA, privacyAB, privacyBA] = await Promise.all([
    eventRef.get(),
    db.collection("blocks").doc(uidA).collection("blocked").doc(uidB).get(),
    db.collection("blocks").doc(uidB).collection("blocked").doc(uidA).get(),
    db.collection("privacyRelations").doc(uidA).collection("members").doc(uidB).get(),
    db.collection("privacyRelations").doc(uidB).collection("members").doc(uidA).get(),
  ]);
  return event.exists &&
    !blockAB.exists &&
    !blockBA.exists &&
    privacyAB.data()?.profileHidden !== true &&
    privacyBA.data()?.profileHidden !== true;
}


async function contactRequestNotificationStillAllowed(
  requestRef: FirebaseFirestore.DocumentReference,
  requesterUid: string,
  targetUid: string
): Promise<boolean> {
  const [
    request,
    requester,
    target,
    match,
    blockAB,
    blockBA,
    privacyAB,
    privacyBA,
    targetSettings,
  ] = await Promise.all([
    requestRef.get(),
    db.collection("users").doc(requesterUid).get(),
    db.collection("users").doc(targetUid).get(),
    db.collection("matches").doc(relationshipMatchId(requesterUid, targetUid)).get(),
    db.collection("blocks").doc(requesterUid).collection("blocked").doc(targetUid).get(),
    db.collection("blocks").doc(targetUid).collection("blocked").doc(requesterUid).get(),
    db.collection("privacyRelations").doc(requesterUid).collection("members").doc(targetUid).get(),
    db.collection("privacyRelations").doc(targetUid).collection("members").doc(requesterUid).get(),
    db.collection("privacySettings").doc(targetUid).get(),
  ]);
  const matchUsers = match.data()?.users;
  return request.exists &&
    request.data()?.status === "PENDING" &&
    requester.exists &&
    target.exists &&
    accountIsActive(requester.data()?.accountStatus) &&
    accountIsActive(target.data()?.accountStatus) &&
    match.exists &&
    Array.isArray(matchUsers) &&
    matchUsers.length === 2 &&
    matchUsers.includes(requesterUid) &&
    matchUsers.includes(targetUid) &&
    !blockAB.exists &&
    !blockBA.exists &&
    privacyAB.data()?.profileHidden !== true &&
    privacyBA.data()?.profileHidden !== true &&
    privacyBA.data()?.contactHidden !== true &&
    String(targetSettings.data()?.contactVisibility || "selected_people") !== "nobody";
}

async function chatNotificationStillAllowed(
  threadId: string,
  fromUid: string,
  toUid: string
): Promise<boolean> {
  const [
    thread,
    match,
    sender,
    recipient,
    blockAB,
    blockBA,
    privacyAB,
    privacyBA,
    recipientChatPreference,
  ] = await Promise.all([
    db.collection("chats").doc(threadId).get(),
    db.collection("matches").doc(relationshipMatchId(fromUid, toUid)).get(),
    db.collection("users").doc(fromUid).get(),
    db.collection("users").doc(toUid).get(),
    db.collection("blocks").doc(fromUid).collection("blocked").doc(toUid).get(),
    db.collection("blocks").doc(toUid).collection("blocked").doc(fromUid).get(),
    db.collection("privacyRelations").doc(fromUid).collection("members").doc(toUid).get(),
    db.collection("privacyRelations").doc(toUid).collection("members").doc(fromUid).get(),
    db.collection("chatPreferences").doc(toUid).collection("threads").doc(threadId).get(),
  ]);
  const participants = thread.data()?.participantUids;
  const matchUsers = match.data()?.users;
  return thread.exists &&
    match.exists &&
    sender.exists &&
    recipient.exists &&
    accountIsActive(sender.data()?.accountStatus) &&
    accountIsActive(recipient.data()?.accountStatus) &&
    Array.isArray(participants) &&
    participants.length === 2 &&
    participants.includes(fromUid) &&
    participants.includes(toUid) &&
    Array.isArray(matchUsers) &&
    matchUsers.length === 2 &&
    matchUsers.includes(fromUid) &&
    matchUsers.includes(toUid) &&
    !blockAB.exists &&
    !blockBA.exists &&
    privacyAB.data()?.profileHidden !== true &&
    privacyBA.data()?.profileHidden !== true &&
    recipientChatPreference.data()?.muted !== true;
}

async function deliverPersistedNotification(
  notificationId: string,
  payload: NotificationPayload
): Promise<void> {
  const pushType = payload.type === "INTEREST" ? "interest_received" :
    payload.type === "MATCH" ? "mutual_match" :
      payload.type === "MESSAGE" ? "message" : payload.type.toLowerCase();

  await persistAndSendNotification({
    notificationId,
    userId: payload.userId,
    type: payload.type,
    title: payload.title,
    body: payload.body,
    entityType: payload.entityType,
    entityId: payload.entityId,
    deepLink: payload.deepLink,
    action: notificationActionFor(payload.type),
    pushType,
    preferenceKey: preferenceFor(payload.type),
    priority: "high",
    fromFirebaseUid: payload.fromFirebaseUid,
  });
}

export const onInterestCreated = functions.firestore
  .document("interests/{interestId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();
    const fromUid = data.fromUid as string;
    const toUid = data.toUid as string;
    if (!fromUid || !toUid) return;
    if (!await relationshipEventStillVisible(snap.ref, fromUid, toUid)) return;

    await deliverPersistedNotification(
      `interest_${context.params.interestId}_${toUid}`,
      {
        userId: toUid,
        type: "INTEREST",
        title: "New interest",
        body: "Someone is interested in your profile. Open the app to view it.",
        entityType: "profile",
        entityId: fromUid,
        deepLink: notificationDeepLink("interests", functions.config().app_links?.host),
        fromFirebaseUid: fromUid,
      }
    );
  });

export const onMatchCreated = functions.firestore
  .document("matches/{matchId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();
    const users = data.users as string[];
    if (!users || users.length !== 2) return;

    const [uid1, uid2] = users;
    if (!await relationshipEventStillVisible(snap.ref, uid1, uid2)) return;
    await Promise.allSettled([
      deliverPersistedNotification(
        `match_${context.params.matchId}_${uid1}`,
        {
          userId: uid1,
          type: "MATCH",
          title: "New mutual match",
          body: "You have a new mutual match. Open the app to view the profile.",
          entityType: "profile",
          entityId: uid2,
          deepLink: notificationDeepLink("matches", functions.config().app_links?.host),
          fromFirebaseUid: uid2,
        }
      ),
      deliverPersistedNotification(
        `match_${context.params.matchId}_${uid2}`,
        {
          userId: uid2,
          type: "MATCH",
          title: "New mutual match",
          body: "You have a new mutual match. Open the app to view the profile.",
          entityType: "profile",
          entityId: uid1,
          deepLink: notificationDeepLink("matches", functions.config().app_links?.host),
          fromFirebaseUid: uid1,
        }
      ),
    ]);
  });


export const onContactRequestPending = functions.firestore
  .document("contactRequests/{requestId}")
  .onWrite(async (change, context) => {
    const beforeStatus = change.before.exists ? String(change.before.data()?.status || "") : "";
    if (!change.after.exists) return;
    const data = change.after.data();
    if (!data) return;
    const status = String(data.status || "");
    if (status !== "PENDING" || beforeStatus === "PENDING") return;

    const requesterUid = String(data.requesterUid || "");
    const targetUid = String(data.targetUid || "");
    if (!requesterUid || !targetUid || requesterUid === targetUid) return;
    if (!await contactRequestNotificationStillAllowed(
      change.after.ref,
      requesterUid,
      targetUid
    )) return;

    const updatedAt = data.updatedAt instanceof admin.firestore.Timestamp ?
      data.updatedAt.toMillis() : 0;
    await deliverPersistedNotification(
      `contact_request_${context.params.requestId}_${targetUid}_${updatedAt}`,
      {
        userId: targetUid,
        type: "CONTACT_REQUEST",
        title: "Contact access request",
        body: "A mutual match requested permission to reveal your contact. Open Privacy & visibility to respond.",
        entityType: "profile",
        entityId: requesterUid,
        deepLink: notificationDeepLink("notifications", functions.config().app_links?.host),
        fromFirebaseUid: requesterUid,
      }
    );
  });


async function photoRequestNotificationStillAllowed(
  requestRef: FirebaseFirestore.DocumentReference,
  requesterUid: string,
  targetUid: string
): Promise<boolean> {
  const [request, requester, target, blockAB, blockBA, privacyAB, privacyBA] = await Promise.all([
    requestRef.get(),
    db.collection("users").doc(requesterUid).get(),
    db.collection("users").doc(targetUid).get(),
    db.collection("blocks").doc(requesterUid).collection("blocked").doc(targetUid).get(),
    db.collection("blocks").doc(targetUid).collection("blocked").doc(requesterUid).get(),
    db.collection("privacyRelations").doc(requesterUid).collection("members").doc(targetUid).get(),
    db.collection("privacyRelations").doc(targetUid).collection("members").doc(requesterUid).get(),
  ]);
  return request.exists &&
    requester.exists &&
    target.exists &&
    accountIsActive(requester.data()?.accountStatus) &&
    accountIsActive(target.data()?.accountStatus) &&
    !String(target.data()?.photoUrl || "").trim() &&
    !blockAB.exists &&
    !blockBA.exists &&
    privacyAB.data()?.profileHidden !== true &&
    privacyBA.data()?.profileHidden !== true;
}

export const onProfilePhotoRequested = functions.firestore
  .document("photoRequests/{requestId}")
  .onWrite(async (change, context) => {
    if (!change.after.exists) return;
    const after = change.after.data();
    const beforeSequence = change.before.exists ? Number(change.before.data()?.requestSequence || 0) : 0;
    const sequence = Number(after?.requestSequence || 0);
    if (!sequence || sequence === beforeSequence) return;

    const requesterUid = String(after?.requesterUid || "");
    const targetUid = String(after?.targetUid || "");
    if (!requesterUid || !targetUid || requesterUid === targetUid) return;
    if (!await photoRequestNotificationStillAllowed(change.after.ref, requesterUid, targetUid)) return;

    await persistAndSendNotification({
      notificationId: `photo_request_${context.params.requestId}_${sequence}`,
      userId: targetUid,
      type: "PHOTO_REQUEST",
      title: "Photo request",
      body: "Someone viewing your profile requested a profile photo. Add an approved photo when you are comfortable.",
      entityType: "profile",
      entityId: requesterUid,
      deepLink: notificationDeepLink("notifications", functions.config().app_links?.host),
      action: "NOTIFICATIONS",
      pushType: "photo_request",
      preferenceKey: "interests",
      priority: "normal",
      fromFirebaseUid: requesterUid,
    });
  });

async function restoreArchivedConversationOnIncoming(
  threadId: string,
  recipientUid: string
): Promise<void> {
  const prefRef = db.collection("chatPreferences").doc(recipientUid).collection("threads").doc(threadId);
  const pref = await prefRef.get();
  if (!pref.exists || pref.data()?.archived !== true) return;
  await prefRef.set({
    archived: false,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
}

export const onNewMessage = functions.firestore
  .document("chats/{threadId}/messages/{messageId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();
    const fromFirebaseUid = data.fromFirebaseUid as string | undefined;
    const toFirebaseUid = data.toFirebaseUid as string | undefined;
    if (!fromFirebaseUid || !toFirebaseUid) return;
    await restoreArchivedConversationOnIncoming(context.params.threadId, toFirebaseUid);
    if (!await chatNotificationStillAllowed(
      context.params.threadId,
      fromFirebaseUid,
      toFirebaseUid
    )) return;

    await deliverPersistedNotification(
      `message_${context.params.threadId}_${context.params.messageId}_${toFirebaseUid}`,
      {
        userId: toFirebaseUid,
        type: "MESSAGE",
        title: "New message",
        body: "Open the app to view your message.",
        entityType: "chat",
        entityId: context.params.threadId,
        deepLink: notificationDeepLink("messages", functions.config().app_links?.host),
        fromFirebaseUid,
      }
    );
  });
