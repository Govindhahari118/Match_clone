const assert = require("node:assert/strict");
const test = require("node:test");

const {
  protectedObjectIdentity,
} = require("../lib/protectedMediaMigrationPolicy");

test("legacy Firebase download URL becomes owner-scoped gs identity", () => {
  const encoded = encodeURIComponent("photos/alice/profile 1.jpg");
  const value =
    "https://firebasestorage.googleapis.com/v0/b/match-app.appspot.com/o/" +
    encoded +
    "?alt=media&token=legacy-bearer-token";

  assert.equal(
    protectedObjectIdentity(value, "alice", "photoUrl"),
    "gs://match-app.appspot.com/photos/alice/profile 1.jpg"
  );
});

test("storage.googleapis.com owner media also becomes gs identity", () => {
  assert.equal(
    protectedObjectIdentity(
      "https://storage.googleapis.com/match-app.appspot.com/videos/alice/intro.mp4",
      "alice",
      "videoUrl"
    ),
    "gs://match-app.appspot.com/videos/alice/intro.mp4"
  );
});

test("cross-owner and wrong-root references are never migrated", () => {
  assert.equal(
    protectedObjectIdentity(
      "https://firebasestorage.googleapis.com/v0/b/match-app.appspot.com/o/" +
        encodeURIComponent("photos/bob/profile.jpg") +
        "?alt=media&token=x",
      "alice",
      "photoUrl"
    ),
    null
  );
  assert.equal(
    protectedObjectIdentity(
      "https://storage.googleapis.com/match-app.appspot.com/verifications/alice/id.jpg",
      "alice",
      "photoUrl"
    ),
    null
  );
});

test("non-Firebase and already protected references are left alone", () => {
  assert.equal(
    protectedObjectIdentity("https://example.com/photo.jpg", "alice", "photoUrl"),
    null
  );
  assert.equal(
    protectedObjectIdentity("gs://match-app.appspot.com/photos/alice/photo.jpg", "alice", "photoUrl"),
    null
  );
});
