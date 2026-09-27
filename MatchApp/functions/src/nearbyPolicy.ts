export function nearbyAccountIsDiscoverable(
  accountStatus: unknown,
  stealthMode: unknown
): boolean {
  const status = typeof accountStatus === "string" ? accountStatus.trim().toUpperCase() : "";
  const active = status === "" || status === "ACTIVE";
  return active && stealthMode !== true;
}
