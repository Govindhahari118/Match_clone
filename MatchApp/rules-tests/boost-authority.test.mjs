import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  doc,
  getDoc,
  setDoc,
} from 'firebase/firestore';

const projectId = 'matchapp-boost-authority-test';
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

test('client cannot manufacture or inspect private boost entitlement state', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(db, 'subscriptions/alice'), {
    boostUntil: Date.now() + 7 * 24 * 60 * 60 * 1000,
  }));
  await assertFails(getDoc(doc(db, 'subscriptions/alice')));
});

test('client cannot manufacture nested subscription usage or entitlement state', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(db, 'subscriptions/alice/usage/boost'), {
    boostUntil: Date.now() + 60 * 60 * 1000,
  }));
});
