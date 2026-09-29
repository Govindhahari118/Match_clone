import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, setDoc, updateDoc } from 'firebase/firestore';
import { getBytes, ref, uploadBytes } from 'firebase/storage';

const projectId = 'matchapp-rules-test';
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
  await env.withSecurityRulesDisabled(async context => {
    const db = context.firestore();
    await setDoc(doc(db, 'users/alice'), {
      firebaseUid: 'alice',
      displayName: 'Alice',
      accountStatus: 'ACTIVE',
      stealthMode: false,
      photoUrl: '',
      videoUrl: '',
      voiceBioUrl: '',
    });
    await setDoc(doc(db, 'users/bob'), {
      firebaseUid: 'bob',
      displayName: 'Bob',
      accountStatus: 'ACTIVE',
      stealthMode: false,
      photoUrl: '',
      videoUrl: '',
      voiceBioUrl: '',
    });
  });
});

test('owner can read pending profile media but peer cannot before publication', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  const bobStorage = env.authenticatedContext('bob').storage();

  await assertSucceeds(uploadBytes(
    ref(aliceStorage, 'photos/alice/pending.jpg'),
    new Uint8Array([1, 2, 3]),
    { contentType: 'image/jpeg', customMetadata: { ownerUid: 'alice' } },
  ));
  await assertSucceeds(getBytes(ref(aliceStorage, 'photos/alice/pending.jpg')));
  await assertFails(getBytes(ref(bobStorage, 'photos/alice/pending.jpg')));

  await assertSucceeds(uploadBytes(
    ref(aliceStorage, 'videos/alice/pending.mp4'),
    new Uint8Array([4, 5, 6]),
    { contentType: 'video/mp4', customMetadata: { ownerUid: 'alice' } },
  ));
  await assertSucceeds(getBytes(ref(aliceStorage, 'videos/alice/pending.mp4')));
  await assertFails(getBytes(ref(bobStorage, 'videos/alice/pending.mp4')));
});

test('peer can read only the exact published photo and video object', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  const bobStorage = env.authenticatedContext('bob').storage();

  await uploadBytes(
    ref(aliceStorage, 'photos/alice/approved.jpg'),
    new Uint8Array([1]),
    { contentType: 'image/jpeg', customMetadata: { ownerUid: 'alice' } },
  );
  await uploadBytes(
    ref(aliceStorage, 'photos/alice/other.jpg'),
    new Uint8Array([2]),
    { contentType: 'image/jpeg', customMetadata: { ownerUid: 'alice' } },
  );
  await uploadBytes(
    ref(aliceStorage, 'videos/alice/approved.mp4'),
    new Uint8Array([3]),
    { contentType: 'video/mp4', customMetadata: { ownerUid: 'alice' } },
  );
  await uploadBytes(
    ref(aliceStorage, 'videos/alice/other.mp4'),
    new Uint8Array([4]),
    { contentType: 'video/mp4', customMetadata: { ownerUid: 'alice' } },
  );

  await env.withSecurityRulesDisabled(async context => {
    await updateDoc(doc(context.firestore(), 'users/alice'), {
      photoUrl: 'photos/alice/approved.jpg',
      videoUrl: 'videos/alice/approved.mp4',
    });
  });

  await assertSucceeds(getBytes(ref(bobStorage, 'photos/alice/approved.jpg')));
  await assertFails(getBytes(ref(bobStorage, 'photos/alice/other.jpg')));
  await assertSucceeds(getBytes(ref(bobStorage, 'videos/alice/approved.mp4')));
  await assertFails(getBytes(ref(bobStorage, 'videos/alice/other.mp4')));
});

test('block revokes access to previously published profile media', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  const bobStorage = env.authenticatedContext('bob').storage();

  await uploadBytes(
    ref(aliceStorage, 'photos/alice/approved.jpg'),
    new Uint8Array([1, 2, 3]),
    { contentType: 'image/jpeg', customMetadata: { ownerUid: 'alice' } },
  );
  await env.withSecurityRulesDisabled(async context => {
    const db = context.firestore();
    await updateDoc(doc(db, 'users/alice'), {
      photoUrl: 'photos/alice/approved.jpg',
    });
  });
  await assertSucceeds(getBytes(ref(bobStorage, 'photos/alice/approved.jpg')));

  await env.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), 'blocks/alice/blocked/bob'), {
      blockedUid: 'bob',
      blockedAt: Date.now(),
    });
  });
  await assertFails(getBytes(ref(bobStorage, 'photos/alice/approved.jpg')));
});

test('voice bio peer access is also limited to the published object', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  const bobStorage = env.authenticatedContext('bob').storage();

  await uploadBytes(
    ref(aliceStorage, 'voicebios/alice/current.m4a'),
    new Uint8Array([7]),
    { contentType: 'audio/mp4' },
  );
  await uploadBytes(
    ref(aliceStorage, 'voicebios/alice/draft.m4a'),
    new Uint8Array([8]),
    { contentType: 'audio/mp4' },
  );
  await env.withSecurityRulesDisabled(async context => {
    await updateDoc(doc(context.firestore(), 'users/alice'), {
      voiceBioUrl: 'voicebios/alice/current.m4a',
    });
  });

  await assertSucceeds(getBytes(ref(bobStorage, 'voicebios/alice/current.m4a')));
  await assertFails(getBytes(ref(bobStorage, 'voicebios/alice/draft.m4a')));
});


test('profile media upload MIME types are narrow', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();

  await assertFails(uploadBytes(
    ref(aliceStorage, 'photos/alice/not-allowed.png'),
    new Uint8Array([1]),
    { contentType: 'image/png', customMetadata: { ownerUid: 'alice' } },
  ));

  await assertFails(uploadBytes(
    ref(aliceStorage, 'videos/alice/not-allowed.webm'),
    new Uint8Array([1]),
    { contentType: 'video/webm', customMetadata: { ownerUid: 'alice' } },
  ));
});
