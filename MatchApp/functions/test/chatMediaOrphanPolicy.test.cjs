const assert = require("node:assert/strict");
const test = require("node:test");

const {
  CHAT_MEDIA_ORPHAN_TTL_MS,
  chatMediaOrphanId,
  parseChatMediaPath,
} = require("../lib/chatMediaOrphanPolicy");

const threadId = "a".repeat(64);

test("canonical chat media path is parsed and gets a stable marker id", () => {
  const parsed = parseChatMediaPath(
    `chat-media/${threadId}/client_1234567890123456.jpg`
  );
  assert.deepEqual(parsed, {
    threadId,
    messageId: "client_1234567890123456",
    extension: "jpg",
  });
  assert.equal(
    chatMediaOrphanId(parsed),
    `${threadId}_client_1234567890123456_jpg`
  );
});

test("voice attachment uses the same bounded identity contract", () => {
  const parsed = parseChatMediaPath(
    `chat-media/${threadId}/voice_1234567890123456.m4a`
  );
  assert.equal(parsed.extension, "m4a");
  assert.ok(CHAT_MEDIA_ORPHAN_TTL_MS >= 24 * 60 * 60 * 1000);
});

test("unexpected paths and unsafe message ids are rejected", () => {
  assert.equal(parseChatMediaPath("photos/alice/x.jpg"), null);
  assert.equal(parseChatMediaPath(`chat-media/${threadId}/short.jpg`), null);
  assert.equal(parseChatMediaPath(`chat-media/${threadId}/../../x.jpg`), null);
  assert.equal(parseChatMediaPath(`chat-media/${threadId}/client_1234567890123456.exe`), null);
});
