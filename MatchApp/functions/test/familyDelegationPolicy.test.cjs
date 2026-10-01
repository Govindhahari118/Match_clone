const assert = require("node:assert/strict");
const test = require("node:test");

const {
  normalizeFamilyRole,
  normalizeFamilyPermissions,
  sanitizeFamilyProfilePatch,
  familyPatchContainsForbiddenFields,
} = require("../lib/familyDelegationPolicy");

test("family roles and permissions are allowlisted", () => {
  assert.equal(normalizeFamilyRole("parent"), "PARENT");
  assert.equal(normalizeFamilyRole("friend"), null);
  assert.deepEqual(
    normalizeFamilyPermissions(["VIEW_PROFILE", "EDIT_PROFILE", "ADMIN", "VIEW_PROFILE"]),
    ["VIEW_PROFILE", "EDIT_PROFILE"]
  );
});

test("delegated edits exclude identity, contact, religion, trust and billing fields", () => {
  const input = {
    bio: " Updated bio ",
    city: "Hyderabad",
    heightCm: 175,
    phoneNumber: "9999999999",
    religion: "Hindu",
    verificationLevel: 5,
    isPremium: true,
  };
  const patch = sanitizeFamilyProfilePatch(input);
  assert.deepEqual(patch, {
    bio: "Updated bio",
    city: "Hyderabad",
    heightCm: 175,
  });
  assert.equal(familyPatchContainsForbiddenFields(input, patch), true);
});

test("valid delegated patch passes without hidden dropped fields", () => {
  const input = { city: "Hyderabad", willingToRelocate: true, siblings: 2 };
  const patch = sanitizeFamilyProfilePatch(input);
  assert.equal(familyPatchContainsForbiddenFields(input, patch), false);
});
