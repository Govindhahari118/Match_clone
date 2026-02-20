"use client";

import Image from "next/image";
import Link from "next/link";
import { Suspense, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { toast } from "react-toastify";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import { useGuestScrollGate } from "../../../hooks/useGuestScrollGate";
import LoginPromptModal from "../../../components/LoginPromptModal";

const RELIGIONS = ["Any", "Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist"];
const CASTES = [
  "Any",
  "Brahmin",
  "Kshatriya",
  "Vaisya",
  "Kayastha",
  "Rajput",
  "Reddy",
  "Naidu",
  "Chettiar",
  "Jat",
  "Maratha",
  "Vanniyar",
  "Yadav",
  "Ezhava",
  "Nair",
  "SC",
  "ST",
  "OBC",
  "General",
];

const SUBCASTE_MAP = {
  Brahmin: ["Iyer", "Iyengar", "Gaur", "Saraswat", "Maithil", "Kanyakubja", "Nambootiri", "Deshastha", "Smartha"],
  Kshatriya: ["Rajput", "Maratha", "Thakur", "Raghuvanshi"],
  Vaisya: ["Gupta", "Agarwal", "Maheshwari", "Khandelwal", "Jain (Vania)"],
  Kayastha: ["Saxena", "Srivastava", "Mathur", "Nigam", "Bose", "Ghosh", "Dutta"],
  Rajput: ["Chauhan", "Rathore", "Sisodia", "Parmar", "Solanki"],
  Reddy: ["Pokanati", "Motati", "Gudati", "Pakanati", "Chowdary"],
  Naidu: ["Kamma", "Kapu", "Balija", "Gavara", "Velama"],
  Chettiar: ["Nattukottai", "Vaniya", "24 Manai"],
  Jat: ["Sidhu", "Gill", "Mann", "Sandhu", "Grewal", "Dahiya"],
  Maratha: ["96 Kuli", "Deshmukh", "Patil", "Pawar"],
  Vanniyar: ["Padayachi", "Gounder"],
  Yadav: ["Ahir", "Gwal", "Krishnauth"],
  Ezhava: ["Thiyya"],
  Nair: ["Menon", "Pillai", "Kurup", "Nambiar"],
  Any: [],
};

const NAKSHATRAS = [
  "Any",
  "Ashwini",
  "Bharani",
  "Krittika",
  "Rohini",
  "Mrigashira",
  "Ardra",
  "Punarvasu",
  "Pushya",
  "Ashlesha",
  "Magha",
  "Purva Phalguni",
  "Uttara Phalguni",
  "Hasta",
  "Chitra",
  "Swati",
  "Vishakha",
  "Anuradha",
  "Jyeshtha",
  "Mula",
  "Purva Ashadha",
  "Uttara Ashadha",
  "Shravana",
  "Dhanishta",
  "Shatabhisha",
  "Purva Bhadrapada",
  "Uttara Bhadrapada",
  "Revati",
];

const RASHIS = [
  "Any",
  "Aries (Mesha)",
  "Taurus (Vrishabha)",
  "Gemini (Mithuna)",
  "Cancer (Karka)",
  "Leo (Simha)",
  "Virgo (Kanya)",
  "Libra (Tula)",
  "Scorpio (Vrishchika)",
  "Sagittarius (Dhanu)",
  "Capricorn (Makara)",
  "Aquarius (Kumbha)",
  "Pisces (Meena)",
];

const GOTRAS = [
  "Any",
  "Bharadwaj",
  "Kashyap",
  "Shandilya",
  "Vashishtha",
  "Vishwamitra",
  "Garga",
  "Atri",
  "Gautam",
  "Harita",
  "Jamadagni",
  "Kaushik",
  "Srivatsa",
  "Kaundinya",
  "Moudgalya",
  "Parashara",
  "Agastya",
  "Bhrigu",
  "Angirasa",
];
const DOSHAS = ["Any", "No", "Manglik", "Sarpa Dosha", "Don't Know"];
const INCOMES = ["Any", "Below 3L", "3-5L", "5-10L", "10-25L", "25-50L", "50L+"];
const M_STATUS = ["Any", "Never Married", "Divorced", "Widowed", "Awaiting Divorce", "Separated"];
const DIETS = ["Any", "Vegetarian", "Non-Vegetarian", "Vegan", "Eggetarian"];
const TONGUES = ["Any", "Hindi", "English", "Marathi", "Tamil", "Telugu", "Kannada", "Malayalam", "Bengali", "Gujarati", "Punjabi", "Urdu"];
const CITIES = [
  "Any",
  "Mumbai",
  "Delhi",
  "Bangalore",
  "Hyderabad",
  "Chennai",
  "Kolkata",
  "Pune",
  "Jaipur",
  "Ahmedabad",
  "Surat",
  "Lucknow",
  "Kanpur",
  "Nagpur",
  "Indore",
  "Thane",
  "Bhopal",
  "Visakhapatnam",
  "Patna",
  "Vadodara",
  "Ghaziabad",
  "Ludhiana",
  "Agra",
  "Nashik",
  "Faridabad",
  "Meerut",
  "Rajkot",
  "Varanasi",
  "Srinagar",
];
const EDUCATIONS = ["Any", "MBBS", "MD", "B.Tech", "M.Tech", "MBA", "CA", "B.Arch", "B.Des", "LLB", "PhD", "IAS", "IPS", "B.Com", "M.Com", "B.Sc", "M.Sc", "Other"];
const PROFESSIONS = ["Any", "Doctor", "Engineer", "Software Engineer", "Architect", "Designer", "Lawyer", "Teacher", "Professor", "Govt. Officer", "Business", "Student", "Self Employed", "Other"];

const MOCK_PROFILES = [
  {
    userId: "u1",
    firstName: "Priya",
    age: 26,
    city: "Mumbai",
    profession: "Doctor",
    religion: "Hindu",
    caste: "Brahmin",
    subCaste: "Iyer",
    gothra: "Bharadwaj",
    nakshatra: "Rohini",
    rashi: "Taurus (Vrishabha)",
    dosha: "No",
    photo: "https://randomuser.me/api/portraits/women/44.jpg",
    isVerified: true,
    match: 94,
    education: "MBBS",
    income: "10-25L",
    height: 165,
    maritalStatus: "Never Married",
    motherTongue: "Tamil",
    foodHabit: "Vegetarian",
  },
  {
    userId: "u2",
    firstName: "Ananya",
    age: 24,
    city: "Bangalore",
    profession: "Engineer",
    religion: "Hindu",
    caste: "Reddy",
    subCaste: "Pokanati",
    gothra: "Kashyap",
    nakshatra: "Ashwini",
    rashi: "Aries (Mesha)",
    dosha: "Manglik",
    photo: "https://randomuser.me/api/portraits/women/45.jpg",
    isVerified: true,
    match: 89,
    education: "B.Tech",
    income: "10-25L",
    height: 160,
    maritalStatus: "Never Married",
    motherTongue: "Telugu",
    foodHabit: "Non-Vegetarian",
  },
  {
    userId: "u3",
    firstName: "Kavya",
    age: 28,
    city: "Hyderabad",
    profession: "Lawyer",
    religion: "Hindu",
    caste: "Kshatriya",
    subCaste: "Thakur",
    gothra: "Atri",
    nakshatra: "Bharani",
    rashi: "Aries (Mesha)",
    dosha: "Don't Know",
    photo: "https://randomuser.me/api/portraits/women/49.jpg",
    isVerified: false,
    match: 75,
    education: "LLB",
    income: "3-5L",
    height: 155,
    maritalStatus: "Never Married",
    motherTongue: "Hindi",
    foodHabit: "Vegetarian",
  },
  {
    userId: "u4",
    firstName: "Meera",
    age: 29,
    city: "Jaipur",
    profession: "Govt. Officer",
    religion: "Hindu",
    caste: "Brahmin",
    subCaste: "Gaur",
    gothra: "Gautam",
    nakshatra: "Mula",
    rashi: "Sagittarius (Dhanu)",
    dosha: "No",
    photo: "https://randomuser.me/api/portraits/women/50.jpg",
    isVerified: true,
    match: 72,
    education: "IAS",
    income: "5-10L",
    height: 163,
    maritalStatus: "Never Married",
    motherTongue: "Hindi",
    foodHabit: "Vegetarian",
  },
  {
    userId: "u5",
    firstName: "Sara",
    age: 27,
    city: "Delhi",
    profession: "Designer",
    religion: "Muslim",
    caste: "Any",
    subCaste: "Any",
    gothra: "Any",
    nakshatra: "Any",
    rashi: "Any",
    dosha: "Any",
    photo: "https://randomuser.me/api/portraits/women/52.jpg",
    isVerified: true,
    match: 83,
    education: "B.Des",
    income: "5-10L",
    height: 158,
    maritalStatus: "Never Married",
    motherTongue: "English",
    foodHabit: "Non-Vegetarian",
  },
  {
    userId: "u6",
    firstName: "Nisha",
    age: 25,
    city: "Pune",
    profession: "Software Engineer",
    religion: "Hindu",
    caste: "Kayastha",
    subCaste: "Saxena",
    gothra: "Kaushik",
    nakshatra: "Hasta",
    rashi: "Virgo (Kanya)",
    dosha: "No",
    photo: "https://randomuser.me/api/portraits/women/31.jpg",
    isVerified: true,
    match: 88,
    education: "B.Tech",
    income: "10-25L",
    height: 162,
    maritalStatus: "Never Married",
    motherTongue: "Hindi",
    foodHabit: "Eggetarian",
  },
];

function clampNumber(value, min, max) {
  const num = Number(value);
  if (Number.isNaN(num)) return min;
  return Math.min(max, Math.max(min, num));
}

function MatchCard({ profile, isShortlisted, onShortlist, onInterest, viewMode }) {
  if (viewMode === "list") {
    return (
      <article className="panel panel-hover anim-rise" style={{ overflow: "hidden", display: "flex" }}>
        <div style={{ width: 190, flexShrink: 0, position: "relative" }}>
          <Image
            src={profile.photo}
            alt={`${profile.firstName} profile`}
            width={720}
            height={900}
            unoptimized
            style={{ width: "100%", height: "100%", objectFit: "cover" }}
          />
        </div>
        <div style={{ padding: "0.95rem", flex: 1, minWidth: 0 }}>
          <div style={{ display: "flex", justifyContent: "space-between", gap: "0.7rem", alignItems: "flex-start" }}>
            <div style={{ minWidth: 0 }}>
              <h3 style={{ margin: 0, fontSize: "1.08rem" }}>
                {profile.firstName}, {profile.age}
              </h3>
              <p style={{ margin: "0.25rem 0 0", color: "var(--ink-muted)", fontSize: "0.88rem" }}>
                {profile.profession} · {profile.city} · {profile.height ? `${profile.height}cm` : "Height NA"}
              </p>
            </div>
            <span className="chip chip-support" style={{ flexShrink: 0 }}>
              {profile.match}% Match
            </span>
          </div>

          <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap", marginTop: "0.65rem" }}>
            {profile.isVerified && <span className="chip chip-support">Verified</span>}
            {profile.religion && <span className="chip chip-brand">{profile.religion}</span>}
            {profile.education && <span className="chip chip-brand">{profile.education}</span>}
            {profile.income && <span className="chip chip-brand">{profile.income}</span>}
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr auto auto", gap: "0.52rem", marginTop: "0.85rem" }}>
            <button type="button" className="button button-primary" onClick={() => onInterest(profile.userId)}>
              Send Interest
            </button>
            <button
              type="button"
              className="button button-secondary"
              onClick={() => onShortlist(profile.userId)}
              style={{ padding: "0.74rem 0.95rem" }}
            >
              {isShortlisted ? "Saved" : "Save"}
            </button>
            <Link href={`/profile/${profile.userId}`} className="button button-secondary" style={{ padding: "0.74rem 0.95rem" }}>
              View
            </Link>
          </div>
        </div>
      </article>
    );
  }

  return (
    <article className="panel panel-hover anim-rise" style={{ overflow: "hidden" }}>
      <div style={{ position: "relative", height: 210 }}>
        <Image
          src={profile.photo}
          alt={`${profile.firstName} profile`}
          width={720}
          height={900}
          unoptimized
          style={{ width: "100%", height: "100%", objectFit: "cover" }}
        />
        <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(13,26,52,0.82), rgba(13,26,52,0.1) 58%, transparent)" }} />
        <div style={{ position: "absolute", top: 10, left: 10 }}>
          <span className="chip chip-support">{profile.match}% Match</span>
        </div>
        <button
          type="button"
          onClick={() => onShortlist(profile.userId)}
          className="button button-secondary"
          style={{ position: "absolute", top: 10, right: 10, width: 36, height: 36, padding: 0, borderRadius: "50%", fontWeight: 800 }}
        >
          {isShortlisted ? "Saved" : "Save"}
        </button>
        <div style={{ position: "absolute", left: 12, bottom: 12, color: "white" }}>
          <h3 style={{ margin: 0, fontSize: "1.14rem" }}>
            {profile.firstName}, {profile.age}
          </h3>
          <p style={{ margin: "0.2rem 0 0", opacity: 0.82, fontSize: "0.82rem" }}>
            {profile.profession} · {profile.city}
          </p>
        </div>
      </div>

      <div style={{ padding: "0.9rem" }}>
        <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap", marginBottom: "0.75rem" }}>
          {profile.isVerified && <span className="chip chip-support">Verified</span>}
          {profile.education && <span className="chip chip-brand">{profile.education}</span>}
          {profile.income && <span className="chip chip-brand">{profile.income}</span>}
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "0.52rem" }}>
          <button type="button" className="button button-primary" onClick={() => onInterest(profile.userId)}>
            Send Interest
          </button>
          <Link href={`/profile/${profile.userId}`} className="button button-secondary" style={{ padding: "0.75rem 0.95rem" }}>
            View
          </Link>
        </div>
      </div>
    </article>
  );
}

