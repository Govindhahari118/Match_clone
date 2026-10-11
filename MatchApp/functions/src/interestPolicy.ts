export function declinedInterestAllowsNewRequest(
  responseStatus: unknown,
  recipientHasInitiated: boolean
): boolean {
  const status = typeof responseStatus === "string" ?
    responseStatus.trim().toLowerCase() :
    "";
  if (status !== "declined") return true;
  return recipientHasInitiated;
}

/** A legacy underscore-joined interest key never establishes pair authority by itself. */
export function directedInterestMatches(
  value: Record<string, unknown> | undefined,
  fromUid: string,
  toUid: string
): boolean {
  return !!value && !!fromUid && !!toUid && fromUid !== toUid &&
    value.fromUid === fromUid && value.toUid === toUid;
}

/** Matches have unordered, exactly-two member arrays and can share colliding legacy IDs. */
export function matchMembersExactly(
  value: Record<string, unknown> | undefined,
  uidA: string,
  uidB: string
): boolean {
  if (!value || !uidA || !uidB || uidA === uidB || !Array.isArray(value.users) ||
      value.users.length !== 2) return false;
  return value.users.filter((uid) => uid === uidA).length === 1 &&
    value.users.filter((uid) => uid === uidB).length === 1;
}
