import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { doc, setDoc } from 'firebase/firestore';
import { getBytes, ref, uploadBytes } from 'firebase/storage';

// Storage rules that read Firestore must run under the same Firebase project as the emulators.
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
      firebaseUid: 'alice', displayName: 'Alice', age: 28, gender: 'FEMALE', lookingFor: 'MALE',
      isPremium: false, isVerified: false, verificationLevel: 0, subscriptionPlan: 'FREE',
      subscriptionExpiry: 0, stealthMode: false,
    });
    await setDoc(doc(db, 'users/bob'), {
      firebaseUid: 'bob', displayName: 'Bob', age: 30, gender: 'MALE', lookingFor: 'FEMALE',
      isPremium: false, isVerified: false, verificationLevel: 0, subscriptionPlan: 'FREE',
      subscriptionExpiry: 0, stealthMode: false,
    });
    await setDoc(doc(db, 'interests/alice_bob'), { fromUid: 'alice', toUid: 'bob', createdAt: Date.now() });
    await setDoc(doc(db, 'interests/bob_alice'), { fromUid: 'bob', toUid: 'alice', createdAt: Date.now() });
    await setDoc(doc(db, 'chats/thread123'), {
      participantUids: ['alice', 'bob'], lastMessage: '', lastSentAt: 0,
    });
  });
});

const imageMetadata = {
  contentType: 'image/jpeg',
  customMetadata: {
    senderUid: 'alice',
    recipientUid: 'bob',
    threadId: 'thread123',
    kind: 'image',
  },
};

test('chat participant can upload and both participants can read media', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  const bobStorage = env.authenticatedContext('bob').storage();
  const object = ref(aliceStorage, 'chat-media/thread123/message.jpg');
  await assertSucceeds(uploadBytes(object, new Uint8Array([1, 2, 3]), imageMetadata));
  await assertSucceeds(getBytes(object));
  await assertSucceeds(getBytes(ref(bobStorage, 'chat-media/thread123/message.jpg')));
});

test('outsider cannot upload into or read an existing chat thread', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  const malloryStorage = env.authenticatedContext('mallory').storage();
  await assertSucceeds(uploadBytes(
    ref(aliceStorage, 'chat-media/thread123/message.jpg'),
    new Uint8Array([1, 2, 3]),
    imageMetadata,
  ));
  await assertFails(getBytes(ref(malloryStorage, 'chat-media/thread123/message.jpg')));
  await assertFails(uploadBytes(
    ref(malloryStorage, 'chat-media/thread123/forged.jpg'),
    new Uint8Array([9, 9, 9]),
    {
      contentType: 'image/jpeg',
      customMetadata: {
        senderUid: 'mallory', recipientUid: 'bob', threadId: 'thread123', kind: 'image',
      },
    },
  ));
});

test('signed-in user cannot invent a non-existent chat thread using forged metadata', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  await assertFails(uploadBytes(
    ref(aliceStorage, 'chat-media/fake-thread/message.jpg'),
    new Uint8Array([4, 5, 6]),
    {
      contentType: 'image/jpeg',
      customMetadata: {
        senderUid: 'alice', recipientUid: 'bob', threadId: 'fake-thread', kind: 'image',
      },
    },
  ));
});

test('block immediately prevents further chat media access and writes', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  const bobStorage = env.authenticatedContext('bob').storage();
  const object = ref(aliceStorage, 'chat-media/thread123/message.jpg');
  await assertSucceeds(uploadBytes(object, new Uint8Array([7, 8, 9]), imageMetadata));

  // Blocking is a trusted callable in production. Seed its server-side result directly so this
  // test verifies the post-condition rather than bypassing callable authorization.
  await env.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), 'blocks/alice/blocked/bob'), {
      blockedUid: 'bob', blockedAt: Date.now(),
    });
  });

  await assertFails(getBytes(object));
  await assertFails(getBytes(ref(bobStorage, 'chat-media/thread123/message.jpg')));
  await assertFails(uploadBytes(
    ref(aliceStorage, 'chat-media/thread123/after-block.jpg'),
    new Uint8Array([1]),
    imageMetadata,
  ));
});
