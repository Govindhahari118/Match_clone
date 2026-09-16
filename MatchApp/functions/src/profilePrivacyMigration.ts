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
