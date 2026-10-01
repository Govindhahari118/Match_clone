const test = require("node:test");
const assert = require("node:assert/strict");

const { requestIdFromContext, operatorMfaSatisfied } = require("../lib/shared");

function contextWith(header) {
  return {
    rawRequest: {
      get(name) {
        assert.equal(name, "x-request-id");
        return header;
      },
    },
  };
}

test("accepts bounded safe caller correlation id", () => {
  assert.equal(requestIdFromContext(contextWith("req-123:abc")), "req-123:abc");
});

test("rejects unsafe or oversized correlation ids", () => {
  const unsafe = requestIdFromContext(contextWith("bad header with spaces"));
  assert.match(unsafe, /^[0-9a-f-]{36}$/i);

  const oversized = requestIdFromContext(contextWith("a".repeat(129)));
  assert.match(oversized, /^[0-9a-f-]{36}$/i);
});

test("generates id when no correlation header exists", () => {
  assert.match(requestIdFromContext(contextWith(undefined)), /^[0-9a-f-]{36}$/i);
});


test("recognizes Firebase second-factor claim for operator MFA", () => {
  assert.equal(operatorMfaSatisfied({
    firebase: { sign_in_second_factor: "phone" },
  }), true);
  assert.equal(operatorMfaSatisfied({
    firebase: { sign_in_provider: "password" },
  }), false);
  assert.equal(operatorMfaSatisfied(undefined), false);
});
