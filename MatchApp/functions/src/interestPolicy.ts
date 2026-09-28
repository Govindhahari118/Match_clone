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
