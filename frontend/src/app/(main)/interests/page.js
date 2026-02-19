"use client";

import { useState, useEffect } from "react";
import api from "../../../services/api";
import Link from "next/link";
import { toast } from "react-toastify";

const MOCK_RECEIVED = [
    { id: "r1", userId: "u1", firstName: "Arjun", age: 29, city: "Mumbai", profession: "Doctor", photo: "https://randomuser.me/api/portraits/men/11.jpg", isVerified: true, match: 92, receivedAt: "2h ago" },
    { id: "r2", userId: "u2", firstName: "Rohan", age: 27, city: "Bangalore", profession: "Engineer", photo: "https://randomuser.me/api/portraits/men/12.jpg", isVerified: true, match: 86, receivedAt: "5h ago" },
    { id: "r3", userId: "u3", firstName: "Karan", age: 31, city: "Delhi", profession: "Lawyer", photo: "https://randomuser.me/api/portraits/men/13.jpg", isVerified: false, match: 80, receivedAt: "1d ago" },
    { id: "r4", userId: "u4", firstName: "Vivek", age: 28, city: "Pune", profession: "MBA Exec", photo: "https://randomuser.me/api/portraits/men/14.jpg", isVerified: true, match: 75, receivedAt: "2d ago" },
];
const MOCK_SENT = [
    { id: "s1", userId: "u5", firstName: "Priya", age: 26, city: "Mumbai", profession: "Doctor", photo: "https://randomuser.me/api/portraits/women/44.jpg", isVerified: true, match: 94, status: "pending", sentAt: "1h ago" },
    { id: "s2", userId: "u6", firstName: "Ananya", age: 24, city: "Bangalore", profession: "Engineer", photo: "https://randomuser.me/api/portraits/women/45.jpg", isVerified: true, match: 89, status: "accepted", sentAt: "3h ago" },
    { id: "s3", userId: "u7", firstName: "Kavya", age: 27, city: "Chennai", profession: "CA", photo: "https://randomuser.me/api/portraits/women/46.jpg", isVerified: false, match: 75, status: "declined", sentAt: "1d ago" },
];
const MOCK_MUTUAL = [
    { id: "m1", userId: "u8", firstName: "Nisha", age: 25, city: "Hyderabad", profession: "Designer", photo: "https://randomuser.me/api/portraits/women/49.jpg", isVerified: true, match: 88, matchedAt: "30m ago" },
    { id: "m2", userId: "u9", firstName: "Shreya", age: 26, city: "Jaipur", profession: "Teacher", photo: "https://randomuser.me/api/portraits/women/50.jpg", isVerified: false, match: 81, matchedAt: "2h ago" },
    { id: "m3", userId: "u10", firstName: "Tanvi", age: 28, city: "Kolkata", profession: "Architect", photo: "https://randomuser.me/api/portraits/women/51.jpg", isVerified: true, match: 77, matchedAt: "1d ago" },
];

const TABS = [
    { key: "received", label: "Received", icon: "💌", count: MOCK_RECEIVED.length },
    { key: "sent", label: "Sent", icon: "📨", count: MOCK_SENT.length },
    { key: "mutual", label: "Mutual", icon: "💕", count: MOCK_MUTUAL.length },
];

const STATUS_STYLE = {
    pending: { bg: "#fffbeb", color: "#92400e", border: "#fde68a", label: "⏳ Pending" },
    accepted: { bg: "#f0fdf4", color: "#166534", border: "#86efac", label: "✅ Accepted" },
    declined: { bg: "#fff1f2", color: "#9f1239", border: "#fecdd3", label: "✗ Declined" },
};

