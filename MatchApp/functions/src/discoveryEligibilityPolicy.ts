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
  return text(profile.username).length >= 3 &&
    text(profile.displayName).length >= 2 &&
    age >= 18 && age <= 99 &&
    text(profile.state).length > 0 &&
    text(profile.city).length >= 2 &&
    text(profile.motherTongue).length > 0 &&
    text(profile.religion).length > 0 &&
    text(profile.education).length > 0 &&
    text(profile.profession).length >= 2 &&
    text(profile.maritalStatus).length > 0 &&
    height >= 90 && height <= 250;
}

export function partnerPreferencesReady(
  preferences: Record<string, unknown> | undefined
): boolean {
  return preferences?.configured === true;
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
