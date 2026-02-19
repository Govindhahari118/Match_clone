"use client";

import { useState, useEffect } from "react";
import api from "../../../services/api";
import Link from "next/link";

const MOCK_VIEWERS = [
    { id: "v1", userId: "u1", firstName: "Arjun", age: 29, city: "Mumbai", profession: "Doctor", photo: "https://randomuser.me/api/portraits/men/11.jpg", isVerified: true, receivedAt: "5m ago" },
    { id: "v2", userId: "u2", firstName: "Rohan", age: 27, city: "Bangalore", profession: "Engineer", photo: "https://randomuser.me/api/portraits/men/12.jpg", isVerified: true, receivedAt: "2h ago" },
    { id: "v3", userId: "u3", firstName: "Karan", age: 31, city: "Delhi", profession: "Lawyer", photo: "https://randomuser.me/api/portraits/men/13.jpg", isVerified: false, receivedAt: "1d ago" },
    { id: "v4", userId: "u4", firstName: "Vivek", age: 28, city: "Pune", profession: "MBA", photo: "https://randomuser.me/api/portraits/men/14.jpg", isVerified: true, receivedAt: "2d ago" },
    { id: "v5", userId: "u5", firstName: "Rahul", age: 30, city: "Chennai", profession: "Architect", photo: "https://randomuser.me/api/portraits/men/15.jpg", isVerified: false, receivedAt: "3d ago" },
    { id: "v6", userId: "u6", firstName: "Dev", age: 26, city: "Hyderabad", profession: "Designer", photo: "https://randomuser.me/api/portraits/men/16.jpg", isVerified: true, receivedAt: "5d ago" },
];

