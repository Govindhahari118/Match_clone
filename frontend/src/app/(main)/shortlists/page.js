"use client";

import Image from "next/image";
import Link from "next/link";
import { memo, useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";

const PROFILE_PLACEHOLDER = "/profile-placeholder.svg";

function formatSavedAt(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "recently";
  return date.toLocaleDateString([], { day: "numeric", month: "short", year: "numeric" });
}

const ShortlistCard = memo(function ShortlistCard({ profile, viewMode, onRemove, onInterest, busyId }) {
  const compact = viewMode === "list";
  const compatibility = profile.compatibility || {};
  const reasons = Array.isArray(compatibility.reasons) ? compatibility.reasons : [];
  const score = Number.isFinite(Number(compatibility.score)) ? Number(compatibility.score) : null;

  return (
    <article className={`panel panel-hover listing-stage shortlist-card ${compact ? "shortlist-list" : ""}`} style={{ overflow: "hidden", display: compact ? "flex" : "block" }}>
      <div className="shortlist-media" style={{ width: compact ? 196 : "100%", height: compact ? "auto" : 230, minHeight: compact ? 210 : undefined, flexShrink: 0, position: "relative" }}>
        <Image
          className="shortlist-photo"
          src={profile.photo || PROFILE_PLACEHOLDER}
          alt={profile.photo ? `${profile.firstName || "Member"} profile` : "Profile photo unavailable"}
          width={640}
          height={760}
          sizes={compact ? "196px" : "(max-width: 760px) 100vw, 33vw"}
          style={{ width: "100%", height: "100%", objectFit: "cover" }}
        />
        <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(9,18,36,.72), transparent 60%)" }} />
        {score !== null && (
          <span className="chip chip-brand" style={{ position: "absolute", left: 10, top: 10 }}>
            {compatibility.strength || "Compatibility"} · {score}%
          </span>
        )}
        <button type="button" className="button button-secondary remove-fab" disabled={busyId === profile.userId} onClick={() => onRemove(profile.userId)} style={{ position: "absolute", top: 10, right: 10 }}>
          Remove
        </button>
        <div style={{ position: "absolute", left: 12, bottom: 12, right: 12, color: "white" }}>
          <h3 style={{ margin: 0 }}>{profile.firstName || "Member"}{profile.age ? `, ${profile.age}` : ""}</h3>
          <p style={{ margin: "0.2rem 0 0", fontSize: "0.82rem", opacity: 0.92 }}>{[profile.profession, profile.city].filter(Boolean).join(" · ")}</p>
        </div>
      </div>

      <div style={{ padding: "0.95rem", flex: 1, minWidth: 0 }}>
        <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap" }}>
          {profile.isVerified && <span className="chip chip-support">Account verified</span>}
          {profile.photoVerified && <span className="chip chip-support">Photo verified</span>}
          {profile.activity?.label && <span className="chip chip-brand">{profile.activity.label}</span>}
          {profile.managedBy && <span className="chip">Managed by {profile.managedBy}</span>}
        </div>

        <p style={{ margin: "0.55rem 0 0", color: "var(--ink-muted)", fontSize: "0.78rem" }}>
          Saved {formatSavedAt(profile.shortlistedAt)}
        </p>

        {reasons.length > 0 && (
          <div style={{ marginTop: "0.65rem" }}>
            <strong style={{ fontSize: "0.8rem" }}>Why this profile fits</strong>
            <ul style={{ margin: "0.3rem 0 0", paddingLeft: "1.05rem", color: "var(--ink-muted)", fontSize: "0.8rem" }}>
              {reasons.slice(0, 3).map((reason) => <li key={reason}>{reason}</li>)}
            </ul>
          </div>
        )}

        <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "0.52rem", marginTop: "0.85rem" }}>
          <button type="button" className="button button-primary" disabled={busyId === profile.userId} onClick={() => onInterest(profile.userId)}>Send Interest</button>
          <Link href={`/profile/${profile.userId}`} className="button button-secondary">View</Link>
        </div>
      </div>
    </article>
  );
});

