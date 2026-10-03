import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, serverTimestamp, setDoc, updateDoc, writeBatch } from 'firebase/firestore';

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

async function seedUsersAndMatch() {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users/alice'), {
      firebaseUid: 'alice',
      accountStatus: 'ACTIVE',
      stealthMode: false,
    });
    await setDoc(doc(db, 'users/bob'), {
      firebaseUid: 'bob',
      accountStatus: 'ACTIVE',
      stealthMode: false,
    });
    await setDoc(doc(db, 'matches/alice_bob'), {
      users: ['alice', 'bob'],
      createdAt: Date.now(),
    });
  });
}

async function seedServerThreadAndMessage({
  deliveredAt = null,
  isRead = false,
  readAt = null,
} = {}) {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'chats/thread1'), {
      participantUids: ['alice', 'bob'],
      lastMessage: 'hello',
      lastSentAt: Date.now(),
    });
    const message = {
      body: 'hello',
      sentAt: Date.now(),
      isRead,
      fromFirebaseUid: 'alice',
      toFirebaseUid: 'bob',
    };
    if (deliveredAt) message.deliveredAt = deliveredAt;
    if (readAt) message.readAt = readAt;
    await setDoc(doc(db, 'chats/thread1/messages/client_1234567890123456'), message);
  });
}

function forgedFirstMessageBatch(db) {
  const batch = writeBatch(db);
  batch.set(doc(db, 'chats/thread1'), {
    participantUids: ['alice', 'bob'],
    lastMessage: 'hello',
    lastSentAt: Date.now(),
  });
  batch.set(doc(db, 'chats/thread1/messages/client_1234567890123456'), {
    body: 'hello',
    sentAt: Date.now(),
    isRead: false,
    fromFirebaseUid: 'alice',
    toFirebaseUid: 'bob',
  });
  return batch;
}

test('clients cannot create a chat thread or first message even for a valid match', async () => {
  await seedUsersAndMatch();
  const alice = env.authenticatedContext('alice').firestore();

  await assertFails(forgedFirstMessageBatch(alice).commit());
  await assertFails(setDoc(doc(alice, 'chats/thread1'), {
    participantUids: ['alice', 'bob'],
    lastMessage: '',
    lastSentAt: 0,
  }));
});

test('clients cannot append messages or mutate server-owned thread previews', async () => {
  await seedUsersAndMatch();
  await seedServerThreadAndMessage();
  const alice = env.authenticatedContext('alice').firestore();

  await assertFails(setDoc(doc(alice, 'chats/thread1/messages/client_forged_123456789'), {
    body: 'forged',
    sentAt: Date.now(),
    isRead: false,
    fromFirebaseUid: 'alice',
    toFirebaseUid: 'bob',
  }));
  await assertFails(updateDoc(doc(alice, 'chats/thread1'), {
    lastMessage: 'forged preview',
    lastSentAt: Date.now(),
  }));
});

test('only participants can read a server-created thread and messages', async () => {
  await seedUsersAndMatch();
  await seedServerThreadAndMessage();

  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  const mallory = env.authenticatedContext('mallory').firestore();

  await assertSucceeds(getDoc(doc(alice, 'chats/thread1')));
  await assertSucceeds(getDoc(doc(bob, 'chats/thread1')));
  await assertFails(getDoc(doc(mallory, 'chats/thread1')));
  await assertSucceeds(getDoc(doc(bob, 'chats/thread1/messages/client_1234567890123456')));
  await assertFails(getDoc(doc(mallory, 'chats/thread1/messages/client_1234567890123456')));
});

test('only recipient may acknowledge delivery and delivered-before-read is enforced', async () => {
  await seedUsersAndMatch();
  await seedServerThreadAndMessage();

  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  const aliceMessage = doc(alice, 'chats/thread1/messages/client_1234567890123456');
  const bobMessage = doc(bob, 'chats/thread1/messages/client_1234567890123456');

  await assertFails(updateDoc(aliceMessage, { deliveredAt: serverTimestamp() }));
  await assertFails(updateDoc(bobMessage, {
    isRead: true,
    readAt: serverTimestamp(),
  }));
  await assertSucceeds(updateDoc(bobMessage, { deliveredAt: serverTimestamp() }));
  await assertSucceeds(updateDoc(bobMessage, {
    isRead: true,
    readAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(bobMessage, { isRead: false }));
});

test('block immediately revokes thread, message and receipt access', async () => {
  await seedUsersAndMatch();
  await seedServerThreadAndMessage();

  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'blocks/alice/blocked/bob'), {
      blockedUid: 'bob',
      blockedAt: Date.now(),
    });
  });

  const bob = env.authenticatedContext('bob').firestore();
  await assertFails(getDoc(doc(bob, 'chats/thread1')));
  await assertFails(getDoc(doc(bob, 'chats/thread1/messages/client_1234567890123456')));
  await assertFails(updateDoc(
    doc(bob, 'chats/thread1/messages/client_1234567890123456'),
    { deliveredAt: serverTimestamp() },
  ));
});

test('inactive account immediately loses chat read and receipt access', async () => {
  await seedUsersAndMatch();
  await seedServerThreadAndMessage();

  await env.withSecurityRulesDisabled(async (ctx) => {
    await updateDoc(doc(ctx.firestore(), 'users/bob'), { accountStatus: 'SUSPENDED' });
  });

  const bob = env.authenticatedContext('bob').firestore();
  await assertFails(getDoc(doc(bob, 'chats/thread1')));
  await assertFails(getDoc(doc(bob, 'chats/thread1/messages/client_1234567890123456')));
  await assertFails(updateDoc(
    doc(bob, 'chats/thread1/messages/client_1234567890123456'),
    { deliveredAt: serverTimestamp() },
  ));
});


test('typing state is server-owned and readable only by valid chat participants', async () => {
  await seedUsersAndMatch();
  await seedServerThreadAndMessage();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'chats/thread1/typing/alice'), {
      uid: 'alice',
      typing: true,
      expiresAtMillis: Date.now() + 7000,
      updatedAt: serverTimestamp(),
    });
  });

  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  const mallory = env.authenticatedContext('mallory').firestore();

  await assertSucceeds(getDoc(doc(bob, 'chats/thread1/typing/alice')));
  await assertSucceeds(getDoc(doc(alice, 'chats/thread1/typing/alice')));
  await assertFails(getDoc(doc(mallory, 'chats/thread1/typing/alice')));

  await assertFails(setDoc(doc(alice, 'chats/thread1/typing/alice'), {
    uid: 'alice',
    typing: true,
    expiresAtMillis: Date.now() + 7000,
    updatedAt: serverTimestamp(),
  }));
});


test('typing visibility closes immediately when either profile privacy relation hides the pair', async () => {
  await seedUsersAndMatch();
  await seedServerThreadAndMessage();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'chats/thread1/typing/alice'), {
      uid: 'alice',
      typing: true,
      expiresAtMillis: Date.now() + 7000,
      updatedAt: serverTimestamp(),
    });
    await setDoc(doc(db, 'privacyRelations/alice/members/bob'), {
      profileHidden: true,
    });
  });

  const bob = env.authenticatedContext('bob').firestore();
  await assertFails(getDoc(doc(bob, 'chats/thread1/typing/alice')));
});
