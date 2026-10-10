export type CallRequestStatus = "PENDING" | "ACCEPTED" | "DECLINED" | "CANCELLED";

export function normalizeCallRequestStatus(value: unknown): CallRequestStatus | null {
  const status = String(value || "").trim().toUpperCase();
  return ["PENDING", "ACCEPTED", "DECLINED", "CANCELLED"].includes(status) ?
    status as CallRequestStatus : null;
}

export function validProposedCallTime(nowMs: number, proposedAtMs: number): boolean {
  return Number.isFinite(nowMs) && Number.isFinite(proposedAtMs) &&
    proposedAtMs >= nowMs + 15 * 60_000 &&
    proposedAtMs <= nowMs + 14 * 24 * 60 * 60_000;
}

export function canReplaceCallRequest(
  existingStatus: CallRequestStatus | null,
  existingUpdatedAtMs: number,
  nowMs: number
): boolean {
  if (!Number.isFinite(nowMs)) return false;
  if (existingStatus == null) return true;
  if (existingStatus !== "PENDING" && existingStatus !== "ACCEPTED") return true;
  return Number.isFinite(existingUpdatedAtMs) &&
    nowMs - existingUpdatedAtMs >= 6 * 60 * 60_000;
}

export function canRespondToCallRequest(
  status: CallRequestStatus | null,
  actorUid: string,
  targetUid: string
): boolean {
  return status === "PENDING" && actorUid.length > 0 && actorUid === targetUid;
}

export function canCancelCallRequest(
  status: CallRequestStatus | null,
  actorUid: string,
  requesterUid: string
): boolean {
  return (status === "PENDING" || status === "ACCEPTED") &&
    actorUid.length > 0 && actorUid === requesterUid;
}
