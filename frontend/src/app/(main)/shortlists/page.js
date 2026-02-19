"use client";

import { useState, useEffect } from "react";
import api from "../../../services/api";
import Link from "next/link";
import { toast } from "react-toastify";

const MOCK = [
    { id: "sl1", userId: "u1", firstName: "Priya", age: 26, city: "Mumbai", profession: "Doctor", photo: "https://randomuser.me/api/portraits/women/44.jpg", isVerified: true, match: 94, shortlistedAt: "2h ago" },
    { id: "sl2", userId: "u2", firstName: "Ananya", age: 24, city: "Bangalore", profession: "Engineer", photo: "https://randomuser.me/api/portraits/women/45.jpg", isVerified: true, match: 89, shortlistedAt: "5h ago" },
    { id: "sl3", userId: "u3", firstName: "Kavya", age: 27, city: "Chennai", profession: "CA", photo: "https://randomuser.me/api/portraits/women/46.jpg", isVerified: false, match: 85, shortlistedAt: "1d ago" },
    { id: "sl4", userId: "u4", firstName: "Riya", age: 25, city: "Pune", profession: "Architect", photo: "https://randomuser.me/api/portraits/women/47.jpg", isVerified: true, match: 82, shortlistedAt: "2d ago" },
    { id: "sl5", userId: "u5", firstName: "Simran", age: 28, city: "Delhi", profession: "Lawyer", photo: "https://randomuser.me/api/portraits/women/48.jpg", isVerified: true, match: 79, shortlistedAt: "3d ago" },
    { id: "sl6", userId: "u6", firstName: "Naina", age: 23, city: "Hyderabad", profession: "Designer", photo: "https://randomuser.me/api/portraits/women/49.jpg", isVerified: false, match: 75, shortlistedAt: "5d ago" },
];

