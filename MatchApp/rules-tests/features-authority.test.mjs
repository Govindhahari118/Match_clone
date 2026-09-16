import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc } from 'firebase/firestore';

const projectId = 'matchapp-feature-authority-test';
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
    await setDoc(doc(db, 'events/event1'), { title: 'Meet', dateMillis: Date.now() + 86_400_000, attendees: 0 });
    await setDoc(doc(db, 'communities/community1'), { name: 'Community', memberCount: 0 });
    await setDoc(doc(db, 'eventRegistrations/alice_event1'), { uid: 'alice', eventId: 'event1' });
    await setDoc(doc(db, 'counsellingBookings/booking1'), { uid: 'alice', status: 'confirmed' });
    await setDoc(doc(db, 'referrals/ref1'), { referrerUid: 'alice', referredEmail: 'friend@example.test', status: 'pending' });
    await setDoc(doc(db, 'rmRequests/rm1'), { uid: 'alice', status: 'pending' });
    await setDoc(doc(db, 'backgroundChecks/bg1'), { requestedBy: 'alice', targetUid: 'bob', status: 'submitted' });
    await setDoc(doc(db, 'callRequests/call1'), { fromUid: 'alice', toUid: 'bob', status: 'requested', type: 'voice' });
    await setDoc(doc(db, 'communities/community1/members/alice'), { uid: 'alice' });
  });
});

test('feature documents remain readable to their owner where required', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertSucceeds(getDoc(doc(db, 'eventRegistrations/alice_event1')));
  await assertSucceeds(getDoc(doc(db, 'counsellingBookings/booking1')));
  await assertSucceeds(getDoc(doc(db, 'referrals/ref1')));
  await assertSucceeds(getDoc(doc(db, 'rmRequests/rm1')));
  await assertSucceeds(getDoc(doc(db, 'backgroundChecks/bg1')));
  await assertSucceeds(getDoc(doc(db, 'callRequests/call1')));
  await assertSucceeds(getDoc(doc(db, 'communities/community1')));
});

test('clients cannot forge server-managed feature mutations', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(db, 'eventRegistrations/fake'), { uid: 'alice', eventId: 'event1' }));
  await assertFails(setDoc(doc(db, 'counsellingBookings/fake'), { uid: 'alice', status: 'confirmed' }));
  await assertFails(setDoc(doc(db, 'referrals/fake'), { referrerUid: 'alice', status: 'joined' }));
  await assertFails(setDoc(doc(db, 'rmRequests/fake'), { uid: 'alice', status: 'approved', plan: 'PLATINUM' }));
  await assertFails(setDoc(doc(db, 'backgroundChecks/fake'), { requestedBy: 'alice', targetUid: 'bob', status: 'approved' }));
  await assertFails(setDoc(doc(db, 'callRequests/fake'), { fromUid: 'alice', toUid: 'bob', status: 'accepted', type: 'video' }));
  await assertFails(setDoc(doc(db, 'communities/community1/members/fake'), { uid: 'alice' }));
});

test('clients cannot mutate counters or server-owned feature status after server creation', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(updateDoc(doc(db, 'events/event1'), { attendees: 9999 }));
  await assertFails(updateDoc(doc(db, 'communities/community1'), { memberCount: 9999 }));
  await assertFails(updateDoc(doc(db, 'callRequests/call1'), { status: 'accepted' }));
  await assertFails(updateDoc(doc(db, 'backgroundChecks/bg1'), { status: 'approved' }));
});

test('another user cannot read owner-only service requests', async () => {
  const db = env.authenticatedContext('bob').firestore();
  await assertFails(getDoc(doc(db, 'counsellingBookings/booking1')));
  await assertFails(getDoc(doc(db, 'referrals/ref1')));
  await assertFails(getDoc(doc(db, 'rmRequests/rm1')));
  await assertFails(getDoc(doc(db, 'backgroundChecks/bg1')));
  // Bob is a call participant, so that relationship document is intentionally visible to him.
  await assertSucceeds(getDoc(doc(db, 'callRequests/call1')));
});
