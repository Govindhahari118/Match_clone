import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc } from 'firebase/firestore';

const projectId = 'matchapp-chat-preferences-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => env?.cleanup());
beforeEach(async () => env.clearFirestore());

async function seedPreference() {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'chatPreferences/alice/threads/thread1'), {
      ownerUid: 'alice',
      threadId: 'thread1',
      peerUid: 'bob',
      muted: true,
      archived: false,
      updatedAt: new Date(),
    });
  });
}

test('only the preference owner can read chat mute/archive state', async () => {
  await seedPreference();

  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  const anon = env.unauthenticatedContext().firestore();

  await assertSucceeds(getDoc(doc(alice, 'chatPreferences/alice/threads/thread1')));
  await assertFails(getDoc(doc(bob, 'chatPreferences/alice/threads/thread1')));
  await assertFails(getDoc(doc(anon, 'chatPreferences/alice/threads/thread1')));
});

test('clients cannot create or mutate chat preference documents directly', async () => {
  await seedPreference();

  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(updateDoc(doc(alice, 'chatPreferences/alice/threads/thread1'), {
    muted: false,
  }));
  await assertFails(setDoc(doc(alice, 'chatPreferences/alice/threads/thread2'), {
    ownerUid: 'alice',
    threadId: 'thread2',
    peerUid: 'carol',
    muted: false,
    archived: true,
  }));
});
