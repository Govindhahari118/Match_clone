import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db, requireAppCheck } from "./shared";

const GEOHASH_ALPHABET = "0123456789bcdefghjkmnpqrstuvwxyz";
// Nearby represents current proximity, not a historical location. A user must refresh at least
// once per day to remain eligible, and Stop sharing still removes the point immediately.
const LOCATION_MAX_AGE_MS = 24 * 60 * 60 * 1000;
const MAX_RESULTS = 50;
const MAX_CELL_DOCS = 250;
const DELETE_BATCH_SIZE = 300;

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

/** Coarse client-facing proximity. Exact/decimal distance never leaves trusted server code. */
function distanceBucket(km: number): string {
  if (km < 1) return "Less than 1 km away";
  if (km < 2) return "About 1 km away";
  if (km < 5) return "About 2–5 km away";
  if (km < 10) return "About 5–10 km away";
  if (km < 25) return "About 10–25 km away";
  if (km < 50) return "About 25–50 km away";
  return "50+ km away";
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

function locationExpiresAt(data: FirebaseFirestore.DocumentData): number {
  const explicit = Number(data.expiresAtMillis || 0);
  if (Number.isFinite(explicit) && explicit > 0) return explicit;
  const updated = Number(data.updatedAtMillis || 0);
  return Number.isFinite(updated) && updated > 0 ? updated + LOCATION_MAX_AGE_MS : 0;
}

function freshSharingLocation(data: FirebaseFirestore.DocumentData, now = Date.now()): boolean {
  return data.sharingEnabled !== false && locationExpiresAt(data) > now;
}

/** Read sharing state without exposing coordinates to the client. */
export const getNearbyStatus = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const ref = db.collection("userLocations").doc(uid);
  const snap = await ref.get();
  if (!snap.exists) return { sharing: false, updatedAtMillis: 0, expiresAtMillis: 0 };

  const data = snap.data() || {};
  if (!freshSharingLocation(data)) {
    await ref.delete();
    return { sharing: false, updatedAtMillis: 0, expiresAtMillis: 0 };
  }
  return {
    sharing: true,
    updatedAtMillis: Number(data.updatedAtMillis || 0),
    expiresAtMillis: locationExpiresAt(data),
  };
});

export const updateMyLocation = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const location = normalizedLocation(data);
  const profile = await db.collection("users").doc(uid).get();
  if (!profile.exists) throw new functions.https.HttpsError("failed-precondition", "Complete your profile first");

  const now = Date.now();
  const expiresAtMillis = now + LOCATION_MAX_AGE_MS;
  await db.collection("userLocations").doc(uid).set({
    ...location,
    geohash: encodeGeohash(location.latitude, location.longitude, 8),
    sharingEnabled: true,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAtMillis: now,
    expiresAtMillis,
  });
  return { success: true, sharing: true, updatedAtMillis: now, expiresAtMillis };
});

export const clearMyLocation = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  await db.collection("userLocations").doc(uid).delete();
  return { success: true, sharing: false };
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
    if (!viewerLocation.exists) throw new functions.https.HttpsError("failed-precondition", "Enable Nearby first");

    const viewerLocationData = viewerLocation.data() || {};
    if (!freshSharingLocation(viewerLocationData)) {
      await viewerLocation.ref.delete();
      throw new functions.https.HttpsError("failed-precondition", "Nearby sharing expired. Enable it again to continue.");
    }

    const center = normalizedLocation(viewerLocationData);
    const prefixes = nearbyPrefixes(center, radiusKm);
    const candidateLocations = new Map<string, FirebaseFirestore.DocumentData>();

    const snapshots = await Promise.all(prefixes.map((prefix) =>
      db.collection("userLocations")
        .where("geohash", ">=", prefix)
        .where("geohash", "<=", `${prefix}\uf8ff`)
        .limit(MAX_CELL_DOCS)
        .get()
    ));
    for (const snap of snapshots) {
      for (const doc of snap.docs) candidateLocations.set(doc.id, doc.data());
    }

    const outgoing = new Set(outgoingBlocks.docs.map((doc) => doc.id));
    const now = Date.now();
    const distanceEntries = [...candidateLocations.entries()]
      .filter(([candidateUid, value]) => candidateUid !== uid && !outgoing.has(candidateUid) && freshSharingLocation(value, now))
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
    const viewerHiddenRefs = distanceEntries.map((entry) =>
      db.collection("privacyRelations").doc(uid).collection("members").doc(entry.uid)
    );
    const [reverseBlocks, profiles, privacyRelations, viewerPrivacyRelations] = await Promise.all([
      db.getAll(...reverseRefs),
      db.getAll(...profileRefs),
      db.getAll(...hiddenFromViewerRefs),
      db.getAll(...viewerHiddenRefs),
    ]);

    const viewer = viewerProfile.data() || {};
    const result: Array<{ uid: string; distanceBucket: string }> = [];
    for (let i = 0; i < distanceEntries.length && result.length < MAX_RESULTS; i += 1) {
      if (reverseBlocks[i].exists || !profiles[i].exists) continue;
      if (privacyRelations[i].exists && privacyRelations[i].data()?.profileHidden === true) continue;
      if (viewerPrivacyRelations[i].exists && viewerPrivacyRelations[i].data()?.profileHidden === true) continue;
      const candidate = profiles[i].data() || {};
      if (candidate.stealthMode === true || !mutuallyCompatible(viewer, candidate)) continue;
      result.push({
        uid: distanceEntries[i].uid,
        distanceBucket: distanceBucket(distanceEntries[i].distanceKm),
      });
    }
    return { profiles: result };
  });

/** Delete stale exact coordinates frequently; eligibility is also checked synchronously on reads. */
export const cleanupStaleLocations = functions.pubsub
  .schedule("every 1 hours")
  .onRun(async () => {
    const now = Date.now();
    const legacyCutoff = now - LOCATION_MAX_AGE_MS;
    let removed = 0;

    async function remove(query: FirebaseFirestore.Query): Promise<void> {
      let hasMore = true;
      while (hasMore) {
        const stale = await query.limit(DELETE_BATCH_SIZE).get();
        if (stale.empty) return;
        const batch = db.batch();
        stale.docs.forEach((doc) => batch.delete(doc.ref));
        await batch.commit();
        removed += stale.size;
        hasMore = stale.size === DELETE_BATCH_SIZE;
      }
    }

    await remove(db.collection("userLocations").where("expiresAtMillis", "<", now));
    // Migration compatibility for pre-expiry-marker documents.
    await remove(db.collection("userLocations").where("updatedAtMillis", "<", legacyCutoff));
    functions.logger.info("Stale Nearby locations removed", { removed });
    return null;
  });

/** Ensure exact coordinates do not survive account/profile deletion. */
export const cleanupLocationOnUserDelete = functions.firestore
  .document("users/{uid}")
  .onDelete(async (_snap, context) => {
    await db.collection("userLocations").doc(context.params.uid).delete();
  });
