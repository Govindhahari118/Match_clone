import * as admin from "firebase-admin";
import * as crypto from "crypto";
import * as functions from "firebase-functions/v1";
import {
  familyPatchContainsForbiddenFields,
  normalizeFamilyPermissions,
  normalizeFamilyRole,
  sanitizeFamilyProfilePatch,
} from "./familyDelegationPolicy";
import { db, requireAppCheck } from "./shared";
import { accountIsActive } from "./accountStatusPolicy";

const INVITE_TTL_MS = 24 * 60 * 60 * 1000;

function tokenHash(token: string): string {
  return crypto.createHash("sha256").update(token, "utf8").digest("hex");
}

function cleanUid(value: unknown): string {
  const uid = typeof value === "string" ? value.trim() : "";
  if (!uid || uid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "A valid account is required");
  }
  return uid;
}

async function activeDelegation(
  ownerUid: string,
  delegateUid: string,
  required: "VIEW_PROFILE" | "EDIT_PROFILE"
): Promise<FirebaseFirestore.DocumentSnapshot> {
  const ref = db.collection("familyDelegates").doc(ownerUid).collection("members").doc(delegateUid);
  const [snap, owner, delegate] = await Promise.all([
    ref.get(),
    db.collection("users").doc(ownerUid).get(),
    db.collection("users").doc(delegateUid).get(),
  ]);
  const data = snap.data() || {};
  if (
    !snap.exists ||
    data.active !== true ||
    !Array.isArray(data.permissions) ||
    !data.permissions.includes(required)
  ) {
    throw new functions.https.HttpsError("permission-denied", "Family access is not authorized");
  }
  if (
    !owner.exists ||
    !delegate.exists ||
    !accountIsActive(owner.data()?.accountStatus) ||
    !accountIsActive(delegate.data()?.accountStatus)
  ) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Family access is unavailable while either account is not active"
    );
  }
  return snap;
}

export const createFamilyDelegateInvite = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const ownerUid = context.auth?.uid;
  if (!ownerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const owner = await db.collection("users").doc(ownerUid).get();
  if (!owner.exists || String(owner.data()?.accountStatus || "ACTIVE").toUpperCase() !== "ACTIVE") {
    throw new functions.https.HttpsError("failed-precondition", "Active profile required");
  }

  const role = normalizeFamilyRole(data?.role);
  if (!role) throw new functions.https.HttpsError("invalid-argument", "Invalid family role");
  const permissions = normalizeFamilyPermissions(data?.permissions);
  if (!permissions.includes("VIEW_PROFILE")) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Family access must include profile viewing"
    );
  }

  const token = crypto.randomBytes(32).toString("base64url");
  const hash = tokenHash(token);
  const now = Date.now();
  await db.collection("familyInvites").doc(hash).set({
    ownerUid,
    role,
    permissions,
    status: "OPEN",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    expiresAt: admin.firestore.Timestamp.fromMillis(now + INVITE_TTL_MS),
  });

  await db.collection("familyAccessAudit").add({
    ownerUid,
    actorUid: ownerUid,
    action: "INVITE_CREATED",
    role,
    permissions,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return {
    inviteToken: token,
    inviteId: hash,
    expiresAtMillis: now + INVITE_TTL_MS,
    role,
    permissions,
  };
});

