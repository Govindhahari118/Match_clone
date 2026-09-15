import fs from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, setDoc } from 'firebase/firestore';

const projectId = 'matchapp-saved-search-rules-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => { await env?.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });

const valid = {
  name: 'Karnataka Telugu professionals', createdAt: Date.now(),
  ageMin: 25, ageMax: 34, city: '', state: 'Karnataka', caste: '', subCaste: '', minScore: 0,
  religion: 'Hindu', motherTongue: 'Telugu', maritalStatus: 'Never Married', verifiedOnly: true,
  incomeMin: '', incomeMax: '', educationLevel: 'Graduate', diet: '', residentialStatus: '',
  hasChildren: '', keyword: '', gothra: '', nativeState: '', countryOfResidence: '', nriOnly: false,
  willingToRelocate: false, recentlyJoinedDays: 0, smoking: '', drinking: '', familyType: '',
  familyStatus: '', physicalStatus: '', hasChildrenFilter: '', citizenship: '', nriStatus: '',
  educationField: '', occupationCategory: '', employerType: '', nakshatra: '', rasi: '', manglik: '',
  hobbies: '', withPhotoOnly: true, verifiedLevel: 0, premiumOnly: false, lastActiveWithinDays: 7,
  minPoruthamScore: 0, hasHoroscope: '',
};

test('owner can create read and delete a valid saved search', async () => {
  const db = env.authenticatedContext('alice').firestore();
  const ref = doc(db, 'savedSearches/alice/items/search1');
  await assertSucceeds(setDoc(ref, valid));
  await assertSucceeds(getDoc(ref));
  await assertSucceeds(deleteDoc(ref));
});

test('another member cannot read write or delete saved searches', async () => {
  await env.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), 'savedSearches/alice/items/search1'), valid);
  });
  const bob = env.authenticatedContext('bob').firestore();
  const ref = doc(bob, 'savedSearches/alice/items/search1');
  await assertFails(getDoc(ref));
  await assertFails(setDoc(ref, valid));
  await assertFails(deleteDoc(ref));
});

test('saved search rejects unexpected fields and malformed values', async () => {
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(db, 'savedSearches/alice/items/extra'), { ...valid, secret: 'x' }));
  await assertFails(setDoc(doc(db, 'savedSearches/alice/items/empty'), { ...valid, name: '' }));
  await assertFails(setDoc(doc(db, 'savedSearches/alice/items/age'), { ...valid, ageMin: 17 }));
  await assertFails(setDoc(doc(db, 'savedSearches/alice/items/keyword'), { ...valid, keyword: 'x'.repeat(65) }));
});
