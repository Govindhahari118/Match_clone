import { adultDobMatchesProfileAge } from "./discoveryEligibilityPolicy";

/**
 * A previously accepted match is not durable permission to continue chatting or calling
 * after either member becomes ineligible. Private DOB never leaves trusted Functions.
 */
export function bothParticipantsAdult(
  sender: Record<string, unknown> | undefined,
  senderPrivate: Record<string, unknown> | undefined,
  recipient: Record<string, unknown> | undefined,
  recipientPrivate: Record<string, unknown> | undefined,
  nowMillis: number = Date.now()
): boolean {
  if (!sender || !senderPrivate || !recipient || !recipientPrivate) return false;
  return adultDobMatchesProfileAge(senderPrivate.dateOfBirth, sender.age, nowMillis) &&
    adultDobMatchesProfileAge(recipientPrivate.dateOfBirth, recipient.age, nowMillis);
}
