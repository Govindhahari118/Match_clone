export function nearbyAccountIsActive(accountStatus: unknown): boolean {
  const status = typeof accountStatus === "string" ? accountStatus.trim().toUpperCase() : "";
  return status === "" || status === "ACTIVE";
}

export function nearbyAccountIsDiscoverable(
  accountStatus: unknown,
  stealthMode: unknown
): boolean {
  return nearbyAccountIsActive(accountStatus) && stealthMode !== true;
}
