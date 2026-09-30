import * as admin from "firebase-admin";
import * as crypto from "crypto";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck, requireOpsRole } from "./shared";
import { requireActiveConsent } from "./consent";
import {
  fileSignatureMatchesMime,
  PROFILE_PHOTO_MIME_TYPES,
  PROFILE_VIDEO_MIME_TYPES,
} from "./fileSignaturePolicy";

const MAX_PROFILE_PHOTO_BYTES = 2 * 1024 * 1024;
const MAX_PROFILE_VIDEO_BYTES = 50 * 1024 * 1024;

function moderationIdForPath(path: string): string {
  return crypto.createHash("sha256").update(path).digest("hex");
}

function profilePhotoPath(value: unknown, uid: string): string {
  const path = typeof value === "string" ? value.trim() : "";
  const prefix = `photos/${uid}/`;
  if (!path.startsWith(prefix) || path.length > 512 || path.includes("..")) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid profile photo path");
  }
  return path;
}

function profileVideoPath(value: unknown, uid: string): string {
  const path = typeof value === "string" ? value.trim() : "";
  const prefix = `videos/${uid}/`;
  if (!path.startsWith(prefix) || path.length <= prefix.length || path.length > 512 || path.includes("..")) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid profile video path");
  }
  return path;
}

function reviewDecision(value: unknown): "APPROVED" | "REJECTED" {
  const decision = typeof value === "string" ? value.trim().toUpperCase() : "";
  if (decision !== "APPROVED" && decision !== "REJECTED") {
    throw new functions.https.HttpsError("invalid-argument", "Invalid photo review decision");
  }
  return decision;
}

function reviewReason(value: unknown): string {
  const reason = typeof value === "string" ? value.trim() : "";
  if (reason.length < 3 || reason.length > 500) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "A 3-500 character moderation reason is required"
    );
  }
  return reason;
}

/**
 * Registers an owner-uploaded object for moderation. The callable never publishes the object.
 * Peers can discover a profile photo only after an authorized moderator approves it and the
 * backend writes users.photoUrl.
 */
