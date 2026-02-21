"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { toast } from "react-toastify";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import LoginPromptModal from "../../../components/LoginPromptModal";

const FALLBACK_FILTER_META = {
  gender: ["Bride (Woman)", "Groom (Man)"],
  religion: ["Any", "Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist", "Parsi"],
  caste: ["Any", "Brahmin", "Kshatriya", "Vaishya", "Kayastha", "Rajput", "Reddy", "Naidu", "Chettiar", "Jat"],
  motherTongue: ["Any", "Hindi", "English", "Marathi", "Tamil", "Telugu", "Kannada", "Malayalam", "Bengali", "Gujarati", "Punjabi", "Urdu"],
  income: ["Any", "Below 3L", "3-5L", "5-10L", "10-25L", "25-50L", "50L+"],
  education: ["Any", "10th", "12th", "Diploma", "Graduate", "Post Graduate", "Doctorate"],
  profession: ["Any", "Engineer", "Doctor", "CA/Finance", "Govt/PSU", "Lawyer", "Teacher", "Business", "NRI", "Other"],
  maritalStatus: ["Any", "Never Married", "Divorced", "Widowed", "Separated"],
  height: ["Any", "Below 5ft", "5ft-5.5ft", "5.5ft-6ft", "6ft-6.5ft", "Above 6.5ft"],
  bodyType: ["Any", "Slim", "Average", "Athletic", "Heavy"],
};

const SEARCH_MODES = [
  { value: "regular", label: "Regular Search" },
  { value: "advanced", label: "Advanced Search" },
  { value: "keyword", label: "Keyword Search" },
  { value: "profile_id", label: "Profile ID Search" },
];

