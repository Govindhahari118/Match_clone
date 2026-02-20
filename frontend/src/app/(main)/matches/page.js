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
const CITIES = [
  "Any",
  "Mumbai",
  "Delhi",
  "Bengaluru",
  "Hyderabad",
  "Chennai",
  "Pune",
  "Kolkata",
  "Jaipur",
  "Ahmedabad",
  "London",
  "Toronto",
  "Dubai",
  "Singapore",
];

const PROFESSIONS = [
  "Any",
  "Engineer",
  "Doctor",
  "Designer",
  "Lawyer",
  "Teacher",
  "Entrepreneur",
  "Consultant",
  "Architect",
  "Data Scientist",
];

const MOCK_PROFILES = [
  {
    userId: "u1",
    firstName: "Ananya",
    age: 27,
    city: "Mumbai",
    profession: "Product Strategist",
    religion: "Hindu",
    photo: "https://randomuser.me/api/portraits/women/45.jpg",
    isVerified: true,
    match: 94,
    education: "MBA",
    income: "15-25L",
  },
  {
    userId: "u2",
    firstName: "Ishita",
    age: 26,
    city: "Toronto",
    profession: "Data Scientist",
    religion: "Hindu",
    photo: "https://randomuser.me/api/portraits/women/68.jpg",
    isVerified: true,
    match: 91,
    education: "M.Sc",
    income: "25-50L",
  },
  {
    userId: "u3",
    firstName: "Rhea",
    age: 28,
    city: "Singapore",
    profession: "Corporate Lawyer",
    religion: "Hindu",
    photo: "https://randomuser.me/api/portraits/women/75.jpg",
    isVerified: true,
    match: 89,
    education: "LLM",
    income: "25-50L",
  },
  {
    userId: "u4",
    firstName: "Meher",
    age: 29,
    city: "Dubai",
    profession: "Architect",
    religion: "Parsi",
    photo: "https://randomuser.me/api/portraits/women/63.jpg",
    isVerified: false,
    match: 85,
    education: "B.Arch",
    income: "10-25L",
  },
  {
    userId: "u5",
    firstName: "Nisha",
    age: 25,
    city: "Bengaluru",
    profession: "Engineer",
    religion: "Hindu",
    photo: "https://randomuser.me/api/portraits/women/31.jpg",
    isVerified: true,
    match: 88,
    education: "B.Tech",
    income: "10-25L",
  },
  {
    userId: "u6",
    firstName: "Sara",
    age: 27,
    city: "London",
    profession: "Consultant",
    religion: "Muslim",
    photo: "https://randomuser.me/api/portraits/women/52.jpg",
    isVerified: true,
    match: 83,
    education: "MBA",
    income: "25-50L",
  },
];

