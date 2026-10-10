import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";
import { accountIsActive } from "./accountStatusPolicy";
import { resolveMembershipState } from "./membershipAuthority";
import {
  canRequestPhotoAccess,
  canViewPublishedPhoto,
  normalizeProfilePhotoVisibility,
} from "./photoPrivacyPolicy";

const CONTACT_LIMITS: Record<string, number> = {
  SILVER_3M: 75,
  GOLD_6M: 150,
  PLATINUM_12M: 300,
};

const CONTACT_TYPES = new Set(["phone", "whatsapp"]);
const CONTACT_REQUEST_COOLDOWN_MS = 7 * 24 * 60 * 60 * 1000;
const PHOTO_REQUEST_COOLDOWN_MS = 7 * 24 * 60 * 60 * 1000;

function relationRef(ownerUid: string, memberUid: string): FirebaseFirestore.DocumentReference {
  return db.collection("privacyRelations").doc(ownerUid).collection("members").doc(memberUid);
}

function grantRef(ownerUid: string, viewerUid: string): FirebaseFirestore.DocumentReference {
  return db.collection("contactGrants").doc(ownerUid).collection("viewers").doc(viewerUid);
}

function contactRequestRef(requesterUid: string, targetUid: string): FirebaseFirestore.DocumentReference {
  return db.collection("contactRequests").doc(`${requesterUid}_${targetUid}`);
}

function photoGrantRef(ownerUid: string, viewerUid: string): FirebaseFirestore.DocumentReference {
  return db.collection("photoGrants").doc(ownerUid).collection("viewers").doc(viewerUid);
}

function photoRequestRef(requesterUid: string, targetUid: string): FirebaseFirestore.DocumentReference {
  return db.collection("photoAccessRequests").doc(`${requesterUid}_${targetUid}`);
}

