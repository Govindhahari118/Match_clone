const assert = require("node:assert/strict");
const test = require("node:test");

const {
  DISCOVERY_RANKING_VERSION,
  behavioralAdjustment,
  blendedRecommendationRelevance,
} = require("../lib/recommendationPolicy");

test("behavioral reranking is versioned and bounded", () => {
  assert.equal(DISCOVERY_RANKING_VERSION, "discovery-v4-reciprocal-preferences");
  assert.equal(behavioralAdjustment(undefined), 0);
  assert.equal(
    behavioralAdjustment({
      profileOpenCount: 999,
      shortlistCount: 999,
      interestSentCount: 999,
      mutualMatchCount: 999,
    }),
    0.5
  );
});

test("viewer negative actions dominate positive engagement", () => {
  assert.equal(
    behavioralAdjustment({ profileOpenCount: 3, shortlistCount: 1, reportedCount: 1 }),
    -1
  );
  assert.equal(
    behavioralAdjustment({ profileOpenCount: 3, shortlistCount: 1, hiddenCount: 1 }),
    -0.8
  );
});

test("explicit preference fit remains the dominant bounded signal", () => {
  assert.equal(blendedRecommendationRelevance(0.8, 0), 0.8);
  assert.ok(Math.abs(blendedRecommendationRelevance(0.8, 0.5) - 0.86) < 1e-12);
  assert.ok(Math.abs(blendedRecommendationRelevance(0.8, -1) - 0.68) < 1e-12);
  assert.equal(blendedRecommendationRelevance(null, 0), 0.5);
  assert.equal(blendedRecommendationRelevance(1, 0.5), 1);
  assert.equal(blendedRecommendationRelevance(0, -1), 0);
});
