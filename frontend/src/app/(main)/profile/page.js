"use client";

import { useState, useEffect } from "react";
import api from "../../../services/api";
import Link from "next/link";
import { toast } from "react-toastify";

const MOCK_PROFILE = {
    firstName: "Lakshya", lastName: "Singh", dateOfBirth: "1998-04-15",
    city: "Mumbai", state: "Maharashtra", country: "India",
    religion: "Hindu", caste: "Rajput", motherTongue: "Hindi",
    profession: "Software Engineer", educationLevel: "B.Tech", company: "TechCorp",
    incomeBand: "10-25L", maritalStatus: "Never Married", heightCm: 178,
    bio: "Hey there! I'm a software engineer who loves music, travel, and trying new cuisines. Looking for a life partner who shares similar values and is open to building a beautiful life together.",
    photos: [
        { id: "p1", photoUrl: "https://randomuser.me/api/portraits/men/32.jpg", isPrimary: true },
    ],
    hobbies: ["Music 🎵", "Travelling ✈️", "Cricket 🏏", "Cooking 🍳", "Hiking 🏕️"],
    isVerified: false,
    completionPct: 78,
};

const INFO_SECTIONS = [
    {
        title: "Basic Info", icon: "👤",
        fields: [
            { label: "Full Name", key: "name" },
            { label: "Age", key: "age" },
            { label: "Height", key: "height" },
            { label: "Marital Status", key: "maritalStatus" },
        ],
    },
    {
        title: "Community", icon: "🙏",
        fields: [
            { label: "Religion", key: "religion" },
            { label: "Caste", key: "caste" },
            { label: "Mother Tongue", key: "motherTongue" },
        ],
    },
    {
        title: "Career", icon: "💼",
        fields: [
            { label: "Profession", key: "profession" },
            { label: "Education", key: "educationLevel" },
            { label: "Company", key: "company" },
            { label: "Income", key: "incomeBand" },
        ],
    },
    {
        title: "Location", icon: "📍",
        fields: [
            { label: "City", key: "city" },
            { label: "State", key: "state" },
            { label: "Country", key: "country" },
        ],
    },
    {
        title: "Horoscope & Media", icon: "🔮",
        fields: [
            { label: "Video URL", key: "videoUrl" },
            { label: "Birth Time", key: "birthTime" },
            { label: "Birth Place", key: "birthPlace" },
            { label: "Zodiac", key: "zodiacSign" },
            { label: "Gothra", key: "gothra" },
            { label: "Nakshatra", key: "nakshatra" },
        ],
    },
];

function CompletionBar({ pct }) {
    const color = pct >= 90 ? "#10b981" : pct >= 60 ? "#f59e0b" : "#e11d48";
    return (
        <div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
                <span style={{ fontSize: 12, fontWeight: 700, color: "#64748b" }}>Profile Completion</span>
                <span style={{ fontSize: 12, fontWeight: 800, color }}>{pct}%</span>
            </div>
            <div style={{ height: 7, background: "#f1f5f9", borderRadius: 99, overflow: "hidden" }}>
                <div style={{ height: "100%", width: `${pct}%`, background: `linear-gradient(90deg, ${color}, ${color}cc)`, borderRadius: 99, transition: "width 1s ease" }}></div>
            </div>
        </div>
    );
}

