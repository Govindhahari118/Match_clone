import fs from 'node:fs';
import { after, before, test } from 'node:test';
import {
  assertFails,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc } from 'firebase/firestore';
import { getBytes, ref, uploadBytes } from 'firebase/storage';

const projectId = 'matchapp-data-export-authority-test';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
    storage: { rules: fs.readFileSync(new URL('../storage.rules', import.meta.url), 'utf8') },
  });
});

after(async () => env?.cleanup());

test('data export metadata is server-only', async () => {
  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(alice, 'dataExportRequests/request1'), {
    uid: 'alice',
    status: 'READY',
  }));
  await assertFails(getDoc(doc(alice, 'dataExportRequests/request1')));
});

test('data export objects are never directly readable or writable by clients', async () => {
  const aliceStorage = env.authenticatedContext('alice').storage();
  const exportRef = ref(aliceStorage, 'exports/alice/request1.json');
  await assertFails(uploadBytes(
    exportRef,
    new TextEncoder().encode('{"secret":true}'),
    { contentType: 'application/json' },
  ));
  await assertFails(getBytes(exportRef));
});
