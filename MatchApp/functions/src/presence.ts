import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";

const MIN_HEARTBEAT_INTERVAL_MS = 60_000;
const ONLINE_WINDOW_MS = 5 * 60_000;
type ActivityVisibility = "everyone" | "interests" | "mutual" | "nobody";

function requireUid(value: unknown, field: string): string {
  const uid = typeof value === "string" ? value.trim() : "";
  if (!uid || uid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", `Invalid ${field}`);
  }
  return uid;
}

function visibility(value: unknown): ActivityVisibility {
  return value === "everyone" || value === "interests" || value === "mutual" || value === "nobody"
    ? value
    : "mutual";
}

async function relationship(viewerUid: string, targetUid: string): Promise<{ interested: boolean; mutual: boolean }> {
  const forwardRef = db.collection("interests").doc(`${viewerUid}_${targetUid}`);
  const reverseRef = db.collection("interests").doc(`${targetUid}_${viewerUid}`);
  const matchRef = db.collection("matches").doc([viewerUid, targetUid].sort().join("_"));
  const [forward, reverse, match] = await Promise.all([forwardRef.get(), reverseRef.get(), matchRef.get()]);
  return {
    interested: forward.exists || reverse.exists,
    mutual: match.exists || (forward.exists && reverse.exists),
  };
}

function allowed(choice: ActivityVisibility, state: { interested: boolean; mutual: boolean }): boolean {
  switch (choice) {
  case "everyone": return true;
  case "interests": return state.interested;
  case "mutual": return state.mutual;
  case "nobody": return false;
  }
}

/**
 * Records an authenticated member heartbeat. Precise presence lives in a server-only document;
 * the public profile no longer needs to expose lastActiveAt to every signed-in member.
 */
export const touchPresence = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const userRef = db.collection("users").doc(uid);
  const presenceRef = db.collection("presencePrivate").doc(uid);
  const [userSnap, presenceSnap] = await Promise.all([userRef.get(), presenceRef.get()]);
  if (!userSnap.exists) {
    throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");
  }

  const now = Date.now();
  const previous = Number(presenceSnap.data()?.lastActiveAt || 0);
  if (Number.isFinite(previous) && now - previous < MIN_HEARTBEAT_INTERVAL_MS) {
    return { success: true, lastActiveAt: previous, throttled: true };
  }

  const batch = db.batch();
  batch.set(presenceRef, {
    uid,
    lastActiveAt: now,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
  // Opportunistically scrub the legacy public timestamp for this member.
  if (userSnap.data()?.lastActiveAt !== undefined) {
    batch.update(userRef, { lastActiveAt: admin.firestore.FieldValue.delete() });
  }
  await batch.commit();
  return { success: true, lastActiveAt: now, throttled: false };
});

/**
 * Returns only the activity pieces the target member currently permits this viewer to see.
 * Blocking, profile-hide exceptions and stealth are enforced before any timestamp is released.
 */
export const getMemberPresence = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const viewerUid = context.auth?.uid;
  if (!viewerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const targetUid = requireUid(data?.targetUid, "target profile");

  if (targetUid === viewerUid) {
    const own = await db.collection("presencePrivate").doc(targetUid).get();
    const lastActiveAt = Number(own.data()?.lastActiveAt || 0);
    return { online: lastActiveAt > 0 && Date.now() - lastActiveAt <= ONLINE_WINDOW_MS, lastActiveAt };
  }

  const [target, outgoingBlock, incomingBlock, viewerRelation, targetRelation, settings] = await Promise.all([
    db.collection("users").doc(targetUid).get(),
    db.collection("blocks").doc(viewerUid).collection("blocked").doc(targetUid).get(),
    db.collection("blocks").doc(targetUid).collection("blocked").doc(viewerUid).get(),
    db.collection("privacyRelations").doc(viewerUid).collection("members").doc(targetUid).get(),
    db.collection("privacyRelations").doc(targetUid).collection("members").doc(viewerUid).get(),
    db.collection("privacySettings").doc(targetUid).get(),
  ]);
  if (!target.exists) throw new functions.https.HttpsError("not-found", "Profile not found");
  if (outgoingBlock.exists || incomingBlock.exists ||
      viewerRelation.data()?.profileHidden === true || targetRelation.data()?.profileHidden === true ||
      target.data()?.stealthMode === true) {
    throw new functions.https.HttpsError("permission-denied", "Activity is unavailable for this member");
  }

  const relation = await relationship(viewerUid, targetUid);
  const onlineVisibility = visibility(settings.data()?.onlineVisibility);
  const lastActiveVisibility = visibility(settings.data()?.lastActiveVisibility);
  const canSeeOnline = allowed(onlineVisibility, relation);
  const canSeeLastActive = allowed(lastActiveVisibility, relation);
  if (!canSeeOnline && !canSeeLastActive) {
    return { online: false, lastActiveAt: 0 };
  }

  const presence = await db.collection("presencePrivate").doc(targetUid).get();
  const preciseLastActiveAt = Number(presence.data()?.lastActiveAt || 0);
  const online = canSeeOnline && preciseLastActiveAt > 0 && Date.now() - preciseLastActiveAt <= ONLINE_WINDOW_MS;
  return {
    online,
    lastActiveAt: canSeeLastActive ? preciseLastActiveAt : 0,
  };
});

/**
 * Incremental migration for legacy profiles that still contain public activity timestamps. It
 * preserves the timestamp in the private collection before deleting it from the public document.
 */
export const migrateLegacyPublicPresence = functions.pubsub
  .schedule("30 2 * * *")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const snapshot = await db.collection("users").where("lastActiveAt", ">", 0).limit(300).get();
    if (snapshot.empty) return null;
    const batch = db.batch();
    snapshot.docs.forEach((doc) => {
      const lastActiveAt = Number(doc.data().lastActiveAt || 0);
      if (lastActiveAt > 0) {
        batch.set(db.collection("presencePrivate").doc(doc.id), {
          uid: doc.id,
          lastActiveAt,
          migratedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, { merge: true });
      }
      batch.update(doc.ref, { lastActiveAt: admin.firestore.FieldValue.delete() });
    });
    await batch.commit();
    functions.logger.info("Migrated legacy public presence", { count: snapshot.size });
    return null;
  });
