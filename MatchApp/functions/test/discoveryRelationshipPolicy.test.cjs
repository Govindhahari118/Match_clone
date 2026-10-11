const test = require("node:test");
const assert = require("node:assert/strict");
const { discoveryRelationship } = require("../lib/discoveryRelationshipPolicy");

test("no verified interest or match confers any relationship visibility", () => {
  assert.deepEqual(discoveryRelationship("alice", "bob"), { interested: false, mutual: false });
  assert.deepEqual(
    discoveryRelationship("alice", "bob", { fromUid: "other", toUid: "bob" }),
    { interested: false, mutual: false }
  );
});

test("one real interest confers interested but not mutual visibility", () => {
  assert.deepEqual(
    discoveryRelationship("alice", "bob", { fromUid: "alice", toUid: "bob" }),
    { interested: true, mutual: false }
  );
  assert.deepEqual(
    discoveryRelationship("alice", "bob", undefined, { fromUid: "bob", toUid: "alice" }),
    { interested: true, mutual: false }
  );
});

test("exact reciprocal interest and exact match establish mutual visibility", () => {
  const forward = { fromUid: "alice", toUid: "bob" };
  const reverse = { fromUid: "bob", toUid: "alice" };
  assert.deepEqual(
    discoveryRelationship("alice", "bob", forward, reverse),
    { interested: true, mutual: true }
  );
  assert.deepEqual(
    discoveryRelationship("alice", "bob", undefined, undefined, { users: ["bob", "alice"] }),
    { interested: false, mutual: true }
  );
});

test("colliding pair IDs cannot unlock interest or mutual last-active filters", () => {
  const wrongForward = { fromUid: "a_b", toUid: "c" };
  const wrongReverse = { fromUid: "c", toUid: "a_b" };
  const wrongMatch = { users: ["a_b", "c"] };
  assert.deepEqual(
    discoveryRelationship("a", "b_c", wrongForward, wrongReverse, wrongMatch),
    { interested: false, mutual: false }
  );
  assert.deepEqual(
    discoveryRelationship("a", "b_c", undefined, undefined, { users: ["a", "a"] }),
    { interested: false, mutual: false }
  );
});
