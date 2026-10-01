const EXTERNAL_LINK = /\b(?:https?:\/\/|www\.)[^\s]+/i;
const MONEY_REQUEST =
  /(?:\b(?:send|transfer|pay|deposit|loan|urgent)\b.{0,80}\b(?:money|cash|upi|bank|account|inr|rupees?)\b)|(?:₹\s*\d)|(?:\b(?:upi id|bank account|account number)\b)/i;

export type MessageSafetySignals = {
  externalLink: boolean;
  moneyRequest: boolean;
};

export function classifyMessageSafety(body: unknown): MessageSafetySignals {
  const text = typeof body === "string" ? body.slice(0, 3000) : "";
  return {
    externalLink: EXTERNAL_LINK.test(text),
    moneyRequest: MONEY_REQUEST.test(text),
  };
}
