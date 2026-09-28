export function accountIsActive(accountStatus: unknown): boolean {
  const status = typeof accountStatus === "string" ? accountStatus.trim().toUpperCase() : "";
  return status === "" || status === "ACTIVE";
}
