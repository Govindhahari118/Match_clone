import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc } from 'firebase/firestore';

const projectId = 'matchapp-photo-request-authority-test';
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

test('photo requests and rate limits are trusted-server authority only', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'photoRequests/request-1'), {
      requesterUid: 'alice',
      targetUid: 'bob',
      status: 'PENDING',
    });
    await setDoc(doc(ctx.firestore(), 'photoRequestRateLimits/rate-1'), {
      requesterUid: 'alice',
      day: '2026-10-04',
      count: 1,
    });
  });

  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();

  await assertFails(getDoc(doc(alice, 'photoRequests/request-1')));
  await assertFails(getDoc(doc(bob, 'photoRequests/request-1')));
  await assertFails(setDoc(doc(alice, 'photoRequests/request-1'), {
    requesterUid: 'alice',
    targetUid: 'bob',
    status: 'PENDING',
  }));
  await assertFails(getDoc(doc(alice, 'photoRequestRateLimits/rate-1')));
  await assertFails(setDoc(doc(alice, 'photoRequestRateLimits/rate-1'), {
    requesterUid: 'alice',
    day: '2026-10-04',
    count: 0,
  }));
});
