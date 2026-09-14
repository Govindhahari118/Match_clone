import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  setDoc,
  updateDoc,
} from 'firebase/firestore';
import { getBytes, ref, uploadBytes } from 'firebase/storage';

const projectId = 'matchapp-rules-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
    storage: { rules: fs.readFileSync(new URL('../storage.rules', import.meta.url), 'utf8') },
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'users/alice'), {
      firebaseUid: 'alice', displayName: 'Alice', age: 28, gender: 'FEMALE', lookingFor: 'MALE',
      city: 'Hyderabad', religion: 'Hindu', isPremium: false, isVerified: false,
      verificationLevel: 0, subscriptionPlan: 'FREE', subscriptionExpiry: 0,
    });
    await setDoc(doc(db, 'users/bob'), {
      firebaseUid: 'bob', displayName: 'Bob', age: 30, gender: 'MALE', lookingFor: 'FEMALE',
      city: 'Hyderabad', religion: 'Hindu', isPremium: false, isVerified: false,
      verificationLevel: 0, subscriptionPlan: 'FREE', subscriptionExpiry: 0,
    });
    await setDoc(doc(db, 'userPrivate/alice'), { phoneNumber: '9999999999', email: 'alice@example.test' });
  });
});

test('unauthenticated users cannot read profiles', async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, 'users/alice')));
});

test('private account data is owner-only', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  const bobDb = env.authenticatedContext('bob').firestore();
  await assertSucceeds(getDoc(doc(aliceDb, 'userPrivate/alice')));
  await assertFails(getDoc(doc(bobDb, 'userPrivate/alice')));
});

test('clients cannot grant themselves premium or verification', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(updateDoc(doc(db, 'users/alice'), { isPremium: true }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { isVerified: true }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { verificationLevel: 5 }));
});

test('blocking prevents new interests in either direction', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  await assertSucceeds(setDoc(doc(aliceDb, 'blocks/alice/blocked/bob'), { blockedAt: Date.now() }));
  await assertFails(setDoc(doc(aliceDb, 'interests/alice_bob'), {
    fromUid: 'alice', toUid: 'bob', isSuperLike: false, createdAt: new Date(),
  }));
  const bobDb = env.authenticatedContext('bob').firestore();
  await assertFails(setDoc(doc(bobDb, 'interests/bob_alice'), {
    fromUid: 'bob', toUid: 'alice', isSuperLike: false, createdAt: new Date(),
  }));
});

test('chat thread requires mutual interest and stops after a block', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  const bobDb = env.authenticatedContext('bob').firestore();
  const thread = doc(aliceDb, 'chats/alice_bob');

  await assertSucceeds(setDoc(doc(aliceDb, 'interests/alice_bob'), {
    fromUid: 'alice', toUid: 'bob', isSuperLike: false, createdAt: new Date(),
  }));
  await assertFails(setDoc(thread, { participantUids: ['alice', 'bob'], lastMessage: '', lastSentAt: 0 }));

  await assertSucceeds(setDoc(doc(bobDb, 'interests/bob_alice'), {
    fromUid: 'bob', toUid: 'alice', isSuperLike: false, createdAt: new Date(),
  }));
  await assertSucceeds(setDoc(thread, { participantUids: ['alice', 'bob'], lastMessage: '', lastSentAt: 0 }));
  await assertSucceeds(setDoc(doc(aliceDb, 'chats/alice_bob/messages/m1'), {
    body: 'hello', sentAt: Date.now(), isRead: false,
    fromFirebaseUid: 'alice', toFirebaseUid: 'bob',
    voiceUri: null, imageUri: null, voiceDurationMs: null,
  }));

  await assertSucceeds(setDoc(doc(bobDb, 'blocks/bob/blocked/alice'), { blockedAt: Date.now() }));
  await assertFails(setDoc(doc(aliceDb, 'chats/alice_bob/messages/m2'), {
    body: 'blocked', sentAt: Date.now(), isRead: false,
    fromFirebaseUid: 'alice', toFirebaseUid: 'bob',
    voiceUri: null, imageUri: null, voiceDurationMs: null,
  }));
});

test('users cannot write payment authority documents', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(db, 'paymentOrders/order_fake'), { uid: 'alice', status: 'paid' }));
  await assertFails(setDoc(doc(db, 'payments/pay_fake'), { uid: 'alice', amount: 1 }));
});

test('profile photo upload is owner-only', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  const bobStorage = env.authenticatedContext('bob').storage();
  const bytes = new Uint8Array([1, 2, 3, 4]);
  await assertSucceeds(uploadBytes(ref(aliceStorage, 'photos/alice/profile.jpg'), bytes, { contentType: 'image/jpeg' }));
  await assertFails(uploadBytes(ref(bobStorage, 'photos/alice/attack.jpg'), bytes, { contentType: 'image/jpeg' }));
});

test('verification documents cannot be read by another client', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  const bobStorage = env.authenticatedContext('bob').storage();
  const bytes = new Uint8Array([1, 2, 3]);
  await assertSucceeds(uploadBytes(ref(aliceStorage, 'verifications/alice/id.jpg'), bytes, { contentType: 'image/jpeg' }));
  await assertFails(getBytes(ref(bobStorage, 'verifications/alice/id.jpg')));
});
