"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import api from "../../../services/api";
import PageEmptyState from "../../../components/states/PageEmptyState";
import PageLoadingState from "../../../components/states/PageLoadingState";
import PageHero from "../../../components/PageHero";

const MOCK_VIEWERS = [
  { id: "v1", userId: "u1", firstName: "Arjun", age: 29, city: "Mumbai", profession: "Doctor", photo: "https://randomuser.me/api/portraits/men/11.jpg", isVerified: true, receivedAt: "5m ago" },
  { id: "v2", userId: "u2", firstName: "Rohan", age: 27, city: "Bangalore", profession: "Engineer", photo: "https://randomuser.me/api/portraits/men/12.jpg", isVerified: true, receivedAt: "2h ago" },
  { id: "v3", userId: "u3", firstName: "Karan", age: 31, city: "Delhi", profession: "Lawyer", photo: "https://randomuser.me/api/portraits/men/13.jpg", isVerified: false, receivedAt: "1d ago" },
  { id: "v4", userId: "u4", firstName: "Vivek", age: 28, city: "Pune", profession: "MBA", photo: "https://randomuser.me/api/portraits/men/14.jpg", isVerified: true, receivedAt: "2d ago" },
  { id: "v5", userId: "u5", firstName: "Rahul", age: 30, city: "Chennai", profession: "Architect", photo: "https://randomuser.me/api/portraits/men/15.jpg", isVerified: false, receivedAt: "3d ago" },
  { id: "v6", userId: "u6", firstName: "Dev", age: 26, city: "Hyderabad", profession: "Designer", photo: "https://randomuser.me/api/portraits/men/16.jpg", isVerified: true, receivedAt: "5d ago" },
];

function getViewerTier(index) {
  if (index < 2) return "priority";
  if (index < 4) return "standard";
  return "gated";
}

export default function WhoViewedPage() {
  const [viewers, setViewers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isPreview, setIsPreview] = useState(false);

  const dataUpdatedLabel = useMemo(
    () => new Date().toLocaleDateString(undefined, { month: "short", day: "numeric" }),
    []
  );

  useEffect(() => {
    const fetchData = async () => {
      try {
        const res = await api.get("/interactions/profile-viewers");
        const nextViewers = Array.isArray(res.data) && res.data.length > 0 ? res.data : MOCK_VIEWERS;
        setViewers(nextViewers);
        setIsPreview(nextViewers === MOCK_VIEWERS);
      } catch {
        setViewers(MOCK_VIEWERS);
        setIsPreview(true);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const viewerStats = useMemo(() => {
    const total = viewers.length;
    const verified = viewers.filter((viewer) => viewer.isVerified).length;
    const recent = viewers.filter((viewer) => /m ago|h ago/.test(viewer.receivedAt || "")).length;
    return { total, verified, recent };
  }, [viewers]);

  return (
    <div className="stack-md">
      <PageHero
        eyebrow="Profile Visibility"
        title="Who Viewed Your Profile"
        copy="Track visitor intent and convert profile views into meaningful conversations."
        className="listing-hero viewer-hero"
        actions={(
          <Link href="/pricing" className="button button-primary">
            Upgrade Access
          </Link>
        )}
      >
        <div className="result-metrics">
          <span className="metric-chip metric-chip-highlight">{viewerStats.total} visitors</span>
          <span className="metric-chip">{viewerStats.recent} recent</span>
          <span className="metric-chip">{viewerStats.verified} verified</span>
          {isPreview && <span className="metric-chip">Preview mode</span>}
        </div>
        <p className="data-freshness">Updated {dataUpdatedLabel}</p>
      </PageHero>

      <section className="status-banner warning viewer-upgrade-banner">
        <p className="viewer-upgrade-copy">
          Premium unlock: full visitor history, visitor intent scoring, and priority outreach suggestions.
        </p>
      </section>

      {loading ? (
        <PageLoadingState
          title="Loading profile viewers..."
          description="Collecting your recent profile visitor activity."
          compact
        />
      ) : viewers.length === 0 ? (
        <PageEmptyState
          title="No profile views yet"
          description="Complete your profile and stay active to increase discoverability."
          primaryActionLabel="Complete Profile"
          primaryActionHref="/profile"
        />
      ) : (
        <div className="viewer-grid">
          {viewers.map((viewer, index) => {
            const tier = getViewerTier(index);
            const gated = tier === "gated";

            return (
              <article key={viewer.id || viewer.userId} className="panel listing-stage viewer-card viewer-card-body">
                {gated && (
                  <div className="gated-overlay">
                    <Link href="/pricing" className="button button-primary viewer-gated-action">
                      Unlock Viewer
                    </Link>
                  </div>
                )}

                <div className="viewer-row">
                  <div className="viewer-avatar-wrap">
                    <Image
                      src={viewer.photo}
                      alt={viewer.firstName}
                      width={64}
                      height={64}
                      sizes="64px"
                      className="viewer-avatar"
                    />
                    <span className={`viewer-dot ${tier}`} />
                  </div>

                  <div className="viewer-info">
                    <div className="viewer-head">
                      <h3 className="viewer-name">
                        {viewer.firstName}, {viewer.age}
                      </h3>
                      {viewer.isVerified && <span className="chip chip-support">Verified</span>}
                    </div>

                    <p className="profile-meta viewer-meta">
                      {viewer.profession} | {viewer.city}
                    </p>
                    <p className="viewer-time">
                      Viewed {viewer.receivedAt}
                    </p>
                  </div>

                  <Link href={`/profile/${viewer.userId}`} className="button button-secondary viewer-view-btn" aria-label="View profile" title="View profile">
                    View Profile
                  </Link>
                </div>
              </article>
            );
          })}
        </div>
      )}

    </div>
  );
}


