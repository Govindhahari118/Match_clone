const assert = require("node:assert/strict");
const crypto = require("node:crypto");
const test = require("node:test");

const { canonicalChatThreadId } = require("../lib/chatIdentityPolicy");

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
