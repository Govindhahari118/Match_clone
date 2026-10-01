import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc } from 'firebase/firestore';

const projectId = 'matchapp-profile-readiness-test';
let env;

const complete = {
  firebaseUid: 'alice',
  username: 'alice123',
  displayName: 'Alice',
  age: 29,
  gender: 'FEMALE',
  lookingFor: 'MALE',
  state: 'Telangana',
  city: 'Hyderabad',
  motherTongue: 'Telugu',
  religion: 'Hindu',
  education: 'B.Tech',
  profession: 'Engineer',
  maritalStatus: 'Never Married',
  heightCm: 165,
  accountStatus: 'ACTIVE',
  stealthMode: false,
  profileRevision: 0,
};

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

test('owner may read an incomplete profile while peers cannot', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users/alice'), {
      firebaseUid: 'alice',
      displayName: 'Alice',
      accountStatus: 'ACTIVE',
      profileRevision: 0,
    });
  });

  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  await assertSucceeds(getDoc(doc(alice, 'users/alice')));
  await assertFails(getDoc(doc(bob, 'users/alice')));
});

test('peer may read a complete active profile', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users/alice'), complete);
  });

  const bob = env.authenticatedContext('bob').firestore();
  await assertSucceeds(getDoc(doc(bob, 'users/alice')));
});

test('complete profile is still hidden when account is not active', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users/alice'), {
      ...complete,
      accountStatus: 'RESTRICTED',
    });
  });

  const bob = env.authenticatedContext('bob').firestore();
  await assertFails(getDoc(doc(bob, 'users/alice')));
});
