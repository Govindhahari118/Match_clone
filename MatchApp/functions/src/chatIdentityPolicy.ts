import * as crypto from "crypto";

export function canonicalChatThreadId(uidA: string, uidB: string): string {
  const canonical = [uidA, uidB].sort().join("\n");
  return crypto.createHash("sha256").update(canonical, "utf8").digest("hex");
}
