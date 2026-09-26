import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import * as crypto from "crypto";
import * as https from "https";
import { db, requireAppCheck } from "./shared";

const PACKAGE_NAME = "com.match.app";
const ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher";
const OAUTH_AUDIENCE = "https://oauth2.googleapis.com/token";
const HOUR_MS = 60 * 60 * 1000;
const DAY_MS = 24 * HOUR_MS;

type EntitlementType = "MEMBERSHIP" | "BOOST";

interface PlayProduct {
  entitlementType: EntitlementType;
  entitlementId: string;
  productId: string;
  durationMs: number;
}

/**
 * Every paid digital entitlement in the Play-distributed app is declared here. The Android
 * client only transports the product id and opaque purchase token; duration and entitlement
 * semantics are owned by this trusted mapping.
 *
 * These product ids must exist as one-time products in Play Console. Purchases are consumed only
 * after the durable entitlement transaction so a temporary consume failure can be recovered from
 * queryPurchasesAsync without charging the member again.
 */
const PLAY_PRODUCTS: Record<string, PlayProduct> = {
  match_silver_3m: {
    entitlementType: "MEMBERSHIP",
    entitlementId: "SILVER_3M",
    productId: "match_silver_3m",
    durationMs: 90 * DAY_MS,
  },
  match_gold_6m: {
    entitlementType: "MEMBERSHIP",
    entitlementId: "GOLD_6M",
    productId: "match_gold_6m",
    durationMs: 180 * DAY_MS,
  },
  match_platinum_12m: {
    entitlementType: "MEMBERSHIP",
    entitlementId: "PLATINUM_12M",
    productId: "match_platinum_12m",
    durationMs: 365 * DAY_MS,
  },
  match_boost_3h: {
    entitlementType: "BOOST",
    entitlementId: "BOOST_3H",
    productId: "match_boost_3h",
    durationMs: 3 * HOUR_MS,
  },
  match_boost_24h: {
    entitlementType: "BOOST",
    entitlementId: "BOOST_24H",
    productId: "match_boost_24h",
    durationMs: 24 * HOUR_MS,
  },
  match_boost_7d: {
    entitlementType: "BOOST",
    entitlementId: "BOOST_7D",
    productId: "match_boost_7d",
    durationMs: 7 * DAY_MS,
  },
};

interface ProductLineItem {
  productId?: string;
  productOfferDetails?: {
    quantity?: number;
    refundableQuantity?: number;
    consumptionState?: string;
  };
}

interface ProductPurchaseV2 {
  productLineItem?: ProductLineItem[];
  purchaseStateContext?: { purchaseState?: string };
  orderId?: string;
  obfuscatedExternalAccountId?: string;
  regionCode?: string;
  purchaseCompletionTime?: string;
  acknowledgementState?: string;
}

interface DeveloperNotification {
  packageName?: string;
  eventTimeMillis?: string;
  oneTimeProductNotification?: {
    notificationType?: number;
    purchaseToken?: string;
    sku?: string;
  };
  voidedPurchaseNotification?: {
    purchaseToken?: string;
    orderId?: string;
    productType?: number;
    refundType?: number;
  };
}

interface VoidedPurchase {
  purchaseToken?: string;
  orderId?: string;
  voidedTimeMillis?: string;
  voidedReason?: number;
  voidedSource?: number;
  purchaseType?: number;
}

interface VoidedPurchasesResponse {
  voidedPurchases?: VoidedPurchase[];
  tokenPagination?: { nextPageToken?: string };
}

interface GrantResult {
  entitlementType: EntitlementType;
  entitlementId: string;
  expiresAtMillis: number;
  alreadyGranted: boolean;
}

let cachedAccessToken: { token: string; expiresAt: number } | null = null;

function sha256(value: string): string {
  return crypto.createHash("sha256").update(value, "utf8").digest("hex");
}

function base64Url(value: string | Buffer): string {
  return Buffer.from(value).toString("base64")
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");
}

