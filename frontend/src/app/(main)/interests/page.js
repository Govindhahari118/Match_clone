"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import api from "../../../services/api";

const MOCK_RECEIVED = [
  { id: "r1", userId: "u1", firstName: "Arjun", age: 29, city: "Mumbai", profession: "Doctor", photo: "https://randomuser.me/api/portraits/men/11.jpg", isVerified: true, match: 92, receivedAt: "2h ago" },
  { id: "r2", userId: "u2", firstName: "Rohan", age: 27, city: "Bangalore", profession: "Engineer", photo: "https://randomuser.me/api/portraits/men/12.jpg", isVerified: true, match: 86, receivedAt: "5h ago" },
  { id: "r3", userId: "u3", firstName: "Karan", age: 31, city: "Delhi", profession: "Lawyer", photo: "https://randomuser.me/api/portraits/men/13.jpg", isVerified: false, match: 80, receivedAt: "1d ago" },
  { id: "r4", userId: "u4", firstName: "Vivek", age: 28, city: "Pune", profession: "MBA Exec", photo: "https://randomuser.me/api/portraits/men/14.jpg", isVerified: true, match: 75, receivedAt: "2d ago" },
];

const MOCK_SENT = [
  { id: "s1", userId: "u5", firstName: "Priya", age: 26, city: "Mumbai", profession: "Doctor", photo: "https://randomuser.me/api/portraits/women/44.jpg", isVerified: true, match: 94, status: "pending", sentAt: "1h ago" },
  { id: "s2", userId: "u6", firstName: "Ananya", age: 24, city: "Bangalore", profession: "Engineer", photo: "https://randomuser.me/api/portraits/women/45.jpg", isVerified: true, match: 89, status: "accepted", sentAt: "3h ago" },
  { id: "s3", userId: "u7", firstName: "Kavya", age: 27, city: "Chennai", profession: "CA", photo: "https://randomuser.me/api/portraits/women/46.jpg", isVerified: false, match: 75, status: "declined", sentAt: "1d ago" },
];

const MOCK_MUTUAL = [
  { id: "m1", userId: "u8", firstName: "Nisha", age: 25, city: "Hyderabad", profession: "Designer", photo: "https://randomuser.me/api/portraits/women/49.jpg", isVerified: true, match: 88, matchedAt: "30m ago" },
  { id: "m2", userId: "u9", firstName: "Shreya", age: 26, city: "Jaipur", profession: "Teacher", photo: "https://randomuser.me/api/portraits/women/50.jpg", isVerified: false, match: 81, matchedAt: "2h ago" },
  { id: "m3", userId: "u10", firstName: "Tanvi", age: 28, city: "Kolkata", profession: "Architect", photo: "https://randomuser.me/api/portraits/women/51.jpg", isVerified: true, match: 77, matchedAt: "1d ago" },
];

const TABS = [
  { key: "received", label: "Received" },
  { key: "sent", label: "Sent" },
  { key: "mutual", label: "Mutual" },
];

const STATUS_META = {
  pending: { label: "Pending", tone: "status-pending" },
  accepted: { label: "Accepted", tone: "status-accepted" },
  declined: { label: "Declined", tone: "status-declined" },
};

function getMatchTier(matchScore) {
  if (matchScore >= 90) return "elite";
  if (matchScore >= 80) return "strong";
  return "rising";
}

