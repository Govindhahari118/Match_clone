import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";

const SCAN_LIMIT = 60;
const RETURN_LIMIT = 20;
const MAX_KEYWORD_LENGTH = 64;

// Only fields intentionally safe for another signed-in member may leave this endpoint. Birth
// details, astrology inputs, income, precise activity and billing entitlement stay private.
const PUBLIC_PROFILE_FIELDS = [
  "firebaseUid", "displayName", "username", "age", "gender", "lookingFor", "city", "bio",
  "religion", "motherTongue", "education", "profession", "maritalStatus", "heightCm",
  "isVerified", "isPremium", "profileViewCount", "caste", "state", "subCaste", "gothra",
  "faithTradition", "faithSubTradition", "faithInstitution",
  "diet", "familyType", "fatherOccupation", "motherOccupation", "siblings", "smoking",
  "drinking", "personalityType", "hobbies", "spokenLanguages", "videoUrl",
  "residentialStatus", "hasChildren", "boostActiveUntil", "nativeState", "countryOfResidence",
  "visaStatus", "willingToRelocate", "createdAt", "isIncognito", "ageBucket", "familyValues",
  "aboutFamily", "weight", "complexion", "physicalStatus", "familyStatus", "educationField",
  "institution", "graduationYear", "occupationCategory", "employer", "employerType",
  "citizenship", "isNRI", "fitnessActivities", "matrimonyId", "photoUrl", "voiceBioUrl",
  "profileCompleteness", "verificationLevel", "stealthMode", "showHoroscope", "incomeDisclosure",
] as const;

function stringValue(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function normalizedSearchValue(value: unknown): string {
  return stringValue(value).trim().toLocaleLowerCase("en-IN");
}

function matchesKeyword(data: FirebaseFirestore.DocumentData, keyword: string): boolean {
  if (!keyword) return true;
  const normalized = keyword.replace(/^@+/, "");
  const fields = [
    data.displayName,
    data.username,
    data.matrimonyId,
    data.profession,
    data.city,
    data.state,
  ];
  return fields.some((value) => normalizedSearchValue(value).includes(normalized));
}

function publicProfile(uid: string, data: FirebaseFirestore.DocumentData): Record<string, unknown> {
  const result: Record<string, unknown> = { firebaseUid: uid };
  for (const field of PUBLIC_PROFILE_FIELDS) {
    if (field === "firebaseUid") continue;
    const value = data[field];
    if (value === null || ["string", "number", "boolean"].includes(typeof value)) {
      result[field] = value;
    } else if (Array.isArray(value)) {
      result[field] = value.filter((item) => typeof item === "string");
    }
  }
  return result;
}

/**
 * Privacy-safe discovery endpoint. Exact usernames are resolved through a server-only unique
 * registry, while ordinary name/profile searches are filtered during the candidate scan.
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
    const keyword = typeof data?.keyword === "string"
      ? data.keyword.trim().toLocaleLowerCase("en-IN").slice(0, MAX_KEYWORD_LENGTH)
      : "";
    const normalizedUsername = keyword.replace(/^@+/, "");

    const viewerDoc = await db.collection("users").doc(viewerUid).get();
    if (!viewerDoc.exists) throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");
    const viewer = viewerDoc.data() || {};
    const viewerGender = stringValue(viewer.gender).toUpperCase();
    const viewerLookingFor = stringValue(viewer.lookingFor).toUpperCase() || "ANY";

    let query: FirebaseFirestore.Query = db.collection("users")
      .orderBy("createdAt", "desc")
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

    let exactProfile: FirebaseFirestore.DocumentSnapshot | null = null;
    if (!cursor && normalizedUsername.length >= 3 && /^[a-z0-9][a-z0-9._]{2,29}$/.test(normalizedUsername)) {
      const registry = await db.collection("usernames").doc(normalizedUsername).get();
      const exactUid = registry.exists ? stringValue(registry.data()?.uid) : "";
      if (exactUid && exactUid !== viewerUid && !outgoing.has(exactUid)) {
        const snap = await db.collection("users").doc(exactUid).get();
        if (snap.exists) exactProfile = snap;
      }
    }

    const candidateById = new Map<string, FirebaseFirestore.DocumentSnapshot>();
    if (exactProfile) candidateById.set(exactProfile.id, exactProfile);
    for (const doc of scan.docs) {
      if (doc.id !== viewerUid && !outgoing.has(doc.id) && !candidateById.has(doc.id)) {
        candidateById.set(doc.id, doc);
      }
    }
    const candidates = Array.from(candidateById.values());

    const reverseBlockRefs = candidates.map((doc) =>
      db.collection("blocks").doc(doc.id).collection("blocked").doc(viewerUid)
    );
    const hiddenFromViewerRefs = candidates.map((doc) =>
      db.collection("privacyRelations").doc(doc.id).collection("members").doc(viewerUid)
    );
    const [reverseDocs, privacyDocs] = await Promise.all([
      reverseBlockRefs.length ? db.getAll(...reverseBlockRefs) : Promise.resolve([]),
      hiddenFromViewerRefs.length ? db.getAll(...hiddenFromViewerRefs) : Promise.resolve([]),
    ]);

    const reverseBlocked = new Set<string>();
    const hiddenFromViewer = new Set<string>();
    reverseDocs.forEach((doc, index) => {
      if (doc.exists) reverseBlocked.add(candidates[index].id);
    });
    privacyDocs.forEach((doc, index) => {
      if (doc.exists && doc.data()?.profileHidden === true) hiddenFromViewer.add(candidates[index].id);
    });

    const profiles: Record<string, unknown>[] = [];
    for (const doc of candidates) {
      if (profiles.length >= RETURN_LIMIT) break;
      if (reverseBlocked.has(doc.id) || hiddenFromViewer.has(doc.id)) continue;
      const candidate = doc.data() || {};
      if (candidate.stealthMode === true) continue;

      const candidateAge = Number(candidate.age || 0);
      if (!Number.isFinite(candidateAge) || candidateAge < ageMin || candidateAge > ageMax) continue;

      const candidateGender = stringValue(candidate.gender).toUpperCase();
      const candidateLookingFor = stringValue(candidate.lookingFor).toUpperCase() || "ANY";
      const viewerAccepts = viewerLookingFor === "ANY" || viewerLookingFor === candidateGender;
      const candidateAccepts = candidateLookingFor === "ANY" || candidateLookingFor === viewerGender;
      if (!viewerAccepts || !candidateAccepts) continue;
      if (!matchesKeyword(candidate, keyword)) continue;

      profiles.push(publicProfile(doc.id, candidate));
    }

    const nextCursor = scan.docs.length === SCAN_LIMIT ? scan.docs[scan.docs.length - 1].id : null;
    return { profiles, nextCursor };
  });
