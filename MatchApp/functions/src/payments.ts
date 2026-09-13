import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import * as crypto from "crypto";
import * as https from "https";
import { db, requireAppCheck } from "./shared";

interface PlanConfig {
  id: string;
  amount: number;
  currency: "INR";
  durationDays: number;
  contactLimit: number;
}

const PLANS: Record<string, PlanConfig> = {
  SILVER_3M: { id: "SILVER_3M", amount: 299900, currency: "INR", durationDays: 90, contactLimit: 75 },
  GOLD_6M: { id: "GOLD_6M", amount: 499900, currency: "INR", durationDays: 180, contactLimit: 150 },
  PLATINUM_12M: { id: "PLATINUM_12M", amount: 749900, currency: "INR", durationDays: 365, contactLimit: 300 },
};

interface RazorpayPayment {
  id: string;
  amount: number;
  currency: string;
  status: string;
  order_id: string | null;
  captured?: boolean;
}

interface RazorpayOrder {
  id: string;
  amount: number;
  amount_paid: number;
  currency: string;
  status: string;
  receipt?: string;
}

function razorpayCredentials(): { keyId: string; keySecret: string } {
  const keyId = functions.config().razorpay?.key_id as string | undefined;
  const keySecret = functions.config().razorpay?.key_secret as string | undefined;
  if (!keyId || !keySecret) {
    throw new functions.https.HttpsError("failed-precondition", "Payment service is not configured");
  }
  return { keyId, keySecret };
}

function razorpayRequest<T>(method: "GET" | "POST", path: string, body?: unknown): Promise<T> {
  const { keyId, keySecret } = razorpayCredentials();
  const payload = body === undefined ? undefined : JSON.stringify(body);
  return new Promise<T>((resolve, reject) => {
    const req = https.request({
      hostname: "api.razorpay.com",
      port: 443,
      path,
      method,
      headers: {
        Authorization: `Basic ${Buffer.from(`${keyId}:${keySecret}`).toString("base64")}`,
        Accept: "application/json",
        ...(payload ? {
          "Content-Type": "application/json",
          "Content-Length": Buffer.byteLength(payload),
        } : {}),
      },
      timeout: 10000,
    }, (res) => {
      let raw = "";
      res.setEncoding("utf8");
      res.on("data", (chunk) => { raw += chunk; });
      res.on("end", () => {
        const status = res.statusCode || 500;
        if (status < 200 || status >= 300) {
          functions.logger.error("Razorpay API request failed", { path, status });
          reject(new Error(`Razorpay API status ${status}`));
          return;
        }
        try {
          resolve(JSON.parse(raw) as T);
        } catch {
          reject(new Error("Invalid Razorpay API response"));
        }
      });
    });
    req.on("timeout", () => req.destroy(new Error("Razorpay API timeout")));
    req.on("error", reject);
    if (payload) req.write(payload);
    req.end();
  });
}

