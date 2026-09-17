import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";
import { db } from "./shared";

const PAGE_SIZE = 250;
const PRIVATE_PROFILE_FIELDS = [
  "email",
  "phoneNumber",
  "fcmToken",
  "dateOfBirth",
  "rasi",
  "nakshatra",
  "manglik",
  "birthTime",
  "birthPlace",
  "incomeBand",
] as const;

/**
 * Incrementally removes legacy private fields from `users/{uid}` and copies them into
 * `userPrivate/{uid}` before deletion. A persistent cursor makes the migration safe for large
 * production datasets and idempotent across scheduler retries.
 */
export const migrateLegacyPrivateProfileFields = functions.pubsub
  .schedule("15 3 * * *")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const stateRef = db.collection("systemMigrations").doc("privateProfileFieldsV1");
    const state = await stateRef.get();
    const cursor = typeof state.data()?.cursor === "string" ? String(state.data()?.cursor) : "";

    let query: FirebaseFirestore.Query = db.collection("users")
      .orderBy(admin.firestore.FieldPath.documentId())
      .limit(PAGE_SIZE);
    if (cursor) query = query.startAfter(cursor);

    const page = await query.get();
    if (page.empty) {
      await stateRef.set({
        cursor: "",
        completedAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
      return null;
    }

    const batch = db.batch();
    let migratedProfiles = 0;
    page.docs.forEach((doc) => {
      const data = doc.data();
      const privateData: Record<string, unknown> = {};
      const publicDeletes: Record<string, FirebaseFirestore.FieldValue> = {};

      for (const field of PRIVATE_PROFILE_FIELDS) {
        if (data[field] !== undefined) {
          privateData[field] = data[field];
          publicDeletes[field] = admin.firestore.FieldValue.delete();
        }
      }

      if (Object.keys(privateData).length > 0) {
        privateData.updatedAt = admin.firestore.FieldValue.serverTimestamp();
        batch.set(db.collection("userPrivate").doc(doc.id), privateData, { merge: true });
        batch.update(doc.ref, publicDeletes);
        migratedProfiles += 1;
      }
    });

    const lastId = page.docs[page.docs.length - 1].id;
    batch.set(stateRef, {
      cursor: lastId,
      scanned: admin.firestore.FieldValue.increment(page.size),
      migrated: admin.firestore.FieldValue.increment(migratedProfiles),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    await batch.commit();

    functions.logger.info("Legacy public profile privacy migration page completed", {
      scanned: page.size,
      migrated: migratedProfiles,
      cursor: lastId,
    });
    return null;
  });

/**
 * Removes obsolete browsing/activity preferences from public profile documents.
 *
 * `isIncognito` is a device-local browsing choice and must never be discoverable from another
 * member's public profile. `showLastActive` has been replaced by owner-only `privacySettings`.
 * When migrating the old boolean we preserve the user's intent only if they have not already
 * chosen a value in the new privacy dashboard. The transaction prevents the migration from
 * overwriting a concurrent modern preference change.
 */
export const migrateLegacyPublicVisibilityFields = functions.pubsub
  .schedule("45 3 * * *")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const stateRef = db.collection("systemMigrations").doc("publicVisibilityFieldsV1");
    const state = await stateRef.get();
    const cursor = typeof state.data()?.cursor === "string" ? String(state.data()?.cursor) : "";

    let query: FirebaseFirestore.Query = db.collection("users")
      .orderBy(admin.firestore.FieldPath.documentId())
      .limit(PAGE_SIZE);
    if (cursor) query = query.startAfter(cursor);

    const page = await query.get();
    if (page.empty) {
      await stateRef.set({
        cursor: "",
        completedAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
      return null;
    }

    let migratedProfiles = 0;
    for (const pageDoc of page.docs) {
      const changed = await db.runTransaction(async (tx) => {
        const userRef = pageDoc.ref;
        const settingsRef = db.collection("privacySettings").doc(pageDoc.id);
        const [userSnap, settingsSnap] = await Promise.all([
          tx.get(userRef),
          tx.get(settingsRef),
        ]);
        if (!userSnap.exists) return false;

        const user = userSnap.data() || {};
        const hasIncognito = user.isIncognito !== undefined;
        const hasLegacyLastActive = user.showLastActive !== undefined;
        if (!hasIncognito && !hasLegacyLastActive) return false;

        if (hasLegacyLastActive && settingsSnap.data()?.lastActiveVisibility === undefined) {
          tx.set(settingsRef, {
            // The old switch was global: false meant nobody; true meant visible to members.
            lastActiveVisibility: user.showLastActive === false ? "nobody" : "everyone",
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          }, { merge: true });
        }

        const deletes: Record<string, FirebaseFirestore.FieldValue> = {};
        if (hasIncognito) deletes.isIncognito = admin.firestore.FieldValue.delete();
        if (hasLegacyLastActive) deletes.showLastActive = admin.firestore.FieldValue.delete();
        tx.update(userRef, deletes);
        return true;
      });
      if (changed) migratedProfiles += 1;
    }

    const lastId = page.docs[page.docs.length - 1].id;
    await stateRef.set({
      cursor: lastId,
      scanned: admin.firestore.FieldValue.increment(page.size),
      migrated: admin.firestore.FieldValue.increment(migratedProfiles),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });

    functions.logger.info("Legacy public visibility migration page completed", {
      scanned: page.size,
      migrated: migratedProfiles,
      cursor: lastId,
    });
    return null;
  });