export default function InterestsPage() {
  const [activeTab, setActiveTab] = useState("received");
  const [data, setData] = useState({ received: MOCK_RECEIVED, sent: MOCK_SENT, mutual: MOCK_MUTUAL });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchInterests = async () => {
      setLoading(true);
      try {
        const res = await api.get(`/interactions/interests?type=${activeTab}`);
        if (Array.isArray(res.data) && res.data.length > 0) {
          setData((previous) => ({ ...previous, [activeTab]: res.data }));
        }
      } catch {
        // Keep fallback mock data in demo mode.
      } finally {
        setLoading(false);
      }
    };

    fetchInterests();
  }, [activeTab]);

  const currentList = data[activeTab] || [];

  const tabCounts = useMemo(
    () => ({
      received: data.received?.length || 0,
      sent: data.sent?.length || 0,
      mutual: data.mutual?.length || 0,
    }),
    [data]
  );

  const headlineStats = useMemo(() => {
    const pendingSent = (data.sent || []).filter((item) => item.status === "pending").length;
    return {
      total: tabCounts.received + tabCounts.sent + tabCounts.mutual,
      pendingReceived: tabCounts.received,
      pendingSent,
      mutual: tabCounts.mutual,
    };
  }, [tabCounts, data.sent]);

  const handleAccept = async (userId) => {
    try {
      await api.post("/interactions/like", { receiverId: userId });
      toast.success("Interest accepted.");
    } catch {
      toast.info("Accepted in demo mode.");
    }

    setData((previous) => ({
      ...previous,
      received: previous.received.filter((item) => item.userId !== userId),
    }));
  };

  const handleDecline = async (userId) => {
    try {
      await api.post("/interactions/decline", { userId });
      toast.info("Interest declined.");
    } catch {
      toast.info("Declined in demo mode.");
    }

    setData((previous) => ({
      ...previous,
      received: previous.received.filter((item) => item.userId !== userId),
    }));
  };

  return (
    <div style={{ display: "grid", gap: "0.95rem" }}>
      <section className="listing-hero">
        <div>
          <p className="section-label" style={{ marginBottom: "0.22rem" }}>
            Relationship Pipeline
          </p>
          <h1 className="section-title" style={{ margin: 0, fontSize: "clamp(1.64rem, 3vw, 2.2rem)" }}>
            Interest Center
          </h1>
          <p className="section-copy" style={{ marginTop: "0.38rem", fontSize: "0.92rem" }}>
            Track incoming interests, outgoing requests, and mutual connections in one streamlined view.
          </p>

          <div className="result-metrics">
            <span className="metric-chip metric-chip-highlight">{headlineStats.pendingReceived} received</span>
            <span className="metric-chip">{headlineStats.pendingSent} pending sent</span>
            <span className="metric-chip">{headlineStats.mutual} mutual</span>
            <span className="metric-chip">{headlineStats.total} total</span>
          </div>
        </div>
      </section>

      <section className="panel interests-tabs-shell" style={{ padding: "0.6rem" }}>
        <div className="interests-tab-row">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              className={`interests-tab-btn ${activeTab === tab.key ? "active" : ""}`}
              onClick={() => setActiveTab(tab.key)}
            >
              <span>{tab.label}</span>
              <span className="tab-badge">{tabCounts[tab.key]}</span>
            </button>
          ))}
        </div>
      </section>

      {loading ? (
        <div style={{ display: "grid", gap: "0.75rem" }}>
          {Array.from({ length: 4 }).map((_, index) => (
            <div key={`interest-loading-${index}`} className="panel listing-stage skeleton-tile" style={{ height: 110 }} />
          ))}
        </div>
      ) : currentList.length === 0 ? (
        <section className="panel listing-stage" style={{ textAlign: "center", padding: "2.2rem" }}>
          <h2 style={{ marginTop: 0, marginBottom: "0.38rem" }}>No {TABS.find((item) => item.key === activeTab)?.label.toLowerCase()} interests right now</h2>
          <p style={{ margin: 0, color: "var(--ink-muted)", fontSize: "0.9rem" }}>
            Continue exploring profiles to keep your pipeline active and increase high-quality conversations.
          </p>
          <Link href="/matches" className="button button-primary" style={{ marginTop: "0.9rem" }}>
            Browse Matches
          </Link>
        </section>
      ) : (
        <div style={{ display: "grid", gap: "0.72rem" }}>
          {currentList.map((item) => {
            const matchTier = getMatchTier(Number(item.match) || 0);
            const timeLabel =
              activeTab === "received"
                ? `Received ${item.receivedAt}`
                : activeTab === "sent"
                  ? `Sent ${item.sentAt}`
                  : `Matched ${item.matchedAt}`;

            return (
              <article key={item.id} className="panel listing-stage interest-card" style={{ padding: "0.8rem" }}>
                <div className="interest-main-row">
                  <div className="interest-avatar-wrap">
                    <Image src={item.photo} alt={item.firstName} width={74} height={74} unoptimized className="interest-avatar" />
                    <span className={`match-badge match-badge-${matchTier}`}>{item.match}%</span>
                  </div>

                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "0.45rem", flexWrap: "wrap" }}>
                      <h3 style={{ margin: 0, fontSize: "1rem", lineHeight: 1.15 }}>
                        {item.firstName}, {item.age}
                      </h3>
                      {item.isVerified && <span className="chip chip-support">Verified</span>}
                    </div>

                    <p className="profile-meta" style={{ margin: "0.22rem 0 0", color: "var(--ink-muted)", fontSize: "0.86rem" }}>
                      {item.profession} | {item.city}
                    </p>
                    <p style={{ margin: "0.28rem 0 0", color: "var(--ink-muted)", fontSize: "0.76rem" }}>{timeLabel}</p>

                    {activeTab === "sent" && item.status && STATUS_META[item.status] && (
                      <span className={`status-pill ${STATUS_META[item.status].tone}`}>{STATUS_META[item.status].label}</span>
                    )}
                  </div>

                  <div className="interest-actions-wrap">
                    {activeTab === "received" && (
                      <>
                        <button type="button" className="button button-primary" onClick={() => handleAccept(item.userId)}>
                          Accept
                        </button>
                        <button type="button" className="button button-secondary" onClick={() => handleDecline(item.userId)}>
                          Decline
                        </button>
                      </>
                    )}

                    {activeTab === "mutual" && (
                      <Link href={`/chat/${item.userId}`} className="button button-primary">
                        Chat
                      </Link>
                    )}

                    {activeTab === "sent" && item.status === "accepted" && (
                      <Link href={`/chat/${item.userId}`} className="button button-primary">
                        Chat
                      </Link>
                    )}

                    <Link href={`/profile/${item.userId}`} className="button button-secondary">
                      View
                    </Link>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}

      {activeTab === "received" && currentList.length > 0 && (
        <section className="panel" style={{ padding: "0.9rem", borderColor: "rgba(15, 118, 110, 0.24)", background: "linear-gradient(145deg, rgba(234, 250, 246, 0.9), rgba(248, 252, 255, 0.9))" }}>
          <p style={{ margin: 0, color: "#0f5f58", fontSize: "0.86rem", fontWeight: 650 }}>
            Tip: Accepting an interest unlocks faster conversation and helps momentum.
          </p>
        </section>
      )}

      <style jsx>{`
        .listing-hero {
          padding: 1rem;
          border-radius: 22px;
          border: 1px solid rgba(29, 78, 216, 0.2);
          background: linear-gradient(142deg, rgba(255, 255, 255, 0.98), rgba(241, 247, 255, 0.93));
          box-shadow: 0 16px 34px rgba(15, 23, 42, 0.09);
        }

        .result-metrics {
          display: flex;
          align-items: center;
          flex-wrap: wrap;
          gap: 0.42rem;
          margin-top: 0.62rem;
        }

        .metric-chip {
          display: inline-flex;
          align-items: center;
          padding: 0.24rem 0.6rem;
          border-radius: 999px;
          border: 1px solid rgba(148, 163, 184, 0.4);
          background: rgba(255, 255, 255, 0.9);
          color: var(--ink-muted);
          font-size: 0.74rem;
          font-weight: 680;
        }

        .metric-chip-highlight {
          border-color: rgba(227, 68, 117, 0.3);
          background: rgba(227, 68, 117, 0.13);
          color: #b32458;
        }

        .interests-tabs-shell {
          border: 1px solid rgba(29, 78, 216, 0.16);
          background: linear-gradient(160deg, rgba(255, 255, 255, 0.96), rgba(247, 250, 255, 0.93));
          box-shadow: 0 12px 30px rgba(15, 23, 42, 0.07);
        }

        .interests-tab-row {
          display: grid;
          grid-template-columns: repeat(3, minmax(0, 1fr));
          gap: 0.5rem;
        }

        .interests-tab-btn {
          border: 1px solid rgba(29, 78, 216, 0.15);
          background: rgba(255, 255, 255, 0.85);
          color: var(--ink-muted);
          border-radius: 12px;
          min-height: 42px;
          padding: 0.45rem 0.58rem;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: 0.44rem;
          font-size: 0.86rem;
          font-weight: 700;
          cursor: pointer;
          transition: all 0.2s ease;
        }

        .interests-tab-btn:hover {
          border-color: rgba(227, 68, 117, 0.24);
          color: var(--ink);
          transform: translateY(-1px);
        }

        .interests-tab-btn.active {
          border-color: rgba(227, 68, 117, 0.35);
          color: #b32458;
          background: rgba(255, 235, 244, 0.9);
          box-shadow: 0 10px 22px rgba(227, 68, 117, 0.14);
        }

        .tab-badge {
          border-radius: 999px;
          border: 1px solid rgba(148, 163, 184, 0.3);
          background: rgba(255, 255, 255, 0.84);
          padding: 0.1rem 0.45rem;
          font-size: 0.71rem;
          line-height: 1.2;
        }

        .interest-card {
          transition:
            transform 0.22s ease,
            box-shadow 0.22s ease,
            border-color 0.22s ease;
        }

        .interest-card:hover {
          transform: translateY(-2px);
          border-color: rgba(227, 68, 117, 0.2);
          box-shadow: 0 16px 28px rgba(15, 23, 42, 0.1);
        }

        .interest-main-row {
          display: flex;
          align-items: center;
          gap: 0.78rem;
          flex-wrap: wrap;
        }

        .interest-avatar-wrap {
          position: relative;
          width: 74px;
          height: 74px;
          flex-shrink: 0;
        }

        .interest-avatar {
          width: 74px;
          height: 74px;
          border-radius: 18px;
          object-fit: cover;
          border: 2px solid rgba(227, 68, 117, 0.18);
        }

        .profile-meta {
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .match-badge {
          position: absolute;
          right: -4px;
          bottom: -4px;
          display: inline-flex;
          align-items: center;
          padding: 0.2rem 0.45rem;
          border-radius: 999px;
          border: 1px solid transparent;
          font-size: 0.68rem;
          font-weight: 760;
          line-height: 1;
        }

        .match-badge-elite {
          color: #066848;
          border-color: rgba(6, 104, 72, 0.22);
          background: rgba(194, 241, 224, 0.95);
        }

        .match-badge-strong {
          color: #b32458;
          border-color: rgba(227, 68, 117, 0.3);
          background: rgba(254, 217, 231, 0.95);
        }

        .match-badge-rising {
          color: #945b09;
          border-color: rgba(196, 131, 18, 0.26);
          background: rgba(255, 238, 202, 0.95);
        }

        .status-pill {
          display: inline-flex;
          align-items: center;
          margin-top: 0.4rem;
          padding: 0.2rem 0.5rem;
          border-radius: 999px;
          border: 1px solid transparent;
          font-size: 0.72rem;
          font-weight: 700;
          line-height: 1;
        }

        .status-pending {
          color: #945b09;
          border-color: rgba(196, 131, 18, 0.24);
          background: rgba(255, 238, 202, 0.82);
        }

        .status-accepted {
          color: #066848;
          border-color: rgba(6, 104, 72, 0.22);
          background: rgba(194, 241, 224, 0.92);
        }

        .status-declined {
          color: #b32458;
          border-color: rgba(227, 68, 117, 0.3);
          background: rgba(254, 217, 231, 0.92);
        }

        .interest-actions-wrap {
          margin-left: auto;
          display: flex;
          align-items: center;
          gap: 0.44rem;
          flex-wrap: wrap;
        }

        .skeleton-tile {
          overflow: hidden;
          background: linear-gradient(100deg, rgba(238, 244, 253, 0.95) 8%, rgba(255, 255, 255, 0.98) 40%, rgba(238, 244, 253, 0.95) 72%);
          background-size: 200% 100%;
          animation: shimmer 1.3s linear infinite;
        }

        @keyframes shimmer {
          0% {
            background-position: 200% 0;
          }
          100% {
            background-position: -200% 0;
          }
        }

        @media (max-width: 980px) {
          .interests-tab-row {
            grid-template-columns: 1fr;
          }
        }

        @media (max-width: 720px) {
          .interest-actions-wrap {
            margin-left: 0;
            width: 100%;
          }

          .interest-actions-wrap :global(a),
          .interest-actions-wrap :global(button) {
            flex: 1;
            justify-content: center;
          }
        }
      `}</style>
    </div>
  );
}