export default function InterestsPage() {
    const [activeTab, setActiveTab] = useState("received");
    const [data, setData] = useState({ received: MOCK_RECEIVED, sent: MOCK_SENT, mutual: MOCK_MUTUAL });
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        const fetchInterests = async () => {
            setLoading(true);
            try {
                const res = await api.get(`/interactions/interests?type=${activeTab}`);
                if (res.data?.length) setData(prev => ({ ...prev, [activeTab]: res.data }));
            } catch { }
            finally { setLoading(false); }
        };
        fetchInterests();
    }, [activeTab]);

    const currentList = data[activeTab] || [];

    const handleAccept = async (userId) => {
        try {
            await api.post("/interactions/like", { receiverId: userId });
            toast.success("💕 Interest Accepted!");
            setData(prev => ({ ...prev, received: prev.received.filter(r => r.userId !== userId) }));
        } catch { toast.success("💕 Interest Accepted! (Demo)"); }
    };

    const handleDecline = async (userId) => {
        try {
            await api.post("/interactions/decline", { userId });
            toast.info("Interest Declined");
            setData(prev => ({ ...prev, received: prev.received.filter(r => r.userId !== userId) }));
        } catch { toast.info("Interest Declined (Demo)"); setData(prev => ({ ...prev, received: prev.received.filter(r => r.userId !== userId) })); }
    };

    return (
        <div>
            {/* Header */}
            <div style={{ marginBottom: "1.5rem" }}>
                <h1 style={{ fontSize: 24, fontWeight: 800, color: "#111827" }}>Interests 💌</h1>
                <p style={{ fontSize: 13, color: "#94a3b8", marginTop: 4 }}>Manage your interest requests and mutual connections</p>
            </div>

            {/* Tab Bar */}
            <div style={{ display: "flex", gap: 6, background: "#f1f5f9", borderRadius: 16, padding: 5, marginBottom: "1.5rem", width: "fit-content" }}>
                {TABS.map(tab => (
                    <button
                        key={tab.key}
                        onClick={() => setActiveTab(tab.key)}
                        style={{
                            display: "flex", alignItems: "center", gap: 7,
                            padding: "9px 20px", borderRadius: 12, border: "none",
                            cursor: "pointer", fontWeight: 700, fontSize: 13,
                            background: activeTab === tab.key ? "white" : "transparent",
                            color: activeTab === tab.key ? "#e11d48" : "#64748b",
                            boxShadow: activeTab === tab.key ? "0 2px 8px rgba(0,0,0,0.08)" : "none",
                            transition: "all 0.2s",
                        }}
                    >
                        <span>{tab.icon}</span>
                        {tab.label}
                        <span style={{
                            background: activeTab === tab.key ? "#fce7f3" : "#e2e8f0",
                            color: activeTab === tab.key ? "#e11d48" : "#64748b",
                            fontSize: 10, fontWeight: 800, padding: "1px 6px", borderRadius: 99,
                        }}>{tab.count}</span>
                    </button>
                ))}
            </div>

            {/* Content */}
            {loading ? (
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))", gap: "1rem" }}>
                    {Array(3).fill(0).map((_, i) => (
                        <div key={i} style={{ display: "flex", gap: "1rem", padding: "1rem", background: "white", borderRadius: 20, border: "1px solid #f1f5f9" }}>
                            <div className="skeleton" style={{ width: 80, height: 80, borderRadius: 16, flexShrink: 0 }}></div>
                            <div style={{ flex: 1 }}>
                                <div className="skeleton" style={{ height: 14, width: "50%", marginBottom: 8 }}></div>
                                <div className="skeleton" style={{ height: 12, width: "70%", marginBottom: 14 }}></div>
                                <div className="skeleton" style={{ height: 36, borderRadius: 10 }}></div>
                            </div>
                        </div>
                    ))}
                </div>
            ) : currentList.length === 0 ? (
                <div style={{ textAlign: "center", padding: "5rem 2rem", background: "white", borderRadius: 24, border: "1.5px solid #f1f5f9" }}>
                    <div style={{ fontSize: 64, marginBottom: "1rem" }}>
                        {activeTab === "received" ? "📭" : activeTab === "sent" ? "📝" : "💔"}
                    </div>
                    <h3 style={{ fontSize: 18, fontWeight: 800, color: "#111827", marginBottom: 8 }}>
                        No {TABS.find(t => t.key === activeTab)?.label} Interests
                    </h3>
                    <p style={{ color: "#64748b", fontSize: 14, marginBottom: "1.25rem" }}>
                        {activeTab === "received" ? "No one has sent you an interest yet." :
                            activeTab === "sent" ? "You haven't sent any interests yet." :
                                "No mutual connections yet. Accept an interest to create a match!"}
                    </p>
                    <Link href="/matches" style={{ display: "inline-block", padding: "10px 24px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", borderRadius: 999, textDecoration: "none", fontWeight: 700, fontSize: 14 }}>
                        Browse Matches →
                    </Link>
                </div>
            ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: "0.875rem" }}>
                    {currentList.map((item) => (
                        <div key={item.id}
                            style={{
                                display: "flex", alignItems: "center", gap: "1rem", flexWrap: "wrap",
                                background: "white", borderRadius: 20,
                                border: "1.5px solid #f1f5f9",
                                padding: "14px 16px",
                                boxShadow: "0 2px 10px rgba(0,0,0,0.04)",
                                transition: "box-shadow 0.2s",
                            }}
                            onMouseEnter={e => e.currentTarget.style.boxShadow = "0 6px 24px rgba(0,0,0,0.08)"}
                            onMouseLeave={e => e.currentTarget.style.boxShadow = "0 2px 10px rgba(0,0,0,0.04)"}
                        >
                            {/* Photo */}
                            <div style={{ position: "relative", flexShrink: 0 }}>
                                <img
                                    src={item.photo}
                                    alt={item.firstName}
                                    style={{ width: 72, height: 72, borderRadius: 18, objectFit: "cover", border: "2px solid #fce7f3" }}
                                />
                                {item.match && (
                                    <div style={{
                                        position: "absolute", bottom: -4, right: -4,
                                        background: "linear-gradient(135deg, #e11d48, #c2185b)",
                                        color: "white", fontSize: 9, fontWeight: 800,
                                        padding: "2px 6px", borderRadius: 99, border: "2px solid white",
                                    }}>{item.match}%</div>
                                )}
                            </div>

                            {/* Info */}
                            <div style={{ flex: 1, minWidth: 0 }}>
                                <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                                    <h3 style={{ fontWeight: 800, fontSize: 15, color: "#111827" }}>{item.firstName}, {item.age}</h3>
                                    {item.isVerified && <span style={{ fontSize: 13 }}>✅</span>}
                                </div>
                                <p style={{ fontSize: 12, color: "#64748b", marginTop: 2 }}>{item.profession} · {item.city}</p>
                                <p style={{ fontSize: 11, color: "#94a3b8", marginTop: 3 }}>
                                    {activeTab === "received" && `Sent ${item.receivedAt}`}
                                    {activeTab === "sent" && `Sent ${item.sentAt}`}
                                    {activeTab === "mutual" && `Matched ${item.matchedAt}`}
                                </p>

                                {/* Status badge for sent */}
                                {activeTab === "sent" && item.status && (
                                    <span style={{
                                        display: "inline-block", marginTop: 6,
                                        background: STATUS_STYLE[item.status]?.bg,
                                        color: STATUS_STYLE[item.status]?.color,
                                        border: `1px solid ${STATUS_STYLE[item.status]?.border}`,
                                        fontSize: 10, fontWeight: 700, padding: "2px 8px", borderRadius: 99,
                                    }}>
                                        {STATUS_STYLE[item.status]?.label}
                                    </span>
                                )}
                            </div>

                            {/* Actions */}
                            <div style={{ display: "flex", gap: 8, flexShrink: 0 }}>
                                {activeTab === "received" && (
                                    <>
                                        <button onClick={() => handleAccept(item.userId)} style={{ padding: "8px 16px", background: "linear-gradient(135deg, #10b981, #059669)", color: "white", border: "none", borderRadius: 10, fontWeight: 700, fontSize: 13, cursor: "pointer" }}>
                                            ✓ Accept
                                        </button>
                                        <button onClick={() => handleDecline(item.userId)} style={{ padding: "8px 12px", background: "#f8fafc", border: "1.5px solid #e2e8f0", color: "#64748b", borderRadius: 10, fontWeight: 700, fontSize: 13, cursor: "pointer" }}>
                                            ✕
                                        </button>
                                    </>
                                )}
                                {activeTab === "mutual" && (
                                    <Link href={`/chat/${item.userId}`} style={{ padding: "8px 18px", background: "linear-gradient(135deg, #6366f1, #4f46e5)", color: "white", borderRadius: 10, fontWeight: 700, fontSize: 13, textDecoration: "none" }}>
                                        💬 Chat
                                    </Link>
                                )}
                                <Link href={`/profile/${item.userId}`} style={{ display: "flex", alignItems: "center", padding: "8px 12px", background: "#f8fafc", border: "1.5px solid #e2e8f0", color: "#475569", borderRadius: 10, fontSize: 14, textDecoration: "none" }}>
                                    👁
                                </Link>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Tips card */}
            {activeTab === "received" && currentList.length > 0 && (
                <div style={{ marginTop: "1.5rem", background: "linear-gradient(135deg, #f0fdf4, #dcfce7)", border: "1px solid #86efac", borderRadius: 16, padding: "14px 18px", display: "flex", alignItems: "center", gap: 12 }}>
                    <span style={{ fontSize: 24 }}>💡</span>
                    <p style={{ fontSize: 13, color: "#166534", fontWeight: 600 }}>Tip: Accepting an interest allows both of you to chat and view contact details!</p>
                </div>
            )}
        </div>
    );
}
