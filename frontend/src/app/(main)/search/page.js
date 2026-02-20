"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { toast } from "react-toastify";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import LoginPromptModal from "../../../components/LoginPromptModal";

const RELIGIONS = ["Any", "Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist", "Parsi"];
const CASTES = ["Any", "Brahmin", "Kshatriya", "Vaishya", "Kayastha", "Rajput", "Reddy", "Naidu", "Chettiar", "Jat"];
const INCOME_OPTIONS = ["Any", "Below 3L", "3-5L", "5-10L", "10-25L", "25-50L", "50L+"];
const EDUCATIONS = ["Any", "10th", "12th", "Diploma", "Graduate", "Post Graduate", "Doctorate"];
const PROFESSIONS = ["Any", "Engineer", "Doctor", "CA/Finance", "Govt/PSU", "Lawyer", "Teacher", "Business", "NRI", "Other"];
const MARITAL_STATUS = ["Any", "Never Married", "Divorced", "Widowed", "Separated"];
const HEIGHTS = ["Any", "Below 5ft", "5ft-5.5ft", "5.5ft-6ft", "6ft-6.5ft", "Above 6.5ft"];
const BODY_TYPES = ["Any", "Slim", "Average", "Athletic", "Heavy"];

const MOCK_RESULTS = [
  {
    id: "r1",
    userId: "u1",
    firstName: "Aarti",
    age: 26,
    city: "Mumbai",
    state: "Maharashtra",
    profession: "Doctor",
    religion: "Hindu",
    caste: "Brahmin",
    photo: "https://randomuser.me/api/portraits/women/11.jpg",
    isVerified: true,
    match: 92,
    education: "Graduate",
    income: "10-25L",
    height: "5.5ft-6ft",
    body: "Average",
  },
  {
    id: "r2",
    userId: "u2",
    firstName: "Divya",
    age: 24,
    city: "Pune",
    state: "Maharashtra",
    profession: "Engineer",
    religion: "Hindu",
    caste: "Reddy",
    photo: "https://randomuser.me/api/portraits/women/12.jpg",
    isVerified: true,
    match: 87,
    education: "Graduate",
    income: "5-10L",
    height: "5ft-5.5ft",
    body: "Slim",
  },
  {
    id: "r3",
    userId: "u3",
    firstName: "Lakshmi",
    age: 29,
    city: "Chennai",
    state: "Tamil Nadu",
    profession: "CA/Finance",
    religion: "Hindu",
    caste: "Naidu",
    photo: "https://randomuser.me/api/portraits/women/13.jpg",
    isVerified: false,
    match: 81,
    education: "Post Graduate",
    income: "5-10L",
    height: "5.5ft-6ft",
    body: "Average",
  },
  {
    id: "r4",
    userId: "u4",
    firstName: "Pooja",
    age: 27,
    city: "Hyderabad",
    state: "Telangana",
    profession: "Lawyer",
    religion: "Hindu",
    caste: "Reddy",
    photo: "https://randomuser.me/api/portraits/women/14.jpg",
    isVerified: true,
    match: 78,
    education: "Graduate",
    income: "10-25L",
    height: "5ft-5.5ft",
    body: "Athletic",
  },
  {
    id: "r5",
    userId: "u5",
    firstName: "Isha",
    age: 25,
    city: "Delhi",
    state: "Delhi",
    profession: "Govt/PSU",
    religion: "Hindu",
    caste: "Rajput",
    photo: "https://randomuser.me/api/portraits/women/15.jpg",
    isVerified: true,
    match: 75,
    education: "Post Graduate",
    income: "5-10L",
    height: "5.5ft-6ft",
    body: "Average",
  },
  {
    id: "r6",
    userId: "u6",
    firstName: "Zara",
    age: 26,
    city: "Mumbai",
    state: "Maharashtra",
    profession: "Designer",
    religion: "Muslim",
    caste: "Any",
    photo: "https://randomuser.me/api/portraits/women/16.jpg",
    isVerified: false,
    match: 70,
    education: "Graduate",
    income: "3-5L",
    height: "5ft-5.5ft",
    body: "Slim",
  },
];

