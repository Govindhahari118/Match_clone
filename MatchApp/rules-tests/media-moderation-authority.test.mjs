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

test('member cannot publish unmoderated profile media pointers', async () => {
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
  await assertFails(updateDoc(doc(alice, 'users/alice'), {
    videoUrl: 'videos/alice/unreviewed.mp4',
    profileRevision: 1,
  }));
});

test('photo moderation and account enforcement records are server-only', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'photoModeration/case-1'), {
      uid: 'alice',
      status: 'PENDING',
    });
    await setDoc(doc(ctx.firestore(), 'accountEnforcements/alice'), {
      targetUid: 'alice',
      status: 'RESTRICTED',
    });
  });

  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(getDoc(doc(alice, 'photoModeration/case-1')));
  await assertFails(getDoc(doc(alice, 'accountEnforcements/alice')));
  await assertFails(setDoc(doc(alice, 'photoModeration/forged'), {
    uid: 'alice',
    status: 'APPROVED',
  }));
});


test('video moderation and profile video state are server-only', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'videoModeration/case-1'), {
      uid: 'alice',
      status: 'PENDING',
    });
    await setDoc(doc(ctx.firestore(), 'profileVideoState/alice'), {
      uid: 'alice',
      status: 'PENDING',
      storagePath: 'videos/alice/pending.mp4',
    });
  });

  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(getDoc(doc(alice, 'videoModeration/case-1')));
  await assertFails(getDoc(doc(alice, 'profileVideoState/alice')));
  await assertFails(setDoc(doc(alice, 'videoModeration/forged'), {
    uid: 'alice',
    status: 'APPROVED',
  }));
  await assertFails(setDoc(doc(alice, 'profileVideoState/alice'), {
    uid: 'alice',
    status: 'PUBLISHED',
  }));
});
