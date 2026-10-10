const assert = require("node:assert/strict");
const test = require("node:test");
const { localizedNotificationCopy } = require("../lib/notificationCopyPolicy");

test("call and photo events deliver supported-language copy without private content", () => {
  for (const type of [
    "CALL_REQUEST", "CALL_ACCEPTED", "CALL_DECLINED", "CALL_CANCELLED",
    "PHOTO_REQUEST", "PHOTO_ACCESS_REQUEST",
  ]) {
    const copy = localizedNotificationCopy(type);
    for (const locale of ["en", "hi", "te"]) {
      assert.ok(copy[locale].title.length > 0);
      assert.ok(copy[locale].body.length > 0);
      assert.equal(/https?:|\+\d{8}|@/.test(copy[locale].body), false);
    }
    assert.notEqual(copy.en.body, copy.te.body);
    assert.notEqual(copy.en.body, copy.hi.body);
  }
});

test("missing-photo and access-consent notifications remain distinct", () => {
  assert.notDeepEqual(
    localizedNotificationCopy("PHOTO_REQUEST"),
    localizedNotificationCopy("PHOTO_ACCESS_REQUEST")
  );
  assert.deepEqual(localizedNotificationCopy("UNSUPPORTED_EVENT"), {});
});
