import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc } from 'firebase/firestore';

const projectId = 'matchapp-account-lifecycle-boundary-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: {
      rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'),
    },
  });
});

after(async () => env?.cleanup());
beforeEach(async () => env.clearFirestore());

async function seedPair(statusA = 'ACTIVE', statusB = 'ACTIVE') {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users/alice'), { accountStatus: statusA });
    await setDoc(doc(db, 'users/bob'), { accountStatus: statusB });
    await setDoc(doc(db, 'interests/alice_bob'), { fromUid: 'alice', toUid: 'bob' });
    await setDoc(doc(db, 'interests/bob_alice'), { fromUid: 'bob', toUid: 'alice' });
    await setDoc(doc(db, 'matches/alice_bob'), { users: ['alice', 'bob'] });
    await setDoc(doc(db, 'chats/alice_bob'), {
      participantUids: ['alice', 'bob'],
      lastMessage: '',
      lastSentAt: 0,
    });
    await setDoc(doc(db, 'chats/alice_bob/messages/msg_abcdefghijklmnop'), {
      body: 'hello',
      sentAt: 1,
      isRead: false,
      fromFirebaseUid: 'alice',
      toFirebaseUid: 'bob',
    });
  });
}

test('active pair may read an existing chat and match', async () => {
  await seedPair();
  const alice = env.authenticatedContext('alice').firestore();
  await assertSucceeds(getDoc(doc(alice, 'matches/alice_bob')));
  await assertSucceeds(getDoc(doc(alice, 'chats/alice_bob')));
  await assertSucceeds(getDoc(doc(alice, 'chats/alice_bob/messages/msg_abcdefghijklmnop')));
});

test('suspended member cannot keep using old match or chat records', async () => {
  await seedPair('SUSPENDED', 'ACTIVE');
  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();

  await assertFails(getDoc(doc(alice, 'matches/alice_bob')));
  await assertFails(getDoc(doc(alice, 'chats/alice_bob')));
  await assertFails(getDoc(doc(bob, 'chats/alice_bob')));
  await assertFails(getDoc(doc(bob, 'chats/alice_bob/messages/msg_abcdefghijklmnop')));

  await assertFails(updateDoc(doc(alice, 'chats/alice_bob'), {
    lastMessage: 'bypass',
    lastSentAt: 2,
  }));
});


test('member cannot directly rewrite server-owned pause lifecycle fields', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users/alice'), {
      accountStatus: 'ACTIVE',
      userPaused: false,
      matrimonyPaused: false,
      searchStatus: 'ACTIVE',
      profileRevision: 0,
    });
  });

  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(updateDoc(doc(alice, 'users/alice'), {
    userPaused: true,
    matrimonyPaused: true,
    searchStatus: 'PAUSED',
    pausedAt: Date.now(),
    profileRevision: 1,
  }));
});
