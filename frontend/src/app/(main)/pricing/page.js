"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "react-toastify";
import api from "../../../services/api";
import PageHero from "../../../components/PageHero";

const FALLBACK_PLANS = [
  {
    id: "SILVER_3M",
    tier: "silver",
    name: "Silver",
    durationMonths: 3,
    amount: 1499,
    currency: "INR",
    features: ["Unlimited interests", "Saved searches", "See who viewed your profile"],
  },
  {
    id: "GOLD_3M",
    tier: "gold",
    name: "Gold",
    durationMonths: 3,
    amount: 2999,
    currency: "INR",
    features: ["Unlimited messages", "Voice calls", "Advanced filters"],
  },
  {
    id: "PLATINUM_6M",
    tier: "platinum",
    name: "Platinum",
    durationMonths: 6,
    amount: 4999,
    currency: "INR",
    features: ["Priority visibility", "Voice + video calls", "Top placement in discovery"],
  },
  {
    id: "TILL_MARRIAGE",
    tier: "till_marriage",
    name: "Till Marriage",
    durationMonths: 24,
    amount: 12999,
    currency: "INR",
    features: ["All Platinum features", "Extended support", "Long-term membership"],
  },
];

const TIER_ORDER = ["silver", "gold", "platinum", "till_marriage"];
const ASSISTED_HIGHLIGHTS = [
  {
    title: "Relationship manager",
    copy: "A dedicated expert shortlists profiles, follows up, and coordinates introductions.",
  },
  {
    title: "Privacy-first outreach",
    copy: "Control who can view and contact you while we manage the outreach on your behalf.",
  },
  {
    title: "Priority visibility",
    copy: "Boosted placement in discovery for faster responses and higher intent matches.",
  },
];

const PLAN_COMPARISON = [
  { label: "Contact visibility", silver: false, gold: true, platinum: true, till: true },
  { label: "Unlimited interests", silver: true, gold: true, platinum: true, till: true },
  { label: "Advanced filters", silver: false, gold: true, platinum: true, till: true },
  { label: "Priority placement", silver: false, gold: false, platinum: true, till: true },
  { label: "Relationship manager", silver: false, gold: false, platinum: true, till: true },
  { label: "Family advisor support", silver: false, gold: false, platinum: false, till: true },
];

const PRICING_FAQS = [
  {
    q: "Is there a money-back guarantee?",
    a: "Yes. All plans include a 7-day guarantee. Contact support to request a refund.",
  },
  {
    q: "Can I upgrade later?",
    a: "Yes. You can upgrade at any time without losing matches or conversations.",
  },
  {
    q: "Do you offer assisted matchmaking?",
    a: "Yes. Platinum and Till Marriage tiers include assisted matchmaking support.",
  },
  {
    q: "Is browsing still free?",
    a: "Yes. You can explore matches for free and upgrade when ready.",
  },
];

function normalizePlan(plan) {
  return {
    id: plan.id,
    tier: String(plan.tier || "silver").toLowerCase(),
    name: plan.name || plan.id,
    durationMonths: Number(plan.durationMonths || 0),
    amount: Number(plan.amount || plan.price || 0),
    currency: plan.currency || "INR",
    features: Array.isArray(plan.features) ? plan.features : [],
  };
}

function formatAmount(amount, currency) {
  const safeAmount = Number.isFinite(Number(amount)) ? Number(amount) : 0;
  const safeCurrency = currency || "INR";
  try {
    return new Intl.NumberFormat(undefined, {
      style: "currency",
      currency: safeCurrency,
      maximumFractionDigits: 0,
    }).format(safeAmount);
  } catch {
    return `${safeCurrency} ${safeAmount}`;
  }
}

