"use client";

import Image from "next/image";
import dynamic from "next/dynamic";
import Link from "next/link";
import { memo, Suspense, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { toast } from "react-toastify";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import { useGuestScrollGate } from "../../../hooks/useGuestScrollGate";
import { getCachedRequest } from "../../../services/requestCache";
import PageEmptyState from "../../../components/states/PageEmptyState";
import PageLoadingState from "../../../components/states/PageLoadingState";

const LoginPromptModal = dynamic(() => import("../../../components/LoginPromptModal"), {
  ssr: false,
});

const FALLBACK_FILTER_META = {
  religion: ["Any", "Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist"],
  caste: ["Any", "Brahmin", "Kshatriya", "Vaisya", "Kayastha", "Rajput", "Reddy", "Naidu", "Chettiar", "Jat", "Maratha", "Vanniyar", "Yadav", "Ezhava", "Nair", "SC", "ST", "OBC", "General"],
  income: ["Any", "Below 3L", "3-5L", "5-10L", "10-25L", "25-50L", "50L+"],
  maritalStatus: ["Any", "Never Married", "Divorced", "Widowed", "Awaiting Divorce", "Separated"],
  motherTongue: ["Any", "Hindi", "English", "Marathi", "Tamil", "Telugu", "Kannada", "Malayalam", "Bengali", "Gujarati", "Punjabi", "Urdu"],
  education: ["Any", "MBBS", "MD", "B.Tech", "M.Tech", "MBA", "CA", "B.Arch", "B.Des", "LLB", "PhD", "IAS", "IPS", "B.Com", "M.Com", "B.Sc", "M.Sc", "Other"],
  profession: ["Any", "Doctor", "Engineer", "Software Engineer", "Architect", "Designer", "Lawyer", "Teacher", "Professor", "Govt. Officer", "Business", "Student", "Self Employed", "Other"],
};

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

const RASHIS = ["Any", "Aries (Mesha)", "Taurus (Vrishabha)", "Gemini (Mithuna)", "Cancer (Karka)", "Leo (Simha)", "Virgo (Kanya)", "Libra (Tula)", "Scorpio (Vrishchika)", "Sagittarius (Dhanu)", "Capricorn (Makara)", "Aquarius (Kumbha)", "Pisces (Meena)"];
const GOTRAS = ["Any", "Bharadwaj", "Kashyap", "Shandilya", "Vashishtha", "Vishwamitra", "Garga", "Atri", "Gautam", "Harita", "Jamadagni", "Kaushik", "Srivatsa", "Kaundinya", "Moudgalya", "Parashara", "Agastya", "Bhrigu", "Angirasa"];
const DOSHAS = ["Any", "No", "Manglik", "Sarpa Dosha", "Don't Know"];
const DIETS = ["Any", "Vegetarian", "Non-Vegetarian", "Vegan", "Eggetarian"];
const CITIES = ["Any", "Mumbai", "Delhi", "Bangalore", "Hyderabad", "Chennai", "Kolkata", "Pune", "Jaipur", "Ahmedabad", "Surat", "Lucknow", "Kanpur", "Nagpur", "Indore", "Thane", "Bhopal", "Visakhapatnam", "Patna", "Vadodara", "Ghaziabad", "Ludhiana", "Agra", "Nashik", "Faridabad", "Meerut", "Rajkot", "Varanasi", "Srinagar"];

const MOCK_PROFILES = [
  { userId: "u1", firstName: "Priya", age: 26, city: "Mumbai", profession: "Doctor", religion: "Hindu", caste: "Brahmin", subCaste: "Iyer", gothra: "Bharadwaj", nakshatra: "Rohini", rashi: "Taurus (Vrishabha)", dosha: "No", photo: "https://randomuser.me/api/portraits/women/44.jpg", isVerified: true, match: 94, education: "MBBS", income: "10-25L", height: 165, maritalStatus: "Never Married", motherTongue: "Tamil", foodHabit: "Vegetarian" },
  { userId: "u2", firstName: "Ananya", age: 24, city: "Bangalore", profession: "Engineer", religion: "Hindu", caste: "Reddy", subCaste: "Pokanati", gothra: "Kashyap", nakshatra: "Ashwini", rashi: "Aries (Mesha)", dosha: "Manglik", photo: "https://randomuser.me/api/portraits/women/45.jpg", isVerified: true, match: 89, education: "B.Tech", income: "10-25L", height: 160, maritalStatus: "Never Married", motherTongue: "Telugu", foodHabit: "Non-Vegetarian" },
  { userId: "u3", firstName: "Kavya", age: 28, city: "Hyderabad", profession: "Lawyer", religion: "Hindu", caste: "Kshatriya", subCaste: "Thakur", gothra: "Atri", nakshatra: "Bharani", rashi: "Aries (Mesha)", dosha: "Don't Know", photo: "https://randomuser.me/api/portraits/women/49.jpg", isVerified: false, match: 75, education: "LLB", income: "3-5L", height: 155, maritalStatus: "Never Married", motherTongue: "Hindi", foodHabit: "Vegetarian" },
  { userId: "u4", firstName: "Meera", age: 29, city: "Jaipur", profession: "Govt. Officer", religion: "Hindu", caste: "Brahmin", subCaste: "Gaur", gothra: "Gautam", nakshatra: "Mula", rashi: "Sagittarius (Dhanu)", dosha: "No", photo: "https://randomuser.me/api/portraits/women/50.jpg", isVerified: true, match: 72, education: "IAS", income: "5-10L", height: 163, maritalStatus: "Never Married", motherTongue: "Hindi", foodHabit: "Vegetarian" },
  { userId: "u5", firstName: "Sara", age: 27, city: "Delhi", profession: "Designer", religion: "Muslim", caste: "Any", subCaste: "Any", gothra: "Any", nakshatra: "Any", rashi: "Any", dosha: "Any", photo: "https://randomuser.me/api/portraits/women/52.jpg", isVerified: true, match: 83, education: "B.Des", income: "5-10L", height: 158, maritalStatus: "Never Married", motherTongue: "English", foodHabit: "Non-Vegetarian" },
  { userId: "u6", firstName: "Nisha", age: 25, city: "Pune", profession: "Software Engineer", religion: "Hindu", caste: "Kayastha", subCaste: "Saxena", gothra: "Kaushik", nakshatra: "Hasta", rashi: "Virgo (Kanya)", dosha: "No", photo: "https://randomuser.me/api/portraits/women/31.jpg", isVerified: true, match: 88, education: "B.Tech", income: "10-25L", height: 162, maritalStatus: "Never Married", motherTongue: "Hindi", foodHabit: "Eggetarian" },
];

const INITIAL_FILTERS = {
  query: "",
  minAge: 18,
  maxAge: 40,
  minMatch: 0,
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
  verifiedOnly: false,
  shortlistedOnly: false,
};

const MATCHES_VIEW_KEY = "matches:viewMode";
const MATCHES_SORT_KEY = "matches:sortBy";
const GRID_BATCH_SIZE = 12;
const LIST_BATCH_SIZE = 8;

function clampNumber(value, min, max) {
  const number = Number(value);
  if (Number.isNaN(number)) return min;
  return Math.min(max, Math.max(min, number));
}

function normalizeOptions(source, fallback, ensureAny = true) {
  if (!Array.isArray(source) || source.length === 0) {
    return fallback;
  }

  const normalized = Array.from(
    new Set(
      source
        .map((item) => (typeof item === "string" ? item.trim() : ""))
        .filter(Boolean)
    )
  );

  if (!ensureAny) {
    return normalized;
  }

  if (!normalized.some((item) => item.toLowerCase() === "any")) {
    normalized.unshift("Any");
  }

  return normalized;
}

function getMatchTier(matchScore) {
  if (matchScore >= 90) return "elite";
  if (matchScore >= 80) return "strong";
  return "rising";
}

function MatchActionIcon({ kind, active = false }) {
  if (kind === "star") {
    return (
      <svg
        viewBox="0 0 24 24"
        fill={active ? "currentColor" : "none"}
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <path d="m12 3.8 2.6 5.3 5.9.9-4.3 4.2 1 5.9-5.2-2.8-5.2 2.8 1-5.9-4.3-4.2 5.9-.9L12 3.8Z" />
      </svg>
    );
  }

  if (kind === "open") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M8 16 16 8M10 8h6v6" />
      </svg>
    );
  }

  if (kind === "grid") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M4.5 4.5h6v6h-6zM13.5 4.5h6v6h-6zM4.5 13.5h6v6h-6zM13.5 13.5h6v6h-6z" />
      </svg>
    );
  }

  if (kind === "list") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M9 6h11M9 12h11M9 18h11M4.5 6h.01M4.5 12h.01M4.5 18h.01" />
      </svg>
    );
  }

  if (kind === "chevronDown") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="m6 9 6 6 6-6" />
      </svg>
    );
  }

  if (kind === "chevronUp") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="m6 15 6-6 6 6" />
      </svg>
    );
  }

  return null;
}

