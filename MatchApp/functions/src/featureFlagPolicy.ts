export type ProductionFeature = "nearby" | "kundali" | "video_profiles" | "nri_features";

/**
 * Backend production-feature flags are independent from client navigation flags.
 * Both layers must be enabled before a provider/validation-sensitive capability is launch-usable.
 * Missing or malformed values always fail closed.
 */
export function productionFeatureEnabled(value: unknown): boolean {
  return typeof value === "string" && value.trim().toLowerCase() === "true";
}
