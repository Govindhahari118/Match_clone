import { accountIsActive } from "./accountStatusPolicy";

export function nearbyAccountIsActive(accountStatus: unknown): boolean {
  return accountIsActive(accountStatus);
}

export function nearbyAccountIsDiscoverable(
  accountStatus: unknown,
  stealthMode: unknown
): boolean {
  return accountIsActive(accountStatus) && stealthMode !== true;
}
