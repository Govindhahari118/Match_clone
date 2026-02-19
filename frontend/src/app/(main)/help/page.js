"use client";

import { useState } from "react";
import Link from "next/link";

const FAQS = [
    {
        category: "Getting Started",
        icon: "🚀",
        items: [
            { q: "How do I create a profile?", a: "Click 'Create Free Profile', fill in your basic details, upload a photo, and complete your preferences. The whole process takes under 5 minutes." },
            { q: "Is registration really free?", a: "Yes, creating a profile and browsing are free. Premium features like viewing contact numbers and video calling require a subscription." },
            { q: "How do I verify my profile?", a: "Go to Profile → Verification, upload a government-issued ID (Aadhaar, PAN, Passport). Verification usually completes within 24 hours." },
        ],
    },
    {
        category: "Matching & Search",
        icon: "💖",
        items: [
            { q: "How does AI matching work?", a: "Our AI analyses 50+ data points including personality traits, values, lifestyle choices, and preferences to compute a compatibility percentage for each profile." },
            { q: "How do I send an interest?", a: "Click 'Send Interest' on any profile card. If they accept, you'll both be able to chat and view contact info." },
            { q: "What is a mutual match?", a: "A mutual match happens when both users have accepted each other's interest. Mutual matches can chat freely and view contact numbers." },
        ],
    },
    {
        category: "Premium Plans",
        icon: "👑",
        items: [
            { q: "What's included in premium plans?", a: "Premium includes contact number access, priority search placement, AI compatibility reports, and for Platinum a dedicated relationship manager." },
            { q: "Can I get a refund?", a: "Yes, we offer a 7-day money-back guarantee on all plans. Contact support within 7 days of purchase for a full refund." },
            { q: "How do I upgrade my plan?", a: "Go to Pricing in the navigation or your profile menu, choose a plan, and complete the payment via Razorpay, UPI, or card." },
        ],
    },
    {
        category: "Privacy & Safety",
        icon: "🔒",
        items: [
            { q: "Who can see my phone number?", a: "Your phone number is hidden by default. It's only visible to users you've mutually matched with on premium plans." },
            { q: "How do I report someone?", a: "On any profile, tap the '⋮' menu and select 'Report Profile'. Our safety team reviews all reports within 24 hours." },
            { q: "Is my data safe?", a: "We use bank-grade SSL encryption and never sell personal data to third parties. Your privacy is our top priority." },
        ],
    },
];

const CATEGORIES = [
    { title: "Getting Started", icon: "🚀", color: "#e11d48", bg: "#fff1f2", href: "#getting-started" },
    { title: "Matching & AI", icon: "🤖", color: "#8b5cf6", bg: "#faf5ff", href: "#matching" },
    { title: "Premium Plans", icon: "👑", color: "#d97706", bg: "#fffbeb", href: "#premium" },
    { title: "Privacy & Safety", icon: "🔒", color: "#0ea5e9", bg: "#f0f9ff", href: "#privacy" },
    { title: "Technical Issues", icon: "🔧", color: "#10b981", bg: "#f0fdf4", href: "#tech" },
    { title: "Billing", icon: "💳", color: "#f97316", bg: "#fff7ed", href: "#billing" },
];