export default function PricingPage() {
  const router = useRouter();
  const [plans, setPlans] = useState(FALLBACK_PLANS);
  const [region, setRegion] = useState("IN");
  const [couponCode, setCouponCode] = useState("");
  const [loadingPlans, setLoadingPlans] = useState(true);
  const [processingPlanId, setProcessingPlanId] = useState(null);

  useEffect(() => {
    let mounted = true;
    const loadPlans = async () => {
      setLoadingPlans(true);
      try {
        const response = await api.get("/payment/plans", { params: { region } });
        const apiPlans = Array.isArray(response?.data) ? response.data.map(normalizePlan) : [];
        if (mounted && apiPlans.length > 0) {
          setPlans(apiPlans);
        } else if (mounted) {
          setPlans(FALLBACK_PLANS);
        }
      } catch {
        if (mounted) setPlans(FALLBACK_PLANS);
      } finally {
        if (mounted) setLoadingPlans(false);
      }
    };

    loadPlans();
    return () => {
      mounted = false;
    };
  }, [region]);

  const orderedPlans = useMemo(() => {
    return [...plans]
      .map(normalizePlan)
      .sort((a, b) => TIER_ORDER.indexOf(a.tier) - TIER_ORDER.indexOf(b.tier));
  }, [plans]);

  const handlePurchase = async (plan) => {
    setProcessingPlanId(plan.id);
    try {
      const orderResponse = await api.post("/payment/create-order", {
        planId: plan.id,
        couponCode: couponCode.trim() || undefined,
        region,
      });
      const order = orderResponse?.data || {};
      const finalAmount = Number(order.amount || plan.amount || 0);
      const status = order.couponStatus || "none";
      const couponText = order.coupon
        ? `Coupon ${order.coupon} applied.`
        : status !== "none"
          ? `Coupon status: ${status}.`
          : "";
      const confirmText = [
        `Proceed with ${plan.name} (${plan.id})?`,
        `Amount: ${formatAmount(finalAmount, order.currency || plan.currency)}`,
        couponText,
      ]
        .filter(Boolean)
        .join("\n");

      const proceed = confirm(confirmText);
      if (!proceed) return;

      const verifyResponse = await api.post("/payment/verify", {
        paymentId: `pay_mock_${plan.id}_${Date.now()}`,
        orderId: order.orderId,
        planId: plan.id,
      });

      if (verifyResponse?.data?.success) {
        toast.success(`Membership activated: ${plan.name}`);
        router.push("/matches");
        return;
      }

      toast.error("Payment verification failed.");
    } catch (error) {
      const message = error?.response?.data?.error || "Unable to start payment.";
      toast.error(message);
    } finally {
      setProcessingPlanId(null);
    }
  };

  return (
    <div className="pricing-shell pricing-shell-heirloom">
      <PageHero
        eyebrow="Membership"
        title="Choose the plan that matches your pace"
        copy="Plans are loaded from the live catalog and priced by region. Upgrade anytime without losing your progress."
        className="pricing-hero"
      />

      <section className="status-banner">
        Upgrade unlocks contact visibility, priority placement, and faster introductions.
      </section>

      <section className="panel pricing-callout">
        <div>
          <p className="section-label">Guarantee</p>
          <h2 className="section-title section-title-xs">7-day money-back promise</h2>
          <p className="section-copy">
            Try any premium plan with full access. If you are not satisfied, request a refund within 7 days.
          </p>
        </div>
        <div className="pricing-callout-actions">
          <Link href="/help" className="button button-secondary">
            Talk to support
          </Link>
          <Link href="/matches" className="button button-primary">
            Browse free matches
          </Link>
        </div>
      </section>

      <section className="panel pricing-control-panel">
        <div className="form-grid-2">
          <div>
            <label className="form-label" htmlFor="pricing-region">Region</label>
            <select
              id="pricing-region"
              value={region}
              onChange={(event) => setRegion(event.target.value)}
              className="form-input"
            >
              <option value="IN">India (INR)</option>
              <option value="US">United States (USD)</option>
            </select>
          </div>
          <div>
            <label className="form-label" htmlFor="pricing-coupon">Coupon Code</label>
            <input
              id="pricing-coupon"
              value={couponCode}
              onChange={(event) => setCouponCode(event.target.value.toUpperCase())}
              placeholder="WELCOME10 or WINBACK20"
              className="form-input"
            />
          </div>
        </div>
      </section>

      <section className="pricing-grid">
        {orderedPlans.map((plan) => {
          const isPopular = plan.tier === "gold";
          const isLoading = loadingPlans || processingPlanId === plan.id;
          const tierClass = `plan-card plan-tier-${plan.tier}`;
          return (
            <article key={plan.id} className={tierClass}>
              <div className="plan-card-head">
                <p className="plan-card-id">{plan.id}</p>
                <h2 className="plan-card-title">{plan.name}</h2>
                <p className="plan-card-price">
                  {formatAmount(plan.amount, plan.currency)}
                </p>
                <p className="plan-card-duration">
                  {plan.durationMonths} months access
                </p>
              </div>

              <div className="plan-card-body">
                {isPopular && <span className="chip chip-brand plan-popular-chip">Most popular</span>}
                <ul className="plan-feature-list">
                  {plan.features.map((feature) => (
                    <li key={feature} className="plan-feature">
                      <span>+</span>
                      <span>{feature}</span>
                    </li>
                  ))}
                </ul>

                <button
                  type="button"
                  onClick={() => handlePurchase(plan)}
                  disabled={isLoading}
                  className="button button-primary cta-full"
                >
                  {processingPlanId === plan.id ? "Processing..." : `Upgrade to ${plan.name}`}
                </button>
              </div>
            </article>
          );
        })}
      </section>

      <section className="pricing-assist">
        <div className="pricing-assist-head">
          <div>
            <p className="section-label">Assisted matchmaking</p>
            <h2 className="section-title section-title-xs">When you want expert guidance</h2>
            <p className="section-copy">
              Premium tiers include hands-on support for shortlisting, follow-ups, and family coordination.
            </p>
          </div>
          <Link href="/help" className="button button-primary">
            Request an advisor call
          </Link>
        </div>
        <div className="pricing-assist-grid">
          {ASSISTED_HIGHLIGHTS.map((item) => (
            <article key={item.title} className="panel pricing-assist-card">
              <h3>{item.title}</h3>
              <p>{item.copy}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="pricing-compare">
        <p className="section-label">Plan comparison</p>
        <h2 className="section-title section-title-xs">Compare core benefits</h2>
        <div className="panel pricing-table-wrap">
          <table className="pricing-table">
            <thead>
              <tr>
                <th>Feature</th>
                <th>Silver</th>
                <th>Gold</th>
                <th>Platinum</th>
                <th>Till Marriage</th>
              </tr>
            </thead>
            <tbody>
              {PLAN_COMPARISON.map((row) => (
                <tr key={row.label}>
                  <td>{row.label}</td>
                  <td>{row.silver ? "Yes" : "-"}</td>
                  <td>{row.gold ? "Yes" : "-"}</td>
                  <td>{row.platinum ? "Yes" : "-"}</td>
                  <td>{row.till ? "Yes" : "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="pricing-faq">
        <p className="section-label">FAQ</p>
        <h2 className="section-title section-title-xs">Questions about pricing</h2>
        <div className="pricing-faq-grid">
          {PRICING_FAQS.map((item) => (
            <article key={item.q} className="panel pricing-faq-card">
              <h3>{item.q}</h3>
              <p>{item.a}</p>
            </article>
          ))}
        </div>
      </section>

      <p className="form-note pricing-footnote">
        Prefer free browsing for now?{" "}
        <Link href="/matches">
          Continue with basic access
        </Link>
        .
      </p>
    </div>
  );
}
