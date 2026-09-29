import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc } from 'firebase/firestore';

const projectId = 'matchapp-consent-risk-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => env?.cleanup());
beforeEach(async () => env.clearFirestore());

test('member may read only their own server-written consent projection', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'consents/alice/items/location'), {
      purpose: 'location',
      granted: true,
      noticeVersion: '2026-09-29.1',
    });
  });

  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  await assertSucceeds(getDoc(doc(alice, 'consents/alice/items/location')));
  await assertFails(getDoc(doc(bob, 'consents/alice/items/location')));
  await assertFails(updateDoc(doc(alice, 'consents/alice/items/location'), { granted: false }));
});

test('member may read own consent ledger but cannot forge ledger events', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'consentLedger/alice/events/event-1'), {
      purpose: 'identity_verification',
      granted: true,
      noticeVersion: '2026-09-29.1',
    });
  });

  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  await assertSucceeds(getDoc(doc(alice, 'consentLedger/alice/events/event-1')));
  await assertFails(getDoc(doc(bob, 'consentLedger/alice/events/event-1')));
  await assertFails(setDoc(doc(alice, 'consentLedger/alice/events/forged'), {
    purpose: 'identity_verification',
    granted: true,
  }));
});

test('risk signals and reviewed risk assessments are server-only', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'riskSignals/alice'), { reportSignalCount: 2 });
    await setDoc(doc(ctx.firestore(), 'riskAssessments/alice'), { level: 'MEDIUM' });
  });

  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(getDoc(doc(alice, 'riskSignals/alice')));
  await assertFails(getDoc(doc(alice, 'riskAssessments/alice')));
  await assertFails(setDoc(doc(alice, 'riskAssessments/alice'), { level: 'LOW' }));
});