function interestRef(fromUid: string, toUid: string): FirebaseFirestore.DocumentReference {
  return db.collection("interests").doc(`${fromUid}_${toUid}`);
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
  const outgoingBlockRef = db.collection("blocks").doc(uid).collection("blocked").doc(targetUid);
  const incomingBlockRef = db.collection("blocks").doc(targetUid).collection("blocked").doc(uid);
  const requesterRelationRef = relationRef(uid, targetUid);
  const targetRelationRef = relationRef(targetUid, uid);
  const targetSettingsRef = db.collection("privacySettings").doc(targetUid);
  const contactGrantRef = grantRef(targetUid, uid);

  const userRef = db.collection("users").doc(uid);
  const targetRef = db.collection("users").doc(targetUid);
  const privateRef = db.collection("userPrivate").doc(targetUid);
  const matchRef = db.collection("matches").doc(matchId);
  const subscriptionRef = db.collection("subscriptions").doc(uid);
  const usageRef = subscriptionRef.collection("usage").doc("current");

  return db.runTransaction(async (tx) => {
    const [
      userSnap,
      targetSnap,
      targetPrivateSnap,
      matchSnap,
      subscriptionSnap,
      usageSnap,
      outgoingBlock,
      incomingBlock,
      requesterRelation,
      targetRelation,
      targetSettings,
      contactGrant,
    ] = await Promise.all([
      tx.get(userRef),
      tx.get(targetRef),
      tx.get(privateRef),
      tx.get(matchRef),
      tx.get(subscriptionRef),
      tx.get(usageRef),
      tx.get(outgoingBlockRef),
      tx.get(incomingBlockRef),
      tx.get(requesterRelationRef),
      tx.get(targetRelationRef),
      tx.get(targetSettingsRef),
      tx.get(contactGrantRef),
    ]);
    if (!userSnap.exists) throw new functions.https.HttpsError("not-found", "User profile not found");
    if (!targetSnap.exists) throw new functions.https.HttpsError("not-found", "Target profile not found");
    if (
      !accountIsActive(userSnap.data()?.accountStatus) ||
      !accountIsActive(targetSnap.data()?.accountStatus)
    ) {
      throw new functions.https.HttpsError("failed-precondition", "Contact is unavailable while an account is not active");
    }
    if (!matchSnap.exists) throw new functions.https.HttpsError("failed-precondition", "Mutual match required");
    if (outgoingBlock.exists || incomingBlock.exists) {
      throw new functions.https.HttpsError("permission-denied", "Contact is unavailable for this member");
    }
    if (requesterRelation.data()?.profileHidden === true || targetRelation.data()?.profileHidden === true) {
      throw new functions.https.HttpsError("permission-denied", "Contact is unavailable while profile visibility is restricted");
    }
    if (targetRelation.data()?.contactHidden === true) {
      throw new functions.https.HttpsError("permission-denied", "This member has hidden their contact from you");
    }

    const visibility = String(targetSettings.data()?.contactVisibility || "selected_people");
    if (visibility === "nobody") {
      throw new functions.https.HttpsError("permission-denied", "This member is not sharing contact details");
    }
    if (visibility === "selected_people") {
      const grantData = contactGrant.data() || {};
      const allowed = contactType === "whatsapp" ?
        grantData.whatsappAllowed === true :
        grantData.phoneAllowed === true;
      if (!contactGrant.exists || !allowed) {
        throw new functions.https.HttpsError(
          "permission-denied",
          "This member has not shared this contact method with you"
        );
      }
    } else if (visibility !== "mutual_matches") {
      throw new functions.https.HttpsError(
        "permission-denied",
        "This member's contact privacy setting is unavailable"
      );
    }

    const user = userSnap.data() || {};
    const membership = resolveMembershipState(subscriptionSnap.data(), user);
    const planId = membership.planId;
    const contactLimit = CONTACT_LIMITS[planId] || 0;
    if (!membership.active || contactLimit <= 0) {
      throw new functions.https.HttpsError("permission-denied", "Active paid membership required");
    }

    const phoneNumber = String(targetPrivateSnap.data()?.phoneNumber || "").trim();
    if (!phoneNumber) throw new functions.https.HttpsError("failed-precondition", "This member has not shared a phone number");

    const paymentId = membership.paymentId;
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

/**
 * Request explicit contact sharing. This is separate from paid entitlement: a member may approve
 * the request before the requester buys a plan, but the phone number is still returned only by
 * consumeContactReveal after entitlement/quota checks.
 */
export const requestContactAccess = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const requesterUid = context.auth?.uid;
  if (!requesterUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const targetUid = typeof data?.targetUid === "string" ? data.targetUid.trim() : "";
  if (!targetUid || targetUid === requesterUid || targetUid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid target profile");
  }

  const matchId = [requesterUid, targetUid].sort().join("_");
  const requestRef = contactRequestRef(requesterUid, targetUid);
  const result = await db.runTransaction(async (tx) => {
    const [
      requesterProfile,
      targetProfile,
      matchSnap,
      outgoingBlock,
      incomingBlock,
      requesterRelation,
      targetRelation,
      targetSettings,
      existingGrant,
      existingRequest,
    ] = await Promise.all([
      tx.get(db.collection("users").doc(requesterUid)),
      tx.get(db.collection("users").doc(targetUid)),
      tx.get(db.collection("matches").doc(matchId)),
      tx.get(db.collection("blocks").doc(requesterUid).collection("blocked").doc(targetUid)),
      tx.get(db.collection("blocks").doc(targetUid).collection("blocked").doc(requesterUid)),
      tx.get(relationRef(requesterUid, targetUid)),
      tx.get(relationRef(targetUid, requesterUid)),
      tx.get(db.collection("privacySettings").doc(targetUid)),
      tx.get(grantRef(targetUid, requesterUid)),
      tx.get(requestRef),
    ]);

    if (!requesterProfile.exists || !targetProfile.exists) {
      throw new functions.https.HttpsError("not-found", "Profile not found");
    }
    if (
      !accountIsActive(requesterProfile.data()?.accountStatus) ||
      !accountIsActive(targetProfile.data()?.accountStatus)
    ) {
      throw new functions.https.HttpsError("failed-precondition", "Contact request is unavailable while an account is not active");
    }
    if (!matchSnap.exists) {
      throw new functions.https.HttpsError("failed-precondition", "Mutual match required");
    }
    if (outgoingBlock.exists || incomingBlock.exists) {
      throw new functions.https.HttpsError("permission-denied", "Contact request is unavailable for this member");
    }
    if (
      requesterRelation.data()?.profileHidden === true ||
      targetRelation.data()?.profileHidden === true ||
      targetRelation.data()?.contactHidden === true
    ) {
      throw new functions.https.HttpsError("permission-denied", "Contact request is unavailable for this privacy relationship");
    }

    const visibility = String(targetSettings.data()?.contactVisibility || "selected_people");
    if (visibility === "nobody") {
      throw new functions.https.HttpsError("permission-denied", "This member is not accepting contact sharing");
    }
    if (visibility === "mutual_matches") {
      return { status: "AUTO_SHARE_ALLOWED", canReveal: true };
    }

    const grant = existingGrant.data() || {};
    if (existingGrant.exists && grant.phoneAllowed === true) {
      return { status: "APPROVED", canReveal: true };
    }

    const request = existingRequest.data() || {};
    const status = String(request.status || "");
    if (status === "PENDING") return { status: "PENDING", canReveal: false };
    if (status === "APPROVED") return { status: "APPROVED", canReveal: existingGrant.exists };
    if (status === "DECLINED") {
      const updatedAt = request.updatedAt instanceof admin.firestore.Timestamp
        ? request.updatedAt.toMillis()
        : 0;
      if (updatedAt > Date.now() - CONTACT_REQUEST_COOLDOWN_MS) {
        throw new functions.https.HttpsError(
          "resource-exhausted",
          "Please wait before sending another contact request to this member"
        );
      }
    }

    tx.set(requestRef, {
      requesterUid,
      targetUid,
      status: "PENDING",
      requestedTypes: ["phone"],
      createdAt: status ? request.createdAt || admin.firestore.FieldValue.serverTimestamp()
        : admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      respondedAt: admin.firestore.FieldValue.delete(),
    }, { merge: true });
    return { status: "PENDING", canReveal: false };
  });

  return result;
});

/** Target member approves or declines an incoming contact request. */
export const respondContactAccess = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const targetUid = context.auth?.uid;
  if (!targetUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const requesterUid = typeof data?.requesterUid === "string" ? data.requesterUid.trim() : "";
  const approve = data?.approve === true;
  if (!requesterUid || requesterUid === targetUid || requesterUid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid requester profile");
  }

  const requestRef = contactRequestRef(requesterUid, targetUid);
  const grant = grantRef(targetUid, requesterUid);
  return db.runTransaction(async (tx) => {
    const [
      targetProfile,
      requesterProfile,
      requestSnap,
      outgoingBlock,
      incomingBlock,
      targetSettings,
    ] = await Promise.all([
      tx.get(db.collection("users").doc(targetUid)),
      tx.get(db.collection("users").doc(requesterUid)),
      tx.get(requestRef),
      tx.get(db.collection("blocks").doc(targetUid).collection("blocked").doc(requesterUid)),
      tx.get(db.collection("blocks").doc(requesterUid).collection("blocked").doc(targetUid)),
      tx.get(db.collection("privacySettings").doc(targetUid)),
    ]);

    if (!targetProfile.exists || !requesterProfile.exists) {
      throw new functions.https.HttpsError("not-found", "Profile not found");
    }
    if (
      !accountIsActive(targetProfile.data()?.accountStatus) ||
      !accountIsActive(requesterProfile.data()?.accountStatus)
    ) {
      throw new functions.https.HttpsError("failed-precondition", "Contact request is unavailable while an account is not active");
    }
    if (!requestSnap.exists || requestSnap.data()?.targetUid !== targetUid ||
        requestSnap.data()?.requesterUid !== requesterUid) {
      throw new functions.https.HttpsError("not-found", "Contact request not found");
    }
    if (String(requestSnap.data()?.status || "") !== "PENDING") {
      return { status: String(requestSnap.data()?.status || "UNKNOWN") };
    }
    if (outgoingBlock.exists || incomingBlock.exists) {
      throw new functions.https.HttpsError("permission-denied", "Contact request is unavailable after a block");
    }
    const visibility = String(targetSettings.data()?.contactVisibility || "selected_people");
    if (visibility === "nobody") {
      throw new functions.https.HttpsError("failed-precondition", "Contact sharing is disabled");
    }

    const now = admin.firestore.FieldValue.serverTimestamp();
    tx.update(requestRef, {
      status: approve ? "APPROVED" : "DECLINED",
      updatedAt: now,
      respondedAt: now,
    });
    if (approve) {
      tx.set(grant, {
        viewerUid: requesterUid,
        phoneAllowed: true,
        whatsappAllowed: false,
        grantedAt: now,
        updatedAt: now,
        source: "contact_request",
      }, { merge: true });
    } else {
      tx.delete(grant);
    }
    return { status: approve ? "APPROVED" : "DECLINED" };
  });
});


/**
 * Returns current published-photo access for one viewer without exposing another member's raw
 * privacySettings document. Storage Rules remain the byte-level authority.
 */
export const getProfilePhotoAccess = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const viewerUid = context.auth?.uid;
  if (!viewerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const targetUid = typeof data?.targetUid === "string" ? data.targetUid.trim() : "";
  if (!targetUid || targetUid === viewerUid || targetUid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid target profile");
  }

  const [
    viewer,
    target,
    outgoingBlock,
    incomingBlock,
    viewerRelation,
    targetRelation,
    targetSettings,
    viewerInterest,
    targetInterest,
    grant,
    request,
  ] = await Promise.all([
    db.collection("users").doc(viewerUid).get(),
    db.collection("users").doc(targetUid).get(),
    db.collection("blocks").doc(viewerUid).collection("blocked").doc(targetUid).get(),
    db.collection("blocks").doc(targetUid).collection("blocked").doc(viewerUid).get(),
    relationRef(viewerUid, targetUid).get(),
    relationRef(targetUid, viewerUid).get(),
    db.collection("privacySettings").doc(targetUid).get(),
    interestRef(viewerUid, targetUid).get(),
    interestRef(targetUid, viewerUid).get(),
    photoGrantRef(targetUid, viewerUid).get(),
    photoRequestRef(viewerUid, targetUid).get(),
  ]);

  if (!viewer.exists || !target.exists ||
      !accountIsActive(viewer.data()?.accountStatus) ||
      !accountIsActive(target.data()?.accountStatus)) {
    throw new functions.https.HttpsError("failed-precondition", "Profile is unavailable");
  }
  if (outgoingBlock.exists || incomingBlock.exists ||
      viewerRelation.data()?.profileHidden === true ||
      targetRelation.data()?.profileHidden === true) {
    throw new functions.https.HttpsError("permission-denied", "Profile is unavailable");
  }

  const visibility = normalizeProfilePhotoVisibility(targetSettings.data()?.photoVisibility);
  const mutualInterest = viewerInterest.exists && targetInterest.exists;
  const explicitGrant = grant.exists;
  const requesterInterested = viewerInterest.exists;
  return {
    visibility,
    canView: canViewPublishedPhoto(visibility, mutualInterest, explicitGrant),
    canRequest: canRequestPhotoAccess(
      visibility,
      requesterInterested,
      mutualInterest,
      explicitGrant
    ),
    requestStatus: request.exists ? String(request.data()?.status || "") : "",
  };
});

