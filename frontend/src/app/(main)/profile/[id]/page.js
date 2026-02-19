"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import api from "../../../../services/api";
import { toast } from "react-toastify";
import Link from "next/link";
import ReviewsSection from "../../../../components/ReviewsSection";

const MOCK_MALE = { firstName: "Rahul", lastName: "Verma", dateOfBirth: "1995-06-12", city: "Bangalore", state: "Karnataka", country: "India", heightCm: 178, profession: "Senior Engineer", educationLevel: "M.Tech", educationField: "Computer Science", company: "Infosys", incomeBand: "15-25L", religion: "Hindu", caste: "Brahmin", subCaste: "Iyer", gothra: "Bharadwaj", nakshatra: "Rohini", rashi: "Taurus (Vrishabha)", dosha: "No", timeOfBirth: "10:30 AM", motherTongue: "Kannada", maritalStatus: "Never Married", foodHabit: "Vegetarian", drinks: "No", smokes: "No", bio: "Hello! I'm a passionate engineer who loves travelling and cooking. Family values are important to me and I'm looking for someone who shares similar interests.", photos: [], hobbies: ["Cooking 🍳", "Travelling ✈️", "Music 🎵", "Badminton 🏸"], user: { isVerified: true }, completionPct: 92 };
const MOCK_FEMALE = { firstName: "Priya", lastName: "Sharma", dateOfBirth: "1997-03-22", city: "Mumbai", state: "Maharashtra", country: "India", heightCm: 163, profession: "UX Designer", educationLevel: "B.Des", educationField: "Design", company: "Razorpay", incomeBand: "10-15L", religion: "Hindu", caste: "Agarwal", subCaste: "Bisa", gothra: "Kashyap", nakshatra: "Ashwini", rashi: "Aries (Mesha)", dosha: "Manglik", timeOfBirth: "02:15 PM", motherTongue: "Hindi", maritalStatus: "Never Married", foodHabit: "Non-Vegetarian", drinks: "Socially", smokes: "No", bio: "Creative soul. Love good food, better conversations, and long road trips. Looking for a genuine connection with someone kind and ambitious.", photos: [], hobbies: ["Design 🎨", "Reading 📚", "Yoga 🧘", "Travelling ✈️", "Photography 📷"], user: { isVerified: false }, completionPct: 85 };

function InfoRow({ label, value }) {
    if (!value) return null;
    return (
        <div style={{ borderBottom: "1px solid #f8fafc", paddingBottom: 12, marginBottom: 12 }}>
            <p style={{ fontSize: 10, fontWeight: 700, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 3 }}>{label}</p>
            <p style={{ fontSize: 13, fontWeight: 600, color: "#374151", textTransform: "capitalize" }}>{String(value).replace(/_/g, " ")}</p>
        </div>
    );
}

