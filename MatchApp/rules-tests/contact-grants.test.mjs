import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc } from 'firebase/firestore';

const projectId = 'matchapp-contact-grants-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => env?.cleanup());
beforeEach(async () => env.clearFirestore());

test('owner can choose selected-person contact visibility', async () => {
  const alice = env.authenticatedContext('alice').firestore();
  await assertSucceeds(setDoc(doc(alice, 'privacySettings/alice'), {
    contactVisibility: 'selected_people',
    updatedAt: Date.now(),
  }));
});

test('owner can grant selected contact methods to another member', async () => {
  const alice = env.authenticatedContext('alice').firestore();
  await assertSucceeds(setDoc(doc(alice, 'contactGrants/alice/viewers/bob'), {
    viewerUid: 'bob',
    phoneAllowed: true,
    whatsappAllowed: false,
    grantedAt: Date.now(),
    updatedAt: Date.now(),
  }));
});

test('viewer cannot inspect another member grant document', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'contactGrants/alice/viewers/bob'), {
      viewerUid: 'bob', phoneAllowed: true, whatsappAllowed: true,
    });
  });
  const bob = env.authenticatedContext('bob').firestore();
  await assertFails(getDoc(doc(bob, 'contactGrants/alice/viewers/bob')));
});

test('owner cannot create a grant for self or a disabled grant', async () => {
  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(alice, 'contactGrants/alice/viewers/alice'), {
    viewerUid: 'alice', phoneAllowed: true, whatsappAllowed: false,
    grantedAt: Date.now(), updatedAt: Date.now(),
  }));
  await assertFails(setDoc(doc(alice, 'contactGrants/alice/viewers/bob'), {
    viewerUid: 'bob', phoneAllowed: false, whatsappAllowed: false,
    grantedAt: Date.now(), updatedAt: Date.now(),
  }));
});
