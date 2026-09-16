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
} from 'firebase/firestore';

const projectId = 'matchapp-block-boundary-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => env?.cleanup());
beforeEach(async () => env.clearFirestore());

async function seedRelationship() {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users/alice'), { firebaseUid: 'alice', displayName: 'Alice', stealthMode: false });
    await setDoc(doc(db, 'users/bob'), { firebaseUid: 'bob', displayName: 'Bob', stealthMode: false });
    await setDoc(doc(db, 'interests/alice_bob'), { fromUid: 'alice', toUid: 'bob' });
    await setDoc(doc(db, 'interests/bob_alice'), { fromUid: 'bob', toUid: 'alice' });
    await setDoc(doc(db, 'matches/alice_bob'), { users: ['alice', 'bob'] });
    await setDoc(doc(db, 'chats/alice_bob'), { participantUids: ['alice', 'bob'], lastMessage: 'Hi', lastSentAt: 1 });
    await setDoc(doc(db, 'chats/alice_bob/messages/m1'), {
      fromFirebaseUid: 'alice', toFirebaseUid: 'bob', body: 'Hi', sentAt: 1, isRead: false,
    });
  });
}

test('relationship and chat are readable before block', async () => {
  await seedRelationship();
  const alice = env.authenticatedContext('alice').firestore();
  await assertSucceeds(getDoc(doc(alice, 'interests/alice_bob')));
  await assertSucceeds(getDoc(doc(alice, 'matches/alice_bob')));
  await assertSucceeds(getDoc(doc(alice, 'chats/alice_bob')));
  await assertSucceeds(getDoc(doc(alice, 'chats/alice_bob/messages/m1')));
});

test('block immediately closes interest match and chat reads both ways', async () => {
  await seedRelationship();
  const alice = env.authenticatedContext('alice').firestore();
  await assertSucceeds(setDoc(doc(alice, 'blocks/alice/blocked/bob'), {
    blockedUid: 'bob',
    blockedAt: Date.now(),
  }));

  const bob = env.authenticatedContext('bob').firestore();
  for (const db of [alice, bob]) {
    await assertFails(getDoc(doc(db, 'interests/alice_bob')));
    await assertFails(getDoc(doc(db, 'matches/alice_bob')));
    await assertFails(getDoc(doc(db, 'chats/alice_bob')));
    await assertFails(getDoc(doc(db, 'chats/alice_bob/messages/m1')));
  }
});

test('member cannot create another account block or self-block', async () => {
  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(alice, 'blocks/bob/blocked/carol'), {
    blockedUid: 'carol', blockedAt: Date.now(),
  }));
  await assertFails(setDoc(doc(alice, 'blocks/alice/blocked/alice'), {
    blockedUid: 'alice', blockedAt: Date.now(),
  }));
});
