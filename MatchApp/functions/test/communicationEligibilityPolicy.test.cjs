const test = require("node:test");
const assert = require("node:assert/strict");
const { bothParticipantsAdult } = require("../lib/communicationEligibilityPolicy");

const now = Date.UTC(2026, 9, 11);
const adult = { age: 29 };
const adultPrivate = { dateOfBirth: "1997-01-01" };

test("both existing relationship members must remain adult and consistent", () => {
  assert.equal(bothParticipantsAdult(adult, adultPrivate, adult, adultPrivate, now), true);
  assert.equal(bothParticipantsAdult({ age: 18 }, { dateOfBirth: "2008-10-11" }, adult, adultPrivate, now), true);
  assert.equal(bothParticipantsAdult(adult, adultPrivate, { age: 18 }, { dateOfBirth: "2008-10-12" }, now), false);
  assert.equal(bothParticipantsAdult({ age: 18 }, { dateOfBirth: "2008-10-12" }, adult, adultPrivate, now), false);
});

test("missing DOB, malformed dates and mismatched public age deny existing conversations", () => {
  for (const privateData of [
    undefined, {}, { dateOfBirth: "" }, { dateOfBirth: "2008-02-30" },
    { dateOfBirth: "2008-10-12" }, { dateOfBirth: "2030-01-01" },
  ]) {
    assert.equal(bothParticipantsAdult(adult, adultPrivate, adult, privateData, now), false);
    assert.equal(bothParticipantsAdult(adult, privateData, adult, adultPrivate, now), false);
  }
  assert.equal(bothParticipantsAdult(undefined, adultPrivate, adult, adultPrivate, now), false);
  assert.equal(bothParticipantsAdult(adult, adultPrivate, adult, adultPrivate, Number.NaN), false);
  assert.equal(bothParticipantsAdult(adult, adultPrivate, { age: 18 }, adultPrivate, now), false);
});
