import { classifyMessageSafety } from "./messageSafetyPolicy";

export const MAX_INTEREST_INTRO_LENGTH = 280;

const EMAIL = /\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/i;
const PHONE = /(?:\+?\d[\d\s().-]{7,}\d)/;
const OBFUSCATED_EMAIL =
  /\b[a-z0-9._%+-]{1,64}\s*(?:\[?\s*at\s*\]?|\(\s*at\s*\))\s*[a-z0-9.-]{1,63}\s*(?:\[?\s*dot\s*\]?|\(\s*dot\s*\))\s*[a-z]{2,}\b/i;
const OBFUSCATED_DOMAIN =
  /\b[a-z0-9-]{2,63}\s*(?:\[?\s*dot\s*\]?|\(\s*dot\s*\)|\s+dot\s+)\s*(?:com|in|org|net|co|me|io)\b/i;
const EXTERNAL_CONTACT_CHANNEL =
  /\b(?:whats?\s*app|telegram|signal|instagram|insta|snapchat|wechat)\b/i;
const ZERO_WIDTH = /[\u200B-\u200D\uFEFF]/g;

export type InterestIntroResult = {
  note: string;
  allowed: boolean;
  reason?: "too_long" | "contact_details" | "unsafe_money_request";
};

function normalizeIntro(value: string): string {
  return value
    .normalize("NFKC")
    .replace(ZERO_WIDTH, "")
    .replace(/\s+/g, " ")
    .trim();
}

/**
 * Interest intros are intentionally narrower than mutual-match chat. They may add context and
 * warmth, but cannot be used to bypass contact privacy or solicit payments before both members
 * have opted into a match.
 *
 * NFKC + zero-width removal closes common Unicode obfuscation paths without mutating the
 * user-visible wording beyond whitespace normalization.
 */
export function validateInterestIntro(value: unknown): InterestIntroResult {
  if (value == null || value === "") return { note: "", allowed: true };
  if (typeof value !== "string") return { note: "", allowed: false, reason: "too_long" };

  const note = normalizeIntro(value);
  if (!note) return { note: "", allowed: true };
  if (note.length > MAX_INTEREST_INTRO_LENGTH) {
    return { note: note.slice(0, MAX_INTEREST_INTRO_LENGTH), allowed: false, reason: "too_long" };
  }

  const safety = classifyMessageSafety(note);
  if (safety.moneyRequest) {
    return { note, allowed: false, reason: "unsafe_money_request" };
  }
  if (
    safety.externalLink ||
    EMAIL.test(note) ||
    PHONE.test(note) ||
    OBFUSCATED_EMAIL.test(note) ||
    OBFUSCATED_DOMAIN.test(note) ||
    EXTERNAL_CONTACT_CHANNEL.test(note)
  ) {
    return { note, allowed: false, reason: "contact_details" };
  }
  return { note, allowed: true };
}
