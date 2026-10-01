export const DISCOVERY_RANKING_VERSION = "discovery-v5-reciprocal-freshness";

export type RecommendationFeedbackSummary = {
  profileOpenCount?: unknown;
  shortlistCount?: unknown;
  interestSentCount?: unknown;
  mutualMatchCount?: unknown;
  hiddenCount?: unknown;
  reportedCount?: unknown;
};

function boundedCount(value: unknown, max: number): number {
  const numeric = Number(value || 0);
  if (!Number.isFinite(numeric) || numeric <= 0) return 0;
  return Math.min(max, Math.trunc(numeric));
}

/**
 * Small, explainable per-target personalization adjustment based only on the viewer's own
 * interactions with that target. It does not infer protected traits, use payment status, or make
 * relationship-success predictions.
 *
 * Negative actions dominate positive actions so previously hidden/reported profiles are strongly
 * de-prioritized if they ever reach ranking. Values are deliberately bounded to keep explicit
 * partner-preference fit as the main relevance signal.
 */
export function behavioralAdjustment(value: RecommendationFeedbackSummary | undefined): number {
  if (!value) return 0;

  const hidden = boundedCount(value.hiddenCount, 1);
  const reported = boundedCount(value.reportedCount, 1);
  if (reported > 0) return -1;
  if (hidden > 0) return -0.8;

  const opens = boundedCount(value.profileOpenCount, 3) * 0.04;
  const shortlist = boundedCount(value.shortlistCount, 1) * 0.16;
  const interest = boundedCount(value.interestSentCount, 1) * 0.18;
  const mutual = boundedCount(value.mutualMatchCount, 1) * 0.12;

  return Math.min(0.5, opens + shortlist + interest + mutual);
}

/**
 * Preference fit remains dominant. Consented behavior may move a candidate only within a narrow
 * band so feedback cannot silently override the user's explicit partner criteria.
 */
/**
 * Down-rank profiles that appeared in the viewer's immediately preceding discovery batches.
 * This is intentionally small: novelty improves inventory rotation but never overrides strict
 * eligibility or a materially stronger reciprocal preference fit.
 */
export function recentImpressionAdjustment(batchIndex: number | undefined): number {
  if (batchIndex == null || !Number.isInteger(batchIndex) || batchIndex < 0) return 0;
  const penalties = [-0.14, -0.10, -0.07, -0.04, -0.02];
  return penalties[Math.min(batchIndex, penalties.length - 1)];
}

export function blendedRecommendationRelevance(
  preferredFit: number | null,
  behavior: number
): number {
  const base = preferredFit == null
    ? 0.5
    : Math.max(0, Math.min(1, preferredFit));
  const boundedBehavior = Math.max(-1, Math.min(0.5, behavior));
  return Math.max(0, Math.min(1, base + boundedBehavior * 0.12));
}
