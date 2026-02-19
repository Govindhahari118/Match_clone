"use client";

import { useState, useEffect } from "react";
import api from "../../../services/api";
import { toast } from "react-toastify";
import { useRouter } from "next/navigation";
import Link from "next/link";

const PLANS_STATIC = [
    {
        id: "silver",
        name: "Silver",
        price: 999,
        originalPrice: 1499,
        durationMonths: 3,
        color: "from-gray-400 to-gray-600",
        features: [
            "View 50 Contact Numbers / Month",
            "Send Unlimited Interests",
            "Chat with Mutual Matches",
            "Priority in Search Results",
            "Ad-Free Experience",
        ],
    },
    {
        id: "gold",
        name: "Gold",
        price: 1799,
        originalPrice: 2499,
        durationMonths: 6,
        popular: true,
        color: "from-yellow-500 to-amber-600",
        features: [
            "View 200 Contact Numbers / Month",
            "Send Unlimited Interests",
            "Unlimited Chat",
            "Top Priority in Search",
            "WhatsApp & Video Support",
            "Highlighted Profile Badge",
            "AI Match Score Reports",
        ],
    },
    {
        id: "platinum",
        name: "Platinum",
        price: 2999,
        originalPrice: 4499,
        durationMonths: 12,
        color: "from-purple-600 to-indigo-700",
        features: [
            "Unlimited Contact Numbers",
            "Send Unlimited Interests",
            "Unlimited Chat",
            "#1 Search Ranking",
            "Dedicated Relationship Manager",
            "Profile Photo Verification",
            "AI Match Score Reports",
            "Video Calling with Matches",
            "Profile Makeover Service",
        ],
    },
];

const COMPARISON = [
    { feature: "Contact Numbers", silver: "50/month", gold: "200/month", platinum: "Unlimited" },
    { feature: "Interests", silver: "Unlimited", gold: "Unlimited", platinum: "Unlimited" },
    { feature: "Chat", silver: "Mutual only", gold: "Unlimited", platinum: "Unlimited" },
    { feature: "Search Ranking", silver: "Priority", gold: "Top Priority", platinum: "#1 Ranked" },
    { feature: "Video Calling", silver: "✗", gold: "✗", platinum: "✓" },
    { feature: "Relationship Manager", silver: "✗", gold: "✗", platinum: "✓" },
    { feature: "AI Match Reports", silver: "✗", gold: "✓", platinum: "✓" },
    { feature: "Profile Badge", silver: "✗", gold: "✓", platinum: "✓" },
];