export const acceptFamilyDelegateInvite = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const delegateUid = context.auth?.uid;
  if (!delegateUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const token = typeof data?.inviteToken === "string" ? data.inviteToken.trim() : "";
  if (token.length < 32 || token.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid family invite");
  }
  const inviteRef = db.collection("familyInvites").doc(tokenHash(token));

  const result = await db.runTransaction(async (tx) => {
    const invite = await tx.get(inviteRef);
    if (!invite.exists) throw new functions.https.HttpsError("not-found", "Invite not found");
    const value = invite.data() || {};
    const ownerUid = String(value.ownerUid || "");
    if (!ownerUid || ownerUid === delegateUid) {
      throw new functions.https.HttpsError("failed-precondition", "Invite cannot be accepted");
    }
    if (String(value.status || "") !== "OPEN") {
      throw new functions.https.HttpsError("failed-precondition", "Invite is no longer active");
    }
    const expiresAt = value.expiresAt instanceof admin.firestore.Timestamp
      ? value.expiresAt.toMillis()
      : 0;
    if (expiresAt <= Date.now()) {
      throw new functions.https.HttpsError("deadline-exceeded", "Invite has expired");
    }
    const role = normalizeFamilyRole(value.role);
    const permissions = normalizeFamilyPermissions(value.permissions);
    if (!role || !permissions.includes("VIEW_PROFILE")) {
      throw new functions.https.HttpsError("failed-precondition", "Invite permissions are invalid");
    }

    const owner = await tx.get(db.collection("users").doc(ownerUid));
    const delegate = await tx.get(db.collection("users").doc(delegateUid));
    if (!owner.exists || !accountIsActive(owner.data()?.accountStatus)) {
      throw new functions.https.HttpsError("failed-precondition", "Managed profile is unavailable");
    }
    if (!delegate.exists || !accountIsActive(delegate.data()?.accountStatus)) {
      throw new functions.https.HttpsError("failed-precondition", "Delegate account must be active");
    }

    const memberRef = db.collection("familyDelegates")
      .doc(ownerUid)
      .collection("members")
      .doc(delegateUid);
    tx.set(memberRef, {
      ownerUid,
      delegateUid,
      role,
      permissions,
      active: true,
      acceptedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: false });
    tx.update(inviteRef, {
      status: "ACCEPTED",
      acceptedBy: delegateUid,
      acceptedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    tx.create(db.collection("familyAccessAudit").doc(), {
      ownerUid,
      actorUid: delegateUid,
      delegateUid,
      action: "INVITE_ACCEPTED",
      role,
      permissions,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    return { ownerUid, role, permissions };
  });

  return { success: true, ...result };
});

export const cancelFamilyDelegateInvite = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const ownerUid = context.auth?.uid;
  if (!ownerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const inviteId = typeof data?.inviteId === "string" ? data.inviteId.trim() : "";
  if (!/^[a-f0-9]{64}$/.test(inviteId)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid family invite");
  }

  const inviteRef = db.collection("familyInvites").doc(inviteId);
  await db.runTransaction(async (tx) => {
    const invite = await tx.get(inviteRef);
    if (!invite.exists || String(invite.data()?.ownerUid || "") !== ownerUid) {
      throw new functions.https.HttpsError("not-found", "Family invite not found");
    }
    if (String(invite.data()?.status || "") !== "OPEN") return;

    tx.update(inviteRef, {
      status: "CANCELLED",
      cancelledAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    tx.create(db.collection("familyAccessAudit").doc(), {
      ownerUid,
      actorUid: ownerUid,
      action: "INVITE_CANCELLED",
      inviteId,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  return { success: true };
});

export const revokeFamilyDelegate = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const ownerUid = context.auth?.uid;
  if (!ownerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const delegateUid = cleanUid(data?.delegateUid);
  if (delegateUid === ownerUid) {
    throw new functions.https.HttpsError("invalid-argument", "Cannot revoke self");
  }
  const ref = db.collection("familyDelegates").doc(ownerUid).collection("members").doc(delegateUid);
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists || snap.data()?.active !== true) return;
    tx.update(ref, {
      active: false,
      revokedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    tx.create(db.collection("familyAccessAudit").doc(), {
      ownerUid,
      actorUid: ownerUid,
      delegateUid,
      action: "ACCESS_REVOKED",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });
  return { success: true };
});

export const listMyFamilyAccess = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const [owned, delegated, inviteSnapshot] = await Promise.all([
    db.collection("familyDelegates").doc(uid).collection("members").get(),
    db.collectionGroup("members").where("delegateUid", "==", uid).get(),
    db.collection("familyInvites").where("ownerUid", "==", uid).limit(50).get(),
  ]);
  const now = Date.now();
  return {
    delegates: owned.docs.map((doc) => ({
      delegateUid: doc.id,
      role: String(doc.data().role || ""),
      permissions: Array.isArray(doc.data().permissions) ? doc.data().permissions : [],
      active: doc.data().active === true,
    })),
    pendingInvites: inviteSnapshot.docs
      .filter((doc) => {
        const value = doc.data();
        const expiresAt = value.expiresAt instanceof admin.firestore.Timestamp
          ? value.expiresAt.toMillis()
          : 0;
        return String(value.status || "") === "OPEN" && expiresAt > now;
      })
      .map((doc) => ({
        inviteId: doc.id,
        role: String(doc.data().role || ""),
        permissions: Array.isArray(doc.data().permissions) ? doc.data().permissions : [],
        expiresAtMillis: doc.data().expiresAt instanceof admin.firestore.Timestamp
          ? doc.data().expiresAt.toMillis()
          : 0,
      })),
    managedProfiles: delegated.docs
      .filter((doc) => doc.data().active === true)
      .map((doc) => ({
        ownerUid: String(doc.data().ownerUid || ""),
        role: String(doc.data().role || ""),
        permissions: Array.isArray(doc.data().permissions) ? doc.data().permissions : [],
      })),
  };
});

export const getFamilyManagedProfile = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const delegateUid = context.auth?.uid;
  if (!delegateUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const ownerUid = cleanUid(data?.ownerUid);
  await activeDelegation(ownerUid, delegateUid, "VIEW_PROFILE");
  const profile = await db.collection("users").doc(ownerUid).get();
  if (!profile.exists) throw new functions.https.HttpsError("not-found", "Profile not found");
  const value = profile.data() || {};
  return {
    profile: {
      uid: ownerUid,
      displayName: String(value.displayName || ""),
      city: String(value.city || ""),
      bio: String(value.bio || ""),
      education: String(value.education || ""),
      profession: String(value.profession || ""),
      maritalStatus: String(value.maritalStatus || ""),
      heightCm: Number(value.heightCm || 0),
      diet: String(value.diet || ""),
      familyType: String(value.familyType || ""),
      familyStatus: String(value.familyStatus || ""),
      familyValues: String(value.familyValues || ""),
      fatherOccupation: String(value.fatherOccupation || ""),
      motherOccupation: String(value.motherOccupation || ""),
      siblings: Number(value.siblings || 0),
      nativeState: String(value.nativeState || ""),
      countryOfResidence: String(value.countryOfResidence || ""),
      citizenship: String(value.citizenship || ""),
      isNRI: value.isNRI === true,
      willingToRelocate: value.willingToRelocate === true,
      hobbies: Array.isArray(value.hobbies) ? value.hobbies : [],
      spokenLanguages: Array.isArray(value.spokenLanguages) ? value.spokenLanguages : [],
      profileRevision: Number(value.profileRevision || 0),
    },
  };
});

export const updateFamilyManagedProfile = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const delegateUid = context.auth?.uid;
  if (!delegateUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const ownerUid = cleanUid(data?.ownerUid);
  await activeDelegation(ownerUid, delegateUid, "EDIT_PROFILE");

  const patch = sanitizeFamilyProfilePatch(data?.patch);
  if (Object.keys(patch).length === 0 || familyPatchContainsForbiddenFields(data?.patch, patch)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Only delegated non-sensitive profile fields may be edited"
    );
  }
  const expectedRevision = Number(data?.expectedRevision);
  if (!Number.isInteger(expectedRevision) || expectedRevision < 0) {
    throw new functions.https.HttpsError("invalid-argument", "Profile revision is required");
  }

  const profileRef = db.collection("users").doc(ownerUid);
  const auditRef = db.collection("familyAccessAudit").doc();
  const nextRevision = await db.runTransaction(async (tx) => {
    const profile = await tx.get(profileRef);
    if (!profile.exists) throw new functions.https.HttpsError("not-found", "Profile not found");
    const currentRevision = Number(profile.data()?.profileRevision || 0);
    if (currentRevision !== expectedRevision) {
      throw new functions.https.HttpsError(
        "aborted",
        "Profile changed on another device. Refresh before editing again."
      );
    }
    const revision = currentRevision + 1;
    tx.update(profileRef, {
      ...patch,
      profileRevision: revision,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    tx.create(auditRef, {
      ownerUid,
      actorUid: delegateUid,
      delegateUid,
      action: "PROFILE_EDITED",
      fields: Object.keys(patch).sort(),
      previousRevision: currentRevision,
      nextRevision: revision,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    return revision;
  });

  return { success: true, profileRevision: nextRevision };
});
