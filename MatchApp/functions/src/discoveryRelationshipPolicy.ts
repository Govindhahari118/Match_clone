import { directedInterestMatches, matchMembersExactly } from "./interestPolicy";

export type DiscoveryRelationship = {
  interested: boolean;
  mutual: boolean;
};

/**
 * Legacy interest/match document IDs concatenate UIDs with underscores and can collide.
 * Sensitive relationship-dependent discovery filters must verify stored principals.
 * Malformed and unrelated records confer no visibility.
 */
export function discoveryRelationship(
  viewerUid: string,
  candidateUid: string,
  outgoing: Record<string, unknown> | undefined,
  incoming: Record<string, unknown> | undefined,
  match: Record<string, unknown> | undefined
): DiscoveryRelationship {
  const forward = directedInterestMatches(outgoing, viewerUid, candidateUid);
  const reverse = directedInterestMatches(incoming, candidateUid, viewerUid);
  const verifiedMatch = matchMembersExactly(match, viewerUid, candidateUid);
  return {
    interested: forward || reverse,
    mutual: verifiedMatch || (forward && reverse),
  };
}
