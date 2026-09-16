import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { doc, setDoc, updateDoc } from 'firebase/firestore';

const projectId = 'matchapp-religion-rules-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
});

async function createUnconfirmedProfile(uid = 'alice') {
  const db = env.authenticatedContext(uid).firestore();
  await assertSucceeds(setDoc(doc(db, `users/${uid}`), {
    firebaseUid: uid,
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
  }));
  return db;
}

test('client-created profile must begin with unconfirmed religion', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(db, 'users/alice'), {
    firebaseUid: 'alice', displayName: 'Alice', religion: 'Hindu',
    isPremium: false, isVerified: false, verificationLevel: 0,
    subscriptionPlan: 'FREE', subscriptionExpiry: 0,
  }));
  await createUnconfirmedProfile();
});

test('client cannot directly confirm or change canonical religion', async () => {
  const db = await createUnconfirmedProfile();
  await assertFails(updateDoc(doc(db, 'users/alice'), { religion: 'Hindu' }));
  await assertSucceeds(updateDoc(doc(db, 'users/alice'), { city: 'Secunderabad' }));
});

test('legacy populated profile can only be corrected through trusted backend', async () => {
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'users/alice'), {
      firebaseUid: 'alice', displayName: 'Alice', religion: 'Hindu', city: 'Hyderabad',
    });
  });
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(updateDoc(doc(db, 'users/alice'), { religion: 'Muslim' }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { religion: '' }));
  await assertSucceeds(updateDoc(doc(db, 'users/alice'), { city: 'Chennai' }));
});

test('client cannot forge religion authority metadata', async () => {
  const db = await createUnconfirmedProfile();
  await assertFails(updateDoc(doc(db, 'users/alice'), { religionId: 'HINDU' }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { religionLocked: false }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { religionConfirmedAt: new Date() }));
});

test('arbitrary religion values cannot be written through profile updates', async () => {
  const db = await createUnconfirmedProfile();
  await assertFails(updateDoc(doc(db, 'users/alice'), { religion: 'arbitrary-client-value' }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { religion: 'Prefer not to say' }));
});
