const assert = require("node:assert/strict");
const crypto = require("node:crypto");
const test = require("node:test");

const { canonicalChatThreadId, chatThreadMatchesPair } = require("../lib/chatIdentityPolicy");

test("canonical chat thread id is order independent", () => {
  const a = "uid-alpha";
  const b = "uid-beta";
  assert.equal(canonicalChatThreadId(a, b), canonicalChatThreadId(b, a));
});

test("canonical chat thread id matches the Android newline contract", () => {
  const a = "uid-alpha";
  const b = "uid-beta";
  const expected = crypto
    .createHash("sha256")
    .update([a, b].sort().join("\n"), "utf8")
    .digest("hex");
  assert.equal(canonicalChatThreadId(a, b), expected);
});

test("only exact two-member chat threads can be accessed and updated", () => {
  assert.equal(chatThreadMatchesPair(["alice", "bob"], "alice", "bob"), true);
  assert.equal(chatThreadMatchesPair(["bob", "alice"], "alice", "bob"), true);
  assert.equal(chatThreadMatchesPair(["alice", "bob", "extra"], "alice", "bob"), false);
  assert.equal(chatThreadMatchesPair(["alice", "alice"], "alice", "bob"), false);
  assert.equal(chatThreadMatchesPair(["alice", "bob"], "alice", "charlie"), false);
  assert.equal(chatThreadMatchesPair(undefined, "alice", "bob"), false);
});

test("legacy delimiter ambiguity cannot authorize colliding chat participants", () => {
  // Current Android contract is intentionally preserved until a versioned migration.
  // Distinct pairs with newline-containing UIDs can derive the same legacy hash.
  const a = ["a\nb", "c"];
  const b = ["a", "b\nc"];
  assert.equal(canonicalChatThreadId(...a), canonicalChatThreadId(...b));
  assert.equal(chatThreadMatchesPair(a, ...b), false);
  assert.equal(chatThreadMatchesPair(a, ...a), true);
});
