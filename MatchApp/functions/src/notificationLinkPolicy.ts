const HOST_RE = /^(?=.{1,253}$)(?!-)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}$/i;

export type NotificationRoute =
  | "interests"
  | "matches"
  | "messages"
  | "notifications"
  | "verification"
  | "pricing";

function cleanHost(value: unknown): string | null {
  const host = String(value || "").trim().toLowerCase();
  if (!host || host === "invalid.matree.local" || !HOST_RE.test(host)) return null;
  return host;
}

/**
 * Persisted notification links prefer the verified HTTPS App Link contract when the production
 * host is configured. Local FCM taps still use signed-in-account-bound immutable extras because
 * the backend cannot know a device's Room profile id.
 */
export function notificationDeepLink(
  route: NotificationRoute,
  configuredHost: unknown
): string {
  const host = cleanHost(configuredHost);
  if (host) return `https://${host}/app/${route}`;
  return `matrimonyconnect://${route}`;
}

export function notificationActionFor(type: string): "INTERESTS" | "MATCHES" | "CHAT" | "NOTIFICATIONS" {
  switch (String(type || "").trim().toUpperCase()) {
  case "INTEREST":
    return "INTERESTS";
  case "MATCH":
    return "MATCHES";
  case "MESSAGE":
  case "CALL_REQUEST":
  case "CALL_ACCEPTED":
  case "CALL_DECLINED":
  case "CALL_CANCELLED":
    return "CHAT";
  default:
    return "NOTIFICATIONS";
  }
}
