import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import { db, requireAppCheck } from "./shared";

const GEOHASH_ALPHABET = "0123456789bcdefghjkmnpqrstuvwxyz";
const LOCATION_MAX_AGE_MS = 30 * 24 * 60 * 60 * 1000;
const MAX_RESULTS = 50;
const MAX_CELL_DOCS = 250;

type Coordinates = { latitude: number; longitude: number };

function finiteNumber(value: unknown, name: string): number {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    throw new functions.https.HttpsError("invalid-argument", `${name} must be a finite number`);
  }
  return parsed;
}

function normalizedLocation(data: unknown): { latitude: number; longitude: number; accuracy: number } {
  const value = (data || {}) as Record<string, unknown>;
  const latitude = finiteNumber(value.latitude, "latitude");
  const longitude = finiteNumber(value.longitude, "longitude");
  const accuracy = Math.max(0, Math.min(50_000, finiteNumber(value.accuracy ?? 0, "accuracy")));
  if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
    throw new functions.https.HttpsError("invalid-argument", "Coordinates are outside valid bounds");
  }
  return { latitude, longitude, accuracy };
}

function encodeGeohash(latitude: number, longitude: number, precision: number): string {
  let latRange: [number, number] = [-90, 90];
  let lngRange: [number, number] = [-180, 180];
  let evenBit = true;
  let bit = 0;
  let value = 0;
  let hash = "";

  while (hash.length < precision) {
    if (evenBit) {
      const mid = (lngRange[0] + lngRange[1]) / 2;
      if (longitude >= mid) {
        value = (value << 1) | 1;
        lngRange = [mid, lngRange[1]];
      } else {
        value <<= 1;
        lngRange = [lngRange[0], mid];
      }
    } else {
      const mid = (latRange[0] + latRange[1]) / 2;
      if (latitude >= mid) {
        value = (value << 1) | 1;
        latRange = [mid, latRange[1]];
      } else {
        value <<= 1;
        latRange = [latRange[0], mid];
      }
    }
    evenBit = !evenBit;
    bit += 1;
    if (bit === 5) {
      hash += GEOHASH_ALPHABET[value];
      bit = 0;
      value = 0;
    }
  }
  return hash;
}

/**
 * Precision 2 covers enormous regions and can silently truncate a dense cell at MAX_CELL_DOCS.
 * Keep broad 11-100 km searches at precision 3 so the 3x3 neighborhood remains bounded while
 * avoiding the severe false-negative behaviour of a single huge precision-2 bucket.
 */
function geohashPrecision(radiusKm: number): number {
  if (radiusKm <= 10) return 4;
  return 3;
}

function nearbyPrefixes(center: Coordinates, radiusKm: number): string[] {
  const precision = geohashPrecision(radiusKm);
  const latDelta = radiusKm / 110.574;
  const longitudeScale = Math.max(0.1, Math.cos(center.latitude * Math.PI / 180));
  const lngDelta = radiusKm / (111.320 * longitudeScale);
  const hashes = new Set<string>();
  for (const latFactor of [-1, 0, 1]) {
    for (const lngFactor of [-1, 0, 1]) {
      const latitude = Math.max(-90, Math.min(90, center.latitude + latFactor * latDelta));
      let longitude = center.longitude + lngFactor * lngDelta;
      while (longitude > 180) longitude -= 360;
      while (longitude < -180) longitude += 360;
      hashes.add(encodeGeohash(latitude, longitude, precision));
    }
  }
  return [...hashes];
}

function distanceKm(a: Coordinates, b: Coordinates): number {
  const earthRadiusKm = 6371.0088;
  const radians = (degrees: number) => degrees * Math.PI / 180;
  const dLat = radians(b.latitude - a.latitude);
  const dLng = radians(b.longitude - a.longitude);
  const lat1 = radians(a.latitude);
  const lat2 = radians(b.latitude);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
  return 2 * earthRadiusKm * Math.asin(Math.min(1, Math.sqrt(h)));
}

function text(value: unknown): string {
  return typeof value === "string" ? value.toUpperCase() : "";
}

function mutuallyCompatible(viewer: FirebaseFirestore.DocumentData, candidate: FirebaseFirestore.DocumentData): boolean {
  const viewerGender = text(viewer.gender);
  const viewerLookingFor = text(viewer.lookingFor) || "ANY";
  const candidateGender = text(candidate.gender);
  const candidateLookingFor = text(candidate.lookingFor) || "ANY";
  return (viewerLookingFor === "ANY" || viewerLookingFor === candidateGender) &&
    (candidateLookingFor === "ANY" || candidateLookingFor === viewerGender);
}

