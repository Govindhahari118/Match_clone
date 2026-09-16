import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";

const CONTACT_LIMITS: Record<string, number> = {
  SILVER_3M: 75,
  GOLD_6M: 150,
  PLATINUM_12M: 300,
};

const CONTACT_TYPES = new Set(["phone", "whatsapp"]);

function relationRef(ownerUid: string, memberUid: string): FirebaseFirestore.DocumentReference {
  return db.collection("privacyRelations").doc(ownerUid).collection("members").doc(memberUid);
}

function grantRef(ownerUid: string, viewerUid: string): FirebaseFirestore.DocumentReference {
  return db.collection("contactGrants").doc(ownerUid).collection("viewers").doc(viewerUid);
}

async function deleteQuery(query: FirebaseFirestore.Query): Promise<void> {
  let hasMore = true;
  while (hasMore) {
    const snap = await query.limit(300).get();
    if (snap.empty) return;
    const batch = db.batch();
    snap.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
    hasMore = snap.size === 300;
  }
}

/**
 * Return a matched member's phone/WhatsApp number only when the requester is entitled AND the
 * target member's current global/per-person privacy choices permit that exact contact type.
 */
export const consumeContactReveal = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = typeof data?.targetUid === "string" ? data.targetUid.trim() : "";
  const contactType = typeof data?.contactType === "string" ? data.contactType.trim().toLowerCase() : "phone";
  if (!targetUid || targetUid === uid || targetUid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid target profile");
  }
  if (!CONTACT_TYPES.has(contactType)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid contact type");
  }

  const matchId = [uid, targetUid].sort().join("_");
  const [outgoingBlock, incomingBlock, requesterRelation, targetRelation, targetSettings, contactGrant] = await Promise.all([
    db.collection("blocks").doc(uid).collection("blocked").doc(targetUid).get(),
    db.collection("blocks").doc(targetUid).collection("blocked").doc(uid).get(),
    relationRef(uid, targetUid).get(),
    relationRef(targetUid, uid).get(),
    db.collection("privacySettings").doc(targetUid).get(),
    grantRef(targetUid, uid).get(),
  ]);

  if (outgoingBlock.exists || incomingBlock.exists) {
    throw new functions.https.HttpsError("permission-denied", "Contact is unavailable for this member");
  }
  if (requesterRelation.data()?.profileHidden === true || targetRelation.data()?.profileHidden === true) {
    throw new functions.https.HttpsError("permission-denied", "Contact is unavailable while profile visibility is restricted");
  }
  if (targetRelation.data()?.contactHidden === true) {
    throw new functions.https.HttpsError("permission-denied", "This member has hidden their contact from you");
  }

  const visibility = String(targetSettings.data()?.contactVisibility || "mutual_matches");
  if (visibility === "nobody") {
    throw new functions.https.HttpsError("permission-denied", "This member is not sharing contact details");
  }
  if (visibility === "selected_people") {
    const grant = contactGrant.data() || {};
    const allowed = contactType === "whatsapp" ? grant.whatsappAllowed === true : grant.phoneAllowed === true;
    if (!contactGrant.exists || !allowed) {
      throw new functions.https.HttpsError("permission-denied", "This member has not shared this contact method with you");
    }
  } else if (visibility !== "mutual_matches") {
    throw new functions.https.HttpsError("permission-denied", "This member's contact privacy setting is unavailable");
  }

  const userRef = db.collection("users").doc(uid);
  const privateRef = db.collection("userPrivate").doc(targetUid);
  const matchRef = db.collection("matches").doc(matchId);
  const usageRef = db.collection("subscriptions").doc(uid).collection("usage").doc("current");

  return db.runTransaction(async (tx) => {
    const [userSnap, targetPrivateSnap, matchSnap, usageSnap] = await Promise.all([
      tx.get(userRef), tx.get(privateRef), tx.get(matchRef), tx.get(usageRef),
    ]);
    if (!userSnap.exists) throw new functions.https.HttpsError("not-found", "User profile not found");
    if (!matchSnap.exists) throw new functions.https.HttpsError("failed-precondition", "Mutual match required");

    const user = userSnap.data() || {};
    const planId = String(user.subscriptionPlan || user.premiumPlan || "FREE");
    const contactLimit = CONTACT_LIMITS[planId] || 0;
    const expiry = user.premiumUntil instanceof admin.firestore.Timestamp
      ? user.premiumUntil.toMillis() : Number(user.subscriptionExpiry || 0);
    if (user.isPremium !== true || expiry <= Date.now() || contactLimit <= 0) {
      throw new functions.https.HttpsError("permission-denied", "Active paid membership required");
    }

    const phoneNumber = String(targetPrivateSnap.data()?.phoneNumber || "").trim();
    if (!phoneNumber) throw new functions.https.HttpsError("failed-precondition", "This member has not shared a phone number");

    const paymentId = String(user.paymentId || "");
    if (!paymentId) throw new functions.https.HttpsError("failed-precondition", "Membership entitlement is incomplete");
    const usage = usageSnap.data() || {};
    const sameEntitlement = usage.paymentId === paymentId;
    const existingTargets = sameEntitlement && Array.isArray(usage.revealedTargets)
      ? (usage.revealedTargets as unknown[]).filter((v): v is string => typeof v === "string")
      : [];

    if (existingTargets.includes(targetUid)) {
      return { phoneNumber, contactType, contactsUsed: existingTargets.length, contactsLimit: contactLimit };
    }
    if (existingTargets.length >= contactLimit) {
      throw new functions.https.HttpsError("resource-exhausted", "Contact reveal limit reached for this membership");
    }

    const nextTargets = [...existingTargets, targetUid];
    tx.set(usageRef, {
      paymentId,
      planId,
      revealedTargets: nextTargets,
      contactsUsed: nextTargets.length,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: false });

    return { phoneNumber, contactType, contactsUsed: nextTargets.length, contactsLimit: contactLimit };
  });
});

/** Remove owner settings/grants and references to a deleted account from other users' privacy lists. */
export const cleanupPrivacyOnUserDelete = functions.firestore
  .document("users/{uid}")
  .onDelete(async (_snap, context) => {
    const uid = context.params.uid as string;
    await Promise.all([
      deleteQuery(db.collection("privacyRelations").doc(uid).collection("members")),
      deleteQuery(db.collectionGroup("members").where("memberUid", "==", uid)),
      deleteQuery(db.collection("contactGrants").doc(uid).collection("viewers")),
      deleteQuery(db.collectionGroup("viewers").where("viewerUid", "==", uid)),
      db.collection("privacySettings").doc(uid).delete(),
    ]);
    await Promise.all([
      db.collection("privacyRelations").doc(uid).delete().catch(() => undefined),
      db.collection("contactGrants").doc(uid).delete().catch(() => undefined),
    ]);
  });
