import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { doc, setDoc, updateDoc } from 'firebase/firestore';

const projectId = 'matchapp-profile-created-for-test';
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
    stealthMode: false,
    profileRevision: 0,
    profileCreatedFor: 'SELF',
    ...overrides,
  };
}

test('owner can use canonical family-assisted profile relationships', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertSucceeds(setDoc(doc(db, 'users/alice'), baseProfile()));
  await assertSucceeds(updateDoc(doc(db, 'users/alice'), {
    profileCreatedFor: 'DAUGHTER',
    profileRevision: 1,
  }));
  await assertSucceeds(updateDoc(doc(db, 'users/alice'), {
    profileCreatedFor: 'SIBLING',
    profileRevision: 2,
  }));
});

test('unknown profile-created-for values are rejected on create and update', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(db, 'users/alice'), baseProfile({ profileCreatedFor: 'AGENT' })));
  await assertSucceeds(setDoc(doc(db, 'users/alice'), baseProfile()));
  await assertFails(updateDoc(doc(db, 'users/alice'), {
    profileCreatedFor: 'AGENT',
    profileRevision: 1,
  }));
});