export default function UserProfilePage() {
    const { id } = useParams();
    const router = useRouter();
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [sending, setSending] = useState(false);
    const [shortlisted, setShortlisted] = useState(false);
    const [reportModal, setReportModal] = useState(false);
    const [reportReason, setReportReason] = useState("");
    const [activePhoto, setActivePhoto] = useState(0);
    const [horoscope, setHoroscope] = useState(null);

    const handleReport = async () => {
        if (!reportReason) return toast.error("Please select a reason");
        try {
            await api.post("/interactions/report", { reportedUserId: id, reportType: reportReason });
            toast.success("✅ Report submitted. We will review it shortly.");
            setReportModal(false);
        } catch {
            toast.success("✅ Report submitted (Demo)");
            setReportModal(false);
        }
    };

    useEffect(() => {
        const fetch = async () => {
            try {
                const res = await api.get(`/profiles/${id}`);
                setProfile(res.data);

                // Fetch Horoscope Match
                try {
                    const horoRes = await api.get(`/interactions/horoscope/${id}`);
                    setHoroscope(horoRes.data);
                } catch (e) {
                    console.log("Horoscope fetch failed (demo mode)");
                }
            } catch {
                setProfile(id?.length % 2 === 0 ? MOCK_MALE : MOCK_FEMALE);
            } finally {
                setLoading(false);
            }
        };
        if (id) fetch();
    }, [id]);

    const handleInterest = async () => {
        setSending(true);
        try {
            const res = await api.post("/interactions/like", { receiverId: id });
            if (res.data?.matchId) toast.success("🎉 It's a Match! You can now chat.");
            else toast.success("💌 Interest sent successfully!");
        } catch {
            toast.success("💌 Interest sent! (Demo)");
        } finally {
            setSending(false);
        }
    };

    const handleShortlist = async () => {
        try {
            await api.post("/shortlist/add", { profileId: id });
            setShortlisted(true);
            toast.success("⭐ Added to Shortlist!");
        } catch {
            setShortlisted(true);
            toast.success("⭐ Added to Shortlist! (Demo)");
        }
    };

    /* ─ Loading ─ */
    if (loading) return (
        <div>
            <div className="skeleton" style={{ height: 200, borderRadius: 20, marginBottom: 56 }}></div>
            <div style={{ display: "grid", gridTemplateColumns: "280px 1fr", gap: "1.5rem" }}>
                <div className="skeleton" style={{ height: 400, borderRadius: 20 }}></div>
                <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                    <div className="skeleton" style={{ height: 160, borderRadius: 20 }}></div>
                    <div className="skeleton" style={{ height: 240, borderRadius: 20 }}></div>
                </div>
            </div>
        </div>
    );

    if (!profile) return (
        <div style={{ textAlign: "center", padding: "4rem", background: "white", borderRadius: 24, border: "1.5px solid #f1f5f9" }}>
            <div style={{ fontSize: 56, marginBottom: 12 }}>😕</div>
            <h3 style={{ fontWeight: 800, fontSize: 18, color: "#111827", marginBottom: 8 }}>Profile not found</h3>
            <p style={{ color: "#64748b", fontSize: 14, marginBottom: "1.5rem" }}>This profile may have been removed or restricted.</p>
            <button onClick={() => router.back()} style={{ padding: "10px 24px", background: "#e11d48", color: "white", border: "none", borderRadius: 999, fontWeight: 700, cursor: "pointer" }}>← Go Back</button>
        </div>
    );

    const age = new Date().getFullYear() - new Date(profile.dateOfBirth).getFullYear();
    const primaryPhoto = profile.photos?.[activePhoto]?.thumbnailUrl || profile.photos?.[activePhoto]?.photoUrl;

    return (
        <div>
            {/* Back button */}
            <button onClick={() => router.back()} style={{ display: "flex", alignItems: "center", gap: 6, padding: "7px 14px", background: "white", border: "1.5px solid #e2e8f0", borderRadius: 12, fontWeight: 600, fontSize: 12, color: "#374151", cursor: "pointer", marginBottom: "1rem" }}>
                ← Back
            </button>

            {/* Cover + Avatar */}
            <div style={{ position: "relative", marginBottom: 52, borderRadius: 24, overflow: "hidden" }}>
                <div style={{ height: 200, background: "linear-gradient(135deg, #1a0533, #7c1d6f, #e11d48)", position: "relative" }}>
                    <div style={{ position: "absolute", inset: 0, backgroundImage: "radial-gradient(ellipse at 20% 50%, rgba(139,92,246,0.3) 0%, transparent 60%)" }}></div>
                    {/* Decorative hearts */}
                    {["♥", "♥", "♥"].map((h, i) => (
                        <span key={i} style={{ position: "absolute", color: "rgba(255,255,255,0.06)", fontSize: 60 + (i * 30), top: i * 20, right: 40 + i * 120, fontFamily: "serif" }}>{h}</span>
                    ))}
                </div>

                {/* Avatar */}
                <div style={{ position: "absolute", bottom: -40, left: 28 }}>
                    <div style={{ width: 88, height: 88, borderRadius: "50%", border: "4px solid white", overflow: "hidden", background: "linear-gradient(135deg, #e11d48, #8b5cf6)", display: "flex", alignItems: "center", justifyContent: "center", boxShadow: "0 4px 20px rgba(0,0,0,0.15)" }}>
                        {primaryPhoto
                            ? <img src={primaryPhoto} alt={profile.firstName} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                            : <span style={{ fontSize: 32, fontWeight: 900, color: "white" }}>{profile.firstName?.[0]}</span>
                        }
                    </div>
                    {profile.user?.isVerified && (
                        <span style={{ position: "absolute", bottom: 3, right: 3, background: "#2563eb", color: "white", fontSize: 11, width: 22, height: 22, borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", border: "2px solid white" }}>✓</span>
                    )}
                </div>

                {/* Actions (absolute right) */}
                <div style={{ position: "absolute", bottom: -36, right: 20, display: "flex", gap: 8 }}>
                    <button
                        onClick={handleShortlist}
                        disabled={shortlisted}
                        style={{ padding: "8px 16px", background: shortlisted ? "#fffbeb" : "white", border: shortlisted ? "1.5px solid #fde68a" : "1.5px solid #e2e8f0", borderRadius: 12, fontWeight: 700, fontSize: 13, cursor: shortlisted ? "default" : "pointer", color: shortlisted ? "#92400e" : "#374151", transition: "all 0.2s" }}
                    >
                        {shortlisted ? "⭐ Shortlisted" : "⭐ Shortlist"}
                    </button>
                    <button
                        onClick={handleInterest}
                        disabled={sending}
                        style={{ padding: "8px 22px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", border: "none", borderRadius: 12, fontWeight: 700, fontSize: 13, cursor: "pointer", boxShadow: "0 4px 14px rgba(225,29,72,0.3)", transition: "all 0.2s", opacity: sending ? 0.7 : 1 }}
                    >
                        {sending ? "Sending..." : "💌 Send Interest"}
                    </button>
                </div>
            </div>

            {/* Name row */}
            <div style={{ marginBottom: "1.5rem", paddingLeft: 4 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 4 }}>
                    <h1 style={{ fontWeight: 900, fontSize: 24, color: "#111827" }}>{profile.firstName} {profile.lastName}</h1>
                    {profile.user?.isVerified
                        ? <span style={{ fontSize: 11, fontWeight: 700, background: "#dbeafe", color: "#1d4ed8", border: "1px solid #93c5fd", padding: "2px 9px", borderRadius: 99 }}>✅ Verified</span>
                        : <span style={{ fontSize: 11, fontWeight: 700, background: "#f0fdf4", color: "#16a34a", border: "1px solid #86efac", padding: "2px 9px", borderRadius: 99 }}>Profile Active</span>
                    }
                </div>
                <p style={{ fontSize: 14, color: "#64748b" }}>{age} yrs · {profile.heightCm}cm · {profile.profession} · 📍 {profile.city}, {profile.state}</p>
            </div>

            {/* Body grid */}
            <div style={{ display: "grid", gridTemplateColumns: "280px 1fr", gap: "1.5rem" }} className="pid-layout">

                {/* Left col */}
                <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>

                    {/* Photos */}
                    <div style={{ background: "white", borderRadius: 20, border: "1.5px solid #f1f5f9", padding: "1.25rem", boxShadow: "0 2px 10px rgba(0,0,0,0.04)" }}>
                        <h3 style={{ fontWeight: 800, fontSize: 14, color: "#111827", marginBottom: "0.875rem" }}>📸 Photos ({profile.photos?.length || 0})</h3>
                        {primaryPhoto ? (
                            <>
                                <div style={{ borderRadius: 14, overflow: "hidden", aspectRatio: "4/5", marginBottom: 8 }}>
                                    <img src={primaryPhoto} alt="Main" style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                                </div>
                                {profile.photos.length > 1 && (
                                    <div style={{ display: "flex", gap: 6, overflowX: "auto" }}>
                                        {profile.photos.map((p, i) => (
                                            <div key={i} onClick={() => setActivePhoto(i)} style={{ width: 52, height: 60, borderRadius: 10, overflow: "hidden", flexShrink: 0, cursor: "pointer", border: activePhoto === i ? "2px solid #e11d48" : "2px solid transparent" }}>
                                                <img src={p.thumbnailUrl || p.photoUrl} alt="" style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </>
                        ) : (
                            <div style={{ background: "linear-gradient(135deg, #f8fafc, #f1f5f9)", borderRadius: 14, aspectRatio: "4/5", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", color: "#94a3b8" }}>
                                <span style={{ fontSize: 52 }}>👤</span>
                                <p style={{ fontSize: 12, marginTop: 8 }}>No photos uploaded</p>
                            </div>
                        )}
                    </div>

                    {/* Trust */}
                    <div style={{ background: "linear-gradient(135deg, #eff6ff, #dbeafe)", border: "1px solid #bfdbfe", borderRadius: 20, padding: "1.25rem" }}>
                        <h3 style={{ fontWeight: 800, fontSize: 14, color: "#1e3a8a", marginBottom: 12 }}>🔒 Trust Score</h3>
                        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                            <div style={{ fontWeight: 900, fontSize: 36, color: "#2563eb" }}>{profile.completionPct || 85}%</div>
                            <div>
                                <p style={{ fontSize: 11, color: "#3b82f6", fontWeight: 700 }}>Profile Verified</p>
                                <p style={{ fontSize: 10, color: "#64748b", marginTop: 2, lineHeight: 1.5 }}>Based on profile completeness and verification status.</p>
                            </div>
                        </div>
                    </div>

                    {/* Video Profile */}
                    {profile.videoUrl && (
                        <div style={{ background: "black", borderRadius: 20, overflow: "hidden", position: "relative", aspectRatio: "9/16", marginBottom: "1rem" }}>
                            <video
                                src={profile.videoUrl}
                                controls
                                poster={profile.videoThumbnail}
                                style={{ width: "100%", height: "100%", objectFit: "cover" }}
                            />
                            <div style={{ position: "absolute", top: 12, left: 12, background: "rgba(225, 29, 72, 0.9)", color: "white", padding: "4px 10px", borderRadius: 8, fontSize: 10, fontWeight: 800 }}>
                                ▶ VIDEO BIO
                            </div>
                        </div>
                    )}

                    {/* Horoscope Match */}
                    {horoscope && (
                        <div style={{ background: "linear-gradient(135deg, #fdf4ff, #fae8ff)", border: "1px solid #f0abfc", borderRadius: 20, padding: "1.25rem", marginBottom: "1rem" }}>
                            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
                                <h3 style={{ fontWeight: 800, fontSize: 14, color: "#86198f" }}>🔮 Kundli Match</h3>
                                <span style={{ fontSize: 11, fontWeight: 700, color: "#c026d3", background: "white", padding: "2px 8px", borderRadius: 6 }}>{horoscope.tier}</span>
                            </div>
                            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                                <div style={{ width: 48, height: 48, borderRadius: "50%", background: "#f0abfc", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 16, fontWeight: 900, color: "#86198f", border: "3px solid white" }}>
                                    {Math.round(horoscope.score)}
                                </div>
                                <div>
                                    <p style={{ fontSize: 12, color: "#a21caf", fontWeight: 700 }}>Gun Milan Score</p>
                                    <p style={{ fontSize: 10, color: "#d946ef", marginTop: 2 }}>Out of 36 • {horoscope.details?.nadi}</p>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Chat CTA */}
                    <div style={{ background: "linear-gradient(135deg, #fff1f2, #fce7f3)", border: "1px solid #fecdd3", borderRadius: 20, padding: "1.25rem" }}>
                        <p style={{ fontWeight: 800, fontSize: 13, color: "#9f1239", marginBottom: 6 }}>💬 Connected? Start Chatting!</p>
                        <p style={{ fontSize: 11, color: "#be185d", marginBottom: 10, lineHeight: 1.5 }}>Send an interest first. If they accept, you can chat instantly.</p>
                        <Link href="/chat" style={{ display: "block", textAlign: "center", padding: "8px 0", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", borderRadius: 10, fontSize: 12, fontWeight: 800, textDecoration: "none" }}>
                            Go to Chat →
                        </Link>
                    </div>
                </div>

                {/* Right col */}
                <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>

                    {/* Bio */}
                    <div style={{ background: "white", borderRadius: 20, border: "1.5px solid #f1f5f9", padding: "1.5rem", boxShadow: "0 2px 10px rgba(0,0,0,0.04)" }}>
                        <h3 style={{ fontWeight: 800, fontSize: 14, color: "#111827", marginBottom: "0.875rem" }}>📝 About Me</h3>
                        <p style={{ fontSize: 14, color: "#374151", lineHeight: 1.75, whiteSpace: "pre-line" }}>
                            {profile.bio || "Hello! I haven't written a bio yet but I'm excited to find my perfect match here."}
                        </p>
                        {profile.hobbies?.length > 0 && (
                            <div style={{ marginTop: "1rem" }}>
                                <p style={{ fontSize: 11, fontWeight: 700, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 8 }}>Interests & Hobbies</p>
                                <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
                                    {profile.hobbies.map((h, i) => (
                                        <span key={i} style={{ background: "#f0fdf4", color: "#166534", border: "1px solid #86efac", borderRadius: 99, padding: "3px 12px", fontSize: 12, fontWeight: 600 }}>{h}</span>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Horoscope Details */}
                    <div style={{ background: "white", borderRadius: 20, border: "1.5px solid #f1f5f9", padding: "1.5rem", boxShadow: "0 2px 10px rgba(0,0,0,0.04)", marginBottom: "1rem" }}>
                        <h3 style={{ fontWeight: 800, fontSize: 14, color: "#111827", marginBottom: "1rem" }}>🔮 Horoscope Details</h3>
                        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0 2rem" }}>
                            <InfoRow label="Date of Birth" value={new Date(profile.dateOfBirth).toLocaleDateString("en-IN", { day: "numeric", month: "long", year: "numeric" })} />
                            <InfoRow label="Time of Birth" value={profile.timeOfBirth || "Not Specified"} />
                            <InfoRow label="Place of Birth" value={`${profile.city}, ${profile.state}`} />
                            <InfoRow label="Gothra" value={profile.gothra} />
                            <InfoRow label="Rashi (Zodiac)" value={profile.rashi} />
                            <InfoRow label="Nakshatra" value={profile.nakshatra} />
                            <InfoRow label="Manglik Status" value={profile.dosha} />
                        </div>
                    </div>

                    {/* Personal details */}
                    <div style={{ background: "white", borderRadius: 20, border: "1.5px solid #f1f5f9", padding: "1.5rem", boxShadow: "0 2px 10px rgba(0,0,0,0.04)" }}>
                        <h3 style={{ fontWeight: 800, fontSize: 14, color: "#111827", marginBottom: "1rem" }}>ℹ️ Personal Details</h3>
                        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0 2rem" }}>
                            <InfoRow label="Date of Birth" value={new Date(profile.dateOfBirth).toLocaleDateString("en-IN", { day: "numeric", month: "long", year: "numeric" })} />
                            <InfoRow label="Marital Status" value={profile.maritalStatus} />
                            <InfoRow label="Religion" value={profile.religion} />
                            <InfoRow label="Caste" value={profile.caste || "Not specified"} />
                            <InfoRow label="Mother Tongue" value={profile.motherTongue} />
                            <InfoRow label="Food Habit" value={profile.foodHabit} />
                            <InfoRow label="Drinking" value={profile.drinks} />
                            <InfoRow label="Smoking" value={profile.smokes} />
                        </div>

                        <div style={{ borderTop: "1.5px solid #f8fafc", paddingTop: "1rem", marginTop: 4 }}>
                            <h4 style={{ fontWeight: 800, fontSize: 13, color: "#374151", marginBottom: "0.875rem" }}>🎓 Education & Career</h4>
                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0 2rem" }}>
                                <InfoRow label="Education" value={profile.educationLevel} />
                                <InfoRow label="Field" value={profile.educationField} />
                                <InfoRow label="Profession" value={profile.profession} />
                                <InfoRow label="Company" value={profile.company} />
                                <InfoRow label="Income" value={profile.incomeBand} />
                            </div>
                        </div>
                    </div>

                    {/* Contact unlock teaser */}
                    <div style={{ background: "linear-gradient(135deg, #fffbeb, #fef3c7)", border: "1px solid #fde68a", borderRadius: 20, padding: "1.25rem", display: "flex", alignItems: "center", justifyContent: "space-between", gap: 16, flexWrap: "wrap" }}>
                        <div>
                            <p style={{ fontWeight: 800, fontSize: 14, color: "#78350f", marginBottom: 4 }}>📞 View Contact Information</p>
                            <p style={{ fontSize: 12, color: "#92400e", lineHeight: 1.5 }}>Upgrade to see phone number and email address directly.</p>
                        </div>
                        <Link href="/pricing" style={{ padding: "10px 22px", background: "linear-gradient(135deg, #f59e0b, #d97706)", color: "white", borderRadius: 12, fontSize: 13, fontWeight: 800, textDecoration: "none", flexShrink: 0, boxShadow: "0 4px 12px rgba(245,158,11,0.3)" }}>
                            👑 Upgrade Now
                        </Link>
                    </div>

                    {/* Reviews & Ratings */}
                    <ReviewsSection userId={id} userName={profile.firstName} />
                </div>
            </div>

            <style>{`
        @media (max-width: 768px) {
          .pid-layout { grid-template-columns: 1fr !important; }
        }
      `}</style>

            {/* Report Modal */}
            {reportModal && (
                <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.5)", backdropFilter: "blur(4px)", zIndex: 9999, display: "flex", alignItems: "center", justifyContent: "center", padding: "1rem" }}>
                    <div style={{ background: "white", borderRadius: 24, padding: "2rem", maxWidth: 400, width: "100%", boxShadow: "0 20px 60px rgba(0,0,0,0.3)" }}>
                        <h3 style={{ fontWeight: 900, fontSize: 18, color: "#111827", marginBottom: "1rem" }}>🚩 Report User</h3>
                        <p style={{ fontSize: 13, color: "#4b5563", marginBottom: "1rem" }}>Why are you reporting this profile?</p>
                        <select style={{ width: "100%", padding: "10px", borderRadius: 10, border: "1.5px solid #e2e8f0", marginBottom: "1rem", outline: "none" }} onChange={(e) => setReportReason(e.target.value)}>
                            <option value="">Select a reason</option>
                            <option value="fake">Fake Profile / Scammer</option>
                            <option value="harassment">Harassment / Abusive</option>
                            <option value="inappropriate">Inappropriate Content</option>
                            <option value="underage">Underage User</option>
                            <option value="other">Other</option>
                        </select>
                        <div style={{ display: "flex", gap: 10 }}>
                            <button onClick={() => setReportModal(false)} style={{ flex: 1, padding: "10px", background: "#f3f4f6", borderRadius: 10, fontWeight: 700, border: "none", cursor: "pointer" }}>Cancel</button>
                            <button onClick={handleReport} style={{ flex: 1, padding: "10px", background: "#ef4444", color: "white", borderRadius: 10, fontWeight: 700, border: "none", cursor: "pointer" }}>Submit Report</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
