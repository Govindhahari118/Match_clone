const assert = require("node:assert/strict");
const test = require("node:test");

const {
  notificationDeepLink,
  notificationActionFor,
} = require("../lib/notificationLinkPolicy");

test("notification deep links prefer verified HTTPS host", () => {
  assert.equal(
    notificationDeepLink("interests", "match.example.com"),
    "https://match.example.com/app/interests"
  );
  assert.equal(
    notificationDeepLink("matches", "MATCH.EXAMPLE.COM"),
    "https://match.example.com/app/matches"
  );
  assert.equal(
    notificationDeepLink("messages", "match.example.com"),
    "https://match.example.com/app/messages"
  );
});

test("notification deep links fail safely to compatibility scheme without a valid production host", () => {
  assert.equal(
    notificationDeepLink("notifications", ""),
    "matrimonyconnect://notifications"
  );
  assert.equal(
    notificationDeepLink("notifications", "invalid.matree.local"),
    "matrimonyconnect://notifications"
  );
  assert.equal(
    notificationDeepLink("notifications", "https://evil.example"),
    "matrimonyconnect://notifications"
  );
});

test("notification actions are explicit and stable", () => {
  assert.equal(notificationActionFor("INTEREST"), "INTERESTS");
  assert.equal(notificationActionFor("MATCH"), "MATCHES");
  assert.equal(notificationActionFor("MESSAGE"), "CHAT");
  for (const type of ["CALL_REQUEST", "CALL_ACCEPTED", "CALL_DECLINED", "CALL_CANCELLED"]) {
    assert.equal(notificationActionFor(type), "CHAT");
  }
  assert.equal(notificationActionFor("SYSTEM"), "NOTIFICATIONS");
});
