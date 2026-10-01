import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";
import {
  activityVisibilityAllows,
  normalizeActivityVisibility,
} from "./activityVisibilityPolicy";
import {
  bilateralPreferenceMatch,
  STRONG_MUTUAL_MIN_CRITERIA,
  normalizePartnerPreferences,
  strictPreferencesAllow,
} from "./partnerPreferencesPolicy";
import { recordRecommendationImpressionBatch } from "./recommendationFeedback";
import { hasActiveConsent } from "./consent";
import {
  DISCOVERY_RANKING_VERSION,
  behavioralAdjustment,
  blendedRecommendationRelevance,
} from "./recommendationPolicy";
import {
  discoveryActorReady,
  discoveryCandidateReady,
  profileFreshEnough,
} from "./discoveryEligibilityPolicy";
import { resolveMembershipState } from "./membershipAuthority";
import { horoscopeCompatibility } from "./horoscopeCompatibilityPolicy";
import { questionnaireCompatibility } from "./questionnaireCompatibilityPolicy";

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

function isHinduReligion(value: unknown): boolean {
  return normalizedSearchValue(value) === "hindu";
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

function matchesServerFilters(
  candidate: FirebaseFirestore.DocumentData,
  data: unknown,
  now: number,
  visibleLastActiveAt: number,
  premiumActive: boolean
): boolean {
  const textFields: Array<[string, string]> = [
    ["city", "city"], ["state", "state"], ["religion", "religion"],
    ["caste", "caste"], ["subCaste", "subCaste"], ["faithTradition", "faithTradition"],
    ["faithSubTradition", "faithSubTradition"], ["faithInstitution", "faithInstitution"],
    ["motherTongue", "motherTongue"],
    ["maritalStatus", "maritalStatus"], ["diet", "diet"], ["educationLevel", "education"],
    ["educationField", "educationField"], ["occupationCategory", "occupationCategory"],
    ["employerType", "employerType"], ["residentialStatus", "residentialStatus"],
    ["visaStatus", "visaStatus"], ["nativeState", "nativeState"], ["countryOfResidence", "countryOfResidence"],
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
  if (filterBoolean(data, "premiumOnly") && !premiumActive) return false;
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
  const incomeMax = filterString(data, "incomeMax");
  if ((incomeMin || incomeMax) && stringValue(candidate.incomeDisclosure).toLowerCase() === "hidden") {
    return false;
  }
  if (incomeMin && !normalizedSearchValue(candidate.incomeBand).includes(incomeMin.toLocaleLowerCase("en-IN"))) {
    return false;
  }
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
    if (!Number.isFinite(visibleLastActiveAt) || visibleLastActiveAt <= 0 ||
        now - visibleLastActiveAt > lastActiveDays * 86_400_000) return false;
  }

  const hasHoroscope = filterString(data, "hasHoroscope").toLocaleLowerCase("en-IN");
  const astrologyFilterRequested =
    filterString(data, "rasi").length > 0 ||
    filterString(data, "nakshatra").length > 0 ||
    filterString(data, "manglik").length > 0 ||
    hasHoroscope === "yes" ||
    hasHoroscope === "no";
  if (astrologyFilterRequested && candidate.showHoroscope === false) return false;
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
  premiumActive: boolean
): Record<string, unknown> {
  const result: Record<string, unknown> = { firebaseUid: uid };
  for (const field of PUBLIC_PROFILE_FIELDS) {
    if (field === "firebaseUid") continue;
    if (field === "isPremium") {
      result[field] = premiumActive;
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

function needsPrivateFilterData(data: unknown): boolean {
  const horoscope = filterString(data, "hasHoroscope").toLocaleLowerCase("en-IN");
  return filterString(data, "incomeMin").length > 0 ||
    filterString(data, "incomeMax").length > 0 ||
    filterString(data, "rasi").length > 0 ||
    filterString(data, "nakshatra").length > 0 ||
    filterString(data, "manglik").length > 0 ||
    horoscope === "yes" ||
    horoscope === "no";
}

function pairId(uidA: string, uidB: string): string {
  return [uidA, uidB].sort().join("_");
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
    const heightMinRaw = Number(data?.heightMinCm ?? 90);
    const heightMaxRaw = Number(data?.heightMaxCm ?? 250);
    const heightMinCm = Math.max(
      90,
      Math.min(250, Number.isFinite(heightMinRaw) ? Math.trunc(heightMinRaw) : 90)
    );
    const heightMaxCm = Math.max(
      heightMinCm,
      Math.min(250, Number.isFinite(heightMaxRaw) ? Math.trunc(heightMaxRaw) : 250)
    );
    const cursor = typeof data?.cursor === "string" && data.cursor.length <= 128 ? data.cursor : "";
    const keyword = typeof data?.keyword === "string"
      ? data.keyword.trim().toLocaleLowerCase("en-IN").slice(0, MAX_KEYWORD_LENGTH)
      : "";
    const normalizedUsername = keyword.replace(/^@+/, "");
    const includeQuestionnaireFit = filterBoolean(data, "includeQuestionnaireFit");
    const requireQuestionnaireFit = filterBoolean(data, "requireQuestionnaireFit");
    const includeAstrologyFit = filterBoolean(data, "includeAstrologyFit");
    const requireAstrologyFit = filterBoolean(data, "requireAstrologyFit");
    const minMutualMatchPercent = filterInt(data, "minMutualMatchPercent", 0, 100);

    const [viewerDoc, viewerPreferencesDoc, viewerPrivateDoc] = await Promise.all([
      db.collection("users").doc(viewerUid).get(),
      db.collection("partnerPreferences").doc(viewerUid).get(),
      db.collection("userPrivate").doc(viewerUid).get(),
    ]);
    if (!viewerDoc.exists) {
      throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");
    }
    const viewer = viewerDoc.data() || {};
    const viewerPreferencesRaw = viewerPreferencesDoc.exists
      ? viewerPreferencesDoc.data() as Record<string, unknown>
      : undefined;
    const viewerPrivate = viewerPrivateDoc.exists
      ? viewerPrivateDoc.data() as Record<string, unknown>
      : {};
    if (!discoveryActorReady(
      viewer as Record<string, unknown>,
      viewerPrivate,
      viewerPreferencesRaw
    )) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Complete your required profile and partner preferences before using discovery"
      );
    }
    const viewerAccountStatus = stringValue(viewer.accountStatus).toUpperCase() || "ACTIVE";
    if (viewerAccountStatus !== "ACTIVE" || viewer.matrimonyPaused === true) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Discovery is unavailable while your matrimony account is not active"
      );
    }
    const viewerPartnerPreferences = normalizePartnerPreferences(
      viewerPreferencesRaw
    );
    const viewerGender = stringValue(viewer.gender).toUpperCase();
    const viewerLookingFor = stringValue(viewer.lookingFor).toUpperCase() || "ANY";
    const viewerQuestionnaireDoc = includeQuestionnaireFit || requireQuestionnaireFit
      ? await db.collection("questionnaires").doc(viewerUid).get()
      : null;
    const viewerQuestionnaire = viewerQuestionnaireDoc?.exists
      ? viewerQuestionnaireDoc.data() || {}
      : {};

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
    const viewerHiddenRefs = candidates.map((doc) =>
      db.collection("privacyRelations").doc(viewerUid).collection("members").doc(doc.id)
    );
    const subscriptionRefs = candidates.map((doc) => db.collection("subscriptions").doc(doc.id));
    const partnerPreferenceRefs = candidates.map((doc) =>
      db.collection("partnerPreferences").doc(doc.id)
    );
    const questionnaireRefs = includeQuestionnaireFit || requireQuestionnaireFit
      ? candidates.map((doc) => db.collection("questionnaires").doc(doc.id))
      : [];
    const privateFilterRequested =
      needsPrivateFilterData(data) ||
      viewerPartnerPreferences.incomeBandMode !== "NO_PREFERENCE" ||
      includeAstrologyFit ||
      requireAstrologyFit;
    const personalizationActive = !keyword &&
      await hasActiveConsent(viewerUid, "personalization");
    const recommendationFeedbackRefs = personalizationActive
      ? candidates.map((doc) =>
        db.collection("recommendationFeedback")
          .doc(viewerUid)
          .collection("targets")
          .doc(doc.id))
      : [];
    const lastActiveFilterDays = filterInt(data, "lastActiveWithinDays", 0, 3650);
    const privateRefs = privateFilterRequested
      ? candidates.map((doc) => db.collection("userPrivate").doc(doc.id))
      : [];
    // Presence is always read by trusted discovery code so stale inventory can be suppressed.
    // Precise timestamps are still never returned unless the target's activity-visibility policy
    // permits the explicit last-active filter path below.
    const presenceRefs = candidates.map((doc) =>
      db.collection("presencePrivate").doc(doc.id)
    );
    const activitySettingsRefs = lastActiveFilterDays > 0
      ? candidates.map((doc) => db.collection("privacySettings").doc(doc.id))
      : [];
    const outgoingInterestRefs = lastActiveFilterDays > 0
      ? candidates.map((doc) => db.collection("interests").doc(`${viewerUid}_${doc.id}`))
      : [];
    const incomingInterestRefs = lastActiveFilterDays > 0
      ? candidates.map((doc) => db.collection("interests").doc(`${doc.id}_${viewerUid}`))
      : [];
    const matchRefs = lastActiveFilterDays > 0
      ? candidates.map((doc) => db.collection("matches").doc(pairId(viewerUid, doc.id)))
      : [];

    const [
      reverseDocs,
      privacyDocs,
      viewerPrivacyDocs,
      subscriptionDocs,
      partnerPreferenceDocs,
      questionnaireDocs,
      privateDocs,
      presenceDocs,
      activitySettingsDocs,
      outgoingInterestDocs,
      incomingInterestDocs,
      matchDocs,
      recommendationFeedbackDocs,
    ] = await Promise.all([
      reverseBlockRefs.length ? db.getAll(...reverseBlockRefs) : Promise.resolve([]),
      hiddenFromViewerRefs.length ? db.getAll(...hiddenFromViewerRefs) : Promise.resolve([]),
      viewerHiddenRefs.length ? db.getAll(...viewerHiddenRefs) : Promise.resolve([]),
      subscriptionRefs.length ? db.getAll(...subscriptionRefs) : Promise.resolve([]),
      partnerPreferenceRefs.length ? db.getAll(...partnerPreferenceRefs) : Promise.resolve([]),
      questionnaireRefs.length ? db.getAll(...questionnaireRefs) : Promise.resolve([]),
      privateRefs.length ? db.getAll(...privateRefs) : Promise.resolve([]),
      presenceRefs.length ? db.getAll(...presenceRefs) : Promise.resolve([]),
      activitySettingsRefs.length ? db.getAll(...activitySettingsRefs) : Promise.resolve([]),
      outgoingInterestRefs.length ? db.getAll(...outgoingInterestRefs) : Promise.resolve([]),
      incomingInterestRefs.length ? db.getAll(...incomingInterestRefs) : Promise.resolve([]),
      matchRefs.length ? db.getAll(...matchRefs) : Promise.resolve([]),
      recommendationFeedbackRefs.length
        ? db.getAll(...recommendationFeedbackRefs)
        : Promise.resolve([]),
    ]);

    const reverseBlocked = new Set<string>();
    const hiddenFromViewer = new Set<string>();
    const hiddenByViewer = new Set<string>();
    const boostUntilByUid = new Map<string, number>();
    const membershipActiveByUid = new Map<string, boolean>();
    const partnerPreferencesByUid = new Map<
      string,
      ReturnType<typeof normalizePartnerPreferences>
    >();
    const rawPartnerPreferencesByUid = new Map<
      string,
      FirebaseFirestore.DocumentData | undefined
    >();
    candidates.forEach((doc, index) => {
      const raw = partnerPreferenceDocs[index]?.data();
      rawPartnerPreferencesByUid.set(doc.id, raw);
      partnerPreferencesByUid.set(
        doc.id,
        normalizePartnerPreferences(raw)
      );
    });
    const privateFilterByUid = new Map<string, FirebaseFirestore.DocumentData>();
    const questionnaireFitByUid = new Map<string, number>();
    const astrologyFitByUid = new Map<string, number>();
    questionnaireDocs.forEach((doc, index) => {
      if (!doc.exists || !viewerQuestionnaireDoc?.exists) return;
      const score = questionnaireCompatibility(
        viewerQuestionnaire.selfVector,
        viewerQuestionnaire.partnerVector,
        doc.data()?.selfVector,
        doc.data()?.partnerVector
      );
      if (score) questionnaireFitByUid.set(candidates[index].id, score.score);
    });
    const feedbackByUid = new Map<string, FirebaseFirestore.DocumentData>();
    recommendationFeedbackDocs.forEach((doc, index) => {
      if (doc.exists) feedbackByUid.set(candidates[index].id, doc.data() || {});
    });
    const visibleLastActiveByUid = new Map<string, number>();
    const privateLastActiveByUid = new Map<string, number>();
    presenceDocs.forEach((doc, index) => {
      const lastActive = Number(doc.data()?.lastActiveAt || 0);
      if (doc.exists && Number.isFinite(lastActive) && lastActive > 0) {
        privateLastActiveByUid.set(candidates[index].id, lastActive);
      }
    });
    reverseDocs.forEach((doc, index) => {
      if (doc.exists) reverseBlocked.add(candidates[index].id);
    });
    privacyDocs.forEach((doc, index) => {
      if (doc.exists && doc.data()?.profileHidden === true) hiddenFromViewer.add(candidates[index].id);
    });
    viewerPrivacyDocs.forEach((doc, index) => {
      if (doc.exists && doc.data()?.profileHidden === true) hiddenByViewer.add(candidates[index].id);
    });
    subscriptionDocs.forEach((doc, index) => {
      const candidate = candidates[index].data() || {};
      const membership = resolveMembershipState(doc.data(), candidate);
      membershipActiveByUid.set(candidates[index].id, membership.active);
      const raw = Number(doc.data()?.boostUntil || 0);
      boostUntilByUid.set(candidates[index].id, Number.isFinite(raw) ? raw : 0);
    });
    privateDocs.forEach((doc, index) => {
      if (doc.exists) privateFilterByUid.set(candidates[index].id, doc.data() || {});
    });
    if ((includeAstrologyFit || requireAstrologyFit) &&
        isHinduReligion(viewer.religion)) {
      candidates.forEach((candidateDoc) => {
        const candidate = candidateDoc.data() || {};
        if (!isHinduReligion(candidate.religion) || candidate.showHoroscope !== true) return;
        const privateData = privateFilterByUid.get(candidateDoc.id) || {};
        const compatibility = horoscopeCompatibility(
          viewerPrivate.rasi,
          viewerPrivate.nakshatra,
          privateData.rasi,
          privateData.nakshatra
        );
        if (compatibility) astrologyFitByUid.set(candidateDoc.id, compatibility.score);
      });
    }
    if (lastActiveFilterDays > 0) {
      candidates.forEach((candidateDoc, index) => {
        const relationship = {
          interested:
            outgoingInterestDocs[index]?.exists === true ||
            incomingInterestDocs[index]?.exists === true,
          mutual:
            matchDocs[index]?.exists === true ||
            (
              outgoingInterestDocs[index]?.exists === true &&
              incomingInterestDocs[index]?.exists === true
            ),
        };
        const lastActiveVisibility = normalizeActivityVisibility(
          activitySettingsDocs[index]?.data()?.lastActiveVisibility
        );
        if (!activityVisibilityAllows(lastActiveVisibility, relationship)) return;
        const lastActive = Number(presenceDocs[index]?.data()?.lastActiveAt || 0);
        if (Number.isFinite(lastActive) && lastActive > 0) {
          visibleLastActiveByUid.set(candidateDoc.id, lastActive);
        }
      });
    }

    const now = Date.now();
    const eligible: Array<{
      doc: FirebaseFirestore.DocumentSnapshot;
      forwardPreferenceFit: number | null;
      reversePreferenceFit: number | null;
      mutualPreferenceFit: number | null;
      forwardPreferenceCriteria: number;
      reversePreferenceCriteria: number;
      mutualPreferenceCriteria: number;
      boosted: number;
      behavior: number;
      relevance: number;
      createdAt: number;
    }> = [];

    for (const doc of candidates) {
      if (
        reverseBlocked.has(doc.id) ||
        hiddenFromViewer.has(doc.id) ||
        hiddenByViewer.has(doc.id)
      ) continue;
      const candidate = doc.data() || {};
      const accountStatus = stringValue(candidate.accountStatus).toUpperCase() || "ACTIVE";
      if (accountStatus !== "ACTIVE") continue;
      if (!discoveryCandidateReady(candidate, rawPartnerPreferencesByUid.get(doc.id))) continue;
      if (!profileFreshEnough(
        createdAtMillis(candidate),
        privateLastActiveByUid.get(doc.id) || 0,
        now
      )) continue;
      if (candidate.stealthMode === true) continue;

      const candidateAge = Number(candidate.age || 0);
      if (!Number.isFinite(candidateAge) || candidateAge < ageMin || candidateAge > ageMax) continue;
      const candidateHeight = Number(candidate.heightCm || 0);
      if (
        !Number.isFinite(candidateHeight) ||
        candidateHeight < heightMinCm ||
        candidateHeight > heightMaxCm
      ) continue;

      const candidateGender = stringValue(candidate.gender).toUpperCase();
      const candidateLookingFor = stringValue(candidate.lookingFor).toUpperCase() || "ANY";
      const viewerAccepts = viewerLookingFor === "ANY" || viewerLookingFor === candidateGender;
      const candidateAccepts = candidateLookingFor === "ANY" || candidateLookingFor === viewerGender;
      if (!viewerAccepts || !candidateAccepts) continue;
      if (!matchesKeyword(candidate, keyword)) continue;

      const privateData = privateFilterByUid.get(doc.id) || {};
      const filterCandidate = {
        ...candidate,
        incomeBand: privateData.incomeBand,
        rasi: privateData.rasi,
        nakshatra: privateData.nakshatra,
        manglik: privateData.manglik,
      };
      if (!matchesServerFilters(
        filterCandidate,
        data,
        now,
        visibleLastActiveByUid.get(doc.id) || 0,
        membershipActiveByUid.get(doc.id) === true
      )) continue;

      const candidatePartnerPreferences = partnerPreferencesByUid.get(doc.id) ||
        normalizePartnerPreferences(undefined);
      const viewerForPreferences = {
        ...viewer,
        incomeBand: viewerPrivate.incomeBand,
      };
      if (!strictPreferencesAllow(viewerPartnerPreferences, filterCandidate)) continue;
      if (!strictPreferencesAllow(candidatePartnerPreferences, viewerForPreferences)) continue;
      if (requireQuestionnaireFit && !questionnaireFitByUid.has(doc.id)) continue;
      if (requireAstrologyFit && !astrologyFitByUid.has(doc.id)) continue;

      const pairPreferenceMatch = bilateralPreferenceMatch(
        viewerPartnerPreferences,
        viewerForPreferences,
        candidatePartnerPreferences,
        filterCandidate
      );
      if (
        minMutualMatchPercent > 0 &&
        (pairPreferenceMatch.mutual == null ||
          pairPreferenceMatch.mutualCriteria < STRONG_MUTUAL_MIN_CRITERIA ||
          pairPreferenceMatch.mutual * 100 < minMutualMatchPercent)
      ) continue;
      const behavior = personalizationActive
        ? behavioralAdjustment(feedbackByUid.get(doc.id))
        : 0;
      eligible.push({
        doc,
        forwardPreferenceFit: pairPreferenceMatch.forward,
        reversePreferenceFit: pairPreferenceMatch.reverse,
        mutualPreferenceFit: pairPreferenceMatch.mutual,
        forwardPreferenceCriteria: pairPreferenceMatch.forwardCriteria,
        reversePreferenceCriteria: pairPreferenceMatch.reverseCriteria,
        mutualPreferenceCriteria: pairPreferenceMatch.mutualCriteria,
        boosted: (boostUntilByUid.get(doc.id) || 0) > now ? 1 : 0,
        behavior,
        relevance: blendedRecommendationRelevance(pairPreferenceMatch.mutual, behavior),
        createdAt: createdAtMillis(candidate),
      });
    }

    const rankedCandidates = keyword
      ? eligible
      : eligible.sort((left, right) => {
        if (left.relevance !== right.relevance) return right.relevance - left.relevance;
        if (left.boosted !== right.boosted) return right.boosted - left.boosted;
        return right.createdAt - left.createdAt;
      });

    const profiles = rankedCandidates
      .slice(0, RETURN_LIMIT)
      .map(({
        doc,
        forwardPreferenceFit,
        reversePreferenceFit,
        mutualPreferenceFit,
        forwardPreferenceCriteria,
        reversePreferenceCriteria,
        mutualPreferenceCriteria,
      }) => {
        const profile = publicProfile(
          doc.id,
          doc.data() || {},
          membershipActiveByUid.get(doc.id) === true
        );
        const signals: Record<string, number> = {};
        if (mutualPreferenceFit != null) {
          signals.pairPreferenceFit = Math.round(mutualPreferenceFit * 1000) / 1000;
          signals.mutualPreferenceFit = Math.round(mutualPreferenceFit * 1000) / 1000;
        }
        if (forwardPreferenceFit != null) {
          signals.forwardPreferenceFit = Math.round(forwardPreferenceFit * 1000) / 1000;
        }
        if (reversePreferenceFit != null) {
          signals.reversePreferenceFit = Math.round(reversePreferenceFit * 1000) / 1000;
        }
        signals.forwardPreferenceCriteria = forwardPreferenceCriteria;
        signals.reversePreferenceCriteria = reversePreferenceCriteria;
        signals.mutualPreferenceCriteria = mutualPreferenceCriteria;
        const questionnaireFit = questionnaireFitByUid.get(doc.id);
        if (questionnaireFit != null) {
          signals.pairQuestionnaireFit = Math.round(questionnaireFit * 1000) / 1000;
        }
        const astrologyFit = astrologyFitByUid.get(doc.id);
        if (astrologyFit != null) {
          signals.pairAstrologyFit = Math.round(astrologyFit * 1000) / 1000;
        }
        return Object.keys(signals).length === 0
          ? profile
          : { ...profile, ...signals };
      });

    const nextCursor = scan.docs.length === SCAN_LIMIT
      ? scan.docs[scan.docs.length - 1].id
      : null;

    await recordRecommendationImpressionBatch(
      viewerUid,
      profiles.map((profile) => String(profile.firebaseUid || "")),
      keyword ? "SEARCH" : "DISCOVER"
    );

    return {
      profiles,
      nextCursor,
      rankingVersion: DISCOVERY_RANKING_VERSION,
      personalizationApplied: personalizationActive,
    };
  });
