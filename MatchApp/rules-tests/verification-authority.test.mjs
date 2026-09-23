import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc } from 'firebase/firestore';
import { getBytes, ref, uploadBytes } from 'firebase/storage';

const projectId = 'matchapp-verification-authority-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
    storage: { rules: fs.readFileSync(new URL('../storage.rules', import.meta.url), 'utf8') },
  });
});

after(async () => env?.cleanup());
beforeEach(async () => env.clearFirestore());

test('client cannot forge pending verification request', async () => {
  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(alice, 'verificationRequests/alice'), {
    uid: 'alice',
    docType: 'Aadhaar',
    documentPath: 'verifications/alice/document',
    status: 'pending',
    submittedAt: Date.now(),
  }));
});

test('owner can read backend-created status but cannot alter it', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'verificationRequests/alice'), {
      uid: 'alice',
      docType: 'Aadhaar',
      documentPath: 'verifications/alice/document',
      status: 'pending',
      submittedAt: Date.now(),
    });
  });
  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  await assertSucceeds(getDoc(doc(alice, 'verificationRequests/alice')));
  await assertFails(getDoc(doc(bob, 'verificationRequests/alice')));
  await assertFails(updateDoc(doc(alice, 'verificationRequests/alice'), { status: 'verified' }));
});

test('verification artifact remains unreadable to all clients', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  const bobStorage = env.authenticatedContext('bob').storage();
  const object = ref(aliceStorage, 'verifications/alice/document');
  await assertSucceeds(uploadBytes(object, new Uint8Array([1, 2, 3]), {
    contentType: 'image/jpeg',
    customMetadata: { ownerUid: 'alice', docType: 'Aadhaar' },
  }));
  await assertFails(getBytes(object));
  await assertFails(getBytes(ref(bobStorage, 'verifications/alice/document')));
});

test('another member cannot upload into owner verification path', async () => {
  const bobStorage = env.authenticatedContext('bob').storage();
  await assertFails(uploadBytes(
    ref(bobStorage, 'verifications/alice/document'),
    new Uint8Array([9]),
    { contentType: 'image/jpeg' },
  ));
});


test('client cannot forge granular verification signals', async () => {
  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(alice, 'verifications/alice'), {
    phoneStatus: 'VERIFIED',
    identityStatus: 'VERIFIED',
  }));

  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'verifications/alice'), {
      phoneStatus: 'VERIFIED',
      identityStatus: 'NOT_STARTED',
    });
  });

  await assertSucceeds(getDoc(doc(alice, 'verifications/alice')));
  await assertFails(updateDoc(doc(alice, 'verifications/alice'), {
    identityStatus: 'VERIFIED',
  }));
});