const INITIAL_FILTERS = {
  query: "",
  minAge: 18,
  maxAge: 40,
  religion: "Any",
  caste: "Any",
  subCaste: "Any",
  gothra: "Any",
  nakshatra: "Any",
  rashi: "Any",
  dosha: "Any",
  income: "Any",
  city: "Any",
  education: "Any",
  profession: "Any",
  minHeight: 140,
  maxHeight: 200,
  maritalStatus: "Any",
  motherTongue: "Any",
  diet: "Any",
};

function MatchesContent() {
  const searchParams = useSearchParams();
  const { user } = useAuth();
  const { showLoginModal, setShowLoginModal } = useGuestScrollGate();

  const [loading, setLoading] = useState(true);
  const [rawProfiles, setRawProfiles] = useState([]);
  const [shortlisted, setShortlisted] = useState(new Set());
  const [viewMode, setViewMode] = useState("grid");
  const [showAdvanced, setShowAdvanced] = useState(true);
  const [filters, setFilters] = useState(INITIAL_FILTERS);

  useEffect(() => {
    const fromQuery = searchParams.get("religion");
    if (fromQuery) {
      setFilters((prev) => ({ ...prev, religion: fromQuery }));
    }
  }, [searchParams]);

  useEffect(() => {
    const loadMatches = async () => {
      setLoading(true);
      try {
        if (!user) {
          setRawProfiles(MOCK_PROFILES);
          return;
        }

        const params = {
          gender: searchParams.get("gender") || "female",
        };

        const response = await api.get("/matches", { params });
        const list = Array.isArray(response.data) && response.data.length > 0 ? response.data : MOCK_PROFILES;
        setRawProfiles(list);
      } catch {
        setRawProfiles(MOCK_PROFILES);
      } finally {
        setLoading(false);
      }
    };

    loadMatches();
  }, [user, searchParams]);

  const onFilterChange = (key, value) => {
    setFilters((prev) => {
      const next = { ...prev, [key]: value };
      if (key === "caste") next.subCaste = "Any";
      return next;
    });
  };

  const resetFilters = () => {
    setFilters(INITIAL_FILTERS);
  };

  const subcasteOptions = useMemo(() => {
    const list = SUBCASTE_MAP[filters.caste] || [];
    return ["Any", ...list];
  }, [filters.caste]);

  const visibleProfiles = useMemo(() => {
    const query = filters.query.trim().toLowerCase();

    return rawProfiles.filter((p) => {
      if (p.age < Number(filters.minAge) || p.age > Number(filters.maxAge)) return false;
      if (filters.religion !== "Any" && p.religion !== filters.religion) return false;
      if (filters.caste !== "Any" && p.caste !== filters.caste) return false;
      if (filters.subCaste !== "Any" && (p.subCaste || "Any").toLowerCase() !== filters.subCaste.toLowerCase()) return false;

      if (filters.gothra !== "Any" && p.gothra !== filters.gothra) return false;
      if (filters.nakshatra !== "Any" && p.nakshatra !== filters.nakshatra) return false;
      if (filters.rashi !== "Any" && p.rashi !== filters.rashi) return false;
      if (filters.dosha !== "Any" && p.dosha !== filters.dosha) return false;

      if (filters.income !== "Any" && p.income !== filters.income) return false;
      if (filters.city !== "Any" && p.city !== filters.city) return false;
      if (filters.education !== "Any" && !(p.education || "").includes(filters.education)) return false;
      if (filters.profession !== "Any" && !(p.profession || "").includes(filters.profession)) return false;
      if (p.height && (p.height < Number(filters.minHeight) || p.height > Number(filters.maxHeight))) return false;
      if (filters.maritalStatus !== "Any" && p.maritalStatus !== filters.maritalStatus) return false;
      if (filters.motherTongue !== "Any" && p.motherTongue !== filters.motherTongue) return false;
      if (filters.diet !== "Any" && p.foodHabit !== filters.diet) return false;

      if (!query) return true;
      const haystack = [p.firstName, p.city, p.profession, p.religion, p.caste, p.subCaste, p.education]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      return haystack.includes(query);
    });
  }, [rawProfiles, filters]);

  const onInterest = async (userId) => {
    if (!user) {
      setShowLoginModal(true);
      return;
    }

    try {
      const response = await api.post("/interactions/like", { receiverId: userId });
      if (response.data?.matchId) toast.success("It is a match. Start chatting now.");
      else toast.success("Interest sent successfully.");
    } catch {
      toast.info("Interest flow is in demo mode right now.");
    }
  };

  const onShortlist = async (userId) => {
    if (!user) {
      setShowLoginModal(true);
      return;
    }

    const alreadyShortlisted = shortlisted.has(userId);
    setShortlisted((prev) => {
      const next = new Set(prev);
      if (next.has(userId)) next.delete(userId);
      else next.add(userId);
      return next;
    });

    try {
      if (alreadyShortlisted) {
        await api.post("/shortlist/remove", { shortlistedUserId: userId });
        toast.info("Removed from shortlist.");
      } else {
        await api.post("/shortlist/add", { shortlistedUserId: userId });
        toast.success("Added to shortlist.");
      }
    } catch {
      toast.info("Shortlist update saved locally.");
    }
  };

  return (
    <div style={{ display: "grid", gridTemplateColumns: "300px minmax(0,1fr)", gap: "0.95rem" }} className="matches-shell-grid">
      <aside className="panel" style={{ padding: "0.9rem", height: "fit-content", position: "sticky", top: "5.4rem" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.7rem", gap: "0.5rem" }}>
          <p className="section-label" style={{ margin: 0 }}>
            Filters
          </p>
          <div style={{ display: "flex", gap: "0.4rem" }}>
            <button type="button" className="button button-secondary" onClick={() => setShowAdvanced((prev) => !prev)}>
              {showAdvanced ? "Hide" : "Advanced"}
            </button>
            <button type="button" className="button button-secondary" onClick={resetFilters}>
              Reset
            </button>
          </div>
        </div>

        <div style={{ display: "grid", gap: "0.62rem" }}>
          <div>
            <label className="form-label">Search keyword</label>
            <input className="form-input" value={filters.query} onChange={(event) => onFilterChange("query", event.target.value)} placeholder="Name, city, caste, profession" />
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.45rem" }}>
            <div>
              <label className="form-label">Min age</label>
              <input className="form-input" type="number" min={18} max={70} value={filters.minAge} onChange={(event) => onFilterChange("minAge", clampNumber(event.target.value, 18, 70))} />
            </div>
            <div>
              <label className="form-label">Max age</label>
              <input className="form-input" type="number" min={18} max={70} value={filters.maxAge} onChange={(event) => onFilterChange("maxAge", clampNumber(event.target.value, 18, 70))} />
            </div>
          </div>

          <div>
            <label className="form-label">Religion</label>
            <select className="form-input" value={filters.religion} onChange={(event) => onFilterChange("religion", event.target.value)}>
              {RELIGIONS.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.45rem" }}>
            <div>
              <label className="form-label">Caste</label>
              <select className="form-input" value={filters.caste} onChange={(event) => onFilterChange("caste", event.target.value)}>
                {CASTES.map((value) => (
                  <option key={value} value={value}>
                    {value}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="form-label">Sub-caste</label>
              <select className="form-input" value={filters.subCaste} onChange={(event) => onFilterChange("subCaste", event.target.value)}>
                {subcasteOptions.map((value) => (
                  <option key={value} value={value}>
                    {value}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="form-label">City</label>
            <select className="form-input" value={filters.city} onChange={(event) => onFilterChange("city", event.target.value)}>
              {CITIES.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label">Profession</label>
            <select className="form-input" value={filters.profession} onChange={(event) => onFilterChange("profession", event.target.value)}>
              {PROFESSIONS.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </div>

          {showAdvanced && (
            <>
              <div>
                <label className="form-label">Education</label>
                <select className="form-input" value={filters.education} onChange={(event) => onFilterChange("education", event.target.value)}>
                  {EDUCATIONS.map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="form-label">Income</label>
                <select className="form-input" value={filters.income} onChange={(event) => onFilterChange("income", event.target.value)}>
                  {INCOMES.map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.45rem" }}>
                <div>
                  <label className="form-label">Min height (cm)</label>
                  <input className="form-input" type="number" min={120} max={250} value={filters.minHeight} onChange={(event) => onFilterChange("minHeight", clampNumber(event.target.value, 120, 250))} />
                </div>
                <div>
                  <label className="form-label">Max height (cm)</label>
                  <input className="form-input" type="number" min={120} max={250} value={filters.maxHeight} onChange={(event) => onFilterChange("maxHeight", clampNumber(event.target.value, 120, 250))} />
                </div>
              </div>

              <div>
                <label className="form-label">Marital status</label>
                <select className="form-input" value={filters.maritalStatus} onChange={(event) => onFilterChange("maritalStatus", event.target.value)}>
                  {M_STATUS.map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="form-label">Mother tongue</label>
                <select className="form-input" value={filters.motherTongue} onChange={(event) => onFilterChange("motherTongue", event.target.value)}>
                  {TONGUES.map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="form-label">Diet</label>
                <select className="form-input" value={filters.diet} onChange={(event) => onFilterChange("diet", event.target.value)}>
                  {DIETS.map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
              </div>

              <details open style={{ marginTop: "0.2rem" }}>
                <summary style={{ cursor: "pointer", fontWeight: 800, color: "var(--ink)", fontSize: "0.9rem" }}>Horoscope filters</summary>
                <div style={{ display: "grid", gap: "0.6rem", marginTop: "0.6rem" }}>
                  <div>
                    <label className="form-label">Gothra</label>
                    <select className="form-input" value={filters.gothra} onChange={(event) => onFilterChange("gothra", event.target.value)}>
                      {GOTRAS.map((value) => (
                        <option key={value} value={value}>
                          {value}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="form-label">Nakshatra</label>
                    <select className="form-input" value={filters.nakshatra} onChange={(event) => onFilterChange("nakshatra", event.target.value)}>
                      {NAKSHATRAS.map((value) => (
                        <option key={value} value={value}>
                          {value}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="form-label">Rashi</label>
                    <select className="form-input" value={filters.rashi} onChange={(event) => onFilterChange("rashi", event.target.value)}>
                      {RASHIS.map((value) => (
                        <option key={value} value={value}>
                          {value}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="form-label">Dosha</label>
                    <select className="form-input" value={filters.dosha} onChange={(event) => onFilterChange("dosha", event.target.value)}>
                      {DOSHAS.map((value) => (
                        <option key={value} value={value}>
                          {value}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </details>
            </>
          )}
        </div>
      </aside>

      <section>
        <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: "0.8rem", marginBottom: "0.8rem", flexWrap: "wrap" }}>
          <div>
            <p className="section-label" style={{ marginBottom: "0.2rem" }}>
              Compatible Profiles
            </p>
            <h1 className="section-title" style={{ fontSize: "clamp(1.6rem, 3vw, 2.2rem)", margin: 0 }}>
              Match Discovery
            </h1>
            <p className="section-copy" style={{ margin: "0.35rem 0 0", fontSize: "0.9rem" }}>
              {loading ? "Finding your best matches..." : `${visibleProfiles.length} profiles available`}
            </p>
          </div>

          <div style={{ display: "flex", gap: "0.45rem", alignItems: "center" }}>
            <Link href="/search" className="button button-secondary">
              Advanced Search
            </Link>
            <button type="button" className={`button ${viewMode === "grid" ? "button-primary" : "button-secondary"}`} onClick={() => setViewMode("grid")} style={{ padding: "0.65rem 0.9rem" }}>
              Grid
            </button>
            <button type="button" className={`button ${viewMode === "list" ? "button-primary" : "button-secondary"}`} onClick={() => setViewMode("list")} style={{ padding: "0.65rem 0.9rem" }}>
              List
            </button>
          </div>
        </div>

        {loading ? (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(230px, 1fr))", gap: "0.75rem" }}>
            {Array.from({ length: 6 }).map((_, index) => (
              <div key={`sk-${index}`} className="panel" style={{ height: 330, background: "rgba(240, 107, 78, 0.06)" }} />
            ))}
          </div>
        ) : visibleProfiles.length === 0 ? (
          <div className="panel" style={{ padding: "2rem", textAlign: "center" }}>
            <h2 style={{ marginTop: 0 }}>No matches found with current filters</h2>
            <p style={{ color: "var(--ink-muted)" }}>Try broadening caste/city filters or disabling horoscope filters.</p>
            <button type="button" className="button button-primary" onClick={resetFilters}>
              Reset Filters
            </button>
          </div>
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: viewMode === "grid" ? "repeat(auto-fit, minmax(240px, 1fr))" : "1fr", gap: "0.75rem" }}>
            {visibleProfiles.map((profile) => (
              <MatchCard key={profile.userId} profile={profile} isShortlisted={shortlisted.has(profile.userId)} onShortlist={onShortlist} onInterest={onInterest} viewMode={viewMode} />
            ))}
          </div>
        )}
      </section>

      <LoginPromptModal isOpen={showLoginModal} onClose={() => setShowLoginModal(false)} />

      <style jsx>{`
        @media (max-width: 980px) {
          .matches-shell-grid {
            grid-template-columns: 1fr !important;
          }

          .matches-shell-grid aside {
            position: static !important;
          }
        }
      `}</style>
    </div>
  );
}

export default function MatchesPage() {
  return (
    <Suspense fallback={<div className="panel" style={{ padding: "1rem" }}>Loading matches...</div>}>
      <MatchesContent />
    </Suspense>
  );
}
