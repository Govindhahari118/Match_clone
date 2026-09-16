"use client";

import Image from "next/image";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";

const STORAGE_KEY = "matrimony-match-filters-v2";
const SCROLL_KEY = "matrimony-match-scroll-v2";

const DEFAULT_FILTERS = {
  keyword: "",
  minAge: "",
  maxAge: "",
  city: "",
  state: "",
  religion: "",
  caste: "",
  motherTongue: "",
  maritalStatus: "",
  education: "",
  profession: "",
  verifiedOnly: false,
  withPhotoOnly: false,
  lastActiveDays: "30",
  sort: "relevance",
};

function loadStoredFilters() {
  if (typeof window === "undefined") return DEFAULT_FILTERS;
  try {
    return { ...DEFAULT_FILTERS, ...(JSON.parse(sessionStorage.getItem(STORAGE_KEY) || "{}")) };
  } catch {
    return DEFAULT_FILTERS;
  }
}

function humanize(value) {
  return String(value || "").replace(/_/g, " ").replace(/\b\w/g, (char) => char.toUpperCase());
}

function compactParams(filters, page = 1) {
  const params = { mode: "regular", page, limit: 20, sort: filters.sort || "relevance" };
  Object.entries(filters).forEach(([key, value]) => {
    if (key === "sort") return;
    if (typeof value === "boolean") {
      if (value) params[key] = true;
    } else if (String(value || "").trim() !== "") {
      params[key] = value;
    }
  });
  return params;
}

function MatchCard({ item, onInterest, onSkip, onHide, onShortlist }) {
  const verified = item.verification || {};
  const trustChips = [
    verified.identity === "verified" ? "Identity verified" : null,
    verified.photo === "verified" ? "Photo verified" : null,
    verified.phone === "verified" ? "Phone verified" : null,
  ].filter(Boolean).slice(0, 2);

  return (
    <article className="panel panel-hover" style={{ overflow: "hidden", display: "grid" }}>
      <div style={{ minHeight: 245, background: "#f3efe8", position: "relative" }}>
        {item.photo ? (
          <Image src={item.photo} alt={`${item.firstName || "Member"} profile photo`} fill sizes="(max-width: 768px) 100vw, 33vw" style={{ objectFit: "cover" }} />
        ) : (
          <div style={{ minHeight: 245, display: "grid", placeItems: "center", color: "var(--ink-muted)", textAlign: "center", padding: "1rem" }}>
            <div><div style={{ fontSize: "2.2rem", fontWeight: 800 }}>{(item.firstName || "M").charAt(0)}</div><div style={{ marginTop: "0.3rem", fontSize: "0.8rem" }}>{item.photoLocked ? "Photo access is restricted by this member" : "No approved photo available"}</div></div>
          </div>
        )}
        <div style={{ position: "absolute", top: 10, left: 10, display: "flex", gap: "0.32rem", flexWrap: "wrap" }}>
          {item.activity?.label && <span className="chip chip-support">{item.activity.label}</span>}
        </div>
      </div>

      <div style={{ padding: "0.8rem", display: "grid", gap: "0.55rem" }}>
        <div>
          <div style={{ display: "flex", justifyContent: "space-between", gap: "0.5rem", alignItems: "baseline" }}>
            <h2 style={{ margin: 0, fontSize: "1.15rem", fontFamily: "var(--font-display)" }}>{item.firstName || "Member"}{item.age ? `, ${item.age}` : ""}</h2>
            <span style={{ color: "var(--ink-muted)", fontSize: "0.74rem" }}>{humanize(item.compatibilityStrength || "")}</span>
          </div>
          <p style={{ margin: "0.2rem 0 0", color: "var(--ink-muted)", fontSize: "0.82rem" }}>{[item.profession, item.city, item.state].filter(Boolean).join(" · ") || "Profile details available"}</p>
        </div>

        <div style={{ display: "flex", gap: "0.32rem", flexWrap: "wrap" }}>
          {trustChips.map((label) => <span key={label} className="chip chip-support">✓ {label}</span>)}
          {item.managedBy && <span className="chip chip-support">Managed by {humanize(item.managedBy)}</span>}
          {Number.isFinite(item.profileCompleteness) && <span className="chip chip-support">Profile {item.profileCompleteness}%</span>}
        </div>

        {item.whyRecommended?.length > 0 && (
          <div className="panel" style={{ padding: "0.52rem", background: "#faf8f4" }}>
            <strong style={{ fontSize: "0.74rem" }}>Why this profile</strong>
            <p style={{ margin: "0.18rem 0 0", fontSize: "0.75rem", color: "var(--ink-muted)", lineHeight: 1.45 }}>{item.whyRecommended.join(" · ")}</p>
          </div>
        )}

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.4rem" }}>
          <Link href={`/profile/${item.userId}`} className="button button-secondary">View Profile</Link>
          <button type="button" className="button button-primary" onClick={() => onInterest(item)}>Send Interest</button>
          <button type="button" className="button button-secondary" onClick={() => onShortlist(item)}>Shortlist</button>
          <button type="button" className="button button-secondary" onClick={() => onSkip(item)}>Skip for now</button>
        </div>
        <button type="button" onClick={() => onHide(item)} style={{ border: 0, background: "transparent", color: "var(--ink-muted)", cursor: "pointer", fontSize: "0.76rem" }}>Don’t show this profile again</button>
      </div>
    </article>
  );
}