export const submitProfilePhoto = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  await requireActiveConsent(uid, "media_processing");

  const storagePath = profilePhotoPath(data?.storagePath, uid);
  const file = admin.storage().bucket().file(storagePath);
  const [exists] = await file.exists();
  if (!exists) {
    throw new functions.https.HttpsError("failed-precondition", "Uploaded photo was not found");
  }

  const [metadata] = await file.getMetadata();
  const ownerUid = metadata.metadata?.ownerUid;
  const contentType = String(metadata.contentType || "").toLowerCase();
  const size = Number(metadata.size || 0);
  if (ownerUid !== uid || !PROFILE_PHOTO_MIME_TYPES.has(contentType) ||
      !Number.isFinite(size) || size <= 0 || size > MAX_PROFILE_PHOTO_BYTES) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Uploaded photo does not satisfy the protected media contract"
    );
  }

  const [photoPrefix] = await file.download({ start: 0, end: 31 });
  if (!fileSignatureMatchesMime(photoPrefix, contentType)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Profile photo bytes do not match the declared file type"
    );
  }

  const moderationId = moderationIdForPath(storagePath);
  const moderationRef = db.collection("photoModeration").doc(moderationId);
  const rawFingerprint = typeof metadata.md5Hash === "string" ? metadata.md5Hash : "";
  const fingerprintId = rawFingerprint
    ? crypto.createHash("sha256").update(rawFingerprint).digest("hex")
    : "";
  const fingerprintRef = fingerprintId
    ? db.collection("photoFingerprints").doc(fingerprintId)
    : null;

  await db.runTransaction(async (tx) => {
    const [existing, fingerprint] = await Promise.all([
      tx.get(moderationRef),
      fingerprintRef ? tx.get(fingerprintRef) : Promise.resolve(null),
    ]);
    if (existing.exists) {
      const value = existing.data() || {};
      if (value.uid !== uid || value.storagePath !== storagePath) {
        throw new functions.https.HttpsError("permission-denied", "Photo ownership mismatch");
      }
      return;
    }

    const owners = fingerprint?.exists && Array.isArray(fingerprint.data()?.owners)
      ? fingerprint.data()?.owners.filter((item: unknown): item is string =>
        typeof item === "string" && item.length <= 128)
      : [];
    const duplicateAcrossAccounts = owners.some((owner: string) => owner !== uid);

    tx.create(moderationRef, {
      uid,
      storagePath,
      contentType,
      size,
      duplicateAcrossAccounts,
      status: "PENDING",
      source: "ANDROID",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    if (fingerprintRef && !owners.includes(uid)) {
      tx.set(fingerprintRef, {
        owners: [...owners.slice(0, 49), uid],
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
    }
    if (duplicateAcrossAccounts) {
      tx.set(db.collection("riskSignals").doc(uid), {
        duplicatePhotoSignalCount: admin.firestore.FieldValue.increment(1),
        lastDuplicatePhotoSignalAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
    }
  });

  return { moderationId, status: "PENDING", storagePath };
});

/** Authorized moderation queue. Raw image bytes remain in protected Storage. */
export const listPendingPhotoModeration = functions.https.onCall(async (data, context) => {
  requireOpsRole(context, ["moderator", "ops_admin"]);
  const parsed = Number(data?.limit ?? 50);
  const limit = Number.isFinite(parsed) ? Math.max(1, Math.min(100, Math.floor(parsed))) : 50;
  const snapshot = await db.collection("photoModeration")
    .where("status", "==", "PENDING")
    .limit(limit)
    .get();

  return {
    items: snapshot.docs.map((doc) => {
      const value = doc.data();
      return {
        id: doc.id,
        uid: String(value.uid || ""),
        storagePath: String(value.storagePath || ""),
        contentType: String(value.contentType || ""),
        size: Number(value.size || 0),
        duplicateAcrossAccounts: value.duplicateAcrossAccounts === true,
        createdAtMillis: value.createdAt instanceof admin.firestore.Timestamp
          ? value.createdAt.toMillis()
          : null,
      };
    }),
  };
});

/**
 * Approves or rejects a pending profile photo. Approval may fill an empty primary photo slot;
 * rejection deletes the protected object. Every decision has a separate immutable ops audit row.
 */
export const reviewProfilePhoto = functions.https.onCall(async (data, context) => {
  const actor = requireOpsRole(context, ["moderator", "ops_admin"]);
  const moderationId = typeof data?.moderationId === "string"
    ? data.moderationId.trim()
    : "";
  if (!moderationId.match(/^[a-f0-9]{64}$/)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid photo moderation case");
  }
  const decision = reviewDecision(data?.decision);
  const reason = reviewReason(data?.reason);

  const moderationRef = db.collection("photoModeration").doc(moderationId);
  const auditRef = db.collection("opsAuditLog").doc();
  let storagePath = "";
  let ownerUid = "";

  await db.runTransaction(async (tx) => {
    const moderation = await tx.get(moderationRef);
    if (!moderation.exists) {
      throw new functions.https.HttpsError("not-found", "Photo moderation case not found");
    }
    const current = moderation.data() || {};
    ownerUid = String(current.uid || "");
    storagePath = String(current.storagePath || "");
    if (!ownerUid || !storagePath) {
      throw new functions.https.HttpsError("failed-precondition", "Photo case is malformed");
    }
    if (String(current.status || "") !== "PENDING") {
      throw new functions.https.HttpsError("failed-precondition", "Photo was already reviewed");
    }

    const userRef = db.collection("users").doc(ownerUid);
    const user = await tx.get(userRef);

    tx.update(moderationRef, {
      status: decision,
      reviewReason: reason,
      reviewedBy: actor.uid,
      reviewedRole: actor.role,
      reviewedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    if (decision === "APPROVED" && user.exists &&
        !String(user.data()?.photoUrl || "").trim()) {
      tx.update(userRef, { photoUrl: storagePath });
    }

    tx.create(auditRef, {
      actorUid: actor.uid,
      actorRole: actor.role,
      requestId: actor.requestId,
      action: "PROFILE_PHOTO_REVIEWED",
      targetCollection: "photoModeration",
      targetId: moderationId,
      before: {
        status: "PENDING",
        duplicateAcrossAccounts: current.duplicateAcrossAccounts === true,
      },
      after: { status: decision },
      reason,
      ownerUid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  if (decision === "REJECTED") {
    await admin.storage().bucket().file(storagePath).delete({ ignoreNotFound: true });
  }

  return { success: true, moderationId, status: decision };
});

/** Only an approved object may become the public primary profile photo. */
export const setPrimaryApprovedPhoto = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const storagePath = profilePhotoPath(data?.storagePath, uid);
  const moderation = await db.collection("photoModeration")
    .doc(moderationIdForPath(storagePath))
    .get();
  const value = moderation.data() || {};
  if (!moderation.exists || value.uid !== uid || value.storagePath !== storagePath ||
      value.status !== "APPROVED") {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "This photo has not been approved for profile publication"
    );
  }

  await db.collection("users").doc(uid).set({
    photoUrl: storagePath,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
  return { success: true, storagePath };
});

/**
 * If an approved object is deleted, remove stale moderation authority and clear the public pointer.
 */
export const onProfilePhotoDeleted = functions.storage.object().onDelete(async (object) => {
  const storagePath = object.name || "";
  const match = storagePath.match(/^photos\/([^/]+)\//);
  if (!match) return;
  const uid = match[1];
  const moderationRef = db.collection("photoModeration").doc(moderationIdForPath(storagePath));
  const userRef = db.collection("users").doc(uid);

  await db.runTransaction(async (tx) => {
    const [moderation, user] = await Promise.all([
      tx.get(moderationRef),
      tx.get(userRef),
    ]);
    if (moderation.exists) tx.delete(moderationRef);
    if (user.exists && user.data()?.photoUrl === storagePath) {
      tx.update(userRef, { photoUrl: "" });
    }
  });
});


/**
 * Returns a five-minute signed URL for a pending profile-photo review case. Member Storage rules
 * remain unchanged; the access is role-gated and audited at the backend.
 */
export const getPhotoModerationReviewCase = functions.https.onCall(async (data, context) => {
  const actor = requireOpsRole(context, ["moderator", "ops_admin"]);
  const moderationId = typeof data?.moderationId === "string"
    ? data.moderationId.trim()
    : "";
  if (!moderationId.match(/^[a-f0-9]{64}$/)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid photo moderation case");
  }

  const review = await db.collection("photoModeration").doc(moderationId).get();
  if (!review.exists || String(review.data()?.status || "") !== "PENDING") {
    throw new functions.https.HttpsError("not-found", "Pending photo moderation case not found");
  }
  const value = review.data() || {};
  const ownerUid = String(value.uid || "");
  const storagePath = String(value.storagePath || "");
  if (!ownerUid || !storagePath.startsWith(`photos/${ownerUid}/`) || storagePath.includes("..")) {
    throw new functions.https.HttpsError("failed-precondition", "Photo moderation case is malformed");
  }

  const expiresAtMillis = Date.now() + 5 * 60 * 1000;
  const [documentUrl] = await admin.storage().bucket().file(storagePath).getSignedUrl({
    action: "read",
    expires: expiresAtMillis,
  });
  await db.collection("opsAuditLog").add({
    actorUid: actor.uid,
    actorRole: actor.role,
    requestId: actor.requestId,
    action: "PROFILE_PHOTO_ACCESSED",
    targetCollection: "photoModeration",
    targetId: moderationId,
    ownerUid,
    reason: "PHOTO_REVIEW",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return {
    moderationId,
    uid: ownerUid,
    storagePath,
    documentUrl,
    duplicateAcrossAccounts: value.duplicateAcrossAccounts === true,
    expiresAtMillis,
  };
});


/**
 * Registers an owner-uploaded profile video for moderation. Upload success never publishes the
 * object; only reviewProfileVideo may write users.videoUrl.
 */
export const submitProfileVideo = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  await requireActiveConsent(uid, "media_processing");

  const storagePath = profileVideoPath(data?.storagePath, uid);
  const file = admin.storage().bucket().file(storagePath);
  let metadata: { size?: string | number; contentType?: string; metadata?: Record<string, string> };
  try {
    const [raw] = await file.getMetadata();
    metadata = raw as typeof metadata;
  } catch {
    throw new functions.https.HttpsError("failed-precondition", "Uploaded video was not found");
  }

  const ownerUid = metadata.metadata?.ownerUid || "";
  const contentType = String(metadata.contentType || "").toLowerCase();
  const size = Number(metadata.size || 0);
  if (ownerUid !== uid || !PROFILE_VIDEO_MIME_TYPES.has(contentType) ||
      !Number.isFinite(size) || size <= 0 || size > MAX_PROFILE_VIDEO_BYTES) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Uploaded video does not satisfy the protected media contract"
    );
  }

  const [videoPrefix] = await file.download({ start: 0, end: 31 });
  if (!fileSignatureMatchesMime(videoPrefix, contentType)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Profile video bytes do not match the declared file type"
    );
  }

  const moderationId = moderationIdForPath(storagePath);
  const moderationRef = db.collection("videoModeration").doc(moderationId);
  await db.runTransaction(async (tx) => {
    const existing = await tx.get(moderationRef);
    if (existing.exists) {
      const value = existing.data() || {};
      if (value.uid !== uid || value.storagePath !== storagePath) {
        throw new functions.https.HttpsError("permission-denied", "Video ownership mismatch");
      }
      return;
    }
    tx.create(moderationRef, {
      uid,
      storagePath,
      contentType,
      size,
      status: "PENDING",
      source: "ANDROID",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  return { moderationId, status: "PENDING", storagePath };
});

export const listPendingVideoModeration = functions.https.onCall(async (data, context) => {
  requireOpsRole(context, ["moderator", "ops_admin"]);
  const parsed = Number(data?.limit ?? 50);
  const limit = Number.isFinite(parsed) ? Math.max(1, Math.min(100, Math.floor(parsed))) : 50;
  const snapshot = await db.collection("videoModeration")
    .where("status", "==", "PENDING")
    .limit(limit)
    .get();
  return {
    items: snapshot.docs.map((doc) => {
      const value = doc.data();
      return {
        id: doc.id,
        uid: String(value.uid || ""),
        storagePath: String(value.storagePath || ""),
        contentType: String(value.contentType || ""),
        size: Number(value.size || 0),
        createdAtMillis: value.createdAt instanceof admin.firestore.Timestamp
          ? value.createdAt.toMillis()
          : null,
      };
    }),
  };
});

export const getVideoModerationReviewCase = functions.https.onCall(async (data, context) => {
  const actor = requireOpsRole(context, ["moderator", "ops_admin"]);
  const moderationId = typeof data?.moderationId === "string" ? data.moderationId.trim() : "";
  if (!/^[a-f0-9]{64}$/.test(moderationId)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid video moderation case");
  }
  const review = await db.collection("videoModeration").doc(moderationId).get();
  if (!review.exists || String(review.data()?.status || "") !== "PENDING") {
    throw new functions.https.HttpsError("not-found", "Pending video moderation case not found");
  }
  const value = review.data() || {};
  const ownerUid = String(value.uid || "");
  const storagePath = profileVideoPath(value.storagePath, ownerUid);
  const expiresAtMillis = Date.now() + 5 * 60 * 1000;
  const [documentUrl] = await admin.storage().bucket().file(storagePath).getSignedUrl({
    action: "read",
    expires: expiresAtMillis,
  });
  await db.collection("opsAuditLog").add({
    actorUid: actor.uid,
    actorRole: actor.role,
    requestId: actor.requestId,
    action: "PROFILE_VIDEO_ACCESSED",
    targetCollection: "videoModeration",
    targetId: moderationId,
    ownerUid,
    reason: "VIDEO_REVIEW",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  return { moderationId, uid: ownerUid, storagePath, documentUrl, expiresAtMillis };
});

export const reviewProfileVideo = functions.https.onCall(async (data, context) => {
  const actor = requireOpsRole(context, ["moderator", "ops_admin"]);
  const moderationId = typeof data?.moderationId === "string" ? data.moderationId.trim() : "";
  if (!/^[a-f0-9]{64}$/.test(moderationId)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid video moderation case");
  }
  const decision = reviewDecision(data?.decision);
  const reason = reviewReason(data?.reason);
  const moderationRef = db.collection("videoModeration").doc(moderationId);
  const auditRef = db.collection("opsAuditLog").doc();
  let ownerUid = "";
  let storagePath = "";
  let previousVideoPath = "";

  await db.runTransaction(async (tx) => {
    const moderation = await tx.get(moderationRef);
    if (!moderation.exists || String(moderation.data()?.status || "") !== "PENDING") {
      throw new functions.https.HttpsError("failed-precondition", "Video is not pending review");
    }
    ownerUid = String(moderation.data()?.uid || "");
    storagePath = profileVideoPath(moderation.data()?.storagePath, ownerUid);
    const userRef = db.collection("users").doc(ownerUid);
    const user = await tx.get(userRef);
    if (!user.exists) throw new functions.https.HttpsError("not-found", "Profile not found");
    previousVideoPath = String(user.data()?.videoUrl || "");

    tx.update(moderationRef, {
      status: decision,
      reviewReason: reason,
      reviewedBy: actor.uid,
      reviewedRole: actor.role,
      reviewedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    if (decision === "APPROVED") {
      tx.update(userRef, {
        videoUrl: storagePath,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }
    tx.create(auditRef, {
      actorUid: actor.uid,
      actorRole: actor.role,
      requestId: actor.requestId,
      action: "PROFILE_VIDEO_REVIEWED",
      targetCollection: "videoModeration",
      targetId: moderationId,
      before: { status: "PENDING", videoUrl: previousVideoPath },
      after: { status: decision, videoUrl: decision === "APPROVED" ? storagePath : previousVideoPath },
      reason,
      ownerUid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  if (decision === "REJECTED") {
    await admin.storage().bucket().file(storagePath).delete({ ignoreNotFound: true });
  } else if (
    previousVideoPath &&
    previousVideoPath !== storagePath &&
    previousVideoPath.startsWith(`videos/${ownerUid}/`) &&
    !previousVideoPath.includes("..")
  ) {
    await admin.storage().bucket().file(previousVideoPath).delete({ ignoreNotFound: true });
  }

  return { success: true, moderationId, status: decision };
});

export const removeProfileVideo = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const userRef = db.collection("users").doc(uid);
  const user = await userRef.get();
  if (!user.exists) throw new functions.https.HttpsError("not-found", "Profile not found");
  const current = String(user.data()?.videoUrl || "");
  await userRef.update({
    videoUrl: "",
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  if (current.startsWith(`videos/${uid}/`) && !current.includes("..")) {
    await admin.storage().bucket().file(current).delete({ ignoreNotFound: true });
  }
  return { success: true };
});

export const onProfileVideoDeleted = functions.storage.object().onDelete(async (object) => {
  const storagePath = object.name || "";
  const match = storagePath.match(/^videos\/([^/]+)\//);
  if (!match) return;
  const uid = match[1];
  const moderationRef = db.collection("videoModeration").doc(moderationIdForPath(storagePath));
  const userRef = db.collection("users").doc(uid);
  await db.runTransaction(async (tx) => {
    const [moderation, user] = await Promise.all([
      tx.get(moderationRef),
      tx.get(userRef),
    ]);
    if (moderation.exists) tx.delete(moderationRef);
    if (user.exists && user.data()?.videoUrl === storagePath) {
      tx.update(userRef, { videoUrl: "" });
    }
  });
});


/**
 * Defense-in-depth for direct Firebase Storage chat uploads. Storage Rules enforce participant,
 * filename, metadata and MIME contracts, while this trigger verifies the actual file signature.
 * A malicious client that lies about bytes is quarantined by deleting the object and recording a
 * private risk signal; no public/profile state is changed automatically.
 */
export const onChatMediaUploaded = functions.storage.object().onFinalize(async (object) => {
  const storagePath = object.name || "";
  const match = storagePath.match(
    /^chat-media\/([a-f0-9]{64})\/([A-Za-z0-9_-]{16,128})\.(jpg|m4a)$/
  );
  if (!match) return;

  const threadId = match[1];
  const extension = match[3];
  const metadata = object.metadata || {};
  const senderUid = String(metadata.senderUid || "");
  const recipientUid = String(metadata.recipientUid || "");
  const kind = String(metadata.kind || "");
  const declaredThread = String(metadata.threadId || "");
  const contentType = String(object.contentType || "").toLowerCase();
  const expectedMime = extension === "jpg" ? "image/jpeg" : "audio/mp4";
  const expectedKind = extension === "jpg" ? "image" : "voice";
  const file = admin.storage().bucket(object.bucket).file(storagePath);

  let valid = Boolean(
    senderUid &&
    recipientUid &&
    senderUid !== recipientUid &&
    declaredThread === threadId &&
    kind === expectedKind &&
    contentType === expectedMime
  );

  if (valid) {
    try {
      const [prefix] = await file.download({ start: 0, end: 31 });
      valid = fileSignatureMatchesMime(prefix, contentType);
    } catch (error) {
      functions.logger.warn("Chat media signature read failed", {
        storagePath,
        error: error instanceof Error ? error.message : String(error),
      });
      valid = false;
    }
  }

  if (valid) return;

  await file.delete({ ignoreNotFound: true });
  if (senderUid) {
    await db.collection("riskSignals").doc(senderUid).set({
      invalidChatMediaSignalCount: admin.firestore.FieldValue.increment(1),
      lastInvalidChatMediaSignalAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
  }
  functions.logger.warn("Invalid chat media quarantined", {
    storagePath,
    senderUid: senderUid || null,
    contentType,
    kind,
  });
});
