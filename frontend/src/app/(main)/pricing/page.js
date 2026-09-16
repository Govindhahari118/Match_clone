"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { toast } from "react-toastify";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";

function humanize(value) {
  return String(value || "").replace(/_/g, " ").replace(/\b\w/g, (char) => char.toUpperCase());
}

function entitlementRows(entitlements = {}) {
  return [
    ["Daily recommendations", entitlements.dailyRecommendations],
    ["Interests per day", entitlements.interestsPerDay],
    ["Contact requests per month", entitlements.contactRequestsPerMonth],
    ["Messages with connections", entitlements.messagesWithConnections],
    ["Profile boosts per week", entitlements.visibilityBoostsPerWeek],
    ["Who viewed me", entitlements.canViewWhoViewed ? "Included" : "Not included"],
    ["Voice calling", entitlements.canVoiceCall ? "Included" : "Not included"],
    ["Video calling", entitlements.canVideoCall ? "Included" : "Not included"],
  ];
}

function loadRazorpayScript() {
  if (typeof window === "undefined") return Promise.resolve(false);
  if (window.Razorpay) return Promise.resolve(true);
  return new Promise((resolve) => {
    const existing = document.querySelector('script[data-matrimony-razorpay="true"]');
    if (existing) {
      existing.addEventListener("load", () => resolve(true), { once: true });
      existing.addEventListener("error", () => resolve(false), { once: true });
      return;
    }
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.async = true;
    script.dataset.matrimonyRazorpay = "true";
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

export default function PricingPage() {
  const { user } = useAuth();
  const [plans, setPlans] = useState([]);
  const [current, setCurrent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [checkoutPlan, setCheckoutPlan] = useState(null);
  const [pendingOrder, setPendingOrder] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setLoading(true);
      setError("");
      try {
        const [plansResponse, entitlementResponse] = await Promise.all([
          api.get("/payment/plans"),
          api.get("/subscription/entitlements").catch(() => ({ data: null })),
        ]);
        if (cancelled) return;
        setPlans(Array.isArray(plansResponse.data) ? plansResponse.data : []);
        setCurrent(entitlementResponse.data || null);
      } catch (requestError) {
        if (!cancelled) {
          setPlans([]);
          setError(requestError?.response?.data?.error || "Plans could not be loaded. No fallback prices are shown because billing must remain authoritative.");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    load();
    return () => { cancelled = true; };
  }, []);

  const currentLabel = useMemo(() => current?.plan ? humanize(current.plan) : "Free", [current]);

  const refreshEntitlements = async () => {
    const response = await api.get("/subscription/entitlements");
    setCurrent(response.data);
    return response.data;
  };

  const reconcileOrder = async (orderId) => {
    if (!orderId) return;
    try {
      const response = await api.get(`/payment/status/${orderId}`);
      if (response.data?.status === "completed") {
        await refreshEntitlements();
        setPendingOrder(null);
        toast.success("Payment confirmed and membership is active.");
      } else {
        toast.info(`Payment status: ${humanize(response.data?.status || "pending")}`);
      }
    } catch (requestError) {
      toast.error(requestError?.response?.data?.error || "Payment status could not be checked.");
    }
  };

  const handlePurchase = async (plan) => {
    if (!user) {
      toast.info("Please login before purchasing a membership.");
      return;
    }
    const keyId = process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID;
    if (!keyId) {
      toast.error("Checkout is not configured in this environment. No payment was attempted.");
      return;
    }

    setCheckoutPlan(plan.id);
    setError("");
    try {
      const idempotencyKey = typeof crypto !== "undefined" && crypto.randomUUID
        ? crypto.randomUUID()
        : `pay_${Date.now()}_${Math.random().toString(16).slice(2)}`;
      const orderResponse = await api.post(
        "/payment/create-order",
        { planId: plan.id, idempotencyKey },
        { headers: { "Idempotency-Key": idempotencyKey } }
      );
      const order = orderResponse.data;
      setPendingOrder(order);

      const scriptReady = await loadRazorpayScript();
      if (!scriptReady || !window.Razorpay) throw new Error("Secure checkout could not be loaded");

      const checkout = new window.Razorpay({
        key: keyId,
        order_id: order.orderId,
        amount: Math.round(Number(order.amount) * 100),
        currency: order.currency || "INR",
        name: "Matrimony Membership",
        description: `${plan.name} · ${plan.durationMonths} months`,
        prefill: {
          email: user?.email || "",
          contact: user?.phone || "",
        },
        modal: {
          ondismiss: () => {
            toast.info("Checkout closed. If money was debited, use Check payment status instead of paying again.");
          },
        },
        handler: async (response) => {
          try {
            await api.post("/payment/verify", {
              paymentId: response.razorpay_payment_id,
              orderId: response.razorpay_order_id,
              signature: response.razorpay_signature,
              planId: plan.id,
            });
            await refreshEntitlements();
            setPendingOrder(null);
            toast.success(`${plan.name} membership activated.`);
          } catch (verifyError) {
            toast.error(verifyError?.response?.data?.error || "Payment was received but confirmation is pending. Check payment status before retrying.");
          }
        },
      });
      checkout.open();
    } catch (requestError) {
      setError(requestError?.response?.data?.error || requestError?.message || "Checkout could not start.");
    } finally {
      setCheckoutPlan(null);
    }
  };

  return (
    <div style={{ maxWidth: 980, margin: "0 auto", display: "grid", gap: "1rem" }}>
      <header className="panel" style={{ padding: "1.2rem", textAlign: "center" }}>
        <p className="section-label" style={{ marginBottom: "0.25rem" }}>Membership</p>
        <h1 style={{ margin: 0, fontFamily: "var(--font-display)", fontSize: "2rem" }}>Transparent plans, exact limits</h1>
        <p style={{ margin: "0.5rem auto 0", maxWidth: 720, color: "var(--ink-muted)", lineHeight: 1.55 }}>
          Your plan changes available tools and quotas. It never fabricates matches, changes compatibility, or overrides another member’s contact or photo privacy.
        </p>
        <div style={{ marginTop: "0.75rem" }}><span className="chip chip-support">Current plan: {currentLabel}</span></div>
      </header>

      {error && <div className="panel" style={{ padding: "0.85rem", borderColor: "#efb4a9" }}>{error}</div>}

      {pendingOrder && (
        <div className="panel" style={{ padding: "0.9rem", display: "flex", justifyContent: "space-between", gap: "0.7rem", alignItems: "center", flexWrap: "wrap" }}>
          <div>
            <strong>Payment confirmation available</strong>
            <p style={{ margin: "0.2rem 0 0", color: "var(--ink-muted)", fontSize: "0.82rem" }}>Order {pendingOrder.orderId}. If your connection dropped after payment, check status instead of creating another order.</p>
          </div>
          <button className="button button-secondary" type="button" onClick={() => reconcileOrder(pendingOrder.orderId)}>Check payment status</button>
        </div>
      )}

      {loading ? (
        <div className="panel" style={{ padding: "2rem", textAlign: "center" }}>Loading authoritative plan details…</div>
      ) : plans.length === 0 ? (
        <div className="panel" style={{ padding: "2rem", textAlign: "center" }}>No purchasable plans are available right now. You can continue using the free plan.</div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(280px,1fr))", gap: "0.8rem" }}>
          {plans.map((plan) => (
            <article key={plan.id} className="panel panel-hover" style={{ padding: "1rem", display: "grid", gap: "0.75rem" }}>
              <div>
                <p className="section-label" style={{ marginBottom: "0.2rem" }}>{plan.name}</p>
                <div style={{ display: "flex", alignItems: "baseline", gap: "0.35rem" }}>
                  <strong style={{ fontSize: "2rem" }}>₹{Number(plan.price).toLocaleString("en-IN")}</strong>
                  <span style={{ color: "var(--ink-muted)" }}>/ {plan.durationMonths} months</span>
                </div>
                <p style={{ margin: "0.35rem 0 0", color: "var(--ink-muted)", fontSize: "0.78rem" }}>{plan.renewal}</p>
              </div>

              <div style={{ display: "grid", gap: "0.35rem" }}>
                {entitlementRows(plan.entitlements).map(([label, value]) => (
                  <div key={label} style={{ display: "flex", justifyContent: "space-between", gap: "0.7rem", borderBottom: "1px solid var(--line)", padding: "0.38rem 0" }}>
                    <span style={{ color: "var(--ink-muted)", fontSize: "0.8rem" }}>{label}</span>
                    <strong style={{ textAlign: "right", fontSize: "0.8rem" }}>{String(value ?? "Not included")}</strong>
                  </div>
                ))}
              </div>

              <div className="panel" style={{ padding: "0.55rem", background: "#faf8f4" }}>
                <p style={{ margin: 0, fontSize: "0.75rem", lineHeight: 1.45 }}>{plan.contactPrivacy}</p>
                <p style={{ margin: "0.28rem 0 0", fontSize: "0.75rem", color: "var(--ink-muted)" }}>{plan.taxes}</p>
              </div>

              <button className="button button-primary" type="button" disabled={checkoutPlan === plan.id} onClick={() => handlePurchase(plan)}>
                {checkoutPlan === plan.id ? "Opening secure checkout…" : `Choose ${plan.name}`}
              </button>
            </article>
          ))}
        </div>
      )}

      <div className="panel" style={{ padding: "0.9rem" }}>
        <strong>Billing principles</strong>
        <p style={{ margin: "0.35rem 0 0", color: "var(--ink-muted)", lineHeight: 1.55, fontSize: "0.82rem" }}>
          Critical plan limits come from the backend entitlement service, not marketing copy. No fake discounts, hidden “unlimited” caps, automatic contact disclosure, or demo-mode purchase success is shown here.
        </p>
      </div>

      <div style={{ textAlign: "center", fontSize: "0.85rem" }}>
        <Link href="/matches" className="button button-secondary">Continue with current plan</Link>
      </div>
    </div>
  );
}