const MatchCard = memo(function MatchCard({ profile, isShortlisted, onShortlist, onInterest, onOpenProfile, viewMode }) {
  const matchTier = getMatchTier(Number(profile.match) || 0);
  const metaLine = [profile.profession, profile.city, profile.height ? `${profile.height}cm` : null]
    .filter(Boolean)
    .join(" | ");

  if (viewMode === "list") {
    return (
      <article className="panel panel-hover anim-rise listing-stage match-card match-card-list" style={{ overflow: "hidden", display: "flex" }}>
        <div className="match-media" style={{ width: 208, flexShrink: 0, position: "relative" }}>
          <Image
            className="match-photo"
            src={profile.photo}
            alt={`${profile.firstName} profile`}
            width={720}
            height={900}
            sizes="(max-width: 760px) 100vw, 208px"
            style={{ width: "100%", height: "100%", objectFit: "cover" }}
          />
          <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(7, 13, 30, 0.72), transparent 55%)" }} />
          <div style={{ position: "absolute", top: 10, left: 10 }}>
            <span className={`match-badge match-badge-${matchTier}`}>{profile.match}% Match</span>
          </div>
        </div>
        <div className="match-card-content" style={{ padding: "1rem", flex: 1, minWidth: 0 }}>
          <div style={{ display: "flex", justifyContent: "space-between", gap: "0.72rem", alignItems: "flex-start", marginBottom: "0.7rem" }}>
            <div style={{ minWidth: 0 }}>
              <h3 style={{ margin: 0, fontSize: "1.12rem", lineHeight: 1.15 }}>
                {profile.firstName}, {profile.age}
              </h3>
              <p className="profile-meta" style={{ margin: "0.26rem 0 0", color: "var(--ink-muted)", fontSize: "0.84rem" }}>
                {metaLine}
              </p>
            </div>
            <button
              type="button"
              onClick={() => onShortlist(profile.userId)}
              className="button button-secondary shortlist-fab icon-only-btn"
              style={{ width: 40, height: 40, padding: 0, borderRadius: 12, fontWeight: 700, flexShrink: 0 }}
              aria-label={isShortlisted ? "Unsave profile" : "Save profile"}
              title={isShortlisted ? "Unsave profile" : "Save profile"}
            >
              <MatchActionIcon kind="star" active={isShortlisted} />
            </button>
          </div>

          <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap", marginTop: "0.66rem" }}>
            {profile.isVerified && <span className="chip chip-support">Verified</span>}
            {profile.religion && <span className="chip chip-brand">{profile.religion}</span>}
            {profile.education && <span className="chip chip-brand">{profile.education}</span>}
            {profile.income && <span className="chip chip-brand">{profile.income}</span>}
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "0.54rem", marginTop: "0.9rem" }}>
            <button type="button" className="button button-primary" onClick={() => onInterest(profile.userId)}>
              Send Interest
            </button>
            <Link href={`/profile/${profile.userId}`} className="button button-secondary icon-only-btn view-fab" style={{ padding: "0.72rem 0.92rem" }} onClick={() => onOpenProfile(profile.userId)} aria-label="Open profile" title="Open profile">
              <MatchActionIcon kind="open" />
            </Link>
          </div>
        </div>
      </article>
    );
  }

  return (
    <article className="panel panel-hover anim-rise listing-stage match-card" style={{ overflow: "hidden" }}>
      <div className="match-media" style={{ position: "relative", height: 220 }}>
        <Image
          className="match-photo"
          src={profile.photo}
          alt={`${profile.firstName} profile`}
          width={720}
          height={900}
          sizes="(max-width: 760px) 100vw, (max-width: 1200px) 50vw, 33vw"
          style={{ width: "100%", height: "100%", objectFit: "cover" }}
        />
        <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(9, 18, 36, 0.78), rgba(9, 18, 36, 0.1) 58%, transparent)" }} />
        <div style={{ position: "absolute", top: 10, left: 10 }}>
          <span className={`match-badge match-badge-${matchTier}`}>{profile.match}% Match</span>
        </div>
        <button
          type="button"
          onClick={() => onShortlist(profile.userId)}
          className="button button-secondary shortlist-fab icon-only-btn"
          style={{ position: "absolute", top: 10, right: 10, width: 40, height: 40, padding: 0, borderRadius: 12, fontWeight: 700 }}
          aria-label={isShortlisted ? "Unsave profile" : "Save profile"}
          title={isShortlisted ? "Unsave profile" : "Save profile"}
        >
          <MatchActionIcon kind="star" active={isShortlisted} />
        </button>
        <div style={{ position: "absolute", left: 12, bottom: 12, color: "white", right: 12 }}>
          <h3 style={{ margin: 0, fontSize: "1.16rem", lineHeight: 1.14 }}>
            {profile.firstName}, {profile.age}
          </h3>
          <p className="profile-meta" style={{ margin: "0.2rem 0 0", opacity: 0.9, fontSize: "0.82rem" }}>
            {metaLine}
          </p>
        </div>
      </div>

      <div style={{ padding: "0.94rem" }}>
        <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap", marginBottom: "0.74rem" }}>
          {profile.isVerified && <span className="chip chip-support">Verified</span>}
          {profile.religion && <span className="chip chip-brand">{profile.religion}</span>}
          {profile.education && <span className="chip chip-brand">{profile.education}</span>}
          {profile.income && <span className="chip chip-brand">{profile.income}</span>}
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "0.52rem" }}>
          <button type="button" className="button button-primary" onClick={() => onInterest(profile.userId)}>
            Send Interest
          </button>
          <Link href={`/profile/${profile.userId}`} className="button button-secondary icon-only-btn view-fab" style={{ padding: "0.74rem 0.94rem" }} onClick={() => onOpenProfile(profile.userId)} aria-label="Open profile" title="Open profile">
            <MatchActionIcon kind="open" />
          </Link>
        </div>
      </div>
    </article>
  );
});

