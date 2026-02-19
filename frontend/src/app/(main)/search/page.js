"use client";

import { useState } from "react";
import api from "../../../services/api";
import Link from "next/link";
import { toast } from "react-toastify";

const RELIGIONS = ["Any", "Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist", "Parsi"];
const CASTES = ["Any", "Brahmin", "Kshatriya", "Vaishya", "Kayastha", "Rajput", "Reddy", "Naidu", "Chettiar", "Jat"];
const INCOME_OPT = ["Any", "Below 3L", "3-5L", "5-10L", "10-25L", "25-50L", "50L+"];
const EDUCATIONS = ["Any", "10th", "12th", "Diploma", "Graduate", "Post Graduate", "Doctorate"];
const PROFESSIONS = ["Any", "Engineer", "Doctor", "CA/Finance", "Govt/PSU", "Lawyer", "Teacher", "Business", "NRI", "Other"];
const MARITAL = ["Any", "Never Married", "Divorced", "Widowed", "Separated"];
const HEIGHTS = ["Any", "Below 5ft", "5ft–5.5ft", "5.5ft–6ft", "6ft–6.5ft", "Above 6.5ft"];
const BODIES = ["Any", "Slim", "Average", "Athletic", "Heavy"];

const MOCK_RESULTS = [
    { id: "r1", userId: "u1", firstName: "Aarti", age: 26, city: "Mumbai", profession: "Doctor", religion: "Hindu", caste: "Brahmin", photo: "https://randomuser.me/api/portraits/women/11.jpg", isVerified: true, match: 92, education: "MBBS", income: "10-25L" },
    { id: "r2", userId: "u2", firstName: "Divya", age: 24, city: "Pune", profession: "Engineer", religion: "Hindu", caste: "Reddy", photo: "https://randomuser.me/api/portraits/women/12.jpg", isVerified: true, match: 87, education: "B.Tech", income: "5-10L" },
    { id: "r3", userId: "u3", firstName: "Lakshmi", age: 29, city: "Chennai", profession: "CA", religion: "Hindu", caste: "Naidu", photo: "https://randomuser.me/api/portraits/women/13.jpg", isVerified: false, match: 81, education: "CA", income: "5-10L" },
    { id: "r4", userId: "u4", firstName: "Pooja", age: 27, city: "Hyderabad", profession: "Lawyer", religion: "Hindu", caste: "Reddy", photo: "https://randomuser.me/api/portraits/women/14.jpg", isVerified: true, match: 78, education: "LLB", income: "10-25L" },
    { id: "r5", userId: "u5", firstName: "Isha", age: 25, city: "Delhi", profession: "IPS Officer", religion: "Hindu", caste: "Rajput", photo: "https://randomuser.me/api/portraits/women/15.jpg", isVerified: true, match: 75, education: "IPS", income: "5-10L" },
    { id: "r6", userId: "u6", firstName: "Zara", age: 26, city: "Mumbai", profession: "Fashion Designer", religion: "Muslim", caste: "Any", photo: "https://randomuser.me/api/portraits/women/16.jpg", isVerified: false, match: 70, education: "B.Des", income: "3-5L" },
];

const FILTERS_CONFIG = [
    {
        group: "Basic",
        fields: [
            { label: "Looking For", name: "gender", type: "select", options: ["Bride (Woman)", "Groom (Man)"] },
            { label: "Religion", name: "religion", type: "select", options: RELIGIONS },
            { label: "Caste", name: "caste", type: "select", options: CASTES },
            { label: "Marital Status", name: "maritalStatus", type: "select", options: MARITAL },
        ]
    },
    {
        group: "Education & Career",
        fields: [
            { label: "Education", name: "education", type: "select", options: EDUCATIONS },
            { label: "Profession", name: "profession", type: "select", options: PROFESSIONS },
            { label: "Annual Income", name: "income", type: "select", options: INCOME_OPT },
        ]
    },
    {
        group: "Appearance",
        fields: [
            { label: "Height", name: "height", type: "select", options: HEIGHTS },
            { label: "Body Type", name: "body", type: "select", options: BODIES },
        ]
    },
    {
        group: "Location",
        fields: [
            { label: "City", name: "city", type: "text", placeholder: "Any city..." },
            { label: "State", name: "state", type: "text", placeholder: "Any state..." },
        ]
    },
];

