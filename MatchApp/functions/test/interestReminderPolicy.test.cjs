const assert = require("node:assert/strict");
const test = require("node:test");
const {
  INTEREST_REMINDER_INITIAL_DELAY_MS,
  INTEREST_REMINDER_COOLDOWN_MS,
  INTEREST_REMINDER_MAX_COUNT,
  interestReminderAllowed,
  nextInterestReminderAt,
} = require("../lib/interestReminderPolicy");

test("first interest reminder is unavailable before 72 hours", () => {
  const created = 2_000_000_000_000;
  assert.equal(interestReminderAllowed(created, 0, 0, created + INTEREST_REMINDER_INITIAL_DELAY_MS - 1), false);
  assert.equal(interestReminderAllowed(created, 0, 0, created + INTEREST_REMINDER_INITIAL_DELAY_MS), true);
});

test("subsequent reminder requires seven-day cooldown", () => {
  const created = 2_000_000_000_000;
  const reminded = created + INTEREST_REMINDER_INITIAL_DELAY_MS;
  assert.equal(interestReminderAllowed(created, reminded, 1, reminded + INTEREST_REMINDER_COOLDOWN_MS - 1), false);
  assert.equal(interestReminderAllowed(created, reminded, 1, reminded + INTEREST_REMINDER_COOLDOWN_MS), true);
});

test("interest reminder hard-stops after two reminders", () => {
  const created = 2_000_000_000_000;
  const reminded = created + INTEREST_REMINDER_INITIAL_DELAY_MS;
  assert.equal(INTEREST_REMINDER_MAX_COUNT, 2);
  assert.equal(interestReminderAllowed(created, reminded, 2, reminded + 30 * INTEREST_REMINDER_COOLDOWN_MS), false);
  assert.equal(nextInterestReminderAt(created, reminded, 2), 0);
});

test("next reminder timestamps are deterministic", () => {
  const created = 2_000_000_000_000;
  const reminded = created + INTEREST_REMINDER_INITIAL_DELAY_MS;
  assert.equal(nextInterestReminderAt(created, 0, 0), created + INTEREST_REMINDER_INITIAL_DELAY_MS);
  assert.equal(nextInterestReminderAt(created, reminded, 1), reminded + INTEREST_REMINDER_COOLDOWN_MS);
});