const INITIAL_FILTERS = {
  query: "",
  lookingFor: "Bride (Woman)",
  minAge: 22,
  maxAge: 35,
  religion: "Any",
  caste: "Any",
  maritalStatus: "Any",
  education: "Any",
  profession: "Any",
  income: "Any",
  height: "Any",
  body: "Any",
  city: "",
  state: "",
};

function toApiGender(lookingFor) {
  if (!lookingFor) return undefined;
  return lookingFor.toLowerCase().includes("groom") ? "male" : "female";
}

function clampNumber(value, min, max) {
  const number = Number(value);
  if (Number.isNaN(number)) return min;
  return Math.min(max, Math.max(min, number));
}

function SearchResultCard({ profile, onInterest, onShortlist, isShortlisted, viewMode }) {
  if (viewMode === "list") {
    return (
      <article className="panel panel-hover anim-rise listing-stage" style={{ overflow: "hidden", display: "flex" }}>
        <div style={{ width: 186, flexShrink: 0, position: "relative" }}>
          <Image src={profile.photo} alt={`${profile.firstName} profile`} width={620} height={760} unoptimized style={{ width: "100%", height: "100%", objectFit: "cover" }} />
        </div>

        <div style={{ flex: 1, padding: "1rem", minWidth: 0 }}>
          <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: "0.75rem" }}>
            <div style={{ minWidth: 0 }}>
              <h3 style={{ margin: 0, fontSize: "1.05rem" }}>
                {profile.firstName}, {profile.age}
              </h3>
              <p style={{ margin: "0.24rem 0 0", color: "var(--ink-muted)", fontSize: "0.88rem" }}>
                {profile.profession} - {profile.city}
              </p>
            </div>
            <span className="chip chip-support" style={{ flexShrink: 0 }}>
              {profile.match}% Match
            </span>
          </div>

          <div style={{ display: "flex", gap: "0.45rem", flexWrap: "wrap", marginTop: "0.65rem" }}>
            {profile.isVerified && <span className="chip chip-support">Verified</span>}
            {profile.religion && <span className="chip chip-brand">{profile.religion}</span>}
            {profile.caste && <span className="chip chip-brand">{profile.caste}</span>}
            {profile.education && <span className="chip chip-brand">{profile.education}</span>}
            {profile.income && <span className="chip chip-brand">{profile.income}</span>}
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr auto auto", gap: "0.55rem", marginTop: "0.9rem" }}>
            <button type="button" className="button button-primary" onClick={() => onInterest(profile.userId)}>
              Send Interest
            </button>
            <button type="button" className="button button-secondary" onClick={() => onShortlist(profile.userId)} style={{ padding: "0.72rem 0.92rem" }}>
              {isShortlisted ? "Saved" : "Save"}
            </button>
            <Link href={`/profile/${profile.userId}`} className="button button-secondary" style={{ padding: "0.72rem 0.92rem" }}>
              View
            </Link>
          </div>
        </div>
      </article>
    );
  }

  return (
    <article className="panel panel-hover anim-rise listing-stage" style={{ overflow: "hidden" }}>
      <div style={{ position: "relative", height: 220 }}>
        <Image src={profile.photo} alt={`${profile.firstName} profile`} width={640} height={920} unoptimized style={{ width: "100%", height: "100%", objectFit: "cover" }} />
        <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(9, 18, 36, 0.76), transparent 58%)" }} />
        <div style={{ position: "absolute", top: 10, left: 10 }}>
          <span className="chip chip-support">{profile.match}% Match</span>
        </div>
        <button
          type="button"
          onClick={() => onShortlist(profile.userId)}
          className="button button-secondary"
          style={{ position: "absolute", top: 10, right: 10, width: 38, height: 38, borderRadius: 12, padding: 0 }}
        >
          {isShortlisted ? "Saved" : "Save"}
        </button>
        <div style={{ position: "absolute", left: 12, bottom: 12, color: "white" }}>
          <h3 style={{ margin: 0, fontSize: "1.08rem" }}>
            {profile.firstName}, {profile.age}
          </h3>
          <p style={{ margin: "0.22rem 0 0", fontSize: "0.82rem", opacity: 0.92 }}>
            {profile.profession} - {profile.city}
          </p>
        </div>
      </div>

      <div style={{ padding: "0.95rem" }}>
        <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap", marginBottom: "0.74rem" }}>
          {profile.isVerified && <span className="chip chip-support">Verified</span>}
          {profile.religion && <span className="chip chip-brand">{profile.religion}</span>}
          {profile.caste && <span className="chip chip-brand">{profile.caste}</span>}
          {profile.education && <span className="chip chip-brand">{profile.education}</span>}
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "0.52rem" }}>
          <button type="button" className="button button-primary" onClick={() => onInterest(profile.userId)}>
            Send Interest
          </button>
          <Link href={`/profile/${profile.userId}`} className="button button-secondary" style={{ padding: "0.74rem 0.9rem" }}>
            View
          </Link>
        </div>
      </div>
    </article>
  );
}