function secureSignatureMatches(message: string, received: string, secret: string): boolean {
  const expected = crypto.createHmac("sha256", secret).update(message).digest("hex");
  const a = Buffer.from(expected, "utf8");
  const b = Buffer.from(received || "", "utf8");
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

function validateId(value: unknown, prefix: string): string {
  const id = typeof value === "string" ? value.trim() : "";
  if (!id.startsWith(prefix) || id.length > 100) {
    throw new functions.https.HttpsError("invalid-argument", `Invalid ${prefix} identifier`);
  }
  return id;
}

async function activateCapturedPayment(
  uid: string,
  orderId: string,
  paymentId: string,
  source: "checkout" | "webhook"
): Promise<number> {
  const orderRef = db.collection("paymentOrders").doc(orderId);
  const orderSnap = await orderRef.get();
  if (!orderSnap.exists) throw new functions.https.HttpsError("not-found", "Payment order not found");
  const pending = orderSnap.data() || {};
  if (pending.uid !== uid) throw new functions.https.HttpsError("permission-denied", "Order does not belong to this account");

  const planId = pending.planId as string;
  const plan = PLANS[planId];
  if (!plan) throw new functions.https.HttpsError("failed-precondition", "Unknown order plan");
  if ((pending.expiresAt?.toMillis?.() || 0) < Date.now()) {
    throw new functions.https.HttpsError("deadline-exceeded", "Payment order has expired");
  }

  const [payment, order] = await Promise.all([
    razorpayRequest<RazorpayPayment>("GET", `/v1/payments/${encodeURIComponent(paymentId)}`),
    razorpayRequest<RazorpayOrder>("GET", `/v1/orders/${encodeURIComponent(orderId)}`),
  ]);

  if (payment.id !== paymentId || payment.order_id !== orderId) {
    throw new functions.https.HttpsError("invalid-argument", "Payment/order mismatch");
  }
  if (payment.status !== "captured" || payment.captured === false) {
    throw new functions.https.HttpsError("failed-precondition", "Payment is not captured");
  }
  if (order.id !== orderId || order.status !== "paid") {
    throw new functions.https.HttpsError("failed-precondition", "Order is not paid");
  }
  if (payment.amount !== plan.amount || order.amount !== plan.amount || order.amount_paid < plan.amount) {
    throw new functions.https.HttpsError("invalid-argument", "Payment amount mismatch");
  }
  if (payment.currency !== plan.currency || order.currency !== plan.currency) {
    throw new functions.https.HttpsError("invalid-argument", "Payment currency mismatch");
  }

  const paymentRef = db.collection("payments").doc(paymentId);
  const userRef = db.collection("users").doc(uid);
  return db.runTransaction(async (tx) => {
    const [existingPayment, userSnap, freshOrder] = await Promise.all([
      tx.get(paymentRef),
      tx.get(userRef),
      tx.get(orderRef),
    ]);

    if (existingPayment.exists) {
      const existing = existingPayment.data() || {};
      if (existing.uid !== uid || existing.orderId !== orderId) {
        throw new functions.https.HttpsError("already-exists", "Payment already consumed by another order");
      }
      return Number(existing.premiumUntilMillis || 0);
    }
    if (!userSnap.exists) throw new functions.https.HttpsError("not-found", "User profile not found");
    if ((freshOrder.data()?.status as string | undefined) === "ACTIVATED") {
      throw new functions.https.HttpsError("already-exists", "Order was already activated");
    }

    const user = userSnap.data() || {};
    const existingExpiry = user.premiumUntil instanceof admin.firestore.Timestamp
      ? user.premiumUntil.toMillis()
      : Number(user.subscriptionExpiry || 0);
    const base = Math.max(Date.now(), Number.isFinite(existingExpiry) ? existingExpiry : 0);
    const premiumUntilMillis = base + plan.durationDays * 24 * 60 * 60 * 1000;
    const premiumUntil = admin.firestore.Timestamp.fromMillis(premiumUntilMillis);
    const now = admin.firestore.FieldValue.serverTimestamp();

    tx.set(paymentRef, {
      uid,
      orderId,
      paymentId,
      planId,
      amount: plan.amount,
      currency: plan.currency,
      status: "ACTIVATED",
      source,
      verifiedAt: now,
      premiumUntilMillis,
    });
    tx.update(orderRef, {
      status: "ACTIVATED",
      paymentId,
      activatedAt: now,
    });
    tx.update(userRef, {
      isPremium: true,
      premiumPlan: planId,
      subscriptionPlan: planId,
      premiumUntil,
      subscriptionExpiry: premiumUntilMillis,
      paymentId,
      updatedAt: now,
    });
    return premiumUntilMillis;
  });
}

export const createRazorpayOrder = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const planId = typeof data?.planId === "string" ? data.planId.trim().toUpperCase() : "";
  const plan = PLANS[planId];
  if (!plan) throw new functions.https.HttpsError("invalid-argument", "Invalid membership plan");

  const receipt = `mc_${uid.slice(0, 12)}_${Date.now()}`;
  let order: RazorpayOrder;
  try {
    order = await razorpayRequest<RazorpayOrder>("POST", "/v1/orders", {
      amount: plan.amount,
      currency: plan.currency,
      receipt,
      partial_payment: false,
      notes: { uid, planId },
    });
  } catch (err) {
    functions.logger.error("Razorpay order creation failed", { uid, planId, error: err instanceof Error ? err.message : String(err) });
    throw new functions.https.HttpsError("unavailable", "Unable to initialize payment");
  }

  if (!order.id?.startsWith("order_") || order.amount !== plan.amount || order.currency !== plan.currency) {
    throw new functions.https.HttpsError("internal", "Unexpected payment order response");
  }

  await db.collection("paymentOrders").doc(order.id).set({
    uid,
    planId,
    expectedAmount: plan.amount,
    currency: plan.currency,
    status: "CREATED",
    receipt,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    expiresAt: admin.firestore.Timestamp.fromMillis(Date.now() + 60 * 60 * 1000),
  });

  return { id: order.id, planId, amount: plan.amount, currency: plan.currency };
});

