"use client";

import Image from "next/image";
import Link from "next/link";
import { memo, useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";

const MOCK = [
  { id: "sl1", userId: "u1", firstName: "Priya", age: 26, city: "Mumbai", profession: "Doctor", photo: "https://randomuser.me/api/portraits/women/44.jpg", isVerified: true, match: 94, shortlistedAt: "2h ago" },
  { id: "sl2", userId: "u2", firstName: "Ananya", age: 24, city: "Bangalore", profession: "Engineer", photo: "https://randomuser.me/api/portraits/women/45.jpg", isVerified: true, match: 89, shortlistedAt: "5h ago" },
  { id: "sl3", userId: "u3", firstName: "Kavya", age: 27, city: "Chennai", profession: "CA", photo: "https://randomuser.me/api/portraits/women/46.jpg", isVerified: false, match: 85, shortlistedAt: "1d ago" },
  { id: "sl4", userId: "u4", firstName: "Riya", age: 25, city: "Pune", profession: "Architect", photo: "https://randomuser.me/api/portraits/women/47.jpg", isVerified: true, match: 82, shortlistedAt: "2d ago" },
  { id: "sl5", userId: "u5", firstName: "Simran", age: 28, city: "Delhi", profession: "Lawyer", photo: "https://randomuser.me/api/portraits/women/48.jpg", isVerified: true, match: 79, shortlistedAt: "3d ago" },
  { id: "sl6", userId: "u6", firstName: "Naina", age: 23, city: "Hyderabad", profession: "Designer", photo: "https://randomuser.me/api/portraits/women/49.jpg", isVerified: false, match: 75, shortlistedAt: "5d ago" },
];

function getMatchTier(matchScore) {
  if (matchScore >= 90) return "elite";
  if (matchScore >= 80) return "strong";
  return "rising";
}

const ShortlistCard = memo(function ShortlistCard({ profile, viewMode, onRemove, onInterest }) {
  const matchTier = getMatchTier(Number(profile.match) || 0);
  const metaLine = [profile.profession, profile.city].filter(Boolean).join(" | ");

  if (viewMode === "list") {
    return (
      <article className="panel panel-hover anim-rise listing-stage shortlist-card shortlist-list" style={{ overflow: "hidden", display: "flex" }}>
        <div className="shortlist-media" style={{ width: 196, flexShrink: 0, position: "relative" }}>
          <Image
            className="shortlist-photo"
            src={profile.photo}
            alt={`${profile.firstName} profile`}
            width={620}
            height={760}
            sizes="(max-width: 760px) 100vw, 196px"
            style={{ width: "100%", height: "100%", objectFit: "cover" }}
          />
          <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(7, 13, 30, 0.72), transparent 55%)" }} />
          <div style={{ position: "absolute", top: 10, left: 10 }}>
            <span className={`match-badge match-badge-${matchTier}`}>{profile.match}% Match</span>
          </div>
        </div>

        <div className="shortlist-content" style={{ flex: 1, padding: "1rem", minWidth: 0 }}>
          <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: "0.75rem", marginBottom: "0.65rem" }}>
            <div style={{ minWidth: 0 }}>
              <h3 style={{ margin: 0, fontSize: "1.08rem", lineHeight: 1.16 }}>
                {profile.firstName}, {profile.age}
              </h3>
              <p className="profile-meta" style={{ margin: "0.24rem 0 0", color: "var(--ink-muted)", fontSize: "0.85rem" }}>
                {metaLine}
              </p>
              <p style={{ margin: "0.28rem 0 0", color: "var(--ink-muted)", fontSize: "0.76rem" }}>
                Saved {profile.shortlistedAt}
              </p>
            </div>
            <button type="button" className="button button-secondary remove-fab" onClick={() => onRemove(profile.userId)}>
              Remove
            </button>
          </div>

          <div style={{ display: "flex", gap: "0.45rem", flexWrap: "wrap", marginTop: "0.65rem" }}>
            {profile.isVerified && <span className="chip chip-support">Verified</span>}
            {profile.profession && <span className="chip chip-brand">{profile.profession}</span>}
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "0.55rem", marginTop: "0.9rem" }}>
            <button type="button" className="button button-primary" onClick={() => onInterest(profile.userId)}>
              Send Interest
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
    <article className="panel panel-hover anim-rise listing-stage shortlist-card" style={{ overflow: "hidden" }}>
      <div className="shortlist-media" style={{ position: "relative", height: 220 }}>
        <Image
          className="shortlist-photo"
          src={profile.photo}
          alt={`${profile.firstName} profile`}
          width={640}
          height={920}
          sizes="(max-width: 760px) 100vw, (max-width: 1200px) 50vw, 33vw"
          style={{ width: "100%", height: "100%", objectFit: "cover" }}
        />
        <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(9, 18, 36, 0.76), transparent 58%)" }} />
        <div style={{ position: "absolute", top: 10, left: 10 }}>
          <span className={`match-badge match-badge-${matchTier}`}>{profile.match}% Match</span>
        </div>
        <button type="button" onClick={() => onRemove(profile.userId)} className="button button-secondary remove-fab" style={{ position: "absolute", top: 10, right: 10 }}>
          Remove
        </button>
        <div style={{ position: "absolute", left: 12, bottom: 12, color: "white", right: 12 }}>
          <h3 style={{ margin: 0, fontSize: "1.12rem", lineHeight: 1.14 }}>
            {profile.firstName}, {profile.age}
          </h3>
          <p className="profile-meta" style={{ margin: "0.22rem 0 0", fontSize: "0.82rem", opacity: 0.92 }}>
            {metaLine}
          </p>
        </div>
      </div>

      <div style={{ padding: "0.95rem" }}>
        <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap", marginBottom: "0.72rem" }}>
          {profile.isVerified && <span className="chip chip-support">Verified</span>}
          <span className="chip chip-brand">Saved {profile.shortlistedAt}</span>
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
});

