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
const PROFESSIONS = ["Any", "Engineer", "Doctor", "Lawyer", "Designer", "Entrepreneur", "Teacher", "Consultant"];
const INCOME = ["Any", "Below 5L", "5-10L", "10-25L", "25-50L", "50L+"];
const MARITAL_STATUS = ["Any", "Never Married", "Divorced", "Widowed", "Separated"];

const MOCK_RESULTS = [
  {
    userId: "s1",
    firstName: "Aarti",
    age: 26,
    city: "Mumbai",
    profession: "Doctor",
    religion: "Hindu",
    photo: "https://randomuser.me/api/portraits/women/11.jpg",
    isVerified: true,
    match: 92,
    education: "MBBS",
    income: "10-25L",
  },
  {
    userId: "s2",
    firstName: "Divya",
    age: 24,
    city: "Pune",
    profession: "Engineer",
    religion: "Hindu",
    photo: "https://randomuser.me/api/portraits/women/12.jpg",
    isVerified: true,
    match: 87,
    education: "B.Tech",
    income: "5-10L",
  },
  {
    userId: "s3",
    firstName: "Lina",
    age: 27,
    city: "Dubai",
    profession: "Consultant",
    religion: "Muslim",
    photo: "https://randomuser.me/api/portraits/women/56.jpg",
    isVerified: true,
    match: 84,
    education: "MBA",
    income: "25-50L",
  },
  {
    userId: "s4",
    firstName: "Nitya",
    age: 28,
    city: "Bengaluru",
    profession: "Designer",
    religion: "Hindu",
    photo: "https://randomuser.me/api/portraits/women/33.jpg",
    isVerified: false,
    match: 81,
    education: "B.Des",
    income: "10-25L",
  },
  {
    userId: "s5",
    firstName: "Farah",
    age: 29,
    city: "London",
    profession: "Lawyer",
    religion: "Muslim",
    photo: "https://randomuser.me/api/portraits/women/66.jpg",
    isVerified: true,
    match: 79,
    education: "LLB",
    income: "25-50L",
  },
  {
    userId: "s6",
    firstName: "Ritika",
    age: 25,
    city: "Delhi",
    profession: "Entrepreneur",
    religion: "Hindu",
    photo: "https://randomuser.me/api/portraits/women/41.jpg",
    isVerified: true,
    match: 86,
    education: "MBA",
    income: "25-50L",
  },
];

