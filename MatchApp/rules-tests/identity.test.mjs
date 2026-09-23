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
      lastActiveAt: 1000,
    });
    await setDoc(doc(context.firestore(), 'usernames/alice_28'), { uid: 'alice', username: 'alice_28' });
    await setDoc(doc(context.firestore(), 'matrimonyIdAssignments/alice'), {
      uid: 'alice', matrimonyId: 'MAT-0123456789ABCDEF', createdAt: Date.now(),
    });
    await setDoc(doc(context.firestore(), 'matrimonyIds/MAT-0123456789ABCDEF'), {
      uid: 'alice', createdAt: Date.now(),
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


test('clients cannot read or forge Matrimony ID reservation registries', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(getDoc(doc(db, 'matrimonyIdAssignments/alice')));
  await assertFails(getDoc(doc(db, 'matrimonyIds/MAT-0123456789ABCDEF')));
  await assertFails(setDoc(doc(db, 'matrimonyIdAssignments/alice'), {
    uid: 'alice', matrimonyId: 'MAT-FFFFFFFFFFFFFFFF', createdAt: Date.now(),
  }));
  await assertFails(setDoc(doc(db, 'matrimonyIds/MAT-FFFFFFFFFFFFFFFF'), {
    uid: 'alice', createdAt: Date.now(),
  }));
});


test('clients cannot grant boost entitlement directly', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(updateDoc(doc(db, 'users/alice'), {
    boostActiveUntil: Date.now() + 86_400_000,
  }));
});


test('client cannot forge account lifecycle status', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(updateDoc(doc(db, 'users/alice'), { accountStatus: 'ACTIVE' }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { accountStatus: 'DELETING' }));
});

test('deleting account is immediately hidden from peers but remains readable by owner', async () => {
  await env.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), 'users/alice'), {
      accountStatus: 'DELETING',
      stealthMode: true,
    });
  });
  const ownerDb = env.authenticatedContext('alice').firestore();
  const peerDb = env.authenticatedContext('bob').firestore();
  await assertSucceeds(getDoc(doc(ownerDb, 'users/alice')));
  await assertFails(getDoc(doc(peerDb, 'users/alice')));
});


test('paused account is hidden from peers but remains readable by owner', async () => {
  await env.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), 'users/alice'), {
      accountStatus: 'PAUSED',
    });
  });
  const ownerDb = env.authenticatedContext('alice').firestore();
  const peerDb = env.authenticatedContext('bob').firestore();
  await assertSucceeds(getDoc(doc(ownerDb, 'users/alice')));
  await assertFails(getDoc(doc(peerDb, 'users/alice')));
});
