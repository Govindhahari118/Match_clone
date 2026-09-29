import * as admin from "firebase-admin";
import * as crypto from "crypto";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";
import { accountIsActive } from "./accountStatusPolicy";
import {
  MAX_DAILY_MESSAGES_SAFETY,
  safeUsageCount,
  usageAllowed,
} from "./abusePolicy";

const MAX_BODY_LENGTH = 3000;
const MAX_CHAT_IMAGE_BYTES = 8 * 1024 * 1024;
const MAX_CHAT_VOICE_BYTES = 12 * 1024 * 1024;

function cleanUid(value: unknown): string {
  const uid = typeof value === "string" ? value.trim() : "";
  if (!uid || uid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid target member");
  }
  return uid;
}

function cleanMessageId(value: unknown): string {
  const id = typeof value === "string" ? value.trim() : "";
  if (!/^[A-Za-z0-9_-]{16,128}$/.test(id)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid message id");
  }
  return id;
}

function canonicalThreadId(uidA: string, uidB: string): string {
  const canonical = [uidA, uidB].sort().join("\n");
  return crypto.createHash("sha256").update(canonical, "utf8").digest("hex");
}

function dayKey(): string {
  return new Date().toISOString().slice(0, 10);
}

async function validateChatMedia(
  pathValue: unknown,
  expectedPath: string,
  senderUid: string,
  recipientUid: string,
  threadId: string,
  kind: "image" | "voice"
): Promise<string | null> {
  if (pathValue == null || pathValue === "") return null;
  const path = typeof pathValue === "string" ? pathValue.trim() : "";
  if (path !== expectedPath) {
    throw new functions.https.HttpsError("invalid-argument", "Chat media path does not match the message");
  }

  const file = admin.storage().bucket().file(path);
  let metadata: {
    size?: string | number;
    contentType?: string;
    metadata?: Record<string, string>;
  };
  try {
    const [raw] = await file.getMetadata();
    metadata = raw as typeof metadata;
  } catch {
    throw new functions.https.HttpsError("failed-precondition", "Uploaded chat media was not found");
  }

  const custom = metadata.metadata || {};
  const size = Number(metadata.size || 0);
  const contentType = String(metadata.contentType || "").toLowerCase();
  const validSize = Number.isFinite(size) && size > 0 &&
    size <= (kind === "image" ? MAX_CHAT_IMAGE_BYTES : MAX_CHAT_VOICE_BYTES);
  const validType = kind === "image"
    ? contentType === "image/jpeg"
    : contentType === "audio/mp4";

  if (
    custom.senderUid !== senderUid ||
    custom.recipientUid !== recipientUid ||
    custom.threadId !== threadId ||
    custom.kind !== kind ||
    !validSize ||
    !validType
  ) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Uploaded chat media does not satisfy the protected message contract"
    );
  }
  return path;
}

/**
 * Server-authoritative, idempotent chat send.
 *
 * The Android durable outbox keeps the client-generated message id stable across retries. Media is
 * uploaded first under Storage Rules, then this callable re-checks relationship/account state,
 * media ownership and a hard daily safety ceiling before atomically creating the immutable message
 * and thread preview. Recipient delivery/read receipts remain client acknowledgements.
 */
