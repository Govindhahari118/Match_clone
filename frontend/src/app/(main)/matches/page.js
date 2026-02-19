"use client";

import { useState, useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { toast } from "react-toastify";
import { useGuestScrollGate } from "../../../hooks/useGuestScrollGate";
import LoginPromptModal from "../../../components/LoginPromptModal";

const RELIGIONS = ["Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist", "Any"];
const CASTES = ["Any", "Brahmin", "Kshatriya", "Vaisya", "Kayastha", "Rajput", "Reddy", "Naidu", "Chettiar", "Jat", "Maratha", "Vanniyar", "Yadav", "Ezhava", "Nair", "SC", "ST", "OBC", "General"];

const SUBCASTE_MAP = {
    "Brahmin": ["Iyer", "Iyengar", "Gaur", "Saraswat", "Maithil", "Kanyakubja", "Nambootiri", "Deshastha", "Smartha"],
    "Kshatriya": ["Rajput", "Maratha", "Thakur", "Raghuvanshi"],
    "Vaisya": ["Gupta", "Agarwal", "Maheshwari", "Khandelwal", "Jain (Vania)"],
    "Kayastha": ["Saxena", "Srivastava", "Mathur", "Nigam", "Bose", "Ghosh", "Dutta"],
    "Rajput": ["Chauhan", "Rathore", "Sisodia", "Parmar", "Solanki"],
    "Reddy": ["Pokanati", "Motati", "Gudati", "Pakanati", "Chowdary"],
    "Naidu": ["Kamma", "Kapu", "Balija", "Gavara", "Velama"],
    "Chettiar": ["Nattukottai", "Vaniya", "24 Manai"],
    "Jat": ["Sidhu", "Gill", "Mann", "Sandhu", "Grewal", "Dahiya"],
    "Maratha": ["96 Kuli", "Deshmukh", "Patil", "Pawar"],
    "Vanniyar": ["Padayachi", "Gounder"],
    "Yadav": ["Ahir", "Gwal", "Krishnauth"],
    "Ezhava": ["Thiyya"],
    "Nair": ["Menon", "Pillai", "Kurup", "Nambiar"],
    "Any": []
};

const NAKSHATRAS = [
    "Any", "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra", "Punarvasu", "Pushya", "Ashlesha",
    "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Mula",
    "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada", "Uttara Bhadrapada", "Revati"
];

const RASHIS = [
    "Any", "Aries (Mesha)", "Taurus (Vrishabha)", "Gemini (Mithuna)", "Cancer (Karka)", "Leo (Simha)", "Virgo (Kanya)",
    "Libra (Tula)", "Scorpio (Vrishchika)", "Sagittarius (Dhanu)", "Capricorn (Makara)", "Aquarius (Kumbha)", "Pisces (Meena)"
];

const GOTRAS = [
    "Any", "Bharadwaj", "Kashyap", "Shandilya", "Vashishtha", "Vishwamitra", "Garga", "Atri", "Gautam",
    "Harita", "Jamadagni", "Kaushik", "Srivatsa", "Kaundinya", "Moudgalya", "Parashara", "Agastya", "Bhrigu", "Angirasa"
];

const DOSHAS = ["Any", "No", "Manglik", "Sarpa Dosha", "Don't Know"];

const INCOMES = ["Any", "Below 3L", "3-5L", "5-10L", "10-25L", "25-50L", "50L+"];
const M_STATUS = ["Any", "Never Married", "Divorced", "Widowed", "Awaiting Divorce"];
const DIETS = ["Any", "Vegetarian", "Non-Vegetarian", "Vegan", "Eggetarian"];
const TONGUES = ["Any", "Hindi", "English", "Marathi", "Tamil", "Telugu", "Kannada", "Malayalam", "Bengali", "Gujarati", "Punjabi", "Urdu"];
const CITIES = ["Any", "Mumbai", "Delhi", "Bangalore", "Hyderabad", "Chennai", "Kolkata", "Pune", "Jaipur", "Ahmedabad", "Surat", "Lucknow", "Kanpur", "Nagpur", "Indore", "Thane", "Bhopal", "Visakhapatnam", "Patna", "Vadodara", "Ghaziabad", "Ludhiana", "Agra", "Nashik", "Faridabad", "Meerut", "Rajkot", "Varanasi", "Srinagar"];
const EDUCATIONS = ["Any", "MBBS", "MD", "B.Tech", "M.Tech", "MBA", "CA", "B.Arch", "B.Des", "LLB", "PhD", "IAS", "IPS", "B.Com", "M.Com", "B.Sc", "M.Sc", "Other"];
const PROFESSIONS = ["Any", "Doctor", "Engineer", "Software Engineer", "Architect", "Designer", "Lawyer", "Teacher", "Professor", "Govt. Officer", "Business", "Student", "Self Employed", "Other"];

// Generate Heights (4'5" to 7'0")
const HEIGHTS = [];
for (let cm = 135; cm <= 213; cm++) {
    const ft = Math.floor(cm / 30.48);
    const inch = Math.round((cm / 2.54) % 12);
    const displayFt = inch === 12 ? ft + 1 : ft;
    const displayInch = inch === 12 ? 0 : inch;
    HEIGHTS.push({ label: `${displayFt}'${displayInch}" (${cm}cm)`, value: cm });
}

const MOCK = [
    { userId: "u1", firstName: "Priya", age: 26, city: "Mumbai", profession: "Doctor", religion: "Hindu", caste: "Brahmin", subCaste: "Iyer", gothra: "Bharadwaj", nakshatra: "Rohini", rashi: "Taurus (Vrishabha)", dosha: "No", photo: "https://randomuser.me/api/portraits/women/44.jpg", isVerified: true, match: 94, education: "MBBS", income: "10-25L", height: 165, maritalStatus: "Never Married", motherTongue: "Tamil", foodHabit: "Vegetarian" },
    { userId: "u2", firstName: "Ananya", age: 24, city: "Bangalore", profession: "Engineer", religion: "Hindu", caste: "Reddy", subCaste: "Pokanati", gothra: "Kashyap", nakshatra: "Ashwini", rashi: "Aries (Mesha)", dosha: "Manglik", photo: "https://randomuser.me/api/portraits/women/45.jpg", isVerified: true, match: 89, education: "B.Tech", income: "10-25L", height: 180, maritalStatus: "Never Married", motherTongue: "Telugu", foodHabit: "Non-Vegetarian" },
    { userId: "u3", firstName: "Kavya", age: 27, city: "Chennai", profession: "CA", religion: "Hindu", caste: "Naidu", subCaste: "Kamma", gothra: "Vashishtha", nakshatra: "Swati", rashi: "Libra (Tula)", dosha: "No", photo: "https://randomuser.me/api/portraits/women/46.jpg", isVerified: false, match: 85, education: "CA", income: "5-10L", height: 162, maritalStatus: "Never Married", motherTongue: "Tamil", foodHabit: "Eggetarian" },
    { userId: "u4", firstName: "Riya", age: 25, city: "Pune", profession: "Architect", religion: "Hindu", caste: "Kayastha", subCaste: "Saxena", gothra: "Shandilya", nakshatra: "Revati", rashi: "Pisces (Meena)", dosha: "No", photo: "https://randomuser.me/api/portraits/women/47.jpg", isVerified: true, match: 82, education: "B.Arch", income: "5-10L", height: 158, maritalStatus: "Never Married", motherTongue: "Marathi", foodHabit: "Vegetarian" },
    { userId: "u5", firstName: "Simran", age: 28, city: "Delhi", profession: "Lawyer", religion: "Sikh", caste: "Jat", subCaste: "Sidhu", gothra: "Garga", nakshatra: "Pushya", rashi: "Cancer (Karka)", dosha: "No", photo: "https://randomuser.me/api/portraits/women/48.jpg", isVerified: true, match: 79, education: "LLB", income: "10-25L", height: 170, maritalStatus: "Divorced", motherTongue: "Punjabi", foodHabit: "Non-Vegetarian" },
    { userId: "u6", firstName: "Naina", age: 23, city: "Hyderabad", profession: "UI Designer", religion: "Hindu", caste: "Reddy", subCaste: "Motati", gothra: "Atri", nakshatra: "Bharani", rashi: "Aries (Mesha)", dosha: "Manglik", photo: "https://randomuser.me/api/portraits/women/49.jpg", isVerified: false, match: 75, education: "B.Des", income: "3-5L", height: 155, maritalStatus: "Never Married", motherTongue: "Telugu", foodHabit: "Non-Vegetarian" },
    { userId: "u7", firstName: "Meera", age: 29, city: "Jaipur", profession: "Govt. Officer", religion: "Hindu", caste: "Brahmin", subCaste: "Gaur", gothra: "Gautam", nakshatra: "Mula", rashi: "Sagittarius (Dhanu)", dosha: "No", photo: "https://randomuser.me/api/portraits/women/50.jpg", isVerified: true, match: 72, education: "IAS", income: "5-10L", height: 163, maritalStatus: "Never Married", motherTongue: "Hindi", foodHabit: "Vegetarian" },
    { userId: "u8", firstName: "Shreya", age: 26, city: "Kolkata", profession: "Teacher", religion: "Hindu", caste: "Kayastha", subCaste: "Bose", gothra: "Kashyap", nakshatra: "Chitra", rashi: "Libra (Tula)", dosha: "No", photo: "https://randomuser.me/api/portraits/women/51.jpg", isVerified: false, match: 68, education: "M.Ed", income: "3-5L", height: 157, maritalStatus: "Never Married", motherTongue: "Bengali", foodHabit: "Non-Vegetarian" },
];

function ProfileCard({ profile, onInterest, onShortlist, shortlisted, viewMode = "grid" }) {
    const [imgLoaded, setImgLoaded] = useState(false);
    const [hovered, setHovered] = useState(false);
    const matchColor = profile.match >= 90 ? "#10b981" : profile.match >= 80 ? "#e11d48" : "#f59e0b";

    if (viewMode === "list") {
        return (
            <div
                onMouseEnter={() => setHovered(true)}
                onMouseLeave={() => setHovered(false)}
                style={{
                    display: "flex", background: "white", borderRadius: 24, overflow: "hidden", border: "1.5px solid #f1f5f9",
                    boxShadow: hovered ? "0 20px 50px rgba(0,0,0,0.08)" : "0 2px 12px rgba(0,0,0,0.05)",
                    transform: hovered ? "translateY(-4px)" : "translateY(0)",
                    transition: "all 0.3s", cursor: "pointer", height: 220
                }}
            >
                <div style={{ width: "220px", position: "relative", flexShrink: 0 }}>
                    {!imgLoaded && <div className="skeleton" style={{ position: "absolute", inset: 0 }}></div>}
                    <img src={profile.photo} alt={profile.firstName} onLoad={() => setImgLoaded(true)} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                    <div style={{ position: "absolute", top: 12, left: 12, background: matchColor, color: "white", fontSize: 11, fontWeight: 800, padding: "5px 12px", borderRadius: 99 }}>{profile.match}%</div>
                </div>
                <div style={{ flex: 1, padding: 20, display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
                    <div>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start" }}>
                            <div>
                                <h3 style={{ fontWeight: 800, fontSize: 20, color: "#111827" }}>{profile.firstName}, {profile.age}</h3>
                                <p style={{ fontSize: 13, color: "#64748b", marginTop: 4 }}>{profile.profession} • {profile.city} • {profile.caste}</p>
                            </div>
                            <button onClick={(e) => { e.stopPropagation(); onShortlist(profile.userId); }} style={{ fontSize: 22, background: "none", border: "none", cursor: "pointer" }}>{shortlisted ? "⭐" : "☆"}</button>
                        </div>
                        <div style={{ display: "flex", flexWrap: "wrap", gap: 8, marginTop: 12 }}>
                            <span style={{ fontSize: 11, background: "#f1f5f9", padding: "6px 10px", borderRadius: 8, color: "#475569", fontWeight: 600 }}>{profile.education}</span>
                            <span style={{ fontSize: 11, background: "#f1f5f9", padding: "6px 10px", borderRadius: 8, color: "#475569", fontWeight: 600 }}>{profile.income}</span>
                        </div>
                    </div>
                    <div style={{ display: "flex", gap: 12 }}>
                        <button onClick={() => onInterest(profile.userId)} style={{ flex: 1, padding: "10px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", border: "none", borderRadius: 12, fontWeight: 700 }}>💌 Send Interest</button>
                        <Link href={`/profile/${profile.userId}`} style={{ padding: "0 20px", display: "flex", alignItems: "center", border: "1px solid #e2e8f0", borderRadius: 12, textDecoration: "none", color: "#64748b", fontWeight: 600 }}>View</Link>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                background: "white", borderRadius: 24, overflow: "hidden", border: "1.5px solid #f1f5f9",
                boxShadow: hovered ? "0 20px 50px rgba(0,0,0,0.12)" : "0 2px 12px rgba(0,0,0,0.05)",
                transform: hovered ? "translateY(-6px) scale(1.01)" : "translateY(0) scale(1)",
                transition: "all 0.3s cubic-bezier(0.34,1.56,0.64,1)",
                cursor: "pointer",
            }}
        >
            <div style={{ position: "relative", height: 260, overflow: "hidden" }}>
                {!imgLoaded && <div className="skeleton" style={{ position: "absolute", inset: 0 }}></div>}
                <img src={profile.photo} alt={profile.firstName} onLoad={() => setImgLoaded(true)} style={{ width: "100%", height: "100%", objectFit: "cover", transform: hovered ? "scale(1.08)" : "scale(1)", transition: "transform 0.5s ease", opacity: imgLoaded ? 1 : 0 }} />
                <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(0,0,0,0.85) 0%, rgba(0,0,0,0.1) 60%, transparent 100%)" }}></div>

                <div style={{ position: "absolute", top: 12, left: 12, right: 12, display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                    <div style={{ background: matchColor, color: "white", fontSize: 11, fontWeight: 800, padding: "5px 12px", borderRadius: 99, boxShadow: "0 2px 10px rgba(0,0,0,0.3)" }}>{profile.match}% Match</div>
                    <button onClick={(e) => { e.stopPropagation(); onShortlist(profile.userId); }} style={{ width: 36, height: 36, borderRadius: "50%", background: shortlisted ? "#f59e0b" : "rgba(255,255,255,0.9)", border: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 18, transition: "all 0.2s" }}>{shortlisted ? "⭐" : "☆"}</button>
                </div>

                <div style={{ position: "absolute", bottom: 0, left: 0, right: 0, padding: "16px" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 4 }}>
                        <h3 style={{ fontWeight: 800, fontSize: 18, color: "white" }}>{profile.firstName}, {profile.age}</h3>
                        {profile.isVerified && <span style={{ fontSize: 14 }}>✅</span>}
                    </div>
                    <p style={{ fontSize: 13, color: "rgba(255,255,255,0.9)", fontWeight: 500 }}>{profile.profession} • {profile.city}</p>
                    <p style={{ fontSize: 12, color: "rgba(255,255,255,0.7)", marginTop: 2 }}>{profile.caste} {profile.subCaste ? `(${profile.subCaste})` : ""} • {profile.height}cm</p>
                </div>
            </div>

            <div style={{ padding: 16 }}>
                <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginBottom: 16 }}>
                    <span style={{ fontSize: 11, background: "#f1f5f9", padding: "4px 8px", borderRadius: 6, color: "#475569", fontWeight: 600 }}>{profile.education}</span>
                    <span style={{ fontSize: 11, background: "#f1f5f9", padding: "4px 8px", borderRadius: 6, color: "#475569", fontWeight: 600 }}>{profile.income}</span>
                    <span style={{ fontSize: 11, background: "#f1f5f9", padding: "4px 8px", borderRadius: 6, color: "#475569", fontWeight: 600 }}>{profile.gothra || profile.nakshatra}</span>
                </div>
                <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: 10 }}>
                    <button onClick={() => onInterest(profile.userId)} style={{ padding: "10px", background: "linear-gradient(135deg, #e11d48, #c2185b)", color: "white", border: "none", borderRadius: 12, fontWeight: 700, fontSize: 14, cursor: "pointer", transition: "transform 0.1s" }} onMouseDown={e => e.currentTarget.style.transform = "scale(0.96)"} onMouseUp={e => e.currentTarget.style.transform = "scale(1)"}>💌 Send Interest</button>
                    <Link href={`/profile/${profile.userId}`} style={{ display: "flex", alignItems: "center", justifyContent: "center", width: 42, background: "#f1f5f9", border: "1px solid #e2e8f0", borderRadius: 12, fontSize: 18, color: "#475569", textDecoration: "none" }}>👁</Link>
                </div>
            </div>
        </div>
    );
}