export default function SearchPage() {
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searched, setSearched] = useState(false);
    const [shortlisted, setShortlisted] = useState(new Set());

    const initFilters = () => ({
        gender: "Bride (Woman)", religion: "Any", caste: "Any", maritalStatus: "Any",
        education: "Any", profession: "Any", income: "Any",
        height: "Any", body: "Any",
        minAge: "22", maxAge: "35", city: "", state: "",
    });
    const [filters, setFilters] = useState(initFilters());

    const handleChange = (e) => setFilters(p => ({ ...p, [e.target.name]: e.target.value }));
    const handleReset = () => setFilters(initFilters());

    const handleSearch = async (e) => {
        e.preventDefault();
        setLoading(true);
        setSearched(true);
        try {
            const params = {};
            Object.entries(filters).forEach(([key, value]) => {
                if (value && value !== "Any") {
                    if (key === "minAge" || key === "maxAge") params[key] = value;
                    else if (key === "gender") params[key] = value.includes("Bride") ? "female" : "male";
                    else params[key] = value;
                }
            });

            const res = await api.get("/matches", { params });
            setResults(res.data?.length ? res.data : []);
        } catch {
            setResults([]);
        } finally {
            setLoading(false);
        }
    };

    const handleInterest = async (userId) => {
        try {
            await api.post("/interactions/like", { receiverId: userId });
            toast.success("💌 Interest Sent!");
        } catch {
            toast.error("Already sent or an error occurred");
        }
    };

    const toggleShortlist = async (userId) => {
        const already = shortlisted.has(userId);
        try {
            if (already) {
                await api.post("/shortlist/remove", { shortlistedUserId: userId });
                setShortlisted(prev => { const s = new Set(prev); s.delete(userId); return s; });
                toast.info("Removed from shortlist");
            } else {
                await api.post("/shortlist/add", { shortlistedUserId: userId });
                setShortlisted(prev => new Set([...prev, userId]));
                toast.success("⭐ Added to shortlist!");
            }
        } catch {
            setShortlisted(prev => {
                const s = new Set(prev);
                already ? s.delete(userId) : s.add(userId);
                return s;
            });
            toast.success(already ? "Removed from shortlist" : "⭐ Added to shortlist!");
        }
    };

    const inputStyle = {
        width: "100%", padding: "9px 12px",
        border: "1.5px solid #e2e8f0", borderRadius: 10,
        fontSize: 13, color: "#1e293b", background: "#f8fafc",
        fontFamily: "inherit", outline: "none",
        transition: "border-color 0.2s, box-shadow 0.2s",
    };

    return (
        <div>
            {/* Header */}
            <div style={{ marginBottom: "1.5rem" }}>
                <h1 style={{ fontSize: 24, fontWeight: 800, color: "#111827" }}>Advanced Search 🔍</h1>
                <p style={{ fontSize: 13, color: "#94a3b8", marginTop: 4 }}>Use precise filters to find your perfect match</p>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "300px 1fr", gap: "1.5rem" }} className="search-layout">

                {/* ── FILTER SIDEBAR ── */}
                <aside>
                    <form onSubmit={handleSearch} style={{
                        background: "white", borderRadius: 24,
                        border: "1.5px solid #f1f5f9",
                        boxShadow: "0 2px 16px rgba(0,0,0,0.04)",
                        position: "sticky", top: 80, maxHeight: "calc(100vh - 100px)", overflowY: "auto",
                    }}>
                        <div style={{ padding: "1.25rem 1.25rem 0.5rem", borderBottom: "1px solid #f8fafc" }}>
                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                                <h2 style={{ fontWeight: 800, fontSize: 15, color: "#111827" }}>⚙️ Filters</h2>
                                <button type="button" onClick={handleReset} style={{ fontSize: 11, fontWeight: 700, color: "#e11d48", background: "none", border: "none", cursor: "pointer" }}>Reset All</button>
                            </div>
                            {/* Age range inline */}
                            <div style={{ marginTop: "1rem" }}>
                                <label style={{ fontSize: 11, fontWeight: 700, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.5px", display: "block", marginBottom: 6 }}>Age Range</label>
                                <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
                                    <input name="minAge" type="number" min="18" max="70" value={filters.minAge} onChange={handleChange} style={{ ...inputStyle, textAlign: "center" }} />
                                    <span style={{ color: "#94a3b8" }}>—</span>
                                    <input name="maxAge" type="number" min="18" max="70" value={filters.maxAge} onChange={handleChange} style={{ ...inputStyle, textAlign: "center" }} />
                                </div>
                            </div>
                        </div>

                        {FILTERS_CONFIG.map(group => (
                            <div key={group.group} style={{ padding: "1rem 1.25rem", borderBottom: "1px solid #f8fafc" }}>
                                <h3 style={{ fontSize: 11, fontWeight: 800, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "0.8px", marginBottom: "0.875rem" }}>{group.group}</h3>
                                <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
                                    {group.fields.map(field => (
                                        <div key={field.name}>
                                            <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#374151", marginBottom: 5 }}>{field.label}</label>
                                            {field.type === "select" ? (
                                                <select name={field.name} value={filters[field.name]} onChange={handleChange} style={{ ...inputStyle }}>
                                                    {field.options.map(o => <option key={o}>{o}</option>)}
                                                </select>
                                            ) : (
                                                <input type="text" name={field.name} placeholder={field.placeholder} value={filters[field.name]} onChange={handleChange} style={inputStyle} />
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        ))}

                        <div style={{ padding: "1rem 1.25rem" }}>
                            <button type="submit" disabled={loading} style={{
                                width: "100%", padding: "12px",
                                background: "linear-gradient(135deg, #e11d48, #c2185b)",
                                color: "white", border: "none", borderRadius: 14,
                                fontWeight: 800, fontSize: 14, cursor: "pointer",
                                boxShadow: "0 6px 18px rgba(225,29,72,0.35)", transition: "all 0.2s",
                            }}>
                                {loading ? "Searching..." : "Search Profiles →"}
                            </button>
                        </div>
                    </form>
                </aside>

                {/* ── RESULTS ── */}
                <div>
                    {!searched ? (
                        <div style={{ textAlign: "center", padding: "6rem 2rem", background: "white", borderRadius: 24, border: "1.5px solid #f1f5f9" }}>
                            <div style={{ fontSize: 72, marginBottom: "1rem" }}>🔍</div>
                            <h3 style={{ fontSize: 20, fontWeight: 800, color: "#111827", marginBottom: 8 }}>Set Your Preferences</h3>
                            <p style={{ color: "#64748b", fontSize: 14 }}>Use the filters on the left to find your perfect life partner</p>
                        </div>
                    ) : loading ? (
                        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(230px, 1fr))", gap: "1.25rem" }}>
                            {Array(6).fill(0).map((_, i) => (
                                <div key={i} style={{ borderRadius: 24, overflow: "hidden", background: "white", border: "1px solid #f1f5f9" }}>
                                    <div className="skeleton" style={{ height: 220 }}></div>
                                    <div style={{ padding: 14 }}>
                                        <div className="skeleton" style={{ height: 14, width: "60%", marginBottom: 8 }}></div>
                                        <div className="skeleton" style={{ height: 12, width: "80%", marginBottom: 14 }}></div>
                                        <div className="skeleton" style={{ height: 40, borderRadius: 12 }}></div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : results.length === 0 ? (
                        <div style={{ textAlign: "center", padding: "5rem 2rem", background: "white", borderRadius: 24, border: "1.5px solid #f1f5f9" }}>
                            <div style={{ fontSize: 64, marginBottom: "1rem" }}>😔</div>
                            <h3 style={{ fontSize: 20, fontWeight: 800, color: "#111827", marginBottom: 8 }}>No Results Found</h3>
                            <p style={{ color: "#64748b", fontSize: 14, marginBottom: "1.5rem" }}>Try broadening your search criteria</p>
                            <button onClick={handleReset} style={{ padding: "10px 24px", background: "#e11d48", color: "white", border: "none", borderRadius: 999, fontWeight: 700, cursor: "pointer", fontSize: 14 }}>Reset Filters</button>
                        </div>
                    ) : (
                        <>
                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
                                <p style={{ fontSize: 13, color: "#64748b", fontWeight: 600 }}>
                                    <strong style={{ color: "#111827" }}>{results.length}</strong> profiles found
                                </p>
                                <select style={{ ...inputStyle, width: "auto", fontSize: 12 }}>
                                    <option>Sort: Best Match First</option>
                                    <option>Sort: Newest First</option>
                                    <option>Sort: Age: Youngest First</option>
                                </select>
                            </div>
                            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(230px, 1fr))", gap: "1.25rem" }}>
                                {results.map((profile) => (
                                    <div key={profile.id || profile.userId}
                                        style={{
                                            background: "white", borderRadius: 24, overflow: "hidden",
                                            border: "1.5px solid #f1f5f9",
                                            boxShadow: "0 2px 12px rgba(0,0,0,0.05)",
                                            transition: "all 0.3s cubic-bezier(0.34,1.56,0.64,1)",
                                        }}
                                        onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-5px)"; e.currentTarget.style.boxShadow = "0 16px 40px rgba(0,0,0,0.1)"; }}
                                        onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "0 2px 12px rgba(0,0,0,0.05)"; }}
                                    >
                                        <div style={{ position: "relative", height: 210 }}>
                                            <img src={profile.photo} alt={profile.firstName} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                                            <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(0,0,0,0.7) 0%, transparent 55%)" }}></div>
                                            <div style={{ position: "absolute", top: 10, left: 10, background: profile.match >= 90 ? "#10b981" : "#e11d48", color: "white", fontSize: 10, fontWeight: 800, padding: "3px 9px", borderRadius: 99 }}>{profile.match}% Match</div>
                                            <button onClick={() => toggleShortlist(profile.userId)} style={{ position: "absolute", top: 8, right: 8, width: 32, height: 32, borderRadius: "50%", background: shortlisted.has(profile.userId) ? "#f59e0b" : "rgba(255,255,255,0.9)", border: "none", cursor: "pointer", fontSize: 15, display: "flex", alignItems: "center", justifyContent: "center" }}>
                                                {shortlisted.has(profile.userId) ? "⭐" : "☆"}
                                            </button>
                                            {profile.isVerified && <div style={{ position: "absolute", bottom: 42, left: 10, background: "rgba(255,255,255,0.9)", color: "#2563eb", fontSize: 10, fontWeight: 700, padding: "2px 7px", borderRadius: 99 }}>✅ Verified</div>}
                                            <div style={{ position: "absolute", bottom: 10, left: 12 }}>
                                                <p style={{ fontWeight: 800, fontSize: 16, color: "white" }}>{profile.firstName}, {profile.age}</p>
                                                <p style={{ fontSize: 11, color: "rgba(255,255,255,0.8)" }}>📍 {profile.city}</p>
                                            </div>
                                        </div>
                                        <div style={{ padding: "12px 14px" }}>
                                            <div style={{ display: "flex", flexWrap: "wrap", gap: 5, marginBottom: 10 }}>
                                                {[profile.profession, profile.religion, profile.education].filter(Boolean).map(tag => (
                                                    <span key={tag} style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: 99, padding: "2px 8px", fontSize: 10, color: "#475569", fontWeight: 600 }}>{tag}</span>
                                                ))}
                                            </div>
                                            <div style={{ display: "flex", gap: 6 }}>
                                                <button onClick={() => handleInterest(profile.userId)} style={{ flex: 1, padding: "9px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", border: "none", borderRadius: 10, fontWeight: 700, fontSize: 12, cursor: "pointer" }}>
                                                    💌 Interest
                                                </button>
                                                <Link href={`/profile/${profile.userId}`} style={{ display: "flex", alignItems: "center", justifyContent: "center", width: 38, background: "#f8fafc", border: "1.5px solid #e2e8f0", borderRadius: 10, fontSize: 16, textDecoration: "none" }}>👁</Link>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </>
                    )}
                </div>
            </div>

            <style>{`
        @media (max-width: 768px) {
          .search-layout { grid-template-columns: 1fr !important; }
        }
      `}</style>
        </div>
    );
}