function playCredentials(): { email: string; privateKey: string } {
  const email = process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL?.trim();
  const rawKey = process.env.GOOGLE_PLAY_PRIVATE_KEY;
  if (!email || !rawKey) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Google Play purchase verification is not configured"
    );
  }
  return { email, privateKey: rawKey.replace(/\\n/g, "\n") };
}

function requestJson<T>(options: https.RequestOptions, body?: string): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const req = https.request(options, (res) => {
      let raw = "";
      res.setEncoding("utf8");
      res.on("data", (chunk) => { raw += chunk; });
      res.on("end", () => {
        const status = res.statusCode || 500;
        if (status < 200 || status >= 300) {
          reject(new Error(`Google API status ${status}`));
          return;
        }
        if (!raw) {
          resolve({} as T);
          return;
        }
        try {
          resolve(JSON.parse(raw) as T);
        } catch {
          reject(new Error("Invalid Google API response"));
        }
      });
    });
    req.setTimeout(10000, () => req.destroy(new Error("Google API timeout")));
    req.on("error", reject);
    if (body) req.write(body);
    req.end();
  });
}

async function googleAccessToken(): Promise<string> {
  if (cachedAccessToken && cachedAccessToken.expiresAt > Date.now() + 60_000) {
    return cachedAccessToken.token;
  }

  const { email, privateKey } = playCredentials();
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = base64Url(JSON.stringify({
    iss: email,
    scope: ANDROID_PUBLISHER_SCOPE,
    aud: OAUTH_AUDIENCE,
    iat: now,
    exp: now + 3600,
  }));
  const unsignedJwt = `${header}.${claims}`;
  const signature = crypto.sign("RSA-SHA256", Buffer.from(unsignedJwt), privateKey);
  const assertion = `${unsignedJwt}.${base64Url(signature)}`;
  const body = new URLSearchParams({
    grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
    assertion,
  }).toString();

  const result = await requestJson<{ access_token?: string; expires_in?: number }>(
    {
      hostname: "oauth2.googleapis.com",
      port: 443,
      path: "/token",
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "Content-Length": Buffer.byteLength(body),
      },
    },
    body
  );
  if (!result.access_token) throw new Error("Google OAuth token missing");
  const ttlSeconds = Math.min(Math.max(Number(result.expires_in || 3600), 60), 3600);
  cachedAccessToken = { token: result.access_token, expiresAt: Date.now() + ttlSeconds * 1000 };
  return result.access_token;
}

async function publisherRequest<T>(method: "GET" | "POST", path: string): Promise<T> {
  const accessToken = await googleAccessToken();
  return requestJson<T>({
    hostname: "androidpublisher.googleapis.com",
    port: 443,
    path,
    method,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      Accept: "application/json",
      ...(method === "POST" ? { "Content-Length": "0" } : {}),
    },
  });
}

async function getPlayPurchase(purchaseToken: string): Promise<ProductPurchaseV2> {
  return publisherRequest<ProductPurchaseV2>(
    "GET",
    `/androidpublisher/v3/applications/${encodeURIComponent(PACKAGE_NAME)}` +
      `/purchases/productsv2/tokens/${encodeURIComponent(purchaseToken)}`
  );
}

async function consumePlayPurchase(productId: string, purchaseToken: string): Promise<void> {
  await publisherRequest<Record<string, never>>(
    "POST",
    `/androidpublisher/v3/applications/${encodeURIComponent(PACKAGE_NAME)}` +
      `/purchases/products/${encodeURIComponent(productId)}` +
      `/tokens/${encodeURIComponent(purchaseToken)}:consume`
  );
}

function validatePurchaseToken(value: unknown): string {
  const token = typeof value === "string" ? value.trim() : "";
  if (token.length < 10 || token.length > 4096 || /[\r\n]/.test(token)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid purchase token");
  }
  return token;
}

