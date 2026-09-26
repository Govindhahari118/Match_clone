import * as admin from "firebase-admin";
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
  "residentialStatus", "hasChildren", "nativeState", "countryOfResidence", "visaStatus",
  "willingToRelocate", "createdAt", "isIncognito", "ageBucket", "familyValues", "aboutFamily",
  "weight", "complexion", "physicalStatus", "familyStatus", "educationField", "institution",
  "graduationYear", "occupationCategory", "employer", "employerType", "citizenship", "isNRI",
  "fitnessActivities", "matrimonyId", "photoUrl", "voiceBioUrl", "profileCompleteness",
  "verificationLevel", "stealthMode", "showHoroscope", "incomeDisclosure",
] as const;

function stringValue(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function normalizedSearchValue(value: unknown): string {
  return stringValue(value).trim().toLocaleLowerCase("en-IN");
}

function filterString(data: unknown, key: string, max = 100): string {
  if (!data || typeof data !== "object") return "";
  const value = (data as Record<string, unknown>)[key];
  return typeof value === "string" ? value.trim().slice(0, max) : "";
}

function filterBoolean(data: unknown, key: string): boolean {
  if (!data || typeof data !== "object") return false;
  return (data as Record<string, unknown>)[key] === true;
}

function filterInt(data: unknown, key: string, min: number, max: number): number {
  if (!data || typeof data !== "object") return 0;
  const raw = Number((data as Record<string, unknown>)[key] ?? 0);
  if (!Number.isFinite(raw)) return 0;
  return Math.max(min, Math.min(max, Math.trunc(raw)));
}

function equalsFilter(expected: string, actual: unknown): boolean {
  return !expected || normalizedSearchValue(actual) === expected.toLocaleLowerCase("en-IN");
}

function boolFromAnyFilter(value: string, actual: boolean): boolean {
  const normalized = value.toLocaleLowerCase("en-IN");
  if (!normalized || normalized === "any" || normalized === "don't mind") return true;
  if (normalized.startsWith("yes")) return actual;
  if (normalized.startsWith("no")) return !actual;
  return true;
}

function premiumExpiryMillis(data: FirebaseFirestore.DocumentData): number {
  if (data.premiumUntil instanceof admin.firestore.Timestamp) {
    return data.premiumUntil.toMillis();
  }
  const expiry = Number(data.subscriptionExpiry || 0);
  return Number.isFinite(expiry) ? expiry : 0;
}

function premiumIsActive(data: FirebaseFirestore.DocumentData, now: number): boolean {
  return data.isPremium === true && premiumExpiryMillis(data) > now;
}

function matchesServerFilters(
  candidate: FirebaseFirestore.DocumentData,
  data: unknown,
  now: number
): boolean {
  const textFields: Array<[string, string]> = [
    ["city", "city"], ["state", "state"], ["religion", "religion"],
    ["caste", "caste"], ["subCaste", "subCaste"], ["motherTongue", "motherTongue"],
    ["maritalStatus", "maritalStatus"], ["diet", "diet"], ["educationLevel", "education"],
    ["educationField", "educationField"], ["occupationCategory", "occupationCategory"],
    ["employerType", "employerType"], ["residentialStatus", "residentialStatus"],
    ["nativeState", "nativeState"], ["countryOfResidence", "countryOfResidence"],
    ["citizenship", "citizenship"], ["gothra", "gothra"], ["smoking", "smoking"],
    ["drinking", "drinking"], ["familyType", "familyType"], ["familyStatus", "familyStatus"],
    ["physicalStatus", "physicalStatus"], ["rasi", "rasi"], ["nakshatra", "nakshatra"],
    ["manglik", "manglik"],
  ];
  for (const [requestKey, profileKey] of textFields) {
    if (!equalsFilter(filterString(data, requestKey), candidate[profileKey])) return false;
  }

  const verifiedOnly = filterBoolean(data, "verifiedOnly");
  if (verifiedOnly && candidate.isVerified !== true) return false;
  const verifiedLevel = filterInt(data, "verifiedLevel", 0, 100);
  if (verifiedLevel > 0 && Number(candidate.verificationLevel || 0) < verifiedLevel) return false;
  if (filterBoolean(data, "premiumOnly") && !premiumIsActive(candidate, now)) return false;
  if (filterBoolean(data, "withPhotoOnly") && !stringValue(candidate.photoUrl)) return false;
  if (filterBoolean(data, "willingToRelocate") && candidate.willingToRelocate !== true) return false;

  const hobbies = filterString(data, "hobbies");
  const candidateHobbies = Array.isArray(candidate.hobbies)
    ? candidate.hobbies.filter((item: unknown) => typeof item === "string").join(" ")
    : stringValue(candidate.hobbies);
  if (hobbies && !candidateHobbies.toLocaleLowerCase("en-IN").includes(hobbies.toLocaleLowerCase("en-IN"))) {
    return false;
  }

  const incomeMin = filterString(data, "incomeMin");
  if (incomeMin && !normalizedSearchValue(candidate.incomeBand).includes(incomeMin.toLocaleLowerCase("en-IN"))) {
    return false;
  }
  const incomeMax = filterString(data, "incomeMax");
  if (incomeMax && !normalizedSearchValue(candidate.incomeBand).includes(incomeMax.toLocaleLowerCase("en-IN"))) {
    return false;
  }

  if (!boolFromAnyFilter(filterString(data, "hasChildren"), candidate.hasChildren === true)) return false;
  if (!boolFromAnyFilter(filterString(data, "hasChildrenFilter"), candidate.hasChildren === true)) return false;

  const nriOnly = filterBoolean(data, "nriOnly");
  const isNri = candidate.isNRI === true ||
    (stringValue(candidate.countryOfResidence) &&
     normalizedSearchValue(candidate.countryOfResidence) !== "india");
  if (nriOnly && !isNri) return false;
  const nriStatus = filterString(data, "nriStatus").toLocaleLowerCase("en-IN");
  if (nriStatus === "only" && !isNri) return false;
  if (nriStatus === "exclude" && isNri) return false;

  const recentlyJoinedDays = filterInt(data, "recentlyJoinedDays", 0, 3650);
  if (recentlyJoinedDays > 0) {
    const created = createdAtMillis(candidate);
    if (created <= 0 || now - created > recentlyJoinedDays * 86_400_000) return false;
  }
  const lastActiveDays = filterInt(data, "lastActiveWithinDays", 0, 3650);
  if (lastActiveDays > 0) {
    const raw = candidate.lastActiveAt;
    const lastActive = raw instanceof admin.firestore.Timestamp
      ? raw.toMillis()
      : Number(raw || 0);
    if (!Number.isFinite(lastActive) || lastActive <= 0 ||
        now - lastActive > lastActiveDays * 86_400_000) return false;
  }

  const hasHoroscope = filterString(data, "hasHoroscope").toLocaleLowerCase("en-IN");
  const hasAstrology = stringValue(candidate.rasi).length > 0 && stringValue(candidate.nakshatra).length > 0;
  if (hasHoroscope === "yes" && !hasAstrology) return false;
  if (hasHoroscope === "no" && hasAstrology) return false;

  return true;
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

function publicProfile(
  uid: string,
  data: FirebaseFirestore.DocumentData,
  now: number
): Record<string, unknown> {
  const result: Record<string, unknown> = { firebaseUid: uid };
  for (const field of PUBLIC_PROFILE_FIELDS) {
    if (field === "firebaseUid") continue;
    if (field === "isPremium") {
      result[field] = premiumIsActive(data, now);
      continue;
    }
    const value = data[field];
    if (value instanceof admin.firestore.Timestamp) {
      result[field] = value.toMillis();
    } else if (value === null || ["string", "number", "boolean"].includes(typeof value)) {
      result[field] = value;
    } else if (Array.isArray(value)) {
      result[field] = value.filter((item) => typeof item === "string");
    }
  }
  return result;
}

function createdAtMillis(data: FirebaseFirestore.DocumentData): number {
  const value = data.createdAt;
  if (value instanceof admin.firestore.Timestamp) return value.toMillis();
  const numeric = Number(value || 0);
  return Number.isFinite(numeric) ? numeric : 0;
}

/**
 * Privacy-safe discovery endpoint. Exact usernames are resolved through a server-only unique
 * registry. Boost affects ordering only after ordinary eligibility/privacy filtering and is read
 * from the private server-owned subscription document, never from a public profile field.
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
    if (!viewerDoc.exists) {
      throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");
    }
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
    const subscriptionRefs = candidates.map((doc) => db.collection("subscriptions").doc(doc.id));
    const [reverseDocs, privacyDocs, subscriptionDocs] = await Promise.all([
      reverseBlockRefs.length ? db.getAll(...reverseBlockRefs) : Promise.resolve([]),
      hiddenFromViewerRefs.length ? db.getAll(...hiddenFromViewerRefs) : Promise.resolve([]),
      subscriptionRefs.length ? db.getAll(...subscriptionRefs) : Promise.resolve([]),
    ]);

    const reverseBlocked = new Set<string>();
    const hiddenFromViewer = new Set<string>();
    const boostUntilByUid = new Map<string, number>();
    reverseDocs.forEach((doc, index) => {
      if (doc.exists) reverseBlocked.add(candidates[index].id);
    });
    privacyDocs.forEach((doc, index) => {
      if (doc.exists && doc.data()?.profileHidden === true) hiddenFromViewer.add(candidates[index].id);
    });
    subscriptionDocs.forEach((doc, index) => {
      const raw = Number(doc.data()?.boostUntil || 0);
      boostUntilByUid.set(candidates[index].id, Number.isFinite(raw) ? raw : 0);
    });

    const now = Date.now();
    const rankedCandidates = keyword
      ? candidates
      : candidates.slice().sort((left, right) => {
        const leftBoosted = (boostUntilByUid.get(left.id) || 0) > now ? 1 : 0;
        const rightBoosted = (boostUntilByUid.get(right.id) || 0) > now ? 1 : 0;
        if (leftBoosted !== rightBoosted) return rightBoosted - leftBoosted;
        return createdAtMillis(right.data() || {}) - createdAtMillis(left.data() || {});
      });

    const profiles: Record<string, unknown>[] = [];
    for (const doc of rankedCandidates) {
      if (profiles.length >= RETURN_LIMIT) break;
      if (reverseBlocked.has(doc.id) || hiddenFromViewer.has(doc.id)) continue;
      const candidate = doc.data() || {};
      const accountStatus = stringValue(candidate.accountStatus).toUpperCase() || "ACTIVE";
      if (accountStatus !== "ACTIVE") continue;
      if (candidate.stealthMode === true) continue;

      const candidateAge = Number(candidate.age || 0);
      if (!Number.isFinite(candidateAge) || candidateAge < ageMin || candidateAge > ageMax) continue;

      const candidateGender = stringValue(candidate.gender).toUpperCase();
      const candidateLookingFor = stringValue(candidate.lookingFor).toUpperCase() || "ANY";
      const viewerAccepts = viewerLookingFor === "ANY" || viewerLookingFor === candidateGender;
      const candidateAccepts = candidateLookingFor === "ANY" || candidateLookingFor === viewerGender;
      if (!viewerAccepts || !candidateAccepts) continue;
      if (!matchesKeyword(candidate, keyword)) continue;
      if (!matchesServerFilters(candidate, data, now)) continue;

      profiles.push(publicProfile(doc.id, candidate, now));
    }

    const nextCursor = scan.docs.length === SCAN_LIMIT
      ? scan.docs[scan.docs.length - 1].id
      : null;
    return { profiles, nextCursor };
  });
