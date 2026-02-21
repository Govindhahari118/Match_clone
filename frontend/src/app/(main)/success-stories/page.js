"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";

const STORIES = [
    {
        id: "s1", coupleNames: "Rahul & Priya", marriageYear: 2024,
        photo1: "https://randomuser.me/api/portraits/men/32.jpg",
        photo2: "https://randomuser.me/api/portraits/women/44.jpg",
        city: "Mumbai", story: "We matched on MatrimonyConnect in January 2024. Our first conversation about travel turned into hours of talking every day. Six months later, I knew she was the one. We got married in December 2024 and couldn't be happier!",
        tag: "❤️ Love Story", days: 45,
    },
    {
        id: "s2", coupleNames: "Arjun & Sneha", marriageYear: 2024,
        photo1: "https://randomuser.me/api/portraits/men/33.jpg",
        photo2: "https://randomuser.me/api/portraits/women/45.jpg",
        city: "Bangalore", story: "As a software engineer, I was always too busy for marriage. My mom set up my profile. I'm so glad she did — Sneha messaged me first and the rest is history. We tied the knot in August and we're expecting our first child!",
        tag: "👨‍👩‍👧 Family First", days: 120,
    },
    {
        id: "s3", coupleNames: "Vikram & Kavya", marriageYear: 2023,
        photo1: "https://randomuser.me/api/portraits/men/34.jpg",
        photo2: "https://randomuser.me/api/portraits/women/46.jpg",
        city: "Hyderabad", story: "I was skeptical of online matrimony sites. But the compatibility score showed us as 94% — and after our first video call, I understood why. Same values, same dreams. We got married in 3 months. Best decision ever.",
        tag: "⚡ Quick Match", days: 90,
    },
    {
        id: "s4", coupleNames: "Aditya & Meera", marriageYear: 2024,
        photo1: "https://randomuser.me/api/portraits/men/35.jpg",
        photo2: "https://randomuser.me/api/portraits/women/47.jpg",
        city: "Delhi", story: "We were both from Rajasthan but living in different cities. MatrimonyConnect's 'Near Me' feature suggested each other since we were both visiting Jaipur that week. Fate brought us to the same city, the app brought us together!",
        tag: "📍 Near Me Match", days: 200,
    },
    {
        id: "s5", coupleNames: "Suresh & Anita", marriageYear: 2023,
        photo1: "https://randomuser.me/api/portraits/men/36.jpg",
        photo2: "https://randomuser.me/api/portraits/women/48.jpg",
        city: "Chennai", story: "I was a divorcee and was not sure if someone would accept me. MatrimonyConnect never judged — I found Anita who was also divorced. We understood each other perfectly. Today we're building a beautiful second chapter together.",
        tag: "🌟 Second Chance", days: 300,
    },
    {
        id: "s6", coupleNames: "Nikhil & Shruti", marriageYear: 2024,
        photo1: "https://randomuser.me/api/portraits/men/37.jpg",
        photo2: "https://randomuser.me/api/portraits/women/49.jpg",
        city: "Pune", story: "NRI match! I was based in Canada and Shruti in Pune. The platform's video call feature made long-distance courting so natural. She visited me in Toronto, and I visited Pune for the engagement. Now she's here with me!",
        tag: "✈️ NRI Love", days: 160,
    },
];

const STATS = [
    { label: "Happy Couples", value: "50,000+", icon: "💕" },
    { label: "Marriages in 2024", value: "12,000+", icon: "💍" },
    { label: "Avg Days to Match", value: "47", icon: "⚡" },
    { label: "States Covered", value: "28", icon: "🗺️" },
];