export default function SearchPage() {
  const searchParams = useSearchParams();
  const { user } = useAuth();

  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(true);
  const [viewMode, setViewMode] = useState("grid");
  const [results, setResults] = useState([]);
  const [shortlisted, setShortlisted] = useState(new Set());
  const [filters, setFilters] = useState(INITIAL_FILTERS);

  useEffect(() => {
    const incoming = searchParams.get("q") || "";
    if (incoming) {
      setFilters((previous) => ({ ...previous, query: incoming }));
    }
  }, [searchParams]);

  const visibleResults = useMemo(() => {
    const query = filters.query.trim().toLowerCase();
    const city = filters.city.trim().toLowerCase();
    const state = filters.state.trim().toLowerCase();

    return results.filter((profile) => {
      if (profile.age < Number(filters.minAge) || profile.age > Number(filters.maxAge)) return false;
      if (filters.religion !== "Any" && profile.religion !== filters.religion) return false;
      if (filters.caste !== "Any" && profile.caste !== filters.caste) return false;
      if (filters.maritalStatus !== "Any" && profile.maritalStatus && profile.maritalStatus !== filters.maritalStatus) return false;
      if (filters.education !== "Any" && profile.education && profile.education !== filters.education) return false;
      if (filters.profession !== "Any" && !(profile.profession || "").toLowerCase().includes(filters.profession.toLowerCase())) return false;
      if (filters.income !== "Any" && profile.income && profile.income !== filters.income) return false;
      if (filters.height !== "Any" && profile.height && profile.height !== filters.height) return false;
      if (filters.body !== "Any" && profile.body && profile.body !== filters.body) return false;
      if (city && !(profile.city || "").toLowerCase().includes(city)) return false;
      if (state && !(profile.state || "").toLowerCase().includes(state)) return false;

      if (!query) return true;
      const haystack = [profile.firstName, profile.city, profile.state, profile.profession, profile.religion, profile.caste].filter(Boolean).join(" ").toLowerCase();
      return haystack.includes(query);
    });
  }, [results, filters]);

  const onChange = (key, value) => {
    setFilters((previous) => ({ ...previous, [key]: value }));
  };

  const onReset = () => {
    setFilters(INITIAL_FILTERS);
    setSearched(false);
    setResults([]);
  };

  const onSearch = async (event) => {
    event.preventDefault();
    setSearched(true);
    setLoading(true);

    try {
      if (!user) {
        setResults(MOCK_RESULTS);
        return;
      }

      const params = {
        gender: toApiGender(filters.lookingFor),
        minAge: clampNumber(filters.minAge, 18, 70),
        maxAge: clampNumber(filters.maxAge, 18, 70),
      };

      if (filters.religion !== "Any") params.religion = filters.religion;
      if (filters.caste !== "Any") params.caste = filters.caste;
      if (filters.maritalStatus !== "Any") params.maritalStatus = filters.maritalStatus;
      if (filters.education !== "Any") params.education = filters.education;
      if (filters.profession !== "Any") params.profession = filters.profession;
      if (filters.income !== "Any") params.income = filters.income;
      if (filters.city.trim()) params.city = filters.city.trim();
      if (filters.state.trim()) params.state = filters.state.trim();

      const response = await api.get("/matches", { params });
      const payload = Array.isArray(response.data) && response.data.length > 0 ? response.data : MOCK_RESULTS;
      setResults(payload);
    } catch {
      setResults(MOCK_RESULTS);
    } finally {
      setLoading(false);
    }
  };

  const onInterest = async (userId) => {
    if (!user) {
      setShowLoginModal(true);
      return;
    }

    try {
      await api.post("/interactions/like", { receiverId: userId });
      toast.success("Interest sent.");
    } catch {
      toast.info("Interest saved in demo mode.");
    }
  };

  const onShortlist = async (userId) => {
    if (!user) {
      setShowLoginModal(true);
      return;
    }

    const alreadySaved = shortlisted.has(userId);
    setShortlisted((previous) => {
      const next = new Set(previous);
      if (next.has(userId)) next.delete(userId);
      else next.add(userId);
      return next;
    });

    try {
      if (alreadySaved) {
        await api.post("/shortlist/remove", { shortlistedUserId: userId });
        toast.info("Removed from shortlist.");
      } else {
        await api.post("/shortlist/add", { shortlistedUserId: userId });
        toast.success("Added to shortlist.");
      }
    } catch {
      toast.info("Shortlist updated locally.");
    }
  };

  return (
    <div style={{ display: "grid", gridTemplateColumns: "320px minmax(0, 1fr)", gap: "1rem" }} className="search-shell-grid">
      <aside className="panel filter-panel" style={{ padding: "1rem", position: "sticky", top: "5.35rem", height: "fit-content" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.78rem", gap: "0.5rem" }}>
          <p className="section-label" style={{ margin: 0 }}>
            Filters
          </p>

          <div style={{ display: "flex", gap: "0.42rem" }}>
            <button type="button" className="button button-secondary" onClick={() => setShowAdvanced((previous) => !previous)}>
              {showAdvanced ? "Hide" : "Advanced"}
            </button>
            <button type="button" className="button button-secondary" onClick={onReset}>
              Reset
            </button>
          </div>
        </div>

        <form onSubmit={onSearch} style={{ display: "grid", gap: "0.65rem" }}>
          <div>
            <label className="form-label">Keyword</label>
            <input className="form-input" value={filters.query} onChange={(event) => onChange("query", event.target.value)} placeholder="Name, city, profession" />
          </div>

          <div>
            <label className="form-label">Looking for</label>
            <select className="form-input" value={filters.lookingFor} onChange={(event) => onChange("lookingFor", event.target.value)}>
              <option>Bride (Woman)</option>
              <option>Groom (Man)</option>
            </select>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.48rem" }}>
            <div>
              <label className="form-label">Min age</label>
              <input className="form-input" type="number" min={18} max={70} value={filters.minAge} onChange={(event) => onChange("minAge", clampNumber(event.target.value, 18, 70))} />
            </div>
            <div>
              <label className="form-label">Max age</label>
              <input className="form-input" type="number" min={18} max={70} value={filters.maxAge} onChange={(event) => onChange("maxAge", clampNumber(event.target.value, 18, 70))} />
            </div>
          </div>

          <div>
            <label className="form-label">Religion</label>
            <select className="form-input" value={filters.religion} onChange={(event) => onChange("religion", event.target.value)}>
              {RELIGIONS.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label">Caste</label>
            <select className="form-input" value={filters.caste} onChange={(event) => onChange("caste", event.target.value)}>
              {CASTES.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label">Marital status</label>
            <select className="form-input" value={filters.maritalStatus} onChange={(event) => onChange("maritalStatus", event.target.value)}>
              {MARITAL_STATUS.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>

          {showAdvanced && (
            <>
              <div>
                <label className="form-label">Education</label>
                <select className="form-input" value={filters.education} onChange={(event) => onChange("education", event.target.value)}>
                  {EDUCATIONS.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="form-label">Profession</label>
                <select className="form-input" value={filters.profession} onChange={(event) => onChange("profession", event.target.value)}>
                  {PROFESSIONS.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="form-label">Annual income</label>
                <select className="form-input" value={filters.income} onChange={(event) => onChange("income", event.target.value)}>
                  {INCOME_OPTIONS.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.48rem" }}>
                <div>
                  <label className="form-label">Height</label>
                  <select className="form-input" value={filters.height} onChange={(event) => onChange("height", event.target.value)}>
                    {HEIGHTS.map((value) => (
                      <option key={value}>{value}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="form-label">Body type</label>
                  <select className="form-input" value={filters.body} onChange={(event) => onChange("body", event.target.value)}>
                    {BODY_TYPES.map((value) => (
                      <option key={value}>{value}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="form-label">City</label>
                <input className="form-input" value={filters.city} onChange={(event) => onChange("city", event.target.value)} placeholder="Any city..." />
              </div>

              <div>
                <label className="form-label">State</label>
                <input className="form-input" value={filters.state} onChange={(event) => onChange("state", event.target.value)} placeholder="Any state..." />
              </div>
            </>
          )}

          <button type="submit" className="button button-primary" style={{ width: "100%", marginTop: "0.35rem" }}>
            {loading ? "Searching..." : "Search Profiles"}
          </button>
        </form>
      </aside>

      <section>
        <div className="listing-hero">
          <div>
            <p className="section-label" style={{ marginBottom: "0.22rem" }}>
              Discovery
            </p>
            <h1 className="section-title" style={{ margin: 0, fontSize: "clamp(1.64rem, 3vw, 2.2rem)" }}>
              Advanced Search Results
            </h1>
            <p className="section-copy" style={{ marginTop: "0.4rem", fontSize: "0.92rem" }}>
              {searched ? `${visibleResults.length} profiles found` : "Run a search to see compatible profiles."}
            </p>
          </div>

          <div style={{ display: "flex", gap: "0.45rem", alignItems: "center" }}>
            <button type="button" className={`button ${viewMode === "grid" ? "button-primary" : "button-secondary"}`} onClick={() => setViewMode("grid")} style={{ padding: "0.66rem 0.92rem" }}>
              Grid
            </button>
            <button type="button" className={`button ${viewMode === "list" ? "button-primary" : "button-secondary"}`} onClick={() => setViewMode("list")} style={{ padding: "0.66rem 0.92rem" }}>
              List
            </button>
          </div>
        </div>

        {!searched ? (
          <div className="panel listing-stage" style={{ padding: "2.1rem", textAlign: "center" }}>
            <h2 style={{ marginTop: 0 }}>Use filters to discover focused matches</h2>
            <p style={{ color: "var(--ink-muted)" }}>Basic and advanced filters help you get better results across community, career, and location.</p>
          </div>
        ) : loading ? (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(230px, 1fr))", gap: "0.9rem" }}>
            {Array.from({ length: 8 }).map((_, index) => (
              <div key={`loading-${index}`} className="panel listing-stage" style={{ height: 332, background: "rgba(29, 78, 216, 0.06)" }} />
            ))}
          </div>
        ) : visibleResults.length === 0 ? (
          <div className="panel listing-stage" style={{ padding: "2.1rem", textAlign: "center" }}>
            <h2 style={{ marginTop: 0 }}>No profiles match this combination yet</h2>
            <p style={{ color: "var(--ink-muted)" }}>Try widening age range, keeping caste as Any, or turning off some advanced filters.</p>
            <button type="button" className="button button-primary" onClick={onReset}>
              Clear Filters
            </button>
          </div>
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: viewMode === "grid" ? "repeat(auto-fit, minmax(240px, 1fr))" : "1fr", gap: "0.9rem" }}>
            {visibleResults.map((profile) => (
              <SearchResultCard
                key={profile.id || profile.userId}
                profile={profile}
                onInterest={onInterest}
                onShortlist={onShortlist}
                isShortlisted={shortlisted.has(profile.userId)}
                viewMode={viewMode}
              />
            ))}
          </div>
        )}
      </section>

      <LoginPromptModal isOpen={showLoginModal} onClose={() => setShowLoginModal(false)} />

      <style jsx>{`
        @media (max-width: 980px) {
          .search-shell-grid {
            grid-template-columns: 1fr !important;
          }

          .search-shell-grid aside {
            position: static !important;
          }
        }
      `}</style>
    </div>
  );
}