function MatchesContent() {
  const searchParams = useSearchParams();
  const { user } = useAuth();
  const { showLoginModal, setShowLoginModal } = useGuestScrollGate();

  const [loading, setLoading] = useState(true);
  const [rawProfiles, setRawProfiles] = useState([]);
  const [shortlisted, setShortlisted] = useState(new Set());
  const [viewMode, setViewMode] = useState("grid");
  const [sortBy, setSortBy] = useState("compatibility");
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState(INITIAL_FILTERS);
  const [filterMeta, setFilterMeta] = useState(FALLBACK_FILTER_META);
  const [hasLoadedFilterMeta, setHasLoadedFilterMeta] = useState(false);

  useEffect(() => {
    const queryFromParams = searchParams.get("q");
    const religionFromParams = searchParams.get("religion");
    const minAgeFromParams = searchParams.get("minAge");
    const maxAgeFromParams = searchParams.get("maxAge");
    const minMatchFromParams = searchParams.get("minMatch");
    const verifiedFromParams = searchParams.get("verified");
    const savedFromParams = searchParams.get("saved");

    const nextFilters = {};
    if (typeof queryFromParams === "string") {
      nextFilters.query = queryFromParams;
    }
    if (religionFromParams) {
      nextFilters.religion = religionFromParams;
    }
    if (minAgeFromParams !== null) {
      nextFilters.minAge = clampNumber(minAgeFromParams, 18, 70);
    }
    if (maxAgeFromParams !== null) {
      nextFilters.maxAge = clampNumber(maxAgeFromParams, 18, 70);
    }
    if (minMatchFromParams !== null) {
      nextFilters.minMatch = clampNumber(minMatchFromParams, 0, 100);
    }
    if (verifiedFromParams === "1") {
      nextFilters.verifiedOnly = true;
    }
    if (savedFromParams === "1") {
      nextFilters.shortlistedOnly = true;
    }

    if (Object.keys(nextFilters).length > 0) {
      setFilters((previous) => {
        const merged = { ...previous, ...nextFilters };
        if (merged.maxAge < merged.minAge) {
          merged.maxAge = merged.minAge;
        }
        return merged;
      });
    }

    const shouldOpenFilters =
      searchParams.get("filters") === "1" ||
      Boolean(queryFromParams) ||
      Boolean(religionFromParams) ||
      minAgeFromParams !== null ||
      maxAgeFromParams !== null ||
      minMatchFromParams !== null ||
      verifiedFromParams === "1" ||
      savedFromParams === "1";

    if (shouldOpenFilters) {
      setShowFilters(true);
    }
  }, [searchParams]);

  useEffect(() => {
    if (!showFilters || hasLoadedFilterMeta) return;

    let cancelled = false;

    const loadFilterMeta = async () => {
      try {
        const response = await getCachedRequest("meta:filters", () => api.get("/meta/filters"), 5 * 60_000);
        const payload = response?.data?.data || response?.data || {};
        if (cancelled) return;

        setFilterMeta({
          religion: normalizeOptions(payload.religion, FALLBACK_FILTER_META.religion),
          caste: normalizeOptions(payload.caste, FALLBACK_FILTER_META.caste),
          income: normalizeOptions(payload.income, FALLBACK_FILTER_META.income),
          maritalStatus: normalizeOptions(payload.maritalStatus, FALLBACK_FILTER_META.maritalStatus),
          motherTongue: normalizeOptions(payload.motherTongue, FALLBACK_FILTER_META.motherTongue),
          education: normalizeOptions(payload.education, FALLBACK_FILTER_META.education),
          profession: normalizeOptions(payload.profession, FALLBACK_FILTER_META.profession),
        });
        setHasLoadedFilterMeta(true);
      } catch {
        if (!cancelled) {
          setFilterMeta(FALLBACK_FILTER_META);
          setHasLoadedFilterMeta(true);
        }
      }
    };

    loadFilterMeta();
    return () => {
      cancelled = true;
    };
  }, [showFilters, hasLoadedFilterMeta]);

  useEffect(() => {
    const loadMatches = async () => {
      setLoading(true);
      try {
        if (!user) {
          setRawProfiles(MOCK_PROFILES);
          return;
        }

        const gender = searchParams.get("gender") || "female";
        const response = await getCachedRequest(
          `matches:${gender}`,
          () => api.get("/matches", { params: { gender } }),
          45_000
        );
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

  useEffect(() => {
    if (typeof window === "undefined") return;
    const frame = window.requestAnimationFrame(() => {
      try {
        const storedViewMode = window.localStorage.getItem(MATCHES_VIEW_KEY);
        if (storedViewMode === "grid" || storedViewMode === "list") {
          setViewMode(storedViewMode);
        }

        const storedSortBy = window.localStorage.getItem(MATCHES_SORT_KEY);
        if (storedSortBy && ["compatibility", "verified", "ageAsc", "ageDesc", "matchAsc"].includes(storedSortBy)) {
          setSortBy(storedSortBy);
        }
      } catch {}
    });

    return () => window.cancelAnimationFrame(frame);
  }, []);

  useEffect(() => {
    if (typeof window === "undefined") return;
    try {
      window.localStorage.setItem(MATCHES_VIEW_KEY, viewMode);
      window.localStorage.setItem(MATCHES_SORT_KEY, sortBy);
    } catch {}
  }, [viewMode, sortBy]);

  const onFilterChange = (key, value) => {
    setFilters((previous) => {
      const next = { ...previous, [key]: value };
      if (key === "caste") next.subCaste = "Any";
      if (key === "minAge" && Number(next.minAge) > Number(next.maxAge)) {
        next.maxAge = next.minAge;
      }
      if (key === "maxAge" && Number(next.maxAge) < Number(next.minAge)) {
        next.minAge = next.maxAge;
      }
      if (key === "minHeight" && Number(next.minHeight) > Number(next.maxHeight)) {
        next.maxHeight = next.minHeight;
      }
      if (key === "maxHeight" && Number(next.maxHeight) < Number(next.minHeight)) {
        next.minHeight = next.maxHeight;
      }
      return next;
    });
  };

  const resetFilters = () => setFilters({ ...INITIAL_FILTERS });

  const clearFilter = (key) => {
    setFilters((previous) => {
      if (key === "age") {
        return { ...previous, minAge: INITIAL_FILTERS.minAge, maxAge: INITIAL_FILTERS.maxAge };
      }
      if (key === "height") {
        return { ...previous, minHeight: INITIAL_FILTERS.minHeight, maxHeight: INITIAL_FILTERS.maxHeight };
      }
      if (key === "caste") {
        return { ...previous, caste: INITIAL_FILTERS.caste, subCaste: INITIAL_FILTERS.subCaste };
      }
      return { ...previous, [key]: INITIAL_FILTERS[key] };
    });
  };

  const subcasteOptions = useMemo(() => ["Any", ...(SUBCASTE_MAP[filters.caste] || [])], [filters.caste]);

  const visibleProfiles = useMemo(() => {
    const query = filters.query.trim().toLowerCase();
    const filtered = rawProfiles.filter((item) => {
      if (item.age < Number(filters.minAge) || item.age > Number(filters.maxAge)) return false;
      if (Number(item.match || 0) < Number(filters.minMatch || 0)) return false;
      if (filters.religion !== "Any" && item.religion !== filters.religion) return false;
      if (filters.caste !== "Any" && item.caste !== filters.caste) return false;
      if (filters.subCaste !== "Any" && (item.subCaste || "Any").toLowerCase() !== filters.subCaste.toLowerCase()) return false;
      if (filters.gothra !== "Any" && item.gothra !== filters.gothra) return false;
      if (filters.nakshatra !== "Any" && item.nakshatra !== filters.nakshatra) return false;
      if (filters.rashi !== "Any" && item.rashi !== filters.rashi) return false;
      if (filters.dosha !== "Any" && item.dosha !== filters.dosha) return false;
      if (filters.income !== "Any" && item.income !== filters.income) return false;
      if (filters.city !== "Any" && item.city !== filters.city) return false;
      if (filters.education !== "Any" && !(item.education || "").includes(filters.education)) return false;
      if (filters.profession !== "Any" && !(item.profession || "").includes(filters.profession)) return false;
      if (item.height && (item.height < Number(filters.minHeight) || item.height > Number(filters.maxHeight))) return false;
      if (filters.maritalStatus !== "Any" && item.maritalStatus !== filters.maritalStatus) return false;
      if (filters.motherTongue !== "Any" && item.motherTongue !== filters.motherTongue) return false;
      if (filters.diet !== "Any" && item.foodHabit !== filters.diet) return false;
      if (filters.verifiedOnly && !item.isVerified) return false;
      if (filters.shortlistedOnly && !shortlisted.has(item.userId)) return false;
      if (!query) return true;
      return [item.firstName, item.city, item.profession, item.religion, item.caste, item.subCaste, item.education].filter(Boolean).join(" ").toLowerCase().includes(query);
    });

    const sorted = [...filtered];
    if (sortBy === "compatibility") {
      sorted.sort((a, b) => Number(b.match || 0) - Number(a.match || 0));
    } else if (sortBy === "verified") {
      sorted.sort((a, b) => Number(Boolean(b.isVerified)) - Number(Boolean(a.isVerified)) || Number(b.match || 0) - Number(a.match || 0));
    } else if (sortBy === "ageAsc") {
      sorted.sort((a, b) => Number(a.age || 0) - Number(b.age || 0));
    } else if (sortBy === "ageDesc") {
      sorted.sort((a, b) => Number(b.age || 0) - Number(a.age || 0));
    } else if (sortBy === "matchAsc") {
      sorted.sort((a, b) => Number(a.match || 0) - Number(b.match || 0));
    }

    return sorted;
  }, [rawProfiles, filters, sortBy, shortlisted]);

  const profileInsights = useMemo(() => {
    const total = visibleProfiles.length;
    const verified = visibleProfiles.filter((item) => item.isVerified).length;
    const highCompatibility = visibleProfiles.filter((item) => Number(item.match) >= 85).length;
    const averageCompatibility = total > 0
      ? Math.round(visibleProfiles.reduce((sum, item) => sum + Number(item.match || 0), 0) / total)
      : 0;
    return { total, verified, highCompatibility, averageCompatibility };
  }, [visibleProfiles]);

  const activeFilterTags = useMemo(() => {
    const tags = [];
    if (filters.query.trim()) tags.push({ key: "query", label: `Query: ${filters.query.trim()}` });
    if (Number(filters.minMatch) > 0) tags.push({ key: "minMatch", label: `Min match ${filters.minMatch}%` });
    if (Number(filters.minAge) !== INITIAL_FILTERS.minAge || Number(filters.maxAge) !== INITIAL_FILTERS.maxAge) {
      tags.push({ key: "age", label: `Age ${filters.minAge}-${filters.maxAge}` });
    }
    if (filters.religion !== "Any") tags.push({ key: "religion", label: filters.religion });
    if (filters.caste !== "Any") tags.push({ key: "caste", label: filters.caste });
    if (filters.city !== "Any") tags.push({ key: "city", label: filters.city });
    if (filters.profession !== "Any") tags.push({ key: "profession", label: filters.profession });
    if (Number(filters.minHeight) !== INITIAL_FILTERS.minHeight || Number(filters.maxHeight) !== INITIAL_FILTERS.maxHeight) {
      tags.push({ key: "height", label: `Height ${filters.minHeight}-${filters.maxHeight}cm` });
    }
    if (filters.verifiedOnly) tags.push({ key: "verifiedOnly", label: "Verified only" });
    if (filters.shortlistedOnly) tags.push({ key: "shortlistedOnly", label: "Saved only" });
    return tags;
  }, [filters]);

  const activeFilterCount = useMemo(() => {
    const checks = [
      filters.query.trim() !== "",
      Number(filters.minMatch) !== INITIAL_FILTERS.minMatch,
      Number(filters.minAge) !== INITIAL_FILTERS.minAge,
      Number(filters.maxAge) !== INITIAL_FILTERS.maxAge,
      filters.religion !== INITIAL_FILTERS.religion,
      filters.caste !== INITIAL_FILTERS.caste,
      filters.subCaste !== INITIAL_FILTERS.subCaste,
      filters.gothra !== INITIAL_FILTERS.gothra,
      filters.nakshatra !== INITIAL_FILTERS.nakshatra,
      filters.rashi !== INITIAL_FILTERS.rashi,
      filters.dosha !== INITIAL_FILTERS.dosha,
      filters.income !== INITIAL_FILTERS.income,
      filters.city !== INITIAL_FILTERS.city,
      filters.education !== INITIAL_FILTERS.education,
      filters.profession !== INITIAL_FILTERS.profession,
      Number(filters.minHeight) !== INITIAL_FILTERS.minHeight,
      Number(filters.maxHeight) !== INITIAL_FILTERS.maxHeight,
      filters.maritalStatus !== INITIAL_FILTERS.maritalStatus,
      filters.motherTongue !== INITIAL_FILTERS.motherTongue,
      filters.diet !== INITIAL_FILTERS.diet,
      filters.verifiedOnly !== INITIAL_FILTERS.verifiedOnly,
      filters.shortlistedOnly !== INITIAL_FILTERS.shortlistedOnly,
    ];
    return checks.filter(Boolean).length;
  }, [filters]);

  const batchSize = viewMode === "grid" ? GRID_BATCH_SIZE : LIST_BATCH_SIZE;
  const [visibleCount, setVisibleCount] = useState(batchSize);

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      setVisibleCount(batchSize);
    });
    return () => window.cancelAnimationFrame(frame);
  }, [batchSize, filters, sortBy, rawProfiles]);

  const renderedProfiles = useMemo(() => visibleProfiles.slice(0, visibleCount), [visibleProfiles, visibleCount]);
  const hasMoreProfiles = visibleCount < visibleProfiles.length;

  const onInterest = async (userId) => {
    if (!user) {
      setShowLoginModal(true);
      return;
    }
    try {
      const response = await api.post("/interactions/like", { receiverId: userId });
      await api.post("/matches/feedback", { targetUserId: userId, action: "like", source: "matches_feed" });
      if (response.data?.matchId) toast.success("It is a match. Start chatting now.");
      else toast.success("Interest sent successfully.");
    } catch {
      toast.info("Interest flow is in demo mode.");
    }
  };

  const onOpenProfile = async (userId) => {
    if (!user) return;
    try {
      await api.post("/matches/feedback", { targetUserId: userId, action: "open", source: "matches_feed" });
    } catch {
      // Non-blocking feedback event.
    }
  };

  const onShortlist = async (userId) => {
    if (!user) {
      setShowLoginModal(true);
      return;
    }

    const alreadyShortlisted = shortlisted.has(userId);
    setShortlisted((previous) => {
      const next = new Set(previous);
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
    <div
      style={{ display: "grid", gridTemplateColumns: showFilters ? "320px minmax(0, 1fr)" : "1fr", gap: "1rem" }}
      className={`matches-shell-grid ${showFilters ? "filters-open" : "filters-collapsed"}`}
    >
      {showFilters && (
        <aside className="panel filter-panel premium-filter-panel" style={{ padding: "1rem", height: "fit-content", position: "sticky", top: "5.4rem" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.78rem", gap: "0.5rem" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "0.45rem", flexWrap: "wrap" }}>
            <p className="section-label" style={{ margin: 0 }}>
              Filters
            </p>
            <span className="filter-count-chip">{activeFilterCount > 0 ? `${activeFilterCount} active` : "No filters"}</span>
          </div>
          <div>
            <button type="button" className="button button-secondary" onClick={resetFilters}>
              Reset
            </button>
          </div>
        </div>

        <div style={{ display: "grid", gap: "0.64rem" }}>
          <div>
            <label className="form-label">Search keyword</label>
            <input className="form-input" value={filters.query} onChange={(event) => onFilterChange("query", event.target.value)} placeholder="Name, city, caste, profession" />
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.48rem" }}>
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
            <label className="form-label">Minimum compatibility</label>
            <select className="form-input" value={filters.minMatch} onChange={(event) => onFilterChange("minMatch", clampNumber(event.target.value, 0, 100))}>
              <option value={0}>Any score</option>
              <option value={70}>70% and above</option>
              <option value={80}>80% and above</option>
              <option value={90}>90% and above</option>
            </select>
          </div>

          <div className="filter-toggle-row">
            <label className="filter-toggle">
              <input type="checkbox" checked={filters.verifiedOnly} onChange={(event) => onFilterChange("verifiedOnly", event.target.checked)} />
              <span>Verified only</span>
            </label>
            <label className="filter-toggle">
              <input type="checkbox" checked={filters.shortlistedOnly} onChange={(event) => onFilterChange("shortlistedOnly", event.target.checked)} />
              <span>Saved only</span>
            </label>
          </div>

          <div>
            <label className="form-label">Religion</label>
            <select className="form-input" value={filters.religion} onChange={(event) => onFilterChange("religion", event.target.value)}>
              {filterMeta.religion.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.48rem" }}>
            <div>
              <label className="form-label">Caste</label>
              <select className="form-input" value={filters.caste} onChange={(event) => onFilterChange("caste", event.target.value)}>
                {filterMeta.caste.map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="form-label">Sub-caste</label>
              <select className="form-input" value={filters.subCaste} onChange={(event) => onFilterChange("subCaste", event.target.value)}>
                {subcasteOptions.map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="form-label">City</label>
            <select className="form-input" value={filters.city} onChange={(event) => onFilterChange("city", event.target.value)}>
              {CITIES.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label">Profession</label>
            <select className="form-input" value={filters.profession} onChange={(event) => onFilterChange("profession", event.target.value)}>
              {filterMeta.profession.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>

          <details className="filter-disclosure">
            <summary>Advanced filters</summary>
            <div style={{ display: "grid", gap: "0.64rem", marginTop: "0.56rem" }}>
              <div>
                <label className="form-label">Education</label>
                <select className="form-input" value={filters.education} onChange={(event) => onFilterChange("education", event.target.value)}>
                  {filterMeta.education.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="form-label">Income</label>
                <select className="form-input" value={filters.income} onChange={(event) => onFilterChange("income", event.target.value)}>
                  {filterMeta.income.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.48rem" }}>
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
                  {filterMeta.maritalStatus.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="form-label">Mother tongue</label>
                <select className="form-input" value={filters.motherTongue} onChange={(event) => onFilterChange("motherTongue", event.target.value)}>
                  {filterMeta.motherTongue.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="form-label">Diet</label>
                <select className="form-input" value={filters.diet} onChange={(event) => onFilterChange("diet", event.target.value)}>
                  {DIETS.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>

              <details style={{ marginTop: "0.2rem" }}>
                <summary style={{ cursor: "pointer", fontWeight: 760, color: "var(--ink)", fontSize: "0.9rem" }}>Horoscope filters</summary>
                <div style={{ display: "grid", gap: "0.64rem", marginTop: "0.64rem" }}>
                  <div>
                    <label className="form-label">Gothra</label>
                    <select className="form-input" value={filters.gothra} onChange={(event) => onFilterChange("gothra", event.target.value)}>
                      {GOTRAS.map((value) => (
                        <option key={value}>{value}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="form-label">Nakshatra</label>
                    <select className="form-input" value={filters.nakshatra} onChange={(event) => onFilterChange("nakshatra", event.target.value)}>
                      {NAKSHATRAS.map((value) => (
                        <option key={value}>{value}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="form-label">Rashi</label>
                    <select className="form-input" value={filters.rashi} onChange={(event) => onFilterChange("rashi", event.target.value)}>
                      {RASHIS.map((value) => (
                        <option key={value}>{value}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="form-label">Dosha</label>
                    <select className="form-input" value={filters.dosha} onChange={(event) => onFilterChange("dosha", event.target.value)}>
                      {DOSHAS.map((value) => (
                        <option key={value}>{value}</option>
                      ))}
                    </select>
                  </div>
                </div>
              </details>
            </div>
          </details>
        </div>
      </aside>
      )}

      <section>
        <div className="listing-hero">
          <div className="hero-copy">
            <p className="section-label" style={{ marginBottom: "0.2rem" }}>
              Curated Profiles
            </p>
            <h1 className="section-title" style={{ fontSize: "clamp(1.64rem, 3vw, 2.24rem)", margin: 0 }}>
              My Matches
            </h1>
            <p className="section-copy" style={{ margin: "0.35rem 0 0", fontSize: "0.9rem" }}>
              {loading ? "Finding your best matches..." : `${visibleProfiles.length} compatible profiles found`}
            </p>
            {!loading && (
              <div className="result-metrics">
                <span className="metric-chip metric-chip-highlight">{profileInsights.total} curated</span>
                <span className="metric-chip">{profileInsights.verified} verified</span>
                <span className="metric-chip">{profileInsights.highCompatibility} high compatibility</span>
                <span className="metric-chip">{profileInsights.averageCompatibility}% avg match</span>
                <span className="metric-chip">{shortlisted.size} saved</span>
              </div>
            )}
            {!loading && activeFilterTags.length > 0 && (
              <div className="active-filter-row">
                {activeFilterTags.map((tag) => (
                  <button key={tag.key} type="button" className="active-filter-pill" onClick={() => clearFilter(tag.key)}>
                    <span>{tag.label}</span>
                    <span aria-hidden="true">x</span>
                  </button>
                ))}
                <button type="button" className="active-filter-pill active-filter-pill-clear" onClick={resetFilters}>
                  Clear all
                </button>
              </div>
            )}
          </div>

          <div className="hero-actions" style={{ display: "flex", gap: "0.45rem", alignItems: "center" }}>
            <button
              type="button"
              className={`button toolbar-filter-btn ${showFilters ? "button-primary" : "button-secondary"}`}
              onClick={() => setShowFilters((previous) => !previous)}
              aria-label={showFilters ? "Hide filters" : "Show filters"}
              title={showFilters ? "Hide filters" : "Show filters"}
            >
              <span>Filters</span>
              <span className="toolbar-arrow" aria-hidden="true">
                <MatchActionIcon kind={showFilters ? "chevronUp" : "chevronDown"} />
              </span>
              {activeFilterCount > 0 && <span className="toolbar-count">{activeFilterCount}</span>}
            </button>
            <div className="sort-control">
              <label className="form-label" htmlFor="sortBy" style={{ marginBottom: "0.25rem" }}>
                Sort by
              </label>
              <select id="sortBy" className="form-input" value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
                <option value="compatibility">Top Compatibility</option>
                <option value="verified">Verified First</option>
                <option value="ageAsc">Age: Low to High</option>
                <option value="ageDesc">Age: High to Low</option>
                <option value="matchAsc">Compatibility: Low to High</option>
              </select>
            </div>
            <button type="button" className={`button view-switch-btn icon-only-btn ${viewMode === "grid" ? "button-primary" : "button-secondary"}`} onClick={() => setViewMode("grid")} style={{ padding: "0.66rem 0.92rem" }} aria-label="Grid view" title="Grid view">
              <MatchActionIcon kind="grid" />
            </button>
            <button type="button" className={`button view-switch-btn icon-only-btn ${viewMode === "list" ? "button-primary" : "button-secondary"}`} onClick={() => setViewMode("list")} style={{ padding: "0.66rem 0.92rem" }} aria-label="List view" title="List view">
              <MatchActionIcon kind="list" />
            </button>
          </div>
        </div>

        {loading ? (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(230px, 1fr))", gap: "0.9rem" }}>
            {Array.from({ length: 6 }).map((_, index) => (
              <div key={`skeleton-${index}`} className="panel listing-stage skeleton-tile" style={{ height: 334 }} />
            ))}
          </div>
        ) : visibleProfiles.length === 0 ? (
          <PageEmptyState
            title="No matches found with current filters"
            description="Try broadening caste or city filters, or disable horoscope filters."
            primaryActionLabel="Reset Filters"
            onPrimaryAction={resetFilters}
            secondaryActionLabel={!showFilters ? "Open Filters" : ""}
            onSecondaryAction={() => setShowFilters(true)}
          />
        ) : (
          <div className="results-grid" style={{ display: "grid", gridTemplateColumns: viewMode === "grid" ? "repeat(auto-fit, minmax(240px, 1fr))" : "1fr", gap: "0.9rem" }}>
            {renderedProfiles.map((profile) => (
              <MatchCard key={profile.userId} profile={profile} isShortlisted={shortlisted.has(profile.userId)} onShortlist={onShortlist} onInterest={onInterest} onOpenProfile={onOpenProfile} viewMode={viewMode} />
            ))}
          </div>
        )}
        {!loading && hasMoreProfiles && (
          <div className="matches-load-more-row">
            <button type="button" className="button button-secondary" onClick={() => setVisibleCount((previous) => previous + batchSize)}>
              Load more profiles
            </button>
            <span>
              Showing {renderedProfiles.length} of {visibleProfiles.length}
            </span>
          </div>
        )}
      </section>

      <LoginPromptModal isOpen={showLoginModal} onClose={() => setShowLoginModal(false)} />

    </div>
  );
}

export default function MatchesPage() {
  return (
    <Suspense
      fallback={
        <PageLoadingState
          title="Loading matches..."
          description="Preparing curated matches and compatibility insights."
          compact
        />
      }
    >
      <MatchesContent />
    </Suspense>
  );
}
