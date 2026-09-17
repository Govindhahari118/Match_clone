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
      const base = Math.max(Date.now(), Number.isFinite(currentExpiry) ? currentExpiry : 0);
      const expiresAtMillis = base + product.durationMs;
      const now = admin.firestore.FieldValue.serverTimestamp();

      const paymentRecord: Record<string, unknown> = {
        uid,
        provider: "GOOGLE_PLAY",
        status: "ACTIVATED",
        productId,
        entitlementType: product.entitlementType,
        entitlementId: product.entitlementId,
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
        functions.logger.warn("Google Play purchase granted but consumption retry is required", {
          uid,
          productId,
          tokenHash,
          error: err instanceof Error ? err.message : String(err),
        });
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