export const sendChatMessage = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const senderUid = context.auth?.uid;
  if (!senderUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const recipientUid = cleanUid(data?.targetUid);
  if (recipientUid === senderUid) {
    throw new functions.https.HttpsError("invalid-argument", "Cannot message yourself");
  }
  const clientMessageId = cleanMessageId(data?.clientMessageId);
  const body = typeof data?.body === "string" ? data.body.trim() : "";
  if (body.length > MAX_BODY_LENGTH) {
    throw new functions.https.HttpsError("invalid-argument", "Message is too long");
  }

  const threadId = canonicalThreadId(senderUid, recipientUid);
  const imagePath = await validateChatMedia(
    data?.imagePath,
    `chat-media/${threadId}/${clientMessageId}.jpg`,
    senderUid,
    recipientUid,
    threadId,
    "image"
  );
  const voicePath = await validateChatMedia(
    data?.voicePath,
    `chat-media/${threadId}/${clientMessageId}.m4a`,
    senderUid,
    recipientUid,
    threadId,
    "voice"
  );
  if (imagePath && voicePath) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "A message may contain only one media attachment"
    );
  }
  if (!body && !imagePath && !voicePath) {
    throw new functions.https.HttpsError("invalid-argument", "Message content is required");
  }

  const voiceDurationMs = voicePath ? Number(data?.voiceDurationMs || 0) : 0;
  if (voicePath && (!Number.isFinite(voiceDurationMs) ||
      voiceDurationMs < 500 || voiceDurationMs > 5 * 60 * 1000)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid voice duration");
  }

  const pair = [senderUid, recipientUid].sort();
  const matchRef = db.collection("matches").doc(pair.join("_"));
  const senderRef = db.collection("users").doc(senderUid);
  const recipientRef = db.collection("users").doc(recipientUid);
  const senderBlockRef = db.collection("blocks").doc(senderUid).collection("blocked").doc(recipientUid);
  const recipientBlockRef = db.collection("blocks").doc(recipientUid).collection("blocked").doc(senderUid);
  const senderPrivacyRef = db.collection("privacyRelations").doc(senderUid).collection("members").doc(recipientUid);
  const recipientPrivacyRef = db.collection("privacyRelations").doc(recipientUid).collection("members").doc(senderUid);
  const threadRef = db.collection("chats").doc(threadId);
  const messageRef = threadRef.collection("messages").doc(clientMessageId);
  const activityRef = db.collection("riskActivity").doc(senderUid).collection("days").doc(dayKey());

  const preview = imagePath ? "📷 Image" : voicePath ? "🎤 Voice message" : body.slice(0, 120);
  const sentAt = Date.now();

  return db.runTransaction(async (tx) => {
    const [
      sender,
      recipient,
      match,
      senderBlock,
      recipientBlock,
      senderPrivacy,
      recipientPrivacy,
      existing,
      activity,
    ] = await Promise.all([
      tx.get(senderRef),
      tx.get(recipientRef),
      tx.get(matchRef),
      tx.get(senderBlockRef),
      tx.get(recipientBlockRef),
      tx.get(senderPrivacyRef),
      tx.get(recipientPrivacyRef),
      tx.get(messageRef),
      tx.get(activityRef),
    ]);

    if (existing.exists) {
      const value = existing.data() || {};
      if (value.fromFirebaseUid === senderUid && value.toFirebaseUid === recipientUid) {
        return { success: true, alreadySent: true, sentAt: Number(value.sentAt || sentAt) };
      }
      throw new functions.https.HttpsError("already-exists", "Message id collision");
    }

    if (!sender.exists || !recipient.exists) {
      throw new functions.https.HttpsError("not-found", "Profile not found");
    }
    if (!accountIsActive(sender.data()?.accountStatus) ||
        !accountIsActive(recipient.data()?.accountStatus)) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Messaging is unavailable while an account is not active"
      );
    }
    const matchUsers = Array.isArray(match.data()?.users) ? match.data()?.users : [];
    if (!match.exists || !matchUsers.includes(senderUid) || !matchUsers.includes(recipientUid)) {
      throw new functions.https.HttpsError("failed-precondition", "Mutual match required");
    }
    if (senderBlock.exists || recipientBlock.exists) {
      throw new functions.https.HttpsError("permission-denied", "Messaging is unavailable after a block");
    }
    if (senderPrivacy.data()?.profileHidden === true ||
        recipientPrivacy.data()?.profileHidden === true) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Messaging is unavailable for this privacy relationship"
      );
    }

    const sendCount = safeUsageCount(activity.data()?.messageSendCount);
    if (!usageAllowed(sendCount, MAX_DAILY_MESSAGES_SAFETY)) {
      throw new functions.https.HttpsError(
        "resource-exhausted",
        "Daily message safety limit reached. Try again tomorrow."
      );
    }

    tx.set(activityRef, {
      messageSendCount: sendCount + 1,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });

    tx.set(threadRef, {
      participantUids: pair,
      lastMessage: preview,
      lastSentAt: sentAt,
    }, { merge: false });

    const message: Record<string, unknown> = {
      body,
      sentAt,
      isRead: false,
      fromFirebaseUid: senderUid,
      toFirebaseUid: recipientUid,
    };
    if (imagePath) message.imageUri = imagePath;
    if (voicePath) {
      message.voiceUri = voicePath;
      message.voiceDurationMs = Math.trunc(voiceDurationMs);
    }
    tx.create(messageRef, message);

    return { success: true, alreadySent: false, sentAt };
  });
});
