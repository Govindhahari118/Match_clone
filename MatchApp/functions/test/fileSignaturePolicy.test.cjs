const assert = require("node:assert/strict");
const test = require("node:test");

const {
  fileSignatureMatchesMime,
  PROFILE_PHOTO_MIME_TYPES,
  PROFILE_VIDEO_MIME_TYPES,
  VERIFICATION_DOCUMENT_MIME_TYPES,
} = require("../lib/fileSignaturePolicy");

test("profile media allowlists are intentionally narrow", () => {
  assert.deepEqual([...PROFILE_PHOTO_MIME_TYPES], ["image/jpeg", "image/png", "image/webp"]);
  assert.deepEqual([...PROFILE_VIDEO_MIME_TYPES], ["video/mp4"]);
  assert.ok(VERIFICATION_DOCUMENT_MIME_TYPES.has("application/pdf"));
  assert.equal(PROFILE_PHOTO_MIME_TYPES.has("image/svg+xml"), false);
});

test("magic bytes must agree with declared image/PDF mime", () => {
  assert.equal(fileSignatureMatchesMime(Buffer.from([0xff,0xd8,0xff,0x01]), "image/jpeg"), true);
  assert.equal(
    fileSignatureMatchesMime(Buffer.from([0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a]), "image/png"),
    true
  );
  assert.equal(fileSignatureMatchesMime(Buffer.from("RIFFxxxxWEBP", "ascii"), "image/webp"), true);
  assert.equal(fileSignatureMatchesMime(Buffer.from("%PDF-1.7", "ascii"), "application/pdf"), true);
  assert.equal(fileSignatureMatchesMime(Buffer.from("%PDF-1.7", "ascii"), "image/jpeg"), false);
});

test("MP4 requires an ftyp box marker", () => {
  const mp4 = Buffer.concat([Buffer.from([0,0,0,24]), Buffer.from("ftypisom", "ascii")]);
  assert.equal(fileSignatureMatchesMime(mp4, "video/mp4"), true);
  assert.equal(fileSignatureMatchesMime(Buffer.from("not an mp4"), "video/mp4"), false);
});
