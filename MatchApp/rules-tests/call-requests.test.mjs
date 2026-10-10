import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc } from 'firebase/firestore';

const projectId = 'matchapp-call-requests-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => env?.cleanup());
beforeEach(async () => env.clearFirestore());

async function seedActivePairAndRequest() {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users/alice'), { accountStatus: 'ACTIVE' });
    await setDoc(doc(db, 'users/bob'), { accountStatus: 'ACTIVE' });
    await setDoc(doc(db, 'callRequests/alice_bob'), {
      users: ['alice', 'bob'],
      requesterUid: 'alice',
      targetUid: 'bob',
      kind: 'VOICE',
      proposedAtMs: Date.now() + 3600000,
      status: 'PENDING',
    });
  });
}

test('only active request participants can read call coordination state', async () => {
  await seedActivePairAndRequest();
  await assertSucceeds(getDoc(doc(env.authenticatedContext('alice').firestore(), 'callRequests/alice_bob')));
  await assertSucceeds(getDoc(doc(env.authenticatedContext('bob').firestore(), 'callRequests/alice_bob')));
  await assertFails(getDoc(doc(env.authenticatedContext('mallory').firestore(), 'callRequests/alice_bob')));
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), 'callRequests/alice_bob')));
});

test('clients cannot create or mutate call coordination state', async () => {
  await seedActivePairAndRequest();
  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(updateDoc(doc(alice, 'callRequests/alice_bob'), { status: 'ACCEPTED' }));
  await assertFails(setDoc(doc(alice, 'callRequests/alice_carol'), {
    users: ['alice', 'carol'],
    requesterUid: 'alice',
    targetUid: 'carol',
    status: 'PENDING',
  }));
});

test('blocking and inactive accounts revoke participant reads', async () => {
  await seedActivePairAndRequest();
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'blocks/bob/blocked/alice'), { blockedUid: 'alice' });
  });
  await assertFails(getDoc(doc(env.authenticatedContext('alice').firestore(), 'callRequests/alice_bob')));
  await seedActivePairAndRequest();
  await env.clearFirestore();
  await seedActivePairAndRequest();
  await env.withSecurityRulesDisabled(async (ctx) => {
    await updateDoc(doc(ctx.firestore(), 'users/bob'), { accountStatus: 'SUSPENDED' });
  });
  await assertFails(getDoc(doc(env.authenticatedContext('bob').firestore(), 'callRequests/alice_bob')));
});
