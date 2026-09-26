import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  deleteDoc,
  doc,
  getDoc,
  setDoc,
  updateDoc,
} from 'firebase/firestore';
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

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'users/alice'), {
      firebaseUid: 'alice', displayName: 'Alice', age: 28, gender: 'FEMALE', lookingFor: 'MALE',
      city: 'Hyderabad', religion: 'Hindu', isPremium: false, isVerified: false,
      verificationLevel: 0, subscriptionPlan: 'FREE', subscriptionExpiry: 0, stealthMode: false,
    });
    await setDoc(doc(db, 'users/bob'), {
      firebaseUid: 'bob', displayName: 'Bob', age: 30, gender: 'MALE', lookingFor: 'FEMALE',
      city: 'Hyderabad', religion: 'Hindu', isPremium: false, isVerified: false,
      verificationLevel: 0, subscriptionPlan: 'FREE', subscriptionExpiry: 0, stealthMode: false,
    });
    await setDoc(doc(db, 'userPrivate/alice'), { phoneNumber: '9999999999', email: 'alice@example.test' });
  });
});

async function seedInterest(fromUid, toUid) {
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), `interests/${fromUid}_${toUid}`), {
      fromUid, toUid, isSuperLike: false, createdAt: new Date(),
    });
  });
}

test('unauthenticated users cannot read profiles', async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, 'users/alice')));
});

test('stealth profiles are owner-readable but hidden from unrelated clients', async () => {
  await env.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), 'users/bob'), { stealthMode: true });
  });
  const bobDb = env.authenticatedContext('bob').firestore();
  const aliceDb = env.authenticatedContext('alice').firestore();
  await assertSucceeds(getDoc(doc(bobDb, 'users/bob')));
  await assertFails(getDoc(doc(aliceDb, 'users/bob')));
});

test('request recipient can inspect stealth sender profile before accepting', async () => {
  await env.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), 'users/alice'), { stealthMode: true });
  });
  const bobDb = env.authenticatedContext('bob').firestore();

  await assertFails(getDoc(doc(bobDb, 'users/alice')));
  await seedInterest('alice', 'bob');
  await assertSucceeds(getDoc(doc(bobDb, 'users/alice')));
});

test('private account data is owner-only', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  const bobDb = env.authenticatedContext('bob').firestore();
  await assertSucceeds(getDoc(doc(aliceDb, 'userPrivate/alice')));
  await assertFails(getDoc(doc(bobDb, 'userPrivate/alice')));
});

test('exact nearby coordinates are server-only', async () => {
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'userLocations/alice'), {
      latitude: 17.4, longitude: 78.4, geohash: 'te', updatedAtMillis: Date.now(),
    });
  });
  const aliceDb = env.authenticatedContext('alice').firestore();
  const bobDb = env.authenticatedContext('bob').firestore();
  await assertFails(getDoc(doc(aliceDb, 'userLocations/alice')));
  await assertFails(getDoc(doc(bobDb, 'userLocations/alice')));
  await assertFails(setDoc(doc(aliceDb, 'userLocations/alice'), {
    latitude: 17.4, longitude: 78.4, geohash: 'te', updatedAtMillis: Date.now(),
  }));
});

test('clients cannot grant themselves premium or verification', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(updateDoc(doc(db, 'users/alice'), { isPremium: true }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { isVerified: true }));
  await assertFails(updateDoc(doc(db, 'users/alice'), { verificationLevel: 5 }));
});

test('profile deletion must go through the server account-erasure workflow', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(deleteDoc(doc(db, 'users/alice')));
  await assertSucceeds(getDoc(doc(db, 'users/alice')));
});

test('clients cannot create or delete interest and match authority documents directly', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(aliceDb, 'interests/alice_bob'), {
    fromUid: 'alice', toUid: 'bob', isSuperLike: false, createdAt: new Date(),
  }));
  await seedInterest('alice', 'bob');
  await assertFails(deleteDoc(doc(aliceDb, 'interests/alice_bob')));
  await assertFails(setDoc(doc(aliceDb, 'matches/alice_bob'), {
    users: ['alice', 'bob'], createdAt: new Date(), lastActivity: new Date(),
  }));
});

