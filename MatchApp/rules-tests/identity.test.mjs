import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc } from 'firebase/firestore';

const projectId = 'matchapp-rules-test-identity';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => { await env?.cleanup(); });

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'users/alice'), {
      firebaseUid: 'alice', displayName: 'Alice', age: 28, gender: 'FEMALE', lookingFor: 'MALE',
      city: 'Hyderabad', religion: 'Hindu', isPremium: false, isVerified: false,
      verificationLevel: 0, subscriptionPlan: 'FREE', subscriptionExpiry: 0, stealthMode: false,
      lastActiveAt: 1000, matrimonyId: 'MAT-ABCDEFGH2345',
    });
    await setDoc(doc(context.firestore(), 'usernames/alice_28'), { uid: 'alice', username: 'alice_28' });
    await setDoc(doc(context.firestore(), 'matrimonyIds/MAT-ABCDEFGH2345'), {
      uid: 'alice', createdAt: new Date(),
    });
  });
});

test('profile owner cannot bypass unique username reservation with direct user update', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(updateDoc(doc(db, 'users/alice'), { username: 'admin', usernameNormalized: 'admin' }));
});

test('clients cannot forge online activity timestamps', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(updateDoc(doc(db, 'users/alice'), { lastActiveAt: Date.now() }));
});

test('clients cannot enumerate or mutate the username registry directly', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(getDoc(doc(db, 'usernames/alice_28')));
  await assertFails(setDoc(doc(db, 'usernames/stolen'), { uid: 'alice', username: 'stolen' }));
});

test('clients cannot enumerate or mutate the matrimony ID registry', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(getDoc(doc(db, 'matrimonyIds/MAT-ABCDEFGH2345')));
  await assertFails(setDoc(doc(db, 'matrimonyIds/MAT-STOLEN234567'), { uid: 'alice' }));
});