const SORT_OPTIONS = [
  { value: "relevance", label: "Relevance" },
  { value: "compatibility", label: "Compatibility" },
  { value: "activity", label: "Last Active" },
  { value: "newest", label: "Newest" },
];

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
  profileId: "",
  lookingFor: "Bride (Woman)",
  minAge: 22,
  maxAge: 35,
  religion: "Any",
  caste: "Any",
  motherTongue: "Any",
  maritalStatus: "Any",
  education: "Any",
  profession: "Any",
  income: "Any",
  minHeight: 140,
  maxHeight: 200,
  body: "Any",
  city: "",
  state: "",
  verifiedOnly: false,
  withPhotoOnly: false,
  premiumOnly: false,
  onlineNow: false,
  withHoroscopeOnly: false,
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
            {profile.isPremium && <span className="chip chip-support">Premium</span>}
            {profile.religion && <span className="chip chip-brand">{profile.religion}</span>}
            {profile.caste && <span className="chip chip-brand">{profile.caste}</span>}
            {profile.education && <span className="chip chip-brand">{profile.education}</span>}
            {profile.income && <span className="chip chip-brand">{profile.income}</span>}
          </div>

          {Array.isArray(profile.reasons) && profile.reasons.length > 0 && (
            <p style={{ margin: "0.55rem 0 0", color: "var(--ink-muted)", fontSize: "0.78rem" }}>
              {profile.reasons.join(" • ")}
            </p>
          )}

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
          {profile.isPremium && <span className="chip chip-support">Premium</span>}
          {profile.religion && <span className="chip chip-brand">{profile.religion}</span>}
          {profile.caste && <span className="chip chip-brand">{profile.caste}</span>}
          {profile.education && <span className="chip chip-brand">{profile.education}</span>}
        </div>

        {Array.isArray(profile.reasons) && profile.reasons.length > 0 && (
          <p style={{ margin: "0 0 0.7rem", color: "var(--ink-muted)", fontSize: "0.78rem" }}>
            {profile.reasons.join(" • ")}
          </p>
        )}

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
  const [mode, setMode] = useState("advanced");
  const [sort, setSort] = useState("relevance");
  const [results, setResults] = useState([]);
  const [pagination, setPagination] = useState({ page: 1, limit: 20, total: 0, hasNextPage: false });
  const [filterMeta, setFilterMeta] = useState(FALLBACK_FILTER_META);
  const [savedSearches, setSavedSearches] = useState([]);
  const [selectedSavedId, setSelectedSavedId] = useState("");
  const [shortlisted, setShortlisted] = useState(new Set());
  const [filters, setFilters] = useState(INITIAL_FILTERS);

  useEffect(() => {
    const incoming = searchParams.get("q") || "";
    const incomingMode = searchParams.get("mode") || "";
    if (incoming) {
      setFilters((previous) => ({ ...previous, query: incoming }));
    }
    if (incomingMode && SEARCH_MODES.some((item) => item.value === incomingMode)) {
      setMode(incomingMode);
    }
  }, [searchParams]);

  useEffect(() => {
    let cancelled = false;

    const loadFilterMeta = async () => {
      try {
        const response = await api.get("/meta/filters");
        const payload = response?.data?.data || response?.data || {};
        if (cancelled) return;

        setFilterMeta({
          gender: normalizeOptions(payload.gender, FALLBACK_FILTER_META.gender, false),
          religion: normalizeOptions(payload.religion, FALLBACK_FILTER_META.religion),
          caste: normalizeOptions(payload.caste, FALLBACK_FILTER_META.caste),
          motherTongue: normalizeOptions(payload.motherTongue, FALLBACK_FILTER_META.motherTongue),
          income: normalizeOptions(payload.income, FALLBACK_FILTER_META.income),
          education: normalizeOptions(payload.education, FALLBACK_FILTER_META.education),
          profession: normalizeOptions(payload.profession, FALLBACK_FILTER_META.profession),
          maritalStatus: normalizeOptions(payload.maritalStatus, FALLBACK_FILTER_META.maritalStatus),
          height: normalizeOptions(payload.height, FALLBACK_FILTER_META.height),
          bodyType: normalizeOptions(payload.bodyType, FALLBACK_FILTER_META.bodyType),
        });
      } catch {
        if (!cancelled) {
          setFilterMeta(FALLBACK_FILTER_META);
        }
      }
    };

    loadFilterMeta();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    const loadSavedSearches = async () => {
      if (!user) {
        setSavedSearches([]);
        return;
      }

      try {
        const response = await api.get("/search/saved");
        if (!cancelled) {
          setSavedSearches(Array.isArray(response.data) ? response.data : []);
        }
      } catch {
        if (!cancelled) setSavedSearches([]);
      }
    };

    loadSavedSearches();
    return () => {
      cancelled = true;
    };
  }, [user]);

  const visibleResults = useMemo(() => {
    if (user) {
      return results;
    }

    const query = filters.query.trim().toLowerCase();
    const city = filters.city.trim().toLowerCase();
    const state = filters.state.trim().toLowerCase();

    return results.filter((profile) => {
      if (profile.age < Number(filters.minAge) || profile.age > Number(filters.maxAge)) return false;
      if (filters.religion !== "Any" && profile.religion !== filters.religion) return false;
      if (filters.caste !== "Any" && profile.caste !== filters.caste) return false;
      if (filters.maritalStatus !== "Any" && profile.maritalStatus && profile.maritalStatus !== filters.maritalStatus) return false;
      if (filters.motherTongue !== "Any" && profile.motherTongue && profile.motherTongue !== filters.motherTongue) return false;
      if (filters.education !== "Any" && profile.education && profile.education !== filters.education) return false;
      if (filters.profession !== "Any" && !(profile.profession || "").toLowerCase().includes(filters.profession.toLowerCase())) return false;
      if (filters.income !== "Any" && profile.income && profile.income !== filters.income) return false;
      if (profile.heightCm && (profile.heightCm < Number(filters.minHeight) || profile.heightCm > Number(filters.maxHeight))) return false;
      if (filters.body !== "Any" && profile.body && profile.body !== filters.body) return false;
      if (city && !(profile.city || "").toLowerCase().includes(city)) return false;
      if (state && !(profile.state || "").toLowerCase().includes(state)) return false;
      if (filters.profileId && String(profile.userId || "").toLowerCase() !== String(filters.profileId).toLowerCase()) return false;

      if (!query) return true;
      const haystack = [profile.firstName, profile.city, profile.state, profile.profession, profile.religion, profile.caste].filter(Boolean).join(" ").toLowerCase();
      return haystack.includes(query);
    });
  }, [results, filters, user]);

  const onChange = (key, value) => {
    setFilters((previous) => ({ ...previous, [key]: value }));
  };

  const onReset = () => {
    setFilters(INITIAL_FILTERS);
    setMode("advanced");
    setSort("relevance");
    setSelectedSavedId("");
    setSearched(false);
    setResults([]);
    setPagination({ page: 1, limit: 20, total: 0, hasNextPage: false });
  };

  const syncSearchUrl = (params) => {
    if (typeof window === "undefined") return;
    const search = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value === undefined || value === null || value === "" || value === false) return;
      search.set(key, String(value));
    });
    const nextUrl = `${window.location.pathname}?${search.toString()}`;
    window.history.replaceState({}, "", nextUrl);
  };

  const buildSearchParams = (page = 1) => {
    const params = {
      mode,
      sort,
      page,
      limit: pagination.limit,
      gender: toApiGender(filters.lookingFor),
      minAge: clampNumber(filters.minAge, 18, 70),
      maxAge: clampNumber(filters.maxAge, 18, 70),
      minHeight: clampNumber(filters.minHeight, 120, 250),
      maxHeight: clampNumber(filters.maxHeight, 120, 250),
      verifiedOnly: Boolean(filters.verifiedOnly),
      withPhotoOnly: Boolean(filters.withPhotoOnly),
      premiumOnly: Boolean(filters.premiumOnly),
      onlineNow: Boolean(filters.onlineNow),
      withHoroscopeOnly: Boolean(filters.withHoroscopeOnly),
    };

    if (filters.religion !== "Any") params.religion = filters.religion;
    if (filters.caste !== "Any") params.caste = filters.caste;
    if (filters.maritalStatus !== "Any") params.maritalStatus = filters.maritalStatus;
    if (filters.motherTongue !== "Any") params.motherTongue = filters.motherTongue;
    if (filters.education !== "Any") params.education = filters.education;
    if (filters.profession !== "Any") params.profession = filters.profession;
    if (filters.income !== "Any") params.income = filters.income;
    if (filters.city.trim()) params.city = filters.city.trim();
    if (filters.state.trim()) params.state = filters.state.trim();
    if (filters.query.trim()) params.query = filters.query.trim();
    if (filters.profileId.trim()) params.profileId = filters.profileId.trim();

    return params;
  };

  const executeSearch = async (page = 1) => {
    setSearched(true);
    setLoading(true);

    try {
      if (!user) {
        setResults(MOCK_RESULTS);
        setPagination({ page: 1, limit: 20, total: MOCK_RESULTS.length, hasNextPage: false });
        return;
      }

      const params = buildSearchParams(page);
      syncSearchUrl(params);
      const response = await api.get("/search/execute", { params });
      const payload = response?.data?.items || [];
      setResults(payload);
      setPagination({
        page: response?.data?.page || page,
        limit: response?.data?.limit || pagination.limit,
        total: response?.data?.total || payload.length,
        hasNextPage: Boolean(response?.data?.hasNextPage),
      });
    } catch {
      setResults(MOCK_RESULTS);
      setPagination({ page: 1, limit: 20, total: MOCK_RESULTS.length, hasNextPage: false });
    } finally {
      setLoading(false);
    }
  };

  const onSearch = async (event) => {
    event.preventDefault();
    await executeSearch(1);
  };

  const saveCurrentSearch = async () => {
    if (!user) {
      setShowLoginModal(true);
      return;
    }

    const name = window.prompt("Save search as", `Search ${new Date().toLocaleDateString()}`);
    if (name === null) return;

    try {
      await api.post("/search/saved", {
        name,
        filters: {
          ...filters,
          mode,
          sort,
        },
      });
      const response = await api.get("/search/saved");
      setSavedSearches(Array.isArray(response.data) ? response.data : []);
      toast.success("Search saved.");
    } catch {
      toast.error("Could not save search.");
    }
  };

  const applySavedSearch = async (savedId) => {
    setSelectedSavedId(savedId);
    const found = savedSearches.find((item) => item.id === savedId);
    if (!found) return;

    const payload = found.filters || {};
    const { mode: savedMode, sort: savedSort, ...savedFilters } = payload;
    setFilters((previous) => ({
      ...previous,
      ...savedFilters,
      query: savedFilters.query || "",
      profileId: savedFilters.profileId || "",
    }));
    if (savedMode) setMode(savedMode);
    if (savedSort) setSort(savedSort);

    setTimeout(() => {
      executeSearch(1);
    }, 0);
  };

  const deleteSavedSearch = async () => {
    if (!selectedSavedId) return;
    try {
      await api.delete(`/search/saved/${selectedSavedId}`);
      const updated = savedSearches.filter((item) => item.id !== selectedSavedId);
      setSavedSearches(updated);
      setSelectedSavedId("");
      toast.success("Saved search deleted.");
    } catch {
      toast.error("Could not delete saved search.");
    }
  };

  const activeFilterChips = useMemo(() => {
    const chips = [];
    if (filters.query.trim()) chips.push({ key: "query", label: `Keyword: ${filters.query}` });
    if (filters.profileId.trim()) chips.push({ key: "profileId", label: `Profile ID: ${filters.profileId}` });
    if (filters.religion !== "Any") chips.push({ key: "religion", label: `Religion: ${filters.religion}` });
    if (filters.caste !== "Any") chips.push({ key: "caste", label: `Caste: ${filters.caste}` });
    if (filters.maritalStatus !== "Any") chips.push({ key: "maritalStatus", label: `Marital: ${filters.maritalStatus}` });
    if (filters.motherTongue !== "Any") chips.push({ key: "motherTongue", label: `Tongue: ${filters.motherTongue}` });
    if (filters.education !== "Any") chips.push({ key: "education", label: `Education: ${filters.education}` });
    if (filters.profession !== "Any") chips.push({ key: "profession", label: `Profession: ${filters.profession}` });
    if (filters.income !== "Any") chips.push({ key: "income", label: `Income: ${filters.income}` });
    if (filters.city.trim()) chips.push({ key: "city", label: `City: ${filters.city}` });
    if (filters.state.trim()) chips.push({ key: "state", label: `State: ${filters.state}` });
    if (filters.verifiedOnly) chips.push({ key: "verifiedOnly", label: "Verified only" });
    if (filters.withPhotoOnly) chips.push({ key: "withPhotoOnly", label: "With photo" });
    if (filters.premiumOnly) chips.push({ key: "premiumOnly", label: "Premium only" });
    if (filters.onlineNow) chips.push({ key: "onlineNow", label: "Online now" });
    if (filters.withHoroscopeOnly) chips.push({ key: "withHoroscopeOnly", label: "With horoscope" });
    return chips;
  }, [filters]);

  const removeFilterChip = (key) => {
    const defaultValue = INITIAL_FILTERS[key];
    setFilters((previous) => ({
      ...previous,
      [key]: defaultValue === undefined ? "" : defaultValue,
    }));
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
            <button type="button" className="button button-secondary" onClick={saveCurrentSearch}>
              Save Search
            </button>
            <button type="button" className="button button-secondary" onClick={() => setShowAdvanced((previous) => !previous)}>
              {showAdvanced ? "Hide" : "Advanced"}
            </button>
            <button type="button" className="button button-secondary" onClick={onReset}>
              Reset
            </button>
          </div>
        </div>

        <form onSubmit={onSearch} style={{ display: "grid", gap: "0.65rem" }}>
          {savedSearches.length > 0 && (
            <div>
              <label className="form-label">Saved searches</label>
              <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "0.42rem" }}>
                <select
                  className="form-input"
                  value={selectedSavedId}
                  onChange={(event) => applySavedSearch(event.target.value)}
                >
                  <option value="">Select saved search...</option>
                  {savedSearches.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.name}
                    </option>
                  ))}
                </select>
                <button type="button" className="button button-secondary" onClick={deleteSavedSearch} disabled={!selectedSavedId}>
                  Delete
                </button>
              </div>
            </div>
          )}

          <div>
            <label className="form-label">Search mode</label>
            <select className="form-input" value={mode} onChange={(event) => setMode(event.target.value)}>
              {SEARCH_MODES.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label">Keyword</label>
            <input className="form-input" value={filters.query} onChange={(event) => onChange("query", event.target.value)} placeholder="Name, city, profession" />
          </div>

          {mode === "profile_id" && (
            <div>
              <label className="form-label">Profile ID</label>
              <input
                className="form-input"
                value={filters.profileId}
                onChange={(event) => onChange("profileId", event.target.value)}
                placeholder="Enter exact profile ID"
              />
            </div>
          )}

          <div>
            <label className="form-label">Looking for</label>
            <select className="form-input" value={filters.lookingFor} onChange={(event) => onChange("lookingFor", event.target.value)}>
              {filterMeta.gender.map((value) => (
                <option key={value}>{value}</option>
              ))}
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
              {filterMeta.religion.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label">Caste</label>
            <select className="form-input" value={filters.caste} onChange={(event) => onChange("caste", event.target.value)}>
              {filterMeta.caste.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label">Marital status</label>
            <select className="form-input" value={filters.maritalStatus} onChange={(event) => onChange("maritalStatus", event.target.value)}>
              {filterMeta.maritalStatus.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label">Mother tongue</label>
            <select className="form-input" value={filters.motherTongue} onChange={(event) => onChange("motherTongue", event.target.value)}>
              {(filterMeta.motherTongue || FALLBACK_FILTER_META.motherTongue).map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>

          {showAdvanced && (
            <>
              <div>
                <label className="form-label">Education</label>
                <select className="form-input" value={filters.education} onChange={(event) => onChange("education", event.target.value)}>
                  {filterMeta.education.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="form-label">Profession</label>
                <select className="form-input" value={filters.profession} onChange={(event) => onChange("profession", event.target.value)}>
                  {filterMeta.profession.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="form-label">Annual income</label>
                <select className="form-input" value={filters.income} onChange={(event) => onChange("income", event.target.value)}>
                  {filterMeta.income.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.48rem" }}>
                <div>
                  <label className="form-label">Min height (cm)</label>
                  <input className="form-input" type="number" min={120} max={250} value={filters.minHeight} onChange={(event) => onChange("minHeight", clampNumber(event.target.value, 120, 250))} />
                </div>
                <div>
                  <label className="form-label">Max height (cm)</label>
                  <input className="form-input" type="number" min={120} max={250} value={filters.maxHeight} onChange={(event) => onChange("maxHeight", clampNumber(event.target.value, 120, 250))} />
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

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.48rem" }}>
                <label className="form-label" style={{ display: "flex", gap: "0.35rem", alignItems: "center" }}>
                  <input type="checkbox" checked={filters.verifiedOnly} onChange={(event) => onChange("verifiedOnly", event.target.checked)} />
                  Verified only
                </label>
                <label className="form-label" style={{ display: "flex", gap: "0.35rem", alignItems: "center" }}>
                  <input type="checkbox" checked={filters.withPhotoOnly} onChange={(event) => onChange("withPhotoOnly", event.target.checked)} />
                  With photo only
                </label>
                <label className="form-label" style={{ display: "flex", gap: "0.35rem", alignItems: "center" }}>
                  <input type="checkbox" checked={filters.premiumOnly} onChange={(event) => onChange("premiumOnly", event.target.checked)} />
                  Premium only
                </label>
                <label className="form-label" style={{ display: "flex", gap: "0.35rem", alignItems: "center" }}>
                  <input type="checkbox" checked={filters.onlineNow} onChange={(event) => onChange("onlineNow", event.target.checked)} />
                  Online now
                </label>
                <label className="form-label" style={{ display: "flex", gap: "0.35rem", alignItems: "center" }}>
                  <input type="checkbox" checked={filters.withHoroscopeOnly} onChange={(event) => onChange("withHoroscopeOnly", event.target.checked)} />
                  With horoscope only
                </label>
              </div>

              <div style={{ border: "1px dashed var(--line)", borderRadius: 12, padding: "0.7rem" }}>
                <p className="form-label" style={{ marginBottom: "0.45rem" }}>Premium Filters (Locked)</p>
                <div style={{ display: "grid", gap: "0.35rem", fontSize: "0.78rem", color: "var(--ink-muted)" }}>
                  <label style={{ display: "flex", gap: "0.35rem", alignItems: "center", opacity: 0.72 }}>
                    <input type="checkbox" disabled />
                    Profile verification level
                  </label>
                  <label style={{ display: "flex", gap: "0.35rem", alignItems: "center", opacity: 0.72 }}>
                    <input type="checkbox" disabled />
                    Last active within 24h
                  </label>
                  <label style={{ display: "flex", gap: "0.35rem", alignItems: "center", opacity: 0.72 }}>
                    <input type="checkbox" disabled />
                    Horoscope compatibility threshold
                  </label>
                </div>
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
            <select className="form-input" style={{ minWidth: 160 }} value={sort} onChange={(event) => setSort(event.target.value)}>
              {SORT_OPTIONS.map((item) => (
                <option key={item.value} value={item.value}>
                  Sort: {item.label}
                </option>
              ))}
            </select>
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
          <>
            {activeFilterChips.length > 0 && (
              <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap", marginBottom: "0.8rem" }}>
                {activeFilterChips.map((chip) => (
                  <button
                    key={chip.key}
                    type="button"
                    className="button button-secondary"
                    style={{ padding: "0.32rem 0.58rem", fontSize: "0.75rem" }}
                    onClick={() => removeFilterChip(chip.key)}
                  >
                    {chip.label} ×
                  </button>
                ))}
              </div>
            )}

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

            {user && searched && (
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "1rem" }}>
                <p style={{ margin: 0, color: "var(--ink-muted)", fontSize: "0.84rem" }}>
                  Page {pagination.page} • Total {pagination.total}
                </p>
                <div style={{ display: "flex", gap: "0.45rem" }}>
                  <button
                    type="button"
                    className="button button-secondary"
                    disabled={pagination.page <= 1 || loading}
                    onClick={() => executeSearch(Math.max(1, pagination.page - 1))}
                  >
                    Previous
                  </button>
                  <button
                    type="button"
                    className="button button-secondary"
                    disabled={!pagination.hasNextPage || loading}
                    onClick={() => executeSearch(pagination.page + 1)}
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </>
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