test('block documents are private and server-authoritative', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  const bobDb = env.authenticatedContext('bob').firestore();
  await assertFails(setDoc(doc(aliceDb, 'blocks/alice/blocked/bob'), {
    blockedUid: 'bob', blockedAt: Date.now(),
  }));
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'blocks/alice/blocked/bob'), {
      blockedUid: 'bob', blockedAt: Date.now(),
    });
  });
  await assertSucceeds(getDoc(doc(aliceDb, 'blocks/alice/blocked/bob')));
  await assertFails(getDoc(doc(bobDb, 'blocks/alice/blocked/bob')));
});

test('profile hiding denies that member profile read', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  const bobDb = env.authenticatedContext('bob').firestore();
  await assertSucceeds(setDoc(doc(aliceDb, 'privacyRelations/alice/members/bob'), {
    memberUid: 'bob', profileHidden: true, updatedAt: new Date(),
  }));
  await assertFails(getDoc(doc(bobDb, 'users/alice')));
  await assertSucceeds(getDoc(doc(aliceDb, 'privacyRelations/alice/members/bob')));
  await assertFails(getDoc(doc(bobDb, 'privacyRelations/alice/members/bob')));
});

test('contact-only exception does not hide the profile', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  const bobDb = env.authenticatedContext('bob').firestore();
  await assertSucceeds(setDoc(doc(aliceDb, 'privacyRelations/alice/members/bob'), {
    memberUid: 'bob', contactHidden: true, updatedAt: new Date(),
  }));
  await assertSucceeds(getDoc(doc(bobDb, 'users/alice')));
});

test('only owner can configure global contact visibility', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  const bobDb = env.authenticatedContext('bob').firestore();
  await assertSucceeds(setDoc(doc(aliceDb, 'privacySettings/alice'), {
    contactVisibility: 'nobody', updatedAt: new Date(),
  }));
  await assertFails(setDoc(doc(bobDb, 'privacySettings/alice'), {
    contactVisibility: 'mutual_matches', updatedAt: new Date(),
  }));
  await assertFails(getDoc(doc(bobDb, 'privacySettings/alice')));
});

test('chat thread requires server-created mutual interests and stops after a block', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  const bobDb = env.authenticatedContext('bob').firestore();
  const thread = doc(aliceDb, 'chats/alice_bob');

  await seedInterest('alice', 'bob');
  await assertFails(setDoc(thread, { participantUids: ['alice', 'bob'], lastMessage: '', lastSentAt: 0 }));

  await seedInterest('bob', 'alice');
  await assertSucceeds(setDoc(thread, { participantUids: ['alice', 'bob'], lastMessage: '', lastSentAt: 0 }));
  await assertSucceeds(setDoc(doc(aliceDb, 'chats/alice_bob/messages/m1'), {
    body: 'hello', sentAt: Date.now(), isRead: false,
    fromFirebaseUid: 'alice', toFirebaseUid: 'bob',
    voiceUri: null, imageUri: null, voiceDurationMs: null,
  }));

  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'blocks/bob/blocked/alice'), {
      blockedUid: 'alice', blockedAt: Date.now(),
    });
  });
  await assertFails(setDoc(doc(aliceDb, 'chats/alice_bob/messages/m2'), {
    body: 'blocked', sentAt: Date.now(), isRead: false,
    fromFirebaseUid: 'alice', toFirebaseUid: 'bob',
    voiceUri: null, imageUri: null, voiceDurationMs: null,
  }));
});

test('users cannot write payment authority documents', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(db, 'paymentOrders/order_fake'), { uid: 'alice', status: 'paid' }));
  await assertFails(setDoc(doc(db, 'payments/pay_fake'), { uid: 'alice', amount: 1 }));
});

test('clients cannot forge profile views', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(db, 'profileViews/fake'), {
    viewerUid: 'alice', viewedUid: 'bob', viewedAt: Date.now(),
  }));
});