function MatchesContent() {
    const searchParams = useSearchParams();
    const [matches, setMatches] = useState([]);
    const [loading, setLoading] = useState(true);
    const [shortlisted, setShortlisted] = useState(new Set());
    const [viewMode, setViewMode] = useState("grid");

    // Filters State
    const [filters, setFilters] = useState({
        minAge: 18, maxAge: 40,
        religion: "Any", caste: "Any", subCaste: "Any",
        gothra: "Any", nakshatra: "Any", rashi: "Any", dosha: "Any",
        income: "Any", city: "Any",
        education: "Any", profession: "Any",
        minHeight: 140, maxHeight: 200,
        maritalStatus: "Any", motherTongue: "Any", diet: "Any"
    });

    const { showLoginModal, setShowLoginModal } = useGuestScrollGate();

    useEffect(() => {
        const init = { ...filters };
        if (searchParams.get("religion")) init.religion = searchParams.get("religion");
        setFilters(init);
        // Simulate API
        setTimeout(() => {
            setMatches(MOCK);
            setLoading(false);
        }, 800);
    }, [searchParams]);

    const handleFilterChange = (key, value) => {
        setFilters(prev => {
            const next = { ...prev, [key]: value };
            if (key === "caste") {
                next.subCaste = "Any"; // Reset subcaste on caste change
            }
            return next;
        });
    };

    // Subcaste Options
    const subcasteOptions = filters.caste && SUBCASTE_MAP[filters.caste] ? ["Any", ...SUBCASTE_MAP[filters.caste]] : ["Any"];

    const filteredMatches = matches.filter(p => {
        if (p.age < filters.minAge || p.age > filters.maxAge) return false;
        if (filters.religion !== "Any" && p.religion !== filters.religion) return false;
        if (filters.caste !== "Any" && p.caste !== filters.caste) return false;
        if (filters.subCaste !== "Any" && p.subCaste?.toLowerCase() !== filters.subCaste.toLowerCase()) return false;

        // Horoscope Filters
        if (filters.gothra !== "Any" && p.gothra !== filters.gothra) return false;
        if (filters.nakshatra !== "Any" && p.nakshatra !== filters.nakshatra) return false;
        if (filters.rashi !== "Any" && p.rashi !== filters.rashi) return false;
        if (filters.dosha !== "Any" && p.dosha !== filters.dosha) return false;

        if (filters.income !== "Any" && p.income !== filters.income) return false;
        if (filters.city !== "Any" && p.city !== filters.city) return false;
        if (filters.education !== "Any" && !p.education?.includes(filters.education)) return false;
        if (filters.profession !== "Any" && !p.profession?.includes(filters.profession)) return false;
        if (p.height < filters.minHeight || p.height > filters.maxHeight) return false;
        if (filters.maritalStatus !== "Any" && p.maritalStatus !== filters.maritalStatus) return false;
        if (filters.motherTongue !== "Any" && p.motherTongue !== filters.motherTongue) return false;
        if (filters.diet !== "Any" && p.foodHabit !== filters.diet) return false;
        return true;
    });

    const handleInterest = (id) => toast.success("Interest sent successfully! 💌");
    const handleShortlist = (id) => {
        const next = new Set(shortlisted);
        if (next.has(id)) next.delete(id); else next.add(id);
        setShortlisted(next);
        toast.info(next.has(id) ? "Added to shortlisted ⭐" : "Removed from shortlisted");
    };

    return (
        <div style={{ display: "grid", gridTemplateColumns: "280px 1fr", gap: "2rem", alignItems: "start" }} className="matches-layout">
            {/* ══════════ FILTERS SIDEBAR ══════════ */}
            <div style={{ background: "white", borderRadius: 24, padding: "24px", border: "1px solid #e2e8f0", position: "sticky", top: 100 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
                    <h2 style={{ fontSize: 18, fontWeight: 800, color: "#111827", display: "flex", alignItems: "center", gap: 8 }}>
                        <span style={{ fontSize: 18 }}>⚙️</span> Refine Matches
                    </h2>
                    <button onClick={() => window.location.reload()} style={{ fontSize: 12, color: "#e11d48", background: "none", border: "none", cursor: "pointer", fontWeight: 600 }}>Reset All</button>
                </div>

                <div className="filter-scroll" style={{ display: "flex", flexDirection: "column", gap: 20, maxHeight: "calc(100vh - 180px)", overflowY: "auto", paddingRight: 4 }}>
                    {/* Age */}
                    <div className="filter-group">
                        <label className="filter-label">Age Range</label>
                        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                            <input type="number" value={filters.minAge} onChange={e => handleFilterChange("minAge", Number(e.target.value))} className="filter-input-sm" />
                            <span style={{ color: "#94a3b8" }}>-</span>
                            <input type="number" value={filters.maxAge} onChange={e => handleFilterChange("maxAge", Number(e.target.value))} className="filter-input-sm" />
                        </div>
                    </div>

                    {/* Height */}
                    <div className="filter-group">
                        <label className="filter-label">Height</label>
                        <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                            <select value={filters.minHeight} onChange={e => handleFilterChange("minHeight", Number(e.target.value))} className="filter-select">
                                {HEIGHTS.map(h => <option key={`min-${h.value}`} value={h.value}>{h.label}</option>)}
                            </select>
                            <span style={{ fontSize: 12, color: "#94a3b8", textAlign: "center" }}>to</span>
                            <select value={filters.maxHeight} onChange={e => handleFilterChange("maxHeight", Number(e.target.value))} className="filter-select">
                                {HEIGHTS.map(h => <option key={`max-${h.value}`} value={h.value}>{h.label}</option>)}
                            </select>
                        </div>
                    </div>

                    {/* Community */}
                    <div className="filter-group">
                        <label className="filter-label">Religion</label>
                        <select value={filters.religion} onChange={e => handleFilterChange("religion", e.target.value)} className="filter-select">
                            {RELIGIONS.map(r => <option key={r} value={r}>{r}</option>)}
                        </select>
                    </div>

                    <div className="filter-group">
                        <label className="filter-label">Caste / Community</label>
                        <select value={filters.caste} onChange={e => handleFilterChange("caste", e.target.value)} className="filter-select">
                            {CASTES.map(c => <option key={c} value={c}>{c}</option>)}
                        </select>
                    </div>

                    <div className="filter-group">
                        <label className="filter-label">Subcaste</label>
                        <select
                            value={filters.subCaste}
                            onChange={e => handleFilterChange("subCaste", e.target.value)}
                            className="filter-select"
                            disabled={filters.caste === "Any"}
                            style={{ opacity: filters.caste === "Any" ? 0.6 : 1 }}
                        >
                            {filters.caste === "Any" && <option value="Any">Select Caste First</option>}
                            {subcasteOptions.map(sc => <option key={sc} value={sc}>{sc}</option>)}
                        </select>
                    </div>

                    {/* HOROSCOPE */}
                    <div className="filter-group">
                        <label className="filter-label">Gothra</label>
                        <select value={filters.gothra} onChange={e => handleFilterChange("gothra", e.target.value)} className="filter-select">
                            {GOTRAS.map(g => <option key={g} value={g}>{g}</option>)}
                        </select>
                    </div>

                    <div className="filter-group">
                        <label className="filter-label">Nakshatra / Star</label>
                        <select value={filters.nakshatra} onChange={e => handleFilterChange("nakshatra", e.target.value)} className="filter-select">
                            {NAKSHATRAS.map(n => <option key={n} value={n}>{n}</option>)}
                        </select>
                    </div>

                    <div className="filter-group">
                        <label className="filter-label">Rashi / Zodiac</label>
                        <select value={filters.rashi} onChange={e => handleFilterChange("rashi", e.target.value)} className="filter-select">
                            {RASHIS.map(r => <option key={r} value={r}>{r}</option>)}
                        </select>
                    </div>

                    <div className="filter-group">
                        <label className="filter-label">Manglik / Dosha</label>
                        <select value={filters.dosha} onChange={e => handleFilterChange("dosha", e.target.value)} className="filter-select">
                            {DOSHAS.map(d => <option key={d} value={d}>{d}</option>)}
                        </select>
                    </div>

                    {/* Personal */}
                    <div className="filter-group">
                        <label className="filter-label">Mother Tongue</label>
                        <select value={filters.motherTongue} onChange={e => handleFilterChange("motherTongue", e.target.value)} className="filter-select">
                            {TONGUES.map(t => <option key={t} value={t}>{t}</option>)}
                        </select>
                    </div>

                    <div className="filter-group">
                        <label className="filter-label">Marital Status</label>
                        <select value={filters.maritalStatus} onChange={e => handleFilterChange("maritalStatus", e.target.value)} className="filter-select">
                            {M_STATUS.map(s => <option key={s} value={s}>{s}</option>)}
                        </select>
                    </div>

                    <div className="filter-group">
                        <label className="filter-label">Diet</label>
                        <select value={filters.diet} onChange={e => handleFilterChange("diet", e.target.value)} className="filter-select">
                            {DIETS.map(d => <option key={d} value={d}>{d}</option>)}
                        </select>
                    </div>

                    <div className="filter-group">
                        <label className="filter-label">Income</label>
                        <select value={filters.income} onChange={e => handleFilterChange("income", e.target.value)} className="filter-select">
                            {INCOMES.map(i => <option key={i} value={i}>{i}</option>)}
                        </select>
                    </div>

                    <div className="filter-group">
                        <label className="filter-label">Education</label>
                        <select value={filters.education} onChange={e => handleFilterChange("education", e.target.value)} className="filter-select">
                            {EDUCATIONS.map(e => <option key={e} value={e}>{e}</option>)}
                        </select>
                    </div>

                    <div className="filter-group">
                        <label className="filter-label">Profession</label>
                        <select value={filters.profession} onChange={e => handleFilterChange("profession", e.target.value)} className="filter-select">
                            {PROFESSIONS.map(p => <option key={p} value={p}>{p}</option>)}
                        </select>
                    </div>

                    <div className="filter-group">
                        <label className="filter-label">City</label>
                        <select value={filters.city} onChange={e => handleFilterChange("city", e.target.value)} className="filter-select">
                            {CITIES.map(c => <option key={c} value={c}>{c}</option>)}
                        </select>
                    </div>
                </div>

                <style>{`
                    .filter-group { display: flex; flexDirection: column; gap: 6px; }
                    .filter-label { font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; }
                    .filter-select { padding: 10px; border-radius: 10px; border: 1px solid #cbd5e1; font-size: 14px; color: #334155; width: 100%; outline: none; background: #f8fafc; }
                    .filter-select:focus { border-color: #e11d48; background: white; box-shadow: 0 0 0 2px rgba(225, 29, 72, 0.1); }
                    .filter-input-sm { padding: 8px; border-radius: 8px; border: 1px solid #cbd5e1; font-size: 14px; width: 100%; text-align: center; }
                    .filter-scroll::-webkit-scrollbar { width: 4px; }
                    .filter-scroll::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
                `}</style>
            </div>

            {/* ══════════ RESULTS ══════════ */}
            <div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 24 }}>
                    <div>
                        <h1 style={{ fontSize: 26, fontWeight: 900, color: "#111827", marginBottom: 4 }}>My Matches 💖</h1>
                        <p style={{ color: "#64748b", fontSize: 15 }}>{filteredMatches.length} compatible profiles found</p>
                    </div>
                    <div style={{ display: "flex", gap: 8, background: "#f1f5f9", padding: 4, borderRadius: 10 }}>
                        <button onClick={() => setViewMode("grid")} style={{ padding: 8, borderRadius: 8, border: "none", background: viewMode === "grid" ? "white" : "transparent", boxShadow: viewMode === "grid" ? "0 2px 5px rgba(0,0,0,0.05)" : "none", cursor: "pointer", color: viewMode === "grid" ? "#e11d48" : "#64748b" }}>
                            <span style={{ fontSize: 18 }}>田</span>
                        </button>
                        <button onClick={() => setViewMode("list")} style={{ padding: 8, borderRadius: 8, border: "none", background: viewMode === "list" ? "white" : "transparent", boxShadow: viewMode === "list" ? "0 2px 5px rgba(0,0,0,0.05)" : "none", cursor: "pointer", color: viewMode === "list" ? "#e11d48" : "#64748b" }}>
                            <span style={{ fontSize: 18 }}>☰</span>
                        </button>
                    </div>
                </div>

                {loading ? (
                    <div style={{ padding: "4rem", textAlign: "center" }}>
                        <div style={{ width: 40, height: 40, border: "4px solid #fce7f3", borderTopColor: "#e11d48", borderRadius: "50%", animation: "spin 1s linear infinite", margin: "0 auto 16px" }}></div>
                        <p style={{ color: "#94a3b8" }}>Finding your soulmate...</p>
                        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
                    </div>
                ) : (
                    <div style={{ display: viewMode === "grid" ? "grid" : "flex", flexDirection: viewMode === "grid" ? "row" : "column", gridTemplateColumns: viewMode === "grid" ? "repeat(auto-fill, minmax(280px, 1fr))" : "none", gap: "1.5rem" }}>
                        {filteredMatches.map(profile => (
                            <ProfileCard
                                key={profile.userId}
                                profile={profile}
                                onInterest={handleInterest}
                                onShortlist={handleShortlist}
                                shortlisted={shortlisted.has(profile.userId)}
                                viewMode={viewMode}
                            />
                        ))}
                    </div>
                )}

                {filteredMatches.length === 0 && !loading && (
                    <div style={{ padding: "4rem", textAlign: "center", background: "white", borderRadius: 24, border: "1px dashed #cbd5e1" }}>
                        <div style={{ fontSize: 48, marginBottom: 16 }}>🔍</div>
                        <h3 style={{ fontSize: 18, fontWeight: 800, color: "#111827" }}>No matches found</h3>
                        <p style={{ color: "#64748b", marginTop: 8 }}>Try adjusting your filters to see more results.</p>
                        <button onClick={() => setFilters({ minAge: 18, maxAge: 40, religion: "Any", caste: "Any", subCaste: "Any", gothra: "Any", nakshatra: "Any", rashi: "Any", dosha: "Any", income: "Any", city: "Any", education: "Any", profession: "Any", minHeight: 140, maxHeight: 200, maritalStatus: "Any", motherTongue: "Any", diet: "Any" })} style={{ marginTop: 24, padding: "10px 24px", background: "#f1f5f9", color: "#334155", border: "none", borderRadius: 99, fontWeight: 700, cursor: "pointer" }}>Clear Filters</button>
                    </div>
                )}
            </div>

            <LoginPromptModal isOpen={showLoginModal} onClose={() => setShowLoginModal(false)} />

            <style>{`
                @media (max-width: 1024px) {
                    .matches-layout { grid-template-columns: 1fr !important; }
                }
            `}</style>
        </div>
    );
}

export default function MatchesPage() {
    return (
        <Suspense fallback={<div>Loading...</div>}>
            <MatchesContent />
        </Suspense>
    );
}
