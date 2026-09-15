import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";

const MAX_SAVED_SEARCHES = 20;

function text(source: Record<string, unknown>, key: string, max = 100): string {
  const value = source[key];
  return typeof value === "string" ? value.trim().slice(0, max) : "";
}

function bool(source: Record<string, unknown>, key: string, fallback = false): boolean {
  return typeof source[key] === "boolean" ? source[key] as boolean : fallback;
}

function integer(source: Record<string, unknown>, key: string, fallback = 0): number {
  const value = Number(source[key]);
  return Number.isFinite(value) ? Math.trunc(value) : fallback;
}

function number(source: Record<string, unknown>, key: string, fallback = 0): number {
  const value = Number(source[key]);
  return Number.isFinite(value) ? value : fallback;
}

function sanitizeFilter(raw: unknown): Record<string, string | number | boolean> {
  const f = raw && typeof raw === "object" && !Array.isArray(raw) ? raw as Record<string, unknown> : {};
  const ageMin = integer(f, "ageMin", 18);
  const ageMax = integer(f, "ageMax", 70);
  if (ageMin < 18 || ageMax > 99 || ageMin > ageMax) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid age range");
  }

  return {
    ageMin,
    ageMax,
    city: text(f, "city", 80),
    state: text(f, "state", 80),
    caste: text(f, "caste", 80),
    subCaste: text(f, "subCaste", 80),
    minScore: Math.max(0, Math.min(1, number(f, "minScore", 0))),
    religion: text(f, "religion", 80),
    motherTongue: text(f, "motherTongue", 80),
    maritalStatus: text(f, "maritalStatus", 80),
    verifiedOnly: bool(f, "verifiedOnly"),
    incomeMin: text(f, "incomeMin", 80),
    incomeMax: text(f, "incomeMax", 80),
    educationLevel: text(f, "educationLevel", 100),
    diet: text(f, "diet", 80),
    residentialStatus: text(f, "residentialStatus", 80),
    hasChildren: text(f, "hasChildren", 40),
    keyword: text(f, "keyword", 64),
    gothra: text(f, "gothra", 80),
    nativeState: text(f, "nativeState", 80),
    countryOfResidence: text(f, "countryOfResidence", 80),
    nriOnly: bool(f, "nriOnly"),
    willingToRelocate: bool(f, "willingToRelocate"),
    recentlyJoinedDays: Math.max(0, Math.min(365, integer(f, "recentlyJoinedDays"))),
    smoking: text(f, "smoking", 40),
    drinking: text(f, "drinking", 40),
    familyType: text(f, "familyType", 80),
    familyStatus: text(f, "familyStatus", 80),
    physicalStatus: text(f, "physicalStatus", 80),
    hasChildrenFilter: text(f, "hasChildrenFilter", 40),
    citizenship: text(f, "citizenship", 80),
    nriStatus: text(f, "nriStatus", 20),
    educationField: text(f, "educationField", 100),
    occupationCategory: text(f, "occupationCategory", 100),
    employerType: text(f, "employerType", 80),
    nakshatra: text(f, "nakshatra", 80),
    rasi: text(f, "rasi", 80),
    manglik: text(f, "manglik", 40),
    hobbies: text(f, "hobbies", 200),
    withPhotoOnly: bool(f, "withPhotoOnly", true),
    verifiedLevel: Math.max(0, Math.min(5, integer(f, "verifiedLevel"))),
    premiumOnly: bool(f, "premiumOnly"),
    lastActiveWithinDays: Math.max(0, Math.min(365, integer(f, "lastActiveWithinDays"))),
    minPoruthamScore: Math.max(0, Math.min(10, integer(f, "minPoruthamScore"))),
    hasHoroscope: text(f, "hasHoroscope", 20),
  };
}

export const saveSavedSearch = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const name = typeof data?.name === "string" ? data.name.trim().replace(/\s+/g, " ").slice(0, 60) : "";
  if (!name) throw new functions.https.HttpsError("invalid-argument", "Saved-search name required");
  const filter = sanitizeFilter(data?.filter);
  const rootRef = db.collection("savedSearches").doc(uid);
  const itemRef = rootRef.collection("items").doc();
  const now = Date.now();

  await db.runTransaction(async (transaction) => {
    const root = await transaction.get(rootRef);
    const count = Number(root.data()?.count ?? 0);
    if (!Number.isFinite(count) || count < 0) {
      throw new functions.https.HttpsError("internal", "Saved-search metadata is invalid");
    }
    if (count >= MAX_SAVED_SEARCHES) {
      throw new functions.https.HttpsError("resource-exhausted", `You can keep up to ${MAX_SAVED_SEARCHES} saved searches.`);
    }
    transaction.set(itemRef, { name, createdAt: now, ...filter });
    transaction.set(rootRef, { count: count + 1, updatedAt: now }, { merge: true });
  });

  return { id: itemRef.id };
});

export const deleteSavedSearch = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const id = typeof data?.id === "string" ? data.id.trim() : "";
  if (!/^[A-Za-z0-9_-]{8,128}$/.test(id)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid saved-search id");
  }

  const rootRef = db.collection("savedSearches").doc(uid);
  const itemRef = rootRef.collection("items").doc(id);
  await db.runTransaction(async (transaction) => {
    const [root, item] = await Promise.all([transaction.get(rootRef), transaction.get(itemRef)]);
    if (!item.exists) return;
    const count = Math.max(0, Number(root.data()?.count ?? 1));
    transaction.delete(itemRef);
    transaction.set(rootRef, { count: Math.max(0, count - 1), updatedAt: Date.now() }, { merge: true });
  });
  return { success: true };
});