function MatchCard({ profile, isShortlisted, onShortlist, onInterest }) {
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
        <div
          style={{
            position: "absolute",
            inset: 0,
            background: "linear-gradient(to top, rgba(13,26,52,0.82), rgba(13,26,52,0.1) 58%, transparent)",
          }}
        />
        <div style={{ position: "absolute", top: 10, left: 10 }}>
          <span className="chip chip-support">{profile.match}% Match</span>
        </div>
        <button
          type="button"
          onClick={() => onShortlist(profile.userId)}
          className="button button-secondary"
          style={{
            position: "absolute",
            top: 10,
            right: 10,
            width: 36,
            height: 36,
            padding: 0,
            borderRadius: "50%",
            fontWeight: 800,
          }}
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

function MatchesContent() {
  const searchParams = useSearchParams();
  const { user } = useAuth();
  const { showLoginModal, setShowLoginModal } = useGuestScrollGate();

  const [loading, setLoading] = useState(true);
  const [rawProfiles, setRawProfiles] = useState([]);
  const [shortlisted, setShortlisted] = useState(new Set());
  const [filters, setFilters] = useState({
    minAge: 22,
    maxAge: 32,
    religion: "Any",
    city: "Any",
    profession: "Any",
    query: "",
  });

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
          minAge: filters.minAge,
          maxAge: filters.maxAge,
          religion: filters.religion,
          city: filters.city === "Any" ? "" : filters.city,
          profession: filters.profession === "Any" ? "" : filters.profession,
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
  }, [user, searchParams, filters.minAge, filters.maxAge, filters.religion, filters.city, filters.profession]);

  const visibleProfiles = useMemo(() => {
    const query = filters.query.trim().toLowerCase();

    return rawProfiles.filter((profile) => {
      if (profile.age < Number(filters.minAge) || profile.age > Number(filters.maxAge)) return false;
      if (filters.religion !== "Any" && profile.religion !== filters.religion) return false;
      if (filters.city !== "Any" && profile.city !== filters.city) return false;
      if (filters.profession !== "Any" && !(profile.profession || "").includes(filters.profession)) return false;

      if (!query) return true;
      const haystack = [profile.firstName, profile.city, profile.profession, profile.religion]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      return haystack.includes(query);
    });
  }, [rawProfiles, filters]);

  const onFilterChange = (key, value) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
  };

  const resetFilters = () => {
    setFilters({
      minAge: 22,
      maxAge: 32,
      religion: "Any",
      city: "Any",
      profession: "Any",
      query: "",
    });
  };

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
    <div style={{ display: "grid", gridTemplateColumns: "260px minmax(0,1fr)", gap: "0.95rem" }} className="matches-shell-grid">
      <aside className="panel" style={{ padding: "0.9rem", height: "fit-content", position: "sticky", top: "5.4rem" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.7rem" }}>
          <p className="section-label" style={{ margin: 0 }}>
            Filters
          </p>
          <button type="button" className="button button-secondary" onClick={resetFilters}>
            Reset
          </button>
        </div>

        <div style={{ display: "grid", gap: "0.62rem" }}>
          <div>
            <label className="form-label">Search keyword</label>
            <input
              className="form-input"
              value={filters.query}
              onChange={(event) => onFilterChange("query", event.target.value)}
              placeholder="Name, city, profession"
            />
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.45rem" }}>
            <div>
              <label className="form-label">Min age</label>
              <input
                className="form-input"
                type="number"
                min={18}
                max={70}
                value={filters.minAge}
                onChange={(event) => onFilterChange("minAge", Number(event.target.value))}
              />
            </div>
            <div>
              <label className="form-label">Max age</label>
              <input
                className="form-input"
                type="number"
                min={18}
                max={70}
                value={filters.maxAge}
                onChange={(event) => onFilterChange("maxAge", Number(event.target.value))}
              />
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
        </div>
      </aside>

      <section>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "0.7rem", marginBottom: "0.8rem" }}>
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
          <Link href="/search" className="button button-secondary">
            Advanced Search
          </Link>
        </div>

        {loading ? (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(230px, 1fr))", gap: "0.75rem" }}>
            {Array.from({ length: 6 }).map((_, index) => (
              <div key={`sk-${index}`} className="panel" style={{ height: 330, background: "#f7f3ec" }} />
            ))}
          </div>
        ) : visibleProfiles.length === 0 ? (
          <div className="panel" style={{ padding: "2rem", textAlign: "center" }}>
            <h2 style={{ marginTop: 0 }}>No matches found with current filters</h2>
            <p style={{ color: "var(--ink-muted)" }}>Adjust age, city, or profession and try again.</p>
            <button type="button" className="button button-primary" onClick={resetFilters}>
              Reset Filters
            </button>
          </div>
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: "0.75rem" }}>
            {visibleProfiles.map((profile) => (
              <MatchCard
                key={profile.userId}
                profile={profile}
                isShortlisted={shortlisted.has(profile.userId)}
                onShortlist={onShortlist}
                onInterest={onInterest}
              />
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
