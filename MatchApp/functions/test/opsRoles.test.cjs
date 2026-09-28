const assert = require("node:assert/strict");
const test = require("node:test");

const { resolveOpsRole } = require("../lib/shared");

test("operations role resolver grants only an allowed claimed role", () => {
  assert.equal(
    resolveOpsRole(["support"], ["support", "ops_admin"]),
    "support"
  );
  assert.equal(
    resolveOpsRole(["moderator", "support"], ["moderator", "ops_admin"]),
    "moderator"
  );
});

test("operations role resolver denies missing, malformed and unrelated claims", () => {
  assert.equal(resolveOpsRole(undefined, ["support"]), undefined);
  assert.equal(resolveOpsRole("support", ["support"]), undefined);
  assert.equal(resolveOpsRole(["member"], ["support", "ops_admin"]), undefined);
  assert.equal(resolveOpsRole([42, null, "member"], ["moderator"]), undefined);
});

test("operations role resolver respects least privilege rather than role presence alone", () => {
  assert.equal(resolveOpsRole(["support"], ["payment_ops"]), undefined);
  assert.equal(resolveOpsRole(["payment_ops"], ["moderator"]), undefined);
  assert.equal(resolveOpsRole(["ops_admin"], ["support", "ops_admin"]), "ops_admin");
});