export default function PricingPage() {
    const [plans, setPlans] = useState(PLANS_STATIC);
    const [loading, setLoading] = useState(false);
    const [showComparison, setShowComparison] = useState(false);
    const router = useRouter();

    useEffect(() => {
        const fetchFromAPI = async () => {
            try {
                const res = await api.get("/payment/plans");
                if (res.data?.length) setPlans(res.data);
            } catch { }
        };
        fetchFromAPI();
    }, []);

    const handlePurchase = async (plan) => {
        try {
            const orderRes = await api.post("/payment/create-order", { planId: plan.id });
            const order = orderRes.data;
            const isConfirmed = confirm(`Proceed to pay ₹${plan.price} for ${plan.name} plan (${plan.durationMonths} months)?`);
            if (isConfirmed) {
                const verifyRes = await api.post("/payment/verify", {
                    paymentId: `pay_mock_${Date.now()}`,
                    orderId: order.orderId || `ord_mock`,
                    planId: plan.id,
                });
                if (verifyRes.data.success) {
                    toast.success(`🎉 Welcome to ${plan.name} Membership!`);
                    router.push("/profile");
                }
            }
        } catch {
            // Demo mode
            toast.success(`🎉 Welcome to ${plan.name} Membership! (Demo Mode)`);
            router.push("/matches");
        }
    };

    return (
        <div className="max-w-5xl mx-auto">
            {/* Header */}
            <div className="text-center mb-10">
                <div className="inline-flex items-center gap-2 bg-yellow-50 border border-yellow-200 text-yellow-800 px-4 py-1.5 rounded-full text-sm font-semibold mb-4">
                    👑 Limited Time Offer — Up to 40% Off
                </div>
                <h1 className="text-3xl md:text-4xl font-extrabold text-gray-900 mb-3">
                    Choose Your Plan
                </h1>
                <p className="text-gray-500 max-w-2xl mx-auto">
                    Upgrade to unlock contact numbers, video calling, and priority matching. All plans include a 7-day money-back guarantee.
                </p>
            </div>

            {/* Plans */}
            <div className="grid md:grid-cols-3 gap-6 mb-8">
                {plans.map((plan) => (
                    <div
                        key={plan.id || plan.name}
                        className={`relative bg-white rounded-2xl border overflow-hidden shadow-sm hover:shadow-xl transition-all duration-300 flex flex-col ${plan.popular ? "border-yellow-400 ring-2 ring-yellow-300 scale-105" : "border-gray-200"
                            }`}
                    >
                        {plan.popular && (
                            <div className="bg-gradient-to-r from-yellow-500 to-amber-500 text-white text-xs font-bold text-center py-1.5 tracking-wider uppercase">
                                ⭐ Most Popular
                            </div>
                        )}

                        <div className={`bg-gradient-to-br ${plan.color || "from-gray-500 to-gray-700"} p-6 text-white`}>
                            <h2 className="text-2xl font-extrabold mb-1">{plan.name}</h2>
                            <div className="flex items-baseline gap-2">
                                <span className="text-4xl font-extrabold">₹{plan.price || plan.price}</span>
                                {plan.originalPrice && <span className="text-sm line-through opacity-70">₹{plan.originalPrice}</span>}
                            </div>
                            <p className="text-white/80 text-sm mt-1">for {plan.durationMonths} months</p>
                            {plan.originalPrice && (
                                <div className="mt-2 text-xs bg-white/20 px-2 py-0.5 rounded-full inline-block font-semibold">
                                    Save ₹{plan.originalPrice - plan.price}
                                </div>
                            )}
                        </div>

                        <div className="p-6 flex-1 flex flex-col">
                            <ul className="space-y-3 flex-1 mb-6">
                                {(plan.features || []).map((feature, i) => (
                                    <li key={i} className="flex items-start gap-2.5 text-sm text-gray-700">
                                        <span className="text-green-500 font-bold flex-shrink-0 mt-0.5">✓</span>
                                        {feature}
                                    </li>
                                ))}
                            </ul>

                            <button
                                onClick={() => handlePurchase(plan)}
                                disabled={loading}
                                className={`w-full py-3 rounded-xl font-bold text-sm transition-all ${plan.popular
                                        ? "bg-gradient-to-r from-yellow-500 to-amber-500 text-white hover:shadow-lg hover:shadow-yellow-200"
                                        : "bg-gradient-to-r from-pink-600 to-red-600 text-white hover:shadow-lg hover:shadow-pink-200"
                                    } disabled:opacity-50`}
                            >
                                Get {plan.name} Plan
                            </button>
                        </div>
                    </div>
                ))}
            </div>

            {/* Comparison Table */}
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden mb-8">
                <button
                    onClick={() => setShowComparison(!showComparison)}
                    className="w-full px-6 py-4 flex justify-between items-center hover:bg-gray-50 transition"
                >
                    <span className="font-bold text-gray-900">Compare All Plans</span>
                    <span className="text-gray-400 text-xl">{showComparison ? "−" : "+"}</span>
                </button>
                {showComparison && (
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="bg-gray-50">
                                    <th className="px-6 py-3 text-left font-bold text-gray-600">Feature</th>
                                    <th className="px-4 py-3 text-center font-bold text-gray-600">Silver</th>
                                    <th className="px-4 py-3 text-center font-bold text-yellow-600">Gold ⭐</th>
                                    <th className="px-4 py-3 text-center font-bold text-purple-600">Platinum</th>
                                </tr>
                            </thead>
                            <tbody>
                                {COMPARISON.map((row, i) => (
                                    <tr key={i} className="border-t border-gray-50">
                                        <td className="px-6 py-3 text-gray-700 font-medium">{row.feature}</td>
                                        <td className="px-4 py-3 text-center text-gray-500">{row.silver}</td>
                                        <td className="px-4 py-3 text-center text-yellow-700 font-semibold">{row.gold}</td>
                                        <td className="px-4 py-3 text-center text-purple-700 font-semibold">{row.platinum}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* Guarantees */}
            <div className="grid sm:grid-cols-3 gap-4 mb-8">
                {[
                    { icon: "🔒", title: "Secure Payment", desc: "Razorpay / SSL encrypted checkout" },
                    { icon: "↩️", title: "7-Day Refund", desc: "Not satisfied? Get a full refund" },
                    { icon: "📞", title: "24/7 Support", desc: "Dedicated team ready to help you" },
                ].map(item => (
                    <div key={item.title} className="bg-gray-50 rounded-xl p-4 flex items-center gap-3 border border-gray-100">
                        <div className="text-2xl">{item.icon}</div>
                        <div>
                            <p className="font-bold text-gray-800 text-sm">{item.title}</p>
                            <p className="text-xs text-gray-500">{item.desc}</p>
                        </div>
                    </div>
                ))}
            </div>

            {/* Free plan reminder */}
            <div className="text-center text-sm text-gray-500">
                Want to use the free version? <Link href="/matches" className="text-pink-600 font-bold hover:underline">Continue with basic plan</Link>
            </div>
        </div>
    );
}
