"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "react-toastify";
import api from "../../../services/api";

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
const TIER_GRADIENT = {
  silver: "from-slate-500 to-slate-700",
  gold: "from-amber-500 to-yellow-600",
  platinum: "from-indigo-600 to-blue-700",
  till_marriage: "from-rose-600 to-pink-700",
};

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
    <div className="max-w-5xl mx-auto">
      <div className="text-center mb-8">
        <h1 className="text-3xl md:text-4xl font-extrabold text-gray-900 mb-3">Choose Your Plan</h1>
        <p className="text-gray-500 max-w-2xl mx-auto">
          Plans are loaded from live catalog IDs and charged with region-aware pricing.
        </p>
      </div>

      <div className="bg-white rounded-2xl border border-gray-200 p-4 mb-6 grid md:grid-cols-3 gap-3">
        <div>
          <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-1">Region</label>
          <select
            value={region}
            onChange={(event) => setRegion(event.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
          >
            <option value="IN">India (INR)</option>
            <option value="US">US (USD)</option>
          </select>
        </div>
        <div className="md:col-span-2">
          <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-1">Coupon Code</label>
          <input
            value={couponCode}
            onChange={(event) => setCouponCode(event.target.value.toUpperCase())}
            placeholder="WELCOME10 or WINBACK20"
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
          />
        </div>
      </div>

      <div className="grid md:grid-cols-2 xl:grid-cols-4 gap-5 mb-8">
        {orderedPlans.map((plan) => {
          const isPopular = plan.tier === "gold";
          const gradient = TIER_GRADIENT[plan.tier] || "from-slate-600 to-slate-800";
          const isLoading = loadingPlans || processingPlanId === plan.id;
          return (
            <article
              key={plan.id}
              className={`relative bg-white rounded-2xl border overflow-hidden shadow-sm ${isPopular ? "border-amber-400 ring-2 ring-amber-300" : "border-gray-200"
                }`}
            >
              {isPopular && (
                <div className="bg-gradient-to-r from-amber-500 to-yellow-500 text-white text-xs font-bold text-center py-1.5">
                  MOST POPULAR
                </div>
              )}

              <div className={`bg-gradient-to-br ${gradient} p-5 text-white`}>
                <p className="text-xs font-semibold tracking-wide opacity-90">{plan.id}</p>
                <h2 className="text-2xl font-extrabold mt-1">{plan.name}</h2>
                <p className="text-3xl font-extrabold mt-2">{formatAmount(plan.amount, plan.currency)}</p>
                <p className="text-white/85 text-sm mt-1">for {plan.durationMonths} months</p>
              </div>

              <div className="p-5 flex flex-col gap-4">
                <ul className="space-y-2 min-h-[128px]">
                  {plan.features.map((feature) => (
                    <li key={feature} className="text-sm text-gray-700 flex gap-2">
                      <span className="text-green-600 font-bold">+</span>
                      <span>{feature}</span>
                    </li>
                  ))}
                </ul>

                <button
                  type="button"
                  onClick={() => handlePurchase(plan)}
                  disabled={isLoading}
                  className="w-full py-2.5 rounded-xl font-semibold text-sm bg-gradient-to-r from-pink-600 to-red-600 text-white disabled:opacity-60"
                >
                  {processingPlanId === plan.id ? "Processing..." : `Buy ${plan.name}`}
                </button>
              </div>
            </article>
          );
        })}
      </div>

      <div className="text-center text-sm text-gray-500">
        Prefer free browsing for now?{" "}
        <Link href="/matches" className="text-pink-600 font-bold hover:underline">
          Continue with basic access
        </Link>
      </div>
    </div>
  );
}