export default function SuccessStoriesPage() {
    const [expanded, setExpanded] = useState(null);
    const [showForm, setShowForm] = useState(false);
    const [form, setForm] = useState({ name1: "", name2: "", city: "", story: "", email: "" });
    const [submitted, setSubmitted] = useState(false);

    return (
        <div>
            {/* Hero */}
            <div style={{ background: "linear-gradient(135deg, #1a0533, #7c1d6f, #e11d48)", borderRadius: 24, padding: "3rem 2rem", textAlign: "center", marginBottom: "2rem", position: "relative", overflow: "hidden" }}>
                <div style={{ position: "absolute", inset: 0, backgroundImage: "radial-gradient(ellipse at 30% 50%, rgba(139,92,246,0.3) 0%, transparent 60%)" }}></div>
                <div style={{ position: "relative", zIndex: 1 }}>
                    <div style={{ fontSize: 52, marginBottom: "0.75rem" }}>💕</div>
                    <h1 style={{ fontWeight: 900, fontSize: 32, color: "white", marginBottom: "0.75rem" }}>
                        Real Love Stories
                    </h1>
                    <p style={{ fontSize: 15, color: "rgba(255,255,255,0.8)", maxWidth: 500, margin: "0 auto 1.5rem" }}>
                        Thousands of couples found their forever partner on MatrimonyConnect. Here are some of their beautiful journeys.
                    </p>
                    <button
                        onClick={() => setShowForm(true)}
                        style={{ padding: "12px 28px", background: "white", color: "#e11d48", border: "none", borderRadius: 999, fontWeight: 800, fontSize: 14, cursor: "pointer", boxShadow: "0 4px 20px rgba(0,0,0,0.2)" }}
                    >
                        💌 Share Your Story
                    </button>
                </div>
            </div>

            {/* Stats */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "1rem", marginBottom: "2rem" }} className="stats-grid">
                {STATS.map(s => (
                    <div key={s.label} style={{ background: "white", borderRadius: 20, border: "1.5px solid #f1f5f9", padding: "1.25rem", textAlign: "center", boxShadow: "0 2px 10px rgba(0,0,0,0.04)" }}>
                        <div style={{ fontSize: 28, marginBottom: 6 }}>{s.icon}</div>
                        <div style={{ fontWeight: 900, fontSize: 22, color: "#e11d48" }}>{s.value}</div>
                        <div style={{ fontSize: 11, fontWeight: 700, color: "#94a3b8", marginTop: 2, textTransform: "uppercase", letterSpacing: "0.5px" }}>{s.label}</div>
                    </div>
                ))}
            </div>

            {/* Stories grid */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(340px, 1fr))", gap: "1.5rem", marginBottom: "2rem" }}>
                {STORIES.map(story => (
                    <div
                        key={story.id}
                        style={{ background: "white", borderRadius: 24, border: "1.5px solid #f1f5f9", overflow: "hidden", boxShadow: "0 2px 12px rgba(0,0,0,0.05)", transition: "all 0.3s" }}
                        onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-4px)"; e.currentTarget.style.boxShadow = "0 16px 40px rgba(0,0,0,0.1)"; }}
                        onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "0 2px 12px rgba(0,0,0,0.05)"; }}
                    >
                        {/* Photos banner */}
                        <div style={{ position: "relative", height: 160, background: "linear-gradient(135deg, #1a0533, #7c1d6f)" }}>
                            <div style={{ position: "absolute", bottom: -28, left: "50%", transform: "translateX(-50%)", display: "flex", gap: 0 }}>
                                <Image src={story.photo1} alt="" width={64} height={64} unoptimized style={{ width: 64, height: 64, borderRadius: "50%", objectFit: "cover", border: "3px solid white", marginRight: -10, zIndex: 1 }} />
                                <div style={{ width: 28, height: 28, background: "linear-gradient(135deg, #e11d48, #c2185b)", borderRadius: "50%", border: "3px solid white", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, zIndex: 2, alignSelf: "center", marginTop: 18 }}>❤️</div>
                                <Image src={story.photo2} alt="" width={64} height={64} unoptimized style={{ width: 64, height: 64, borderRadius: "50%", objectFit: "cover", border: "3px solid white", marginLeft: -10, zIndex: 1 }} />
                            </div>
                            <div style={{ position: "absolute", top: 12, left: 14 }}>
                                <span style={{ background: "rgba(255,255,255,0.15)", backdropFilter: "blur(8px)", color: "white", fontSize: 11, fontWeight: 700, padding: "3px 10px", borderRadius: 99, border: "1px solid rgba(255,255,255,0.2)" }}>{story.tag}</span>
                            </div>
                            <div style={{ position: "absolute", top: 12, right: 14 }}>
                                <span style={{ background: "rgba(255,255,255,0.15)", backdropFilter: "blur(8px)", color: "white", fontSize: 11, fontWeight: 700, padding: "3px 10px", borderRadius: 99 }}>🕐 {story.days} days</span>
                            </div>
                        </div>

                        {/* Content */}
                        <div style={{ padding: "2.25rem 1.25rem 1.25rem" }}>
                            <h3 style={{ fontWeight: 900, fontSize: 17, color: "#111827", textAlign: "center", marginBottom: 4 }}>{story.coupleNames}</h3>
                            <p style={{ fontSize: 12, color: "#94a3b8", textAlign: "center", marginBottom: "1rem" }}>📍 {story.city} · Married {story.marriageYear}</p>
                            <p style={{ fontSize: 13, color: "#374151", lineHeight: 1.7, display: expanded === story.id ? "block" : "-webkit-box", WebkitLineClamp: 3, WebkitBoxOrient: "vertical", overflow: "hidden" }}>
                                &ldquo;{story.story}&rdquo;
                            </p>
                            <button
                                onClick={() => setExpanded(expanded === story.id ? null : story.id)}
                                style={{ marginTop: 8, background: "none", border: "none", color: "#e11d48", fontWeight: 700, fontSize: 12, cursor: "pointer", padding: 0 }}
                            >
                                {expanded === story.id ? "Read less ↑" : "Read more →"}
                            </button>
                        </div>
                    </div>
                ))}
            </div>

            {/* CTA */}
            <div style={{ background: "linear-gradient(135deg, #fff1f2, #fce7f3)", border: "1.5px solid #fecdd3", borderRadius: 24, padding: "2rem", textAlign: "center" }}>
                <h3 style={{ fontWeight: 900, fontSize: 22, color: "#9f1239", marginBottom: 8 }}>Find Your Story 💕</h3>
                <p style={{ fontSize: 14, color: "#be185d", marginBottom: "1.5rem" }}>Join 50,000+ happy couples. Your perfect match is waiting.</p>
                <Link href="/matches" style={{ display: "inline-block", padding: "12px 32px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", borderRadius: 999, fontWeight: 800, fontSize: 14, textDecoration: "none", boxShadow: "0 4px 20px rgba(225,29,72,0.3)" }}>
                    Browse Matches →
                </Link>
            </div>

            {/* Submit Story Modal */}
            {showForm && (
                <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.5)", backdropFilter: "blur(4px)", zIndex: 9999, display: "flex", alignItems: "center", justifyContent: "center", padding: "1rem" }}>
                    <div style={{ background: "white", borderRadius: 24, padding: "2rem", maxWidth: 500, width: "100%", boxShadow: "0 20px 60px rgba(0,0,0,0.3)" }}>
                        {submitted ? (
                            <div style={{ textAlign: "center", padding: "1rem" }}>
                                <div style={{ fontSize: 64, marginBottom: "1rem" }}>🎉</div>
                                <h3 style={{ fontWeight: 900, fontSize: 20, color: "#111827", marginBottom: 8 }}>Story Submitted!</h3>
                                <p style={{ color: "#64748b", fontSize: 14, marginBottom: "1.5rem" }}>Thank you for sharing. We&apos;ll review and publish your story soon.</p>
                                <button onClick={() => { setShowForm(false); setSubmitted(false); }} style={{ padding: "10px 24px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", border: "none", borderRadius: 999, fontWeight: 700, cursor: "pointer" }}>Close</button>
                            </div>
                        ) : (
                            <>
                                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
                                    <h3 style={{ fontWeight: 900, fontSize: 18, color: "#111827" }}>💌 Share Your Story</h3>
                                    <button onClick={() => setShowForm(false)} style={{ background: "none", border: "none", fontSize: 18, cursor: "pointer", color: "#94a3b8" }}>✕</button>
                                </div>
                                {[
                                    { label: "Your Name", key: "name1", placeholder: "e.g. Rahul" },
                                    { label: "Partner's Name", key: "name2", placeholder: "e.g. Priya" },
                                    { label: "City", key: "city", placeholder: "e.g. Mumbai" },
                                    { label: "Your Email", key: "email", placeholder: "For verification" },
                                ].map(f => (
                                    <div key={f.key} style={{ marginBottom: "1rem" }}>
                                        <label style={{ display: "block", fontSize: 11, fontWeight: 700, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 5 }}>{f.label}</label>
                                        <input value={form[f.key]} onChange={e => setForm(p => ({ ...p, [f.key]: e.target.value }))} placeholder={f.placeholder}
                                            style={{ width: "100%", padding: "10px 14px", border: "1.5px solid #e2e8f0", borderRadius: 12, fontSize: 13, fontFamily: "inherit", outline: "none", background: "#f8fafc" }} />
                                    </div>
                                ))}
                                <div style={{ marginBottom: "1.5rem" }}>
                                    <label style={{ display: "block", fontSize: 11, fontWeight: 700, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 5 }}>Your Story</label>
                                    <textarea value={form.story} onChange={e => setForm(p => ({ ...p, story: e.target.value }))} placeholder="Tell us how you met..." rows={4}
                                        style={{ width: "100%", padding: "10px 14px", border: "1.5px solid #e2e8f0", borderRadius: 12, fontSize: 13, fontFamily: "inherit", outline: "none", resize: "vertical", background: "#f8fafc" }} />
                                </div>
                                <button onClick={() => setSubmitted(true)}
                                    style={{ width: "100%", padding: "12px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", border: "none", borderRadius: 12, fontWeight: 800, fontSize: 14, cursor: "pointer", boxShadow: "0 4px 14px rgba(225,29,72,0.3)" }}>
                                    Submit Story 💌
                                </button>
                            </>
                        )}
                    </div>
                </div>
            )}

            <style>{`
                @media (max-width: 640px) {
                    .stats-grid { grid-template-columns: repeat(2, 1fr) !important; }
                }
            `}</style>
        </div>
    );
}
