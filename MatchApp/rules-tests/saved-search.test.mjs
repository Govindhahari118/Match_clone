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

async function seedAliceSearch() {
  await env.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), 'savedSearches/alice/items/search1'), valid);
    await setDoc(doc(context.firestore(), 'savedSearches/alice'), { count: 1, updatedAt: Date.now() });
  });
}

test('owner can read a server-created saved search', async () => {
  await seedAliceSearch();
  const db = env.authenticatedContext('alice').firestore();
  await assertSucceeds(getDoc(doc(db, 'savedSearches/alice/items/search1')));
  await assertSucceeds(getDoc(doc(db, 'savedSearches/alice')));
});

test('clients cannot directly create update or delete saved searches, including the owner', async () => {
  await seedAliceSearch();
  const db = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(db, 'savedSearches/alice/items/new-search'), valid));
  await assertFails(setDoc(doc(db, 'savedSearches/alice/items/search1'), { ...valid, name: 'Changed' }));
  await assertFails(deleteDoc(doc(db, 'savedSearches/alice/items/search1')));
  await assertFails(setDoc(doc(db, 'savedSearches/alice'), { count: 999 }));
});

test('another member cannot read or mutate saved searches', async () => {
  await seedAliceSearch();
  const bob = env.authenticatedContext('bob').firestore();
  const ref = doc(bob, 'savedSearches/alice/items/search1');
  await assertFails(getDoc(ref));
  await assertFails(setDoc(ref, valid));
  await assertFails(deleteDoc(ref));
  await assertFails(getDoc(doc(bob, 'savedSearches/alice')));
});
