import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";

function isHindu(religion: unknown): boolean {
  return typeof religion === "string" && religion.trim().toLowerCase() === "hindu";
}

/**
 * Returns only the minimum horoscope fields a viewer is currently allowed to use for a
 * compatibility check. Birth date/time/place and all other private profile data remain private.
 *
 * A stealth member is still inspectable by the recipient of an interest that the stealth member
 * explicitly sent. Blocks and the member's per-person profile hide always override that exception.
 */
export const getSharedHoroscope = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const viewerUid = context.auth?.uid;
  if (!viewerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const targetUid = typeof data?.targetUid === "string" ? data.targetUid.trim() : "";
  if (!targetUid || targetUid === viewerUid || targetUid.length > 128) {
    throw new functions.https.HttpsError("invalid-argument", "A valid target member is required");
  }

  const [viewerProfile, targetProfile, viewerPrivate, targetPrivate, viewerBlocked, targetBlocked, targetPrivacy, targetInterest] = await Promise.all([
    db.collection("users").doc(viewerUid).get(),
    db.collection("users").doc(targetUid).get(),
    db.collection("userPrivate").doc(viewerUid).get(),
    db.collection("userPrivate").doc(targetUid).get(),
    db.collection("blocks").doc(viewerUid).collection("blocked").doc(targetUid).get(),
    db.collection("blocks").doc(targetUid).collection("blocked").doc(viewerUid).get(),
    db.collection("privacyRelations").doc(targetUid).collection("members").doc(viewerUid).get(),
    db.collection("interests").doc(`${targetUid}_${viewerUid}`).get(),
  ]);

  if (!viewerProfile.exists || !targetProfile.exists) {
    throw new functions.https.HttpsError("not-found", "Profile is unavailable");
  }
  if (viewerBlocked.exists || targetBlocked.exists || targetPrivacy.data()?.profileHidden === true) {
    throw new functions.https.HttpsError("permission-denied", "Profile is unavailable");
  }

  const targetPublic = targetProfile.data() || {};
  if (targetPublic.stealthMode === true && !targetInterest.exists) {
    throw new functions.https.HttpsError("permission-denied", "Profile is unavailable");
  }
  if (targetPublic.showHoroscope !== true) {
    return { available: false, reason: "not_shared" };
  }

  const viewerPublic = viewerProfile.data() || {};
  if (!isHindu(viewerPublic.religion) || !isHindu(targetPublic.religion)) {
    return { available: false, reason: "not_applicable" };
  }

  const viewerData = viewerPrivate.data() || {};
  const targetData = targetPrivate.data() || {};
  const myRasi = typeof viewerData.rasi === "string" ? viewerData.rasi.trim() : "";
  const myNakshatra = typeof viewerData.nakshatra === "string" ? viewerData.nakshatra.trim() : "";
  const targetRasi = typeof targetData.rasi === "string" ? targetData.rasi.trim() : "";
  const targetNakshatra = typeof targetData.nakshatra === "string" ? targetData.nakshatra.trim() : "";

  if (!myRasi || !myNakshatra) {
    return { available: false, reason: "viewer_incomplete" };
  }
  if (!targetRasi || !targetNakshatra) {
    return { available: false, reason: "target_incomplete" };
  }

  return {
    available: true,
    targetUid,
    targetName: String(targetPublic.displayName || "Member").slice(0, 120),
    myRasi,
    myNakshatra,
    targetRasi,
    targetNakshatra,
  };
});