test('profile view records are visible only to viewer and viewed user', async () => {
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'profileViews/server_written'), {
      viewerUid: 'alice', viewedUid: 'bob', viewedAt: Date.now(),
    });
  });
  const aliceDb = env.authenticatedContext('alice').firestore();
  const bobDb = env.authenticatedContext('bob').firestore();
  const malloryDb = env.authenticatedContext('mallory').firestore();
  await assertSucceeds(getDoc(doc(aliceDb, 'profileViews/server_written')));
  await assertSucceeds(getDoc(doc(bobDb, 'profileViews/server_written')));
  await assertFails(getDoc(doc(malloryDb, 'profileViews/server_written')));
});

test('profile photo upload is owner-only', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  const bobStorage = env.authenticatedContext('bob').storage();
  const bytes = new Uint8Array([1, 2, 3, 4]);
  await assertSucceeds(uploadBytes(ref(aliceStorage, 'photos/alice/profile.jpg'), bytes, { contentType: 'image/jpeg' }));
  await assertFails(uploadBytes(ref(bobStorage, 'photos/alice/attack.jpg'), bytes, { contentType: 'image/jpeg' }));
});

test('hidden member cannot read profile photo while owner still can', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  const aliceStorage = env.authenticatedContext('alice').storage();
  const bobStorage = env.authenticatedContext('bob').storage();
  const object = ref(aliceStorage, 'photos/alice/private-profile.jpg');
  const bytes = new Uint8Array([8, 6, 7, 5, 3, 0, 9]);

  await assertSucceeds(uploadBytes(object, bytes, { contentType: 'image/jpeg' }));
  await assertSucceeds(getBytes(object));
  await assertSucceeds(getBytes(ref(bobStorage, 'photos/alice/private-profile.jpg')));

  await assertSucceeds(setDoc(doc(aliceDb, 'privacyRelations/alice/members/bob'), {
    memberUid: 'bob', profileHidden: true, updatedAt: new Date(),
  }));

  await assertFails(getBytes(ref(bobStorage, 'photos/alice/private-profile.jpg')));
  await assertSucceeds(getBytes(object));
});

test('blocked member cannot read profile media', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  const aliceStorage = env.authenticatedContext('alice').storage();
  const bobStorage = env.authenticatedContext('bob').storage();
  const object = ref(aliceStorage, 'photos/alice/block-test.jpg');
  const bytes = new Uint8Array([4, 2, 4, 2]);

  await assertSucceeds(uploadBytes(object, bytes, { contentType: 'image/jpeg' }));
  await assertSucceeds(getBytes(ref(bobStorage, 'photos/alice/block-test.jpg')));
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'blocks/alice/blocked/bob'), {
      blockedUid: 'bob', blockedAt: Date.now(),
    });
  });
  await assertFails(getBytes(ref(bobStorage, 'photos/alice/block-test.jpg')));
});

test('stealth profile media is private except to an explicit interest recipient', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  const bobStorage = env.authenticatedContext('bob').storage();
  const object = ref(aliceStorage, 'photos/alice/stealth-test.jpg');
  const bytes = new Uint8Array([1, 9, 9, 9]);

  await assertSucceeds(uploadBytes(object, bytes, { contentType: 'image/jpeg' }));
  await env.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), 'users/alice'), { stealthMode: true });
  });
  await assertFails(getBytes(ref(bobStorage, 'photos/alice/stealth-test.jpg')));

  await seedInterest('alice', 'bob');
  await assertSucceeds(getBytes(ref(bobStorage, 'photos/alice/stealth-test.jpg')));
});

test('verification documents cannot be read by another client', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  const bobStorage = env.authenticatedContext('bob').storage();
  const bytes = new Uint8Array([1, 2, 3]);
  await assertSucceeds(uploadBytes(ref(aliceStorage, 'verifications/alice/id.jpg'), bytes, { contentType: 'image/jpeg' }));
  await assertFails(getBytes(ref(bobStorage, 'verifications/alice/id.jpg')));
});


