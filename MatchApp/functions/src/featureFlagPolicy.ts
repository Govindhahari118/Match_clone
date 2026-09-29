export type ProductionFeature = "nearby" | "kundali";

/**
 * Backend production-feature flags are intentionally independent from client navigation flags.
 * Both layers must be enabled before a BETA capability is launch-usable. Missing/malformed values
 * always fail closed.
 */
export function productionFeatureEnabled(value: unknown): boolean {
  return typeof value === "string" && value.trim().toLowerCase() === "true";
}
