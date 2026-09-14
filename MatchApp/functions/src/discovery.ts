import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import { db, requireAppCheck } from "./shared";

const SCAN_LIMIT = 60;
const RETURN_LIMIT = 20;

const PUBLIC_PROFILE_FIELDS = [
  "firebaseUid", "displayName", "age", "gender", "lookingFor", "city", "bio",
  "rasi", "nakshatra", "religion", "motherTongue", "education", "profession",
  "maritalStatus", "heightCm", "isVerified", "isPremium", "profileViewCount",
  "caste", "state", "subCaste", "gothra", "incomeBand", "diet", "familyType",
  "fatherOccupation", "motherOccupation", "siblings", "smoking", "drinking",
  "personalityType", "hobbies", "spokenLanguages", "videoUrl", "residentialStatus",
  "hasChildren", "boostActiveUntil", "nativeState", "countryOfResidence", "visaStatus",
  "willingToRelocate", "createdAt", "lastActiveAt", "isIncognito", "ageBucket",
  "familyValues", "aboutFamily", "manglik", "weight", "complexion", "physicalStatus",
  "birthPlace", "familyStatus", "educationField", "institution", "graduationYear",
  "occupationCategory", "employer", "employerType", "citizenship", "isNRI",
  "fitnessActivities", "matrimonyId", "photoUrl", "voiceBioUrl", "profileCompleteness",
  "verificationLevel", "stealthMode", "showLastActive", "showHoroscope",
  "incomeDisclosure", "subscriptionPlan", "subscriptionExpiry", "matchScore",
] as const;

function stringValue(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function publicProfile(uid: string, data: FirebaseFirestore.DocumentData): Record<string, unknown> {
  const result: Record<string, unknown> = { firebaseUid: uid };
  for (const field of PUBLIC_PROFILE_FIELDS) {
    if (field === "firebaseUid") continue;
    const value = data[field];
    // Callable payloads should contain plain JSON-compatible profile primitives only.
    if (value === null || ["string", "number", "boolean"].includes(typeof value)) {
      result[field] = value;
    } else if (Array.isArray(value)) {
      result[field] = value.filter((item) => typeof item === "string");
    }
  }
  return result;
}

/**
 * Privacy-safe discovery endpoint.
 *
 * Firestore client list queries cannot safely combine per-document block/stealth rules because
 * rules are not filters. This trusted endpoint performs the scan server-side, derives the viewer
 * identity from Auth, removes either-direction blocks and stealth profiles, and returns only a
 * strict public-field allowlist. Direct profile reads can therefore remain restrictive.
 */
export const discoverProfiles = functions
  .runWith({ timeoutSeconds: 30, memory: "256MB" })
  .https.onCall(async (data, context) => {
    requireAppCheck(context);
    const viewerUid = context.auth?.uid;
    if (!viewerUid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

    const ageMinRaw = Number(data?.ageMin ?? 18);
    const ageMaxRaw = Number(data?.ageMax ?? 70);
    const ageMin = Math.max(18, Math.min(99, Number.isFinite(ageMinRaw) ? Math.trunc(ageMinRaw) : 18));
    const ageMax = Math.max(ageMin, Math.min(99, Number.isFinite(ageMaxRaw) ? Math.trunc(ageMaxRaw) : 70));
    const cursor = typeof data?.cursor === "string" && data.cursor.length <= 128 ? data.cursor : "";

    const viewerDoc = await db.collection("users").doc(viewerUid).get();
    if (!viewerDoc.exists) throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");
    const viewer = viewerDoc.data() || {};
    const viewerGender = stringValue(viewer.gender).toUpperCase();
    const viewerLookingFor = stringValue(viewer.lookingFor).toUpperCase() || "ANY";

    let query: FirebaseFirestore.Query = db.collection("users")
      .orderBy("lastActiveAt", "desc")
      .limit(SCAN_LIMIT);

    if (cursor) {
      const cursorDoc = await db.collection("users").doc(cursor).get();
      if (cursorDoc.exists) query = query.startAfter(cursorDoc);
    }

    const [scan, outgoingBlocks] = await Promise.all([
      query.get(),
      db.collection("blocks").doc(viewerUid).collection("blocked").get(),
    ]);
    const outgoing = new Set(outgoingBlocks.docs.map((doc) => doc.id));

    const candidates = scan.docs.filter((doc) => doc.id !== viewerUid && !outgoing.has(doc.id));
    const reverseBlockRefs = candidates.map((doc) =>
      db.collection("blocks").doc(doc.id).collection("blocked").doc(viewerUid)
    );
    const reverseBlocked = new Set<string>();
    if (reverseBlockRefs.length > 0) {
      const reverseDocs = await db.getAll(...reverseBlockRefs);
      reverseDocs.forEach((doc, index) => {
        if (doc.exists) reverseBlocked.add(candidates[index].id);
      });
    }

    const profiles: Record<string, unknown>[] = [];
    for (const doc of candidates) {
      if (profiles.length >= RETURN_LIMIT) break;
      if (reverseBlocked.has(doc.id)) continue;
      const candidate = doc.data();
      if (candidate.stealthMode === true) continue;

      const candidateAge = Number(candidate.age || 0);
      if (!Number.isFinite(candidateAge) || candidateAge < ageMin || candidateAge > ageMax) continue;

      const candidateGender = stringValue(candidate.gender).toUpperCase();
      const candidateLookingFor = stringValue(candidate.lookingFor).toUpperCase() || "ANY";
      const viewerAccepts = viewerLookingFor === "ANY" || viewerLookingFor === candidateGender;
      const candidateAccepts = candidateLookingFor === "ANY" || candidateLookingFor === viewerGender;
      if (!viewerAccepts || !candidateAccepts) continue;

      profiles.push(publicProfile(doc.id, candidate));
    }

    const nextCursor = scan.docs.length === SCAN_LIMIT ? scan.docs[scan.docs.length - 1].id : null;
    return { profiles, nextCursor };
  });
