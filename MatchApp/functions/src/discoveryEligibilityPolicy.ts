function text(value: unknown): string {
  return typeof value === "string" ? value.trim() : "";
}

function finiteInt(value: unknown): number {
  const n = Number(value);
  return Number.isFinite(n) ? Math.trunc(n) : 0;
}

/**
 * Mirrors the required account profile gate without depending on Android-local state.
 * These fields are the minimum needed for a truthful matrimony profile in discovery.
 */
export function publicDiscoveryProfileReady(
  profile: Record<string, unknown>
): boolean {
  const age = finiteInt(profile.age);
  const height = finiteInt(profile.heightCm);
  const weight = Number(profile.weight || 0);
  return text(profile.username).length >= 3 &&
    text(profile.displayName).length >= 2 &&
    age >= 18 && age <= 99 &&
    text(profile.state).length > 0 &&
    text(profile.city).length >= 2 &&
    text(profile.motherTongue).length > 0 &&
    text(profile.religion).length > 0 &&
    text(profile.education).length > 0 &&
    text(profile.profession).length >= 2 &&
    text(profile.occupationCategory).length > 0 &&
    text(profile.maritalStatus).length > 0 &&
    height >= 90 && height <= 250 &&
    Number.isFinite(weight) && weight >= 30 && weight <= 250;
}

export function partnerPreferencesReady(
  preferences: Record<string, unknown> | undefined
): boolean {
  return preferences?.configured === true;
}

export const DEFAULT_STALE_DISCOVERY_DAYS = 90;
export const MIN_STALE_DISCOVERY_DAYS = 14;
export const MAX_STALE_DISCOVERY_DAYS = 365;

export function normalizeStaleDiscoveryDays(
  value: unknown,
  fallback = DEFAULT_STALE_DISCOVERY_DAYS
): number {
  const fallbackDays = Number.isFinite(Number(fallback))
    ? Math.max(
      MIN_STALE_DISCOVERY_DAYS,
      Math.min(MAX_STALE_DISCOVERY_DAYS, Math.trunc(Number(fallback)))
    )
    : DEFAULT_STALE_DISCOVERY_DAYS;
  // A missing or unset remote-config value must use the product default.
  // Number(null) and Number("") equal zero, which would otherwise force 14 days.
  if (value === null || value === undefined ||
      (typeof value === "string" && value.trim() === "")) return fallbackDays;
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return fallbackDays;
  return Math.max(
    MIN_STALE_DISCOVERY_DAYS,
    Math.min(MAX_STALE_DISCOVERY_DAYS, Math.trunc(parsed))
  );
}

/**
 * Keep discovery inventory fresh without exposing precise activity timestamps.
 * The backend may use private presence as an eligibility signal even when the member hides
 * "last active" from other members. Creation time is the fallback freshness anchor for legacy
 * profiles that predate trusted private-presence heartbeats.
 */
export function profileFreshEnough(
  createdAtMillis: number,
  lastActiveAtMillis: number,
  nowMillis: number,
  maxInactiveDays = DEFAULT_STALE_DISCOVERY_DAYS
): boolean {
  const now = Number.isFinite(nowMillis) && nowMillis > 0 ? nowMillis : Date.now();
  const created = Number.isFinite(createdAtMillis) && createdAtMillis > 0 ? createdAtMillis : 0;
  const active = Number.isFinite(lastActiveAtMillis) && lastActiveAtMillis > 0 ? lastActiveAtMillis : 0;
  // Activity records from imports or stale clocks must not predate a newer
  // profile creation event and incorrectly exclude a newly created account.
  const anchor = Math.max(active, created);
  if (anchor === 0) return true;

  // Trusted clocks can still drift. Clamp a future anchor to "now" so bad data cannot create an
  // effectively permanent freshness exemption.
  const boundedAnchor = Math.min(anchor, now);
  const maxAgeMs = normalizeStaleDiscoveryDays(maxInactiveDays) * 86_400_000;
  return now - boundedAnchor <= maxAgeMs;
}


export function discoveryActorReady(
  profile: Record<string, unknown>,
  privateProfile: Record<string, unknown>,
  preferences: Record<string, unknown> | undefined
): boolean {
  return publicDiscoveryProfileReady(profile) &&
    text(privateProfile.dateOfBirth).length > 0 &&
    partnerPreferencesReady(preferences);
}

export function discoveryCandidateReady(
  profile: Record<string, unknown>,
  preferences: Record<string, unknown> | undefined
): boolean {
  return publicDiscoveryProfileReady(profile) &&
    partnerPreferencesReady(preferences);
}

/** Explicit yes/no filters fail closed for unknown input, never broadening discovery. */
export function boolFromAnyFilter(value: string, actual: boolean): boolean {
  const normalized = value.trim().toLocaleLowerCase("en-IN");
  if (!normalized || normalized === "any" || normalized === "don't mind") return true;
  if (normalized.startsWith("yes")) return actual;
  if (normalized.startsWith("no")) return !actual;
  return false;
}