function existingGrant(
  existing: FirebaseFirestore.DocumentData,
  product: PlayProduct,
  uid: string,
  productId: string
): GrantResult {
  if (existing.uid !== uid || existing.productId !== productId) {
    throw new functions.https.HttpsError("already-exists", "Purchase token already used");
  }
  const storedType: EntitlementType = existing.entitlementType === "BOOST"
    ? "BOOST"
    : "MEMBERSHIP";
  const expiresAtMillis = Number(
    existing.expiresAtMillis || existing.premiumUntilMillis || existing.boostUntilMillis || 0
  );
  return {
    entitlementType: storedType,
    entitlementId: String(existing.entitlementId || existing.planId || product.entitlementId),
    expiresAtMillis: Number.isFinite(expiresAtMillis) ? expiresAtMillis : 0,
    alreadyGranted: true,
  };
}

/**
 * Verifies a Play one-time product with Google before granting any digital entitlement.
 * The client cannot supply price, duration, expiry, membership plan or boost duration.
 */
export const verifyGooglePlayPurchase = functions
  .runWith({
    timeoutSeconds: 60,
    secrets: ["GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL", "GOOGLE_PLAY_PRIVATE_KEY"],
  })
  .https.onCall(async (data, context) => {
    requireAppCheck(context);
    const uid = context.auth?.uid;
    if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

    const productId = typeof data?.productId === "string" ? data.productId.trim() : "";
    const product = PLAY_PRODUCTS[productId];
    if (!product) throw new functions.https.HttpsError("invalid-argument", "Unknown Play product");
    const purchaseToken = validatePurchaseToken(data?.purchaseToken);

    let purchase: ProductPurchaseV2;
    try {
      purchase = await getPlayPurchase(purchaseToken);
    } catch (err) {
      functions.logger.error("Google Play purchase lookup failed", {
        uid,
        productId,
        error: err instanceof Error ? err.message : String(err),
      });
      throw new functions.https.HttpsError("unavailable", "Unable to verify Google Play purchase");
    }

    if (purchase.purchaseStateContext?.purchaseState !== "PURCHASED") {
      if (purchase.purchaseStateContext?.purchaseState === "PENDING") {
        throw new functions.https.HttpsError("failed-precondition", "Purchase is still pending");
      }
      throw new functions.https.HttpsError("failed-precondition", "Purchase is not completed");
    }

    const lineItems = purchase.productLineItem || [];
    const matchingLineItems = lineItems.filter((item) => item.productId === productId);
    if (matchingLineItems.length !== 1 || lineItems.length !== 1) {
      throw new functions.https.HttpsError("invalid-argument", "Purchase product mismatch");
    }
    const quantity = matchingLineItems[0].productOfferDetails?.quantity ?? 1;
    if (quantity !== 1) {
      throw new functions.https.HttpsError("invalid-argument", "Unexpected purchase quantity");
    }

    if (purchase.obfuscatedExternalAccountId !== sha256(uid)) {
      throw new functions.https.HttpsError("permission-denied", "Purchase belongs to another account");
    }

    const tokenHash = sha256(purchaseToken);
    const paymentId = `play_${tokenHash}`;
    const purchaseRef = db.collection("payments").doc(paymentId);
    const userRef = db.collection("users").doc(uid);
    const subscriptionRef = db.collection("subscriptions").doc(uid);

    const result = await db.runTransaction<GrantResult>(async (tx) => {
      const reads = [tx.get(purchaseRef), tx.get(userRef)];
      if (product.entitlementType === "BOOST") reads.push(tx.get(subscriptionRef));
      const snapshots = await Promise.all(reads);
      const existingPurchase = snapshots[0];
      const userSnap = snapshots[1];
      const subscriptionSnap = snapshots[2];

      if (!userSnap.exists) {
        throw new functions.https.HttpsError("not-found", "User profile not found");
      }
      if (existingPurchase.exists) {
        return existingGrant(existingPurchase.data() || {}, product, uid, productId);
      }

      const user = userSnap.data() || {};
      let currentExpiry = 0;
      if (product.entitlementType === "MEMBERSHIP") {
        currentExpiry = user.premiumUntil instanceof admin.firestore.Timestamp
          ? user.premiumUntil.toMillis()
          : Number(user.subscriptionExpiry || 0);
      } else {
        currentExpiry = Number(subscriptionSnap?.data()?.boostUntil || 0);
      }
      const grantedAtMillis = Date.now();
      const base = Math.max(grantedAtMillis, Number.isFinite(currentExpiry) ? currentExpiry : 0);
      const expiresAtMillis = base + product.durationMs;
      const now = admin.firestore.FieldValue.serverTimestamp();

      const paymentRecord: Record<string, unknown> = {
        uid,
        provider: "GOOGLE_PLAY",
        status: "ACTIVATED",
        productId,
        entitlementType: product.entitlementType,
        entitlementId: product.entitlementId,
        durationMs: product.durationMs,
        grantedAtMillis,
        entitlementStartMillis: base,
        tokenHash,
        orderId: purchase.orderId || null,
        regionCode: purchase.regionCode || null,
        purchaseCompletionTime: purchase.purchaseCompletionTime || null,
        verifiedAt: now,
        expiresAtMillis,
      };

      if (product.entitlementType === "MEMBERSHIP") {
        const premiumUntil = admin.firestore.Timestamp.fromMillis(expiresAtMillis);
        paymentRecord.planId = product.entitlementId;
        paymentRecord.premiumUntilMillis = expiresAtMillis;
        tx.update(userRef, {
          isPremium: true,
          premiumPlan: product.entitlementId,
          subscriptionPlan: product.entitlementId,
          premiumUntil,
          subscriptionExpiry: expiresAtMillis,
          paymentId,
          updatedAt: now,
        });
      } else {
        paymentRecord.boostId = product.entitlementId;
        paymentRecord.boostUntilMillis = expiresAtMillis;
        tx.set(subscriptionRef, {
          boostUntil: expiresAtMillis,
          boostUpdatedAt: now,
        }, { merge: true });
      }
      tx.set(purchaseRef, paymentRecord, { merge: false });

      return {
        entitlementType: product.entitlementType,
        entitlementId: product.entitlementId,
        expiresAtMillis,
        alreadyGranted: false,
      };
    });

    let consumptionPending = false;
    const consumptionState = matchingLineItems[0].productOfferDetails?.consumptionState;
    if (consumptionState !== "CONSUMPTION_STATE_CONSUMED") {
      try {
        await consumePlayPurchase(productId, purchaseToken);
      } catch (err) {
        consumptionPending = true;
        await purchaseRef.set({
          consumptionPending: true,
          consumptionUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, { merge: true });
        functions.logger.warn("Google Play purchase granted but consumption retry is required", {
          uid,
          productId,
          tokenHash,
          error: err instanceof Error ? err.message : String(err),
        });
      }
      if (!consumptionPending) {
        await purchaseRef.set({
          consumptionPending: false,
          consumptionUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, { merge: true });
      }
    }

    const response: Record<string, unknown> = {
      success: true,
      entitlementType: result.entitlementType,
      entitlementId: result.entitlementId,
      expiresAt: result.expiresAtMillis,
      alreadyGranted: result.alreadyGranted,
      consumptionPending,
    };
    if (result.entitlementType === "MEMBERSHIP") {
      response.planId = result.entitlementId;
      response.premiumUntil = result.expiresAtMillis;
    } else {
      response.boostId = result.entitlementId;
      response.boostUntil = result.expiresAtMillis;
    }
    return response;
  });

/** Owner-only boost status. Entitlement documents remain unreadable to Android clients. */
export const getMyBoostStatus = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  const snapshot = await db.collection("subscriptions").doc(uid).get();
  const raw = Number(snapshot.data()?.boostUntil || 0);
  const boostUntil = Number.isFinite(raw) && raw > Date.now() ? raw : 0;
  return { boostUntil, active: boostUntil > Date.now() };
});


