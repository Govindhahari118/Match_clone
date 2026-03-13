"use client";

import Image from "next/image";
import Link from "next/link";
import { memo, useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";
import PageEmptyState from "../../../components/states/PageEmptyState";
import PageLoadingState from "../../../components/states/PageLoadingState";
import PageHero from "../../../components/PageHero";

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

function ViewIcon({ kind }) {
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

  return null;
}

const ShortlistCard = memo(function ShortlistCard({ profile, viewMode, onRemove, onInterest }) {
  const matchTier = getMatchTier(Number(profile.match) || 0);
  const metaLine = [profile.profession, profile.city].filter(Boolean).join(" | ");

  if (viewMode === "list") {
    return (
      <article className="panel panel-hover anim-rise listing-stage shortlist-card shortlist-card-list">
        <div className="shortlist-media">
          <Image
            className="shortlist-photo"
            src={profile.photo}
            alt={`${profile.firstName} profile`}
            width={620}
            height={760}
            sizes="(max-width: 760px) 100vw, 196px"
          />
          <div className="shortlist-media-overlay" />
          <span className={`match-badge match-badge-${matchTier}`}>{profile.match}% Match</span>
        </div>

        <div className="shortlist-content">
          <div className="shortlist-card-header">
            <div className="shortlist-card-title">
              <h3 className="shortlist-card-name">
                {profile.firstName}, {profile.age}
              </h3>
              <p className="profile-meta shortlist-card-meta">
                {metaLine}
              </p>
              <p className="shortlist-card-saved">
                Saved {profile.shortlistedAt}
              </p>
            </div>
            <button type="button" className="button button-secondary remove-fab icon-only-btn shortlist-remove" onClick={() => onRemove(profile.userId)} aria-label="Remove from shortlist" title="Remove from shortlist">
              x
            </button>
          </div>

          <div className="shortlist-card-tags">
            {profile.isVerified && <span className="chip chip-support">Verified</span>}
            {profile.profession && <span className="chip chip-brand">{profile.profession}</span>}
          </div>

          <div className="shortlist-card-actions">
            <button type="button" className="button button-primary" onClick={() => onInterest(profile.userId)}>
              Send Interest
            </button>
            <Link href={`/profile/${profile.userId}`} className="button button-secondary" aria-label="View profile" title="View profile">
              View Profile
            </Link>
          </div>
        </div>
      </article>
    );
  }

  return (
    <article className="panel panel-hover anim-rise listing-stage shortlist-card">
      <div className="shortlist-media">
        <Image
          className="shortlist-photo"
          src={profile.photo}
          alt={`${profile.firstName} profile`}
          width={640}
          height={920}
          sizes="(max-width: 760px) 100vw, (max-width: 1200px) 50vw, 33vw"
        />
        <div className="shortlist-media-overlay" />
        <span className={`match-badge match-badge-${matchTier}`}>{profile.match}% Match</span>
        <button type="button" onClick={() => onRemove(profile.userId)} className="button button-secondary remove-fab icon-only-btn shortlist-remove" aria-label="Remove from shortlist" title="Remove from shortlist">
          x
        </button>
      </div>

      <div className="shortlist-card-body">
        <div className="shortlist-card-heading">
          <h3 className="shortlist-card-name">
            {profile.firstName}, {profile.age}
          </h3>
          <p className="profile-meta shortlist-card-meta">
            {metaLine}
          </p>
          <p className="shortlist-card-saved">
            Saved {profile.shortlistedAt}
          </p>
        </div>

        <div className="shortlist-card-tags">
          {profile.isVerified && <span className="chip chip-support">Verified</span>}
          {profile.profession && <span className="chip chip-brand">{profile.profession}</span>}
        </div>

        <div className="shortlist-card-actions">
          <button type="button" className="button button-primary" onClick={() => onInterest(profile.userId)}>
            Send Interest
          </button>
          <Link href={`/profile/${profile.userId}`} className="button button-secondary" aria-label="View profile" title="View profile">
            View Profile
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
  const [isPreview, setIsPreview] = useState(false);

  const dataUpdatedLabel = useMemo(
    () => new Date().toLocaleDateString(undefined, { month: "short", day: "numeric" }),
    []
  );

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const res = await api.get("/shortlist");
        const nextList = Array.isArray(res.data) && res.data.length > 0 ? res.data : MOCK;
        setList(nextList);
        setIsPreview(nextList === MOCK);
      } catch {
        setList(MOCK);
        setIsPreview(true);
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
    if (!window.confirm("Remove this profile from your shortlist?")) {
      return;
    }
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
    <div className="stack-md">
      <PageHero
        eyebrow="Saved Matches"
        title="Shortlists"
        copy="Review your saved profiles and decide who to move into conversation."
        className="listing-hero shortlist-hero"
        actions={(
          <div className="shortlist-hero-actions">
            <Link href="/matches" className="button button-primary">
              Add More
            </Link>
            <button type="button" className={`button view-switch-btn icon-only-btn ${viewMode === "grid" ? "button-primary" : "button-secondary"}`} onClick={() => setViewMode("grid")} aria-label="Grid view" title="Grid view">
              <ViewIcon kind="grid" />
            </button>
            <button type="button" className={`button view-switch-btn icon-only-btn ${viewMode === "list" ? "button-primary" : "button-secondary"}`} onClick={() => setViewMode("list")} aria-label="List view" title="List view">
              <ViewIcon kind="list" />
            </button>
          </div>
        )}
      >
        <div className="result-metrics">
          <span className="metric-chip metric-chip-highlight">{shortlistStats.total} saved</span>
          <span className="metric-chip">{shortlistStats.verified} verified</span>
          <span className="metric-chip">{shortlistStats.highMatch} high compatibility</span>
          {isPreview && <span className="metric-chip">Preview mode</span>}
        </div>
        <p className="data-freshness">Updated {dataUpdatedLabel}</p>
      </PageHero>

      {loading ? (
        <PageLoadingState
          title="Loading shortlists..."
          description="Pulling your saved profiles and recent shortlist activity."
          compact
        />
      ) : list.length === 0 ? (
        <PageEmptyState
          title="No shortlisted profiles yet"
          description="Browse matches and save profiles here for quick comparison."
          primaryActionLabel="Browse Matches"
          primaryActionHref="/matches"
        />
      ) : (
        <div className={`results-grid shortlist-results-grid ${viewMode === "grid" ? "is-grid" : "is-list"}`}>
          {list.map((profile) => (
            <ShortlistCard key={profile.id || profile.userId} profile={profile} viewMode={viewMode} onRemove={handleRemove} onInterest={handleInterest} />
          ))}
        </div>
      )}

    </div>
  );
}



