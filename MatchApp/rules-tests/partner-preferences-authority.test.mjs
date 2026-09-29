import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc } from 'firebase/firestore';

const projectId = 'matchapp-partner-preferences-test';
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

test('partner preference documents are never directly readable or writable by clients', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'partnerPreferences/alice'), {
      configured: true,
      cityMode: 'STRICT',
      cities: ['Hyderabad'],
    });
  });

  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();

  await assertFails(getDoc(doc(alice, 'partnerPreferences/alice')));
  await assertFails(getDoc(doc(bob, 'partnerPreferences/alice')));
  await assertFails(setDoc(doc(alice, 'partnerPreferences/alice'), {
    configured: true,
    cityMode: 'NO_PREFERENCE',
  }));
});
