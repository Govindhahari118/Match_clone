import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";

const RELIGION_IDS: Record<string, string> = {
  "hindu": "HINDU",
  "muslim": "MUSLIM",
  "christian": "CHRISTIAN",
  "sikh": "SIKH",
  "buddhist": "BUDDHIST",
  "jain": "JAIN",
  "parsi / zoroastrian": "PARSI_ZOROASTRIAN",
  "other": "OTHER",
  "prefer not to say": "PREFER_NOT_TO_SAY",
};

function religionIdFor(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const normalized = value.trim().toLowerCase();
  if (!normalized) return null;
  return RELIGION_IDS[normalized] ?? "OTHER";
}

/**
 * Backfills authoritative religion metadata once a profile has a confirmed religion value.
 * Firestore rules make a populated religion immutable to the client immediately, so there is
 * no race window while this trigger writes religionId/religionLocked/religionConfirmedAt.
 * Trusted support/admin correction remains possible through Admin SDK workflows.
 */
export const onReligionProfileWrite = functions.firestore
  .document("users/{uid}")
  .onWrite(async (change) => {
    if (!change.after.exists) return;

    const after = change.after.data();
    const religionId = religionIdFor(after.religion);
    if (!religionId) return;

    const alreadyLocked = after.religionLocked === true;
    const idCurrent = after.religionId === religionId;
    const hasConfirmedAt = after.religionConfirmedAt != null;
    if (alreadyLocked && idCurrent && hasConfirmedAt) return;

    const patch: Record<string, unknown> = {
      religionId,
      religionLocked: true,
    };
    if (!hasConfirmedAt) {
      patch.religionConfirmedAt = admin.firestore.FieldValue.serverTimestamp();
    }

    await change.after.ref.set(patch, { merge: true });
  });
