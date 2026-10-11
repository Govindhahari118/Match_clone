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

/**
 * Pair IDs use legacy underscore-delimited Firebase UIDs, which are not collision-free.
 * Never allow an existing document to be read or overwritten for a different two-person
 * relationship, even when the request is expired or terminal.
 */
export function callRequestBelongsToPair(
  value: Record<string, unknown> | undefined,
  uidA: string,
  uidB: string
): boolean {
  if (!value || !uidA || !uidB || uidA === uidB) return false;
  const requester = value.requesterUid;
  const target = value.targetUid;
  const users = value.users;
  return typeof requester === "string" && typeof target === "string" &&
    ((requester === uidA && target === uidB) ||
      (requester === uidB && target === uidA)) &&
    Array.isArray(users) && users.length === 2 &&
    users.filter((uid) => uid === uidA).length === 1 &&
    users.filter((uid) => uid === uidB).length === 1;
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
