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

/**
 * Keep discovery inventory fresh without exposing precise activity timestamps.
 * The backend may use private presence as an eligibility signal even when the member hides
 * "last active" from other members. Unknown legacy activity remains eligible rather than being
 * silently removed until the account has a trustworthy freshness anchor.
 */
export function profileFreshEnough(
  createdAtMillis: number,
  lastActiveAtMillis: number,
  nowMillis: number,
  maxInactiveDays = DEFAULT_STALE_DISCOVERY_DAYS
): boolean {
  const created = Number.isFinite(createdAtMillis) && createdAtMillis > 0 ? createdAtMillis : 0;
  const active = Number.isFinite(lastActiveAtMillis) && lastActiveAtMillis > 0 ? lastActiveAtMillis : 0;
  const anchor = active || created;
  if (anchor === 0) return true;
  const maxAgeMs = Math.max(1, Math.trunc(maxInactiveDays)) * 86_400_000;
  return nowMillis - anchor <= maxAgeMs;
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