export const updateMyLocation = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const location = normalizedLocation(data);
  const profile = await db.collection("users").doc(uid).get();
  if (!profile.exists) throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");

  await db.collection("userLocations").doc(uid).set({
    ...location,
    geohash: encodeGeohash(location.latitude, location.longitude, 8),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAtMillis: Date.now(),
  });
  return { success: true };
});

export const clearMyLocation = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  await db.collection("userLocations").doc(uid).delete();
  return { success: true };
});

export const nearbyProfiles = functions
  .runWith({ timeoutSeconds: 30, memory: "256MB" })
  .https.onCall(async (data, context) => {
    requireAppCheck(context);
    const uid = context.auth?.uid;
    if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

    const radiusRaw = finiteNumber(data?.radiusKm ?? 25, "radiusKm");
    const radiusKm = Math.max(5, Math.min(100, radiusRaw));
    const [viewerProfile, viewerLocation, outgoingBlocks] = await Promise.all([
      db.collection("users").doc(uid).get(),
      db.collection("userLocations").doc(uid).get(),
      db.collection("blocks").doc(uid).collection("blocked").get(),
    ]);
    if (!viewerProfile.exists) throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");
    if (!viewerLocation.exists) throw new functions.https.HttpsError("failed-precondition", "Share your location first");

    const cutoff = Date.now() - LOCATION_MAX_AGE_MS;
    const viewerLocationData = viewerLocation.data() || {};
    if (Number(viewerLocationData.updatedAtMillis || 0) < cutoff) {
      throw new functions.https.HttpsError("failed-precondition", "Refresh your location before searching nearby");
    }

    const center = normalizedLocation(viewerLocationData);
    const prefixes = nearbyPrefixes(center, radiusKm);
    const candidateLocations = new Map<string, FirebaseFirestore.DocumentData>();

    for (const prefix of prefixes) {
      const snap = await db.collection("userLocations")
        .where("geohash", ">=", prefix)
        .where("geohash", "<=", `${prefix}\uf8ff`)
        .limit(MAX_CELL_DOCS)
        .get();
      for (const doc of snap.docs) candidateLocations.set(doc.id, doc.data());
    }

    const outgoing = new Set(outgoingBlocks.docs.map((doc) => doc.id));
    const distanceEntries = [...candidateLocations.entries()]
      .filter(([candidateUid, value]) => candidateUid !== uid && !outgoing.has(candidateUid) && Number(value.updatedAtMillis || 0) >= cutoff)
      .map(([candidateUid, value]) => {
        const latitude = Number(value.latitude);
        const longitude = Number(value.longitude);
        if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return null;
        return { uid: candidateUid, distanceKm: distanceKm(center, { latitude, longitude }) };
      })
      .filter((entry): entry is { uid: string; distanceKm: number } => entry !== null && entry.distanceKm <= radiusKm)
      .sort((a, b) => a.distanceKm - b.distanceKm)
      .slice(0, MAX_RESULTS * 2);

    if (distanceEntries.length === 0) return { profiles: [] };

    const reverseRefs = distanceEntries.map((entry) =>
      db.collection("blocks").doc(entry.uid).collection("blocked").doc(uid)
    );
    const profileRefs = distanceEntries.map((entry) => db.collection("users").doc(entry.uid));
    const hiddenFromViewerRefs = distanceEntries.map((entry) =>
      db.collection("privacyRelations").doc(entry.uid).collection("members").doc(uid)
    );
    const [reverseBlocks, profiles, privacyRelations] = await Promise.all([
      db.getAll(...reverseRefs),
      db.getAll(...profileRefs),
      db.getAll(...hiddenFromViewerRefs),
    ]);

    const viewer = viewerProfile.data() || {};
    const result: Array<{ uid: string; distanceKm: number }> = [];
    for (let i = 0; i < distanceEntries.length && result.length < MAX_RESULTS; i += 1) {
      if (reverseBlocks[i].exists || !profiles[i].exists) continue;
      if (privacyRelations[i].exists && privacyRelations[i].data()?.profileHidden === true) continue;
      const candidate = profiles[i].data() || {};
      if (candidate.stealthMode === true || !mutuallyCompatible(viewer, candidate)) continue;
      result.push({
        uid: distanceEntries[i].uid,
        distanceKm: Math.round(distanceEntries[i].distanceKm * 10) / 10,
      });
    }
    return { profiles: result };
  });

/** Ensure exact coordinates do not survive account/profile deletion. */
export const cleanupLocationOnUserDelete = functions.firestore
  .document("users/{uid}")
  .onDelete(async (_snap, context) => {
    await db.collection("userLocations").doc(context.params.uid).delete();
  });