export default function MatchesPage() {
  const { user } = useAuth();
  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(1);
  const [hasNextPage, setHasNextPage] = useState(false);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    setFilters(loadStoredFilters());
    setInitialized(true);
  }, []);

  useEffect(() => {
    if (!initialized || typeof window === "undefined") return;
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(filters));
  }, [filters, initialized]);

  useEffect(() => {
    if (!initialized || typeof window === "undefined") return undefined;
    const restore = Number(sessionStorage.getItem(SCROLL_KEY) || 0);
    if (restore > 0) window.requestAnimationFrame(() => window.scrollTo({ top: restore, behavior: "auto" }));
    const save = () => sessionStorage.setItem(SCROLL_KEY, String(window.scrollY));
    window.addEventListener("scroll", save, { passive: true });
    return () => window.removeEventListener("scroll", save);
  }, [initialized]);

  const fetchPage = useCallback(async (targetPage = 1, append = false) => {
    if (!user) {
      setItems([]);
      setLoading(false);
      return;
    }
    append ? setLoadingMore(true) : setLoading(true);
    setError("");
    try {
      const response = await api.get("/search", { params: compactParams(filters, targetPage) });
      const data = response.data || {};
      const incoming = Array.isArray(data.items) ? data.items : [];
      setItems((current) => append ? [...current, ...incoming] : incoming);
      setPage(data.page || targetPage);
      setHasNextPage(Boolean(data.hasNextPage));
      setTotal(Number(data.total || incoming.length));
    } catch (requestError) {
      if (!append) setItems([]);
      setError(requestError?.response?.data?.error || "Live matches could not be loaded. No demo profiles have been substituted.");
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }, [filters, user]);

  useEffect(() => {
    if (!initialized) return;
    const timer = setTimeout(() => fetchPage(1, false), 180);
    return () => clearTimeout(timer);
  }, [fetchPage, initialized]);

  const activeFilterCount = useMemo(() => Object.entries(filters).filter(([key, value]) => {
    if (key === "sort") return value !== DEFAULT_FILTERS.sort;
    if (typeof value === "boolean") return value;
    return String(value || "").trim() !== "" && value !== DEFAULT_FILTERS[key];
  }).length, [filters]);

  const removeItem = (userId) => setItems((current) => current.filter((item) => item.userId !== userId));

  const sendInterest = async (item) => {
    try {
      const response = await api.post("/interactions/like", { receiverId: item.userId });
      toast.success(response.data?.isMatch ? "Mutual connection created." : "Interest sent.");
      removeItem(item.userId);
    } catch (requestError) {
      toast.error(requestError?.response?.data?.error || "Interest could not be sent.");
    }
  };

  const skip = async (item) => {
    try {
      await api.post(`/trust/users/${item.userId}/skip`);
      removeItem(item.userId);
      toast.info("Skipped for now. The profile has a cooldown before it can reappear.");
    } catch (requestError) {
      toast.error(requestError?.response?.data?.error || "Profile could not be skipped.");
    }
  };

  const hide = async (item) => {
    try {
      await api.post(`/trust/users/${item.userId}/hide`);
      removeItem(item.userId);
      toast.info("Profile hidden until you explicitly restore it.");
    } catch (requestError) {
      toast.error(requestError?.response?.data?.error || "Profile could not be hidden.");
    }
  };

  const shortlist = async (item) => {
    try {
      await api.post("/shortlist/add", { shortlistedUserId: item.userId });
      toast.success("Added to shortlist.");
    } catch (requestError) {
      toast.error(requestError?.response?.data?.error || "Shortlist could not be updated.");
    }
  };

  if (!user) {
    return (
      <div style={{ maxWidth: 760, margin: "0 auto" }}>
        <section className="panel" style={{ padding: "1.4rem", textAlign: "center" }}>
          <p className="section-label">Live matrimony discovery</p>
          <h1 style={{ margin: "0.25rem 0", fontFamily: "var(--font-display)" }}>Login to view real eligible profiles</h1>
          <p style={{ color: "var(--ink-muted)", lineHeight: 1.55 }}>We do not substitute fabricated members when live data is unavailable. After login, discovery respects member privacy, blocks, activity freshness and your must-match preferences.</p>
          <Link className="button button-primary" href="/login">Login / Create account</Link>
        </section>
      </div>
    );
  }

  return (
    <div style={{ display: "grid", gap: "0.9rem" }}>
      <header className="panel" style={{ padding: "1rem" }}>
        <div style={{ display: "flex", justifyContent: "space-between", gap: "0.7rem", alignItems: "flex-start", flexWrap: "wrap" }}>
          <div>
            <p className="section-label" style={{ marginBottom: "0.2rem" }}>Fresh discovery</p>
            <h1 style={{ margin: 0, fontFamily: "var(--font-display)", fontSize: "1.8rem" }}>Matches</h1>
            <p style={{ margin: "0.35rem 0 0", color: "var(--ink-muted)" }}>Active, eligible profiles only. Paid status does not manufacture compatibility or bypass filters.</p>
          </div>
          <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap" }}><span className="chip chip-support">{total} eligible</span><span className="chip chip-support">{activeFilterCount} filters</span></div>
        </div>
      </header>

      <section className="panel" style={{ padding: "0.9rem" }}>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(160px,1fr))", gap: "0.55rem" }}>
          <input className="form-input" value={filters.keyword} onChange={(e) => setFilters((p) => ({ ...p, keyword: e.target.value }))} placeholder="Name, city, profession…" />
          <input className="form-input" type="number" min="18" max="80" value={filters.minAge} onChange={(e) => setFilters((p) => ({ ...p, minAge: e.target.value }))} placeholder="Min age" />
          <input className="form-input" type="number" min="18" max="80" value={filters.maxAge} onChange={(e) => setFilters((p) => ({ ...p, maxAge: e.target.value }))} placeholder="Max age" />
          <input className="form-input" value={filters.city} onChange={(e) => setFilters((p) => ({ ...p, city: e.target.value }))} placeholder="City" />
          <input className="form-input" value={filters.state} onChange={(e) => setFilters((p) => ({ ...p, state: e.target.value }))} placeholder="State" />
          <input className="form-input" value={filters.religion} onChange={(e) => setFilters((p) => ({ ...p, religion: e.target.value }))} placeholder="Religion" />
          <input className="form-input" value={filters.caste} onChange={(e) => setFilters((p) => ({ ...p, caste: e.target.value }))} placeholder="Community / caste" />
          <input className="form-input" value={filters.motherTongue} onChange={(e) => setFilters((p) => ({ ...p, motherTongue: e.target.value }))} placeholder="Mother tongue" />
          <input className="form-input" value={filters.profession} onChange={(e) => setFilters((p) => ({ ...p, profession: e.target.value }))} placeholder="Profession" />
          <select className="form-input" value={filters.lastActiveDays} onChange={(e) => setFilters((p) => ({ ...p, lastActiveDays: e.target.value }))}><option value="7">Active in 7 days</option><option value="30">Active in 30 days</option><option value="60">Active in 60 days</option><option value="90">Active in 90 days</option><option value="">Any non-stale active profile</option></select>
          <select className="form-input" value={filters.sort} onChange={(e) => setFilters((p) => ({ ...p, sort: e.target.value }))}><option value="relevance">Best preference alignment</option><option value="activity">Most recently active</option><option value="newest">Fresh activity first</option></select>
        </div>
        <div style={{ display: "flex", gap: "0.55rem", flexWrap: "wrap", marginTop: "0.6rem" }}>
          <label className="chip chip-support"><input type="checkbox" checked={filters.verifiedOnly} onChange={(e) => setFilters((p) => ({ ...p, verifiedOnly: e.target.checked }))} /> Verified only</label>
          <label className="chip chip-support"><input type="checkbox" checked={filters.withPhotoOnly} onChange={(e) => setFilters((p) => ({ ...p, withPhotoOnly: e.target.checked }))} /> Approved photo only</label>
          <button type="button" className="button button-secondary" onClick={() => setFilters(DEFAULT_FILTERS)}>Reset filters</button>
          <Link href="/settings" className="button button-secondary">Must-match preferences</Link>
        </div>
      </section>

      {error && <div className="panel" style={{ padding: "0.9rem", borderColor: "#efb4a9" }}><strong>Couldn’t load live matches</strong><p style={{ margin: "0.25rem 0 0", color: "var(--ink-muted)" }}>{error}</p><button className="button button-secondary" type="button" style={{ marginTop: "0.55rem" }} onClick={() => fetchPage(1, false)}>Retry</button></div>}

      {loading ? (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(250px,1fr))", gap: "0.7rem" }}>{Array.from({ length: 6 }).map((_, i) => <div className="panel" key={i} style={{ height: 420, background: "#f7f3ec" }} />)}</div>
      ) : items.length === 0 && !error ? (
        <section className="panel" style={{ padding: "1.4rem", textAlign: "center" }}>
          <h2 style={{ marginTop: 0 }}>No profiles match all current rules</h2>
          <p style={{ color: "var(--ink-muted)", lineHeight: 1.5 }}>Your mandatory preferences have not been silently relaxed. Adjust a filter yourself or review which preferences are marked Must match.</p>
          <Link href="/settings" className="button button-secondary">Review preferences</Link>
        </section>
      ) : (
        <>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(260px,1fr))", gap: "0.75rem" }}>
            {items.map((item) => <MatchCard key={item.userId} item={item} onInterest={sendInterest} onSkip={skip} onHide={hide} onShortlist={shortlist} />)}
          </div>
          {hasNextPage && <div style={{ textAlign: "center" }}><button className="button button-secondary" type="button" disabled={loadingMore} onClick={() => fetchPage(page + 1, true)}>{loadingMore ? "Loading…" : "Load more"}</button></div>}
        </>
      )}
    </div>
  );
}