export default function HelpPage() {
    const [search, setSearch] = useState("");
    const [openFaq, setOpenFaq] = useState(null);
    const [activeCategory, setActiveCategory] = useState(null);

    const filteredFaqs = FAQS.map(section => ({
        ...section,
        items: section.items.filter(
            item =>
                item.q.toLowerCase().includes(search.toLowerCase()) ||
                item.a.toLowerCase().includes(search.toLowerCase())
        ),
    })).filter(section => section.items.length > 0 && (!activeCategory || section.category === activeCategory));

    return (
        <div>
            {/* Hero */}
            <div style={{
                background: "linear-gradient(135deg, #1a0533, #4a0070, #8b0037)",
                borderRadius: 28, padding: "3rem 2rem", marginBottom: "2rem",
                textAlign: "center", position: "relative", overflow: "hidden",
            }}>
                <div style={{ position: "absolute", top: -40, left: -40, width: 200, height: 200, background: "rgba(139,92,246,0.15)", borderRadius: "50%", filter: "blur(40px)" }}></div>
                <div style={{ position: "absolute", bottom: -40, right: -40, width: 200, height: 200, background: "rgba(225,29,72,0.15)", borderRadius: "50%", filter: "blur(40px)" }}></div>
                <div style={{ position: "relative" }}>
                    <div style={{ fontSize: 48, marginBottom: "1rem" }}>💬</div>
                    <h1 style={{ fontSize: 28, fontWeight: 900, color: "white", marginBottom: 8 }}>How can we help you?</h1>
                    <p style={{ color: "rgba(255,255,255,0.65)", fontSize: 14, marginBottom: "1.75rem" }}>Search our knowledge base or browse categories below</p>
                    <div style={{ maxWidth: 500, margin: "0 auto", position: "relative" }}>
                        <input
                            type="text"
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                            placeholder="Search for help... e.g. 'how to verify profile'"
                            style={{
                                width: "100%", padding: "14px 20px 14px 50px",
                                borderRadius: 16, border: "none",
                                background: "rgba(255,255,255,0.95)",
                                fontSize: 14, outline: "none", fontFamily: "inherit",
                                boxShadow: "0 8px 30px rgba(0,0,0,0.2)",
                            }}
                        />
                        <span style={{ position: "absolute", left: 16, top: "50%", transform: "translateY(-50%)", fontSize: 18 }}>🔍</span>
                    </div>
                </div>
            </div>

            {/* Quick Categories */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(160px, 1fr))", gap: "0.875rem", marginBottom: "2rem" }}>
                {CATEGORIES.map(cat => (
                    <button
                        key={cat.title}
                        onClick={() => setActiveCategory(activeCategory === cat.title ? null : cat.title)}
                        style={{
                            display: "flex", flexDirection: "column", gap: 10,
                            padding: "1.25rem", borderRadius: 18, textAlign: "left",
                            background: activeCategory === cat.title ? cat.color : cat.bg,
                            border: `1.5px solid ${cat.color}22`,
                            cursor: "pointer",
                            transform: activeCategory === cat.title ? "scale(1.02)" : "scale(1)",
                            boxShadow: activeCategory === cat.title ? `0 8px 24px ${cat.color}33` : "none",
                            transition: "all 0.2s",
                        }}
                    >
                        <span style={{ fontSize: 28 }}>{cat.icon}</span>
                        <span style={{ fontSize: 13, fontWeight: 700, color: activeCategory === cat.title ? "white" : cat.color }}>{cat.title}</span>
                    </button>
                ))}
            </div>

            {/* FAQ Section */}
            <div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
                    <h2 style={{ fontSize: 18, fontWeight: 800, color: "#111827" }}>Frequently Asked Questions</h2>
                    {(activeCategory || search) && (
                        <button onClick={() => { setActiveCategory(null); setSearch(""); }} style={{ fontSize: 12, fontWeight: 700, color: "#e11d48", background: "none", border: "none", cursor: "pointer" }}>
                            Clear Filter ✕
                        </button>
                    )}
                </div>

                {filteredFaqs.length === 0 ? (
                    <div style={{ textAlign: "center", padding: "4rem", background: "white", borderRadius: 20, border: "1.5px solid #f1f5f9" }}>
                        <div style={{ fontSize: 48, marginBottom: "1rem" }}>🤔</div>
                        <h3 style={{ fontWeight: 800, color: "#111827", marginBottom: 8 }}>No results found</h3>
                        <p style={{ color: "#64748b", fontSize: 14 }}>Try different keywords or contact support below</p>
                    </div>
                ) : (
                    <div style={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}>
                        {filteredFaqs.map((section) => (
                            <div key={section.category} style={{ background: "white", borderRadius: 20, border: "1.5px solid #f1f5f9", overflow: "hidden", boxShadow: "0 2px 10px rgba(0,0,0,0.04)" }}>
                                <div style={{ padding: "14px 20px", borderBottom: "1px solid #f8fafc", display: "flex", alignItems: "center", gap: 8 }}>
                                    <span style={{ fontSize: 20 }}>{section.icon}</span>
                                    <h3 style={{ fontWeight: 800, fontSize: 15, color: "#111827" }}>{section.category}</h3>
                                    <span style={{ marginLeft: "auto", fontSize: 11, fontWeight: 700, background: "#f1f5f9", color: "#64748b", padding: "2px 8px", borderRadius: 99 }}>{section.items.length} articles</span>
                                </div>
                                {section.items.map((item, idx) => {
                                    const key = `${section.category}-${idx}`;
                                    const isOpen = openFaq === key;
                                    return (
                                        <div key={idx} style={{ borderBottom: idx < section.items.length - 1 ? "1px solid #f8fafc" : "none" }}>
                                            <button
                                                onClick={() => setOpenFaq(isOpen ? null : key)}
                                                style={{
                                                    width: "100%", display: "flex", justifyContent: "space-between", alignItems: "center",
                                                    padding: "14px 20px", background: isOpen ? "#fafafa" : "none", border: "none",
                                                    cursor: "pointer", textAlign: "left", gap: 12,
                                                    transition: "background 0.2s",
                                                }}
                                            >
                                                <span style={{ fontSize: 14, fontWeight: 700, color: isOpen ? "#e11d48" : "#1e293b" }}>{item.q}</span>
                                                <span style={{
                                                    width: 24, height: 24, borderRadius: "50%", flexShrink: 0,
                                                    background: isOpen ? "#fce7f3" : "#f1f5f9", color: isOpen ? "#e11d48" : "#64748b",
                                                    display: "flex", alignItems: "center", justifyContent: "center",
                                                    fontSize: 16, fontWeight: 700, transition: "all 0.2s",
                                                    transform: isOpen ? "rotate(45deg)" : "rotate(0)",
                                                }}>+</span>
                                            </button>
                                            {isOpen && (
                                                <div style={{ padding: "0 20px 16px", animation: "fadeIn 0.2s ease" }}>
                                                    <p style={{ fontSize: 14, color: "#64748b", lineHeight: 1.7 }}>{item.a}</p>
                                                </div>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* Contact Support */}
            <div style={{ marginTop: "2rem" }}>
                <h2 style={{ fontSize: 18, fontWeight: 800, color: "#111827", marginBottom: "1rem" }}>Still need help?</h2>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))", gap: "1rem" }}>
                    {[
                        { icon: "💬", title: "Live Chat", desc: "Chat with us in real-time", action: "Start Chat →", color: "#10b981", bg: "#f0fdf4", border: "#86efac" },
                        { icon: "📧", title: "Email Us", desc: "support@matrimonyconnect.com", action: "Send Email →", color: "#0ea5e9", bg: "#f0f9ff", border: "#7dd3fc" },
                        { icon: "📞", title: "Call Us", desc: "Mon–Sat 9AM–8PM IST", action: "1800-XXX-XXXX", color: "#8b5cf6", bg: "#faf5ff", border: "#c4b5fd" },
                        { icon: "🎫", title: "Raise Ticket", desc: "We respond within 24 hours", action: "Open Ticket →", color: "#f97316", bg: "#fff7ed", border: "#fdba74" },
                    ].map(card => (
                        <div key={card.title}
                            style={{ background: card.bg, border: `1.5px solid ${card.border}`, borderRadius: 20, padding: "1.5rem", cursor: "pointer", transition: "all 0.2s" }}
                            onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-3px)"; e.currentTarget.style.boxShadow = `0 8px 24px ${card.border}99`; }}
                            onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "none"; }}
                        >
                            <div style={{ fontSize: 36, marginBottom: 10 }}>{card.icon}</div>
                            <h3 style={{ fontWeight: 800, fontSize: 15, color: "#111827", marginBottom: 4 }}>{card.title}</h3>
                            <p style={{ fontSize: 12, color: "#64748b", marginBottom: "1rem" }}>{card.desc}</p>
                            <span style={{ fontSize: 13, fontWeight: 800, color: card.color }}>{card.action}</span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
