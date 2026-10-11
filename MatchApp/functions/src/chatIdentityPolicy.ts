import * as crypto from "crypto";

export function canonicalChatThreadId(uidA: string, uidB: string): string {
  const canonical = [uidA, uidB].sort().join("\n");
  return crypto.createHash("sha256").update(canonical, "utf8").digest("hex");
}

/**
 * A canonical thread ID can be shared by malformed legacy user identifiers.
 * Never read or overwrite a thread without checking the recorded participant pair.
 */
export function chatThreadMatchesPair(
  participants: unknown,
  uidA: string,
  uidB: string
): boolean {
  return !!uidA && !!uidB && uidA !== uidB &&
    Array.isArray(participants) && participants.length === 2 &&
    participants.filter((uid) => uid === uidA).length === 1 &&
    participants.filter((uid) => uid === uidB).length === 1;
}