test('notifications are server-created, recipient-only, and clients may only mark read', async () => {
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'notifications/n1'), {
      userId: 'alice',
      type: 'INTEREST',
      title: 'New interest',
      body: 'Open the app to view it.',
      entityType: 'profile',
      entityId: 'bob',
      deepLink: 'matrimonyconnect://match?uid=bob',
      fromFirebaseUid: 'bob',
      createdAt: new Date(),
      readAt: null,
    });
  });

  const aliceDb = env.authenticatedContext('alice').firestore();
  const bobDb = env.authenticatedContext('bob').firestore();
  const n = doc(aliceDb, 'notifications/n1');

  await assertSucceeds(getDoc(n));
  await assertFails(getDoc(doc(bobDb, 'notifications/n1')));
  await assertFails(setDoc(doc(aliceDb, 'notifications/forged'), {
    userId: 'alice', type: 'MATCH', title: 'Fake', body: 'Fake', createdAt: new Date(), readAt: null,
  }));
  await assertSucceeds(updateDoc(n, { readAt: new Date() }));
  await assertFails(updateDoc(n, { title: 'Tampered' }));
});


test('notification preferences are owner-only and schema constrained', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  const bobDb = env.authenticatedContext('bob').firestore();
  const prefs = doc(aliceDb, 'notificationPrefs/alice');

  await assertSucceeds(setDoc(prefs, {
    interests: true,
    matches: false,
    messages: true,
    system: true,
  }));
  await assertSucceeds(getDoc(prefs));
  await assertFails(getDoc(doc(bobDb, 'notificationPrefs/alice')));
  await assertFails(updateDoc(prefs, { arbitraryField: true }));
  await assertFails(updateDoc(prefs, { interests: 'yes' }));
});


test('FCM device registry is server-only and legacy client token writes are rejected', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  const bobDb = env.authenticatedContext('bob').firestore();

  await assertFails(updateDoc(doc(aliceDb, 'userPrivate/alice'), {
    fcmToken: 'client-must-not-write-a-token-value',
  }));

  const devicePath = 'fcmTokens/alice/devices/device_1234567890abcdef';
  await assertFails(setDoc(doc(aliceDb, devicePath), {
    uid: 'alice',
    deviceId: 'device_1234567890abcdef',
    token: 'forged-client-token-that-is-long-enough',
  }));
  await assertFails(getDoc(doc(aliceDb, devicePath)));
  await assertFails(getDoc(doc(bobDb, devicePath)));

  await assertFails(setDoc(doc(aliceDb, 'fcmDeviceOwners/device_1234567890abcdef'), {
    uid: 'alice',
  }));
});


test('operations audit log is never client readable or writable', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  const audit = doc(aliceDb, 'adminAudit/fake-client-audit');

  await assertFails(getDoc(audit));
  await assertFails(setDoc(audit, {
    actorUid: 'alice',
    actorRole: 'ops_admin',
    action: 'verification.review',
    targetUid: 'bob',
    createdAt: new Date(),
  }));
});


test('appearance preferences are private, account-scoped, and presentation-only', async () => {
  const aliceDb = env.authenticatedContext('alice').firestore();
  const bobDb = env.authenticatedContext('bob').firestore();
  const prefs = doc(aliceDb, 'appearancePrefs/alice');

  await assertSucceeds(setDoc(prefs, {
    themePreference: 'AUTOMATIC',
    manualThemeKey: 'HINDU',
    updatedAt: new Date(),
  }));
  await assertSucceeds(getDoc(prefs));
  await assertFails(getDoc(doc(bobDb, 'appearancePrefs/alice')));
  await assertFails(setDoc(doc(bobDb, 'appearancePrefs/alice'), {
    themePreference: 'MANUAL',
    manualThemeKey: 'MUSLIM',
    updatedAt: new Date(),
  }));
  await assertFails(updateDoc(prefs, { religion: 'Muslim' }));
  await assertFails(updateDoc(prefs, { manualThemeKey: 'UNSAFE_UNKNOWN_THEME' }));
  await assertFails(updateDoc(prefs, { themePreference: 'RELIGION_OVERRIDE' }));
});
