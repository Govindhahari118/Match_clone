import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, serverTimestamp, setDoc, updateDoc, writeBatch } from 'firebase/firestore';

const projectId = 'matchapp-chat-atomic-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => env?.cleanup());
beforeEach(async () => env.clearFirestore());

async function seedUsersAndMutual() {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users/alice'), { firebaseUid: 'alice', stealthMode: false });
    await setDoc(doc(db, 'users/bob'), { firebaseUid: 'bob', stealthMode: false });
    await setDoc(doc(db, 'interests/alice_bob'), { fromUid: 'alice', toUid: 'bob' });
    await setDoc(doc(db, 'interests/bob_alice'), { fromUid: 'bob', toUid: 'alice' });
  });
}

function firstMessageBatch(db, body = 'hello') {
  const batch = writeBatch(db);
  batch.set(doc(db, 'chats/thread1'), {
    participantUids: ['alice', 'bob'],
    lastMessage: body,
    lastSentAt: Date.now(),
  });
  batch.set(doc(db, 'chats/thread1/messages/client_1234567890'), {
    body,
    sentAt: Date.now(),
    isRead: false,
    fromFirebaseUid: 'alice',
    toFirebaseUid: 'bob',
  });
  return batch;
}

test('mutual members may atomically create a thread and first message', async () => {
  await seedUsersAndMutual();
  const alice = env.authenticatedContext('alice').firestore();
  await assertSucceeds(firstMessageBatch(alice).commit());
});

test('first-message batch is denied without mutual interests', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users/alice'), { firebaseUid: 'alice', stealthMode: false });
    await setDoc(doc(db, 'users/bob'), { firebaseUid: 'bob', stealthMode: false });
    await setDoc(doc(db, 'interests/alice_bob'), { fromUid: 'alice', toUid: 'bob' });
  });
  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(firstMessageBatch(alice).commit());
});

test('first-message batch is denied when either member blocked the other', async () => {
  await seedUsersAndMutual();
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'blocks/bob/blocked/alice'), { blockedAt: Date.now() });
  });
  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(firstMessageBatch(alice).commit());
});


test('only recipient may acknowledge delivery and read state', async () => {
  await seedUsersAndMutual();
  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  await assertSucceeds(firstMessageBatch(alice).commit());

  const aliceMessage = doc(alice, 'chats/thread1/messages/client_1234567890');
  const bobMessage = doc(bob, 'chats/thread1/messages/client_1234567890');

  await assertFails(updateDoc(aliceMessage, { deliveredAt: serverTimestamp() }));
  await assertSucceeds(updateDoc(bobMessage, { deliveredAt: serverTimestamp() }));
  await assertSucceeds(updateDoc(bobMessage, {
    isRead: true,
    readAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(bobMessage, { isRead: false }));
});

test('block revokes recipient receipt updates immediately', async () => {
  await seedUsersAndMutual();
  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  await assertSucceeds(firstMessageBatch(alice).commit());

  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'blocks/alice/blocked/bob'), {
      blockedUid: 'bob',
      blockedAt: Date.now(),
    });
  });

  await assertFails(updateDoc(
    doc(bob, 'chats/thread1/messages/client_1234567890'),
    { deliveredAt: serverTimestamp() },
  ));
});