function SearchResultCard({ profile, onInterest, onShortlist, isShortlisted }) {
  return (
    <article className="panel panel-hover anim-rise" style={{ overflow: "hidden" }}>
      <div style={{ position: "relative", height: 210 }}>
        <Image
          src={profile.photo}
          alt={`${profile.firstName} profile`}
          width={640}
          height={900}
          unoptimized
          style={{ width: "100%", height: "100%", objectFit: "cover" }}
        />
        <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(12,20,40,0.75), transparent 58%)" }} />
        <div style={{ position: "absolute", top: 10, left: 10 }}>
          <span className="chip chip-brand">{profile.match}% Match</span>
        </div>
        <button
          type="button"
          onClick={() => onShortlist(profile.userId)}
          className="button button-secondary"
          style={{ position: "absolute", top: 10, right: 10, width: 36, height: 36, borderRadius: "50%", padding: 0 }}
        >
          {isShortlisted ? "Saved" : "Save"}
        </button>
        <div style={{ position: "absolute", left: 12, bottom: 12, color: "white" }}>
          <h3 style={{ margin: 0, fontSize: "1.08rem" }}>
            {profile.firstName}, {profile.age}
          </h3>
          <p style={{ margin: "0.2rem 0 0", fontSize: "0.8rem", opacity: 0.9 }}>
            {profile.profession} · {profile.city}
          </p>
        </div>
      </div>

      <div style={{ padding: "0.9rem" }}>
        <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap", marginBottom: "0.7rem" }}>
          {profile.isVerified && <span className="chip chip-support">Verified</span>}
          <span className="chip chip-brand">{profile.religion || "Community"}</span>
          <span className="chip chip-brand">{profile.education || "Graduate"}</span>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "0.5rem" }}>
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
  const [results, setResults] = useState([]);
  const [shortlisted, setShortlisted] = useState(new Set());

  const [filters, setFilters] = useState({
    query: "",
    minAge: 22,
    maxAge: 32,
    religion: "Any",
    profession: "Any",
    income: "Any",
    maritalStatus: "Any",
    city: "",
  });

  useEffect(() => {
    const incoming = searchParams.get("q") || "";
    if (incoming) {
      setFilters((prev) => ({ ...prev, query: incoming }));
    }
  }, [searchParams]);

  const visibleResults = useMemo(() => {
    const query = filters.query.trim().toLowerCase();

    return results.filter((profile) => {
      if (profile.age < Number(filters.minAge) || profile.age > Number(filters.maxAge)) return false;
      if (filters.religion !== "Any" && profile.religion !== filters.religion) return false;
      if (filters.profession !== "Any" && !(profile.profession || "").includes(filters.profession)) return false;
      if (filters.city && !(profile.city || "").toLowerCase().includes(filters.city.toLowerCase())) return false;

      if (!query) return true;
      const haystack = [profile.firstName, profile.city, profile.profession, profile.religion]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      return haystack.includes(query);
    });
  }, [results, filters]);

  const onChange = (key, value) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
  };

  const onReset = () => {
    setFilters({
      query: "",
      minAge: 22,
      maxAge: 32,
      religion: "Any",
      profession: "Any",
      income: "Any",
      maritalStatus: "Any",
      city: "",
    });
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
        gender: "female",
        minAge: filters.minAge,
        maxAge: filters.maxAge,
        religion: filters.religion,
        profession: filters.profession,
        city: filters.city,
        income: filters.income,
        maritalStatus: filters.maritalStatus,
      };

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

    const exists = shortlisted.has(userId);
    setShortlisted((prev) => {
      const next = new Set(prev);
      if (next.has(userId)) next.delete(userId);
      else next.add(userId);
      return next;
    });

    try {
      if (exists) {
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
    <div style={{ display: "grid", gridTemplateColumns: "290px minmax(0, 1fr)", gap: "0.95rem" }} className="search-shell-grid">
      <aside className="panel" style={{ padding: "0.9rem", position: "sticky", top: "5.35rem", height: "fit-content" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.7rem" }}>
          <p className="section-label" style={{ margin: 0 }}>
            Advanced Filters
          </p>
          <button type="button" className="button button-secondary" onClick={onReset}>
            Reset
          </button>
        </div>

        <form onSubmit={onSearch} style={{ display: "grid", gap: "0.6rem" }}>
          <div>
            <label className="form-label">Keyword</label>
            <input
              className="form-input"
              value={filters.query}
              onChange={(event) => onChange("query", event.target.value)}
              placeholder="Name, city, profession"
            />
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.45rem" }}>
            <div>
              <label className="form-label">Min age</label>
              <input className="form-input" type="number" min={18} max={70} value={filters.minAge} onChange={(event) => onChange("minAge", Number(event.target.value))} />
            </div>
            <div>
              <label className="form-label">Max age</label>
              <input className="form-input" type="number" min={18} max={70} value={filters.maxAge} onChange={(event) => onChange("maxAge", Number(event.target.value))} />
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
            <label className="form-label">Profession</label>
            <select className="form-input" value={filters.profession} onChange={(event) => onChange("profession", event.target.value)}>
              {PROFESSIONS.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label">Income</label>
            <select className="form-input" value={filters.income} onChange={(event) => onChange("income", event.target.value)}>
              {INCOME.map((value) => (
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

          <div>
            <label className="form-label">City</label>
            <input className="form-input" value={filters.city} onChange={(event) => onChange("city", event.target.value)} placeholder="e.g. Mumbai" />
          </div>

          <button type="submit" className="button button-primary" style={{ width: "100%", marginTop: "0.25rem" }}>
            {loading ? "Searching..." : "Search Profiles"}
          </button>
        </form>
      </aside>

      <section>
        <div style={{ marginBottom: "0.8rem" }}>
          <p className="section-label" style={{ marginBottom: "0.2rem" }}>
            Discovery
          </p>
          <h1 className="section-title" style={{ margin: 0, fontSize: "clamp(1.7rem, 3vw, 2.3rem)" }}>
            Advanced Search Results
          </h1>
          <p className="section-copy" style={{ marginTop: "0.4rem", fontSize: "0.92rem" }}>
            {searched ? `${visibleResults.length} profiles found` : "Run a search to see compatible profiles."}
          </p>
        </div>

        {!searched ? (
          <div className="panel" style={{ padding: "2rem", textAlign: "center" }}>
            <h2 style={{ marginTop: 0 }}>Use filters to discover focused matches</h2>
            <p style={{ color: "var(--ink-muted)" }}>
              Combine age, profession, location, and community criteria for better results.
            </p>
          </div>
        ) : loading ? (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(230px, 1fr))", gap: "0.75rem" }}>
            {Array.from({ length: 8 }).map((_, index) => (
              <div key={`load-${index}`} className="panel" style={{ height: 330, background: "#f7f3ec" }} />
            ))}
          </div>
        ) : visibleResults.length === 0 ? (
          <div className="panel" style={{ padding: "2rem", textAlign: "center" }}>
            <h2 style={{ marginTop: 0 }}>No profiles match this combination yet</h2>
            <p style={{ color: "var(--ink-muted)" }}>
              Try widening age range or keeping religion/profession as Any.
            </p>
            <button type="button" className="button button-primary" onClick={onReset}>
              Clear Filters
            </button>
          </div>
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: "0.75rem" }}>
            {visibleResults.map((profile) => (
              <SearchResultCard
                key={profile.userId}
                profile={profile}
                onInterest={onInterest}
                onShortlist={onShortlist}
                isShortlisted={shortlisted.has(profile.userId)}
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
