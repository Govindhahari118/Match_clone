import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  doc,
  getDoc,
  setDoc,
  updateDoc,
} from 'firebase/firestore';

const projectId = 'matchapp-profile-privacy-test';
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
    religion: 'Hindu',
    isPremium: false,
    isVerified: false,
    verificationLevel: 0,
    subscriptionPlan: 'FREE',
    subscriptionExpiry: 0,
    stealthMode: false,
    ...overrides,
  };
}

for (const field of ['email', 'phoneNumber', 'dateOfBirth', 'rasi', 'nakshatra', 'manglik', 'birthTime', 'birthPlace', 'incomeBand']) {
  test(`public profile rejects private field ${field}`, async () => {
    const db = env.authenticatedContext('alice').firestore();
    await assertFails(setDoc(doc(db, 'users/alice'), baseProfile({ [field]: 'private-value' })));
  });
}

test('owner may store private matrimonial inputs in userPrivate', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertSucceeds(setDoc(doc(db, 'userPrivate/alice'), {
    email: 'alice@example.test',
    phoneNumber: '+919999999999',
    dateOfBirth: '1998-01-01',
    rasi: 'Cancer',
    nakshatra: 'Pushya',
    manglik: 'No',
    birthTime: '10:10',
    birthPlace: 'Hyderabad',
    incomeBand: '10-20 LPA',
    updatedAt: Date.now(),
  }));
});

test('another member cannot read userPrivate', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'userPrivate/alice'), { phoneNumber: '+919999999999' });
  });
  const bob = env.authenticatedContext('bob').firestore();
  await assertFails(getDoc(doc(bob, 'userPrivate/alice')));
});

test('owner may set valid online and last-active visibility', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertSucceeds(setDoc(doc(db, 'privacySettings/alice'), {
    contactVisibility: 'mutual_matches',
    onlineVisibility: 'everyone',
    lastActiveVisibility: 'mutual',
    updatedAt: Date.now(),
  }));
  await assertSucceeds(updateDoc(doc(db, 'privacySettings/alice'), {
    onlineVisibility: 'nobody',
    lastActiveVisibility: 'interests',
  }));
});

test('invalid activity visibility is rejected', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(db, 'privacySettings/alice'), {
    onlineVisibility: 'public_forever',
    lastActiveVisibility: 'mutual',
    updatedAt: Date.now(),
  }));
});

test('client cannot read or write server-only presence', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'presencePrivate/alice'), { lastActiveAt: Date.now() });
  });
  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(getDoc(doc(alice, 'presencePrivate/alice')));
  await assertFails(setDoc(doc(alice, 'presencePrivate/alice'), { lastActiveAt: Date.now() }));
});
