import { classifyMessageSafety } from "./messageSafetyPolicy";

export const MAX_INTEREST_INTRO_LENGTH = 280;

const EMAIL = /\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/i;
const PHONE = /(?:\+?\d[\d\s().-]{7,}\d)/;

export type InterestIntroResult = {
  note: string;
  allowed: boolean;
  reason?: "too_long" | "contact_details" | "unsafe_money_request";
};

/**
 * Interest intros are intentionally narrower than mutual-match chat. They may add context and
 * warmth, but cannot be used to bypass contact privacy or solicit payments before both members
 * have opted into a match.
 */
export function validateInterestIntro(value: unknown): InterestIntroResult {
  if (value == null || value === "") return { note: "", allowed: true };
  if (typeof value !== "string") return { note: "", allowed: false, reason: "too_long" };

  const note = value.replace(/\s+/g, " ").trim();
  if (!note) return { note: "", allowed: true };
  if (note.length > MAX_INTEREST_INTRO_LENGTH) {
    return { note: note.slice(0, MAX_INTEREST_INTRO_LENGTH), allowed: false, reason: "too_long" };
  }

  const safety = classifyMessageSafety(note);
  if (safety.moneyRequest) {
    return { note, allowed: false, reason: "unsafe_money_request" };
  }
  if (safety.externalLink || EMAIL.test(note) || PHONE.test(note)) {
    return { note, allowed: false, reason: "contact_details" };
  }
  return { note, allowed: true };
}
