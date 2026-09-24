import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { doc, setDoc, updateDoc } from 'firebase/firestore';

const projectId = 'matchapp-religion-lock-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => { await env?.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });

function baseProfile(overrides = {}) {
  return {
    firebaseUid: 'alice',
    displayName: 'Alice',
    age: 28,
    gender: 'FEMALE',
    lookingFor: 'MALE',
    city: 'Hyderabad',
    religion: '',
    isPremium: false,
    isVerified: false,
    verificationLevel: 0,
    subscriptionPlan: 'FREE',
    subscriptionExpiry: 0,
    stealthMode: false,
    ...overrides,
  };
}

test('client-created profile begins with unconfirmed religion authority', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertSucceeds(setDoc(doc(db, 'users/alice'), baseProfile()));
  await assertFails(setDoc(doc(db, 'users/bob'), baseProfile({
    firebaseUid: 'bob',
    religion: 'Hindu',
    religionId: 'HINDU',
    religionLocked: true,
    religionConfirmedAt: Date.now(),
  })));
});

test('owner cannot directly confirm religion or forge lock metadata', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await setDoc(doc(db, 'users/alice'), baseProfile());

  await assertFails(updateDoc(doc(db, 'users/alice'), { religion: 'Hindu' }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { religionId: 'HINDU' }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { religionLocked: true }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { religionConfirmedAt: Date.now() }));
});

test('ordinary owner profile updates remain allowed while religion is server-controlled', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await setDoc(doc(db, 'users/alice'), baseProfile());
  await assertSucceeds(updateDoc(doc(db, 'users/alice'), { displayName: 'Alice S.' }));
});

test('locked canonical religion cannot be changed or unlocked by owner', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users/alice'), baseProfile({
      religion: 'Hindu',
      religionId: 'HINDU',
      religionLocked: true,
      religionConfirmedAt: Date.now(),
    }));
  });
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(updateDoc(doc(db, 'users/alice'), { religion: 'Christian' }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { religionLocked: false }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { religionConfirmedAt: Date.now() + 1000 }));
  await assertSucceeds(updateDoc(doc(db, 'users/alice'), { city: 'Secunderabad' }));
});

test('arbitrary religion values cannot enter the public profile', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(db, 'users/alice'), baseProfile({ religion: 'arbitrary-client-value' })));
});