/**
 * Owner-only profile-level privacy for the currently published primary photo.
 * HIDDEN revokes every explicit grant immediately. Existing object paths never bypass Storage Rules.
 */
export const setProfilePhotoVisibility = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const ownerUid = context.auth?.uid;
  if (!ownerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const raw = typeof data?.visibility === "string" ? data.visibility.trim().toUpperCase() : "";
  if (!["PUBLIC", "ACCEPTED_ONLY", "HIDDEN"].includes(raw)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid profile photo visibility");
  }
  const visibility = normalizeProfilePhotoVisibility(raw);

  const owner = await db.collection("users").doc(ownerUid).get();
  if (!owner.exists || !accountIsActive(owner.data()?.accountStatus)) {
    throw new functions.https.HttpsError("failed-precondition", "Profile is unavailable");
  }

  await db.collection("privacySettings").doc(ownerUid).set({
    photoVisibility: visibility,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });

  if (visibility === "HIDDEN") {
    await deleteQuery(db.collection("photoGrants").doc(ownerUid).collection("viewers"));
  }
  return { success: true, visibility };
});

/**
 * Request access to an ACCEPTED_ONLY published photo. The requester must already have expressed
 * interest. Mutual-interest pairs and explicit grants already have access and do not need a request.
 */
export const requestProfilePhotoAccess = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const requesterUid = context.auth?.uid;
  if (!requesterUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const targetUid = typeof data?.targetUid === "string" ? data.targetUid.trim() : "";
  if (!targetUid || targetUid === requesterUid || targetUid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid target profile");
  }

  const requestRef = photoRequestRef(requesterUid, targetUid);
  return db.runTransaction(async (tx) => {
    const [
      requester,
      target,
      outgoingBlock,
      incomingBlock,
      requesterRelation,
      targetRelation,
      targetSettings,
      forwardInterest,
      reverseInterest,
      existingGrant,
      existingRequest,
    ] = await Promise.all([
      tx.get(db.collection("users").doc(requesterUid)),
      tx.get(db.collection("users").doc(targetUid)),
      tx.get(db.collection("blocks").doc(requesterUid).collection("blocked").doc(targetUid)),
      tx.get(db.collection("blocks").doc(targetUid).collection("blocked").doc(requesterUid)),
      tx.get(relationRef(requesterUid, targetUid)),
      tx.get(relationRef(targetUid, requesterUid)),
      tx.get(db.collection("privacySettings").doc(targetUid)),
      tx.get(interestRef(requesterUid, targetUid)),
      tx.get(interestRef(targetUid, requesterUid)),
      tx.get(photoGrantRef(targetUid, requesterUid)),
      tx.get(requestRef),
    ]);

    if (!requester.exists || !target.exists) {
      throw new functions.https.HttpsError("not-found", "Profile not found");
    }
    if (!accountIsActive(requester.data()?.accountStatus) ||
        !accountIsActive(target.data()?.accountStatus)) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Photo request is unavailable while an account is not active"
      );
    }
    if (outgoingBlock.exists || incomingBlock.exists ||
        requesterRelation.data()?.profileHidden === true ||
        targetRelation.data()?.profileHidden === true) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Photo request is unavailable for this privacy relationship"
      );
    }

    const visibility = normalizeProfilePhotoVisibility(targetSettings.data()?.photoVisibility);
    const mutualInterest = forwardInterest.exists && reverseInterest.exists;
    const explicitGrant = existingGrant.exists;
    if (canViewPublishedPhoto(visibility, mutualInterest, explicitGrant)) {
      return { status: "ALREADY_ALLOWED", canView: true };
    }
    if (!canRequestPhotoAccess(
      visibility,
      forwardInterest.exists,
      mutualInterest,
      explicitGrant
    )) {
      if (visibility === "HIDDEN") {
        throw new functions.https.HttpsError(
          "permission-denied",
          "This member is not accepting photo requests"
        );
      }
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Express interest before requesting photo access"
      );
    }

    const existing = existingRequest.data() || {};
    const status = String(existing.status || "");
    if (status === "PENDING") return { status: "PENDING", canView: false };
    if (status === "APPROVED") {
      return { status: "APPROVED", canView: existingGrant.exists };
    }
    if (status === "DECLINED") {
      const updatedAt = existing.updatedAt instanceof admin.firestore.Timestamp
        ? existing.updatedAt.toMillis()
        : 0;
      if (updatedAt > Date.now() - PHOTO_REQUEST_COOLDOWN_MS) {
        throw new functions.https.HttpsError(
          "resource-exhausted",
          "Please wait before sending another photo request to this member"
        );
      }
    }

    const now = admin.firestore.FieldValue.serverTimestamp();
    tx.set(requestRef, {
      requesterUid,
      targetUid,
      status: "PENDING",
      createdAt: status ? existing.createdAt || now : now,
      updatedAt: now,
      respondedAt: admin.firestore.FieldValue.delete(),
    }, { merge: true });
    return { status: "PENDING", canView: false };
  });
});

