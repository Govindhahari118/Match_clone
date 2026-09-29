import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { doc, setDoc } from 'firebase/firestore';
import { getBytes, ref, uploadBytes } from 'firebase/storage';

const projectId = 'matchapp-profile-media-visibility-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
    storage: { rules: fs.readFileSync(new URL('../storage.rules', import.meta.url), 'utf8') },
  });
});

after(async () => { await env?.cleanup(); });

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async ctx => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users/alice'), {
      firebaseUid: 'alice',
      accountStatus: 'ACTIVE',
      stealthMode: false,
      photoUrl: '',
      videoUrl: '',
    });
    await setDoc(doc(db, 'users/bob'), {
      firebaseUid: 'bob',
      accountStatus: 'ACTIVE',
      stealthMode: false,
      photoUrl: '',
      videoUrl: '',
    });
  });
});

const ownerPhotoMetadata = {
  contentType: 'image/jpeg',
  customMetadata: { ownerUid: 'alice' },
};

const ownerVideoMetadata = {
  contentType: 'video/mp4',
  customMetadata: { ownerUid: 'alice' },
};

test('owner can read pending photo but peer cannot read unpublished media', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  const bobStorage = env.authenticatedContext('bob').storage();
  const object = ref(aliceStorage, 'photos/alice/pending.jpg');

  await assertSucceeds(uploadBytes(object, new Uint8Array([1, 2, 3]), ownerPhotoMetadata));
  await assertSucceeds(getBytes(object));
  await assertFails(getBytes(ref(bobStorage, 'photos/alice/pending.jpg')));
});

test('member cannot self-assign trusted published metadata', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  await assertFails(uploadBytes(
    ref(aliceStorage, 'photos/alice/forged.jpg'),
    new Uint8Array([1, 2, 3]),
    {
      contentType: 'image/jpeg',
      customMetadata: { ownerUid: 'alice', published: 'true' },
    },
  ));
  await assertFails(uploadBytes(
    ref(aliceStorage, 'videos/alice/forged.mp4'),
    new Uint8Array([1, 2, 3]),
    {
      contentType: 'video/mp4',
      customMetadata: { ownerUid: 'alice', published: 'true' },
    },
  ));
});

test('peer may read only backend-published photo and video for a visible active profile', async () => {
  await env.withSecurityRulesDisabled(async ctx => {
    const storage = ctx.storage();
    await uploadBytes(
      ref(storage, 'photos/alice/approved.jpg'),
      new Uint8Array([4, 5, 6]),
      {
        contentType: 'image/jpeg',
        customMetadata: { ownerUid: 'alice', published: 'true' },
      },
    );
    await uploadBytes(
      ref(storage, 'videos/alice/approved.mp4'),
      new Uint8Array([7, 8, 9]),
      {
        contentType: 'video/mp4',
        customMetadata: { ownerUid: 'alice', published: 'true' },
      },
    );
  });

  const bobStorage = env.authenticatedContext('bob').storage();
  await assertSucceeds(getBytes(ref(bobStorage, 'photos/alice/approved.jpg')));
  await assertSucceeds(getBytes(ref(bobStorage, 'videos/alice/approved.mp4')));
});

test('block revokes access even to previously published profile media', async () => {
  await env.withSecurityRulesDisabled(async ctx => {
    await uploadBytes(
      ref(ctx.storage(), 'photos/alice/approved.jpg'),
      new Uint8Array([4, 5, 6]),
      {
        contentType: 'image/jpeg',
        customMetadata: { ownerUid: 'alice', published: 'true' },
      },
    );
    await setDoc(doc(ctx.firestore(), 'blocks/alice/blocked/bob'), {
      blockedUid: 'bob',
      blockedAt: Date.now(),
    });
  });

  const bobStorage = env.authenticatedContext('bob').storage();
  await assertFails(getBytes(ref(bobStorage, 'photos/alice/approved.jpg')));
});
