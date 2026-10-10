import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  doc,
  deleteField,
  getDoc,
  setDoc,
  updateDoc,
} from 'firebase/firestore';

const projectId = 'matchapp-profile-privacy-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
});

function baseProfile(overrides = {}) {
  return {
    firebaseUid: 'alice',
    displayName: 'Alice',
    age: 28,
    gender: 'FEMALE',
    lookingFor: 'MALE',
    city: 'Hyderabad',
    religion: 'Hindu',
    isPremium: false,
    isVerified: false,
    verificationLevel: 0,
    stealthMode: false,
    ...overrides,
  };
}

test('profile create rejects billing metadata even when values look free', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(db, 'users/alice'), baseProfile({
    subscriptionPlan: 'FREE',
  })));
  await assertFails(setDoc(doc(db, 'users/alice'), baseProfile({
    subscriptionExpiry: 0,
  })));
  await assertFails(setDoc(doc(db, 'users/alice'), baseProfile({
    paymentId: 'play_fake',
  })));
});

for (const field of ['email', 'phoneNumber', 'dateOfBirth', 'rasi', 'nakshatra', 'manglik', 'birthTime', 'birthPlace', 'incomeBand']) {
  test(`public profile rejects private field ${field}`, async () => {
    const db = env.authenticatedContext('alice').firestore();
    await assertFails(setDoc(doc(db, 'users/alice'), baseProfile({ [field]: 'private-value' })));
  });
}

test('owner may store private matrimonial inputs in userPrivate', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertSucceeds(setDoc(doc(db, 'userPrivate/alice'), {
    email: 'alice@example.test',
    phoneNumber: '+919999999999',
    dateOfBirth: '1998-01-01',
    rasi: 'Cancer',
    nakshatra: 'Pushya',
    manglik: 'No',
    birthTime: '10:10',
    birthPlace: 'Hyderabad',
    incomeBand: '10-20 LPA',
    updatedAt: Date.now(),
  }));
});

test('another member cannot read userPrivate', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'userPrivate/alice'), { phoneNumber: '+919999999999' });
  });
  const bob = env.authenticatedContext('bob').firestore();
  await assertFails(getDoc(doc(bob, 'userPrivate/alice')));
});

test('owner may set valid online and last-active visibility', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertSucceeds(setDoc(doc(db, 'privacySettings/alice'), {
    contactVisibility: 'mutual_matches',
    onlineVisibility: 'everyone',
    lastActiveVisibility: 'mutual',
    updatedAt: Date.now(),
  }));
  await assertSucceeds(updateDoc(doc(db, 'privacySettings/alice'), {
    onlineVisibility: 'nobody',
    lastActiveVisibility: 'interests',
  }));
});

test('invalid activity visibility is rejected', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(db, 'privacySettings/alice'), {
    onlineVisibility: 'public_forever',
    lastActiveVisibility: 'mutual',
    updatedAt: Date.now(),
  }));
});

test('client cannot read or write server-only presence', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'presencePrivate/alice'), { lastActiveAt: Date.now() });
  });
  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(getDoc(doc(alice, 'presencePrivate/alice')));
  await assertFails(setDoc(doc(alice, 'presencePrivate/alice'), { lastActiveAt: Date.now() }));
});


test('photo visibility is server-controlled while owner can still update other privacy settings', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'privacySettings/alice'), {
      contactVisibility: 'mutual_matches',
      onlineVisibility: 'mutual',
      lastActiveVisibility: 'mutual',
      photoVisibility: 'ACCEPTED_ONLY',
      updatedAt: Date.now(),
    });
  });
  const alice = env.authenticatedContext('alice').firestore();
  await assertSucceeds(updateDoc(doc(alice, 'privacySettings/alice'), {
    onlineVisibility: 'nobody',
  }));
  await assertFails(updateDoc(doc(alice, 'privacySettings/alice'), {
    photoVisibility: 'PUBLIC',
  }));
});

test('photo grants and requests are readable only by participants and never client-writable', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users/alice'), { accountStatus: 'ACTIVE' });
    await setDoc(doc(db, 'users/bob'), { accountStatus: 'ACTIVE' });
    await setDoc(doc(db, 'photoGrants/alice/viewers/bob'), {
      viewerUid: 'bob',
      grantedAt: Date.now(),
      updatedAt: Date.now(),
      source: 'photo_request',
    });
    await setDoc(doc(db, 'photoAccessRequests/bob_alice'), {
      requesterUid: 'bob',
      targetUid: 'alice',
      status: 'PENDING',
      createdAt: Date.now(),
      updatedAt: Date.now(),
    });
  });

  const alice = env.authenticatedContext('alice').firestore();
  const bob = env.authenticatedContext('bob').firestore();
  const charlie = env.authenticatedContext('charlie').firestore();

  await assertSucceeds(getDoc(doc(alice, 'photoGrants/alice/viewers/bob')));
  await assertFails(getDoc(doc(bob, 'photoGrants/alice/viewers/bob')));
  await assertFails(setDoc(doc(alice, 'photoGrants/alice/viewers/charlie'), {
    viewerUid: 'charlie',
  }));

  await assertSucceeds(getDoc(doc(alice, 'photoAccessRequests/bob_alice')));
  await assertSucceeds(getDoc(doc(bob, 'photoAccessRequests/bob_alice')));
  await assertFails(getDoc(doc(charlie, 'photoAccessRequests/bob_alice')));
  await assertFails(updateDoc(doc(alice, 'photoAccessRequests/bob_alice'), { status: 'APPROVED' }));
});


test('owner cannot delete privacySettings and reset protected photo visibility', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'privacySettings/alice'), {
      contactVisibility: 'mutual_matches',
      onlineVisibility: 'mutual',
      lastActiveVisibility: 'mutual',
      photoVisibility: 'HIDDEN',
      updatedAt: Date.now(),
    });
  });
  const alice = env.authenticatedContext('alice').firestore();
  const { deleteDoc } = await import('firebase/firestore');
  await assertFails(deleteDoc(doc(alice, 'privacySettings/alice')));
});

test('clients cannot remove server photo visibility while editing other preferences', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users/alice'), { accountStatus: 'ACTIVE' });
    await setDoc(doc(ctx.firestore(), 'privacySettings/alice'), { photoVisibility: 'HIDDEN' });
  });
  const ref = doc(env.authenticatedContext('alice').firestore(), 'privacySettings/alice');
  await assertFails(updateDoc(ref, { photoVisibility: deleteField() }));
  await assertFails(setDoc(ref, { contactVisibility: 'nobody' }));
  await assertSucceeds(updateDoc(ref, { contactVisibility: 'nobody' }));
});
