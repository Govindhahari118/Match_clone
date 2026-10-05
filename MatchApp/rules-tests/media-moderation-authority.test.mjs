import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc } from 'firebase/firestore';

const projectId = 'matchapp-media-authority-test';
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

test('member cannot publish an unmoderated photoUrl', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users/alice'), {
      firebaseUid: 'alice',
      displayName: 'Alice',
      accountStatus: 'ACTIVE',
      profileRevision: 0,
      photoUrl: '',
    });
  });

  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(updateDoc(doc(alice, 'users/alice'), {
    photoUrl: 'photos/alice/unreviewed.jpg',
    profileRevision: 1,
  }));
});

test('photo moderation, photo request and account enforcement records are server-only', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'photoModeration/case-1'), {
      uid: 'alice',
      status: 'PENDING',
    });
    await setDoc(doc(ctx.firestore(), 'photoRequests/alice_bob'), {
      requesterUid: 'alice',
      targetUid: 'bob',
      status: 'PENDING',
      requestSequence: 1,
      requestedAtMillis: 1,
    });
    await setDoc(doc(ctx.firestore(), 'accountEnforcements/alice'), {
      targetUid: 'alice',
      status: 'RESTRICTED',
    });
  });

  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(getDoc(doc(alice, 'photoModeration/case-1')));
  await assertFails(getDoc(doc(alice, 'photoRequests/alice_bob')));
  await assertFails(setDoc(doc(alice, 'photoRequests/alice_bob'), {
    requesterUid: 'alice',
    targetUid: 'bob',
    status: 'PENDING',
    requestSequence: 99,
    requestedAtMillis: Date.now(),
  }));
  await assertFails(getDoc(doc(alice, 'accountEnforcements/alice')));
  await assertFails(setDoc(doc(alice, 'photoModeration/forged'), {
    uid: 'alice',
    status: 'APPROVED',
  }));
});


test('member cannot publish an unmoderated videoUrl', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users/alice'), {
      firebaseUid: 'alice',
      displayName: 'Alice',
      accountStatus: 'ACTIVE',
      profileRevision: 0,
      photoUrl: '',
      videoUrl: '',
    });
  });

  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(updateDoc(doc(alice, 'users/alice'), {
    videoUrl: 'videos/alice/unreviewed.mp4',
    profileRevision: 1,
  }));
});

test('video moderation records are server-only', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'videoModeration/case-1'), {
      uid: 'alice',
      storagePath: 'videos/alice/pending.mp4',
      status: 'PENDING',
    });
  });

  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(getDoc(doc(alice, 'videoModeration/case-1')));
  await assertFails(setDoc(doc(alice, 'videoModeration/forged'), {
    uid: 'alice',
    storagePath: 'videos/alice/forged.mp4',
    status: 'APPROVED',
  }));
});