export default function ShortlistsPage() {
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [viewMode, setViewMode] = useState("grid");

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const res = await api.get("/shortlist");
        setList(Array.isArray(res.data) && res.data.length > 0 ? res.data : MOCK);
      } catch {
        setList(MOCK);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const shortlistStats = useMemo(() => {
    const total = list.length;
    const verified = list.filter((item) => item.isVerified).length;
    const highMatch = list.filter((item) => Number(item.match || 0) >= 85).length;
    return { total, verified, highMatch };
  }, [list]);

  const handleRemove = async (userId) => {
    setList((previous) => previous.filter((item) => item.userId !== userId));
    try {
      await api.post("/shortlist/remove", { shortlistedUserId: userId });
      toast.success("Removed from shortlist.");
    } catch {
      toast.info("Removed in demo mode.");
    }
  };

  const handleInterest = async (userId) => {
    try {
      await api.post("/interactions/like", { receiverId: userId });
      toast.success("Interest sent.");
    } catch {
      toast.info("Interest sent in demo mode.");
    }
  };

  return (
    <div style={{ display: "grid", gap: "0.95rem" }}>
      <section className="listing-hero">
        <div>
          <p className="section-label" style={{ marginBottom: "0.22rem" }}>
            Saved Pipeline
          </p>
          <h1 className="section-title" style={{ margin: 0, fontSize: "clamp(1.64rem, 3vw, 2.2rem)" }}>
            Shortlists
          </h1>
          <p className="section-copy" style={{ marginTop: "0.38rem", fontSize: "0.92rem" }}>
            Revisit your prioritized profiles and move quickly when the timing is right.
          </p>
          <div className="result-metrics">
            <span className="metric-chip metric-chip-highlight">{shortlistStats.total} saved</span>
            <span className="metric-chip">{shortlistStats.verified} verified</span>
            <span className="metric-chip">{shortlistStats.highMatch} high compatibility</span>
          </div>
        </div>

        <div className="hero-actions" style={{ display: "flex", gap: "0.45rem", alignItems: "center" }}>
          <Link href="/matches" className="button button-primary">
            Add More
          </Link>
          <button type="button" className={`button view-switch-btn ${viewMode === "grid" ? "button-primary" : "button-secondary"}`} onClick={() => setViewMode("grid")}>
            Grid
          </button>
          <button type="button" className={`button view-switch-btn ${viewMode === "list" ? "button-primary" : "button-secondary"}`} onClick={() => setViewMode("list")}>
            List
          </button>
        </div>
      </section>

      {loading ? (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(230px, 1fr))", gap: "0.9rem" }}>
          {Array.from({ length: 6 }).map((_, index) => (
            <div key={`shortlist-loading-${index}`} className="panel listing-stage skeleton-tile" style={{ height: 330 }} />
          ))}
        </div>
      ) : list.length === 0 ? (
        <section className="panel listing-stage" style={{ textAlign: "center", padding: "2.2rem" }}>
          <h2 style={{ marginTop: 0, marginBottom: "0.38rem" }}>No shortlisted profiles yet</h2>
          <p style={{ margin: 0, color: "var(--ink-muted)", fontSize: "0.9rem" }}>
            Browse matches and save profiles here for quick comparison.
          </p>
          <Link href="/matches" className="button button-primary" style={{ marginTop: "0.9rem" }}>
            Browse Matches
          </Link>
        </section>
      ) : (
        <div className="results-grid" style={{ display: "grid", gridTemplateColumns: viewMode === "grid" ? "repeat(auto-fit, minmax(240px, 1fr))" : "1fr", gap: "0.9rem" }}>
          {list.map((profile) => (
            <ShortlistCard key={profile.id || profile.userId} profile={profile} viewMode={viewMode} onRemove={handleRemove} onInterest={handleInterest} />
          ))}
        </div>
      )}

    </div>
  );
}