export default function ShortlistsPage() {
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [viewMode, setViewMode] = useState("grid");
  const [busyId, setBusyId] = useState(null);

  const loadShortlists = async () => {
    setLoading(true);
    setErrorMessage("");
    try {
      const res = await api.get("/shortlist");
      setList(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
      setList([]);
      setErrorMessage(error.response?.data?.error || "Shortlisted profiles could not be loaded. No demo profiles were substituted.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadShortlists(); }, []);

  const shortlistStats = useMemo(() => ({
    total: list.length,
    verified: list.filter((item) => item.isVerified).length,
    active: list.filter((item) => ["active_today", "active_this_week", "active_recently"].includes(item.activity?.code)).length,
  }), [list]);

  const handleRemove = async (userId) => {
    const previous = list;
    setBusyId(userId);
    setList((items) => items.filter((item) => item.userId !== userId));
    try {
      await api.post("/shortlist/remove", { shortlistedUserId: userId });
      toast.success("Removed from shortlist.");
    } catch (error) {
      setList(previous);
      toast.error(error.response?.data?.error || "Unable to remove this profile.");
    } finally {
      setBusyId(null);
    }
  };

  const handleInterest = async (userId) => {
    setBusyId(userId);
    try {
      const response = await api.post("/interactions/like", { receiverId: userId });
      toast.success(response.data?.isMatch ? "Interest accepted. You are now connected." : "Interest sent.");
    } catch (error) {
      toast.error(error.response?.data?.error || "Unable to send interest.");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div style={{ display: "grid", gap: "0.95rem" }}>
      <section className="listing-hero">
        <div>
          <p className="section-label" style={{ marginBottom: "0.22rem" }}>Saved Profiles</p>
          <h1 className="section-title" style={{ margin: 0, fontSize: "clamp(1.64rem, 3vw, 2.2rem)" }}>Shortlists</h1>
          <p className="section-copy" style={{ marginTop: "0.38rem", fontSize: "0.92rem" }}>
            Only profiles you actually saved appear here. Unavailable, blocked, deleted, married, or paused profiles are suppressed by the server.
          </p>
          <div className="result-metrics">
            <span className="metric-chip metric-chip-highlight">{shortlistStats.total} saved</span>
            <span className="metric-chip">{shortlistStats.verified} account verified</span>
            <span className="metric-chip">{shortlistStats.active} recently active</span>
          </div>
        </div>
        <div className="hero-actions" style={{ display: "flex", gap: "0.45rem", alignItems: "center" }}>
          <Link href="/matches" className="button button-primary">Add More</Link>
          <button type="button" className={`button ${viewMode === "grid" ? "button-primary" : "button-secondary"}`} onClick={() => setViewMode("grid")}>Grid</button>
          <button type="button" className={`button ${viewMode === "list" ? "button-primary" : "button-secondary"}`} onClick={() => setViewMode("list")}>List</button>
        </div>
      </section>

      {errorMessage && (
        <section className="panel" style={{ padding: "0.9rem", borderColor: "rgba(185,28,28,.2)" }}>
          <p style={{ margin: 0, color: "#991b1b" }}>{errorMessage}</p>
          <button type="button" className="button button-secondary" style={{ marginTop: "0.65rem" }} onClick={loadShortlists}>Retry</button>
        </section>
      )}

      {loading ? (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(230px,1fr))", gap: "0.9rem" }}>
          {Array.from({ length: 4 }).map((_, index) => <div key={index} className="panel skeleton-tile" style={{ height: 330 }} />)}
        </div>
      ) : list.length === 0 ? (
        <section className="panel listing-stage" style={{ textAlign: "center", padding: "2.2rem" }}>
          <h2 style={{ marginTop: 0 }}>No shortlisted profiles</h2>
          <p style={{ color: "var(--ink-muted)" }}>This is the real current state of your shortlist.</p>
          <Link href="/matches" className="button button-primary">Browse Matches</Link>
        </section>
      ) : (
        <div className="results-grid" style={{ display: "grid", gridTemplateColumns: viewMode === "grid" ? "repeat(auto-fit,minmax(250px,1fr))" : "1fr", gap: "0.9rem" }}>
          {list.map((profile) => <ShortlistCard key={profile.shortlistId || profile.userId} profile={profile} viewMode={viewMode} onRemove={handleRemove} onInterest={handleInterest} busyId={busyId} />)}
        </div>
      )}
    </div>
  );
}