export default function WhoViewedPage() {
    const [viewers, setViewers] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetch = async () => {
            try {
                const res = await api.get("/interactions/profile-viewers");
                setViewers(res.data?.length ? res.data : MOCK_VIEWERS);
            } catch {
                setViewers(MOCK_VIEWERS);
            } finally {
                setLoading(false);
            }
        };
        fetch();
    }, []);

    return (
        <div>
            {/* Header */}
            <div style={{ marginBottom: "1.5rem" }}>
                <h1 style={{ fontSize: 24, fontWeight: 800, color: "#111827" }}>Who Viewed My Profile 👁</h1>
                <p style={{ fontSize: 13, color: "#94a3b8", marginTop: 4 }}>
                    These people recently visited your profile — {viewers.length} unique visitors
                </p>
            </div>

            {/* Upgrade teaser banner */}
            <div style={{ background: "linear-gradient(135deg, #fffbeb, #fef3c7)", border: "1px solid #fde68a", borderRadius: 20, padding: "1rem 1.5rem", display: "flex", alignItems: "center", justifyContent: "space-between", gap: "1rem", marginBottom: "1.5rem", flexWrap: "wrap" }}>
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                    <span style={{ fontSize: 28 }}>👑</span>
                    <div>
                        <p style={{ fontWeight: 800, fontSize: 14, color: "#78350f" }}>See all visitors in real-time</p>
                        <p style={{ fontSize: 12, color: "#92400e", marginTop: 2 }}>Upgrade to Gold to see complete visitor list with contact details</p>
                    </div>
                </div>
                <Link href="/pricing" style={{ padding: "9px 22px", background: "linear-gradient(135deg, #f59e0b, #d97706)", color: "white", borderRadius: 12, fontSize: 13, fontWeight: 800, textDecoration: "none", flexShrink: 0, boxShadow: "0 4px 12px rgba(245,158,11,0.3)" }}>
                    Upgrade Now →
                </Link>
            </div>

            {loading ? (
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))", gap: "1rem" }}>
                    {Array(6).fill(0).map((_, i) => (
                        <div key={i} style={{ display: "flex", gap: 12, padding: 16, background: "white", borderRadius: 20, border: "1px solid #f1f5f9" }}>
                            <div className="skeleton" style={{ width: 64, height: 64, borderRadius: "50%", flexShrink: 0 }}></div>
                            <div style={{ flex: 1 }}>
                                <div className="skeleton" style={{ height: 13, width: "55%", marginBottom: 8 }}></div>
                                <div className="skeleton" style={{ height: 11, width: "75%" }}></div>
                            </div>
                        </div>
                    ))}
                </div>
            ) : viewers.length === 0 ? (
                <div style={{ textAlign: "center", padding: "5rem 2rem", background: "white", borderRadius: 24, border: "1.5px solid #f1f5f9" }}>
                    <div style={{ fontSize: 64, marginBottom: "1rem" }}>👁</div>
                    <h3 style={{ fontWeight: 800, fontSize: 18, color: "#111827", marginBottom: 8 }}>No profile views yet</h3>
                    <p style={{ color: "#64748b", fontSize: 14, marginBottom: "1.5rem" }}>Complete your profile to attract more visitors</p>
                    <Link href="/profile" style={{ display: "inline-block", padding: "10px 24px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", borderRadius: 999, textDecoration: "none", fontWeight: 700, fontSize: 14 }}>
                        Complete Profile →
                    </Link>
                </div>
            ) : (
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))", gap: "1rem" }}>
                    {viewers.map((viewer, idx) => {
                        const blurred = idx >= 3; // Blur after 3 for non-premium
                        return (
                            <div
                                key={viewer.id || viewer.userId}
                                style={{
                                    display: "flex", alignItems: "center", gap: 14,
                                    background: "white", borderRadius: 20,
                                    border: "1.5px solid #f1f5f9",
                                    padding: "14px 16px",
                                    boxShadow: "0 2px 10px rgba(0,0,0,0.04)",
                                    transition: "box-shadow 0.2s",
                                    position: "relative",
                                    overflow: "hidden",
                                }}
                                onMouseEnter={e => e.currentTarget.style.boxShadow = "0 6px 24px rgba(0,0,0,0.09)"}
                                onMouseLeave={e => e.currentTarget.style.boxShadow = "0 2px 10px rgba(0,0,0,0.04)"}
                            >
                                {/* Blur overlay for non-premium */}
                                {blurred && (
                                    <div style={{ position: "absolute", inset: 0, backdropFilter: "blur(6px)", background: "rgba(255,255,255,0.6)", zIndex: 2, display: "flex", alignItems: "center", justifyContent: "center", borderRadius: 20 }}>
                                        <Link href="/pricing" style={{ padding: "7px 18px", background: "linear-gradient(135deg, #f59e0b, #d97706)", color: "white", borderRadius: 999, fontSize: 12, fontWeight: 800, textDecoration: "none" }}>
                                            👑 Unlock
                                        </Link>
                                    </div>
                                )}
                                <div style={{ position: "relative", flexShrink: 0 }}>
                                    <img src={viewer.photo} alt={viewer.firstName} style={{ width: 60, height: 60, borderRadius: "50%", objectFit: "cover", border: "2px solid #fce7f3" }} />
                                    <span style={{ position: "absolute", bottom: 1, right: 1, width: 12, height: 12, background: "#10b981", borderRadius: "50%", border: "2px solid white" }}></span>
                                </div>
                                <div style={{ flex: 1, minWidth: 0 }}>
                                    <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                                        <h3 style={{ fontWeight: 800, fontSize: 14, color: "#111827" }}>{viewer.firstName}, {viewer.age}</h3>
                                        {viewer.isVerified && <span style={{ fontSize: 12 }}>✅</span>}
                                    </div>
                                    <p style={{ fontSize: 12, color: "#64748b", marginTop: 2 }}>{viewer.profession} · {viewer.city}</p>
                                    <p style={{ fontSize: 11, color: "#94a3b8", marginTop: 2 }}>👁 Viewed {viewer.receivedAt}</p>
                                </div>
                                <Link href={`/profile/${viewer.userId}`} style={{ flexShrink: 0, padding: "7px 14px", background: "#fff1f2", border: "1.5px solid #fecdd3", color: "#e11d48", borderRadius: 10, fontSize: 12, fontWeight: 700, textDecoration: "none" }}>
                                    View
                                </Link>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