export const verifyRazorpayPayment = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Sign in required");

  const orderId = validateId(data?.orderId, "order_");
  const paymentId = validateId(data?.paymentId, "pay_");
  const signature = typeof data?.signature === "string" ? data.signature.trim() : "";
  if (!/^[a-fA-F0-9]{64}$/.test(signature)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid payment signature");
  }

  const pending = await db.collection("paymentOrders").doc(orderId).get();
  if (!pending.exists || pending.data()?.uid !== uid) {
    throw new functions.https.HttpsError("permission-denied", "Unknown payment order");
  }

  const { keySecret } = razorpayCredentials();
  if (!secureSignatureMatches(`${orderId}|${paymentId}`, signature, keySecret)) {
    throw new functions.https.HttpsError("invalid-argument", "Payment verification failed");
  }

  const premiumUntil = await activateCapturedPayment(uid, orderId, paymentId, "checkout");
  return { success: true, premiumUntil };
});

async function revokeRefundedPayment(paymentId: string): Promise<void> {
  const paymentRef = db.collection("payments").doc(paymentId);
  await db.runTransaction(async (tx) => {
    const paymentSnap = await tx.get(paymentRef);
    if (!paymentSnap.exists) return;
    const payment = paymentSnap.data() || {};
    if (payment.status === "REFUNDED") return;
    const uid = payment.uid as string;
    const userRef = db.collection("users").doc(uid);
    const userSnap = await tx.get(userRef);
    tx.update(paymentRef, { status: "REFUNDED", refundedAt: admin.firestore.FieldValue.serverTimestamp() });
    if (userSnap.exists && userSnap.data()?.paymentId === paymentId) {
      tx.update(userRef, {
        isPremium: false,
        premiumPlan: admin.firestore.FieldValue.delete(),
        subscriptionPlan: "FREE",
        premiumUntil: admin.firestore.FieldValue.delete(),
        subscriptionExpiry: 0,
        paymentId: admin.firestore.FieldValue.delete(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }
  });
}

export const razorpayWebhook = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") { res.status(405).send("Method Not Allowed"); return; }
  const secret = functions.config().razorpay?.webhook_secret as string | undefined;
  if (!secret) { functions.logger.error("Razorpay webhook secret not configured"); res.status(503).send("Unavailable"); return; }

  const received = String(req.get("X-Razorpay-Signature") || "");
  const rawBody = req.rawBody;
  if (!rawBody || !secureSignatureMatches(rawBody.toString("utf8"), received, secret)) {
    res.status(401).send("Invalid signature"); return;
  }

  const eventId = String(req.get("x-razorpay-event-id") || "").slice(0, 200);
  const event = req.body?.event as string | undefined;
  try {
    if (event === "payment.captured") {
      const entity = req.body?.payload?.payment?.entity as RazorpayPayment | undefined;
      if (entity?.id && entity.order_id) {
        const orderSnap = await db.collection("paymentOrders").doc(entity.order_id).get();
        const uid = orderSnap.data()?.uid as string | undefined;
        if (uid) await activateCapturedPayment(uid, entity.order_id, entity.id, "webhook");
      }
    } else if (event === "payment.refunded") {
      const entity = req.body?.payload?.payment?.entity as RazorpayPayment | undefined;
      if (entity?.id) await revokeRefundedPayment(entity.id);
    } else if (event === "refund.processed") {
      const paymentId = req.body?.payload?.refund?.entity?.payment_id as string | undefined;
      if (paymentId) {
        const payment = await razorpayRequest<RazorpayPayment>("GET", `/v1/payments/${encodeURIComponent(paymentId)}`);
        if (payment.status === "refunded") await revokeRefundedPayment(paymentId);
      }
    }

    if (eventId) {
      await db.collection("webhookEvents").doc(eventId).set({
        event: event || "unknown",
        processedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
    }
    res.status(200).send("ok");
  } catch (err) {
    functions.logger.error("Razorpay webhook processing failed", { event, eventId, error: err instanceof Error ? err.message : String(err) });
    res.status(500).send("retry");
  }
});
