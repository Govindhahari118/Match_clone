import * as admin from "firebase-admin";
import * as crypto from "crypto";
import * as functions from "firebase-functions/v1";
import { db } from "./shared";
import {
  HIGH_VOLUME_MESSAGE_SIGNAL_THRESHOLD,
  crossesThreshold,
  safeUsageCount,
} from "./abusePolicy";
import { classifyMessageSafety } from "./messageSafetyPolicy";

const MESSAGE_EVENT_RETENTION_MS = 30 * 24 * 60 * 60 * 1000;

function eventId(threadId: string, messageId: string): string {
  return crypto.createHash("sha256").update(threadId + "|" + messageId).digest("hex");
}

function dayKey(): string {
  return new Date().toISOString().slice(0, 10);
}

/**
 * Privacy-safe abuse telemetry for authoritative chat messages.
 *
 * This never blocks a message and never changes Trust Score/account state by itself. It records
 * bounded private signals for human moderation. The trigger is idempotent so event retries do not
 * inflate counts.
 */
export const onChatMessageSafetySignal = functions.firestore
  .document("chats/{threadId}/messages/{messageId}")
  .onCreate(async (snap, context) => {
    const value = snap.data() || {};
    const senderUid = typeof value.fromFirebaseUid === "string"
      ? value.fromFirebaseUid.trim()
      : "";
    const recipientUid = typeof value.toFirebaseUid === "string"
      ? value.toFirebaseUid.trim()
      : "";
    if (!senderUid || !recipientUid || senderUid === recipientUid) return;

    const signals = classifyMessageSafety(value.body);
    const hasExternalLink = signals.externalLink;
    const hasMoneyRequest = signals.moneyRequest;
    const day = dayKey();

    const activityRef = db.collection("riskActivity").doc(senderUid).collection("days").doc(day);
    const markerRef = db.collection("riskActivity")
      .doc(senderUid)
      .collection("messageEvents")
      .doc(eventId(context.params.threadId, context.params.messageId));
    const signalRef = db.collection("riskSignals").doc(senderUid);

    await db.runTransaction(async (tx) => {
      const [marker, activity] = await Promise.all([
        tx.get(markerRef),
        tx.get(activityRef),
      ]);
      if (marker.exists) return;

      const currentCount = safeUsageCount(activity.data()?.messageCount);
      const nextCount = currentCount + 1;
      tx.set(activityRef, {
        messageCount: nextCount,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });

      const signalUpdate: Record<string, unknown> = {
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      };
      let hasSignalUpdate = false;

      if (crossesThreshold(currentCount, HIGH_VOLUME_MESSAGE_SIGNAL_THRESHOLD)) {
        signalUpdate.highVolumeMessageDayCount = admin.firestore.FieldValue.increment(1);
        signalUpdate.lastHighVolumeMessageAt = admin.firestore.FieldValue.serverTimestamp();
        hasSignalUpdate = true;
      }
      if (hasExternalLink) {
        signalUpdate.externalLinkMessageCount = admin.firestore.FieldValue.increment(1);
        signalUpdate.lastExternalLinkMessageAt = admin.firestore.FieldValue.serverTimestamp();
        hasSignalUpdate = true;
      }
      if (hasMoneyRequest) {
        signalUpdate.moneyRequestSignalCount = admin.firestore.FieldValue.increment(1);
        signalUpdate.lastMoneyRequestSignalAt = admin.firestore.FieldValue.serverTimestamp();
        hasSignalUpdate = true;
      }
      if (hasSignalUpdate) {
        tx.set(signalRef, signalUpdate, { merge: true });
      }

      tx.create(markerRef, {
        senderUid,
        recipientUid,
        threadId: context.params.threadId,
        messageId: context.params.messageId,
        externalLinkSignal: hasExternalLink,
        moneyRequestSignal: hasMoneyRequest,
        processedAt: admin.firestore.FieldValue.serverTimestamp(),
        expireAt: admin.firestore.Timestamp.fromMillis(Date.now() + MESSAGE_EVENT_RETENTION_MS),
      });
    });
  });

export const cleanupExpiredMessageSafetyMarkers = functions.pubsub
  .schedule("every 24 hours")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const now = admin.firestore.Timestamp.now();
    let hasMore = true;
    while (hasMore) {
      const snapshot = await db.collectionGroup("messageEvents")
        .where("expireAt", "<=", now)
        .limit(500)
        .get();
      if (snapshot.empty) return;
      const batch = db.batch();
      snapshot.docs.forEach((doc) => batch.delete(doc.ref));
      await batch.commit();
      hasMore = snapshot.size === 500;
    }
  });
