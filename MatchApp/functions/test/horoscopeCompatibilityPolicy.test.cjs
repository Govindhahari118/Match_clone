const test = require("node:test");
const assert = require("node:assert/strict");
const {
  horoscopeCompatibility,
} = require("../lib/horoscopeCompatibilityPolicy");

test("horoscope compatibility is deterministic, bounded and versioned", () => {
  const result = horoscopeCompatibility("Aries", "Ashwini", "Libra", "Chitra");
  assert.ok(result);
  assert.equal(result.formulaVersion, "RASI_NAKSHATRA_2026_09_V1");
  assert.ok(result.score >= 0 && result.score <= 1);
  assert.deepEqual(
    result,
    horoscopeCompatibility("Aries", "Ashwini", "Libra", "Chitra")
  );
});

test("identical valid Rasi and Nakshatra produces the maximum current signal", () => {
  assert.deepEqual(
    horoscopeCompatibility("Pisces", "Revati", "Pisces", "Revati"),
    { score: 1, formulaVersion: "RASI_NAKSHATRA_2026_09_V1" }
  );
});

test("unknown astrology values fail closed instead of fabricating a result", () => {
  assert.equal(
    horoscopeCompatibility("Unknown", "Ashwini", "Pisces", "Revati"),
    null
  );
  assert.equal(
    horoscopeCompatibility("Aries", "", "Pisces", "Revati"),
    null
  );
});
