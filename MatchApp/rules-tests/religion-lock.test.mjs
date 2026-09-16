import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  doc,
  setDoc,
  updateDoc,
} from 'firebase/firestore';

const projectId = 'matchapp-religion-lock-test';
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

test('owner may create an unlocked profile before religion confirmation', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertSucceeds(setDoc(doc(db, 'users/alice'), baseProfile()));
});

test('owner may confirm and lock a non-empty religion in one update', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await setDoc(doc(db, 'users/alice'), baseProfile());
  await assertSucceeds(updateDoc(doc(db, 'users/alice'), {
    religion: 'Hindu',
    religionLocked: true,
    religionConfirmedAt: 1_700_000_000_000,
  }));
});

test('owner cannot lock an empty religion', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await setDoc(doc(db, 'users/alice'), baseProfile());
  await assertFails(updateDoc(doc(db, 'users/alice'), {
    religionLocked: true,
    religionConfirmedAt: 1_700_000_000_000,
  }));
});

test('owner cannot change religion after confirmation lock', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await setDoc(doc(db, 'users/alice'), baseProfile({
    religion: 'Hindu',
    religionLocked: true,
    religionConfirmedAt: 1_700_000_000_000,
  }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { religion: 'Christian' }));
});

test('owner cannot clear the religion lock after confirmation', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await setDoc(doc(db, 'users/alice'), baseProfile({
    religion: 'Hindu',
    religionLocked: true,
    religionConfirmedAt: 1_700_000_000_000,
  }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { religionLocked: false }));
});

test('owner cannot rewrite religion confirmation timestamp after lock', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await setDoc(doc(db, 'users/alice'), baseProfile({
    religion: 'Hindu',
    religionLocked: true,
    religionConfirmedAt: 1_700_000_000_000,
  }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { religionConfirmedAt: 1_800_000_000_000 }));
});

test('ordinary profile updates continue after religion lock', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await setDoc(doc(db, 'users/alice'), baseProfile({
    religion: 'Hindu',
    religionLocked: true,
    religionConfirmedAt: 1_700_000_000_000,
  }));
  await assertSucceeds(updateDoc(doc(db, 'users/alice'), { displayName: 'Alice S.' }));
});