type ReconcileResult = "MISSING" | "ALREADY_VOIDED" | "REVOKED";

function paymentDurationMillis(data: FirebaseFirestore.DocumentData): number {
  const stored = Number(data.durationMs || 0);
  if (Number.isFinite(stored) && stored > 0) return stored;
  const product = PLAY_PRODUCTS[String(data.productId || "")];
  return product?.durationMs || 0;
}

function paymentGrantMillis(data: FirebaseFirestore.DocumentData): number {
  const explicit = Number(data.grantedAtMillis || 0);
  if (Number.isFinite(explicit) && explicit > 0) return explicit;
  const verified = data.verifiedAt;
  if (verified instanceof admin.firestore.Timestamp) return verified.toMillis();
  const completion = Date.parse(String(data.purchaseCompletionTime || ""));
  if (Number.isFinite(completion) && completion > 0) return completion;
  const duration = paymentDurationMillis(data);
  const expiry = Number(data.expiresAtMillis || 0);
  if (duration > 0 && Number.isFinite(expiry) && expiry > duration) return expiry - duration;
  return 0;
}

function isVoidedStatus(status: unknown): boolean {
  return ["VOIDED", "REFUNDED", "CHARGEBACK", "REVOKED", "CANCELED"]
    .includes(String(status || "").toUpperCase());
}

