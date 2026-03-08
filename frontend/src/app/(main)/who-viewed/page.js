"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import api from "../../../services/api";
import PageEmptyState from "../../../components/states/PageEmptyState";
import PageLoadingState from "../../../components/states/PageLoadingState";

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

  useEffect(() => {
    const fetchData = async () => {
      try {
        const res = await api.get("/interactions/profile-viewers");
        setViewers(Array.isArray(res.data) && res.data.length > 0 ? res.data : MOCK_VIEWERS);
      } catch {
        setViewers(MOCK_VIEWERS);
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
    <div style={{ display: "grid", gap: "0.95rem" }}>
      <section className="listing-hero">
        <div>
          <p className="section-label" style={{ marginBottom: "0.22rem" }}>
            Visibility Insights
          </p>
          <h1 className="section-title" style={{ margin: 0, fontSize: "clamp(1.64rem, 3vw, 2.2rem)" }}>
            Who Viewed Your Profile
          </h1>
          <p className="section-copy" style={{ marginTop: "0.38rem", fontSize: "0.92rem" }}>
            Monitor profile attention and convert high-intent visitors into conversations.
          </p>
          <div className="result-metrics">
            <span className="metric-chip metric-chip-highlight">{viewerStats.total} visitors</span>
            <span className="metric-chip">{viewerStats.recent} recent</span>
            <span className="metric-chip">{viewerStats.verified} verified</span>
          </div>
        </div>

        <div className="hero-actions" style={{ display: "flex", gap: "0.45rem", alignItems: "center" }}>
          <Link href="/pricing" className="button button-primary">
            Upgrade Access
          </Link>
        </div>
      </section>

      <section className="panel upgrade-banner" style={{ padding: "0.9rem" }}>
        <p style={{ margin: 0, color: "#8a5b00", fontSize: "0.85rem", fontWeight: 650 }}>
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
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(270px, 1fr))", gap: "0.9rem" }}>
          {viewers.map((viewer, index) => {
            const tier = getViewerTier(index);
            const gated = tier === "gated";

            return (
              <article key={viewer.id || viewer.userId} className="panel listing-stage viewer-card" style={{ padding: "0.72rem", position: "relative", overflow: "hidden" }}>
                {gated && (
                  <div className="gated-overlay">
                    <Link href="/pricing" className="button button-primary" style={{ paddingInline: "0.9rem" }}>
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

                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "0.45rem", flexWrap: "wrap" }}>
                      <h3 style={{ margin: 0, fontSize: "0.96rem", lineHeight: 1.14 }}>
                        {viewer.firstName}, {viewer.age}
                      </h3>
                      {viewer.isVerified && <span className="chip chip-support">Verified</span>}
                    </div>

                    <p className="profile-meta" style={{ margin: "0.22rem 0 0", color: "var(--ink-muted)", fontSize: "0.83rem" }}>
                      {viewer.profession} | {viewer.city}
                    </p>
                    <p style={{ margin: "0.24rem 0 0", color: "var(--ink-muted)", fontSize: "0.74rem" }}>
                      Viewed {viewer.receivedAt}
                    </p>
                  </div>

                  <Link href={`/profile/${viewer.userId}`} className="button button-secondary icon-only-btn view-fab" style={{ padding: "0.52rem 0.72rem" }} aria-label="Open profile" title="Open profile">
                    ↗
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
