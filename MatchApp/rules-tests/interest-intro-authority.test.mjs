import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  deleteDoc,
  doc,
  getDoc,
  setDoc,
  updateDoc,
} from 'firebase/firestore';

const projectId = 'matchapp-interest-intro-authority-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => env?.cleanup());
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users/alice'), { firebaseUid: 'alice', accountStatus: 'ACTIVE' });
    await setDoc(doc(db, 'users/bob'), { firebaseUid: 'bob', accountStatus: 'ACTIVE' });
    await setDoc(doc(db, 'users/carol'), { firebaseUid: 'carol', accountStatus: 'ACTIVE' });
    await setDoc(doc(db, 'interests/alice_bob'), {
      fromUid: 'alice',
      toUid: 'bob',
      introNote: 'I liked your profile and would be happy to connect.',
      isSuperLike: false,
      createdAt: new Date(),
    });
  });
});

test('only interest participants can read a server-created introduction', async () => {
  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  const carol = env.authenticatedContext('carol').firestore();
  const anon = env.unauthenticatedContext().firestore();

  await assertSucceeds(getDoc(doc(alice, 'interests/alice_bob')));
  await assertSucceeds(getDoc(doc(bob, 'interests/alice_bob')));
  await assertFails(getDoc(doc(carol, 'interests/alice_bob')));
  await assertFails(getDoc(doc(anon, 'interests/alice_bob')));
});

test('clients cannot forge, edit, or delete interest introductions', async () => {
  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();

  await assertFails(setDoc(doc(alice, 'interests/alice_carol'), {
    fromUid: 'alice',
    toUid: 'carol',
    introNote: 'bypass trusted callable',
    createdAt: new Date(),
  }));
  await assertFails(updateDoc(doc(alice, 'interests/alice_bob'), {
    introNote: 'edited after server validation',
  }));
  await assertFails(updateDoc(doc(bob, 'interests/alice_bob'), {
    introNote: 'recipient edit',
  }));
  await assertFails(deleteDoc(doc(alice, 'interests/alice_bob')));
  await assertFails(deleteDoc(doc(bob, 'interests/alice_bob')));
});

test('block immediately revokes introduction visibility in both directions', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'blocks/bob/blocked/alice'), {
      blockedUid: 'alice',
      blockedAt: Date.now(),
    });
  });

  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  await assertFails(getDoc(doc(alice, 'interests/alice_bob')));
  await assertFails(getDoc(doc(bob, 'interests/alice_bob')));
});

test('inactive account immediately revokes introduction visibility', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await updateDoc(doc(ctx.firestore(), 'users/alice'), { accountStatus: 'DELETED' });
  });

  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  await assertFails(getDoc(doc(alice, 'interests/alice_bob')));
  await assertFails(getDoc(doc(bob, 'interests/alice_bob')));
});

test('decline response ledger remains server-only', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'interestResponses/alice_bob'), {
      senderUid: 'alice',
      recipientUid: 'bob',
      status: 'declined',
      respondedAt: new Date(),
    });
  });

  for (const uid of ['alice', 'bob', 'carol']) {
    const db = env.authenticatedContext(uid).firestore();
    await assertFails(getDoc(doc(db, 'interestResponses/alice_bob')));
  }
  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(alice, 'interestResponses/alice_bob'), {
    senderUid: 'alice',
    recipientUid: 'bob',
    status: 'declined',
  }));
});