export default function ShortlistsPage() {
    const [list, setList] = useState([]);
    const [loading, setLoading] = useState(true);
    const [view, setView] = useState("grid");

    useEffect(() => {
        const fetchData = async () => {
            setLoading(true);
            try {
                const res = await api.get("/shortlist");
                setList(res.data?.length ? res.data : MOCK);
            } catch { setList(MOCK); }
            finally { setLoading(false); }
        };
        fetchData();
    }, []);

    const handleRemove = async (userId) => {
        try {
            await api.post("/shortlist/remove", { shortlistedUserId: userId });
            setList(prev => prev.filter(p => p.userId !== userId));
            toast.success("Removed from shortlist");
        } catch {
            setList(prev => prev.filter(p => p.userId !== userId));
            toast.success("Removed from shortlist");
        }
    };

    const handleInterest = async (userId) => {
        try {
            await api.post("/interactions/like", { receiverId: userId });
            toast.success("💌 Interest Sent!");
        } catch { toast.error("Already sent or error occurred"); }
    };

    return (
        <div>
            {/* Header */}
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "1rem", marginBottom: "1.5rem" }}>
                <div>
                    <h1 style={{ fontSize: 24, fontWeight: 800, color: "#111827" }}>Shortlisted ⭐</h1>
                    <p style={{ fontSize: 13, color: "#94a3b8", marginTop: 4 }}>{list.length} profiles saved for later</p>
                </div>
                <div style={{ display: "flex", gap: 8 }}>
                    <div style={{ display: "flex", background: "#f1f5f9", borderRadius: 10, padding: 3 }}>
                        {[{ v: "grid", icon: "⊞" }, { v: "list", icon: "≡" }].map(({ v, icon }) => (
                            <button key={v} onClick={() => setView(v)} style={{
                                padding: "6px 12px", borderRadius: 8, border: "none", cursor: "pointer",
                                background: view === v ? "white" : "transparent",
                                color: view === v ? "#e11d48" : "#64748b",
                                fontWeight: 700, fontSize: 18,
                                boxShadow: view === v ? "0 1px 4px rgba(0,0,0,0.1)" : "none",
                                transition: "all 0.2s",
                            }}>{icon}</button>
                        ))}
                    </div>
                    <Link href="/matches" style={{ padding: "9px 18px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", borderRadius: 10, fontSize: 13, fontWeight: 700, textDecoration: "none", boxShadow: "0 4px 12px rgba(225,29,72,0.3)" }}>
                        + Add More
                    </Link>
                </div>
            </div>

            {/* Loading */}
            {loading ? (
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(230px, 1fr))", gap: "1.25rem" }}>
                    {Array(6).fill(0).map((_, i) => (
                        <div key={i} style={{ borderRadius: 24, overflow: "hidden", background: "white", border: "1px solid #f1f5f9" }}>
                            <div className="skeleton" style={{ height: 200 }}></div>
                            <div style={{ padding: 14 }}>
                                <div className="skeleton" style={{ height: 14, width: "60%", marginBottom: 8 }}></div>
                                <div className="skeleton" style={{ height: 12, width: "80%", marginBottom: 14 }}></div>
                                <div className="skeleton" style={{ height: 38, borderRadius: 10 }}></div>
                            </div>
                        </div>
                    ))}
                </div>
            ) : list.length === 0 ? (
                <div style={{ textAlign: "center", padding: "6rem 2rem", background: "white", borderRadius: 24, border: "1.5px solid #f1f5f9" }}>
                    <div style={{ fontSize: 64, marginBottom: "1rem" }}>⭐</div>
                    <h3 style={{ fontSize: 20, fontWeight: 800, color: "#111827", marginBottom: 8 }}>No Shortlisted Profiles</h3>
                    <p style={{ color: "#64748b", fontSize: 14, marginBottom: "1.5rem" }}>Browse matches and star the ones you like to save them here.</p>
                    <Link href="/matches" style={{ display: "inline-block", padding: "11px 28px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", borderRadius: 999, textDecoration: "none", fontWeight: 700, fontSize: 14 }}>
                        Browse Matches →
                    </Link>
                </div>
            ) : view === "grid" ? (
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(230px, 1fr))", gap: "1.25rem" }}>
                    {list.map(profile => (
                        <div key={profile.id || profile.userId}
                            style={{ background: "white", borderRadius: 24, overflow: "hidden", border: "1.5px solid #f1f5f9", boxShadow: "0 2px 12px rgba(0,0,0,0.05)", transition: "all 0.3s cubic-bezier(0.34,1.56,0.64,1)" }}
                            onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-5px)"; e.currentTarget.style.boxShadow = "0 16px 40px rgba(0,0,0,0.1)"; }}
                            onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "0 2px 12px rgba(0,0,0,0.05)"; }}
                        >
                            <div style={{ position: "relative", height: 200 }}>
                                <img src={profile.photo} alt={profile.firstName} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                                <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(0,0,0,0.7) 0%, transparent 55%)" }}></div>
                                <div style={{ position: "absolute", top: 10, left: 10, background: "#f59e0b", color: "white", fontSize: 10, fontWeight: 800, padding: "3px 9px", borderRadius: 99 }}>⭐ {profile.match}%</div>
                                <button onClick={() => handleRemove(profile.userId)} style={{ position: "absolute", top: 8, right: 8, width: 30, height: 30, borderRadius: "50%", background: "rgba(255,255,255,0.9)", border: "none", cursor: "pointer", fontSize: 14, display: "flex", alignItems: "center", justifyContent: "center" }} title="Remove from shortlist">✕</button>
                                {profile.isVerified && <div style={{ position: "absolute", bottom: 40, left: 10, background: "rgba(255,255,255,0.9)", color: "#2563eb", fontSize: 10, fontWeight: 700, padding: "2px 7px", borderRadius: 99 }}>✅ Verified</div>}
                                <div style={{ position: "absolute", bottom: 10, left: 12 }}>
                                    <p style={{ fontWeight: 800, fontSize: 16, color: "white" }}>{profile.firstName}, {profile.age}</p>
                                    <p style={{ fontSize: 11, color: "rgba(255,255,255,0.8)" }}>📍 {profile.city}</p>
                                </div>
                            </div>
                            <div style={{ padding: "12px 14px" }}>
                                <p style={{ fontSize: 12, color: "#64748b", fontWeight: 600, marginBottom: 10 }}>
                                    {profile.profession} · Saved {profile.shortlistedAt}
                                </p>
                                <div style={{ display: "flex", gap: 6 }}>
                                    <button onClick={() => handleInterest(profile.userId)} style={{ flex: 1, padding: "9px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", border: "none", borderRadius: 10, fontWeight: 700, fontSize: 12, cursor: "pointer" }}>
                                        💌 Send Interest
                                    </button>
                                    <Link href={`/profile/${profile.userId}`} style={{ display: "flex", alignItems: "center", justifyContent: "center", width: 38, background: "#f8fafc", border: "1.5px solid #e2e8f0", borderRadius: 10, fontSize: 15, textDecoration: "none" }}>👁</Link>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            ) : (
                /* List view */
                <div style={{ display: "flex", flexDirection: "column", gap: "0.875rem" }}>
                    {list.map(profile => (
                        <div key={profile.id || profile.userId}
                            style={{ display: "flex", alignItems: "center", gap: "1rem", flexWrap: "wrap", background: "white", borderRadius: 20, border: "1.5px solid #f1f5f9", padding: "14px 16px", boxShadow: "0 2px 10px rgba(0,0,0,0.04)", transition: "box-shadow 0.2s" }}
                            onMouseEnter={e => e.currentTarget.style.boxShadow = "0 6px 24px rgba(0,0,0,0.08)"}
                            onMouseLeave={e => e.currentTarget.style.boxShadow = "0 2px 10px rgba(0,0,0,0.04)"}
                        >
                            <div style={{ position: "relative" }}>
                                <img src={profile.photo} alt={profile.firstName} style={{ width: 72, height: 72, borderRadius: 18, objectFit: "cover", border: "2px solid #fef3c7" }} />
                                <div style={{ position: "absolute", bottom: -4, right: -4, background: "#f59e0b", color: "white", fontSize: 9, fontWeight: 800, padding: "2px 5px", borderRadius: 99, border: "2px solid white" }}>⭐</div>
                            </div>
                            <div style={{ flex: 1, minWidth: 0 }}>
                                <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                                    <h3 style={{ fontWeight: 800, fontSize: 15, color: "#111827" }}>{profile.firstName}, {profile.age}</h3>
                                    {profile.isVerified && <span style={{ fontSize: 12 }}>✅</span>}
                                </div>
                                <p style={{ fontSize: 12, color: "#64748b", marginTop: 2 }}>{profile.profession} · {profile.city}</p>
                                <p style={{ fontSize: 11, color: "#94a3b8", marginTop: 2 }}>Saved {profile.shortlistedAt} · {profile.match}% match</p>
                            </div>
                            <div style={{ display: "flex", gap: 8 }}>
                                <button onClick={() => handleInterest(profile.userId)} style={{ padding: "8px 16px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", border: "none", borderRadius: 10, fontWeight: 700, fontSize: 13, cursor: "pointer" }}>💌 Interest</button>
                                <Link href={`/profile/${profile.userId}`} style={{ display: "flex", alignItems: "center", padding: "8px 12px", background: "#f8fafc", border: "1.5px solid #e2e8f0", color: "#475569", borderRadius: 10, textDecoration: "none", fontSize: 14 }}>👁</Link>
                                <button onClick={() => handleRemove(profile.userId)} style={{ padding: "8px 12px", background: "#fff1f2", border: "1.5px solid #fecdd3", color: "#e11d48", borderRadius: 10, fontWeight: 700, fontSize: 13, cursor: "pointer" }}>✕</button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