export default function ProfilePage() {
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [editing, setEditing] = useState(false);
    const [editData, setEditData] = useState({});

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                const res = await api.get("/user/profile");
                setProfile(res.data);
                setEditData(res.data);
            } catch {
                setProfile(MOCK_PROFILE);
                setEditData(MOCK_PROFILE);
            } finally {
                setLoading(false);
            }
        };
        fetchProfile();
    }, []);

    const handleSave = async () => {
        try {
            await api.put("/user/profile", editData);
            setProfile(editData);
            setEditing(false);
            toast.success("✅ Profile updated!");
        } catch {
            setProfile(editData);
            setEditing(false);
            toast.success("✅ Profile updated! (Demo)");
        }
    };

    const getFieldValue = (p, key) => {
        if (key === "name") return `${p.firstName} ${p.lastName}`;
        if (key === "age") return p.dateOfBirth ? `${new Date().getFullYear() - new Date(p.dateOfBirth).getFullYear()} years` : null;
        if (key === "height") return p.heightCm ? `${p.heightCm} cm` : null;
        if (key === "videoUrl") return p.videoUrl ? "🎥 Video Added" : null;
        return p[key] ? String(p[key]).replace(/_/g, " ") : null;
    };

    if (loading) return (
        <div>
            <div className="skeleton" style={{ height: 180, borderRadius: 20, marginBottom: 48 }}></div>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 2fr", gap: "1.5rem" }}>
                <div className="skeleton" style={{ height: 300, borderRadius: 20 }}></div>
                <div>
                    <div className="skeleton" style={{ height: 160, borderRadius: 20, marginBottom: "1rem" }}></div>
                    <div className="skeleton" style={{ height: 200, borderRadius: 20 }}></div>
                </div>
            </div>
        </div>
    );

    if (!profile) return (
        <div style={{ textAlign: "center", padding: "4rem", background: "white", borderRadius: 24, border: "1.5px solid #f1f5f9" }}>
            <div style={{ fontSize: 56, marginBottom: "1rem" }}>😕</div>
            <h3 style={{ fontWeight: 800, fontSize: 18, color: "#111827", marginBottom: 8 }}>Profile not found</h3>
            <p style={{ color: "#64748b", fontSize: 14, marginBottom: "1.5rem" }}>We couldn't load your profile. Please try again.</p>
            <button onClick={() => window.location.reload()} style={{ padding: "10px 24px", background: "#e11d48", color: "white", border: "none", borderRadius: 999, fontWeight: 700, cursor: "pointer" }}>Retry</button>
        </div>
    );

    const primaryPhoto = profile.photos?.[0]?.photoUrl || profile.photos?.[0]?.thumbnailUrl;

    return (
        <div>
            {/* Cover banner + avatar */}
            <div style={{ position: "relative", marginBottom: 56, borderRadius: 24, overflow: "hidden" }}>
                {/* Cover */}
                <div style={{ height: 180, background: "linear-gradient(135deg, #1a0533, #7c1d6f, #e11d48)", position: "relative" }}>
                    <div style={{ position: "absolute", inset: 0, background: "url(\"data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.04'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E\")" }}></div>
                </div>

                {/* Avatar */}
                <div style={{ position: "absolute", bottom: -44, left: 28 }}>
                    <div style={{ position: "relative" }}>
                        <div style={{ width: 88, height: 88, borderRadius: "50%", border: "4px solid white", overflow: "hidden", background: "linear-gradient(135deg, #e11d48, #8b5cf6)", display: "flex", alignItems: "center", justifyContent: "center", boxShadow: "0 4px 20px rgba(0,0,0,0.15)" }}>
                            {primaryPhoto ? (
                                <img src={primaryPhoto} alt={profile.firstName} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                            ) : (
                                <span style={{ fontSize: 36, fontWeight: 900, color: "white" }}>{profile.firstName?.[0]}</span>
                            )}
                        </div>
                        {profile.isVerified && (
                            <span style={{ position: "absolute", bottom: 2, right: 2, background: "#2563eb", color: "white", fontSize: 12, width: 22, height: 22, borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", border: "2px solid white" }}>✓</span>
                        )}
                    </div>
                </div>

                {/* Edit / Save buttons */}
                <div style={{ position: "absolute", bottom: -36, right: 20, display: "flex", gap: 8 }}>
                    {editing ? (
                        <>
                            <button onClick={() => setEditing(false)} style={{ padding: "8px 18px", background: "white", border: "1.5px solid #e2e8f0", borderRadius: 10, fontWeight: 700, fontSize: 13, cursor: "pointer", color: "#64748b" }}>Cancel</button>
                            <button onClick={handleSave} style={{ padding: "8px 20px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", border: "none", borderRadius: 10, fontWeight: 700, fontSize: 13, cursor: "pointer", boxShadow: "0 4px 12px rgba(225,29,72,0.3)" }}>Save Changes</button>
                        </>
                    ) : (
                        <button onClick={() => setEditing(true)} style={{ padding: "8px 20px", background: "white", border: "1.5px solid #e2e8f0", borderRadius: 10, fontWeight: 700, fontSize: 13, cursor: "pointer", color: "#374151", boxShadow: "0 2px 8px rgba(0,0,0,0.06)" }}>
                            ✏️ Edit Profile
                        </button>
                    )}
                </div>
            </div>

            {/* Name + completion */}
            <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "1.5rem", alignItems: "start", marginBottom: "1.5rem", paddingLeft: 4 }}>
                <div>
                    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                        <h1 style={{ fontWeight: 900, fontSize: 24, color: "#111827" }}>{profile.firstName} {profile.lastName}</h1>
                        {!profile.isVerified && (
                            <Link href="/settings" style={{ fontSize: 11, fontWeight: 700, background: "#fef3c7", color: "#92400e", border: "1px solid #fde68a", padding: "2px 8px", borderRadius: 99, textDecoration: "none" }}>
                                Get Verified →
                            </Link>
                        )}
                    </div>
                    <p style={{ fontSize: 14, color: "#64748b", marginTop: 4 }}>
                        {profile.profession} · {profile.city}, {profile.state}
                    </p>
                </div>
                <div style={{ background: "white", borderRadius: 16, border: "1.5px solid #f1f5f9", padding: "14px 18px", minWidth: 220 }}>
                    <CompletionBar pct={profile.completionPct || 78} />
                    {(profile.completionPct || 78) < 100 && (
                        <p style={{ fontSize: 11, color: "#94a3b8", marginTop: 6 }}>
                            Complete your profile to get <strong style={{ color: "#e11d48" }}>3× more matches</strong>
                        </p>
                    )}
                </div>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "300px 1fr", gap: "1.5rem" }} className="profile-layout">

                {/* ── Left column ── */}
                <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>

                    {/* Photos */}
                    <div style={{ background: "white", borderRadius: 20, border: "1.5px solid #f1f5f9", padding: "1.25rem", boxShadow: "0 2px 10px rgba(0,0,0,0.04)" }}>
                        <h3 style={{ fontWeight: 800, fontSize: 14, color: "#111827", marginBottom: "0.875rem" }}>📸 Photos ({profile.photos?.length || 0})</h3>
                        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
                            {profile.photos?.map((photo, i) => (
                                <div key={photo.id || i} style={{ position: "relative", borderRadius: 12, overflow: "hidden", aspectRatio: "1" }}>
                                    <img src={photo.photoUrl || photo.thumbnailUrl} alt="Photo" style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                                    {photo.isPrimary && <span style={{ position: "absolute", bottom: 5, left: 5, background: "#e11d48", color: "white", fontSize: 9, fontWeight: 800, padding: "1px 6px", borderRadius: 99 }}>Primary</span>}
                                </div>
                            ))}
                            {/* Add photo slot */}
                            <label style={{ borderRadius: 12, border: "2px dashed #e2e8f0", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", aspectRatio: "1", cursor: "pointer", color: "#94a3b8", fontSize: 12, fontWeight: 600, gap: 4, transition: "all 0.2s" }}
                                onMouseEnter={e => { e.currentTarget.style.borderColor = "#fda4af"; e.currentTarget.style.color = "#e11d48"; }}
                                onMouseLeave={e => { e.currentTarget.style.borderColor = "#e2e8f0"; e.currentTarget.style.color = "#94a3b8"; }}
                            >
                                <span style={{ fontSize: 22 }}>+</span>
                                <span>Add Photo</span>
                                <input type="file" style={{ display: "none" }} accept="image/*" onChange={() => toast.info("Photo upload coming soon!")} />
                            </label>
                        </div>
                    </div>

                    {/* Hobbies */}
                    <div style={{ background: "white", borderRadius: 20, border: "1.5px solid #f1f5f9", padding: "1.25rem", boxShadow: "0 2px 10px rgba(0,0,0,0.04)" }}>
                        <h3 style={{ fontWeight: 800, fontSize: 14, color: "#111827", marginBottom: "0.875rem" }}>🎯 Interests & Hobbies</h3>
                        <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
                            {(profile.hobbies || []).map((h, i) => (
                                <span key={i} style={{ background: "#f0fdf4", color: "#166534", border: "1px solid #86efac", borderRadius: 99, padding: "4px 12px", fontSize: 12, fontWeight: 600 }}>{h}</span>
                            ))}
                            {(!profile.hobbies || profile.hobbies.length === 0) && (
                                <p style={{ fontSize: 12, color: "#94a3b8", fontStyle: "italic" }}>No hobbies added yet</p>
                            )}
                            {editing && (
                                <button onClick={() => toast.info("Hobby editor coming soon!")} style={{ borderRadius: 99, border: "1.5px dashed #e2e8f0", padding: "4px 12px", fontSize: 12, fontWeight: 600, color: "#94a3b8", background: "none", cursor: "pointer" }}>+ Add</button>
                            )}
                        </div>
                    </div>

                    {/* Upgrade nudge */}
                    <div style={{ background: "linear-gradient(135deg, #fffbeb, #fef3c7)", border: "1px solid #fde68a", borderRadius: 20, padding: "1.25rem" }}>
                        <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 8 }}>
                            <span style={{ fontSize: 20 }}>👑</span>
                            <p style={{ fontWeight: 800, fontSize: 13, color: "#92400e" }}>Upgrade to Gold</p>
                        </div>
                        <p style={{ fontSize: 12, color: "#78350f", marginBottom: 10, lineHeight: 1.5 }}>View contact numbers, unlock AI match reports, and get priority search ranking.</p>
                        <Link href="/pricing" style={{ display: "block", textAlign: "center", padding: "8px 0", background: "linear-gradient(135deg, #f59e0b, #d97706)", color: "white", borderRadius: 10, fontSize: 12, fontWeight: 800, textDecoration: "none" }}>
                            See Plans →
                        </Link>
                    </div>
                </div>

                {/* ── Right column ── */}
                <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>

                    {/* Bio */}
                    <div style={{ background: "white", borderRadius: 20, border: "1.5px solid #f1f5f9", padding: "1.25rem", boxShadow: "0 2px 10px rgba(0,0,0,0.04)" }}>
                        <h3 style={{ fontWeight: 800, fontSize: 14, color: "#111827", marginBottom: "0.875rem" }}>📝 About Me</h3>
                        {editing ? (
                            <textarea
                                value={editData.bio || ""}
                                onChange={e => setEditData(p => ({ ...p, bio: e.target.value }))}
                                rows={4}
                                placeholder="Write something about yourself..."
                                style={{ width: "100%", padding: "10px 14px", border: "1.5px solid #e2e8f0", borderRadius: 12, fontSize: 13, fontFamily: "inherit", outline: "none", resize: "vertical", lineHeight: 1.6 }}
                            />
                        ) : (
                            <p style={{ fontSize: 14, color: "#374151", lineHeight: 1.7, whiteSpace: "pre-line" }}>{profile.bio || "No bio yet. Click Edit Profile to add one!"}</p>
                        )}
                    </div>

                    {/* Info sections */}
                    {INFO_SECTIONS.map(section => (
                        <div key={section.title} style={{ background: "white", borderRadius: 20, border: "1.5px solid #f1f5f9", padding: "1.25rem", boxShadow: "0 2px 10px rgba(0,0,0,0.04)" }}>
                            <h3 style={{ fontWeight: 800, fontSize: 14, color: "#111827", marginBottom: "1rem", display: "flex", alignItems: "center", gap: 6 }}>
                                <span>{section.icon}</span> {section.title}
                            </h3>
                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.875rem" }}>
                                {section.fields.map(({ label, key }) => {
                                    const value = getFieldValue(profile, key);
                                    if (!value && !editing) return null;
                                    return (
                                        <div key={key}>
                                            <p style={{ fontSize: 10, fontWeight: 700, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 4 }}>{label}</p>
                                            {editing && !["name", "age", "height"].includes(key) ? (
                                                <input
                                                    value={editData[key] || ""}
                                                    onChange={e => setEditData(p => ({ ...p, [key]: e.target.value }))}
                                                    style={{ width: "100%", padding: "7px 11px", border: "1.5px solid #e2e8f0", borderRadius: 9, fontSize: 13, fontFamily: "inherit", outline: "none", background: "#f8fafc" }}
                                                />
                                            ) : (
                                                <p style={{ fontSize: 13, fontWeight: 600, color: "#374151", textTransform: "capitalize" }}>{value || "—"}</p>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            <style>{`
        @media (max-width: 768px) {
          .profile-layout { grid-template-columns: 1fr !important; }
        }
      `}</style>
        </div>
    );
}
