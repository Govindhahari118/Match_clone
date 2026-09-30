const test = require("node:test");
const assert = require("node:assert/strict");
const {
  QUESTIONNAIRE_VECTOR_LENGTH,
  questionnaireCompatibility,
  sanitizeQuestionnaireVectors,
} = require("../lib/questionnaireCompatibilityPolicy");

function vector(value = 0) {
  return Array.from({ length: QUESTIONNAIRE_VECTOR_LENGTH }, () => value);
}

test("questionnaire vectors fail closed on malformed length or values", () => {
  assert.equal(sanitizeQuestionnaireVectors([0], vector()), null);
  const invalid = vector();
  invalid[2] = 2;
  assert.equal(sanitizeQuestionnaireVectors(invalid, vector()), null);
});

test("bilateral questionnaire compatibility is bounded and symmetric", () => {
  const aSelf = vector(0.5);
  const aPartner = vector(1);
  const bSelf = vector(1);
  const bPartner = vector(0.5);
  const forward = questionnaireCompatibility(aSelf, aPartner, bSelf, bPartner);
  const reverse = questionnaireCompatibility(bSelf, bPartner, aSelf, aPartner);
  assert.ok(forward);
  assert.ok(forward.score >= 0 && forward.score <= 1);
  assert.deepEqual(forward, reverse);
  assert.equal(forward.formulaVersion, "questionnaire-v1-bilateral-cosine");
});

test("opposite non-zero vectors produce the minimum bilateral signal", () => {
  const result = questionnaireCompatibility(vector(1), vector(1), vector(-1), vector(-1));
  assert.ok(result);
  assert.equal(result.score, 0);
});