function rebuiltEntitlement(
  docs: FirebaseFirestore.QueryDocumentSnapshot[],
  excludedPaymentId: string
): { expiresAtMillis: number; entitlementId: string; paymentId: string } {
  const entries = docs
    .filter((doc) => doc.id !== excludedPaymentId)
    .map((doc) => ({ id: doc.id, data: doc.data() }))
    .filter(({ data }) =>
      data.provider === "GOOGLE_PLAY" &&
      !isVoidedStatus(data.status) &&
      paymentDurationMillis(data) > 0
    )
    .map(({ id, data }) => ({
      id,
      data,
      durationMs: paymentDurationMillis(data),
      grantedAtMillis: paymentGrantMillis(data),
    }))
    .sort((a, b) => a.grantedAtMillis - b.grantedAtMillis || a.id.localeCompare(b.id));

  let cursor = 0;
  let entitlementId = "";
  let paymentId = "";
  for (const entry of entries) {
    const start = Math.max(entry.grantedAtMillis, cursor);
    cursor = start + entry.durationMs;
    entitlementId = String(entry.data.entitlementId || entry.data.planId || entry.data.boostId || "");
    paymentId = entry.id;
  }
  return { expiresAtMillis: cursor, entitlementId, paymentId };
}

/**
 * Revokes a voided/refunded/charged-back Play purchase without storing the raw purchase token.
 * The remaining entitlement is rebuilt from all non-voided grant contributions so stacked
 * purchases converge deterministically after any sequence of refunds.
 */
async function reconcileVoidedPurchase(
  purchaseToken: string,
  reason: string,
  eventTimeMillis: number,
  orderId?: string
): Promise<ReconcileResult> {
  const safeToken = validatePurchaseToken(purchaseToken);
  const tokenHash = sha256(safeToken);
  const paymentRef = db.collection("payments").doc(`play_${tokenHash}`);

  return db.runTransaction<ReconcileResult>(async (tx) => {
    const paymentSnap = await tx.get(paymentRef);
    if (!paymentSnap.exists) return "MISSING";
    const payment = paymentSnap.data() || {};
    if (isVoidedStatus(payment.status)) return "ALREADY_VOIDED";

    const uid = String(payment.uid || "");
    const entitlementType: EntitlementType =
      payment.entitlementType === "BOOST" ? "BOOST" : "MEMBERSHIP";
    if (!uid) throw new Error("Play payment is missing uid");
    if (orderId && payment.orderId && String(payment.orderId) !== orderId) {
      throw new Error("Play voided purchase order mismatch");
    }

    const ledgerQuery = db.collection("payments")
      .where("uid", "==", uid)
      .where("entitlementType", "==", entitlementType);
    const authorityRef = entitlementType === "MEMBERSHIP"
      ? db.collection("users").doc(uid)
      : db.collection("subscriptions").doc(uid);
    const [ledgerSnap, authoritySnap] = await Promise.all([
      tx.get(ledgerQuery),
      tx.get(authorityRef),
    ]);
    if (!authoritySnap.exists && entitlementType === "MEMBERSHIP") {
      throw new Error("Play entitlement user no longer exists");
    }

    const rebuilt = rebuiltEntitlement(ledgerSnap.docs, paymentRef.id);
    const nowMillis = Date.now();
    const stillActive = rebuilt.expiresAtMillis > nowMillis;
    const serverNow = admin.firestore.FieldValue.serverTimestamp();

    tx.update(paymentRef, {
      status: "VOIDED",
      voidedReason: reason,
      voidedAtMillis: eventTimeMillis > 0 ? eventTimeMillis : nowMillis,
      voidedAt: serverNow,
      reconciledAt: serverNow,
      orderId: orderId || payment.orderId || null,
    });

    if (entitlementType === "MEMBERSHIP") {
      tx.update(authorityRef, {
        isPremium: stillActive,
        premiumPlan: stillActive ? rebuilt.entitlementId : admin.firestore.FieldValue.delete(),
        subscriptionPlan: stillActive ? rebuilt.entitlementId : "FREE",
        premiumUntil: stillActive
          ? admin.firestore.Timestamp.fromMillis(rebuilt.expiresAtMillis)
          : admin.firestore.FieldValue.delete(),
        subscriptionExpiry: stillActive ? rebuilt.expiresAtMillis : 0,
        paymentId: stillActive ? rebuilt.paymentId : admin.firestore.FieldValue.delete(),
        updatedAt: serverNow,
      });
    } else {
      tx.set(authorityRef, {
        boostUntil: stillActive ? rebuilt.expiresAtMillis : 0,
        boostUpdatedAt: serverNow,
      }, { merge: true });
    }
    return "REVOKED";
  });
}

