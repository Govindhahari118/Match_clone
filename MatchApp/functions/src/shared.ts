import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

export const db = admin.firestore();
export const messaging = admin.messaging();

/** ── Helper: Generate TeluguMatrimony ID ──────────────────────────────────── */
export function generateMatrimonyId(): string {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  let id = "TLG-";
  for (let i = 0; i < 5; i++) {
    id += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return id;
}