/** Owner approves or declines an incoming profile-photo request. */
export const respondProfilePhotoAccess = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const targetUid = context.auth?.uid;
  if (!targetUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const requesterUid = typeof data?.requesterUid === "string" ? data.requesterUid.trim() : "";
  const approve = data?.approve === true;
  if (!requesterUid || requesterUid === targetUid || requesterUid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid requester profile");
  }

  const requestRef = photoRequestRef(requesterUid, targetUid);
  const grantRef = photoGrantRef(targetUid, requesterUid);
  return db.runTransaction(async (tx) => {
    const [
      owner,
      requester,
      request,
      outgoingBlock,
      incomingBlock,
      ownerRelation,
      requesterRelation,
      settings,
    ] = await Promise.all([
      tx.get(db.collection("users").doc(targetUid)),
      tx.get(db.collection("users").doc(requesterUid)),
      tx.get(requestRef),
      tx.get(db.collection("blocks").doc(targetUid).collection("blocked").doc(requesterUid)),
      tx.get(db.collection("blocks").doc(requesterUid).collection("blocked").doc(targetUid)),
      tx.get(relationRef(targetUid, requesterUid)),
      tx.get(relationRef(requesterUid, targetUid)),
      tx.get(db.collection("privacySettings").doc(targetUid)),
    ]);

    if (!owner.exists || !requester.exists ||
        !accountIsActive(owner.data()?.accountStatus) ||
        !accountIsActive(requester.data()?.accountStatus)) {
      throw new functions.https.HttpsError("failed-precondition", "Profile is unavailable");
    }
    if (!request.exists ||
        request.data()?.requesterUid !== requesterUid ||
        request.data()?.targetUid !== targetUid) {
      throw new functions.https.HttpsError("not-found", "Photo request not found");
    }
    if (String(request.data()?.status || "") !== "PENDING") {
      return { status: String(request.data()?.status || "UNKNOWN") };
    }
    if (outgoingBlock.exists || incomingBlock.exists ||
        ownerRelation.data()?.profileHidden === true ||
        requesterRelation.data()?.profileHidden === true) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Photo request is unavailable for this privacy relationship"
      );
    }
    const visibility = normalizeProfilePhotoVisibility(settings.data()?.photoVisibility);
    if (approve && visibility !== "ACCEPTED_ONLY") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Photo approval is unavailable for the current visibility setting"
      );
    }

    const now = admin.firestore.FieldValue.serverTimestamp();
    tx.update(requestRef, {
      status: approve ? "APPROVED" : "DECLINED",
      updatedAt: now,
      respondedAt: now,
    });
    if (approve) {
      tx.set(grantRef, {
        viewerUid: requesterUid,
        grantedAt: now,
        updatedAt: now,
        source: "photo_request",
      }, { merge: false });
    } else {
      tx.delete(grantRef);
    }
    return { status: approve ? "APPROVED" : "DECLINED" };
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
      deleteQuery(db.collection("photoGrants").doc(uid).collection("viewers")),
      deleteQuery(db.collectionGroup("viewers").where("viewerUid", "==", uid)),
      deleteQuery(db.collection("photoAccessRequests").where("requesterUid", "==", uid)),
      deleteQuery(db.collection("photoAccessRequests").where("targetUid", "==", uid)),
      db.collection("privacySettings").doc(uid).delete(),
    ]);
    await Promise.all([
      db.collection("privacyRelations").doc(uid).delete().catch(() => undefined),
      db.collection("contactGrants").doc(uid).delete().catch(() => undefined),
      db.collection("photoGrants").doc(uid).delete().catch(() => undefined),
    ]);
  });