async function reconcileCancelledOneTimePurchase(
  purchaseToken: string,
  eventTimeMillis: number
): Promise<ReconcileResult> {
  const tokenHash = sha256(validatePurchaseToken(purchaseToken));
  const payment = await db.collection("payments").doc(`play_${tokenHash}`).get();
  if (!payment.exists) return "MISSING";

  const latest = await getPlayPurchase(purchaseToken);
  if (latest.purchaseStateContext?.purchaseState === "PURCHASED") {
    return "ALREADY_VOIDED";
  }
  return reconcileVoidedPurchase(
    purchaseToken,
    "ONE_TIME_PRODUCT_CANCELED",
    eventTimeMillis,
    latest.orderId
  );
}

async function processRtdnEvent(
  payload: DeveloperNotification,
  eventId: string
): Promise<void> {
  if (payload.packageName !== PACKAGE_NAME) {
    functions.logger.warn("Ignoring Google Play RTDN for another package", {
      packageName: payload.packageName || null,
    });
    return;
  }

  const eventTimeMillis = Number(payload.eventTimeMillis || 0);
  const voided = payload.voidedPurchaseNotification;
  const oneTime = payload.oneTimeProductNotification;
  const purchaseToken = voided?.purchaseToken || oneTime?.purchaseToken || "";
  const eventRef = db.collection("webhookEvents").doc(`google_play_${eventId}`);
  const shouldProcess = await db.runTransaction(async (tx) => {
    const existing = await tx.get(eventRef);
    if (existing.exists && existing.data()?.status === "PROCESSED") return false;
    tx.set(eventRef, {
      provider: "GOOGLE_PLAY",
      type: voided ? "VOIDED_PURCHASE" : oneTime ? "ONE_TIME_PRODUCT" : "OTHER",
      status: "PROCESSING",
      eventTimeMillis: Number.isFinite(eventTimeMillis) ? eventTimeMillis : 0,
      purchaseTokenHash: purchaseToken ? sha256(purchaseToken) : null,
      orderId: voided?.orderId || null,
      attempts: admin.firestore.FieldValue.increment(1),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    return true;
  });
  if (!shouldProcess) return;

  try {
    if (voided?.purchaseToken && (voided.productType === 2 || voided.productType == null)) {
      await reconcileVoidedPurchase(
        voided.purchaseToken,
        `VOIDED_REFUND_TYPE_${voided.refundType || 0}`,
        eventTimeMillis,
        voided.orderId
      );
    } else if (oneTime?.purchaseToken && oneTime.notificationType === 2) {
      await reconcileCancelledOneTimePurchase(oneTime.purchaseToken, eventTimeMillis);
    }

    await eventRef.set({
      status: "PROCESSED",
      processedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
  } catch (err) {
    await eventRef.set({
      status: "FAILED",
      lastError: err instanceof Error ? err.message.slice(0, 300) : "Unknown processing failure",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    throw err;
  }
}

/**
 * Play Console must publish RTDNs to this topic. The trigger stores only hashes/metadata and is
 * idempotent by Pub/Sub event id. Play Console topic wiring remains an external production gate.
 */
export const handleGooglePlayRtdn = functions
  .runWith({
    timeoutSeconds: 60,
    secrets: ["GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL", "GOOGLE_PLAY_PRIVATE_KEY"],
  })
  .pubsub.topic("google-play-billing-rtdn")
  .onPublish(async (message, context) => {
    const payload = message.json as DeveloperNotification;
    await processRtdnEvent(payload, context.eventId);
  });

/**
 * Pull reconciliation is a fallback for missed RTDNs. Google limits startTime to the last 30 days,
 * so each run uses a rolling 29-day window and idempotent payment status updates.
 */
export const reconcileVoidedGooglePlayPurchases = functions
  .runWith({
    timeoutSeconds: 300,
    secrets: ["GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL", "GOOGLE_PLAY_PRIVATE_KEY"],
  })
  .pubsub.schedule("every 6 hours")
  .onRun(async () => {
    const startTime = Date.now() - 29 * DAY_MS;
    let pageToken = "";
    let pages = 0;
    do {
      const query = new URLSearchParams({
        startTime: String(startTime),
        maxResults: "1000",
        type: "0",
      });
      if (pageToken) query.set("token", pageToken);
      const response = await publisherRequest<VoidedPurchasesResponse>(
        "GET",
        `/androidpublisher/v3/applications/${encodeURIComponent(PACKAGE_NAME)}` +
          `/purchases/voidedpurchases?${query.toString()}`
      );
      for (const item of response.voidedPurchases || []) {
        if (!item.purchaseToken) continue;
        await reconcileVoidedPurchase(
          item.purchaseToken,
          `VOIDED_REASON_${item.voidedReason || 0}_SOURCE_${item.voidedSource || 0}`,
          Number(item.voidedTimeMillis || 0),
          item.orderId
        );
      }
      pageToken = response.tokenPagination?.nextPageToken || "";
      pages += 1;
    } while (pageToken && pages < 20);
    return null;
  });

/**
 * Expiry cleanup keeps public flags honest even when no client opens the app at the moment an
 * entitlement expires. Server authorization still checks expiry independently of these flags.
 */
export const reconcileExpiredGooglePlayEntitlements = functions.pubsub
  .schedule("every 60 minutes")
  .onRun(async () => {
    const nowMillis = Date.now();
    let userPasses = 0;
    while (userPasses < 20) {
      const expiredUsers = await db.collection("users")
        .where("subscriptionExpiry", ">", 0)
        .where("subscriptionExpiry", "<=", nowMillis)
        .limit(300)
        .get();
      if (expiredUsers.empty) break;
      const batch = db.batch();
      expiredUsers.docs.forEach((doc) => {
        batch.update(doc.ref, {
          isPremium: false,
          premiumPlan: admin.firestore.FieldValue.delete(),
          subscriptionPlan: "FREE",
          premiumUntil: admin.firestore.FieldValue.delete(),
          subscriptionExpiry: 0,
          paymentId: admin.firestore.FieldValue.delete(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      });
      await batch.commit();
      userPasses += 1;
      if (expiredUsers.size < 300) break;
    }

    let boostPasses = 0;
    while (boostPasses < 20) {
      const expiredBoosts = await db.collection("subscriptions")
        .where("boostUntil", ">", 0)
        .where("boostUntil", "<=", nowMillis)
        .limit(300)
        .get();
      if (expiredBoosts.empty) break;
      const batch = db.batch();
      expiredBoosts.docs.forEach((doc) => {
        batch.set(doc.ref, {
          boostUntil: 0,
          boostUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, { merge: true });
      });
      await batch.commit();
      boostPasses += 1;
      if (expiredBoosts.size < 300) break;
    }
    return null;
  });

